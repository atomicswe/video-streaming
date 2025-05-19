package com.atomicswe.videostreaming.repositories;

import com.atomicswe.videostreaming.models.Video;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoContentStore extends ContentStore<Video, String> {

}
