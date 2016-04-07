package com.citisense.vidklopcic.citisense.data;


import android.os.AsyncTask;
import android.util.Log;

import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.data.entities.SavedState;
import com.citisense.vidklopcic.citisense.util.Network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class DataAPI {
    private static final String LOG_ID = "DataAPI";
    private ArrayList<CitiSenseStation> mActiveStations;
    private DataUpdateListener mListener;
    private boolean mForceUpdate = false;
    private boolean mFirstRun = true;
    private SavedState mSavedState;
    private UpdateTask mUpdateTask;
    public interface DataUpdateListener {
        void onDataReady();
        void onDataUpdate();
        void onStationUpdate(CitiSenseStation station);
    }

    public interface DataRangeListener {
        void onDataRetrieved(Long limit);
    }

    public DataAPI() {
        mSavedState = new SavedState().getSavedState();
        getConfig();
        mActiveStations = new ArrayList<>();
        mUpdateTask = new UpdateTask();
    }

    public void setObservedStations(ArrayList<CitiSenseStation> stations) {
        if (stations != null)
            mActiveStations = stations;
    }

    public void setDataUpdateListener(DataUpdateListener listener) {
        mListener = listener;
    }

    public void updateData() {
        mForceUpdate = true;
    }

    private void getConfig() {
        new LoadConfigTask().execute(Constants.DataSources.config_version_url, Constants.DataSources.config_url);
    }

    public static void getMeasurementsInRange(List<CitiSenseStation> stations, Long limit_millis, DataRangeListener listener) {
        new GetDataRangeTask(stations, limit_millis, listener).execute();
    }

    class LoadConfigTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            try {
                int config_version = Integer.valueOf(Network.GET(params[0]));
                if (mSavedState.getConfigVersion() != null && config_version == mSavedState.getConfigVersion()) {
                    return null;
                }
                mSavedState.setConfigVersion(config_version);
            } catch (IOException ignored) {}
            Integer config_version = mSavedState.getConfigVersion();
            try {
                String result = Network.GET(params[1]);
                try {
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
                                List<CitiSenseStation> old_station = CitiSenseStation.find(CitiSenseStation.class, "stationid = ?", id);
                                CitiSenseStation stationdb;
                                if (old_station.size() != 0) {
                                    stationdb = old_station.get(0);
                                    stationdb.setCity(place_key);
                                    stationdb.setPollutants(pollutants);
                                    stationdb.setLocation(lat, lng);
                                    stationdb.setConfigVersion(config_version);
                                } else {
                                    stationdb = new CitiSenseStation(
                                            config_version, id, place_key, pollutants, lat, lng
                                    );
                                }
                                stationdb.save();
                            }
                        }
                    }
                    List<CitiSenseStation> stations_to_remove = CitiSenseStation.find(
                            CitiSenseStation.class, "configversion != ?", config_version.toString()
                    );
                    for (CitiSenseStation station : stations_to_remove) station.delete();
                } catch (JSONException e) {
                    Log.d(LOG_ID, "json can't be read");
                    mSavedState.setConfigVersion(null);
                }
            } catch (IOException ignored) {
                Log.d(LOG_ID, "config GET error");
                mSavedState.setConfigVersion(null);
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            Log.d(LOG_ID, "config loaded");
            mUpdateTask.execute();
        }
    }

    class UpdateTask extends AsyncTask<Void, CitiSenseStation, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            Boolean updated = false;
            try {
                Thread.sleep(Constants.MILLI);
            } catch (InterruptedException ignored) {}
            for (CitiSenseStation station : mActiveStations) {
                if (new Date().getTime() - station.getLastUpdateTime()
                        > Constants.CitiSenseStation.update_interval || mForceUpdate) {
                    updated = true;
                    try {
                        String last_measurement = Network.GET(Constants.CitiSenseStation.last_measurement_url
                                + station.getStationId());
                        station.setLastMeasurement(last_measurement);
                    } catch (IOException e) {
                        Log.d(LOG_ID, "couldn't get last measurement for " + station.getStationId());
                    } catch (JSONException e) {
                        Log.d(LOG_ID, "couldn't parse last measurement for " + station.getStationId());
                    }
                    station.save();
                    publishProgress(station);
                }
            }
            mForceUpdate = false;
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
            new UpdateTask().execute();
            if (mListener != null && mFirstRun) {
                mListener.onDataReady();
                mFirstRun = false;
            }
        }
    }

    static class GetDataRangeTask extends AsyncTask<Void, Void, Void> {
        String mUrl;
        Long limit;
        List<CitiSenseStation> mStations;
        DataRangeListener mListener;

        public GetDataRangeTask(List<CitiSenseStation> stations, Long limit, DataRangeListener listener) {
            this.limit = limit;
            mListener = listener;
            mStations = stations;
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (CitiSenseStation station : mStations) {
                long start = 0;
                long end = 0;
                if (station.getLastRangeUpdateTime() == null || station.getOldestStoredMeasurementTime() == null) {
                    if (limit < new Date().getTime()) {
                        start = limit;
                        end = new Date().getTime();
                    }
                } else if (limit+Constants.CitiSenseStation.update_interval < station.getOldestStoredMeasurementTime()) {
                    start = limit;
                    end = station.getOldestStoredMeasurementTime();
                } else if (limit-Constants.CitiSenseStation.update_interval > station.getLastRangeUpdateTime()) {
                    start = station.getLastRangeUpdateTime();
                    end = limit;
                }

                if (start != 0 && end != 0) {
                    String start_date = CitiSenseStation.dateToString(new Date(start));
                    String end_date = CitiSenseStation.dateToString(new Date(end));

                    mUrl = Constants.CitiSenseStation.measurement_range_url
                            .replace(Constants.CitiSenseStation.measurement_range_url_start, start_date)
                            .replace(Constants.CitiSenseStation.measurement_range_url_end, end_date);

                    String id = station.getStationId();
                    try {
                        JSONArray measurements = new JSONArray(Network.GET(mUrl.replace(Constants.CitiSenseStation.measurement_range_url_id, id)));
                        station.setMeasurements(measurements);
                    } catch (IOException | JSONException ignored) {
                    }
                }
            }
            return null;
        }

        protected void onPostExecute(Void params) {
            mListener.onDataRetrieved(limit);
        }
    }
}