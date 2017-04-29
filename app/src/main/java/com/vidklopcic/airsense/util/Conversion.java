package com.vidklopcic.airsense.util;

import android.graphics.Color;

import com.vidklopcic.airsense.data.Constants;
import com.vidklopcic.airsense.data.Gson.PollutionMeasurement;
import com.vidklopcic.airsense.data.entities.MeasuringStation;
import com.vidklopcic.airsense.data.entities.StationMeasurement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;

public abstract class Conversion {
    public static class Time {
        public static String dateToString(Date date) {
            SimpleDateFormat format = new SimpleDateFormat(Constants.API.time_format);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.format(date);
        }

        public static Date stringToDate(String time) {
            SimpleDateFormat format = new SimpleDateFormat(Constants.API.time_format);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                return format.parse(time);
            } catch (ParseException e) {
                return null;
            }
        }
    }

    public static class IO {
        public static String inputStreamToString(InputStream is) {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            StringBuilder total = new StringBuilder();
            String line;
            try {
                while ((line = r.readLine()) != null) {
                    total.append(line);
                }
            } catch (IOException e) {
                return "";
            }
            return total.toString();
        }
    }

    public static PollutionMeasurement getValueByKey(String key, MeasuringStation station) {
        PollutionMeasurement result = new PollutionMeasurement();
        result.property = key;
        switch (key) {
            case Constants.ARSOStation.CO_KEY:
                result.ug_m3 = station.CO_ugm3;
                result.ppm = station.CO_ppm;
                result.aqi = station.CO_aqi;
                break;
            case Constants.ARSOStation.NO2_KEY:
                result.ug_m3 = station.NO2_ugm3;
                result.ppm = station.NO2_ppm;
                result.aqi = station.NO2_aqi;
                break;
            case Constants.ARSOStation.O3_KEY:
                result.ug_m3 = station.O3_ugm3;
                result.ppm = station.O3_ppm;
                result.aqi = station.O3_aqi;
                break;
            case Constants.ARSOStation.PM10_KEY:
                result.ug_m3 = station.PM10_ugm3;
                result.ppm = station.PM10_ppm;
                result.aqi = station.PM10_aqi;
                break;
            case Constants.ARSOStation.PM25_KEY:
                result.ug_m3 = station.PM25_ugm3;
                result.ppm = station.PM25_ppm;
                result.aqi = station.PM25_aqi;
                break;
            case Constants.ARSOStation.SO2_KEY:
                result.ug_m3 = station.SO2_ugm3;
                result.ppm = station.SO2_ppm;
                result.aqi = station.SO2_aqi;
                break;
        }
        return result;
    }

    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    public enum AQI {
        NO2(0xffe91e63),    // ppb
        O3(0xff03a9f4),    // ppb
        PM10(0xffff5722),  // ug/m3
        PM25(0xffffc107),  // ug/m3
        CO(0xff259b24),    // ppm
        SO2(0xffffc107);    // ppb
        private int color;

        AQI(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }

    public static AQI getPollutant(String pollutant) {
        return AQI.valueOf(pollutant);
    }

    public static Double sum(Collection<Double> coll) {
        Double sum = 0d;
        for (Double o : coll) {
            sum += o;
        }
        return sum;
    }

    private static float interpolate(float a, float b, float proportion) {
        Float shortest_angle = ((((b - a) % 360) + 540) % 360) - 180;
        return (a + (shortest_angle * proportion));
    }

    public static int interpolateColor(int a, int b, float proportion) {
        float[] hsva = new float[3];
        float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }
        return Color.HSVToColor(hsvb);
    }

    public static String zfill(Integer num, int size) {
        String s = num + "";
        while (s.length() < size) s = "0" + s;
        return s;
    }
}
