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
            preprocessed = Preprocesing.preprocessIncreased(imagePath);
        } else {
            preprocessed = Preprocesing.preprocessMinimal(imagePath);
        }

        return ocrEngine.extractTextFromImage(preprocessed);
    }
}
