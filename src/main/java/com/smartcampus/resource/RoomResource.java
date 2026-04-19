package com.smartcampus.resource;

import com.smartcampus.exception.ApiError;
import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.storage.DataStore;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @GET
    public Collection<Room> getRooms() {
        return DataStore.getRooms().values();
    }

    @POST
    public Response createRoom(Room room) {
        if (room == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ApiError("Invalid request", "Room body is required", 400))
                    .build();
        }

        if (room.getId() == null || room.getId().isBlank()) {
            room.setId(UUID.randomUUID().toString());
        }

        if (room.getSensorIds() == null) {
            room.setSensorIds(new java.util.ArrayList<>());
        }

        Map<String, Room> rooms = DataStore.getRooms();
        if (rooms.containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ApiError("Room already exists", "Room ID already in use", 409))
                    .build();
        }

        rooms.put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError("Room not found", "No room with ID: " + roomId, 404))
                    .build();
        }

        return Response.ok(room).build();
    }

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRooms().get(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ApiError("Room not found", "No room with ID: " + roomId, 404))
                    .build();
        }

        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Cannot delete room because sensors are still assigned");
        }

        DataStore.getRooms().remove(roomId);
        return Response.ok(Map.of("message", "Room deleted successfully", "roomId", roomId)).build();
    }
}
