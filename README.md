# Spring Boot Integration RabbitMQ

```
A Spring Boot helloworld style web application that publishes and consumes messages from RabbitMQ using Java 17 and Gradle build system. RabbitMQ runs in a docker container.
```

## Features

```
- **RabbitMQ Integration**: Configured to connect to RabbitMQ running at `127.0.0.1:5672`
- **Message Consumer**: Listens to configured queues and processes messages
- **Dead Letter Queue**: Handles failed messages with retry mechanism
- **REST API**: Provides endpoints for testing and monitoring
- **Message Processing**: Supports different message types (order, payment, notification, user)
- **Monitoring**: Includes metrics and health checks via Spring Actuator
- **Docker Support**: Includes Docker Compose for RabbitMQ setup
```

## Usage

```
1. Run RabbitMQ:

podman run -d --name rabbitmq-server -p 5672:5672 -p 15672:15672 -e RABBITMQ_DEFAULT_USER=guest -e RABBITMQ_DEFAULT_PASS=guest rabbitmq:3.12-management

2. Run the Spring Boot app:

.\gradlew bootRun

3. Publish a message:

curl -X POST "http://localhost:8080/api/messages/publish" -H "Content-Type: application/json" -d "{\"id\": \"1\", \"content\": \"mycontent\", \"type\": \"mytype\"}" -v

4. The Spring Boot app's RabbitMQListener should receive and echo the message like:

Successfully processed message with ID: 1
```



