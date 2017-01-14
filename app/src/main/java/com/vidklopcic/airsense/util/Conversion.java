package com.vidklopcic.airsense.util;

import android.graphics.Color;

import com.vidklopcic.airsense.data.Constants;
import com.vidklopcic.airsense.data.entities.MeasuringStation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

public abstract class Conversion {
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

    public static Integer getAqi(String pollutant_name, Double value) {
        Integer aqi_val = null;
        switch (pollutant_name) {
            case Constants.ARSOStation.CO_KEY:
                aqi_val = AQI.CO.getAqi(value);
                break;
            case Constants.ARSOStation.NO2_KEY:
                aqi_val = AQI.NO2.getAqi(value);
                break;
            case Constants.ARSOStation.O3_KEY:
                aqi_val = AQI.O3.getAqi(value);
                break;
            case Constants.ARSOStation.PM10_KEY:
                aqi_val = AQI.PM10.getAqi(value);
                break;
            case Constants.ARSOStation.SO2_KEY:
                aqi_val = AQI.SO2.getAqi(value);
                break;
        }
        return aqi_val;
    }

    public static Double getValueByKey(String key, MeasuringStation station) {
        switch (key) {
            case Constants.ARSOStation.CO_KEY:
                return station.CO;
            case Constants.ARSOStation.NO2_KEY:
                return station.NO2;
            case Constants.ARSOStation.O3_KEY:
                return station.O3;
            case Constants.ARSOStation.PM10_KEY:
                return station.PM10;
            case Constants.ARSOStation.SO2_KEY:
                return station.SO2;
            default:
                return null;
        }
    }

    public static AQI getAQIbyKey(String key) {
        switch (key) {
            case Constants.ARSOStation.CO_KEY:
                return Conversion.AQI.CO;
            case Constants.ARSOStation.NO2_KEY:
                return Conversion.AQI.NO2;
            case Constants.ARSOStation.O3_KEY:
                return Conversion.AQI.O3;
            case Constants.ARSOStation.PM10_KEY:
                return Conversion.AQI.PM10;
            case Constants.ARSOStation.SO2_KEY:
                return Conversion.AQI.SO2;
            default:
                return null;
        }
    }

    public enum AQI {
        NO2(50, 100, 200, 349, 400, 500, 600, "ug/m3", 0xffe91e63),    // ppb
        O3(60, 120, 180, 305, 400, 500, 600, "ug/m3", 0xff03a9f4),    // ppb
        PM10(40, 75, 100, 354, 424, 504, 604, "ug/m3", 0xffff5722),  // ug/m3
        PM25(12, 35.4f, 55.4f, 150.4f, 250.4f, 350.4f, 500.4f, "ug/m3", 0xffffc107),  // ug/m3
        CO(4400f, 9400f, 12400f, 15400f, 30400f, 40400f, 50400f, "ppm", 0xff259b24),    // ppm
        SO2(50, 100, 350, 404, 504, 604, 700, "ug/m3", 0xffffc107);    // ppb
        public static final int GOOD_RANGE = 50;
        public static final int MODERATE_RANGE = 25;
        public static final int UNH_FOR_SENS_RANGE = 25;
        public static final int UNH_RANGE = 25;
        public static final int VERY_UNH_RANGE = 100;
        public static final int HAZ_RANGE = 100;
        public static final int VERY_HAZ_RANGE = 100;
        private float good, moderate, unh_for_sens, unh, very_unh, haz, very_haz;
        private int color;

        public String unit;

        AQI(float good, float moderate, float unh_for_sens, float unh, float very_unh, float haz, float very_haz, String unit, int color) {
            this.good = good;
            this.moderate = moderate;
            this.unh_for_sens = unh_for_sens;
            this.unh = unh;
            this.very_unh = very_unh;
            this.haz = haz;
            this.very_haz = very_haz;
            this.unit = unit;
            this.color = color;
        }

        public int getColor() {
            return color;
        }

        public Integer getAqi(Double C) {
            if (C==null || C < 0) return null;
            float Chigh, Clow, Ilow;
            int range;
            if (C < good) {
                Chigh = good;
                Clow = 0;
                range = GOOD_RANGE;
                Ilow = 0;
            } else if (C < moderate) {
                Chigh = moderate;
                Clow = good;
                range = MODERATE_RANGE;
                Ilow = Constants.AQI.MODERATE;
            } else if (C < unh_for_sens) {
                Chigh = unh_for_sens;
                Clow = moderate;
                range = UNH_FOR_SENS_RANGE;
                Ilow = Constants.AQI.UNHEALTHY_SENSITIVE;
            } else if (C < unh) {
                Chigh = unh;
                Clow = unh_for_sens;
                range = UNH_RANGE;
                Ilow = Constants.AQI.UNHEALTHY;
            } else if (C < very_unh) {
                Chigh = very_unh;
                Clow = unh;
                range = VERY_UNH_RANGE;
                Ilow = Constants.AQI.VERY_UNHEALTHY;
            } else if (C < haz) {
                Chigh = haz;
                Clow = very_unh;
                range = HAZ_RANGE;
                Ilow = Constants.AQI.HAZARDOUS;
            } else {
                Chigh = very_haz;
                Clow = haz;
                range = VERY_HAZ_RANGE;
                Ilow = Constants.AQI.HAZARDOUS + range;
            }
            int aqi = (int) ((range / (Chigh - Clow)) * (C - Clow) + Ilow);
            return aqi < 501 ? aqi : 500;
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
