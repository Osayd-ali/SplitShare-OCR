package com.splitshare.splitshare.service;
import org.bytedeco.opencv.opencv_core.*;
// import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;

public class ImageQualityChecker {
   
    public static double computeVariance(String imagePath) {
        Mat image = opencv_imgcodecs.imread(imagePath, opencv_imgcodecs.IMREAD_GRAYSCALE);
        if (image.empty()) {
            throw new IllegalArgumentException("Could not load image: " + imagePath);
        }

        Mat laplacian = new Mat();
        opencv_imgproc.Laplacian(image, laplacian, opencv_core.CV_64F);

        Mat mean = new Mat();
        Mat stddev = new Mat();
        opencv_core.meanStdDev(laplacian, mean, stddev);

        double stddevVal = stddev.createIndexer().getDouble(0);
        return Math.pow(stddevVal, 2);
    }

    
    public static boolean isBlurry(String imagePath, double threshold) {
        double variance = computeVariance(imagePath);
        // System.out.printf("Image blur score (variance): %.2f\n", variance);
        return variance < threshold;
    }
}
