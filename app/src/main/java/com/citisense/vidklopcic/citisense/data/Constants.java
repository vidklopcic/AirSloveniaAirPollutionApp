package com.citisense.vidklopcic.citisense.data;

public abstract class Constants {
    public static final int MILLI = 1000;
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
    }

    public static class CitiSenseStation {
        public static final String date_format = "yyyy-MM-ddTHH:mm:ss.000";
        public static final String time_key = "start_time";
        public static final String pollutant_name_key = "observedproperty";
        public static String value_key = "value";
        public static final String last_measurement_url = "https://prod.citisense.snowflakesoftware.com/json/sensor/lastobservation?sensorid=";
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
    }
}
