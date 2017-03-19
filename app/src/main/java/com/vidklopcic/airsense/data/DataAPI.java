package com.vidklopcic.airsense.data;


import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.vidklopcic.airsense.data.Gson.Measurement;
import com.vidklopcic.airsense.data.Gson.MeasurementRangeParams;
import com.vidklopcic.airsense.data.Serializers.ARSOMeasurements;
import com.vidklopcic.airsense.data.Serializers.ARSOStation;
import com.vidklopcic.airsense.data.entities.MeasuringStation;
import com.vidklopcic.airsense.util.Network;

import org.json.JSONArray;
import org.json.JSONException;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataAPI {
    private static final String LOG_ID = "DataAPI";
    private List<MeasuringStation> mActiveStations;
    private DataUpdateListener mListener;
    private boolean mForceUpdate = false;
    private boolean mFirstRun = true;
    private UpdateTask mUpdateTask;
    private Activity mContext;
    public interface DataUpdateListener {
        void onDataReady();
        void onDataUpdate();
        void onStationUpdate(MeasuringStation station);
    }

    public interface DataRangeListener {
        void onDataRetrieved(List<String> station_ids, Long limit);
    }

    public DataAPI(Activity context) {
        mContext = context;
        mUpdateTask = new UpdateTask();
        startUpdateTask();
    }

    public static Realm getRealmOrCreateInstance(Activity activity) {
        try {
            return Realm.getDefaultInstance();
        } catch (NullPointerException e) {
            RealmConfiguration config = new RealmConfiguration.Builder(activity)
                    .name("citisense_cache")
                    .schemaVersion(1)
                    .deleteRealmIfMigrationNeeded()
                    .build();
            Realm.setDefaultConfiguration(config);
        }
        return Realm.getDefaultInstance();
    }

    public void setObservedStations(List<MeasuringStation> stations) {
        if (stations != null)
            mActiveStations = stations;
    }

    public void setDataUpdateListener(DataUpdateListener listener) {
        mListener = listener;
    }

    public void updateData() {
        mForceUpdate = true;
    }

    private void startUpdateTask() {
        Thread t = new Thread(mUpdateTask);
        t.setDaemon(true);
        t.start();
    }

    public static void getMeasurementsInRange(List<MeasuringStation> stations, Long limit_millis, DataRangeListener listener) {
        new GetDataRangeTask(stations, limit_millis, listener).execute();
    }

    class UpdateTask implements Runnable {
        List<String> mActiveStationsIds;

        @Override
        public void run() {
            Realm realm = Realm.getDefaultInstance();
            try {
                while (true) {
                    updateStations();
                    Boolean updated = false;
                    try {
                        String last_measurement = Network.GET(Constants.ARSOStation.last_measurement_url);
                        // ARSO BUG v XMLju
                        last_measurement = last_measurement.replace("&lt;", "");
                        Serializer serializer = new Persister();
                        ARSOMeasurements measurements = serializer.read(ARSOMeasurements.class, last_measurement);
                        for (ARSOStation station : measurements.stations) {
                            MeasuringStation mstat = MeasuringStation.findById(realm, station.id);
                            notifyStationUpdated(MeasuringStation.updateOrCreate(realm, station));
                            updated = true;
                        }
//                        for (com.vidklopcic.citisense.data.Serializers.ARSOStation arso : measurements.stations) {
//                            MeasuringStation station = MeasuringStation.create(realm, arso);
//
//                            if (new Date().getTime() - station.getLastUpdateTime()
//                                    > Constants.ARSOStation.update_interval || mForceUpdate) {
//                                updated = true;
//                                try {
//                                    String last_measurement = Network.GET(Constants.ARSOStation.last_measurement_url);
//                                    station.setLastMeasurement(realm, last_measurement);
//                                } catch (IOException e) {
//                                    Log.d(LOG_ID, "couldn't get last measurement for " + station.getStationId());
//                                } catch (JSONException e) {
//                                    Log.d(LOG_ID, "couldn't parse last measurement for " + station.getStationId());
//                                }
//                                notifyStationUpdated(station);
//                            }
//                        }
                    } catch (IOException e) {
                        Log.d(LOG_ID, "couldn't get last measurements");
                    } catch (Exception e) {
                        Log.d(LOG_ID, "couldn't parse last measurements");
                    }

                    notifyCycleEnded(updated);
                    mForceUpdate = false;
                    try {
                        Long s = new Date().getTime();
                        Long end = s + Constants.ARSOStation.update_interval;
                        while (new Date().getTime() < end && !mForceUpdate) {
                            Thread.sleep(10);
                        }
                    } catch (InterruptedException ignored) {}
                }
            } finally {
                realm.close();
            }
        }

        private void updateStations() {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActiveStationsIds = MeasuringStation.stationsToIdList(mActiveStations);
                }
            });
        }

        private void notifyStationUpdated(final MeasuringStation station) {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) mListener.onStationUpdate(station);
                    Log.d(LOG_ID, "station updated");
                }
            });
        }

        private void notifyCycleEnded(final Boolean was_updated) {
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mListener != null && was_updated) {
                        Log.d(LOG_ID, "data updated");
                        mListener.onDataUpdate();
                    }
                    if (mListener != null && mFirstRun) {
                        mListener.onDataReady();
                        mFirstRun = false;
                    }
                }
            });
        }
    }

    static class GetDataRangeTask extends AsyncTask<Void, Void, Void> {
        String mUrl;
        Long limit;
        List<String> mStationIds;
        DataRangeListener mListener;
        API.AirSense mAirSenseApi;


        public GetDataRangeTask(List<MeasuringStation> stations, Long limit, DataRangeListener listener) {
            this.limit = limit;
            mListener = listener;
            mStationIds = MeasuringStation.stationsToIdList(stations);
            mAirSenseApi = API.initApi();
        }

        @Override
        protected Void doInBackground(Void... params) {
            Realm realm = Realm.getDefaultInstance();
            List<MeasuringStation> stations = MeasuringStation.idListToStations(realm, mStationIds);
            for (final MeasuringStation station : stations) {
                long startUpdateTask = 0;
                long end = 0;
                if (station.getLastRangeUpdateTime() == null || station.getOldestStoredMeasurementTime() == null) {
                    if (limit < new Date().getTime()) {
                        startUpdateTask = limit;
                    }
                } else if (station.getLastRangeUpdateTime() == null || limit+ Constants.ARSOStation.update_interval < station.getOldestRangeRequest()) {
                    startUpdateTask = limit;
                    end = station.getOldestRangeRequest();
                } else if (new Date().getTime()- Constants.ARSOStation.update_interval > station.getLastRangeUpdateTime()) {
                    startUpdateTask = station.getLastRangeUpdateTime();
                }

                if (startUpdateTask != 0) {
                    if (startUpdateTask < (new Date().getTime() - 3*24*60*60*1000)) {
                        startUpdateTask = new Date().getTime() - 3*24*60*60*1000;
                    }

                    String id = station.getStationId();

                    if (end == 0) {
                        end = new Date().getTime();
                    }
                    final long finalEnd = end;
                    final long finalStartUpdateTask = startUpdateTask;
                    Call<Measurement[]> result = mAirSenseApi.getMeasurementsRange(id, new MeasurementRangeParams(new Date(startUpdateTask), new Date(end)));
                    try {
                        Response<Measurement[]> c = result.execute();
                        if (c.isSuccessful()) {
                            List<Measurement> measurements = Arrays.asList(c.body());
                            station.setMeasurements(realm, measurements);
                            if (station.getLastRangeUpdateTime() == null || finalEnd > station.getLastRangeUpdateTime())
                                station.setLastRangeUpdateTime(realm, finalEnd);
                            if (station.getOldestRangeRequest() == null || finalStartUpdateTask < station.getOldestRangeRequest())
                                station.setOldestRangeRequest(realm, finalStartUpdateTask);
                        }
                    } catch (IOException ignored) {}
                }
            }
            return null;
        }

        protected void onPostExecute(Void params) {
            mListener.onDataRetrieved(mStationIds, limit);
        }
    }
}