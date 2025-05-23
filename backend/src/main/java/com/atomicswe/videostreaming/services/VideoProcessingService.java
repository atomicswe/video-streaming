package com.atomicswe.videostreaming.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class VideoProcessingService {
    @Value("${video.storage.path:videos}")
    private String videoStoragePath;

    @Async
    public CompletableFuture<String> processVideoAsync(Path tempFile, String finalFilename) {
        Path optimizedFile = Paths.get(videoStoragePath, finalFilename);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-i", tempFile.toString(),
                    "-movflags", "faststart",
                    "-c", "copy",
                    optimizedFile.toString()
            );

            Process process = pb.start();
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);

            if (finished && process.exitValue() == 0) {
                return CompletableFuture.completedFuture(finalFilename);
            } else {
                throw new RuntimeException("FFmpeg processing failed");
            }

        } catch (Exception e) {
            throw new RuntimeException("Video processing failed", e);
        }
    }
}
