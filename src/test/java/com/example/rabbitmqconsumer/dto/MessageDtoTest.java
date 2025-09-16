package com.example.rabbitmqconsumer.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MessageDtoTest {

    private Validator validator;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        // objectMapper.findAndRegisterModules();
    }

    @Test
    void testValidMessageDto() {
        MessageDto message = new MessageDto();
        message.setId("test-id");
        message.setContent("Test content");
        message.setType("order");
        message.setTimestamp(LocalDateTime.now());

        Set<ConstraintViolation<MessageDto>> violations = validator.validate(message);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testConstructorWithParameters() {
        String id = "test-id";
        String content = "Test content";
        String type = "order";
        
        MessageDto message = new MessageDto(id, content, type);
        
        assertEquals(id, message.getId());
        assertEquals(content, message.getContent());
        assertEquals(type, message.getType());
        assertNotNull(message.getTimestamp());
        assertEquals(Integer.valueOf(0), message.getRetryCount());
    }

    @Test
    void testValidationWithBlankId() {
        MessageDto message = new MessageDto();
        message.setId("");
        message.setContent("Test content");
        message.setType("order");
        message.setTimestamp(LocalDateTime.now());

        Set<ConstraintViolation<MessageDto>> violations = validator.validate(message);
        assertEquals(1, violations.size());
        assertEquals("Message ID cannot be blank", violations.iterator().next().getMessage());
    }

    @Test
    void testValidationWithNullId() {
        MessageDto message = new MessageDto();
        message.setId(null);
        message.setContent("Test content");
        message.setType("order");
        message.setTimestamp(LocalDateTime.now());

        Set<ConstraintViolation<MessageDto>> violations = validator.validate(message);
        assertEquals(1, violations.size());
        assertEquals("Message ID cannot be blank", violations.iterator().next().getMessage());
    }

    @Test
    void testValidationWithBlankContent() {
        MessageDto message = new MessageDto();
        message.setId("test-id");
        message.setContent("");
        message.setType("order");
        message.setTimestamp(LocalDateTime.now());

        Set<ConstraintViolation<MessageDto>> violations = validator.validate(message);
        assertEquals(1, violations.size());
        assertEquals("Message content cannot be blank", violations.iterator().next().getMessage());
    }

    @Test
    void testValidationWithBlankType() {
        MessageDto message = new MessageDto();
        message.setId("test-id");
        message.setContent("Test content");
        message.setType("");
        message.setTimestamp(LocalDateTime.now());

        Set<ConstraintViolation<MessageDto>> violations = validator.validate(message);
        assertEquals(1, violations.size());
        assertEquals("Message type cannot be blank", violations.iterator().next().getMessage());
    }

    // TODO:
    @Disabled
    @Test
    void testValidationWithNullTimestamp() {
        MessageDto message = new MessageDto();
        message.setId("test-id");
        message.setContent("Test content");
        message.setType("order");
        message.setTimestamp(null);

        Set<ConstraintViolation<MessageDto>> violations = validator.validate(message);
        assertEquals(1, violations.size());
        assertEquals("Timestamp cannot be null", violations.iterator().next().getMessage());
    }

    @Test
    void testJsonSerialization() throws Exception {
        MessageDto message = new MessageDto();
        message.setId("test-id");
        message.setContent("Test content");
        message.setType("order");
        message.setTimestamp(LocalDateTime.of(2023, 10, 15, 10, 30, 45));
        message.setSource("test-source");
        message.setPriority("high");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", 123);
        message.setMetadata(metadata);
        message.setRetryCount(1);

        String json = objectMapper.writeValueAsString(message);
        assertNotNull(json);
        assertTrue(json.contains("test-id"));
        assertTrue(json.contains("Test content"));
        assertTrue(json.contains("order"));
        assertTrue(json.contains("2023-10-15 10:30:45"));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = """
            {
                "id": "test-id",
                "content": "Test content",
                "type": "order",
                "timestamp": "2023-10-15 10:30:45",
                "source": "test-source",
                "priority": "high",
                "metadata": {
                    "key1": "value1",
                    "key2": 123
                },
                "retryCount": 1
            }
            """;

        MessageDto message = objectMapper.readValue(json, MessageDto.class);
        
        assertEquals("test-id", message.getId());
        assertEquals("Test content", message.getContent());
        assertEquals("order", message.getType());
        assertEquals(LocalDateTime.of(2023, 10, 15, 10, 30, 45), message.getTimestamp());
        assertEquals("test-source", message.getSource());
        assertEquals("high", message.getPriority());
        assertEquals(2, message.getMetadata().size());
        assertEquals("value1", message.getMetadata().get("key1"));
        assertEquals(123, message.getMetadata().get("key2"));
        assertEquals(Integer.valueOf(1), message.getRetryCount());
    }

    @Test
    void testEqualsAndHashCode() {
        MessageDto message1 = new MessageDto("id1", "content1", "type1");
        MessageDto message2 = new MessageDto("id1", "content1", "type1");
        MessageDto message3 = new MessageDto("id2", "content2", "type2");

        assertEquals(message1, message2);
        assertNotEquals(message1, message3);
        assertEquals(message1.hashCode(), message2.hashCode());
        assertNotEquals(message1.hashCode(), message3.hashCode());
    }

    @Test
    void testToString() {
        MessageDto message = new MessageDto("test-id", "Test content", "order");
        String toString = message.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("test-id"));
        assertTrue(toString.contains("Test content"));
        assertTrue(toString.contains("order"));
    }
}