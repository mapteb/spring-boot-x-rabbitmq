package com.example.rabbitmqconsumer.controller;

import com.example.rabbitmqconsumer.dto.MessageDto;
import com.example.rabbitmqconsumer.service.MessageProcessingService;
import com.example.rabbitmqconsumer.service.MessagePublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageProcessingService messageProcessingService;
    private final MessagePublisherService messagePublisherService;

    @PostMapping("/publish")
    public ResponseEntity<Map<String, String>> publishMessage(@Valid @RequestBody MessageDto messageDto) {
        if (messageDto.getId() == null) {
            messageDto.setId(UUID.randomUUID().toString());
        }
        if (messageDto.getTimestamp() == null) {
            messageDto.setTimestamp(LocalDateTime.now());
        }

        messagePublisherService.publishMessage(messageDto);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("messageId", messageDto.getId());
        response.put("message", "Message published successfully");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/publish/bulk")
    public ResponseEntity<Map<String, Object>> publishBulkMessages(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "order") String type) {
        
        Map<String, Object> response = new HashMap<>();
        
        for (int i = 0; i < count; i++) {
            MessageDto message = new MessageDto(
                UUID.randomUUID().toString(),
                String.format("Test %s message #%d", type, i + 1),
                type
            );
            messagePublisherService.publishMessage(message);
        }
        
        response.put("status", "success");
        response.put("publishedCount", count);
        response.put("type", type);
        response.put("message", "Bulk messages published successfully");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMessageStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("processedCount", messageProcessingService.getProcessedCount());
        stats.put("failedCount", messageProcessingService.getFailedCount());
        stats.put("deadLetterCount", messageProcessingService.getDeadLetterCount());
        stats.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/processed")
    public ResponseEntity<Map<String, MessageDto>> getProcessedMessages() {
        return ResponseEntity.ok(messageProcessingService.getProcessedMessages());
    }

    @DeleteMapping("/processed")
    public ResponseEntity<Map<String, String>> clearProcessedMessages() {
        messageProcessingService.clearProcessedMessages();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Processed messages cleared");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test-error")
    public ResponseEntity<Map<String, String>> publishErrorMessage() {
        MessageDto errorMessage = new MessageDto(
            UUID.randomUUID().toString(),
            "This is an invalid message that should fail processing",
            "order"
        );
        
        messagePublisherService.publishMessage(errorMessage);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("messageId", errorMessage.getId());
        response.put("message", "Error message published for testing");
        
        return ResponseEntity.ok(response);
    }
}