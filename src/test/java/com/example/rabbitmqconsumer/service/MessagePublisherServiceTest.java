package com.example.rabbitmqconsumer.service;

import com.example.rabbitmqconsumer.dto.MessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagePublisherServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private MessagePublisherService messagePublisherService;

    private static final String TEST_EXCHANGE = "test.exchange";
    private static final String TEST_ROUTING_KEY = "test.routing.key";

    @BeforeEach
    void setUp() {
        // Set the properties using reflection since they're injected via @Value
        ReflectionTestUtils.setField(messagePublisherService, "exchangeName", TEST_EXCHANGE);
        ReflectionTestUtils.setField(messagePublisherService, "routingKey", TEST_ROUTING_KEY);
    }

    @Test
    void publishMessage_ShouldSuccessfullyPublishMessageDto() {
        // Arrange
        MessageDto messageDto = new MessageDto("id1", "Test message", "Test sender");
        
        // Act
        messagePublisherService.publishMessage(messageDto);
        
        // Assert
        verify(rabbitTemplate, times(1)).convertAndSend(TEST_EXCHANGE, TEST_ROUTING_KEY, messageDto);
    }

    @Test
    void publishMessage_ShouldCaptureCorrectMessageData() {
        // Arrange
        MessageDto messageDto = new MessageDto("id1", "Hello RabbitMQ", "John Doe");
        ArgumentCaptor<MessageDto> messageCaptor = ArgumentCaptor.forClass(MessageDto.class);
        
        // Act
        messagePublisherService.publishMessage(messageDto);
        
        // Assert
        verify(rabbitTemplate).convertAndSend(eq(TEST_EXCHANGE), eq(TEST_ROUTING_KEY), messageCaptor.capture());
        MessageDto capturedMessage = messageCaptor.getValue();
        
        assertEquals("Hello RabbitMQ", capturedMessage.getContent());
        assertEquals("John Doe", capturedMessage.getType());
        assertNotNull(capturedMessage.getTimestamp());
    }

    @Test
    void publishMessage_ShouldThrowRuntimeException_WhenRabbitTemplateThrowsException() {
        // Arrange
        MessageDto messageDto = new MessageDto("id1", "Test message", "Test sender");
        doThrow(new RuntimeException("Connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messagePublisherService.publishMessage(messageDto);
        });
        
        assertEquals("Failed to publish message", exception.getMessage());
        assertEquals("Connection failed", exception.getCause().getMessage());
    }

}