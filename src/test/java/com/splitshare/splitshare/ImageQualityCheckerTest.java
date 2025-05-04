package com.splitshare.splitshare;

import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.splitshare.splitshare.service.ImageQualityChecker;

public class ImageQualityCheckerTest {
    

    // Threshold for blur detection
    private static final double BLUR_THRESHOLD = 150.0;

    @Test
    public void testClearImageIsNotBlurry() {
        String imagePath = "src/test/resources/clear_receipt.png";
        boolean isBlurry = ImageQualityChecker.isBlurry(imagePath, BLUR_THRESHOLD);
        assertFalse(isBlurry, "Clear image should not be blurry");
    }

    @Test
    public void testBlurryImageIsBlurry() {
        String imagePath = "src/test/resources/blurry_receipt.png";
        double variance = ImageQualityChecker.computeVariance(imagePath);
        
        boolean isBlurry = variance < BLUR_THRESHOLD;
        assertTrue(isBlurry, "Blurry image should be detected as blurry (variance=" + variance + ")");
    }

}
