package com.citisense.vidklopcic.citisense.data.entities;

import com.google.android.gms.maps.model.LatLng;
import com.orm.SugarRecord;
import com.orm.dsl.Unique;

import org.json.JSONArray;
import org.json.JSONException;

public class CitiSenseStation extends SugarRecord {
    String city;
    String pollutants;
    Integer lat;
    Integer lng;
    @Unique
    String station_id;

    public CitiSenseStation() {}

    public CitiSenseStation(String id, String city, JSONArray pollutants, Integer lat, Integer lng) {
        this.station_id = id;
        this.city = city;
        this.pollutants = pollutants.toString();
        this.lat = lat;
        this.lng = lng;
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
