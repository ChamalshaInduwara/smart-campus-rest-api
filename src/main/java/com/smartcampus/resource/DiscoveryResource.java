package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Map<String, Object> discover() {
        Map<String, Object> links = new LinkedHashMap<>();
        links.put("self", "/api/v1");
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        links.put("sensorReadings", "/api/v1/sensors/{id}/readings");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("apiName", "Smart Campus API");
        response.put("version", "v1");
        response.put("adminContact", "admin@smartcampus.local");
        response.put("resources", links);

        return response;
    }
}
