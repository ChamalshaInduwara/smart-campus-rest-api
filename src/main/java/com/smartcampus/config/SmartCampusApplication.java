package com.smartcampus.config;

import com.smartcampus.exception.GlobalExceptionMapper;
import com.smartcampus.exception.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.exception.RoomNotEmptyExceptionMapper;
import com.smartcampus.exception.SensorUnavailableExceptionMapper;
import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        classes.add(DiscoveryResource.class);
        classes.add(RoomResource.class);
        classes.add(SensorResource.class);

        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(GlobalExceptionMapper.class);

        classes.add(LoggingFilter.class);
        return classes;
    }
}
