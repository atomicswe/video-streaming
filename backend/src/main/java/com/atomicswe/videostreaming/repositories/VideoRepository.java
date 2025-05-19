package com.atomicswe.videostreaming.repositories;

import com.atomicswe.videostreaming.models.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource(path="videos", collectionResourceRel="videos")
public interface VideoRepository extends JpaRepository<Video, Long> {
    Optional<Video> findByName(String name);
}
