package com.vidklopcic.airsense.util;

import com.github.mikephil.charting.components.LimitLine;
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

    public static LimitLine[] getLimitLines(Long start_date, Integer xDataSize, Integer tick_interval_millis) {
        if (start_date == null)
            return null;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(start_date+xDataSize*tick_interval_millis);
        cal.setTimeZone(TimeZone.getDefault());
        int hours_in_millis = cal.get(Calendar.HOUR_OF_DAY)*Constants.MINUTES*Constants.SECONDS*Constants.MILLIS;
        LimitLine l1 = new LimitLine(xDataSize-hours_in_millis/tick_interval_millis);
        LimitLine l2 = new LimitLine(
                (xDataSize-hours_in_millis/tick_interval_millis)
                - (Constants.HOURS*Constants.MINUTES*Constants.SECONDS*Constants.MILLIS)/tick_interval_millis);
        LimitLine l3 = new LimitLine(
                (xDataSize-hours_in_millis/tick_interval_millis)
                        - 2*(Constants.HOURS*Constants.MINUTES*Constants.SECONDS*Constants.MILLIS)/tick_interval_millis);
        LimitLine[] result = new LimitLine[3];
        l1.setLabel("today");
        l2.setLabel("yesterday");
        l3.setLabel("the day before yesterday");
        l1.setTextSize(13);
        l2.setTextSize(13);
        l3.setTextSize(13);
        l1.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        l2.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        l3.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        result[0] = l1;
        result[1] = l2;
        result[2] = l3;
        return result;
    }
}
