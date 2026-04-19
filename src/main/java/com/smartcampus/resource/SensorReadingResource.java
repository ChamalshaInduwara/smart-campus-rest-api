package com.smartcampus.resource;

import com.smartcampus.exception.ApiError;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.storage.DataStore;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        Sensor sensor = DataStore.getSensors().get(sensorId);
        if (sensor == null) {
            throw new LinkedResourceNotFoundException("Sensor not found for sensorId=" + sensorId);
        }

        List<SensorReading> readings = DataStore.getOrCreateReadingsForSensor(sensorId);
        return Response.ok(readings).build();
    }

    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = DataStore.getSensors().get(sensorId);
        if (sensor == null) {
            throw new LinkedResourceNotFoundException("Cannot add reading: sensor not found for sensorId=" + sensorId);
        }

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException("Cannot add reading: sensor is in MAINTENANCE mode");
        }

        if (reading == null || reading.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiError("Invalid request", "Reading body with value is required", 400))
                    .build();
        }

        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }

        if (reading.getTimestamp() == null || reading.getTimestamp().isBlank()) {
            reading.setTimestamp(Instant.now().toString());
        }

        List<SensorReading> readings = DataStore.getOrCreateReadingsForSensor(sensorId);
        readings.add(reading);

        sensor.setCurrentValue(reading.getValue());
        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
