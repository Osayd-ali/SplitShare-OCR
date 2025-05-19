package com.splitshare.splitshare.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final OcrEngine ocrEngine;
    private final ReceiptStorageService storageService;
    private final ImageHandlingService imageHandlingService;
    // Pattern 1: Qty + Item Name + Price
    private final static String QTY_NAME_PRICE = "(?i)(\\d+)\\s+([A-Za-z &]+?)\\s+\\$?([0-9]{1,3}\\.\\d{2})\\b";

    // Pattern 2: Just Item Name + Price
    private final static String NAME_PRICE = "(?i)([A-Za-z &]+?)\\s+\\$?([0-9]{1,3}\\.\\d{2})\\b";
    /**
     * Constructor that injects the OCR engine dependency
     */
    @Autowired
    public ReceiptOcrController(OcrEngine ocrEngine, ReceiptStorageService storageService, ImageHandlingService imageHandlingService) {
        this.ocrEngine = ocrEngine;
        this.storageService = storageService;
        this.imageHandlingService = imageHandlingService;
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
            ReceiptData parsedData = parseReceiptText(rawText);
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
            if (!file.getContentType().startsWith("image/")) {
                logger.warn("Invalid file type uploaded: {}", file.getContentType());
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

            // Use the OCR engine to extract raw text from the image
            // This calls the provided OcrEngine.extractTextFromImage() method
            File tempFile = File.createTempFile("receipt-", ".png");
            file.transferTo(tempFile);
            String rawText = imageHandlingService.handleImage(tempFile.getAbsolutePath());
            // System.out.println("OCR Text:\n" + rawText);
            tempFile.delete();
            // String rawText = ocrEngine.extractTextFromImage(image);
            // After extracting raw text, parse it into structured data
            // This includes store name, date, total, and individual items
            ReceiptData parsedData = parseReceiptText(rawText);
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
    /**
     * Main method for parsing raw OCR text into structured receipt data.
     * Coordinates the extraction of different receipt components.
     *
     * @param rawText The raw text extracted by OCR
     * @return A ReceiptData object containing structured information
     */
    private ReceiptData parseReceiptText(String rawText) {
        ReceiptData receiptData = new ReceiptData();

        // Split the text into lines for easier processing
        // Different receipt components are often on different lines
        String[] lines = rawText.split("\n");

        // Extract store name (usually found in the first few lines of a receipt)
        receiptData.setStoreName(extractStoreName(lines));

        // Extract transaction date (using various date formats commonly found on receipts)
        receiptData.setDate(extractDate(rawText));

        // Extract the total amount paid (usually contains "total" keyword)
        receiptData.setTotal(extractTotal(rawText));

        // Extract individual items purchased (the most complex part)
        receiptData.setItems(extractItems(lines));

        return receiptData;
    }

    /**
     * Extracts the store name from the receipt text.
     * Store names are typically found in the header, within the first few lines.
     *
     * @param lines Array of text lines from the receipt
     * @return The extracted store name or "Unknown Store" if not found
     */
    private String extractStoreName(String[] lines) {
        if (lines.length == 0) return "Unknown Store";

        // Strategy: Check the first few lines (typically header contains store info)
        // Skip lines that look like dates or just prices
        // In production, this could be enhanced with a database of known store names
        for (int i = 0; i < Math.min(5, lines.length); i++) {
            String line = lines[i].trim();
            // Skip empty lines, date lines, or lines that just contain a price
             if (!isDateLine(line) &&
                !isPriceLine(line) &&
                !line.toLowerCase().contains("total") &&
                !line.toLowerCase().contains("tax") &&
                !line.matches(".*\\d+(\\.\\d{2})?.*")) {
                return line;
            }
        }

        // If no suitable line found, return default value
        return "Unknown Store";
    }

    /**
     * Extracts the transaction date from the receipt text.
     * Looks for common date formats found on receipts.
     *
     * @param rawText The complete raw text from the receipt
     * @return The parsed date or current date if no date found
     */
    private Date extractDate(String rawText) {
        // Define common date patterns used in receipts
        // The patterns account for variations in separators and digit counts
        List<Pattern> datePatterns = Arrays.asList(
                // MM/DD/YYYY format (e.g., 05/12/2023)
                Pattern.compile("(0?[1-9]|1[0-2])/(0?[1-9]|[12][0-9]|3[01])/([0-9]{4})"),

                // DD-MM-YYYY format (e.g., 12-05-2023)
                Pattern.compile("(0?[1-9]|[12][0-9]|3[01])-(0?[1-9]|1[0-2])-([0-9]{4})"),

                // DD/MM/YYYY format (e.g., 12/05/2023)
                Pattern.compile("(0?[1-9]|[12][0-9]|3[01])/(0?[1-9]|1[0-2])/([0-9]{4})"),

                // YYYY-MM-DD format (e.g., 2023-05-12)
                Pattern.compile("([0-9]{4})-(0?[1-9]|1[0-2])-(0?[1-9]|[12][0-9]|3[01])")
        );

        // Define date format parsers corresponding to each pattern above
        List<SimpleDateFormat> dateFormats = Arrays.asList(
                new SimpleDateFormat("MM/dd/yyyy"),
                new SimpleDateFormat("dd-MM-yyyy"),
                new SimpleDateFormat("dd/MM/yyyy"),
                new SimpleDateFormat("yyyy-MM-dd")
        );
        // Try each pattern in sequence
        for (int i = 0; i < datePatterns.size(); i++) {
            Matcher matcher = datePatterns.get(i).matcher(rawText);
            if (matcher.find()) {
                try {
                    // If pattern matches, attempt to parse it with the corresponding format
                    return dateFormats.get(i).parse(matcher.group());
                } catch (ParseException e) {
                    // Log warning if date format is recognized but parsing fails
                    logger.warn("Found date pattern but couldn't parse date: {}", matcher.group());
                }
            }
        }
        // Default to current date if no valid date found in the receipt
        // In a production app, you might want to require manual entry instead
        return new Date();
    }
    /**
     * Extracts the total amount from the receipt text.
     * Looks for "total" keyword followed by a numeric value.
     * Falls back to searching for the largest dollar amount if "total" pattern fails.
     *
     * @param rawText The complete raw text from the receipt
     * @return The extracted total amount as a double
     */
    private double extractTotal(String rawText) {
        // First strategy: Look for variations of "total" followed by a price
        // This handles formats like "TOTAL $XX.XX", "TOTAL: $XX.XX", etc.
        // The (?i) makes the pattern case-insensitive
        Pattern totalPattern = Pattern.compile("(?i)total\\s*[:$]?\\s*\\$?([0-9]+\\.[0-9]{2})");

        Matcher matcher = totalPattern.matcher(rawText);

        if (matcher.find()) {
            try {
                // Extract and parse the numeric part
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Found total pattern but couldn't parse amount: {}", matcher.group(1));
            }
        }
        // Second strategy (fallback): Find the largest dollar amount
        // This assumes the total is likely the largest monetary value on the receipt
        // This is less reliable but can work when the "total" keyword is not recognized
        Pattern pricePattern = Pattern.compile("\\$?([0-9]+\\.[0-9]{2})");
        matcher = pricePattern.matcher(rawText);

        double largestAmount = 0.0;
        while (matcher.find()) {
            try {
                double amount = Double.parseDouble(matcher.group(1));
                if (amount > largestAmount) {
                    largestAmount = amount;
                }
            } catch (NumberFormatException e) {
                // Skip if not parseable
            }
        }
        return largestAmount;
    }
    /**
     * Extracts individual purchased items from the receipt text.
     * This is the most complex extraction due to variations in receipt formats.
     *
     * @param lines Array of text lines from the receipt
     * @return List of ReceiptItem objects representing individual purchases
     */
    private List<ReceiptItem> extractItems(String[] lines) {
        List<ReceiptItem> items = new ArrayList<>();

        
        // Pattern 1: Qty + Item Name + Price
        Pattern qtyPattern = Pattern.compile(QTY_NAME_PRICE);


        // Pattern 2: Just Item Name + Price
        Pattern namePricePattern = Pattern.compile(NAME_PRICE);

        // Skip the header and footer lines
        // Items are typically in the middle section of a receipt
        // This is a heuristic approach - real receipts vary greatly
        // int startLine = Math.min(5, lines.length / 4);
        // int endLine = Math.max(lines.length - 5, lines.length * 3 / 4);

        // for (int i = startLine; i < endLine; i++) {
        //     String line = lines[i].trim();
        for (String line : lines) {
            line.trim();
            // Skip lines that are likely not items
            // This includes empty lines, subtotal/total lines, and date lines
            if (line.isEmpty()) continue;
            if (line.toLowerCase().contains("subtotal") ||
                line.toLowerCase().contains("total") || 
                line.toLowerCase().contains("tax") ||
                isDateLine(line)){
                continue;
            }

            
            Matcher matcher = qtyPattern.matcher(line);
            // qty & name & price
            if (matcher.find()) {
                String itemName = matcher.group(2).trim();
                try {
                    int quantity = Integer.parseInt(matcher.group(1));
                    double price = Double.parseDouble(matcher.group(3));
                    if (!itemName.isEmpty() && price > 0 && price < 1000 && quantity > 0) {
                        items.add(new ReceiptItem(itemName, price, quantity));
                    }
                    continue;
                } catch (NumberFormatException e) { /* fallback */ }
            }
            
            //Name & Price 
            matcher = namePricePattern.matcher(line);
            if (matcher.find()) {
                String itemName = matcher.group(1).trim();
                try {
                    // Extract the item name and price
                    
                    double price = Double.parseDouble(matcher.group(2));

                    // Basic validation to filter out non-item entries
                    // Items should have a non-empty name and reasonable price
                    if (!itemName.isEmpty() && price > 0 && price < 1000) {
                        items.add(new ReceiptItem(itemName, price, 1));
                    }
                } catch (NumberFormatException e) {
                    // Skip this line if price parsing fails
                }
            }
        }

        return items;
    }

    /**
     * Utility method to check if a line contains a date.
     * Used to filter out date lines when looking for store names and items.
     *
     * @param line A single line of text from the receipt
     * @return true if the line appears to contain a date
     */
    private boolean isDateLine(String line) {
        // Basic regex to detect common date formats with digits and separators
        // This detects patterns like MM/DD/YYYY, DD-MM-YYYY, etc.
        return line.matches(".*\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}.*");
    }

    /**
     * Utility method to check if a line contains only a price.
     * Used to filter out price-only lines when looking for store names.
     *
     * @param line A single line of text from the receipt
     * @return true if the line appears to contain only a price
     */
    private boolean isPriceLine(String line) {
        // Regex to detect lines that contain just a dollar amount
        // This matches formats like "$12.34" or "12.34"
        return line.matches("\\s*\\$?\\d+\\.\\d{2}\\s*");
    }

    /**
     * Data class for structured receipt information.
     * Contains all the extracted components of a receipt.
     */
    public static class ReceiptData {
        private String storeName;
        private Date date;
        private double total;
        private List<ReceiptItem> items;

        /**
         * Default constructor initializes an empty items list
         */
        public ReceiptData() {
            this.items = new ArrayList<>();
        }

        // Getters and setters for all fields

        public String getStoreName() {
            return storeName;
        }

        public void setStoreName(String storeName) {
            this.storeName = storeName;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public double getTotal() {
            return total;
        }

        public void setTotal(double total) {
            this.total = total;
        }

        public List<ReceiptItem> getItems() {
            return items;
        }

        public void setItems(List<ReceiptItem> items) {
            this.items = items;
        }
    }

    /**
     * Data class for individual receipt items.
     * Represents a single purchased item with its name and price.
     */
    public static class ReceiptItem {
        private String name;
        private double price;
        private int quantity;

        /**
         * Constructor for creating a new receipt item
         *
         * @param name The item description
         * @param price The item price
         * @param quantity The item quantity
         * 
         */
        public ReceiptItem(String name, double price,int quantity) {
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        // Getters and setters

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public int getQuantity() { 
            return quantity; 
        }

        public void setQuantity(int quantity) { 
            this.quantity = quantity; 
        }
    }

    /**
     * Simple data class for error responses.
     * Used to return user-friendly error messages in a structured format.
     */
    public static class ErrorResponse {
        private String message;

        /**
         * Constructor for creating a new error response
         *
         * @param message The error message to display to the user
         */
        public ErrorResponse(String message) {
            this.message = message;
        }

        /**
         * Getter for the error message
         */
        public String getMessage() {
            return message;
        }
    }
}