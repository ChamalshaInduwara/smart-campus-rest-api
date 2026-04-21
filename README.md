# Smart Campus REST API (JAX-RS)

## 1. Project Title

Smart Campus REST API (JAX-RS)

## 2. Overview

This project is a Java 17 REST API for a Smart Campus monitoring scenario, developed for Client-Server Architectures coursework. It is implemented using JAX-RS (Jersey) and demonstrates resource-oriented design, HTTP semantics, exception mapping, and centralized logging.

## 3. Description

The API manages three core resources:

- Room: a physical campus space with capacity and linked sensor IDs.
- Sensor: a monitoring device assigned to one room.
- SensorReading: a timestamped measurement linked to one sensor.

Resource relationships and hierarchy:

- Room -> Sensors: one-to-many relationship represented by `sensorIds` in each room.
- Sensor -> SensorReadings: one-to-many relationship exposed as a nested resource path.
- Hierarchical URI design: `/sensors/{sensorId}/readings` models readings as children of a sensor.

## 4. Technology Stack

- Java 17
- Maven
- Jersey (JAX-RS)
- Grizzly HTTP server
- In-memory storage using `HashMap` and `ArrayList`

## 5. Project Structure

```text
src/main/java/com/smartcampus/
├── config/
│   ├── ServerLauncher.java
│   └── SmartCampusApplication.java
├── model/
│   ├── Room.java
│   ├── Sensor.java
│   └── SensorReading.java
├── resource/
│   ├── DiscoveryResource.java
│   ├── RoomResource.java
│   ├── SensorResource.java
│   └── SensorReadingResource.java
├── storage/
│   └── DataStore.java
├── exception/
│   ├── ApiError.java
│   ├── GlobalExceptionMapper.java
│   ├── LinkedResourceNotFoundException.java
│   ├── LinkedResourceNotFoundExceptionMapper.java
│   ├── RoomNotEmptyException.java
│   ├── RoomNotEmptyExceptionMapper.java
│   ├── SensorUnavailableException.java
│   └── SensorUnavailableExceptionMapper.java
└── filter/
    └── LoggingFilter.java
```

## 6. Build and Run Instructions

Compile the project:

```bash
mvn clean compile
```

Run the server (configured in Maven exec plugin):

```bash
mvn exec:java
```

Run server with explicit main class:

```bash
mvn exec:java -Dexec.mainClass="com.smartcampus.config.ServerLauncher"
```

Base URL:

```text
http://localhost:8080/api/v1
```

## 7. API Design Overview

Base path:

- `/api/v1`

Resource-based API design:

- `/rooms` represents room collection and item resources.
- `/sensors` represents sensor collection and item resources.
- `/sensors/{sensorId}/readings` represents readings scoped to one sensor.

REST principles applied:

- Resource identification through stable URIs.
- HTTP method semantics: `GET`, `POST`, `DELETE`.
- Stateless request handling (no server-side session).
- Representation in JSON using JAX-RS providers.
- Proper status code usage for success and failure paths.

## 8. Discovery Endpoint

Endpoint:

- `GET /api/v1`

Returned JSON structure:

- `apiName`
- `version`
- `adminContact`
- `resources` object containing:
  - `self`
  - `rooms`
  - `sensors`
  - `sensorReadings`

Example response:

```json
{
  "apiName": "Smart Campus API",
  "version": "v1",
  "adminContact": "admin@smartcampus.local",
  "resources": {
    "self": "/api/v1",
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors",
    "sensorReadings": "/api/v1/sensors/{id}/readings"
  }
}
```

HATEOAS explanation (theory):

The discovery response provides navigable links so clients can discover available resources from the API itself rather than hard-coding all endpoint paths. This improves client decoupling and supports safer API evolution.

## 9. Room Management

Endpoints:

- `GET /rooms`
- `POST /rooms`
- `GET /rooms/{id}`
- `DELETE /rooms/{id}`

Validation rules in `POST /rooms`:

- Body must be present.
- `name` is required and must not be blank.
- `capacity` must be greater than 0.
- `sensorIds` must be empty on creation.
- If `id` is missing, a UUID is generated.
- Duplicate room IDs return conflict.

DELETE idempotency explanation (theory):

`DELETE` is idempotent because repeating the same deletion request does not create additional side effects after the room has already been removed. The first call may return `204`, while later calls can return `404`; the final server state remains the same.

## 10. Sensor Management

Endpoints:

- `GET /sensors`
- `POST /sensors`
- `GET /sensors?type=...`

Validation and behavior:

- Body must be present.
- `roomId` is required.
- `type` is required.
- `roomId` must reference an existing room; otherwise a linked-resource exception is raised.
- `status` defaults to `ACTIVE` when missing.
- Allowed statuses: `ACTIVE`, `MAINTENANCE`.
- `currentValue` defaults to `0.0` when missing.
- On successful sensor creation, sensor ID is added to the parent room `sensorIds`.

`@Consumes` behavior explanation (theory):

`@Consumes(MediaType.APPLICATION_JSON)` defines that these endpoints accept JSON payloads. It enforces representation contracts and ensures request entity parsing is performed as JSON by the JAX-RS runtime.

Query parameter vs path parameter explanation (theory):

- Path parameters identify a specific resource (for example, `/sensors/{id}`).
- Query parameters express filtering or optional constraints on a collection (for example, `/sensors?type=CO2`).

## 11. Sensor Readings (Sub-Resource)

Sub-resource locator pattern:

- `SensorResource` exposes `@Path("/{sensorId}/readings")` and returns a `SensorReadingResource` instance for that sensor context.

Endpoints:

