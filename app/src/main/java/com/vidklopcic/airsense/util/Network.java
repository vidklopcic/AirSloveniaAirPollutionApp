package com.vidklopcic.airsense.util;

import com.vidklopcic.airsense.data.Constants;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by vidklopcic on 22/03/16.
 */
public abstract class Network {
    public static String GET(String address) throws IOException {
        URL url = new URL(address);
        HttpURLConnection conn;
        conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(Constants.DataSources.timeout);
        conn.setConnectTimeout(Constants.DataSources.timeout);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();

        return Conversion.IO.inputStreamToString(conn.getInputStream());
    }
}
