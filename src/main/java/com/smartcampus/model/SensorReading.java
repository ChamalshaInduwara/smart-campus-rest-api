package com.smartcampus.model;

public class SensorReading {

    private String id;
    private String timestamp;
    private Double value;

    public SensorReading() {
    }

    public SensorReading(String id, String timestamp, Double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
