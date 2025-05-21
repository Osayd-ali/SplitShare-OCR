package com.splitshare.splitshare.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.splitshare.splitshare.dto.ReceiptData;
import com.splitshare.splitshare.dto.ReceiptItem;
import com.splitshare.splitshare.service.ErrorResponse;
import com.splitshare.splitshare.service.ItemSplitService;
import com.splitshare.splitshare.service.ReceiptStorageService;

@RestController
@RequestMapping("/api/split")
public class ItemSplitContoller {
    private final ItemSplitService itemSplitService;
    private final ReceiptStorageService storageService;
    @Autowired
    public ItemSplitContoller(ItemSplitService itemSplitService, ReceiptStorageService storageService) {
        this.storageService = storageService;
        this.itemSplitService = itemSplitService;
    }

    @PostMapping("/calculate-split")
    public ResponseEntity<Map<Integer, Double>> calculateSplit(
            @RequestParam("userId") Long userId,
            @RequestParam("receiptId") String receiptId) {

        ReceiptData receipt = storageService.getReceiptById(userId, receiptId);
        if (receipt == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Map<Integer, Double> result = itemSplitService.calculateUserOwedAmounts(receipt);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/assign-users")
    public ResponseEntity<?> assignUsersToItem(
            @RequestParam("userId") Long userId,
            @RequestParam("receiptId") String receiptId,
            @RequestParam("itemIndex") int itemIndex,
            @RequestBody List<Integer> userIds) {

        // Load structured receipt data
        ReceiptData receiptData = storageService.getReceiptById(userId, receiptId);
        if (receiptData == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Receipt not found"));
        }

        List<ReceiptItem> items = receiptData.getItems();
        if (itemIndex < 0 || itemIndex >= items.size()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid item index"));
        }

        // Assign users to item
        ReceiptItem item = items.get(itemIndex);
        item.setAssignedUsers(userIds);

        // Persist updated data as JSON
        storageService.updateReceipt(userId, receiptId, receiptData);

        return ResponseEntity.ok("Users assigned to item successfully");
    }
}
