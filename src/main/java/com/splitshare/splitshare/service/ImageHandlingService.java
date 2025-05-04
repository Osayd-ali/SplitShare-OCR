package com.splitshare.splitshare.service;

import java.io.File;

import org.springframework.stereotype.Service;
import net.sourceforge.tess4j.Tesseract;


@Service
public class ImageHandlingService {
    private Tesseract tesseract;
    public ImageHandlingService(){
        tesseract = new Tesseract();
    }

    //This method does not do any pre processing just looks an image and gets the text.
    public String extractTextFromImage(File file) throws Exception{
        

        tesseract.setDatapath("/usr/share/tessdata/");  // inside Docker
        tesseract.setLanguage("eng");

        //there is some weirdness with this in my example version mode 1 worked fine with everything else being the same
        //in this version mode 4 with the same images.
        tesseract.setPageSegMode(4); 
        // tesseract.setVariable("page_seperator", "\n");

        String result = tesseract.doOCR(file);
        System.out.println(result);
        file.delete();

        
        return result;
    }
  
}
