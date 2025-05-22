package com.atomicswe.videostreaming.controllers;

import com.atomicswe.videostreaming.repositories.FileSystemVideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Optional;

@RestController
@RequestMapping("/videos")
public class VideosController {
    private final FileSystemVideoRepository videoRepository;

    @Autowired
    public VideosController(FileSystemVideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    //region Video Metadata
    public record VideoMetadata(String name, long size, long lastModified, String mimeType) {}

    @GetMapping(value = "/{file-name}/metadata")
    public final ResponseEntity<VideoMetadata> getVideoMetadata(
            @PathVariable(value = "file-name") final String fileName) {

        Optional<File> fileOpt = videoRepository.getFileByName(fileName);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        File file = fileOpt.get();

        VideoMetadata videoMetadata = new VideoMetadata(
                file.getName(), file.length(), file.lastModified(), "video/mp4"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new ResponseEntity<>(videoMetadata, headers, HttpStatus.OK);
    }
    //endregion

    @GetMapping("/{fileName}")
    public ResponseEntity<ResourceRegion> getVideoByFileName(
            @RequestHeader(value = "Range", required = false) String range,
            @PathVariable("fileName") String fileName) {

        Optional<File> fileOpt = videoRepository.getFileByName(fileName);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        File file = fileOpt.get();
        FileSystemResource videoResource = new FileSystemResource(file);
        long contentLength = file.length();

        if (range == null || !range.startsWith("bytes=")) {
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
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                        .build();
            }

            rangeEnd = Math.min(rangeEnd, contentLength - 1);

            if (rangeStart > rangeEnd) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                        .build();
            }

        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                    .build();
        }

        long rangeLength = rangeEnd - rangeStart + 1;

        ResourceRegion region = new ResourceRegion(videoResource, rangeStart, rangeLength);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentLength(rangeLength)
                .body(region);
    }
}
