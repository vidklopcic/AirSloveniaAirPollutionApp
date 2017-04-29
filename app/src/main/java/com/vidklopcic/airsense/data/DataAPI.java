package com.vidklopcic.airsense.data;


import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.vidklopcic.airsense.data.Gson.Measurement;
import com.vidklopcic.airsense.data.Gson.MeasurementRangeParams;
import com.vidklopcic.airsense.data.Gson.OtherMeasurement;
import com.vidklopcic.airsense.data.Gson.PollutionMeasurement;
import com.vidklopcic.airsense.data.Gson.Station;
import com.vidklopcic.airsense.data.Serializers.ARSOMeasurements;
import com.vidklopcic.airsense.data.Serializers.ARSOStation;
import com.vidklopcic.airsense.data.entities.MeasuringStation;
import com.vidklopcic.airsense.data.entities.SavedState;
import com.vidklopcic.airsense.data.entities.StationMeasurement;
import com.vidklopcic.airsense.util.Conversion;
import com.vidklopcic.airsense.util.Network;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import retrofit2.Call;
import retrofit2.Response;

public class DataAPI {
    private static final String LOG_ID = "DataAPI";
    private List<MeasuringStation> mActiveStations;
    private DataUpdateListener mListener;
    private boolean mForceUpdate = false;
    private boolean mFirstRun = true;
    private boolean mShouldExitTask = false;
    private UpdateTask mUpdateTask;
    private Activity mContext;
    private boolean mUpdateTaskIsRunning = false;
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
                    .schemaVersion(2)
                    .deleteRealmIfMigrationNeeded()
                    .build();
            Realm.setDefaultConfiguration(config);
        }
        return Realm.getDefaultInstance();
    }

    public void setObservedStations(List<MeasuringStation> stations) {
        if (stations != null)
            mActiveStations = stations;
        updateData();
    }

    public void setDataUpdateListener(DataUpdateListener listener) {
        mListener = listener;
    }

    public void updateData() {
        mForceUpdate = true;
    }

    private void startUpdateTask() {
        if (mUpdateTaskIsRunning)
            return;
        Thread t = new Thread(mUpdateTask);
        t.setDaemon(true);
        t.start();
        mUpdateTaskIsRunning = true;
    }

    public void pauseUpdateTask() {
        mShouldExitTask = true;
    }

    public void resumeUpdateTask() {
        startUpdateTask();
    }

    public static void getMeasurementsInRange(List<MeasuringStation> stations, Long limit_millis, DataRangeListener listener) {
        new GetDataRangeTask(stations, limit_millis, listener).execute();
    }

    class UpdateTask implements Runnable {
        List<String> mActiveStationsIds;
        API.AirSense mAirSenseApi;

        public UpdateTask() {
            mAirSenseApi = API.initApi();
        }

        public void updateStation(Realm realm, MeasuringStation mstation) throws IOException {
            Response<Measurement> resp = mAirSenseApi.getLastMeasurement(mstation.getStationId()).execute();
            Measurement last_measurement = resp.body();
            mstation.setMeasurement(realm, last_measurement);
            notifyStationUpdated(mstation);
        }

        @Override
        public void run() {
            Realm realm;
            try {
                while (!mShouldExitTask) {
                    realm = Realm.getDefaultInstance();
                    SavedState savedState = SavedState.getSavedState(realm);
                    Integer schema_version = savedState.getSchemaVersion();
                    if (schema_version == null) {
                        schema_version = 1;
                    }

                    updateStations();
                    List<MeasuringStation> stations = MeasuringStation.idListToStations(realm, mActiveStationsIds);
                    Boolean updated = false;
                    try {
                        Response<Station[]> new_stations_response = mAirSenseApi.getAllStationsBySchema(schema_version).execute();
                        Station[] new_stations = new_stations_response.body();
                        while (new_stations.length > 0) {
                            schema_version += 1;
                            savedState.setSchemaVersion(realm, schema_version);
                            for (Station station : new_stations) {
                                MeasuringStation mstation = MeasuringStation.updateOrCreate(realm, station);
                                updateStation(realm, mstation);
                                updated = true;
                            }
                            new_stations_response = mAirSenseApi.getAllStationsBySchema(schema_version).execute();
                            new_stations = new_stations_response.body();
                        }

                        for (MeasuringStation station : stations) {
                            updateStation(realm, station);
                            updated = true;
                        }
                    } catch (Exception e) {
                    }

                    notifyCycleEnded(updated);
                    mForceUpdate = false;

                    Long s = new Date().getTime();
                    Long end = s + Constants.ARSOStation.update_interval;
                    RealmResults<StationMeasurement> result = realm.where(StationMeasurement.class)
                            .lessThan("measurement_time", new Date().getTime() - 5*24*60*60*1000).findAll();
                    if (result.size() > 0) {
                        realm.beginTransaction();
                        result.deleteAllFromRealm();
                        realm.commitTransaction();
                    }

                    realm.close();

                    try {
                        while (new Date().getTime() < end && !mForceUpdate && !mShouldExitTask) {
                            Thread.sleep(Constants.ARSOStation.update_interval);
                        }
                    } catch (InterruptedException ignored) {}
                }
            } finally {
            }
            mShouldExitTask = false;
            mUpdateTaskIsRunning = false;
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
        Long limit;
        List<String> mStationIds;
        DataRangeListener mListener;
        API.AirSense mAirSenseApi;


        public GetDataRangeTask(List<MeasuringStation> stations, Long limit, DataRangeListener listener) {
            if (limit>new Date().getTime()) {
                limit = new Date().getTime();
            }
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
                if (station.getLastMeasurementTime() == null || station.getOldestStoredMeasurementTime() == null) {
                    if (limit < new Date().getTime()) {
                        startUpdateTask = limit;
                    }
                } else if (station.getLastMeasurementTime() == null || limit+ Constants.ARSOStation.update_interval < station.getOldestRangeRequest()) {
                    startUpdateTask = limit;
                    end = station.getOldestRangeRequest();
                } else if (new Date().getTime() - Constants.ARSOStation.update_interval > station.getLastMeasurementTime()) {
                    startUpdateTask = station.getLastMeasurementTime();
                }

                if (startUpdateTask != 0) {
                    if (startUpdateTask < (new Date().getTime() - 3*24*60*60*1000)) {
                        startUpdateTask = new Date().getTime() - 3*24*60*60*1000;
                    }

                    String id = station.getStationId();

                    if (end == 0) {
                        end = new Date().getTime();
                    }

                    Call<Measurement[]> result = mAirSenseApi.getMeasurementsRange(id, Conversion.Time.dateToString(new Date(startUpdateTask)), Conversion.Time.dateToString(new Date(end)));
                    try {
                        Response<Measurement[]> c = result.execute();
                        if (c.isSuccessful()) {
                            List<Measurement> measurements = Arrays.asList(c.body());
                            station.setLastMeasurementTime(realm, station.setMeasurements(realm, measurements));
                            if (station.getOldestRangeRequest() == null || startUpdateTask < station.getOldestRangeRequest())
                                station.setOldestRangeRequest(realm, startUpdateTask);
                        }
                    } catch (IOException ignored) {}
                }
            }
            realm.close();
            return null;
        }

        protected void onPostExecute(Void params) {
            mListener.onDataRetrieved(mStationIds, limit);
        }
    }
}