package com.citisense.vidklopcic.citisense.data;


import android.os.AsyncTask;
import android.util.Log;

import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.util.Conversion;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

public class DataAPI {
    public DataAPI() {
        getConfig();
    }

    private void updateThread() {

    }

    private void getConfig() {
        new LoadConfigTask().execute(Constants.DataSources.config_url);
    }

    class LoadConfigTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            URL url = null;
            try {
                url = new URL(params[0]);
                HttpURLConnection conn = null;
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(Constants.DataSources.timeout);
                conn.setConnectTimeout(Constants.DataSources.timeout);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();

                String result = Conversion.IO.inputStreamToString(conn.getInputStream());
                try {
                    CitiSenseStation.deleteAll(CitiSenseStation.class);
                    JSONObject config = new JSONObject(result);
                    Iterator<String> providers = config.keys();
                    while (providers.hasNext()) {
                        String provider_key = providers.next();
                        JSONObject provider = config.getJSONObject(provider_key);
                        Iterator<String> places = provider.keys();
                        while (places.hasNext()) {
                            String place_key = places.next();
                            JSONArray place = provider.getJSONArray(place_key);
                            for (int i=0;i<place.length();i++)  {
                                JSONArray station = place.getJSONArray(i);
                                String id = station.getString(0);
                                Integer lat = station.getInt(1);
                                Integer lng = station.getInt(2);
                                JSONArray pollutants = station.getJSONArray(3);
                                CitiSenseStation stationdb = new CitiSenseStation(
                                        id, place_key, pollutants, lat, lng
                                );
                                stationdb.save();
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.d("DataAPI", "json can't be read");
                }
                return result;
            } catch (Exception ignored) {
                Log.d("DataAPI", "config GET error");
            }
            return "";
        }

        protected void onPostExecute(String result) {
            Log.d("DataAPI", result);
        }
    }
}