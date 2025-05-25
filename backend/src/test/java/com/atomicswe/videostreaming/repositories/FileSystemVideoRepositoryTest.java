package com.atomicswe.videostreaming.repositories;

import com.atomicswe.videostreaming.models.Video;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemVideoRepositoryTest {
    private FileSystemVideoRepositoryImpl repository;
    private File tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("videos-test").toFile();
        repository = new FileSystemVideoRepositoryImpl(tempDir.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        File[] files = tempDir.listFiles();
        if (files != null) {
            for (File file : files) {
                boolean ignored = file.delete();
            }
        }
        boolean ignored = tempDir.delete();
    }

    @Test
    void testFindAll_Empty() {
        List<Video> videos = repository.findAll();
        assertTrue(videos.isEmpty());
    }

    @Test
    void testSaveUploadedFile_AndFindAll() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp4", "video/mp4", "dummy content".getBytes());
        File saved = repository.saveUploadedFile(file, "test.mp4");
        assertTrue(saved.exists());
        List<Video> videos = repository.findAll();
        assertEquals(1, videos.size());
        assertEquals("test.mp4", videos.get(0).getName());
    }

    @Test
    void testGetFileByName_NotFound() {
        Optional<File> file = repository.getFileByName("notfound.mp4");
        assertTrue(file.isEmpty());
    }

    @Test
    void testGetFileByName_Found() throws IOException {
        File f = new File(tempDir, "video.mp4");
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write("abc".getBytes());
        }
        Optional<File> found = repository.getFileByName("video.mp4");
        assertTrue(found.isPresent());
        assertEquals(f.getName(), found.get().getName());
    }

    @Test
    void testDeleteFile() throws IOException {
        File f = new File(tempDir, "delete.mp4");
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write("abc".getBytes());
        }
        assertTrue(f.exists());
        boolean deleted = repository.deleteFile("delete.mp4");
        assertTrue(deleted);
        assertFalse(f.exists());
    }

    @Test
    void testDeleteFile_NotFound() {
        boolean deleted = repository.deleteFile("notfound.mp4");
        assertFalse(deleted);
    }
}
