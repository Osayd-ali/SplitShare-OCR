package com.splitshare.splitshare.service;

import net.sourceforge.tess4j.Tesseract;
import java.awt.image.BufferedImage;

import org.springframework.stereotype.Service;

@Service
public class OcrEngine {
    private Tesseract tesseract;
    public OcrEngine(){
        tesseract = new Tesseract();
        tesseract.setDatapath("/usr/share/tessdata/");  // inside Docker
        tesseract.setLanguage("eng");
        //this helps with weird cases that creates borders into pipe (|) characters
        tesseract.setVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.$&:/+=-, ");  
        
        //there is some weirdness with this in my example version mode 1 worked fine with everything else being the same
        //in this version mode 4 with the same images.
        tesseract.setPageSegMode(6);
    }

    //This method does not do any pre processing just looks an image and gets the text.
    public String extractTextFromImage(BufferedImage image) throws Exception{
        try {
            return tesseract.doOCR(image);
        } catch (Exception e) {
            e.printStackTrace(); // Print to console to help debug
            throw e;
        }
    }
}
