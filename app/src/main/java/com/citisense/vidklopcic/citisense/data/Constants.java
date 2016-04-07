package com.citisense.vidklopcic.citisense.data;

import com.citisense.vidklopcic.citisense.util.Overlay.MapOverlay;
import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;
import java.util.List;

public abstract class Constants {
    public static final int MILLI = 1000;
    public static final String TEMPERATURE_UNIT = "°C";
    public static final String HUMIDITY_UNIT = "%";

    public static class DataSources {
        public static final String config_version_url = "http://pastebin.com/raw/RQnbq08t";
        public static final String config_url = "http://pastebin.com/raw/P1dnQb0L";
        public static final int timeout = 20000;
    }

    public static class AQI {
        public static final float MODERATE = 51;
        public static final float UNHEALTHY_SENSITIVE = 101;
        public static final float UNHEALTHY = 151;
        public static final float VERY_UNHEALTHY = 201;
        public static final float HAZARDOUS = 301;
        public static final float SUM = 500;
        public static final int BAR_OFFSET = 20;
        public static final List<String> supported_pollutants = Arrays.asList(
                "CO",
                "NO2",
                "O3",
                "PM2.5",
                "PM10"
        );
    }

    public static class CitiSenseStation {
        public static final String date_format = "yyyy-MM-dd'T'HH:mm:ss.000";
        public static final String time_key = "start_time";
        public static final String pollutant_name_key = "observedproperty";
        public static String value_key = "value";
        public static final String last_measurement_url = "https://prod.citisense.snowflakesoftware.com/json/sensor/lastobservation?sensorid=";
        public static final String measurement_range_url_id = "{id}";
        public static final String measurement_range_url_start = "{start}";
        public static final String measurement_range_url_end = "{end}";
        public static final String measurement_range_url = "https://prod.citisense.snowflakesoftware.com/json/sensor/observationfinishtime/between?sensorid={id}&from={start}&to={end}";
        public static final int update_interval = 900 * MILLI;
        public static final String CO_KEY = "CO";
        public static final String NO2_KEY = "NO2";
        public static final String PM2_5_KEY = "PM2.5";
        public static final String PM10_KEY = "PM10";
        public static final String O3_KEY = "O3";
        public static final String HUMIDITY_KEY = "Relative Humidity";
        public static final String TEMPERATURE_KEY = "Temperature";
    }

    public static class Map {
        public static final int default_zoom = 16;
        public static final double station_radius_meters = 1000;
        public static final int max_overlay_resolution_meters = 200;
        public static final int default_overlay_resolution_pixels = 10;
        public static final double overlay_transparency = 0.5;

        public static LatLng getStationRadiusOffset(LatLng location) {
            return MapOverlay.getOffset(location, station_radius_meters);
        }
    }
}
