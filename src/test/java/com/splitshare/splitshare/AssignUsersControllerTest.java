package com.splitshare.splitshare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitshare.splitshare.controller.ItemSplitContoller;
import com.splitshare.splitshare.controller.ReceiptOcrController;
import com.splitshare.splitshare.dto.ReceiptData;
import com.splitshare.splitshare.dto.ReceiptItem;
import com.splitshare.splitshare.service.ItemSplitService;
import com.splitshare.splitshare.service.ReceiptStorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = ItemSplitContoller.class)
@Import(AssignUsersControllerTest.TestConfig.class)
public class AssignUsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReceiptStorageService storageService;

    private ItemSplitService itemSplitService;

    private ReceiptData testReceipt;
    private final Long userId = 1L;
    private final String receiptId = "test-receipt";

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ReceiptStorageService storageService() {
            return Mockito.mock(ReceiptStorageService.class);
        }

        @Bean
        public ItemSplitService itemSplitService() {
            return Mockito.mock(ItemSplitService.class);
        }
    }

    @BeforeEach
    void setUp() {
        itemSplitService = new ItemSplitService();
        testReceipt = new ReceiptData();
        List<ReceiptItem> items = new ArrayList<>();
        items.add(new ReceiptItem("Burger", 8.99, 1));
        testReceipt.setItems(items);
    }

    @Test
    void testAssignUsersToItem_Success() throws Exception {
        List<Integer> assignedUsers = List.of(2, 3);

        when(storageService.getReceiptById(userId, receiptId)).thenReturn(testReceipt);

        mockMvc.perform(post("/api/split/assign-users")
                        .param("userId", userId.toString())
                        .param("receiptId", receiptId)
                        .param("itemIndex", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignedUsers)))
                .andExpect(status().isOk());

        ReceiptItem item = testReceipt.getItems().get(0);
        assert item.getAssignedUser().equals(assignedUsers);

        verify(storageService).updateReceipt(eq(userId), eq(receiptId), eq(testReceipt));
    }

    @Test
    void testAssignUsersToItem_InvalidItemIndex() throws Exception {
        List<Integer> assignedUsers = List.of(2, 3);
        when(storageService.getReceiptById(userId, receiptId)).thenReturn(testReceipt);

        mockMvc.perform(post("/api/split/assign-users")
                        .param("userId", userId.toString())
                        .param("receiptId", receiptId)
                        .param("itemIndex", "5")  // invalid index
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(assignedUsers))))
                .andExpect(status().isBadRequest());
    }


    @Test
    void testAssignUsersToItem_ReceiptNotFound() throws Exception {
        Mockito.when(storageService.getReceiptById(userId, receiptId)).thenReturn(null);

        mockMvc.perform(post("/assign-users")
                        .param("userId", userId.toString())
                        .param("receiptId", receiptId)
                        .param("itemIndex", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(1))))
                .andExpect(status().isNotFound());
    }

    @Test
    void testSplitAmongMultipleUsers() {
        ReceiptItem item1 = new ReceiptItem("Pizza", 12.00, 1);
        item1.setAssignedUsers(List.of(1, 2));

        ReceiptItem item2 = new ReceiptItem("Fries", 6.00, 1);
        item2.setAssignedUsers(List.of(2));

        ReceiptItem item3 = new ReceiptItem("Soda", 9.00, 1);
        item3.setAssignedUsers(List.of(1, 2, 3));

        ReceiptData receipt = new ReceiptData();
        receipt.setItems(List.of(item1, item2, item3));

        Map<Integer, Double> result = itemSplitService.calculateUserOwedAmounts(receipt);

        // Round values to 2 decimal places for comparison
        assertEquals(9.0, result.get(1), 0.01); // (12.00 / 2) + (9.00 / 3)
        assertEquals(15.0, result.get(2), 0.01);  // (12.00 / 2) + (6.00 / 1) + (9.00 / 3)
        assertEquals(3.0, result.get(3), 0.01); // (9.00 / 3)
    }

    @Test
    void testFallbackToUploaderIfNoUsersAssigned() {
        ReceiptItem item = new ReceiptItem("Solo Item", 10.00, 1);
        item.setAssignedUsers(new ArrayList<>()); // No assigned users

        ReceiptData receipt = new ReceiptData();
        receipt.setItems(List.of(item));

        Map<Integer, Double> result = itemSplitService.calculateUserOwedAmounts(receipt);

        assertEquals(10.00, result.get(1), 0.01); //defaults to oine user
    }

    @Test
    void testMultipleQuantities() {
        ReceiptItem item = new ReceiptItem("Wings", 5.00, 3); // 3 * $5 = $15
        item.setAssignedUsers(List.of(1, 2, 3));

        ReceiptData receipt = new ReceiptData();
        receipt.setItems(List.of(item));

        Map<Integer, Double> result = itemSplitService.calculateUserOwedAmounts(receipt);

        assertEquals(5.0, result.get(1), 0.01);
        assertEquals(5.0, result.get(2), 0.01);
        assertEquals(5.0, result.get(3), 0.01);
    }
    
}