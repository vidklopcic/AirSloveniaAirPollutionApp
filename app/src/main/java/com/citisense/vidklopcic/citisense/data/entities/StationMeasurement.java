package com.citisense.vidklopcic.citisense.data.entities;

import io.realm.Realm;
import io.realm.RealmObject;

public class StationMeasurement extends RealmObject {
    Long measurement_time;
    String property;
    Double value;

    MeasuringStation measuring_station;

    public StationMeasurement() {}

    public static StationMeasurement create(Realm r, MeasuringStation measuring_station, Long measurement_time, String property, Double value) {
        r.beginTransaction();
        StationMeasurement measurement = r.createObject(StationMeasurement.class);
        measurement.measuring_station = measuring_station;
        measurement.measurement_time = measurement_time;
        measurement.property = property;
        measurement.value = value;
        r.commitTransaction();
        return measurement;
    }

    public static StationMeasurement createForNested(Realm r, MeasuringStation measuring_station, Long measurement_time, String property, Double value) {
        StationMeasurement measurement =  r.createObject(StationMeasurement.class);
        measurement.measuring_station = measuring_station;
        measurement.measurement_time = measurement_time;
        measurement.property = property;
        measurement.value = value;
        return measurement;
    }

    public String getProperty() {
        return property;
    }

    public Long getMeasurementTime() {
        if (measurement_time == null)
            return 0l;
        return measurement_time;
    }

    public Double getValue() {
        return value;
    }
}
