package com.citisense.vidklopcic.citisense.util;

import android.app.Activity;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;

import com.citisense.vidklopcic.citisense.R;
import com.citisense.vidklopcic.citisense.data.Constants;

/**
 * Various methods for calculating aqis dependent on AQI scale
 */
public abstract class AQI {
    public static int getColor(int aqi, Activity context) {
        return ContextCompat.getColor(context, getColor(aqi));
    }

    public static int getColor(int aqi) {
        if (aqi < Constants.AQI.MODERATE) {
            return R.color.aqi_good;
        } else if (aqi < Constants.AQI.UNHEALTHY_SENSITIVE) {
            return R.color.aqi_moderate;
        } else if (aqi < Constants.AQI.UNHEALTHY) {
            return R.color.aqi_unhealthy_for_sensitive;
        } else if (aqi < Constants.AQI.VERY_UNHEALTHY) {
            return R.color.aqi_unhealthy;
        } else if (aqi < Constants.AQI.HAZARDOUS) {
            return R.color.aqi_very_unhealthy;
        } else {
            return R.color.aqi_hazardous;
        }
    }

    public static int toText(int aqi) {
        if (aqi< Constants.AQI.MODERATE) {
            return R.string.aqi_summary_good_title;
        } else if (aqi < Constants.AQI.UNHEALTHY_SENSITIVE) {
            return R.string.aqi_summary_moderate_title;
        } else if (aqi < Constants.AQI.UNHEALTHY) {
            return R.string.aqi_summary_unhealthy_for_sensitive_title;
        } else if (aqi < Constants.AQI.VERY_UNHEALTHY) {
            return R.string.aqi_summary_unhealthy_title;
        } else if (aqi < Constants.AQI.HAZARDOUS) {
            return R.string.aqi_summary_very_unhealthy_title;
        } else {
            return R.string.aqi_summary_hazardous_title;
        }
    }

    public static int getColor(Float aqi) {
        return getColor(aqi.intValue());
    }

    public static int getLinearColor(Integer aqi, Activity c) {
        if (aqi < Constants.AQI.MODERATE) {
            return ContextCompat.getColor(c, R.color.aqi_good);
        } else if (aqi < Constants.AQI.UNHEALTHY_SENSITIVE) {
            int color1 = ContextCompat.getColor(c, R.color.aqi_moderate);
            int color2 = ContextCompat.getColor(c, R.color.aqi_unhealthy_for_sensitive);
            return interpolateColor(color1, color2, (aqi-Constants.AQI.MODERATE)/(Constants.AQI.UNHEALTHY_SENSITIVE-Constants.AQI.MODERATE));
        } else if (aqi < Constants.AQI.UNHEALTHY) {
            int color1 = ContextCompat.getColor(c, R.color.aqi_unhealthy_for_sensitive);
            int color2 = ContextCompat.getColor(c, R.color.aqi_unhealthy);
            return interpolateColor(color1, color2, (aqi-Constants.AQI.UNHEALTHY_SENSITIVE)/(Constants.AQI.UNHEALTHY-Constants.AQI.UNHEALTHY_SENSITIVE));
        } else if (aqi < Constants.AQI.VERY_UNHEALTHY) {
            int color1 = ContextCompat.getColor(c, R.color.aqi_unhealthy);
            int color2 = ContextCompat.getColor(c, R.color.aqi_very_unhealthy);
            return interpolateColor(color1, color2, (aqi-Constants.AQI.UNHEALTHY)/(Constants.AQI.VERY_UNHEALTHY-Constants.AQI.UNHEALTHY));
        } else if (aqi < Constants.AQI.HAZARDOUS) {
            int color1 = ContextCompat.getColor(c, R.color.aqi_very_unhealthy);
            int color2 = ContextCompat.getColor(c, R.color.aqi_hazardous);
            return interpolateColor(color1, color2, (aqi-Constants.AQI.VERY_UNHEALTHY)/(Constants.AQI.HAZARDOUS-Constants.AQI.VERY_UNHEALTHY));
        } else {
            return ContextCompat.getColor(c, R.color.aqi_hazardous);
        }
    }

    private static float interpolate(float a, float b, float proportion) {
        Float shortest_angle=((((b - a) % 360) + 540) % 360) - 180;
        return (a + (shortest_angle * proportion));
    }

    /** Returns an interpoloated color, between <code>a</code> and <code>b</code> */
    private static int interpolateColor(int a, int b, float proportion) {
        float[] hsva = new float[3];
        float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }
        return Color.HSVToColor(hsvb);
    }
}
