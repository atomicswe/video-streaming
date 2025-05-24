package com.atomicswe.videostreaming.controllers;

import com.atomicswe.videostreaming.models.Video;
import com.atomicswe.videostreaming.repositories.FileSystemVideoRepository;
import com.atomicswe.videostreaming.services.VideoProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/videos")
public class VideosController {
    private final FileSystemVideoRepository videoRepository;
    private final VideoProcessingService videoProcessingService;
    private final Map<String, CompletableFuture<String>> processingJobs = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(VideosController.class);

    @Autowired
    public VideosController(FileSystemVideoRepository videoRepository,
                            VideoProcessingService videoProcessingService) {
        this.videoRepository = videoRepository;
        this.videoProcessingService = videoProcessingService;
    }

    //region Video Metadata
    @GetMapping(value = "/{file-name}/metadata")
    public final ResponseEntity<Video> getVideoMetadata(
            @PathVariable(value = "file-name") final String fileName) {

        Optional<Video> video = videoRepository.getVideoByName(fileName);
        if (video.isEmpty()) {
            logger.error("File not found: {}", fileName);
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(video.get(), headers, HttpStatus.OK);
    }
    //endregion

    //region Video Streaming
    @GetMapping("/{fileName}")
    public ResponseEntity<ResourceRegion> streamVideo(
            @RequestHeader(value = "Range", required = false) String range,
            @PathVariable("fileName") String fileName) {

        // validate file name to prevent directory traversal attacks
        if (!fileName.matches("[a-zA-Z0-9._-]+")) {
            logger.error("Received request with invalid file name: {}", fileName);
            return ResponseEntity.badRequest().build();
        }

        logger.info("Processing request for file: {}, Range: {}", fileName, range != null ? range : "none");

        Optional<File> fileOpt = videoRepository.getFileByName(fileName);
        if (fileOpt.isEmpty()) {
            logger.error("File not found: {}", fileName);
            return ResponseEntity.notFound().build();
        }

        File file = fileOpt.get();
        FileSystemResource videoResource = new FileSystemResource(file);
        long contentLength = file.length();

        if (range == null || !range.startsWith("bytes=")) {
            logger.info("No range specified, returning full video.");
            ResourceRegion fullRegion = new ResourceRegion(videoResource, 0, contentLength);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentLength(contentLength)
                    .body(fullRegion);
        }

        long rangeStart = 0;
        long rangeEnd = contentLength - 1;

        String rangeValue = range.substring(6);
        String[] rangeParts = rangeValue.split("-", 2);

        try {
            if (!rangeParts[0].isEmpty()) {
                rangeStart = Long.parseLong(rangeParts[0]);
            }

            if (rangeParts.length > 1 && !rangeParts[1].isEmpty()) {
                rangeEnd = Long.parseLong(rangeParts[1]);
            } else if (rangeParts[0].isEmpty() && rangeParts.length > 1) {
                long suffixLength = Long.parseLong(rangeParts[1]);
                rangeStart = Math.max(0, contentLength - suffixLength);
                rangeEnd = contentLength - 1;
            }

            if (rangeStart < 0 || rangeStart >= contentLength) {
                logger.error("Invalid range start: {} for file: {}", rangeStart, fileName);
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                        .build();
            }

            rangeEnd = Math.min(rangeEnd, contentLength - 1);

            if (rangeStart > rangeEnd) {
                logger.error("Invalid range: {}-{} for file: {}", rangeStart, rangeEnd, fileName);
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                        .build();
            }

        } catch (NumberFormatException e) {
            logger.error("Invalid range format: {} for file: {}", range, fileName);
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                    .build();
        }

        long rangeLength = rangeEnd - rangeStart + 1;
        ResourceRegion region = new ResourceRegion(videoResource, rangeStart, rangeLength);

        logger.info("Returning range: {}-{} of {}", rangeStart, rangeEnd, fileName);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentLength(rangeLength)
                .body(region);
    }
    //endregion

    //region Video List
    public static class GetAllVideosRequest {
        private Integer page;
        private Integer pageSize;

        public Integer getPage() {
            return page;
        }

        public Integer getPageSize() {
            return pageSize;
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> getAllVideos(
            @RequestBody(required = false) GetAllVideosRequest request
    ) {
        Integer page = request != null ? request.getPage() : null;
        Integer pageSize = request != null ? request.getPageSize() : null;

        logger.info("Processing request for videos list, page: {}, pageSize: {}",
                page != null ? page : 0,
                pageSize != null ? pageSize : 0);

        if (page != null && pageSize == null) {
            logger.error("Page size is required when page is specified.");
            return ResponseEntity.badRequest().body(null);
        }

        List<Video> videos = videoRepository.findAll();
        if (videos.isEmpty()) {
            logger.info("No videos found.");
            if (page != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("videos", Collections.emptyList());
                response.put("currentPage", 0);
                response.put("totalPages", 0);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(Map.of("videos", Collections.emptyList()));
            }
        }

        if (page != null) {
            int totalVideos = videos.size();
            int totalPages = (int) Math.ceil((double) totalVideos / pageSize) - 1;
            int firstIndex = page * pageSize;
            int lastIndex = Math.min(totalVideos, firstIndex + pageSize);

            if (page < 0 || firstIndex >= totalVideos) {
                logger.error("Invalid page number: {} for total videos: {}", page, totalVideos);
                return ResponseEntity.badRequest().body(null);
            }

            List<Video> pagedVideos = videos.subList(firstIndex, lastIndex);
            Map<String, Object> response = new HashMap<>();
            response.put("videos", pagedVideos);
            response.put("currentPage", page);
            response.put("totalPages", totalPages);

            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.ok(Map.of("videos", videos));
        }
    }
    //endregion

    //region Video Upload
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadVideo(@RequestParam("file") MultipartFile file) throws IOException {
        logger.info("Processing file: {}", file.getOriginalFilename());

        String tmpFilename = "temp_" + UUID.randomUUID() + ".mp4";
        File tmpFile = videoRepository.saveUploadedFile(file, tmpFilename);

        String fileName = file.getOriginalFilename();
        String finalFilename = UUID.randomUUID().toString().replace("-", "") +
                "-" +
                (fileName != null ? fileName.replace( ".mp4", "") : "") +
                ".mp4";

        CompletableFuture<String> future = videoProcessingService
                .processVideoAsync(tmpFile.toPath(), finalFilename);

        future.whenComplete((_, _) -> {
            logger.info("Finished processing: {}", finalFilename);
            boolean deleted = videoRepository.deleteFile(tmpFilename);
            logger.info("Deleted video? {}", deleted);
        });

        String jobId = UUID.randomUUID().toString();
        processingJobs.put(jobId, future);
        logger.info("Processing job: {}", jobId);

        return ResponseEntity.accepted()
                .body(Map.of(
                        "jobId", jobId,
                        "status", "processing",
                        "message", "Video upload started"
                ));
    }

    @GetMapping("/upload/status/{jobId}")
    public ResponseEntity<Map<String, Object>> checkStatus(@PathVariable String jobId) {
        logger.info("Getting info for job: {}", jobId);
        CompletableFuture<String> future = processingJobs.get(jobId);

        if (future == null) {
            return ResponseEntity.notFound().build();
        }

        if (future.isDone()) {
            try {
                String filename = future.get();
                return ResponseEntity.ok(Map.of(
                        "jobId", jobId,
                        "status", "completed",
                        "filename", filename,
                        "url", "/videos/" + filename
                ));
            } catch (Exception e) {
                return ResponseEntity.ok(Map.of(
                        "jobId", jobId,
                        "status", "failed",
                        "error", e.getMessage()
                ));
            }
        } else {
            return ResponseEntity.ok(Map.of(
                    "jobId", jobId,
                    "status", "processing"
            ));
        }
    }

    @Scheduled(fixedDelay = 3600000) // Every hour
    public void cleanupOldJobs() {
        logger.info("Cleaning up completed jobs");
        processingJobs.entrySet().removeIf(entry ->
                entry.getValue().isDone()
        );
    }
    //endregion

    //region Video Deletion
    @DeleteMapping("/{fileName}")
    public ResponseEntity<Void> deleteVideo(@PathVariable("fileName") String fileName) {
        logger.info("Deleting video: {}", fileName);
        boolean deleted = videoRepository.deleteFile(fileName);

        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    //endregion
}
