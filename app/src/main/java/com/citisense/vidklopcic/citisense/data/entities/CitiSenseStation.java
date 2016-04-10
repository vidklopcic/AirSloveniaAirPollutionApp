package com.citisense.vidklopcic.citisense.data.entities;
import android.util.Log;

import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.util.AQI;
import com.citisense.vidklopcic.citisense.util.Conversion;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class CitiSenseStation extends RealmObject {
    public interface MeasurementsTransactionListener {
        void onTransactionFinished(List<StationMeasurement> measurements);
    }

    public static final int AVERAGES_POLLUTANTS = 0;
    public static final int AVERAGES_OTHER = 1;
    String city;
    String pollutants;
    Double lat;
    Double lng;
    @PrimaryKey
    String id;
    Integer config_version;
    Long last_range_update_time;
    Long last_update_time;
    String last_measurement;
    Long oldest_stored_measurement;

    public CitiSenseStation() {}

    public static CitiSenseStation create(Realm r, Integer config_version, String id, String city, JSONArray pollutants, Double lat, Double lng) {
        r.beginTransaction();
        CitiSenseStation station = r.createObject(CitiSenseStation.class);
        station.config_version = config_version;
        station.id = id;
        station.city = city;
        station.pollutants = pollutants.toString();
        station.lat = lat;
        station.lng = lng;
        r.commitTransaction();
        return station;
    }

    public void setLastMeasurement(Realm r, String measurement) throws JSONException {
        r.beginTransaction();
        last_update_time = new Date().getTime();
        last_measurement = measurement;
        r.commitTransaction();
    }

    public JSONArray getLastMeasurement() {
        if (last_measurement == null) return null;
        try {
            return new JSONArray(last_measurement);
        } catch (Exception e) {
            Log.d("CitiSenseStation", "error parsing last_measurement");
        }
        return null;
    }

    public Long getLastRangeUpdateTime() {
        return last_range_update_time;
    }

    public void setLastRangeUpdateTime(Realm r, Long time) {
        r.beginTransaction();
        last_range_update_time = time;
        r.commitTransaction();
    }

    public void setOldestStoredMeasurement(Long time) {
        oldest_stored_measurement = time;
    }

    public Long getLastUpdateTime() {
        if (last_update_time == null)
            return 0l;
        return last_update_time;
    }

    public JSONArray getPollutants() {
        try {
            return new JSONArray(pollutants);
        } catch (JSONException e) {
            return null;
        }
    }

    public static Long getMeasurementTime(JSONArray measurement) {
        try {
            Date date = stringToDate(measurement.getJSONObject(0).getString(Constants.CitiSenseStation.time_key));
            if (date == null) return null;
            return date.getTime();
        } catch (JSONException e) {
            Log.d("CitiSenseStation", "date parsing failed");
        }
        return null;
    }

    public boolean hasPollutant(String pollutant) {
        return getPollutantAqi(pollutant, getLastMeasurement()) != null;
    }

    public boolean hasPollutant(String pollutant, JSONArray measurement) {
        return getPollutantAqi(pollutant, measurement) != null;
    }

    public LatLng getLocation() {
        return new LatLng(lat, lng);
    }

    public String getCity() {
        return city;
    }

    public String getStationId() {
        return id;
    }

    public void setCity(Realm r, String city) {
        r.beginTransaction();
        this.city = city;
        r.commitTransaction();
    }

    public void setLocation(Realm r, Double lat, Double lng) {
        r.beginTransaction();
        this.lat = lat;
        this.lng = lng;
        r.commitTransaction();
    }

    public void setPollutants(Realm r, JSONArray pollutants) {
        r.beginTransaction();
        this.pollutants = pollutants.toString();
        r.commitTransaction();
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
        return getLastMeasurement() != null;
    }

    public int getColor() {
        return AQI.getColor(getMaxAqi());
    }

    public static List<CitiSenseStation> getStationsAroundPoint(Realm r, LatLng latLng, Double half_square_side) {
        return getStationsInArea(r, new LatLngBounds(
                SphericalUtil.computeOffset(
                        SphericalUtil.computeOffset(latLng, half_square_side, 270),
                        half_square_side,
                        180),
                SphericalUtil.computeOffset(
                        SphericalUtil.computeOffset(latLng, half_square_side, 0),
                        half_square_side,
                        90)
        ));
    }
    public static List<CitiSenseStation> getStationsInArea(Realm r, LatLngBounds bounds) {
        Double b1 = bounds.northeast.latitude;
        Double b2 = bounds.southwest.latitude;
        Double b3 = bounds.northeast.longitude;
        Double b4 = bounds.southwest.longitude;

        return r.where(CitiSenseStation.class)
                .lessThan("lat", b1)
                .greaterThan("lat", b2)
                .lessThan("lng", b3)
                .greaterThan("lng", b4).findAll();
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
        return station.getStationId().equals(this.id);
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

    public void setConfigVersion(Realm r, Integer version) {
        r.beginTransaction();
        config_version = version;
        r.commitTransaction();
    }

    public static ArrayList<HashMap<String, Integer>> getAverages(List<CitiSenseStation> stations) {
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

    public Integer getPollutantAqi(String pollutant, JSONArray measurement) {
        if (measurement == null) return null;
        try {
            for (int i = 0; i < measurement.length(); i++) {
                JSONObject m = measurement.getJSONObject(i);
                if (m.getString(Constants.CitiSenseStation.pollutant_name_key).equals(pollutant)) {
                    return getAqi(pollutant, m.getDouble(Constants.CitiSenseStation.value_key));
                }
            }
        } catch (JSONException ignored) {}
        return null;
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

    public List<StationMeasurement> getMeasurementsInRange(Realm realm, Long start, Long end) {
        return realm.where(StationMeasurement.class)
                .equalTo("measuring_station.id", id)
                .greaterThan("measurement_time", start)
                .lessThan("measurement_time", end).findAll();
    }

    public void setMeasurements(Realm r, JSONArray measurements) {
        r.beginTransaction();
        Long oldest_in_list = null;
        for (int i=0;i<measurements.length();i++) {
            try {
                Date date = stringToDate(measurements.getJSONObject(i).getString(Constants.CitiSenseStation.time_key));
                if (date != null) {
                    JSONObject measurement = measurements.getJSONObject(i);
                    String pollutant = measurement.getString(Constants.CitiSenseStation.pollutant_name_key);

                    if (getOldestStoredMeasurementTime() == null || date.getTime() < getOldestStoredMeasurementTime()) {
                        if (oldest_in_list == null || oldest_in_list > date.getTime())
                            oldest_in_list = date.getTime();
                        StationMeasurement.createForNested(
                                r,
                                this,
                                date.getTime(),
                                pollutant,
                                measurement.getDouble(Constants.CitiSenseStation.value_key));
                    }
                }
            } catch (JSONException ignored) {}
        }
        if (oldest_in_list != null) {
            setOldestStoredMeasurement(oldest_in_list);
        }
        r.commitTransaction();

    }

    public static Date stringToDate(String time) {
        SimpleDateFormat format = new SimpleDateFormat(Constants.CitiSenseStation.date_format);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return format.parse(time);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String dateToString(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(Constants.CitiSenseStation.date_format);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date);
    }

    public Long getOldestStoredMeasurementTime() {
        return oldest_stored_measurement;
    }

    public static List<String> stationsToIdList(List<CitiSenseStation> stations) {
        try {
            if (stations == null) return new ArrayList<>();
            List<String> result = new ArrayList<>();
            for (CitiSenseStation station : stations) {
                result.add(station.getStationId());
            }
            return result;
        } catch (IllegalStateException realm_instance_already_closed) {
            return new ArrayList<>();
        }
    }

    public static List<CitiSenseStation> idListToStations(Realm realm, List<String> id_list) {
        List<CitiSenseStation> result = new ArrayList<>();
        for (String id : id_list) {
            CitiSenseStation station = realm.where(CitiSenseStation.class).equalTo("id", id).findFirst();
            if (station != null) {
                result.add(station);
            }
        }
        return result;
    }

    public static CitiSenseStation idToStation(Realm realm, String id) {
        return realm.where(CitiSenseStation.class).equalTo("id", id).findFirst();
    }
}
