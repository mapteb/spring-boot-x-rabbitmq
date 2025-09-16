package com.example.rabbitmqconsumer.controller;

import com.example.rabbitmqconsumer.dto.MessageDto;
import com.example.rabbitmqconsumer.service.MessageProcessingService;
import com.example.rabbitmqconsumer.service.MessagePublisherService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MessageProcessingService messageProcessingService;

    @MockitoBean
    private MessagePublisherService messagePublisherService;

    @Test
    void testPublishMessage() throws Exception {
        MessageDto messageDto = new MessageDto();
        messageDto.setId("id1");
        messageDto.setContent("Test message");
        messageDto.setType("order");

        doNothing().when(messagePublisherService).publishMessage(any(MessageDto.class));

        mockMvc.perform(post("/api/messages/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(messageDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.messageId").exists())
                .andExpect(jsonPath("$.message").value("Message published successfully"));

        verify(messagePublisherService).publishMessage(any(MessageDto.class));
    }

    @Test
    void testPublishMessageWithValidationError() throws Exception {
        MessageDto messageDto = new MessageDto();
        // Missing required fields

        mockMvc.perform(post("/api/messages/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(messageDto)))
                .andExpect(status().isBadRequest());

        verify(messagePublisherService, never()).publishMessage(any(MessageDto.class));
    }

    @Test
    void testPublishMessageWithCompleteData() throws Exception {
        MessageDto messageDto = new MessageDto();
        messageDto.setId("test-id");
        messageDto.setContent("Test message");
        messageDto.setType("order");
        messageDto.setTimestamp(LocalDateTime.now());

        doNothing().when(messagePublisherService).publishMessage(any(MessageDto.class));

        mockMvc.perform(post("/api/messages/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(messageDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.messageId").value("test-id"))
                .andExpect(jsonPath("$.message").value("Message published successfully"));

        verify(messagePublisherService).publishMessage(any(MessageDto.class));
    }

    @Test
    void testPublishBulkMessages() throws Exception {
        doNothing().when(messagePublisherService).publishMessage(any(MessageDto.class));

        mockMvc.perform(post("/api/messages/publish/bulk")
                .param("count", "5")
                .param("type", "payment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.publishedCount").value(5))
                .andExpect(jsonPath("$.type").value("payment"))
                .andExpect(jsonPath("$.message").value("Bulk messages published successfully"));

        verify(messagePublisherService, times(5)).publishMessage(any(MessageDto.class));
    }

    @Test
    void testPublishBulkMessagesWithDefaultParameters() throws Exception {
        doNothing().when(messagePublisherService).publishMessage(any(MessageDto.class));

        mockMvc.perform(post("/api/messages/publish/bulk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.publishedCount").value(10))
                .andExpect(jsonPath("$.type").value("order"));

        verify(messagePublisherService, times(10)).publishMessage(any(MessageDto.class));
    }

    @Test
    void testGetMessageStats() throws Exception {
        when(messageProcessingService.getProcessedCount()).thenReturn(15L);
        when(messageProcessingService.getFailedCount()).thenReturn(2L);
        when(messageProcessingService.getDeadLetterCount()).thenReturn(1L);

        mockMvc.perform(get("/api/messages/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(15))
                .andExpect(jsonPath("$.failedCount").value(2))
                .andExpect(jsonPath("$.deadLetterCount").value(1))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(messageProcessingService).getProcessedCount();
        verify(messageProcessingService).getFailedCount();
        verify(messageProcessingService).getDeadLetterCount();
    }

    @Test
    void testGetProcessedMessages() throws Exception {
        Map<String, MessageDto> processedMessages = new ConcurrentHashMap<>();
        MessageDto message1 = new MessageDto("id1", "content1", "type1");
        MessageDto message2 = new MessageDto("id2", "content2", "type2");
        processedMessages.put("id1", message1);
        processedMessages.put("id2", message2);

        when(messageProcessingService.getProcessedMessages()).thenReturn(processedMessages);

        mockMvc.perform(get("/api/messages/processed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id1.content").value("content1"))
                .andExpect(jsonPath("$.id2.content").value("content2"));

        verify(messageProcessingService).getProcessedMessages();
    }

    @Test
    void testClearProcessedMessages() throws Exception {
        doNothing().when(messageProcessingService).clearProcessedMessages();

        mockMvc.perform(delete("/api/messages/processed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Processed messages cleared"));

        verify(messageProcessingService).clearProcessedMessages();
    }

    @Test
    void testPublishErrorMessage() throws Exception {
        doNothing().when(messagePublisherService).publishMessage(any(MessageDto.class));

        mockMvc.perform(post("/api/messages/test-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.messageId").exists())
                .andExpect(jsonPath("$.message").value("Error message published for testing"));

        verify(messagePublisherService).publishMessage(any(MessageDto.class));
    }

    // TODO:
    @Disabled
    @Test
    void testPublishMessageServiceException() throws Exception {
        MessageDto messageDto = new MessageDto();
        messageDto.setContent("Test message");
        messageDto.setType("order");
        messageDto.setId("id1");

        doThrow(new RuntimeException("Publication failed"))
                .when(messagePublisherService).publishMessage(any(MessageDto.class));

        mockMvc.perform(post("/api/messages/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(messageDto)))
                .andExpect(status().isInternalServerError());

        verify(messagePublisherService).publishMessage(any(MessageDto.class));
    }

    @Test
    void testInvalidJsonRequest() throws Exception {
        String invalidJson = "{ invalid json }";

        mockMvc.perform(post("/api/messages/publish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(messagePublisherService, never()).publishMessage(any(MessageDto.class));
    }
}