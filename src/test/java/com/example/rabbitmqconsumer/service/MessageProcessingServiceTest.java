package com.example.rabbitmqconsumer.service;

import com.example.rabbitmqconsumer.dto.MessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MessageProcessingServiceTest {

    private MessageProcessingService messageProcessingService;

    @BeforeEach
    void setUp() {
        messageProcessingService = new MessageProcessingService();
    }

    @Test
    void testProcessOrderMessage() {
        MessageDto orderMessage = createTestMessage("order", "Valid order data");
        Map<String, Object> headers = new HashMap<>();

        boolean result = messageProcessingService.processMessage(orderMessage, headers);

        assertTrue(result);
        assertEquals(1, messageProcessingService.getProcessedCount());
        assertEquals(0, messageProcessingService.getFailedCount());
        assertTrue(messageProcessingService.getProcessedMessages().containsKey(orderMessage.getId()));
    }

    @Test
    void testProcessPaymentMessage() {
        MessageDto paymentMessage = createTestMessage("payment", "Valid payment data");
        Map<String, Object> headers = new HashMap<>();

        boolean result = messageProcessingService.processMessage(paymentMessage, headers);

        assertTrue(result);
        assertEquals(1, messageProcessingService.getProcessedCount());
        assertEquals(0, messageProcessingService.getFailedCount());
        assertTrue(messageProcessingService.getProcessedMessages().containsKey(paymentMessage.getId()));
    }

    @Test
    void testProcessNotificationMessage() {
        MessageDto notificationMessage = createTestMessage("notification", "Valid notification data");
        Map<String, Object> headers = new HashMap<>();

        boolean result = messageProcessingService.processMessage(notificationMessage, headers);

        assertTrue(result);
        assertEquals(1, messageProcessingService.getProcessedCount());
        assertEquals(0, messageProcessingService.getFailedCount());
        assertTrue(messageProcessingService.getProcessedMessages().containsKey(notificationMessage.getId()));
    }

    @Test
    void testProcessUserMessage() {
        MessageDto userMessage = createTestMessage("user", "Valid user data");
        Map<String, Object> headers = new HashMap<>();

        boolean result = messageProcessingService.processMessage(userMessage, headers);

        assertTrue(result);
        assertEquals(1, messageProcessingService.getProcessedCount());
        assertEquals(0, messageProcessingService.getFailedCount());
        assertTrue(messageProcessingService.getProcessedMessages().containsKey(userMessage.getId()));
    }

    @Test
    void testProcessGenericMessage() {
        MessageDto genericMessage = createTestMessage("unknown", "Generic message data");
        Map<String, Object> headers = new HashMap<>();

        boolean result = messageProcessingService.processMessage(genericMessage, headers);

        assertTrue(result);
        assertEquals(1, messageProcessingService.getProcessedCount());
        assertEquals(0, messageProcessingService.getFailedCount());
        assertTrue(messageProcessingService.getProcessedMessages().containsKey(genericMessage.getId()));
    }

    // TODO:
    @Disabled
    @Test
    void testProcessInvalidOrderMessage() {
        MessageDto invalidOrderMessage = createTestMessage("order", "This is invalid order data");
        Map<String, Object> headers = new HashMap<>();

        assertThrows(RuntimeException.class, () -> {
            messageProcessingService.processMessage(invalidOrderMessage, headers);
        });

        assertEquals(0, messageProcessingService.getProcessedCount());
        assertEquals(1, messageProcessingService.getFailedCount());
        assertFalse(messageProcessingService.getProcessedMessages().containsKey(invalidOrderMessage.getId()));
    }

    // TODO:
    @Disabled
    @Test
    void testProcessDeclinedPaymentMessage() {
        MessageDto declinedPaymentMessage = createTestMessage("payment", "Payment declined");
        Map<String, Object> headers = new HashMap<>();

        assertThrows(RuntimeException.class, () -> {
            messageProcessingService.processMessage(declinedPaymentMessage, headers);
        });

        assertEquals(0, messageProcessingService.getProcessedCount());
        assertEquals(1, messageProcessingService.getFailedCount());
        assertFalse(messageProcessingService.getProcessedMessages().containsKey(declinedPaymentMessage.getId()));
    }

    @Test
    void testProcessMultipleMessages() {
        MessageDto message1 = createTestMessage("order", "Valid order 1");
        MessageDto message2 = createTestMessage("payment", "Valid payment 1");
        MessageDto message3 = createTestMessage("notification", "Valid notification 1");
        Map<String, Object> headers = new HashMap<>();

        boolean result1 = messageProcessingService.processMessage(message1, headers);
        boolean result2 = messageProcessingService.processMessage(message2, headers);
        boolean result3 = messageProcessingService.processMessage(message3, headers);

        assertTrue(result1);
        assertTrue(result2);
        assertTrue(result3);
        assertEquals(3, messageProcessingService.getProcessedCount());
        assertEquals(0, messageProcessingService.getFailedCount());
        assertEquals(3, messageProcessingService.getProcessedMessages().size());
    }

    @Test
    void testHandleDeadLetterMessage() {
        MessageDto deadLetterMessage = createTestMessage("order", "Failed message");
        deadLetterMessage.setRetryCount(3);
        Map<String, Object> headers = new HashMap<>();

        assertDoesNotThrow(() -> {
            messageProcessingService.handleDeadLetterMessage(deadLetterMessage, headers);
        });

        assertEquals(1, messageProcessingService.getDeadLetterCount());
    }

    @Test
    void testClearProcessedMessages() {
        // Process some messages first
        MessageDto message1 = createTestMessage("order", "Valid order");
        MessageDto message2 = createTestMessage("payment", "Valid payment");
        Map<String, Object> headers = new HashMap<>();

        messageProcessingService.processMessage(message1, headers);
        messageProcessingService.processMessage(message2, headers);

        assertEquals(2, messageProcessingService.getProcessedCount());
        assertEquals(2, messageProcessingService.getProcessedMessages().size());

        // Clear messages
        messageProcessingService.clearProcessedMessages();

        assertEquals(0, messageProcessingService.getProcessedCount());
        assertEquals(0, messageProcessingService.getFailedCount());
        assertEquals(0, messageProcessingService.getDeadLetterCount());
        assertEquals(0, messageProcessingService.getProcessedMessages().size());
    }

    @Test
    void testGetProcessedMessagesReturnsNewMap() {
        MessageDto message = createTestMessage("order", "Valid order");
        Map<String, Object> headers = new HashMap<>();

        messageProcessingService.processMessage(message, headers);

        Map<String, MessageDto> processedMessages = messageProcessingService.getProcessedMessages();
        int originalSize = processedMessages.size();

        // Modify the returned map
        processedMessages.clear();

        // Original should be unchanged
        assertEquals(originalSize, messageProcessingService.getProcessedMessages().size());
    }

    @Test
    void testConcurrentProcessing() throws InterruptedException {
        final int numberOfThreads = 5;
        final int messagesPerThread = 10;
        Thread[] threads = new Thread[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < messagesPerThread; j++) {
                    MessageDto message = createTestMessage("order", 
                        "Valid order from thread " + threadId + " message " + j);
                    message.setId("thread-" + threadId + "-msg-" + j);
                    Map<String, Object> headers = new HashMap<>();
                    messageProcessingService.processMessage(message, headers);
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(numberOfThreads * messagesPerThread, messageProcessingService.getProcessedCount());
        assertEquals(0, messageProcessingService.getFailedCount());
        assertEquals(numberOfThreads * messagesPerThread, messageProcessingService.getProcessedMessages().size());
    }

    private MessageDto createTestMessage(String type, String content) {
        MessageDto message = new MessageDto();
        message.setId("test-id-" + System.currentTimeMillis() + "-" + Math.random());
        message.setContent(content);
        message.setType(type);
        message.setTimestamp(LocalDateTime.now());
        message.setRetryCount(0);
        return message;
    }
}