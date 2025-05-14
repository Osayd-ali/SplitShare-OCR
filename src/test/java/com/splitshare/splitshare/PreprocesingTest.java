package com.splitshare.splitshare;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;
import java.awt.image.BufferedImage;
import java.io.File;
import com.splitshare.splitshare.service.Preprocesing;

public class PreprocesingTest {
@Test
    public void testPreprocessImageProducesValidBufferedImage() throws Exception {
        
        File imageFile = ResourceUtils.getFile("classpath:clear_receipt.png");

        BufferedImage result = Preprocesing.preprocessIncreased(imageFile.getAbsolutePath());

        assertNotNull(result, "Preprocessed image should not be null");
        assertTrue(result.getWidth() > 0 && result.getHeight() > 0, "Image dimensions must be positive");

        BufferedImage result2 = Preprocesing.preprocessMinimal(imageFile.getAbsolutePath());

        assertNotNull(result2, "Preprocessed image should not be null");
        assertTrue(result2.getWidth() > 0 && result.getHeight() > 0, "Image dimensions must be positive");
    }
}
