# Smart Campus REST API (JAX-RS/Jersey)

This project is a Java Maven REST API for Smart Campus monitoring, developed for Client-Server Architectures coursework.
It follows JAX-RS (Jersey) principles, uses in-memory collections, and exposes clear resource-based endpoints.

## Description

The API manages rooms, sensors assigned to rooms, and sensor readings.
It is designed to demonstrate RESTful design, HTTP method usage, exception mapping, and request/response logging in a simple academic project.

## Tech stack

- Java 17
- Maven
- Jersey (JAX-RS)
- Grizzly HTTP server
- In-memory storage with HashMap and ArrayList

## Project structure

- src/main/java/com/smartcampus/config
- src/main/java/com/smartcampus/exception
- src/main/java/com/smartcampus/model
- src/main/java/com/smartcampus/resource
- src/main/java/com/smartcampus/storage
- src/main/java/com/smartcampus/filter

## Run instructions

1. Open terminal in the project root.
2. Compile the project:

   mvn clean compile

Or compile and run all automated tests:

mvn clean test

3. Start the server using the configured Maven execution:

   mvn exec:java

4. Explicit main class execution command:

   mvn exec:java -Dexec.mainClass="com.smartcampus.config.ServerLauncher"

5. The API base URL:

   http://localhost:8080/api/v1

## API Design Overview

Base path:

- /api/v1

Main resources:

- /rooms
- /sensors
- /sensors/{id}/readings

Resource relationships:

- Room -> Sensors: one room contains zero or more sensors.
- Sensor -> Readings: one sensor contains zero or more readings.

REST principles used:

- Resource-based URI design.
- Standard HTTP methods:
  - GET for retrieval
  - POST for creation
  - DELETE for removal
- Stateless request handling with JSON request/response bodies.

Discovery endpoint:

- GET /api/v1 returns API metadata including project name, version, admin contact, and resource links.

## API test commands (curl)

### 1) Discovery endpoint

    curl -X GET http://localhost:8080/api/v1

### 2) Create a room

    curl -X POST http://localhost:8080/api/v1/rooms \
      -H "Content-Type: application/json" \
      -d "{\"id\":\"room-101\",\"name\":\"Lab 101\",\"capacity\":40,\"sensorIds\":[]}"

### 3) Get all rooms

    curl -X GET http://localhost:8080/api/v1/rooms

### 4) Create a sensor

    curl -X POST http://localhost:8080/api/v1/sensors \
      -H "Content-Type: application/json" \
      -d "{\"id\":\"sensor-1\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0.0,\"roomId\":\"room-101\"}"

### 5) Filter sensors by type

    curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"

### 6) Add sensor reading

    curl -X POST http://localhost:8080/api/v1/sensors/sensor-1/readings \
      -H "Content-Type: application/json" \
      -d "{\"id\":\"reading-1\",\"timestamp\":1713607200000,\"value\":550.5}"

### 7) Delete room

    curl -X DELETE http://localhost:8080/api/v1/rooms/room-101

Note: Deleting a room with assigned sensors returns HTTP 409 (Room not empty).

## Error Handling

The API provides custom exceptions with mapped HTTP status codes:

- RoomNotEmptyException -> 409 Conflict
- LinkedResourceNotFoundException -> 422 Unprocessable Entity (or 400 depending on validation context)
- SensorUnavailableException -> 403 Forbidden
- GlobalExceptionMapper -> 500 Internal Server Error

Example JSON error format:

    {
      "error": "Room not empty",
      "message": "Cannot delete room because sensors are still assigned",
      "status": 409
    }

## Logging

Request and response logging is implemented using JAX-RS filters:

- ContainerRequestFilter logs incoming HTTP method and URI.
- ContainerResponseFilter logs outgoing HTTP status code.

This helps with debugging, endpoint verification, and viva demonstration.

## Notes

- All data is stored in memory using HashMap and ArrayList.
- No database is used.
- The project is intentionally simple and designed for coursework demonstration and viva explanation.

## Quality Assurance

The project includes automated API tests to validate core coursework rules:

- Discovery endpoint response structure.
- Sensor creation requires a valid roomId.
- Sensor is linked into room sensorIds.
- Reading accepts long timestamp (epoch milliseconds).
- Reading updates sensor currentValue.
- MAINTENANCE sensors reject reading POST (403).
- Rooms with assigned sensors cannot be deleted (409).

Run test suite:

mvn test
