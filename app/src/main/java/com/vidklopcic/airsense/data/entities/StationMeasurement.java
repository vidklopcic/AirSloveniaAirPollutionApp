package com.vidklopcic.airsense.data.entities;

import io.realm.Realm;
import io.realm.RealmObject;

public class StationMeasurement extends RealmObject {
    Long measurement_time;
    String property;
    Double ug_m3;
    Integer ppm;
    Integer aqi;
    Double other;

    MeasuringStation measuring_station;

    public StationMeasurement() {}

    public static StationMeasurement create(Realm r, MeasuringStation measuring_station, Long measurement_time, String property, Double ugm3, Integer ppm, Integer aqi, Double other) {
        r.beginTransaction();
        StationMeasurement measurement = r.createObject(StationMeasurement.class);
        measurement.measuring_station = measuring_station;
        measurement.measurement_time = measurement_time;
        measurement.property = property;
        measurement.ug_m3 = ugm3;
        measurement.ppm = ppm;
        measurement.aqi = aqi;
        measurement.other = other;
        r.commitTransaction();
        return measurement;
    }

    public static StationMeasurement createForNested(Realm r, MeasuringStation measuring_station, Long measurement_time, String property, Double ugm3, Integer ppm, Integer aqi, Double other) {
        StationMeasurement measurement =  r.createObject(StationMeasurement.class);
        measurement.measuring_station = measuring_station;
        measurement.measurement_time = measurement_time;
        measurement.property = property;
        measurement.ug_m3 = ugm3;
        measurement.ppm = ppm;
        measurement.aqi = aqi;
        measurement.other = other;
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

    public Integer getAqiValue() {
        return aqi;
    }
    public Double getUgM3Value() {
        return ug_m3;
    }
    public Integer getPPMValue() {
        return ppm;
    }
    public Double getOtherValue() {
        return other;
    }
}
