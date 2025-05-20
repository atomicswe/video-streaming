package com.atomicswe.videostreaming.repositories;

import com.atomicswe.videostreaming.models.Video;
import java.util.List;
import java.util.Optional;
import java.io.File;

public interface FileSystemVideoRepository {
    List<Video> findAll();
    Optional<Video> findByName(String name);
    Optional<File> getFileByName(String name);
}
