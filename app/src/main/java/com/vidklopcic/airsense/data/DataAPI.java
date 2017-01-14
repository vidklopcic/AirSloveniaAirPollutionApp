package com.vidklopcic.airsense.data;


import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.vidklopcic.airsense.data.Serializers.ARSOMeasurements;
import com.vidklopcic.airsense.data.Serializers.ARSOStation;
import com.vidklopcic.airsense.data.entities.MeasuringStation;
import com.vidklopcic.airsense.util.Network;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;

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

                    mForceUpdate = false;
                    notifyCycleEnded(updated);
                    try {
                        Long s = new Date().getTime();
                        Long end = s + Constants.ARSOStation.update_interval;
                        while (new Date().getTime() < end || mForceUpdate) {
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

        public GetDataRangeTask(List<MeasuringStation> stations, Long limit, DataRangeListener listener) {
            this.limit = limit;
            mListener = listener;
            mStationIds = MeasuringStation.stationsToIdList(stations);
        }

        @Override
        protected Void doInBackground(Void... params) {
//            Realm realm = Realm.getDefaultInstance();
//            List<MeasuringStation> stations = MeasuringStation.idListToStations(realm, mStationIds);
//            for (MeasuringStation station : stations) {
//                long startUpdateTask = 0;
//                long end = 0;
//                if (station.getLastRangeUpdateTime() == null || station.getOldestStoredMeasurementTime() == null) {
//                    if (limit < new Date().getTime()) {
//                        startUpdateTask = limit;
//                        end = new Date().getTime();
//                    }
//                } else if (station.getLastRangeUpdateTime() == null || limit+ Constants.ARSOStation.update_interval < station.getOldestRangeRequest()) {
//                    startUpdateTask = limit;
//                    end = station.getOldestRangeRequest();
//                } else if (new Date().getTime()- Constants.ARSOStation.update_interval > station.getLastRangeUpdateTime()) {
//                    startUpdateTask = station.getLastRangeUpdateTime();
//                    end = new Date().getTime();
//                }
//
//                if (startUpdateTask != 0 && end != 0) {
//                    String start_date = MeasuringStation.dateToString(new Date(startUpdateTask));
//                    String end_date = MeasuringStation.dateToString(new Date(end));
//
//                    mUrl = Constants.ARSOStation.measurement_range_url
//                            .replace(Constants.ARSOStation.measurement_range_url_start, start_date)
//                            .replace(Constants.ARSOStation.measurement_range_url_end, end_date);
//
//                    String id = station.getStationId();
//                    try {
//                        JSONArray measurements = new JSONArray(Network.GET(mUrl.replace(Constants.ARSOStation.measurement_range_url_id, id)));
//                        station.setMeasurements(realm, measurements);
//                        if (station.getLastRangeUpdateTime() == null || end > station.getLastRangeUpdateTime())
//                            station.setLastRangeUpdateTime(realm, end);
//                        if (station.getOldestRangeRequest() == null || startUpdateTask < station.getOldestRangeRequest())
//                            station.setOldestRangeRequest(realm, startUpdateTask);
//
//                    } catch (IOException | JSONException ignored) {
//                    }
//                }
//            }
//            return null;
            return null;
        }

        protected void onPostExecute(Void params) {
            mListener.onDataRetrieved(mStationIds, limit);
        }
    }
}