package com.citisense.vidklopcic.citisense.util;

import android.graphics.Color;

import com.citisense.vidklopcic.citisense.data.Constants;

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

    public enum AQI {
        NO2(53,100,360,649,1249,1649,2049, "ppb"),    // ppb
        O3(54,70,85,105,200,504,604, "ppb"),    // ppb
        PM10(54,154,254,354,424,504,604, "ug/m3"),    // ug/m3
        PM25(12,35.4f,55.4f,150.4f,250.4f,350.4f,500.4f, "ug/m3"),    // ug/m3
        CO(4400f,9400f,12400f,15400f,30400f,40400f,50400f, "ppm"),    // ppm
        SO2(35,75,185,304,604,804,1004, "ppb");    // ppb
        public static final int GOOD_RANGE = 50;
        public static final int MODERATE_RANGE = 50;
        public static final int UNH_FOR_SENS_RANGE = 50;
        public static final int UNH_RANGE = 50;
        public static final int VERY_UNH_RANGE = 100;
        public static final int HAZ_RANGE = 100;
        public static final int VERY_HAZ_RANGE = 100;
        private float good, moderate, unh_for_sens, unh, very_unh, haz, very_haz;
        public String UNIT;
        AQI(float good, float moderate, float unh_for_sens, float unh, float very_unh, float haz, float very_haz, String unit) {
            this.good = good;
            this.moderate = moderate;
            this.unh_for_sens = unh_for_sens;
            this.unh = unh;
            this.very_unh = very_unh;
            this.haz = haz;
            this.very_haz = very_haz;
            this.UNIT = unit;
        }

        public Integer getAqi(Double C) {
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
            return (int) ((range / (Chigh - Clow)) *  (C - Clow) + Ilow);
        }
    }

    public static Double sum(Collection<Double> coll) {
        Double sum = 0d;
        for (Double o : coll) {
            sum += o;
        }
        return sum;
    }

    private static float interpolate(float a, float b, float proportion) {
        Float shortest_angle=((((b - a) % 360) + 540) % 360) - 180;
        return (a + (shortest_angle * proportion));
    }

    public static int interpolateColor(int a, int b, float proportion) {
        float[] hsva = new float[3];
        float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = Conversion.interpolate(hsva[i], hsvb[i], proportion);
        }
        return Color.HSVToColor(hsvb);
    }
}
