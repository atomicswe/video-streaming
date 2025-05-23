package com.atomicswe.videostreaming.repositories;

import com.atomicswe.videostreaming.models.Video;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Repository
public class FileSystemVideoRepositoryImpl implements FileSystemVideoRepository {

    @Value("${video.storage.path:videos}")
    private String videoStoragePath;

    private final Logger logger = LoggerFactory.getLogger(FileSystemVideoRepositoryImpl.class);

    @PostConstruct
    public void init() {
        File directory = new File(videoStoragePath);
        if (!directory.exists()) {
            boolean _ = directory.mkdirs();
            logger.info("Created video directory: {}", directory.getAbsolutePath());
        }
        logger.info("Video repository initialized at: {}", directory.getAbsolutePath());
    }

    @Override
    public List<Video> findAll() {
        logger.info("Finding all videos in: {}", videoStoragePath);

        List<Video> videos = new ArrayList<>();
        File dir = new File(videoStoragePath);
        if (dir.exists() && dir.isDirectory()) {
            logger.info("Directory exists: {}", dir.getAbsolutePath());
            File[] files = dir.listFiles();

            if (files == null) {
                logger.warn("No files found in directory: {}", dir.getAbsolutePath());
                return Collections.emptyList();
            }

            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".mp4")) {
                    videos.add(fileToVideo(file));
                }
            }
        }
        logger.info("Found {} videos", videos.size());
        return videos;
    }

    @Override
    public Optional<Video> getVideoByName(String name) {
        logger.info("Trying to get video by name: {}", name);
        File file = new File(videoStoragePath, name);
        if (file.exists() && file.isFile()) {
            logger.info("Video found: {}", file.getAbsolutePath());
            return Optional.of(fileToVideo(file));
        }
        return Optional.empty();
    }

    public Optional<File> getFileByName(String name) {
        logger.info("Trying to get file by name: {}", name);
        File file = new File(videoStoragePath, name);
        if (file.exists() && file.isFile()) {
            logger.info("File found: {}", file.getAbsolutePath());
            return Optional.of(file);
        }
        return Optional.empty();
    }

    private Video fileToVideo(File file) {
        logger.info("Converting file to Video object: {}", file.getName());
        Video video = new Video();
        video.setName(file.getName());
        video.setContentLength(file.length());
        video.setCreated(new Date(file.lastModified()));
        video.setContentMimeType("video/mp4");

        return video;
    }

    public File saveUploadedFile(MultipartFile multipartFile, String fileName) throws IOException {
        logger.info("Trying to save uploaded file: {}", fileName);
        File targetFile = new File(videoStoragePath, fileName);

        boolean _ = targetFile.getParentFile().mkdirs();

        try (InputStream input = multipartFile.getInputStream();
             OutputStream output = new FileOutputStream(targetFile)) {
            input.transferTo(output);
        }

        logger.info("Saved uploaded file: {} ({} bytes)", fileName, targetFile.length());
        return targetFile;
    }

    public File saveFile(Path sourcePath, String fileName) throws IOException {
        logger.info("Trying to save file: {} to {}", sourcePath, fileName);
        File targetFile = new File(videoStoragePath, fileName);

        Files.copy(sourcePath, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        logger.info("Saved file: {} ({} bytes)", fileName, targetFile.length());
        return targetFile;
    }

    public boolean deleteFile(String fileName) {
        logger.info("Trying to delete file: {}", fileName);
        File file = new File(videoStoragePath, fileName);
        boolean deleted = file.delete();

        if (deleted) {
            logger.info("Deleted file: {}", fileName);
        } else {
            logger.warn("Failed to delete file: {} in {}", fileName, file.getAbsolutePath());
        }

        return deleted;
    }

    public boolean exists(String fileName) {
        logger.info("Checking existence of file: {}", fileName);
        return new File(videoStoragePath, fileName).exists();
    }

    public long getFileSize(String fileName) {
        logger.info("Getting size of file: {}", fileName);
        File file = new File(videoStoragePath, fileName);
        return file.exists() ? file.length() : -1;
    }

    public Path getFullPath(String fileName) {
        logger.info("Getting full path of file: {}", fileName);
        return new File(videoStoragePath, fileName).toPath();
    }

    public void cleanupTempFiles() {
        logger.info("Cleaning up temporary files in: {}", videoStoragePath);
        File directory = new File(videoStoragePath);
        File[] tempFiles = directory.listFiles((dir, name) ->
                name.startsWith("temp_") && name.endsWith(".mp4"));

        if (tempFiles != null) {
            for (File tempFile : tempFiles) {
                if (System.currentTimeMillis() - tempFile.lastModified() > 3600000) {
                    if (tempFile.delete()) {
                        logger.info("Cleaned up old temp file: {}", tempFile.getName());
                    }
                }
            }
        }
    }
}
