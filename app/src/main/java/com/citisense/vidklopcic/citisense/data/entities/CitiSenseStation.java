package com.citisense.vidklopcic.citisense.data.entities;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.orm.SugarRecord;
import com.orm.dsl.Unique;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class CitiSenseStation extends SugarRecord {
    String city;
    String pollutants;
    Float lat;
    Float lng;
    @Unique
    String station_id;
    String last_measurement;
    Long last_measurement_time;

    public CitiSenseStation() {}

    public CitiSenseStation(String id, String city, JSONArray pollutants, Float lat, Float lng) {
        this.station_id = id;
        this.city = city;
        this.pollutants = pollutants.toString();
        this.lat = lat;
        this.lng = lng;
    }

    public void setLastMeasurement(String measurement) throws JSONException {
            JSONArray lm = new JSONArray(measurement);
            this.last_measurement = measurement;
            last_measurement_time = new Date().getTime();  // milliseconds since 1970
    }

    public JSONObject getLastMeasurement() {
        try {
            return new JSONObject(last_measurement);
        } catch (JSONException e) {
            Log.d("CitiSenseStation", "error parsing last_measurement");
            return null;
        }
    }

    public Long getLastMeasurementTime() {
        if (last_measurement_time == null) {
            return 0l;
        }
        return last_measurement_time;
    }

    public JSONArray getPollutants() {
        try {
            return new JSONArray(pollutants);
        } catch (JSONException e) {
            return null;
        }
    }

    public LatLng getLocation() {
        return new LatLng(lat, lng);
    }

    public String getCity() {
        return city;
    }

    public String getStationId() {
        return station_id;
    }
}
