package com.splitshare.splitshare.service;

import java.io.File;

import net.sourceforge.tess4j.Tesseract;

public class ImageHandlingService {
    private Tesseract tesseract;
    public ImageHandlingService(){
        tesseract = new Tesseract();
    }

    //This method does not do any pre processing just looks an image and gets the text.
    public String extractTextFromImage(File file) throws Exception{
        tesseract.setDatapath("/usr/share/tessdata/"); // inside Docker
        tesseract.setLanguage("eng");

        tesseract.setPageSegMode(1);
        tesseract.setVariable("page_seperator", "\n");

        String result = tesseract.doOCR(file);
        System.out.println(result);

        return result;
    }
}
