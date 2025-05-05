package com.splitshare.splitshare.service;


import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;



import java.awt.image.BufferedImage;


public class Preprocesing {

    public static BufferedImage preprocessIncreased(String inputPath) {
        Mat image = opencv_imgcodecs.imread(inputPath, opencv_imgcodecs.IMREAD_COLOR);
        if (image.empty()) throw new IllegalArgumentException("Could not read image: " + inputPath);

        // Grayscale only
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);

        // Light blur
        Mat blurred = new Mat();
        opencv_imgproc.GaussianBlur(gray, blurred, new Size(3, 3), 0);

        // Contrast analysis
        Mat mean = new Mat();
        Mat stddev = new Mat();
        opencv_core.meanStdDev(blurred, mean, stddev);
        double contrast = stddev.createIndexer().getDouble(0);

        // Threshold on low contrast
        if (contrast < 25) {
            Mat thresholded = new Mat();
            opencv_imgproc.adaptiveThreshold(blurred, thresholded, 255, opencv_imgproc.ADAPTIVE_THRESH_MEAN_C, opencv_imgproc.THRESH_BINARY, 15, 5);
            return matToBufferedImage(thresholded);
        }

        return matToBufferedImage(blurred);
    }

    public static BufferedImage preprocessMinimal(String inputPath) {
        Mat image = opencv_imgcodecs.imread(inputPath, opencv_imgcodecs.IMREAD_COLOR);
        if (image.empty()) throw new IllegalArgumentException("Could not read image: " + inputPath);
    
        // Convert to grayscale
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);
    
        return matToBufferedImage(gray);
    }
    

    private static BufferedImage matToBufferedImage(Mat mat) {
        try (
            OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();
            Java2DFrameConverter toBuffered = new Java2DFrameConverter()
        ) {
            return toBuffered.convert(toMat.convert(mat));
        }
    }

}
