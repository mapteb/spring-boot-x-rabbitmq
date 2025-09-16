package com.example.rabbitmqconsumer.consumer;

import com.example.rabbitmqconsumer.dto.MessageDto;
import com.example.rabbitmqconsumer.service.MessageProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageConsumer {

    private final MessageProcessingService messageProcessingService;

    @RabbitListener(queues = "${app.rabbitmq.queue.name}")
    public void consumeMessage(
            @Payload MessageDto messageDto,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey,
            Message message) {
        
        try {
            log.info("Received message: {}", messageDto);
            log.debug("Routing key: {}", routingKey);
            log.debug("Message properties: {}", message.getMessageProperties());
            
            // Extract headers from message properties
            Map<String, Object> headers = message.getMessageProperties().getHeaders();
            log.debug("Message headers: {}", headers);

            // Validate message
            if (messageDto == null || messageDto.getId() == null || messageDto.getContent() == null) {
                log.error("Invalid message received: {}", messageDto);
                throw new IllegalArgumentException("Message validation failed");
            }

            // Process the message
            boolean processed = messageProcessingService.processMessage(messageDto, headers);
            
            if (processed) {
                log.info("Successfully processed message with ID: {}", messageDto.getId());
            } else {
                log.warn("Message processing failed for ID: {}", messageDto.getId());
                throw new RuntimeException("Message processing failed");
            }

        } catch (Exception e) {
            log.error("Error processing message: {}", messageDto, e);
            
            // Extract headers from message properties for error handling
            Map<String, Object> headers = message.getMessageProperties().getHeaders();
            handleProcessingError(messageDto, e, headers);
            throw e; // Re-throw to trigger retry mechanism
        }
    }

    @RabbitListener(queues = "dlq.${app.rabbitmq.queue.name}")
    public void consumeDeadLetterMessage(
            @Payload MessageDto messageDto,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey,
            Message message) {
        
        log.error("Processing message from Dead Letter Queue: {}", messageDto);
        log.error("DLQ Routing key: {}", routingKey);
        
        // Extract headers from message properties
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        log.error("DLQ Headers: {}", headers);
        
        // Handle dead letter messages (could save to database, send alerts, etc.)
        messageProcessingService.handleDeadLetterMessage(messageDto, headers);
    }

    private void handleProcessingError(MessageDto messageDto, Exception e, Map<String, Object> headers) {
        log.error("Handling processing error for message ID: {}, Error: {}", 
                 messageDto != null ? messageDto.getId() : "unknown", e.getMessage());
        
        // Could implement custom error handling logic here
        // For example: increment retry count, send to error topic, etc.
        
        if (messageDto != null && messageDto.getRetryCount() != null) {
            messageDto.setRetryCount(messageDto.getRetryCount() + 1);
        }
    }
}