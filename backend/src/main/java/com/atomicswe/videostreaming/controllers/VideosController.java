package com.atomicswe.videostreaming.controllers;

import com.atomicswe.videostreaming.models.Video;
import com.atomicswe.videostreaming.repositories.VideoContentStore;
import com.atomicswe.videostreaming.repositories.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.Optional;

import static java.lang.String.format;

@RestController
public class VideosController {
    private final VideoRepository videoRepository;
    private final VideoContentStore videoContentStore;

    @Autowired
    public VideosController(VideoRepository videoRepository, VideoContentStore videoContentStore) {
        this.videoRepository = videoRepository;
        this.videoContentStore = videoContentStore;
    }

    @GetMapping(value = "/videos/{file-name}")
    public final ResponseEntity<InputStreamResource> getVideoByFileName(
            @RequestHeader(value = "Range", required = false) String range,
            @PathVariable(value = "file-name") final String fileName) {

        long rangeStart = Long.parseLong(range.replace("bytes=","").split("-")[0]);
        long rangeEnd = Long.parseLong(range.replace("bytes=","").split("-")[0]);

        Optional<Video> video = videoRepository.findByName(fileName);
        if (video.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        long contentLength = video.get().getContentLength();

        InputStream inputStream = videoContentStore.getContent(video.get());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("video/mp4"));
        headers.set("Accept-Ranges", "bytes");
        headers.set("Expires", "0");
        headers.set("Cache-Control", "no-cache, no-store");
        headers.set("Connection", "keep-alive");
        headers.set("Content-Transfer-Encoding", "binary");
        headers.set("Content-Length", String.valueOf(rangeEnd - rangeStart));

        if (rangeStart == 0) {
            return new ResponseEntity<>(new InputStreamResource(inputStream), headers, HttpStatus.OK);
        } else {
            headers.set("Content-Range", format("bytes %s-%s/%s", rangeStart, rangeEnd, contentLength));
            return new ResponseEntity<>(new InputStreamResource(inputStream), headers, HttpStatus.PARTIAL_CONTENT);
        }
    }
}
