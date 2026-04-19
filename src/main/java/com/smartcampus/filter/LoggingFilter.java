package com.smartcampus.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.logging.Logger;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());
    private static final String START_TIME_PROPERTY = "smartcampus.request.startNano";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestContext.setProperty(START_TIME_PROPERTY, System.nanoTime());
        LOGGER.info("Incoming request: " + requestContext.getMethod() + " " + requestContext.getUriInfo().getRequestUri());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        long durationMs = -1L;
        Object start = requestContext.getProperty(START_TIME_PROPERTY);
        if (start instanceof Long startNano) {
            durationMs = (System.nanoTime() - startNano) / 1_000_000;
        }

        LOGGER.info("Outgoing response: "
                + requestContext.getMethod() + " "
                + requestContext.getUriInfo().getRequestUri()
                + " -> " + responseContext.getStatus()
                + (durationMs >= 0 ? " (" + durationMs + " ms)" : ""));
    }
}
