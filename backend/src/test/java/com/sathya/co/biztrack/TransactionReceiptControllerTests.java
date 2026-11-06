package com.sathya.co.biztrack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sathya.co.biztrack.controller.TransactionController;
import com.sathya.co.biztrack.dto.TransactionRequest;
import com.sathya.co.biztrack.entity.Transaction;
import com.sathya.co.biztrack.service.CurrentUser;
import com.sathya.co.biztrack.service.ReceiptExtractionService;
import com.sathya.co.biztrack.service.ReceiptTransactionParser;
import com.sathya.co.biztrack.service.TabularTransactionExtractor;
import com.sathya.co.biztrack.service.TransactionService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TransactionReceiptControllerTests {

    private TransactionService transactionService;
    private CurrentUser currentUser;
    private ReceiptExtractionService extractionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        transactionService = mock(TransactionService.class);
        currentUser = mock(CurrentUser.class);
        extractionService = new ReceiptExtractionService(
                image -> "Green Basket\nDate: 02/10/2026\nPaid INR 25.00",
                new ReceiptTransactionParser(),
                new TabularTransactionExtractor(),
                1024 * 1024);
        TransactionController controller = new TransactionController();
        ReflectionTestUtils.setField(controller, "service", transactionService);
        ReflectionTestUtils.setField(controller, "currentUser", currentUser);
        ReflectionTestUtils.setField(controller, "receiptExtractionService", extractionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void multipartExtractionReturnsParsedResponseFromUploadedBytes() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.png", "image/png", png());

        mockMvc.perform(multipart("/api/transactions/upload-and-extract").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.fileName").value("receipt.png"))
                .andExpect(jsonPath("$.extractedText").value(
                        "Green Basket\nDate: 02/10/2026\nPaid INR 25.00"))
                .andExpect(jsonPath("$.transactions[0].merchant").value("Green Basket"))
                .andExpect(jsonPath("$.transactions[0].amount").value(25.0));
    }

    @Test
    void confirmedMultipartTransactionIsSavedWithValidatedReceipt() throws Exception {
        Transaction saved = new Transaction();
        when(transactionService.addWithReceipt(
                any(), anyString(), eq("image/png"), any(byte[].class))).thenReturn(saved);
        MockMultipartFile transaction = new MockMultipartFile(
                "transaction",
                "transaction.json",
                "application/json",
                """
                        {"transactionType":"EXPENSE","amount":25.0,
                         "transactionDate":"2026-10-02","transactionTime":"14:35:00",
                         "categoryId":1,
                         "description":"Groceries"}
                        """.getBytes(StandardCharsets.UTF_8));
        MockMultipartFile receipt = new MockMultipartFile(
                "receipt", "receipt.png", "image/png", png());

        mockMvc.perform(multipart("/api/transactions/with-receipt")
                        .file(transaction)
                        .file(receipt))
                .andExpect(status().isCreated());

        ArgumentCaptor<TransactionRequest> transactionCaptor =
                ArgumentCaptor.forClass(TransactionRequest.class);
        verify(transactionService).addWithReceipt(
                transactionCaptor.capture(), eq("receipt.png"), eq("image/png"), any(byte[].class));
        assertEquals(LocalTime.of(14, 35), transactionCaptor.getValue().transactionTime());
    }

    private byte[] png() throws IOException {
        BufferedImage image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
