package com.splitshare.splitshare;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;
import com.splitshare.splitshare.service.ImageQualityChecker;

public class ImageQualityCheckerTest {
    

    final static double BLUR_THRESHOLD = 150;

    @Test
    public void testClearImageIsNotBlurry() {
        String imagePath = "src/test/resources/clear_receipt.png";
        boolean isBlurry = ImageQualityChecker.isBlurry(imagePath);
        assertFalse(isBlurry, "Clear image should not be blurry");
    }

    @Test
    public void testBlurryImageIsBlurry() {
        String imagePath = "src/test/resources/blurry_receipt.png";
        boolean isBlurry = ImageQualityChecker.isBlurry(imagePath);
        assertTrue(isBlurry, "Blurry image should be detected as blurry");
    }

    @Test
    public void testThreshHoldUnder() {
        String imagePath = "src/test/resources/blurry_receipt.png";
        double variance = ImageQualityChecker.computeVariance(imagePath);
        
        boolean isBlurry = variance < BLUR_THRESHOLD;
        assertTrue(isBlurry, "Blurry image should be detected as blurry (variance=" + variance + ")");
    }

    @Test
    public void testThreshHoldOver() throws Exception {
        File imageFile = ResourceUtils.getFile("classpath:clear_receipt.png");
        double variance = ImageQualityChecker.computeVariance(imageFile.getAbsolutePath());
        boolean isBlurry = variance < BLUR_THRESHOLD;
        assertFalse(isBlurry, "Clear image should be detected as clear (variance=" + variance + ")");
    }

}
