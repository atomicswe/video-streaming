package com.atomicswe.videostreaming.repositories;

import com.atomicswe.videostreaming.models.Video;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class FileSystemVideoRepositoryImpl implements FileSystemVideoRepository {

    @Value("${video.storage.path:videos}")
    private String videoStoragePath;

    @Override
    public List<Video> findAll() {
        List<Video> videos = new ArrayList<>();
        File dir = new File(videoStoragePath);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".mp4")) {
                    videos.add(fileToVideo(file));
                }
            }
        }
        return videos;
    }

    @Override
    public Optional<Video> findByName(String name) {
        File file = new File(videoStoragePath, name);
        if (file.exists() && file.isFile()) {
            return Optional.of(fileToVideo(file));
        }
        return Optional.empty();
    }

    public Optional<File> getFileByName(String name) {
        File file = new File(videoStoragePath, name);
        if (file.exists() && file.isFile()) {
            return Optional.of(file);
        }
        return Optional.empty();
    }

    private Video fileToVideo(File file) {
        Video video = new Video();
        video.setName(file.getName());
        video.setContentLength(file.length());
        video.setCreated(new Date(file.lastModified()));
        video.setContentMimeType("video/mp4");
        // You can set more fields as needed
        return video;
    }
}
