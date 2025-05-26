package com.splitshare.splitshare.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.splitshare.splitshare.dto.ReceiptData;
import com.splitshare.splitshare.dto.ReceiptItem;

@Service
public class ItemSplitService {
    public Map<Integer, Double> calculateUserOwedAmounts(ReceiptData receipt) {
        Map<Integer, Double> userOwedMap = new HashMap<>();

        for (ReceiptItem item : receipt.getItems()) {
            List<Integer> assignedUsers = item.getAssignedUser();
            double itemTotal = item.getPrice() * item.getQuantity();

            if (assignedUsers == null || assignedUsers.isEmpty()) {
                assignedUsers = List.of(1);
            }

            int numUsers = assignedUsers.size();
            double splitAmount = itemTotal / numUsers;

            for (Integer userId : assignedUsers) {
                double previous = userOwedMap.getOrDefault(userId, 0.0);
                userOwedMap.put(userId, previous + splitAmount);
            }
        }

        // Round all values to 2 decimal places before returning
        userOwedMap.replaceAll((k, v) -> Math.round(v * 100.0) / 100.0);

        return userOwedMap;
    }
}
