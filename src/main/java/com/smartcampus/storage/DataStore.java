package com.smartcampus.storage;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DataStore {

    private static final Map<String, Room> ROOMS = new HashMap<>();
    private static final Map<String, Sensor> SENSORS = new HashMap<>();
    private static final Map<String, List<SensorReading>> READINGS = new HashMap<>();

    static {
        seedDefaultRooms();
    }

    private DataStore() {
    }

    public static Map<String, Room> getRooms() {
        return ROOMS;
    }

    public static Map<String, Sensor> getSensors() {
        return SENSORS;
    }

    public static Map<String, List<SensorReading>> getReadings() {
        return READINGS;
    }

    public static List<SensorReading> getOrCreateReadingsForSensor(String sensorId) {
        return READINGS.computeIfAbsent(sensorId, key -> new ArrayList<>());
    }

    public static void clearAll() {
        ROOMS.clear();
        SENSORS.clear();
        READINGS.clear();
    }

    private static void seedDefaultRooms() {
        addSeedRoom("room-101", "Lab 101", 40);
        addSeedRoom("room-102", "Lecture Hall", 100);
    }

    private static void addSeedRoom(String id, String name, int capacity) {
        if (!ROOMS.containsKey(id)) {
            ROOMS.put(id, new Room(id, name, capacity, new ArrayList<>()));
        }
    }
}
