package com.vidklopcic.airsense.data.entities;

import android.util.Log;

import com.vidklopcic.airsense.data.Constants;
import com.vidklopcic.airsense.data.Serializers.ARSOStation;
import com.vidklopcic.airsense.util.AQI;
import com.vidklopcic.airsense.util.Conversion;
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

public class MeasuringStation extends RealmObject {
    public interface MeasurementsTransactionListener {
        void onTransactionFinished(List<StationMeasurement> measurements);
    }

    public static final int AVERAGES_POLLUTANTS = 0;
    public static final int AVERAGES_OTHER = 1;
    public static final int RAW_VALUE = 0;
    public static final int RAW_UNIT = 1;
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
    Long oldest_range_request;
    public Double PM10;
    public Double SO2;
    public Double O3;
    public Double CO;
    public Double NO2;

    public MeasuringStation() {
    }

    public static MeasuringStation create(Realm r, ARSOStation data) {
        return findById(r, data.id);
    }

    public static MeasuringStation updateOrCreate(Realm r, ARSOStation data) {
        MeasuringStation station;
        r.beginTransaction();
        station = findById(r, data.id);
        if (station == null) {
            station = r.createObject(MeasuringStation.class);
        }
        station.id = data.id;
        station.city = data.place;
        station.pollutants = "[\"PM10\", \"SO2\", \"O3\", \"CO\", \"NO2\"]";
        station.lat = data.lat;
        station.lng = data.lng;
        station.CO = data.co;
        station.SO2 = data.so2;
        station.O3 = data.o3;
        station.PM10 = data.pm10;
        station.last_update_time = new Date().getTime();
        r.commitTransaction();
        return station;
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

    public HashMap<String, ArrayList<Object>> getPollutants() {
        HashMap<String, ArrayList<Object>> raw = new HashMap<>();
        for (String key : Constants.AQI.supported_pollutants) {
            ArrayList<Object> value = new ArrayList<>();
            Conversion.AQI aqi = Conversion.getAQIbyKey(key);
            value.add(aqi.unit);
            value.add(Conversion.getValueByKey(key, this));
        }
        return raw;
    }

    public ArrayList<Object> getRaw(String pollutant) {
        ArrayList<Object> result = new ArrayList<>();
        result.add(Conversion.getValueByKey(pollutant, this));
        Conversion.AQI a = Conversion.getAQIbyKey(pollutant);
        if (a != null) {
            result.add(a.unit);
            return result;
        }
        return null;
    }

    public static Long getMeasurementTime(JSONArray measurement) {
        try {
            Date date = stringToDate(measurement.getJSONObject(0).getString(Constants.ARSOStation.time_key));
            if (date == null) return null;
            return date.getTime();
        } catch (JSONException e) {
            Log.d("MeasuringStation", "date parsing failed");
        }
        return null;
    }

    public boolean hasPollutant(String pollutant) {
        return getAqi(pollutant) != null;
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
        int max_aqi = 0;
        for (int i = 0; i < Constants.AQI.supported_pollutants.size(); i++) {
            Integer aqi = getAqi(Constants.AQI.supported_pollutants.get(i));
            if (aqi != null) {
                max_aqi = max_aqi < aqi ? aqi : max_aqi;
            }
        }
        return max_aqi;
    }

    public boolean hasData() {
        if (new Date().getTime() - getLastUpdateTime() < Constants.MINUTES * Constants.SECONDS * Constants.MILLIS) {
            return true;
        } else {
            return false;
        }
    }

    public int getColor() {
        return AQI.getColor(getMaxAqi());
    }

    public static List<MeasuringStation> getStationsAroundPoint(Realm r, LatLng latLng, Double half_square_side) {
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

    public static List<MeasuringStation> getStationsInArea(Realm r, LatLngBounds bounds) {
        Double b1 = bounds.northeast.latitude;
        Double b2 = bounds.southwest.latitude;
        Double b3 = bounds.northeast.longitude;
        Double b4 = bounds.southwest.longitude;

        return r.where(MeasuringStation.class)
                .lessThan("lat", b1)
                .greaterThan("lat", b2)
                .lessThan("lng", b3)
                .greaterThan("lng", b4).findAll();
    }

    public static List<MeasuringStation> getStationsInRadius(LatLng center, Double meters, List<MeasuringStation> candidates) {
        List<MeasuringStation> result = new ArrayList<>();
        for (MeasuringStation candidate : candidates) {
            if (SphericalUtil.computeDistanceBetween(candidate.getLocation(), center) < Constants.Map.station_radius_meters) {
                result.add(candidate);
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        MeasuringStation station = (MeasuringStation) obj;
        return station.getStationId().equals(this.id);
    }

    public Integer getAqi(String pollutant_name) {
        Conversion.AQI a = Conversion.getAQIbyKey(pollutant_name);
        if (a != null) {
            return a.getAqi(Conversion.getValueByKey(pollutant_name, this));
        }
        return null;
    }

    public Integer getConfigVersion() {
        return config_version;
    }

    public void setConfigVersion(Realm r, Integer version) {
        r.beginTransaction();
        config_version = version;
        r.commitTransaction();
    }

    public static ArrayList<HashMap<String, Integer>> getAverages(List<MeasuringStation> stations) {
        if (stations.size() == 0) return null;
        ArrayList<HashMap<String, Integer>> result = new ArrayList<>();
        HashMap<String, Integer> aqi = new HashMap<>();
        HashMap<String, Integer> other = new HashMap<>();
        for (MeasuringStation station : stations) {
            for (int i = 0; i < Constants.AQI.supported_pollutants.size(); i++) {
                String pollutant_name = Constants.AQI.supported_pollutants.get(i);
                Double value = station.getPollutant(pollutant_name);
                Integer aqi_val = station.getAqi(pollutant_name);
                if (aqi_val != null && 0 <= aqi_val && aqi_val <= Constants.AQI.SUM) {
                    if (aqi.containsKey(pollutant_name)) {
                        aqi.put(pollutant_name, (aqi.get(pollutant_name) + aqi_val) / 2);
                    } else {
                        aqi.put(pollutant_name, aqi_val);
                    }
                }

// vcasih za humidity itd..
//                    } else {
//                        if (other.containsKey(pollutant_name)) {
//                            other.put(pollutant_name, (other.get(pollutant_name) + value.intValue())/2);
//                        } else {
//                            other.put(pollutant_name, value.intValue());
//                        }
//                    }

            }
        }
        result.add(aqi);
        result.add(other);
        return result;
    }

    private Double getPollutant(String pollutant_name) {
        return Conversion.getValueByKey(pollutant_name, this);
    }

    public static LatLngBounds getBounds(List<MeasuringStation> stations) {
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
            for (MeasuringStation station : stations) {
                LatLng loc = station.getLocation();
                if (loc.latitude < southwest_lat) southwest_lat = loc.latitude;
                if (loc.longitude < southwest_lng) southwest_lng = loc.longitude;
                if (loc.latitude > northeast_lat) northeast_lat = loc.latitude;
                if (loc.longitude > northeast_lng) northeast_lng = loc.longitude;
            }
        }

        LatLng offset = Constants.Map.getStationRadiusOffset(stations.get(0).getLocation());
        return new LatLngBounds(
                new LatLng(southwest_lat - offset.latitude, southwest_lng - offset.longitude),
                new LatLng(northeast_lat + offset.latitude, northeast_lng + offset.longitude));
    }

    public List<StationMeasurement> getMeasurementsInRange(Realm realm, Long start_utc, Long end_utc) {
        return realm.where(StationMeasurement.class)
                .equalTo("measuring_station.id", id)
                .greaterThan("measurement_time", start_utc)
                .lessThan("measurement_time", end_utc).findAll();
    }

    public void setMeasurements(Realm r, JSONArray measurements) {
        r.beginTransaction();
        Long oldest_in_list = null;
        for (int i = 0; i < measurements.length(); i++) {
            try {
                Date date = stringToDate(measurements.getJSONObject(i).getString(Constants.ARSOStation.time_key));
                if (date != null) {
                    JSONObject measurement = measurements.getJSONObject(i);
                    String pollutant = measurement.getString(Constants.ARSOStation.pollutant_name_key);

                    if (getOldestRangeRequest() == null
                            || date.getTime() < getOldestRangeRequest()
                            || date.getTime() > getLastRangeUpdateTime()) {
                        if (oldest_in_list == null || oldest_in_list > date.getTime())
                            oldest_in_list = date.getTime();
                        StationMeasurement.createForNested(
                                r,
                                this,
                                date.getTime() + 2 * Constants.MINUTES * Constants.SECONDS * Constants.MILLIS, // add 2 hours - tmp fix because of CitiSense server issue (UTC is off)
                                pollutant,
                                measurement.getDouble(Constants.ARSOStation.value_key));
                    }
                }
            } catch (JSONException ignored) {
            }
        }
        if (oldest_in_list != null) {
            setOldestStoredMeasurement(oldest_in_list);
        }
        r.commitTransaction();

    }

    public static Date stringToDate(String time) {
        SimpleDateFormat format = new SimpleDateFormat(Constants.ARSOStation.date_format);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return format.parse(time);
        } catch (ParseException e) {
            return null;
        }
    }

    public static String dateToString(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(Constants.ARSOStation.date_format);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(date);
    }

    public Long getOldestStoredMeasurementTime() {
        return oldest_stored_measurement;
    }

    public static List<String> stationsToIdList(List<MeasuringStation> stations) {
        try {
            if (stations == null) return new ArrayList<>();
            List<String> result = new ArrayList<>();
            for (MeasuringStation station : stations) {
                result.add(station.getStationId());
            }
            return result;
        } catch (IllegalStateException realm_instance_already_closed) {
            return new ArrayList<>();
        }
    }

    public static List<MeasuringStation> idListToStations(Realm realm, List<String> id_list) {
        List<MeasuringStation> result = new ArrayList<>();
        for (String id : id_list) {
            MeasuringStation station = realm.where(MeasuringStation.class).equalTo("id", id).findFirst();
            if (station != null) {
                result.add(station);
            }
        }
        return result;
    }

    public static MeasuringStation findById(Realm realm, String id) {
        return realm.where(MeasuringStation.class).equalTo("id", id).findFirst();
    }

    public Long getOldestRangeRequest() {
        return oldest_range_request;
    }

    public void setOldestRangeRequest(Realm r, Long time) {
        if (r != null) r.beginTransaction();
        oldest_range_request = time;
        if (r != null) r.commitTransaction();
    }
}
