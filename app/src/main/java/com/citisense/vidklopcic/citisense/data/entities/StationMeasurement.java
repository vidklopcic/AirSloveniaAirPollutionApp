package com.citisense.vidklopcic.citisense.data.entities;

import com.orm.SugarRecord;
import java.util.Date;

public class StationMeasurement extends SugarRecord {
    Long measurement_time;
    String property;
    Double value;

    CitiSenseStation measuring_station;

    public StationMeasurement() {}

    public StationMeasurement(CitiSenseStation measuring_station, Long measurement_time, String property, Double value) {
        this.measuring_station = measuring_station;
        this.measurement_time = measurement_time;
        this.property = property;
        this.value = value;
    }

    public String getProperty() {
        return property;
    }

    public Date getMeasurementTime() {
        return new Date(measurement_time);
    }

    public Double getValue() {
        return value;
    }
}
