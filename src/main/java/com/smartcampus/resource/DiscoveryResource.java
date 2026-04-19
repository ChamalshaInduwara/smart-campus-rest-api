package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.Map;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Map<String, Object> discover() {
        Map<String, Object> links = new HashMap<>();
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");

        Map<String, Object> response = new HashMap<>();
        response.put("apiName", "Smart Campus API");
        response.put("version", "v1");
        response.put("adminContact", "admin@smartcampus.local");
        response.put("resources", links);

        return response;
    }
}
