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
        DataStore.getRooms().clear();
        DataStore.getSensors().clear();
        DataStore.getReadings().clear();
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
}
