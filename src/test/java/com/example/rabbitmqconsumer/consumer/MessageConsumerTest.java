package com.example.rabbitmqconsumer.consumer;

import com.example.rabbitmqconsumer.dto.MessageDto;
import com.example.rabbitmqconsumer.service.MessageProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageConsumerTest {

    @Mock
    private MessageProcessingService messageProcessingService;

    @Mock
    private Message message;

    @Mock
    private MessageProperties messageProperties;

    @InjectMocks
    private MessageConsumer messageConsumer;

    private MessageDto testMessageDto;
    private Map<String, Object> testHeaders;

    @BeforeEach
    void setUp() {
        testMessageDto = new MessageDto();
        testMessageDto.setId("test-id-123");
        testMessageDto.setContent("Test message content");
        testMessageDto.setType("order");
        testMessageDto.setTimestamp(LocalDateTime.now());
        testMessageDto.setRetryCount(0);

        testHeaders = new HashMap<>();
        testHeaders.put("contentType", "application/json");
        testHeaders.put("priority", "1");

        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getHeaders()).thenReturn(testHeaders);
    }

    @Test
    void testConsumeMessageSuccess() {
        // Arrange
        String routingKey = "test.routing.key";
        when(messageProcessingService.processMessage(any(MessageDto.class), any(Map.class)))
                .thenReturn(true);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            messageConsumer.consumeMessage(testMessageDto, routingKey, message);
        });

        // Verify
        verify(messageProcessingService).processMessage(eq(testMessageDto), eq(testHeaders));
    }

    @Test
    void testConsumeMessageWithNullMessage() {
        // Arrange
        String routingKey = "test.routing.key";

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messageConsumer.consumeMessage(null, routingKey, message);
        });

        // Verify
        verify(messageProcessingService, never()).processMessage(any(), any());
    }

    @Test
    void testConsumeMessageWithNullId() {
        // Arrange
        testMessageDto.setId(null);
        String routingKey = "test.routing.key";

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messageConsumer.consumeMessage(testMessageDto, routingKey, message);
        });

        // Verify
        verify(messageProcessingService, never()).processMessage(any(), any());
    }

    @Test
    void testConsumeMessageWithNullContent() {
        // Arrange
        testMessageDto.setContent(null);
        String routingKey = "test.routing.key";

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messageConsumer.consumeMessage(testMessageDto, routingKey, message);
        });

        // Verify
        verify(messageProcessingService, never()).processMessage(any(), any());
    }

    @Test
    void testConsumeMessageProcessingFailure() {
        // Arrange
        String routingKey = "test.routing.key";
        when(messageProcessingService.processMessage(any(MessageDto.class), any(Map.class)))
                .thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messageConsumer.consumeMessage(testMessageDto, routingKey, message);
        });

        assertEquals("Message processing failed", exception.getMessage());
        
        // Verify
        verify(messageProcessingService).processMessage(eq(testMessageDto), eq(testHeaders));
    }

    @Test
    void testConsumeMessageProcessingException() {
        // Arrange
        String routingKey = "test.routing.key";
        RuntimeException processingException = new RuntimeException("Processing error");
        when(messageProcessingService.processMessage(any(MessageDto.class), any(Map.class)))
                .thenThrow(processingException);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messageConsumer.consumeMessage(testMessageDto, routingKey, message);
        });

        assertEquals("Processing error", exception.getMessage());
        
        // Verify
        verify(messageProcessingService).processMessage(eq(testMessageDto), eq(testHeaders));
    }

    @Test
    void testConsumeDeadLetterMessage() {
        // Arrange
        String routingKey = "test.routing.key";
        testMessageDto.setRetryCount(3);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            messageConsumer.consumeDeadLetterMessage(testMessageDto, routingKey, message);
        });

        // Verify
        verify(messageProcessingService).handleDeadLetterMessage(eq(testMessageDto), eq(testHeaders));
    }

    @Test
    void testConsumeMessageWithEmptyHeaders() {
        // Arrange
        String routingKey = "test.routing.key";
        Map<String, Object> emptyHeaders = new HashMap<>();
        when(messageProperties.getHeaders()).thenReturn(emptyHeaders);
        when(messageProcessingService.processMessage(any(MessageDto.class), any(Map.class)))
                .thenReturn(true);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            messageConsumer.consumeMessage(testMessageDto, routingKey, message);
        });

        // Verify
        verify(messageProcessingService).processMessage(eq(testMessageDto), eq(emptyHeaders));
    }

    @Test
    void testConsumeMessageIncrementRetryCount() {
        // Arrange
        String routingKey = "test.routing.key";
        testMessageDto.setRetryCount(1);
        RuntimeException processingException = new RuntimeException("Processing failed");
        when(messageProcessingService.processMessage(any(MessageDto.class), any(Map.class)))
                .thenThrow(processingException);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messageConsumer.consumeMessage(testMessageDto, routingKey, message);
        });

        assertEquals("Processing failed", exception.getMessage());
        assertEquals(Integer.valueOf(2), testMessageDto.getRetryCount()); // Should be incremented
        
        // Verify
        verify(messageProcessingService).processMessage(eq(testMessageDto), eq(testHeaders));
    }

    @Test
    void testConsumeMessageWithDifferentMessageTypes() {
        // Arrange
        String routingKey = "test.routing.key";
        when(messageProcessingService.processMessage(any(MessageDto.class), any(Map.class)))
                .thenReturn(true);

        // Test different message types
        String[] messageTypes = {"order", "payment", "notification", "user", "generic"};
        
        for (String type : messageTypes) {
            testMessageDto.setType(type);
            testMessageDto.setId("test-id-" + type);

            // Act & Assert - should not throw exception
            assertDoesNotThrow(() -> {
                messageConsumer.consumeMessage(testMessageDto, routingKey, message);
            });
        }

        // Verify processMessage was called for each type
        verify(messageProcessingService, times(messageTypes.length))
                .processMessage(any(MessageDto.class), eq(testHeaders));
    }

    @Test
    void testConsumeMessageWithComplexHeaders() {
        // Arrange
        String routingKey = "test.routing.key";
        Map<String, Object> complexHeaders = new HashMap<>();
        complexHeaders.put("x-death", "some-death-info");
        complexHeaders.put("x-retry-count", 2);
        complexHeaders.put("custom-header", "custom-value");
        complexHeaders.put("timestamp", System.currentTimeMillis());
        
        when(messageProperties.getHeaders()).thenReturn(complexHeaders);
        when(messageProcessingService.processMessage(any(MessageDto.class), any(Map.class)))
                .thenReturn(true);

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            messageConsumer.consumeMessage(testMessageDto, routingKey, message);
        });

        // Verify
        verify(messageProcessingService).processMessage(eq(testMessageDto), eq(complexHeaders));
    }

    @Test
    void testHandleProcessingErrorWithNullRetryCount() {
        // Arrange
        String routingKey = "test.routing.key";
        testMessageDto.setRetryCount(null);
        RuntimeException processingException = new RuntimeException("Processing failed");
        when(messageProcessingService.processMessage(any(MessageDto.class), any(Map.class)))
                .thenThrow(processingException);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messageConsumer.consumeMessage(testMessageDto, routingKey, message);
        });

        // Should not throw NullPointerException when incrementing retry count
        assertEquals("Processing failed", exception.getMessage());
        
        // Verify
        verify(messageProcessingService).processMessage(eq(testMessageDto), eq(testHeaders));
    }

    @Test
    void testConsumeDeadLetterMessageWithNullMessage() {
        // Arrange
        String routingKey = "dlq.routing.key";

        // Act & Assert - should not throw exception for logging
        assertDoesNotThrow(() -> {
            messageConsumer.consumeDeadLetterMessage(null, routingKey, message);
        });

        // Verify - should still call handleDeadLetterMessage even with null message
        verify(messageProcessingService).handleDeadLetterMessage(isNull(), eq(testHeaders));
    }

    @Test
    void testConsumeDeadLetterMessageWithHighRetryCount() {
        // Arrange
        String routingKey = "dlq.routing.key";
        testMessageDto.setRetryCount(5);
        testMessageDto.setContent("Message that failed multiple times");

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            messageConsumer.consumeDeadLetterMessage(testMessageDto, routingKey, message);
        });

        // Verify
        verify(messageProcessingService).handleDeadLetterMessage(eq(testMessageDto), eq(testHeaders));
    }

    // TODO: 
    @Disabled
    @Test
    void testMessageValidationWithBlankFields() {
        // Test with blank ID
        testMessageDto.setId("");
        String routingKey = "test.routing.key";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messageConsumer.consumeMessage(testMessageDto, routingKey, message);
        });

        verify(messageProcessingService, never()).processMessage(any(), any());

        // Test with blank content
        testMessageDto.setId("valid-id");
        testMessageDto.setContent("");

        exception = assertThrows(RuntimeException.class, () -> {
            messageConsumer.consumeMessage(testMessageDto, routingKey, message);
        });

        verify(messageProcessingService, never()).processMessage(any(), any());
    }

    @Test
    void testMessageConsumerLogging() {
        // This test verifies that appropriate logging occurs
        // In a real scenario, you might use a logging framework test utility
        String routingKey = "test.routing.key";
        when(messageProcessingService.processMessage(any(MessageDto.class), any(Map.class)))
                .thenReturn(true);

        // Act
        assertDoesNotThrow(() -> {
            messageConsumer.consumeMessage(testMessageDto, routingKey, message);
        });

        // Verify processing was called (logging verification would require additional test setup)
        verify(messageProcessingService).processMessage(eq(testMessageDto), eq(testHeaders));
        verify(message, atLeastOnce()).getMessageProperties();
        verify(messageProperties, atLeastOnce()).getHeaders();
    }
}