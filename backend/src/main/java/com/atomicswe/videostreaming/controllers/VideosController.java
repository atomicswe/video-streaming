package com.atomicswe.videostreaming.controllers;

import com.atomicswe.videostreaming.repositories.FileSystemVideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Optional;

import static java.lang.String.format;

@RestController
public class VideosController {
    private final FileSystemVideoRepository videoRepository;

    @Autowired
    public VideosController(FileSystemVideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @GetMapping(value = "/videos/{file-name}")
    public final ResponseEntity<InputStreamResource> getVideoByFileName(
            @RequestHeader(value = "Range", required = false) String range,
            @PathVariable(value = "file-name") final String fileName) {

        Optional<File> fileOpt = videoRepository.getFileByName(fileName);
        if (fileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        File file = fileOpt.get();

        long contentLength = file.length();
        long rangeStart = 0;
        long rangeEnd = contentLength - 1;

        if (range != null && range.startsWith("bytes=")) {
            String[] ranges = range.substring(6).split("-");
            try {
                rangeStart = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    rangeEnd = Long.parseLong(ranges[1]);
                }
            } catch (NumberFormatException ignored) {}
        }

        InputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("video/mp4"));
        headers.set("Accept-Ranges", "bytes");
        headers.set("Expires", "0");
        headers.set("Cache-Control", "no-cache, no-store");
        headers.set("Connection", "keep-alive");
        headers.set("Content-Transfer-Encoding", "binary");

        if (rangeStart == 0) {
            return new ResponseEntity<>(new InputStreamResource(inputStream), headers, HttpStatus.OK);
        } else {
            headers.set("Content-Range", format("bytes %s-%s/%s", rangeStart, rangeEnd, contentLength));
            return new ResponseEntity<>(new InputStreamResource(inputStream), headers, HttpStatus.PARTIAL_CONTENT);
        }
    }
}
