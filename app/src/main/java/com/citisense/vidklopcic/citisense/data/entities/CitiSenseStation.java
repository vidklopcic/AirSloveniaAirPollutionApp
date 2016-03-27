package com.citisense.vidklopcic.citisense.data.entities;
import android.util.Log;

import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.util.AQI;
import com.citisense.vidklopcic.citisense.util.Conversion;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.orm.SugarRecord;
import com.orm.dsl.Unique;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class CitiSenseStation extends SugarRecord {
    String city;
    String pollutants;
    Float lat;
    Float lng;
    @Unique
    String station_id;
    Integer config_version;
    String last_measurement;
    Long last_measurement_time;

    public CitiSenseStation() {}

    public CitiSenseStation(Integer config_version, String id, String city, JSONArray pollutants, Float lat, Float lng) {
        this.config_version = config_version;
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

    public JSONArray getLastMeasurement() {
        try {
            return new JSONArray(last_measurement);
        } catch (Exception e) {
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

    public void setCity(String city) {
        this.city = city;
    }

    public void setLocation(Float lat, Float lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public void setPollutants(JSONArray pollutants) {
        this.pollutants = pollutants.toString();
    }

    public Integer getMaxAqi() {
        JSONArray m = getLastMeasurement();
        if (m == null) return null;
        int max_aqi = 0;
        for (int i=0;i<m.length();i++) {
            try {
                JSONObject p = m.getJSONObject(i);
                Integer aqi = getAqi(
                        p.getString(Constants.CitiSenseStation.pollutant_name_key),
                        p.getDouble(Constants.CitiSenseStation.value_key));
                if (aqi != null && aqi > max_aqi) max_aqi = aqi;
            } catch (JSONException e) {
                return 0;
            }
        }
        return max_aqi;
    }

    public int getColor() {
        return AQI.getColor(getMaxAqi());
    }

    public static List<CitiSenseStation> getStationsInArea(LatLngBounds bounds) {
        Double b1 = bounds.northeast.latitude;
        Double b2 = bounds.southwest.latitude;
        Double b3 = bounds.northeast.longitude;
        Double b4 = bounds.southwest.longitude;
        return CitiSenseStation.find(
                CitiSenseStation.class, "lat < ? and lat > ? and lng < ? and lng > ?",
                b1.toString(), b2.toString(), b3.toString(), b4.toString());
    }

    @Override
    public boolean equals(Object obj) {
        CitiSenseStation station = (CitiSenseStation) obj;
        return station.getStationId().equals(this.station_id);
    }

    public static Integer getAqi(String pollutant_name, Double value) {
        Integer aqi_val = null;
        switch (pollutant_name) {
            case Constants.CitiSenseStation.CO_KEY:
                aqi_val = Conversion.AQI.CO.getAqi(value);
                break;
            case Constants.CitiSenseStation.NO2_KEY:
                aqi_val = Conversion.AQI.NO2.getAqi(value);
                break;
            case Constants.CitiSenseStation.O3_KEY:
                aqi_val = Conversion.AQI.O3.getAqi(value);
                break;
            case Constants.CitiSenseStation.PM10_KEY:
                aqi_val = Conversion.AQI.PM10.getAqi(value);
                break;
            case Constants.CitiSenseStation.PM2_5_KEY:
                aqi_val = Conversion.AQI.PM25.getAqi(value);
                break;
        }
        return aqi_val;
    }

    public static ArrayList<HashMap<String, Integer>> getAverages(ArrayList<CitiSenseStation> stations) {
        ArrayList<HashMap<String, Integer>> result = new ArrayList<>();
        HashMap<String, Integer> aqi = new HashMap<>();
        HashMap<String, Integer> other = new HashMap<>();
        for (CitiSenseStation station : stations) {
            JSONArray measurement = station.getLastMeasurement();
            if (measurement == null) return null;
            for (int i=0;i<measurement.length();i++) {
                try {
                    JSONObject pollutant = measurement.getJSONObject(i);
                    String pollutant_name = pollutant.getString(Constants.CitiSenseStation.pollutant_name_key);
                    Double value = pollutant.getDouble(Constants.CitiSenseStation.value_key);
                    Integer aqi_val = getAqi(pollutant_name, value);
                    if (aqi_val != null && 0 <= aqi_val && aqi_val <= Constants.AQI.SUM) {
                        if (aqi.containsKey(pollutant_name)) {
                            aqi.put(pollutant_name, (aqi.get(pollutant_name) + aqi_val)/2);
                        } else {
                            aqi.put(pollutant_name, aqi_val);
                        }
                    } else {
                        if (other.containsKey(pollutant_name)) {
                            other.put(pollutant_name, (other.get(pollutant_name) + value.intValue())/2);
                        } else {
                            other.put(pollutant_name, value.intValue());
                        }
                    }

                } catch (JSONException ignored) {}
            }
        }
        result.add(aqi);
        result.add(other);
        return result;
    }
}
