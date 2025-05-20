package com.splitshare.splitshare.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.splitshare.splitshare.dto.ReceiptData;
import com.splitshare.splitshare.dto.ReceiptItem;

@Service
public class ReceiptExtractionHelper {
    private static final Logger logger = LoggerFactory.getLogger(ReceiptExtractionHelper.class);
        // Pattern 1: Qty + Item Name + Price
    private final static String QTY_NAME_PRICE = "(?i)(\\d+)\\s+([A-Za-z &]+?)\\s+\\$?([0-9]{1,3}\\.\\d{2})\\b";

    // Pattern 2: Just Item Name + Price
    private final static String NAME_PRICE = "(?i)([A-Za-z &]+?)\\s+\\$?([0-9]{1,3}\\.\\d{2})\\b";
    /**
     * Main method for parsing raw OCR text into structured receipt data.
     * Coordinates the extraction of different receipt components.
     *
     * @param rawText The raw text extracted by OCR
     * @return A ReceiptData object containing structured information
     */
    public ReceiptData parseReceiptText(String rawText) {
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

        receiptData.setSubtotal(extractSubtotal(rawText));
        receiptData.setTax(extractTax(rawText));
        receiptData.setTip(extractTip(rawText));

        return receiptData;
    }

    private double extractSubtotal(String text) {
        Pattern pattern = Pattern.compile("(?i)sub\\s*total[:\\s]*[€$]?([0-9]+\\.[0-9]{2})");
        Matcher matcher = pattern.matcher(text);
        
        double subtotal = 0.0;
        while (matcher.find()) {
            try {
                subtotal = Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Could not parse subtotal from: {}", matcher.group(1));
            }
        }
        return subtotal;
    }

    private double extractTax(String text) {
        Pattern pattern = Pattern.compile("(?i)(sales\\s*)?tax[:\\s]*[€$]?([0-9]+\\.[0-9]{2})");
        Matcher matcher = pattern.matcher(text);

        double totalTax = 0.0;
        while (matcher.find()) {
            try {
                totalTax += Double.parseDouble(matcher.group(2));
            } catch (NumberFormatException e) {
                logger.warn("Could not parse tax from: {}", matcher.group(2));
            }
        }
        return totalTax;
    }
    
    private double extractTip(String text) {
        Pattern pattern = Pattern.compile("(?i)(tip|gratuity|t\\s*1\\s*p)[:\\s]*\\$?([0-9]+\\.[0-9]{2})");
        return extractAmountFromText(pattern, text, 2);
    }

    private double extractAmountFromText(Pattern pattern, String text, int group) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(group));
            } catch (NumberFormatException e) {
                logger.warn("Could not parse amount from: {}", matcher.group(group));
            }
        }
        return 0.0;
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
        Pattern totalPattern = Pattern.compile("(?i)total\\s*[:€$]?\\s*\\$?([0-9]+\\.[0-9]{2})");

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

        double lastMatch = 0.0;
        while (matcher.find()) {
            try {
                lastMatch = Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse total amount: {}", matcher.group(1));
            }
        }
        return lastMatch;
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
            if (line.isEmpty() || isDateLine(line) || isNonItemLine(line)) continue;

            
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
    //helper method for extractItems
    private boolean isNonItemLine(String line) {
        String lower = line.toLowerCase();
        return lower.matches(".*(sub\\s*total|tax|sales\\s*tax|sales[-\\s]*tax|tip|gratuity|total).*");
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
}
