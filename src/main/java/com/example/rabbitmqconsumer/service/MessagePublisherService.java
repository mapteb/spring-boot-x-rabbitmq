package com.example.rabbitmqconsumer.service;

import com.example.rabbitmqconsumer.dto.MessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagePublisherService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    @Value("${app.rabbitmq.queue.name}")
    private String queueName;

    /*
     * Current Flow:
        MessagePublisherService.publishMessage() → sends to exchange (message.exchange)
        Uses routing key (message.routing.key)
        RabbitMQ routes the message to the queue (message.queue) 
        based on the binding configuration in RabbitMqConfig.java
    */
    public void publishMessage(MessageDto messageDto) {
        try {
            log.info("Publishing message with ID: {} to exchange: {} with routing key: {} (will route to queue: {})", 
                     messageDto.getId(), exchangeName, routingKey, queueName);
            
            rabbitTemplate.convertAndSend(exchangeName, routingKey, messageDto);
            
            log.info("Message published successfully: {} -> Exchange: {} -> Queue: {}", 
                     messageDto.getId(), exchangeName, queueName);
        } catch (Exception e) {
            log.error("Failed to publish message: {} to exchange: {} -> queue: {}", 
                     messageDto.getId(), exchangeName, queueName, e);
            throw new RuntimeException("Failed to publish message", e);
        }
    }

    public void publishMessage(MessageDto messageDto, String customRoutingKey) {
        try {
            log.info("Publishing message with ID: {} to exchange: {} with custom routing key: {} (target queue depends on bindings)", 
                     messageDto.getId(), exchangeName, customRoutingKey);
            
            rabbitTemplate.convertAndSend(exchangeName, customRoutingKey, messageDto);
            
            log.info("Message published successfully with custom routing key: {} -> Exchange: {}", 
                     messageDto.getId(), exchangeName);
        } catch (Exception e) {
            log.error("Failed to publish message with custom routing key: {} to exchange: {}", 
                     messageDto.getId(), exchangeName, e);
            throw new RuntimeException("Failed to publish message", e);
        }
    }

    /**
     * Publishes message directly to a specific queue (bypasses exchange routing)
     * Note: This uses the default exchange ("") with queue name as routing key
     */
    public void publishDirectlyToQueue(MessageDto messageDto, String targetQueueName) {
        try {
            log.info("Publishing message with ID: {} directly to queue: {} (using default exchange)", 
                     messageDto.getId(), targetQueueName);
            
            rabbitTemplate.convertAndSend("", targetQueueName, messageDto);
            
            log.info("Message published directly to queue: {} -> {}", messageDto.getId(), targetQueueName);
        } catch (Exception e) {
            log.error("Failed to publish message directly to queue: {} -> {}", 
                     messageDto.getId(), targetQueueName, e);
            throw new RuntimeException("Failed to publish message directly to queue", e);
        }
    }

    /**
     * Gets the configured queue name that messages will be routed to
     */
    public String getTargetQueueName() {
        return queueName;
    }

    /**
     * Gets the configured exchange name
     */
    public String getExchangeName() {
        return exchangeName;
    }

    /**
     * Gets the configured routing key
     */
    public String getRoutingKey() {
        return routingKey;
    }
}