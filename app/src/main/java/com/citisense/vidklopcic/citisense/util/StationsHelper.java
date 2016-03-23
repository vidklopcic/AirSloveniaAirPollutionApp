package com.citisense.vidklopcic.citisense.util;

import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class StationsHelper {
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
