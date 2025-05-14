package com.splitshare.splitshare.service;


import java.awt.image.BufferedImage;

import org.springframework.stereotype.Service;


@Service
public class ImageHandlingService {
    private final OcrEngine ocrEngine;
  
    public ImageHandlingService() {
        this.ocrEngine = new OcrEngine();
    }

    public String handleImage(String imagePath) throws Exception {
        // Check if blurry
        boolean blurry = ImageQualityChecker.isBlurry(imagePath);

        //Preprocess accordingly
        BufferedImage preprocessed;
        if (blurry) {
            System.out.println("Image is blurry, using increased preprocessing.");
            preprocessed = Preprocesing.preprocessIncreased(imagePath);
        } else {
            System.out.println("Image is not blurry, using minimal preprocessing.");
            preprocessed = Preprocesing.preprocessMinimal(imagePath);
        }

        return ocrEngine.extractTextFromImage(preprocessed);
    }
}
