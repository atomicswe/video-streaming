package com.atomicswe.videostreaming.controllers;

import com.atomicswe.videostreaming.models.Video;
import com.atomicswe.videostreaming.repositories.FileSystemVideoRepository;
import com.atomicswe.videostreaming.services.VideoProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VideosControllerTest {
    @Mock
    private FileSystemVideoRepository videoRepository;
    @Mock
    private VideoProcessingService videoProcessingService;
    @InjectMocks
    private VideosController videosController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        videosController = new VideosController(videoRepository, videoProcessingService);
    }

    @Test
    void testGetAllVideos_NoVideos() {
        when(videoRepository.findAll()).thenReturn(Collections.emptyList());
        ResponseEntity<?> response = videosController.getAllVideos(null);
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        Map<?,?> body = (Map<?,?>) response.getBody();
        assertTrue(((List<?>) body.get("videos")).isEmpty());
    }

    @Test
    void testGetAllVideos_WithPagination() {
        List<Video> videos = Arrays.asList(new Video(), new Video(), new Video());
        when(videoRepository.findAll()).thenReturn(videos);
        VideosController.GetAllVideosRequest request = new VideosController.GetAllVideosRequest();
        request.setPage(0);
        request.setPageSize(2);
        ResponseEntity<?> response = videosController.getAllVideos(request);
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        Map<?,?> body = (Map<?,?>) response.getBody();
        assertEquals(0, body.get("currentPage"));
        assertEquals(1, body.get("totalPages"));
    }

    @Test
    void testUploadVideo() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "dummy".getBytes());
        File tempFile = File.createTempFile("temp", ".mp4");
        when(videoRepository.saveUploadedFile(any(), anyString())).thenReturn(tempFile);
        when(videoProcessingService.processVideoAsync(any(), anyString())).thenReturn(CompletableFuture.completedFuture("final.mp4"));
        ResponseEntity<?> response = videosController.uploadVideo(file);
        assertEquals(HttpStatusCode.valueOf(202), response.getStatusCode());
        Map<?,?> body = (Map<?,?>) response.getBody();
        assertEquals("processing", body.get("status"));
    }

    @Test
    void testCheckStatus_NotFound() {
        ResponseEntity<?> response = videosController.checkStatus("invalid");
        assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
    }

    @Test
    void testDeleteVideo_Success() {
        when(videoRepository.deleteFile(anyString())).thenReturn(true);
        ResponseEntity<?> response = videosController.deleteVideo("file.mp4");
        assertEquals(HttpStatusCode.valueOf(204), response.getStatusCode());
    }

    @Test
    void testDeleteVideo_NotFound() {
        when(videoRepository.deleteFile(anyString())).thenReturn(false);
        ResponseEntity<?> response = videosController.deleteVideo("file.mp4");
        assertEquals(HttpStatusCode.valueOf(404), response.getStatusCode());
    }
}

