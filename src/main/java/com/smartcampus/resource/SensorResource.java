package com.smartcampus.resource;

import com.smartcampus.exception.ApiError;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.storage.DataStore;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/sensors")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    @GET
    public Collection<Sensor> getSensors(@QueryParam("type") String type) {
        Collection<Sensor> sensors = DataStore.getSensors().values();
        if (type == null || type.isBlank()) {
            return sensors;
        }

        List<Sensor> filtered = new ArrayList<>();
        for (Sensor sensor : sensors) {
            if (sensor.getType() != null && sensor.getType().equalsIgnoreCase(type)) {
                filtered.add(sensor);
            }
        }
        return filtered;
    }

    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("Invalid request", "Sensor body is required", 400))
                    .build();
        }

        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("Invalid request", "roomId is required", 400))
                    .build();
        }

        Room room = DataStore.getRooms().get(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException("Cannot create sensor: room does not exist for roomId=" + sensor.getRoomId());
        }

        if (sensor.getId() == null || sensor.getId().isBlank()) {
            sensor.setId(UUID.randomUUID().toString());
        }

        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        Map<String, Sensor> sensors = DataStore.getSensors();
        if (sensors.containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ApiError("Sensor already exists", "Sensor ID already in use", 409))
                    .build();
        }

        sensors.put(sensor.getId(), sensor);
        DataStore.getOrCreateReadingsForSensor(sensor.getId());
        room.getSensorIds().add(sensor.getId());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
