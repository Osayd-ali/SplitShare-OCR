package com.splitshare.splitshare.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.splitshare.splitshare.dto.ReceiptData;

import javax.imageio.ImageIO;


import java.awt.image.BufferedImage;
import java.io.File;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;


/**
 * Controller for handling receipt OCR operations.
 * This class is responsible for:
 * 1. Receiving uploaded receipt images
 * 2. Extracting text using OCR
 * 3. Parsing the raw text to extract structured data
 * 4. Returning the structured data to the client
 */
@RestController
@RequestMapping("/api/receipts")
public class ReceiptOcrController {
    // Logger for tracking operations and debugging
    private static final Logger logger = LoggerFactory.getLogger(ReceiptOcrController.class);
    // The OCR engine used to extract text from images

    private final ReceiptStorageService storageService;
    private final ImageHandlingService imageHandlingService;
    private final ReceiptExtractionHelper extractor;

    /**
     * Constructor that injects the OCR engine dependency
     */
    @Autowired
    public ReceiptOcrController( ReceiptStorageService storageService, ImageHandlingService imageHandlingService, ReceiptExtractionHelper extractor) {

        this.storageService = storageService;
        this.imageHandlingService = imageHandlingService;
        this.extractor = extractor;
    }
    /**
     * API endpoint for extracting data from receipt images.
     * Takes a multipart file upload and processes it using OCR.
     *
     * @param file The uploaded receipt image
     * @return Structured receipt data or an error message
     */

    //this is the endpoint for passing the file name of a file that is already on the server
    @PostMapping("/extract-from-server")
    public ResponseEntity<?> extractReceiptData(
            @RequestParam("fileName") String fileName,
            @RequestParam("userId") Long userId) {
        File file = null;
        try {
            // Load file from the server
            Path filePath = Paths.get("uploads", fileName).toAbsolutePath();
            file = filePath.toFile();

            if (!file.exists()) {
                logger.warn("File not found: {}", filePath);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("The requested file does not exist on the server."));
            }

            // Convert to image
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                logger.error("Failed to read image from saved file");
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Unable to process the saved image."));
            }

            // OCR & parsing logic stays the same
            String rawText = imageHandlingService.handleImage(file.getAbsolutePath());
            ReceiptData parsedData = extractor.parseReceiptText(rawText);
            String receiptId = storageService.storeReceiptText(userId, rawText, parsedData, fileName);

            // Build structured response
            Map<String, Object> response = new HashMap<>();
            response.put("receiptId", receiptId);
            response.put("storeName", parsedData.getStoreName());
            response.put("date", parsedData.getDate());
            response.put("total", parsedData.getTotal());
            response.put("items", parsedData.getItems());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Log the complete stack trace for technical debugging
            logger.error("Error processing receipt image: ", e);
            // Log the failure (but don't store the text since OCR failed)
            storageService.logOcrFailure(
                    userId,
                    fileName,
                    e.getMessage()
            );
            // Return a user-friendly error message
            // The message suggests possible solutions to help the user resolve the issue
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("We couldn't process this receipt image. " +
                            "Please ensure the image is clear and try again, or enter the details manually."));
        }
    }
    
    //this is the rest api for if use uploads file from their device
    @PostMapping("/extract")
    public ResponseEntity<?> extractReceiptData(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        try {
            // Check if the uploaded file is actually an image by examining its MIME type
            // This prevents processing non-image files which would fail in OCR
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                logger.warn("Invalid file type uploaded: {}", contentType);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Please upload a valid image file"));
            }

            // Convert the uploaded file to a BufferedImage for OCR processing
            // ImageIO.read will return null if the file cannot be read as an image
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                logger.error("Failed to read image from uploaded file");
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Unable to process the uploaded image"));
            }


            File tempFile = File.createTempFile("receipt-", ".png");
            file.transferTo(tempFile);
            //uses imageHandlingService to call ocrEngine and control how the image is processed
            String rawText = imageHandlingService.handleImage(tempFile.getAbsolutePath());

            tempFile.delete();


            // This includes store name, date, total, and individual items
            ReceiptData parsedData = extractor.parseReceiptText(rawText);
            // Store the extracted text and receipt data
            String receiptId = storageService.storeReceiptText(
                    userId,
                    rawText,
                    parsedData,
                    file.getOriginalFilename()
            );
            // Log successful extraction for monitoring and debugging
            logger.info("Successfully extracted and stored receipt data from uploaded image");
            // Return response with receipt ID and parsed data
            Map<String, Object> response = new HashMap<>();
            response.put("receiptId", receiptId);
            response.put("storeName", parsedData.getStoreName());
            response.put("date", parsedData.getDate());
            response.put("total", parsedData.getTotal());
            response.put("items", parsedData.getItems());
            // Return the structured data to the client with HTTP 200 OK status
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Log the complete stack trace for technical debugging
            logger.error("Error processing receipt image: ", e);
            // Log the failure (but don't store the text since OCR failed)
            storageService.logOcrFailure(
                    userId,
                    file.getOriginalFilename(),
                    e.getMessage()
            );
            // Return a user-friendly error message
            // The message suggests possible solutions to help the user resolve the issue
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("We couldn't process this receipt image. " +
                            "Please ensure the image is clear and try again, or enter the details manually."));
        }
    }
    /**
     * Retrieve a stored receipt by ID
     */
    @GetMapping("/{userId}/{receiptId}")
    public ResponseEntity<?> getReceipt(
            @PathVariable Long userId,
            @PathVariable String receiptId) {

        String receiptText = storageService.getReceiptText(userId, receiptId);

        if (receiptText == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(receiptText);
    }
    

}