- `GET /sensors/{id}/readings`
- `POST /sensors/{id}/readings`

Why sub-resources improve design (theory):

- They keep child-resource logic localized to parent context.
- They produce intuitive hierarchical URIs.
- They improve cohesion by separating reading-specific behavior from general sensor collection logic.

Current value update behavior:

- After a successful reading POST, the sensor's `currentValue` is immediately updated to the new reading `value`.

## 12. Error Handling

Mapped exception outcomes:

- `RoomNotEmptyException` -> `409 Conflict`
- `LinkedResourceNotFoundException` -> `422 Unprocessable Entity`
- `SensorUnavailableException` -> `403 Forbidden`
- Unhandled exceptions (`Throwable`) -> `500 Internal Server Error`

Other handled API error outcomes in resources include:

- `400 Bad Request` for invalid payload fields.
- `404 Not Found` for missing room/sensor lookups.

Why `422` is better than `404` for linked-resource validation (theory):

`422` indicates that the request body is syntactically valid but semantically invalid (for example, creating a sensor with a `roomId` that does not exist). `404` is more appropriate when the requested URI resource itself is not found.

Security risk of stack traces (theory):

Returning stack traces to clients can reveal internal class names, packages, and execution paths, increasing attack surface. This project avoids that by returning sanitized JSON error payloads.

Sample JSON error response:

```json
{
  "error": "Linked resource not found",
  "message": "Cannot create sensor: room does not exist for roomId=room-x",
  "status": 422
}
```

## 13. Logging

Request and response logging is implemented with a provider filter that implements both `ContainerRequestFilter` and `ContainerResponseFilter`.

Logged details include:

- Incoming HTTP method and URI.
- Outgoing status code.
- End-to-end request duration in milliseconds.

Why filters are better than logging inside each method (theory):

Filters provide cross-cutting behavior in one place, avoiding duplication across resource methods, improving consistency, and reducing maintenance risk.

## 14. HTTP Status Codes

Status codes used by this implementation:

- `200 OK`: successful retrieval operations.
- `201 Created`: successful room/sensor/reading creation.
- `204 No Content`: successful room deletion.
- `400 Bad Request`: invalid request payload or validation failure.
- `403 Forbidden`: reading submission blocked for sensor in MAINTENANCE.
- `404 Not Found`: requested room or sensor not found.
- `409 Conflict`: duplicate identifiers or deleting room with linked sensors.
- `422 Unprocessable Entity`: linked referenced resource missing.
- `500 Internal Server Error`: unhandled server-side exceptions.

## 15. API Testing (Postman)

Primary testing tool used:

- Postman was used for manual endpoint testing and demo validation.
- Collection file: `Smart-Campus-API.postman_collection.json` (project root).

Postman test flow used:

1. Import `Smart-Campus-API.postman_collection.json`.
2. Confirm collection variable `baseUrl` is set to `http://localhost:8080/api/v1`.
3. Run requests in order:
   - Discovery: `GET {{baseUrl}}`
   - Rooms: create and fetch room
   - Sensors: create sensor and filter by type
   - Sensor Readings: add and fetch readings
   - Error Cases: invalid room link, maintenance reading rejection, delete room with linked sensor
4. Validate response codes and response bodies against expected behavior.
5. Use built-in Postman test scripts in create/delete/readings requests for quick status verification.

## 16. Quick cURL Commands

Set a reusable base URL variable first:

```bash
BASE_URL="http://localhost:8080/api/v1"
```

1. Get discovery metadata:

```bash
curl -i "$BASE_URL"
```

2. Create a room:

```bash
curl -i -X POST "$BASE_URL/rooms" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "room-a101",
    "name": "Engineering Lab A101",
    "capacity": 40,
    "sensorIds": []
  }'
```

3. List all rooms:

```bash
curl -i "$BASE_URL/rooms"
```

4. Create a sensor linked to a room:

```bash
curl -i -X POST "$BASE_URL/sensors" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "sensor-co2-1",
    "roomId": "room-a101",
    "type": "CO2",
    "status": "ACTIVE",
    "currentValue": 0.0
  }'
```

5. Filter sensors by type:

```bash
curl -i "$BASE_URL/sensors?type=CO2"
```

6. Add a reading for a sensor:

```bash
curl -i -X POST "$BASE_URL/sensors/sensor-co2-1/readings" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "reading-1",
    "timestamp": 1713657600000,
    "value": 612.7
  }'
```

7. List readings for a sensor:

```bash
curl -i "$BASE_URL/sensors/sensor-co2-1/readings"
```

8. Delete a room by ID:

```bash
curl -i -X DELETE "$BASE_URL/rooms/room-a101"
```

## 17. Quality Assurance

Automated testing is implemented with JUnit 5 and Jersey Test Framework.

Manual API testing was performed only in Postman using the project collection to verify normal flows and error scenarios (for example `403`, `409`, and `422` responses).

Run tests:

```bash
mvn test
```

Validated scenarios include:

- Discovery metadata correctness.
- Room payload validation rules.
- Sensor creation requiring valid `roomId`.
- Status normalization and default values for sensors.
- Reading timestamp validation (`long` epoch milliseconds).
- Reading blocked when sensor is in MAINTENANCE (`403`).
- Sensor `currentValue` update after reading POST.
- Duplicate reading ID conflict handling (`409`).
- Room deletion conflict when sensors are linked (`409`).
- Room deletion success when no sensors are linked (`204`).

## 18. Notes

- Storage is fully in memory (`HashMap` + `ArrayList`) via `DataStore`.
- No database is used.
- All data resets when the application restarts.
