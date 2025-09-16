package com.example.rabbitmqconsumer;

import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

@SpringBootTest
@EnableRabbit
public class RabbitMqConsumerApplicationTest {

    @Test
    void contextLoads() {
        // This should work without external dependencies
    }
}