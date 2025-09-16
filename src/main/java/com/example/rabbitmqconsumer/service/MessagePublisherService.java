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

    public void publishMessage(MessageDto messageDto) {
        try {
            log.info("Publishing message with ID: {} to exchange: {} with routing key: {}", 
                     messageDto.getId(), exchangeName, routingKey);
            
            rabbitTemplate.convertAndSend(exchangeName, routingKey, messageDto);
            
            log.info("Message published successfully: {}", messageDto.getId());
        } catch (Exception e) {
            log.error("Failed to publish message: {}", messageDto.getId(), e);
            throw new RuntimeException("Failed to publish message", e);
        }
    }

    public void publishMessage(MessageDto messageDto, String customRoutingKey) {
        try {
            log.info("Publishing message with ID: {} to exchange: {} with custom routing key: {}", 
                     messageDto.getId(), exchangeName, customRoutingKey);
            
            rabbitTemplate.convertAndSend(exchangeName, customRoutingKey, messageDto);
            
            log.info("Message published successfully with custom routing key: {}", messageDto.getId());
        } catch (Exception e) {
            log.error("Failed to publish message with custom routing key: {}", messageDto.getId(), e);
            throw new RuntimeException("Failed to publish message", e);
        }
    }
}