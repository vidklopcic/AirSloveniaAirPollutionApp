package com.citisense.vidklopcic.citisense.data;


import android.os.AsyncTask;
import android.util.Log;

import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.util.Network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class DataAPI {
    private static final String LOG_ID = "DataAPI";
    private ArrayList<CitiSenseStation> mActiveStations;
    private DataUpdateListener mListener;
    private boolean mForceUpdate = false;

    public interface DataUpdateListener {
        void onDataUpdate();
        void onStationUpdate(CitiSenseStation station);
    }

    public DataAPI() {
        getConfig();
        mActiveStations = new ArrayList<>();
        new UpdateTask().execute(mActiveStations.toArray(new CitiSenseStation[mActiveStations.size()]));
    }

    public void setObservedStations(ArrayList<CitiSenseStation> stations) {
        mActiveStations = stations;
    }

    public void setDataUpdateListener(DataUpdateListener listener) {
        mListener = listener;
    }

    public void updateData() {
        mForceUpdate = true;
    }

    private void getConfig() {
        new LoadConfigTask().execute(Constants.DataSources.config_url);
    }

    class LoadConfigTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                String result = Network.GET(params[0]);
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
                                Float lat = (float) station.getDouble(1);
                                Float lng = (float) station.getDouble(2);
                                JSONArray pollutants = station.getJSONArray(3);
                                CitiSenseStation stationdb = new CitiSenseStation(
                                        id, place_key, pollutants, lat, lng
                                );
                                stationdb.save();
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.d(LOG_ID, "json can't be read");
                }
                return result;
            } catch (Exception ignored) {
                Log.d(LOG_ID, "config GET error");
            }
            return "";
        }

        protected void onPostExecute(String result) {
            Log.d(LOG_ID, result);
        }
    }

    class UpdateTask extends AsyncTask<CitiSenseStation, CitiSenseStation, Boolean> {

        @Override
        protected Boolean doInBackground(CitiSenseStation... params) {
            Boolean updated = false;
            try {
                Thread.sleep(Constants.MILLI);
            } catch (InterruptedException ignored) {}
            for (CitiSenseStation station : mActiveStations) {
                if (new Date().getTime() - station.getLastMeasurementTime()
                        > Constants.CitiSenseStation.update_interval || mForceUpdate) {
                    Log.d("aqi_fragment", station.getLastMeasurementTime().toString());
                    mForceUpdate = false;
                    updated = true;
                    try {
                        String last_measurement = Network.GET(Constants.CitiSenseStation.last_measurement_url
                                + station.getStationId());
                        Log.d(LOG_ID, last_measurement);
                        station.setLastMeasurement(last_measurement);
                        station.save();
                        publishProgress(station);
                    } catch (IOException e) {
                        Log.d(LOG_ID, "couldn't get last measurement for " + station.getStationId());
                    } catch (JSONException e) {
                        Log.d(LOG_ID, "couldn't parse last measurement for " + station.getStationId());
                    }
                }
            }
            return updated;
        }

        @Override
        protected void onProgressUpdate(CitiSenseStation... stations) {
            if (mListener != null) mListener.onStationUpdate(stations[0]);
            Log.d(LOG_ID, "station updated");
        }

        protected void onPostExecute(Boolean was_updated) {
            if (mListener != null && was_updated) {
                Log.d(LOG_ID, "data updated");
                mListener.onDataUpdate();
            }
            new UpdateTask().execute(mActiveStations.toArray(new CitiSenseStation[mActiveStations.size()]));
        }
    }
}