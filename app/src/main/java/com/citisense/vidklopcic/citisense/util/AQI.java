package com.citisense.vidklopcic.citisense.util;

import android.support.v4.content.ContextCompat;

import com.citisense.vidklopcic.citisense.R;
import com.citisense.vidklopcic.citisense.data.Constants;

/**
 * Various methods for calculating aqis dependent on AQI scale
 */
public abstract class AQI {
    public static int getColor(int aqi) {
        if (aqi < Constants.AQI.SUM/Constants.AQI.MODERATE) {
            return R.color.aqi_good;
        } else if (aqi < Constants.AQI.SUM/Constants.AQI.UNHEALTHY_SENSITIVE) {
            return R.color.aqi_moderate;
        } else if (aqi < Constants.AQI.SUM/Constants.AQI.UNHEALTHY) {
            return R.color.aqi_unhealthy_for_sensitive;
        } else if (aqi < Constants.AQI.SUM/Constants.AQI.VERY_UNHEALTHY) {
            return R.color.aqi_unhealthy;
        } else if (aqi < Constants.AQI.SUM/Constants.AQI.HAZARDOUS) {
            return R.color.aqi_very_unhealthy;
        } else {
            return R.color.aqi_hazardous;
        }
    }
}
