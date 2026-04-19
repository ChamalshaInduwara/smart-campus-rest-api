# Smart Campus REST API

A coursework-ready Java REST API for smart campus monitoring, implemented with JAX-RS (Jersey) and in-memory data structures only.

## Overview

This project demonstrates clean resource-oriented API design for managing rooms, sensors, and sensor readings. It is designed for Client-Server Architectures assessment, with emphasis on:

- Correct HTTP method and status usage
- Linked-resource validation
- Sub-resource locator usage
- Consistent exception-to-HTTP mapping
- Centralized request/response logging

## Description

The API manages three related entities:

- Room: physical campus space with capacity and linked sensors
- Sensor: device assigned to one room
- SensorReading: measurement history for one sensor

Data is persisted only in memory using HashMap and ArrayList, with no database integration.

## Technology Stack

- Java 17
- Maven
- JAX-RS / Jersey 3
- Grizzly HTTP server
- Jackson JSON support (via Jersey media module)
- JUnit 5 + Jersey Test Framework

## Project Structure

- src/main/java/com/smartcampus/config
- src/main/java/com/smartcampus/model
- src/main/java/com/smartcampus/resource
- src/main/java/com/smartcampus/storage
- src/main/java/com/smartcampus/exception
- src/main/java/com/smartcampus/filter
- src/test/java/com/smartcampus/resource

## Build, Test, and Run

1. Compile:

   mvn clean compile

2. Run automated tests:

   mvn clean test

3. Start server (plugin main class configured in pom.xml):

   mvn exec:java

4. Explicit main class command:

   mvn exec:java -Dexec.mainClass="com.smartcampus.config.ServerLauncher"

Base URL:

http://localhost:8080/api/v1

## API Design Overview

Base path:

- /api/v1

Primary resources:

- /rooms
- /sensors
- /sensors/{sensorId}/readings

Design decisions:

- Room to Sensor is one-to-many, represented by sensorIds on room
- Sensor to SensorReading is one-to-many, exposed using a sub-resource locator
- Resource linking is validated during writes (for example, sensor roomId must exist)

## Discovery Endpoint

Endpoint:

- GET /api/v1

Returns:

- apiName
- version
- adminContact
- resources map containing discoverable links:
  - self
  - rooms
  - sensors
  - sensorReadings template

## Core Endpoint Behavior

### Rooms

- GET /rooms returns all rooms
- POST /rooms creates room, returns 201 Created and Location header
- GET /rooms/{roomId} returns one room or 404
- DELETE /rooms/{roomId} returns:
  - 204 No Content when deleted
  - 409 Conflict when sensors are still linked

Validation:

- name is required
- capacity must be greater than 0
- sensorIds must be empty on creation (linking is controlled by sensor creation)

### Sensors

- GET /sensors returns all sensors
- GET /sensors?type=CO2 filters by sensor type
- GET /sensors/{sensorId} returns one sensor or 404
- POST /sensors creates sensor, returns 201 Created and Location header

Validation:

- roomId is required and must exist (422 if linked room does not exist)
- type is required
- status defaults to ACTIVE if absent
- allowed statuses are ACTIVE and MAINTENANCE
- currentValue defaults to 0.0 when absent
- created sensor ID is linked into room sensorIds

### Sensor Readings (Sub-resource)

- GET /sensors/{sensorId}/readings returns reading history
- POST /sensors/{sensorId}/readings creates reading, returns 201 Created and Location header

Validation:

- sensor must exist (422 when linked sensor missing)
- MAINTENANCE sensors reject POST with 403
- timestamp must be a positive long epoch-milliseconds value
- reading value is required
- reading ID must be unique per sensor
- successful POST updates sensor currentValue

## Working curl Commands

1. Discovery

   curl -X GET http://localhost:8080/api/v1

2. Create room

   curl -X POST http://localhost:8080/api/v1/rooms \
    -H "Content-Type: application/json" \
    -d "{\"id\":\"room-101\",\"name\":\"Lab 101\",\"capacity\":40,\"sensorIds\":[]}"

3. Get all rooms

   curl -X GET http://localhost:8080/api/v1/rooms

4. Create sensor linked to room

   curl -X POST http://localhost:8080/api/v1/sensors \
    -H "Content-Type: application/json" \
    -d "{\"id\":\"sensor-1\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0.0,\"roomId\":\"room-101\"}"

5. Filter sensors by type

   curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"

