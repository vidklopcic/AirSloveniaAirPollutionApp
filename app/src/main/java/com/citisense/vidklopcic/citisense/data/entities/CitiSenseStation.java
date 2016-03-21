package com.citisense.vidklopcic.citisense.data.entities;
import android.util.Log;
import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.util.Conversion;
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
    Integer lat;
    Integer lng;
    @Unique
    String station_id;
    String last_measurement;
    Long last_measurement_time;

    public CitiSenseStation() {}

    public CitiSenseStation(String id, String city, JSONArray pollutants, Integer lat, Integer lng) {
        this.station_id = id;
        this.city = city;
        this.pollutants = pollutants.toString();
        this.lat = lat;
        this.lng = lng;
    }

    public void setMeasurement(JSONObject measurement) {
        this.last_measurement = measurement.toString();
        try {
            Date time = Conversion.Time.stringToDate(
                    Constants.CitiSenseStation.date_format,
                    measurement.getString(Constants.CitiSenseStation.time_key));
            last_measurement_time = time.getTime() / 1000;  // milliseconds since 1970 (/1000) -> seconds
        } catch (Exception e) {
            Log.d("CitiSenseStation", "error parsing measurement time");
        }
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
