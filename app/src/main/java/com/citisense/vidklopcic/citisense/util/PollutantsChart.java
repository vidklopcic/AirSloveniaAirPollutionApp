package com.citisense.vidklopcic.citisense.util;

import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.data.entities.StationMeasurement;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class PollutantsChart {
    public static HashMap<String, ArrayList<Entry>> measurementsToYData(Long start_date, Integer tick_interval_millis, List<StationMeasurement> measurements) {
        HashMap<String, ArrayList<Entry>> ydata = new HashMap<>();
        for (StationMeasurement measurement : measurements) {
            if (!ydata.keySet().contains(measurement.getProperty()))
                ydata.put(measurement.getProperty(), new ArrayList<Entry>());

            if (Constants.AQI.supported_pollutants.contains(measurement.getProperty())) {
                Integer aqi_val = CitiSenseStation.getAqi(measurement.getProperty(), measurement.getValue());
                if (aqi_val != null && start_date != null && measurement.getMeasurementTime() != null) {
                    ydata.get(measurement.getProperty()).add(new Entry(
                            aqi_val,
                            (int) (measurement.getMeasurementTime() - start_date) / tick_interval_millis));
                }
            } else {
                if (start_date != null) {
                    ydata.get(measurement.getProperty()).add(new Entry(
                            measurement.getValue().floatValue(),
                            (int) (measurement.getMeasurementTime() - start_date) / tick_interval_millis));
                }
            }
        }
        return ydata;
    }
}