6. Add reading with long timestamp (epoch ms)

   curl -X POST http://localhost:8080/api/v1/sensors/sensor-1/readings \
    -H "Content-Type: application/json" \
    -d "{\"id\":\"reading-1\",\"timestamp\":1713607200000,\"value\":550.5}"

7. Attempt room deletion with linked sensor (expects 409)

   curl -X DELETE http://localhost:8080/api/v1/rooms/room-101

## Error Handling

Consistent JSON error payload:

```json
{
  "error": "<short error>",
  "message": "<human-readable detail>",
  "status": 400
}
```

Exception mapping:

- RoomNotEmptyException -> 409 Conflict
- LinkedResourceNotFoundException -> 422 Unprocessable Entity
- SensorUnavailableException -> 403 Forbidden
- GlobalExceptionMapper -> 500 Internal Server Error

Security and robustness note:

- Global exception responses avoid leaking stack traces or internals to API clients.

## Logging

A centralized filter implements:

- ContainerRequestFilter: logs incoming method and URI
- ContainerResponseFilter: logs method, URI, response status, and request duration

This keeps logging cross-cutting, consistent, and easy to explain in viva.

## In-Memory Storage Note

This project uses only in-memory structures:

- HashMap for rooms, sensors, and reading buckets
- ArrayList for room sensorIds and sensor reading history

No database, JPA, Hibernate, or repository framework is used.

## Quality Assurance

Automated API tests cover key coursework rules:

- Discovery metadata structure
- Sensor creation requires valid roomId
- Sensor linking into room sensorIds
- Reading accepts long timestamp
- Reading updates sensor currentValue
- MAINTENANCE sensors reject reading POST (403)
- Room deletion blocked while linked sensors exist (409)
- Room deletion returns 204 when room is empty
- Missing sensor lookup returns 404
- Duplicate reading ID for same sensor returns 409
- Room and sensor validation rules

Run tests:

mvn test

## Report and Viva Answer Support

### 1) JAX-RS resource lifecycle and in-memory data handling

By default, JAX-RS resource classes are request-scoped, so a new resource instance can be created per request. In this project, persistent state is intentionally stored in static in-memory collections inside DataStore. This separates transient request handling from shared application data and makes lifecycle behavior predictable for coursework demos.

### 2) HATEOAS and hypermedia benefits

The discovery endpoint provides resource links, helping clients navigate available endpoints without hardcoding every URI. This improves client decoupling and makes API evolution safer because discoverable links can be updated centrally.

### 3) Returning room IDs vs full room objects

Returning sensorIds within room keeps payloads small, avoids unnecessary nested data transfer, and prevents over-fetching. It also avoids accidental circular object graphs and keeps resource boundaries clear.

### 4) Consequences of @Consumes(MediaType.APPLICATION_JSON) mismatch

If a client sends an unsupported Content-Type while the resource expects JSON, request deserialization fails and the server can return a client error (typically 415 Unsupported Media Type or 400 depending on handler path). Declaring @Consumes clearly enforces contract correctness.

### 5) Why query parameters are better for filtering than path segments

Filtering (for example, /sensors?type=CO2) does not identify a different resource identity; it identifies a view of a collection. Query parameters are therefore the correct REST mechanism for optional filtering criteria.

### 6) Benefits of sub-resource locator pattern

Using /sensors/{id}/readings modeled via sub-resource locator keeps child-resource logic cohesive and context-aware. It naturally scopes reading operations to the parent sensor and keeps URI design hierarchical and intuitive.

### 7) Why 422 is more accurate than 404 for linked resource validation

For payloads that are syntactically valid but semantically invalid (for example, roomId references a non-existent room during sensor creation), 422 better communicates semantic validation failure than 404, which is typically for the requested endpoint/resource URI itself.

### 8) Risks of exposing stack traces

Exposing stack traces leaks internal class names, code paths, and implementation details that can aid misuse and reduce security posture. Sanitized error bodies are safer and professionally expected.

### 9) Why filters are better for cross-cutting logging

Filters apply uniformly to all endpoints, reducing duplication and guaranteeing consistent request/response logging behavior. This improves maintainability and keeps resource classes focused on business logic.

### 10) DELETE idempotency explanation

DELETE is idempotent because repeating the same delete request should not produce additional side effects after the resource is already removed. In practice, first call may return success, repeated calls may return not found, but server state remains unchanged after the first successful deletion.
