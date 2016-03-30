package com.citisense.vidklopcic.citisense.data.entities;
import android.util.Log;

import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.util.AQI;
import com.citisense.vidklopcic.citisense.util.Conversion;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;
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
    public static final int AVERAGES_POLLUTANTS = 0;
    public static final int AVERAGES_OTHER = 1;
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

    public boolean hasData() {
        return last_measurement != null;
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

    public static List<CitiSenseStation> getStationsInRadius(LatLng center, Double meters, List<CitiSenseStation> candidates) {
        List<CitiSenseStation> result = new ArrayList<>();
        for (CitiSenseStation candidate : candidates) {
            if (SphericalUtil.computeDistanceBetween(candidate.getLocation(), center) < Constants.Map.station_radius_meters) {
                result.add(candidate);
            }
        }
        return result;
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

    public Integer getConfigVersion() {
        return config_version;
    }

    public void setConfigVersion(Integer version) {
        config_version = version;
    }

    public static ArrayList<HashMap<String, Integer>> getAverages(ArrayList<CitiSenseStation> stations) {
        if (stations.size() == 0) return null;
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

    public static LatLngBounds getBounds(List<CitiSenseStation> stations) {
        Double southwest_lat = 85d;
        Double southwest_lng = 180d;
        Double northeast_lat = -85d;
        Double northeast_lng = -180d;

        if (stations.size() == 0) {
            return null;
        } else if (stations.size() == 1) {
            LatLng loc = stations.get(0).getLocation();
            southwest_lat = loc.latitude;
            southwest_lng = loc.longitude;
            northeast_lat = loc.latitude;
            northeast_lng = loc.longitude;
        } else {
            for (CitiSenseStation station : stations) {
                LatLng loc = station.getLocation();
                if (loc.latitude < southwest_lat) southwest_lat = loc.latitude;
                if (loc.longitude < southwest_lng) southwest_lng = loc.longitude;
                if (loc.latitude > northeast_lat) northeast_lat = loc.latitude;
                if (loc.longitude > northeast_lng) northeast_lng = loc.longitude;
            }
        }
        
        LatLng offset = Constants.Map.getStationRadiusOffset(stations.get(0).getLocation());
        return new LatLngBounds(
                new LatLng(southwest_lat-offset.latitude, southwest_lng-offset.longitude),
                new LatLng(northeast_lat+offset.latitude, northeast_lng+offset.longitude));
    }
}
