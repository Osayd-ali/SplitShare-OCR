package com.splitshare.splitshare;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import com.splitshare.splitshare.service.ImageHandlingService;

public class ImageHandlingTest {
    private ImageHandlingService imageHandlingService;

    @BeforeEach
    void setUp() {
        imageHandlingService = new ImageHandlingService();
    }

    @Test
    void testReceipt1Extraction() throws Exception {
        File imageFile = ResourceUtils.getFile("classpath:receipt1.png");
        String result = imageHandlingService.extractTextFromImage(imageFile);

        assertNotNull(result);
        assertTrue(result.contains("COFFEE") || result.contains("Coffee"));
        assertTrue(result.contains("TOTAL") || result.contains("Total"));
    }
}
