package com.vidklopcic.airsense.data.entities;

import android.util.Log;

import com.vidklopcic.airsense.data.Constants;
import com.vidklopcic.airsense.data.Gson.Measurement;
import com.vidklopcic.airsense.data.Gson.OtherMeasurement;
import com.vidklopcic.airsense.data.Gson.PollutionMeasurement;
import com.vidklopcic.airsense.data.Gson.Station;
import com.vidklopcic.airsense.data.Serializers.ARSOStation;
import com.vidklopcic.airsense.util.AQI;
import com.vidklopcic.airsense.util.Conversion;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;

import org.json.JSONArray;
import org.json.JSONException;

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
    Long last_measurement_time;
    Long last_update_time;
    Long oldest_stored_measurement;
    Long oldest_range_request;

    public Double PM25_ugm3;
    public Integer PM25_ppm;
    public Integer PM25_aqi;

    public Double PM10_ugm3;
    public Integer PM10_ppm;
    public Integer PM10_aqi;

    public Double SO2_ugm3;
    public Integer SO2_ppm;
    public Integer SO2_aqi;

    public Double O3_ugm3;
    public Integer O3_ppm;
    public Integer O3_aqi;

    public Double CO_ugm3;
    public Integer CO_ppm;
    public Integer CO_aqi;

    public Double NO2_ugm3;
    public Integer NO2_ppm;
    public Integer NO2_aqi;

    public Double temperature;
    public Double humidity;

    public MeasuringStation() {
    }

    public static MeasuringStation create(Realm r, ARSOStation data) {
        return findById(r, data.id);
    }

    public static MeasuringStation updateOrCreate(Realm r, Station data) {
        MeasuringStation station;
        r.beginTransaction();
        station = findById(r, data.station_id);
        if (station == null) {
            station = r.createObject(MeasuringStation.class);
        }
        station.id = data.station_id;
        station.city = data.district;
        station.pollutants = "[\"PM10\", \"SO2\", \"O3\", \"CO\", \"NO2\"]";
        station.lat = data.lat;
        station.lng = data.lng;
        r.commitTransaction();
        return station;
    }

    public Long getLastMeasurementTime() {
        return last_measurement_time;
    }

    public void setLastMeasurementTime(Realm r, Long time) {
        r.beginTransaction();
        last_measurement_time = time;
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

    public ArrayList<Object> getRaw(String pollutant) {
        ArrayList<Object> result = new ArrayList<>();
        PollutionMeasurement m = Conversion.getValueByKey(pollutant, this);
        if (m.ug_m3 != null) {
            result.add(m.ug_m3);
            result.add("μg/m³");
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

    public void clearLastMeasurement() {
        CO_aqi = null;
        CO_ppm = null;
        CO_ugm3 = null;

        O3_aqi = null;
        O3_ppm = null;
        O3_ugm3 = null;

        SO2_aqi = null;
        SO2_ppm = null;
        SO2_ugm3 = null;

        NO2_aqi = null;
        NO2_ppm = null;
        NO2_ugm3 = null;

        PM10_aqi = null;
        PM10_ppm = null;
        PM10_ugm3 = null;

        PM25_aqi = null;
        PM25_ppm = null;
        PM25_ugm3 = null;
    }

    public void setMeasurement(Realm r, Measurement measurement) {
        r.beginTransaction();
        List<PollutionMeasurement> pollutants = measurement.getPollutants();
        List<OtherMeasurement> others = measurement.getOthers();
        for (PollutionMeasurement m : pollutants) {
            setProperty(m.property, m.ug_m3, m.ppm, m.aqi);
        }
        for (OtherMeasurement m : others) {
            setProperty(m.property, m.value);
        }
        r.commitTransaction();
    }

    public void setProperty(String property, Double other) {
        setProperty(property, null, null, null, other);
    }

    public void setProperty(String property, Double ugm3, Integer ppm, Integer aqi) {
        setProperty(property, ugm3, ppm, aqi, null);
    }

    public void setProperty(String property, Double ugm3, Integer ppm, Integer aqi, Double other) {
        switch (property){
            case "CO":
                CO_aqi = aqi;
                CO_ppm = ppm;
                CO_ugm3 = ugm3;
                break;
            case "O3":
                O3_aqi = aqi;
                O3_ppm = ppm;
                O3_ugm3 = ugm3;
                break;
            case "NO2":
                NO2_aqi = aqi;
                NO2_ppm = ppm;
                NO2_ugm3 = ugm3;
                break;
            case "SO2":
                SO2_aqi = aqi;
                SO2_ppm = ppm;
                SO2_ugm3 = ugm3;
                break;
            case "PM10":
                PM10_aqi = aqi;
                PM10_ppm = ppm;
                PM10_ugm3 = ugm3;
                break;
            case "PM2.5":
                PM10_aqi = aqi;
                PM10_ppm = ppm;
                PM10_ugm3 = ugm3;
                break;
            case "humidity":
                humidity = other;
                break;
            case "temperature":
                temperature = other;
                break;
            default:
                return;
        }
        last_update_time = new Date().getTime();
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

    public boolean hasCachedData() {
        for (int i = 0; i < Constants.AQI.supported_pollutants.size(); i++) {
            Integer aqi = getAqi(Constants.AQI.supported_pollutants.get(i));
            if (aqi != null) {
                return true;
            }
        }
        return false;
    }

    public boolean hasUpdatedData() {
        if (!wasUpdated()) {
            return false;
        }
        return hasCachedData();
    }

    public boolean wasUpdated() {
        if (new Date().getTime() - getLastUpdateTime() < Constants.ARSOStation.update_interval) {
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
        PollutionMeasurement m = Conversion.getValueByKey(pollutant_name, this);
        return m.aqi;
    }

    public static ArrayList<HashMap<String, Integer>> getAverages(List<MeasuringStation> stations) {
        if (stations == null || stations.size() == 0) return null;
        ArrayList<HashMap<String, Integer>> result = new ArrayList<>();
        HashMap<String, Integer> aqi = new HashMap<>();
        HashMap<String, Integer> other = new HashMap<>();
        for (MeasuringStation station : stations) {
            for (int i = 0; i < Constants.AQI.supported_pollutants.size(); i++) {
                String pollutant_name = Constants.AQI.supported_pollutants.get(i);
                Integer aqi_val = station.getAqi(pollutant_name);
                if (aqi_val != null && 0 <= aqi_val && aqi_val <= Constants.AQI.SUM) {
                    if (aqi.containsKey(pollutant_name)) {
                        aqi.put(pollutant_name, (aqi.get(pollutant_name) + aqi_val) / 2);
                    } else {
                        aqi.put(pollutant_name, aqi_val);
                    }
                }
            }

            if (station.temperature != null) {
                if (other.containsKey(Constants.ARSOStation.TEMPERATURE_KEY)) {
                    other.put(Constants.ARSOStation.TEMPERATURE_KEY, (other.get(Constants.ARSOStation.TEMPERATURE_KEY) + station.temperature.intValue()) / 2);
                } else {
                    other.put(Constants.ARSOStation.TEMPERATURE_KEY, station.temperature.intValue());
                }
            }

            if (station.humidity != null) {
                if (other.containsKey(Constants.ARSOStation.HUMIDITY_KEY)) {
                    other.put(Constants.ARSOStation.HUMIDITY_KEY, (other.get(Constants.ARSOStation.HUMIDITY_KEY) + station.humidity.intValue()) / 2);
                } else {
                    other.put(Constants.ARSOStation.HUMIDITY_KEY, station.humidity.intValue());
                }
            }
        }
        result.add(aqi);
        result.add(other);
        return result;
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

    public long setMeasurements(Realm r, List<Measurement> measurements) {
        Long lmt = getLastMeasurementTime();
        if (lmt == null) {
            lmt = 0L;
        }
        r.beginTransaction();
        Long oldest_in_list = getOldestStoredMeasurementTime();
        for (int i = 0; i < measurements.size(); i++) {
            Date date = measurements.get(i).getTime();
            if (date != null) {
                for (PollutionMeasurement measurement : measurements.get(i).getPollutants()) {
                    if (getOldestRangeRequest() == null
                            || date.getTime() < getOldestStoredMeasurementTime()
                            || date.getTime() > getLastMeasurementTime()) {
                        if (lmt < date.getTime()) {
                            lmt = date.getTime();
                        }
                        if (oldest_in_list == null || oldest_in_list > date.getTime())
                            oldest_in_list = date.getTime();
                        StationMeasurement.createForNested(
                                r,
                                this,
                                date.getTime(),
                                measurement.property,
                                measurement.ug_m3,
                                measurement.ppm,
                                measurement.aqi,
                                null);
                    }
                }

                for (OtherMeasurement measurement : measurements.get(i).getOthers()) {
                    if (getOldestRangeRequest() == null
                            || date.getTime() < getOldestStoredMeasurementTime()
                            || date.getTime() > getLastMeasurementTime()) {

                        if (lmt <= date.getTime()) {
                            if (measurement.property.equals(Constants.ARSOStation.TEMPERATURE_KEY)) {
                                temperature = measurement.value;
                            } else if (measurement.property.equals(Constants.ARSOStation.HUMIDITY_KEY)) {
                                humidity = measurement.value;
                            }
                            lmt = date.getTime();
                        }
                        if (oldest_in_list == null || oldest_in_list > date.getTime())
                            oldest_in_list = date.getTime();
                        StationMeasurement.createForNested(
                                r,
                                this,
                                date.getTime(),
                                measurement.property,
                                null,
                                null,
                                null,
                                measurement.value);
                    }
                }
            }
        }

        if (oldest_in_list != null) {
            setOldestStoredMeasurement(oldest_in_list);
        }
        r.commitTransaction();
        return lmt;
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
        if (id_list == null) {
            return result;
        }
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
