package com.splitshare.splitshare;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import com.splitshare.splitshare.service.ImageHandlingService;
import com.splitshare.splitshare.service.OcrEngine;
import com.splitshare.splitshare.service.Preprocesing;
import java.awt.image.BufferedImage;

public class ImageHandlingTest {
    private OcrEngine ocr;
    private ImageHandlingService imageHandlingService;
    @BeforeEach
    void setUp() {
        ocr = new OcrEngine();
        imageHandlingService = new ImageHandlingService();
    }

    @Test
    void testExtractionMinimal() throws Exception {
        File imageFile = ResourceUtils.getFile("classpath:receipt1.png");
        BufferedImage preprocessed = Preprocesing.preprocessMinimal(imageFile.getAbsolutePath());
        String result = ocr.extractTextFromImage(preprocessed);

        assertNotNull(result);
        assertTrue(result.contains("COFFEE") || result.contains("Coffee"));
        assertTrue(result.contains("TOTAL") || result.contains("Total"));
    }


    @Test
    void testOcrEngineMinimal() throws Exception {
        File imageFile = ResourceUtils.getFile("classpath:receipt1.png");
        

        String ocrResult = imageHandlingService.handleImage(imageFile.getAbsolutePath());

        assertNotNull(ocrResult);
        assertTrue(ocrResult.toLowerCase().contains("coffee"), "Expected to find 'coffee'");
        assertTrue(ocrResult.toLowerCase().contains("total"), "Expected to find 'total'");
    }

    @Test
    void testOcrEngineIncreased() throws Exception {
        File imageFile = ResourceUtils.getFile("classpath:Blurry_1.png");
        

        String ocrResult = imageHandlingService.handleImage(imageFile.getAbsolutePath());

        assertNotNull(ocrResult);
        assertTrue(ocrResult.toLowerCase().contains("coffee"), "Expected to find 'coffee'");
        assertTrue(ocrResult.toLowerCase().contains("total"), "Expected to find 'total'");
    }

    
}
