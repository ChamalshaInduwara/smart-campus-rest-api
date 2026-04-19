package com.smartcampus.config;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

public class ServerLauncher {

    private static final Logger LOGGER = Logger.getLogger(ServerLauncher.class.getName());
    private static final String BIND_URI = "http://0.0.0.0:8080/";
    private static final String DISPLAY_URI = "http://localhost:8080/api/v1";

    public static HttpServer startServer() {
        return GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BIND_URI),
                ResourceConfig.forApplication(new SmartCampusApplication())
        );
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = startServer();
        LOGGER.info("Smart Campus API started at " + DISPLAY_URI);
        LOGGER.info("Press Enter to stop the server...");
        System.in.read();
        server.shutdownNow();
    }
}
