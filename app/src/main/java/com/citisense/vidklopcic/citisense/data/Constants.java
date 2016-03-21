package com.citisense.vidklopcic.citisense.data;

import java.util.ArrayList;

public abstract class Constants {
    public static class DataSources {
        public static final String config_version_url = "http://pastebin.com/raw/RQnbq08t";
        public static final String config_url = "http://pastebin.com/raw/P1dnQb0L";
        public static final int timeout = 1000;
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
}
