package com.splitshare.splitshare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitshare.splitshare.dto.ReceiptData;
import com.splitshare.splitshare.dto.ReceiptItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import java.io.FileWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Service for storing OCR text data from receipts
 * This handles the persistence of extracted receipt text
 */
@Service
public class ReceiptStorageService {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptStorageService.class);

    @Value("${receipt.storage.path:/tmp/receipts}")
    private String storageBasePath;

    /**
     * Stores the extracted OCR text and receipt metadata
     * @param userId The ID of the user who uploaded the receipt
     * @param rawText The raw text extracted via OCR
     * @param receiptData The parsed receipt data
     * @param originalFilename The original filename of the uploaded receipt image
     * @return The unique ID assigned to this receipt
     */
    public String storeReceiptText(Long userId, String rawText, ReceiptData receiptData, String originalFilename) {
        String receiptId = UUID.randomUUID().toString();
        try {
            // Create user directory if it doesn't exist
            Path userDir = Paths.get(storageBasePath, userId.toString());
            if (!Files.exists(userDir)) {
                Files.createDirectories(userDir);
            }

            // Create receipt data file
            Path receiptFile = userDir.resolve(receiptId + ".txt");

            // Write receipt data to file
            try (FileWriter writer = new FileWriter(receiptFile.toFile())) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                writer.write("Receipt ID: " + receiptId + "\n");
                writer.write("User ID: " + userId + "\n");
                writer.write("Original Filename: " + originalFilename + "\n");
                writer.write("Processed: " + dateFormat.format(new Date()) + "\n\n");

                writer.write("Store: " + receiptData.getStoreName() + "\n");
                if (receiptData.getDate() != null) {
                    writer.write("Date: " + dateFormat.format(receiptData.getDate()) + "\n");
                }
                writer.write("Total: $" + String.format("%.2f", receiptData.getTotal()) + "\n\n");

                writer.write("Items:\n");
                if (receiptData.getItems() != null) {
                    for (ReceiptItem item : receiptData.getItems()) {
                        writer.write("- " + item.getName() + ": $" + String.format("%.2f", item.getPrice()) + "\n");
                    }
                }

                writer.write("\n---- RAW OCR TEXT ----\n\n");
                writer.write(rawText);
            }

            ObjectMapper mapper = new ObjectMapper();
            Path jsonFile = userDir.resolve(receiptId + ".json");
            mapper.writeValue(jsonFile.toFile(), receiptData);

            logger.info("Successfully stored receipt text for user {} with ID {}", userId, receiptId);
            return receiptId;

        } catch (Exception e) {
            logger.error("Failed to store receipt text for user {}", userId, e);
            throw new RuntimeException("Failed to store receipt data", e);
        }
    }
    /**
     * Records a failed OCR attempt
     * @param userId The ID of the user who uploaded the receipt
     * @param originalFilename The original filename of the uploaded receipt image
     * @param errorMessage The error message describing why OCR failed
     */
    public void logOcrFailure(Long userId, String originalFilename, String errorMessage) {
        try {
            // Create user directory if it doesn't exist
            Path userDir = Paths.get(storageBasePath, userId.toString());
            if (!Files.exists(userDir)) {
                Files.createDirectories(userDir);
            }
            // Create failure log file with timestamp
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = dateFormat.format(new Date());
            Path failureFile = userDir.resolve("ocr_failure_" + timestamp + ".log");
            // Write failure details to file
            try (FileWriter writer = new FileWriter(failureFile.toFile())) {
                writer.write("OCR Failure Log\n");
                writer.write("User ID: " + userId + "\n");
                writer.write("Original Filename: " + originalFilename + "\n");
                writer.write("Timestamp: " + new Date() + "\n");
                writer.write("Error: " + errorMessage + "\n");
            }
            logger.info("Logged OCR failure for user {} with file {}", userId, originalFilename);
        } catch (Exception e) {
            logger.error("Failed to log OCR failure for user {}", userId, e);
        }
    }
    /**
     * Retrieves the stored receipt text
     * @param userId The ID of the user
     * @param receiptId The unique ID of the receipt
     * @return The content of the receipt file, or null if not found
     */
    public String getReceiptText(Long userId, String receiptId) {
        try {
            Path receiptFile = Paths.get(storageBasePath, userId.toString(), receiptId + ".txt");

            if (Files.exists(receiptFile)) {
                return new String(Files.readAllBytes(receiptFile));
            } else {
                logger.warn("Receipt file not found for user {} with ID {}", userId, receiptId);
                return null;
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve receipt text for user {} with ID {}", userId, receiptId, e);
            return null;
        }
    }

    
    public ReceiptData getReceiptById(Long userId, String receiptId) {
        try {
            Path receiptJsonFile = Paths.get(storageBasePath, userId.toString(), receiptId + ".json");

            if (!Files.exists(receiptJsonFile)) {
                logger.warn("JSON receipt file not found for user {} with ID {}", userId, receiptId);
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(receiptJsonFile.toFile(), ReceiptData.class);

        } catch (Exception e) {
            logger.error("Failed to load structured receipt for user {} with ID {}", userId, receiptId, e);
            return null;
        }
    }
    
    public void updateReceipt(Long userId, String receiptId, ReceiptData updatedReceiptData) {
        try {
            Path userDir = Paths.get(storageBasePath, userId.toString());
            if (!Files.exists(userDir)) {
                Files.createDirectories(userDir);
            }

            Path receiptJsonFile = userDir.resolve(receiptId + ".json");

            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(receiptJsonFile.toFile(), updatedReceiptData);

            logger.info("Updated structured receipt for user {} with ID {}", userId, receiptId);
        } catch (Exception e) {
            logger.error("Failed to update structured receipt for user {} with ID {}", userId, receiptId, e);
            throw new RuntimeException("Failed to update receipt data", e);
        }
    }


}