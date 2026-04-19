package com.smartcampus.resource;

import com.smartcampus.config.SmartCampusApplication;
import com.smartcampus.storage.DataStore;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartCampusApiTest extends JerseyTest {

    @Override
    protected Application configure() {
        return ResourceConfig.forApplication(new SmartCampusApplication());
    }

    @BeforeEach
    void resetInMemoryStore() {
                DataStore.clearAll();
    }

    @Test
    void discoveryEndpointReturnsMetadata() {
        Response response = target("/api/v1").request().get();
        assertEquals(200, response.getStatus());

        Map<String, Object> body = response.readEntity(new GenericType<>() {});
        assertEquals("Smart Campus API", body.get("apiName"));
        assertEquals("v1", body.get("version"));
        assertNotNull(body.get("adminContact"));
        assertNotNull(body.get("resources"));
    }

    @Test
    void sensorCreationValidatesRoomAndLinksSensorToRoom() {
        String roomJson = "{\"id\":\"room-t1\",\"name\":\"Lab\",\"capacity\":30,\"sensorIds\":[]}";
        Response createRoom = target("/api/v1/rooms")
                .request()
                .post(Entity.entity(roomJson, MediaType.APPLICATION_JSON));
        assertEquals(201, createRoom.getStatus());
        assertNotNull(createRoom.getLocation());

        String badSensorJson = "{\"id\":\"sensor-bad\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0.0,\"roomId\":\"missing-room\"}";
        Response badSensor = target("/api/v1/sensors")
                .request()
                .post(Entity.entity(badSensorJson, MediaType.APPLICATION_JSON));
        assertEquals(422, badSensor.getStatus());

        String goodSensorJson = "{\"id\":\"sensor-t1\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0.0,\"roomId\":\"room-t1\"}";
        Response goodSensor = target("/api/v1/sensors")
                .request()
                .post(Entity.entity(goodSensorJson, MediaType.APPLICATION_JSON));
        assertEquals(201, goodSensor.getStatus());
        assertNotNull(goodSensor.getLocation());

        Response getRoom = target("/api/v1/rooms/room-t1").request().get();
        Map<String, Object> roomBody = getRoom.readEntity(new GenericType<>() {});
        List<String> sensorIds = (List<String>) roomBody.get("sensorIds");
        assertTrue(sensorIds.contains("sensor-t1"));
    }

    @Test
    void postingReadingsUsesLongTimestampAndUpdatesCurrentValue() {
        target("/api/v1/rooms")
                .request()
                .post(Entity.entity("{\"id\":\"room-t2\",\"name\":\"Room T2\",\"capacity\":10,\"sensorIds\":[]}", MediaType.APPLICATION_JSON));

        target("/api/v1/sensors")
                .request()
                .post(Entity.entity("{\"id\":\"sensor-t2\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0.0,\"roomId\":\"room-t2\"}", MediaType.APPLICATION_JSON));

        Response addReading = target("/api/v1/sensors/sensor-t2/readings")
                .request()
                .post(Entity.entity("{\"id\":\"reading-t2\",\"timestamp\":1713607200000,\"value\":777.7}", MediaType.APPLICATION_JSON));
        assertEquals(201, addReading.getStatus());
        assertNotNull(addReading.getLocation());

        Map<String, Object> readingBody = addReading.readEntity(new GenericType<>() {});
        Object timestamp = readingBody.get("timestamp");
        assertTrue(timestamp instanceof Number);
        assertEquals(1713607200000L, ((Number) timestamp).longValue());

        Response sensorsResponse = target("/api/v1/sensors").request().get();
        List<Map<String, Object>> sensors = sensorsResponse.readEntity(new GenericType<>() {});
        Map<String, Object> sensor = sensors.stream()
                .filter(it -> "sensor-t2".equals(it.get("id")))
                .findFirst()
                .orElseThrow();
        assertEquals(777.7, ((Number) sensor.get("currentValue")).doubleValue(), 0.0001);
    }

    @Test
    void maintenanceSensorBlocksReadingsAndRoomDeletionConflictsWhenLinked() {
        target("/api/v1/rooms")
                .request()
                .post(Entity.entity("{\"id\":\"room-t3\",\"name\":\"Room T3\",\"capacity\":20,\"sensorIds\":[]}", MediaType.APPLICATION_JSON));

        target("/api/v1/sensors")
                .request()
                .post(Entity.entity("{\"id\":\"sensor-maint\",\"type\":\"TEMP\",\"status\":\"MAINTENANCE\",\"currentValue\":0.0,\"roomId\":\"room-t3\"}", MediaType.APPLICATION_JSON));

        Response blocked = target("/api/v1/sensors/sensor-maint/readings")
                .request()
                .post(Entity.entity("{\"id\":\"reading-maint\",\"timestamp\":1713607201234,\"value\":21.1}", MediaType.APPLICATION_JSON));
        assertEquals(403, blocked.getStatus());

        Response deleteRoom = target("/api/v1/rooms/room-t3").request().delete();
        assertEquals(409, deleteRoom.getStatus());

        Response readings = target("/api/v1/sensors/sensor-maint/readings").request().get();
        List<Map<String, Object>> readingList = readings.readEntity(new GenericType<>() {});
        assertFalse(readingList.stream().anyMatch(it -> "reading-maint".equals(it.get("id"))));
    }

    @Test
    void roomCreationRejectsInvalidPayload() {
        Response missingName = target("/api/v1/rooms")
                .request()
                .post(Entity.entity("{\"id\":\"room-bad-1\",\"capacity\":10,\"sensorIds\":[]}", MediaType.APPLICATION_JSON));
        assertEquals(400, missingName.getStatus());

        Response invalidCapacity = target("/api/v1/rooms")
                .request()
                .post(Entity.entity("{\"id\":\"room-bad-2\",\"name\":\"Bad\",\"capacity\":0,\"sensorIds\":[]}", MediaType.APPLICATION_JSON));
        assertEquals(400, invalidCapacity.getStatus());

        Response preLinkedSensors = target("/api/v1/rooms")
                .request()
                .post(Entity.entity("{\"id\":\"room-bad-3\",\"name\":\"Bad\",\"capacity\":5,\"sensorIds\":[\"sensor-x\"]}", MediaType.APPLICATION_JSON));
        assertEquals(400, preLinkedSensors.getStatus());
    }

    @Test
    void sensorCreationNormalizesStatusAndDefaultsCurrentValue() {
        target("/api/v1/rooms")
                .request()
                .post(Entity.entity("{\"id\":\"room-t4\",\"name\":\"Room T4\",\"capacity\":25,\"sensorIds\":[]}", MediaType.APPLICATION_JSON));

        Response createdSensor = target("/api/v1/sensors")
                .request()
                .post(Entity.entity("{\"id\":\"sensor-t4\",\"type\":\"TEMP\",\"status\":\"maintenance\",\"roomId\":\"room-t4\"}", MediaType.APPLICATION_JSON));
        assertEquals(201, createdSensor.getStatus());

        Map<String, Object> sensor = createdSensor.readEntity(new GenericType<>() {});
        assertEquals("MAINTENANCE", sensor.get("status"));
        assertEquals(0.0, ((Number) sensor.get("currentValue")).doubleValue(), 0.0001);

        Response invalidStatus = target("/api/v1/sensors")
                .request()
                .post(Entity.entity("{\"id\":\"sensor-t4-bad\",\"type\":\"TEMP\",\"status\":\"OFFLINE\",\"roomId\":\"room-t4\"}", MediaType.APPLICATION_JSON));
        assertEquals(400, invalidStatus.getStatus());
    }

    @Test
    void readingPostRejectsInvalidTimestamp() {
        target("/api/v1/rooms")
                .request()
                .post(Entity.entity("{\"id\":\"room-t5\",\"name\":\"Room T5\",\"capacity\":12,\"sensorIds\":[]}", MediaType.APPLICATION_JSON));

        target("/api/v1/sensors")
                .request()
                .post(Entity.entity("{\"id\":\"sensor-t5\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0.0,\"roomId\":\"room-t5\"}", MediaType.APPLICATION_JSON));

        Response badReading = target("/api/v1/sensors/sensor-t5/readings")
                .request()
                .post(Entity.entity("{\"id\":\"reading-t5\",\"timestamp\":0,\"value\":500.0}", MediaType.APPLICATION_JSON));
        assertEquals(400, badReading.getStatus());
    }

    @Test
    void roomDeletionReturnsNoContentWhenNoSensorsAssigned() {
        target("/api/v1/rooms")
                .request()
                .post(Entity.entity("{\"id\":\"room-t6\",\"name\":\"Room T6\",\"capacity\":16,\"sensorIds\":[]}", MediaType.APPLICATION_JSON));

        Response deleteRoom = target("/api/v1/rooms/room-t6").request().delete();
        assertEquals(204, deleteRoom.getStatus());
    }

    @Test
    void getSensorByIdReturnsNotFoundForMissingSensor() {
        Response response = target("/api/v1/sensors/missing-sensor").request().get();
        assertEquals(404, response.getStatus());
    }

    @Test
    void postingDuplicateReadingIdReturnsConflict() {
        target("/api/v1/rooms")
                .request()
                .post(Entity.entity("{\"id\":\"room-t7\",\"name\":\"Room T7\",\"capacity\":18,\"sensorIds\":[]}", MediaType.APPLICATION_JSON));

        target("/api/v1/sensors")
                .request()
                .post(Entity.entity("{\"id\":\"sensor-t7\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0.0,\"roomId\":\"room-t7\"}", MediaType.APPLICATION_JSON));

        Response first = target("/api/v1/sensors/sensor-t7/readings")
                .request()
                .post(Entity.entity("{\"id\":\"reading-dup\",\"timestamp\":1713607200001,\"value\":400.0}", MediaType.APPLICATION_JSON));
        assertEquals(201, first.getStatus());

        Response duplicate = target("/api/v1/sensors/sensor-t7/readings")
                .request()
                .post(Entity.entity("{\"id\":\"reading-dup\",\"timestamp\":1713607200002,\"value\":450.0}", MediaType.APPLICATION_JSON));
        assertEquals(409, duplicate.getStatus());
    }
}
