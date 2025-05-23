package com.atomicswe.videostreaming.repositories;

import com.atomicswe.videostreaming.models.Video;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.io.File;

public interface FileSystemVideoRepository {
    List<Video> findAll();
    Optional<Video> getVideoByName(String name);
    Optional<File> getFileByName(String name);
    File saveUploadedFile(MultipartFile multipartFile, String fileName) throws IOException;
    File saveFile(Path sourcePath, String fileName) throws IOException;
    boolean deleteFile(String fileName);
    boolean exists(String fileName);
    long getFileSize(String fileName);
    Path getFullPath(String fileName);
    void cleanupTempFiles();
}
