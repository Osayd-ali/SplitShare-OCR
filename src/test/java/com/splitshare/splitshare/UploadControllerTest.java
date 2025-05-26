package com.splitshare.splitshare;

import com.splitshare.splitshare.controller.UploadController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UploadController.class)
public class UploadControllerTest {

        @Autowired
        private MockMvc mockMvc;

        // ✅ Test: Valid PNG upload
        @Test
        public void testValidPngUpload_Returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.png", "image/png", "fake image content".getBytes());

        mockMvc.perform(multipart("/api/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Upload successful"))
                .andExpect(jsonPath("$.fileName").exists())
                .andExpect(jsonPath("$.filePath").exists());
        }

        // ✅ Test: Valid JPEG upload (.jpeg)
        @Test
        public void testValidJpegUpload_Returns200() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "receipt.jpeg", "image/jpeg", "fake jpeg content".getBytes());

                mockMvc.perform(multipart("/api/upload")
                                .file(file))
                        .andExpect(status().isOk())
                        .andExpect(content().string(org.hamcrest.Matchers.containsString("Upload successful")));
        }

        // ✅ Test: Valid JPG upload (.jpg)
        @Test
        public void testValidJpgUpload_Returns200() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "receipt.jpg", "image/jpeg", "fake jpg content".getBytes());

                mockMvc.perform(multipart("/api/upload")
                        .file(file))
                        .andExpect(status().isOk())
                        .andExpect(content().string(org.hamcrest.Matchers.containsString("Upload successful")));
        }

        // ❌ Test: Invalid file type (.exe)
        @Test
        public void testInvalidFileType_Returns415() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "virus.exe", "application/octet-stream", "bad file".getBytes());

                mockMvc.perform(multipart("/api/upload")
                        .file(file))
                        .andExpect(status().isUnsupportedMediaType())
                        .andExpect(content().string(org.hamcrest.Matchers.containsString("Unsupported file format")));
        }

        // ❌ Test: Oversized file (>5MB)
        @Test
        public void testOversizedFile_Returns413() throws Exception {
                byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
                MockMultipartFile file = new MockMultipartFile(
                        "file", "bigfile.jpg", "image/jpeg", largeContent);

                mockMvc.perform(multipart("/api/upload")
                        .file(file))
                        .andExpect(status().isPayloadTooLarge());
        }

        // ❌ Test: No file provided
        @Test
        public void testNoFileProvided_Returns400() throws Exception {
                MockMultipartFile emptyFile = new MockMultipartFile(
                        "file", "", "application/octet-stream", new byte[0]);

                mockMvc.perform(multipart("/api/upload").file(emptyFile))
                        .andExpect(status().isBadRequest())
                        .andExpect(content().string(org.hamcrest.Matchers.containsString("No file selected")));
        }
}
