package com.vidklopcic.airsense.util;

import com.vidklopcic.airsense.data.Constants;
import com.vidklopcic.airsense.data.entities.StationMeasurement;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import static com.vidklopcic.airsense.util.Conversion.getAqi;

public abstract class PollutantsChart {
    public static HashMap<String, ArrayList<Entry>> measurementsToYData(Long start_date, Integer tick_interval_millis, List<StationMeasurement> measurements) {
        HashMap<String, ArrayList<Entry>> ydata = new HashMap<>();
        for (StationMeasurement measurement : measurements) {
            if (!ydata.containsKey(measurement.getProperty()))
                ydata.put(measurement.getProperty(), new ArrayList<Entry>());

            if (Constants.AQI.supported_pollutants.contains(measurement.getProperty())) {
                Integer aqi_val = getAqi(measurement.getProperty(), measurement.getValue());
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

    public static String xAxisValueFormatter(Integer index, Long start_date, Integer tick_interval_millis) {
        Calendar cal = Calendar.getInstance();
        if (start_date == null) return "";
        cal.setTimeInMillis(start_date + tick_interval_millis * index);
        cal.setTimeZone(TimeZone.getDefault());
        if (cal.get(Calendar.MINUTE) == 0) {
            return String.valueOf(Conversion.zfill(cal.get(Calendar.HOUR_OF_DAY), 2));
        }
        return String.valueOf(Conversion.zfill(cal.get(Calendar.HOUR_OF_DAY), 2)) + ":"
                + String.valueOf(Conversion.zfill(cal.get(Calendar.MINUTE), 2));
    }

    public static class EntryComparator implements Comparator<Entry> {

        @Override
        public int compare(Entry lhs, Entry rhs) {
            return Integer.valueOf(lhs.getXIndex()).compareTo(rhs.getXIndex());
        }
    }
}
