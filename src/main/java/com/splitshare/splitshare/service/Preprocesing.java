package com.splitshare.splitshare.service;


import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.global.opencv_core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
        Mat result = blurred;
        if (contrast < 25) {
            Mat thresholded = new Mat();
            opencv_imgproc.adaptiveThreshold(blurred, thresholded, 255, opencv_imgproc.ADAPTIVE_THRESH_MEAN_C, opencv_imgproc.THRESH_BINARY, 15, 5);
            result = thresholded;
        }
        BufferedImage buffered = matToBufferedImage(result);
        BufferedImage padded = addWhiteSpace(buffered, 20);
        if (image.cols() < 1000 && image.rows() < 1000) {
            return scaleImage(padded, 2);
        }
        return padded;
    }

    public static BufferedImage preprocessMinimal(String inputPath) {
        Mat image = opencv_imgcodecs.imread(inputPath, opencv_imgcodecs.IMREAD_COLOR);
        if (image.empty()) throw new IllegalArgumentException("Could not read image: " + inputPath);
        // Convert to grayscale
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);
        BufferedImage buffered = matToBufferedImage(gray);
        // Scale ONLY if image is small
        if (image.cols() < 1000 && image.rows() < 1000) {
            BufferedImage padded = addWhiteSpace(buffered, 20);
            System.out.println("Image is small, scaling up.");
            return scaleImage(padded, 2);
        }
        return buffered;
    }
    

    private static BufferedImage matToBufferedImage(Mat mat) {
        try (
            OpenCVFrameConverter.ToMat toMat = new OpenCVFrameConverter.ToMat();
            Java2DFrameConverter toBuffered = new Java2DFrameConverter()
        ) {
            return toBuffered.convert(toMat.convert(mat));
        }
    }

    // Method to scale up the image
    private static BufferedImage scaleImage(BufferedImage originalImage, int scaleFactor) {
        int scaledWidth = originalImage.getWidth() * scaleFactor;
        int scaledHeight = originalImage.getHeight() * scaleFactor;
        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, originalImage.getType());
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();
        return scaledImage;
    }

    // Method to add white space around the image
    private static BufferedImage addWhiteSpace(BufferedImage originalImage, int padding) {
        int paddedWidth = originalImage.getWidth() + 2 * padding;
        int paddedHeight = originalImage.getHeight() + 2 * padding;
        BufferedImage paddedImage = new BufferedImage(paddedWidth, paddedHeight, originalImage.getType());
        Graphics2D g2d = paddedImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, paddedWidth, paddedHeight);
        g2d.drawImage(originalImage, padding, padding, null);
        g2d.dispose();
        return paddedImage;
    }

}
