package com.citisense.vidklopcic.citisense.util.Overlay;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;

import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.util.AQI;
import com.citisense.vidklopcic.citisense.util.Conversion;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapOverlay {
    Activity mContext;
    GoogleMap mMap;
    Projection mDefaultProjection;
    HashMap<LatLngBounds, BitmapDescriptor> buffer;
    GroundOverlay mCurrentOverlay;
    DrawImageTask task;

    public MapOverlay(Activity context, GoogleMap map) {
        mContext = context;
        mMap = map;
        mDefaultProjection = mMap.getProjection();
    }

    public void update() {

    }

    public void draw(List<CitiSenseStation> stations, Projection projection) {
        if (task != null) task.stop();
        task = new DrawImageTask(stations, projection);
        task.execute();
    }

    public static LatLng getOffset(LatLng latLng, Double meters) {
        LatLng tmp = SphericalUtil.computeOffset(SphericalUtil.computeOffset(latLng, meters, 0), meters, 90);
        return new LatLng(tmp.latitude-latLng.latitude, tmp.longitude - latLng.longitude);
    }

    class DrawImageTask {
        int THREAD_LIMIT = 15;
        public List<CitiSenseStation> mStations;
        public Projection mProjection;
        public LatLng mPixelSize;
        public List<CitiSenseStation> candidates;
        public LatLngBounds bounds;
        public Integer y_img_size;
        public Integer x_img_size;
        private GroundOverlayOptions result;
        List<Thread> tasks;
        Thread worker;
        int[] pixels;

        public DrawImageTask(List<CitiSenseStation> stations, Projection projection) {
            mStations = stations;
            mProjection = projection;
            tasks = new ArrayList<>();
        }

        protected void stop() {
            worker.interrupt();
        }

        protected void execute() {
            worker = new Thread(new Runnable() {
                @Override
                public void run() {
                    bounds = CitiSenseStation.getBounds(mStations);
                    if (bounds == null) return;
                    Double distance = SphericalUtil.computeDistanceBetween(
                            mProjection.fromScreenLocation(new Point(0, 0)),
                            mProjection.fromScreenLocation(new Point(Constants.Map.default_overlay_resolution_pixels, Constants.Map.default_overlay_resolution_pixels)));

                    Double pixel_side = Math.sqrt((distance * distance) / 2);
                    if (pixel_side < Constants.Map.max_overlay_resolution_meters)
                        pixel_side = (double) Constants.Map.max_overlay_resolution_meters;
                    mPixelSize = SphericalUtil.computeOffset(SphericalUtil.computeOffset(bounds.getCenter(), pixel_side, 0), pixel_side, 90);
                    mPixelSize = new LatLng(mPixelSize.latitude-bounds.getCenter().latitude, mPixelSize.longitude - bounds.getCenter().longitude);

                    if (bounds == null) return;
                    x_img_size = (int) (Math.abs(bounds.southwest.longitude - bounds.northeast.longitude)/mPixelSize.longitude);
                    y_img_size = (int) (Math.abs(bounds.southwest.latitude - bounds.northeast.latitude)/mPixelSize.latitude);
                    pixels = new int[x_img_size*y_img_size];
                    Log.d("asdfg", x_img_size.toString() + " x " + y_img_size.toString());
                    if (y_img_size < 1 || x_img_size < 1) return;
                    candidates = CitiSenseStation.getStationsInArea(bounds);

                    Log.d("asdfg", "start");

                    int chunk = y_img_size / THREAD_LIMIT;

                    for (int i=0;i<THREAD_LIMIT;i++) {
                        if (i == THREAD_LIMIT-1)
                            spawnWorker(0, i * chunk, y_img_size, x_img_size);
                        else
                            spawnWorker(0, i * chunk, (i + 1) * chunk, x_img_size);
                    }
                    waitForTasks();
                    if (Thread.currentThread().isInterrupted()) return;

                    Bitmap bitmap = Bitmap.createBitmap(pixels, x_img_size, y_img_size, Bitmap.Config.ARGB_8888);
                    Log.d("asdfg", "fin");
                    result = new GroundOverlayOptions()
                            .image(BitmapDescriptorFactory.fromBitmap(bitmap))
                            .position(
                                    bounds.getCenter(),
                                    (float) SphericalUtil.computeDistanceBetween(bounds.southwest, new LatLng(bounds.southwest.latitude, bounds.northeast.longitude)))
                            .transparency((float) Constants.Map.overlay_transparency);

                    finished();
                }

                public void finished() {
                    mContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (result != null) {
                                if (mCurrentOverlay != null) mCurrentOverlay.remove();
                                mCurrentOverlay = mMap.addGroundOverlay(result);
                            }
                        }
                    });
                }
            });

            worker.start();
        }

        public void waitForTasks() {
            boolean finished = false;
            while (!finished) {
                finished = true;
                if (Thread.currentThread().isInterrupted()) {
                    for (Thread dbr : tasks) {
                        dbr.interrupt();
                    }
                    return;
                }
                for (Thread dbr : tasks) {
                    finished = finished && !dbr.isAlive();
                }
            }
        }

        public void spawnWorker(int x_start, int y_start, int y_end, int x_end) {
            WorkerRunnable runnable = new WorkerRunnable(x_start, y_start, y_end, x_end);
            Thread t = new Thread(runnable);
            t.setDaemon(true);
            t.start();
            while (!t.isAlive());
            tasks.add(t);
        }

        public void setPixel(int x, int y, int color) {
            int n = y*x_img_size+x;
            pixels[n] = color;
        }

        class WorkerRunnable implements Runnable {
            int x_start;
            int x_end;
            int y_start;
            int y_end;
            public WorkerRunnable(int x_start, int y_start, int y_end, int x_end) {
                this.x_start = x_start;
                this.x_end = x_end;
                this.y_start = y_start;
                this.y_end = y_end;
            }

            @Override
            public void run() {
                LatLng center;
                List<CitiSenseStation> affecting_stations;
                List<Double> importance  = new ArrayList<>();
                Double importance_sum;
                Double weight;
                Double pixel_intensity;
                Double current_aqi;

                for (int y=y_start;y<y_end;y++) {
                    for (int x=x_start;x<x_end;x++) {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        center = new LatLng(
                                bounds.southwest.latitude+(y+1)*mPixelSize.latitude,
                                bounds.southwest.longitude+(x+1)*mPixelSize.longitude);
                        affecting_stations =  CitiSenseStation.getStationsInRadius(
                                center, Constants.Map.station_radius_meters, candidates);

                        for (int i=0;i<affecting_stations.size();i++) {
                            if (!affecting_stations.get(i).hasData()) {
                                affecting_stations.remove(i);
                                i--;
                            }
                        }

                        if (affecting_stations.size() > 0) {
                            importance.clear();
                            for (CitiSenseStation station : affecting_stations) {
                                if (station.hasData())
                                    importance.add(SphericalUtil.computeDistanceBetween(center, station.getLocation()));
                            }

                            for (int i = 0; i < importance.size(); i++) {
                                Double distance = importance.get(i);
                                Double percent_of_distance = distance/Constants.Map.station_radius_meters;
                                importance.set(i, 1 - percent_of_distance);
                                if (percent_of_distance < 0.2) {
                                    importance.set(i, importance.get(i)+importance.get(i)*4*(1-percent_of_distance*5));
                                }
                            }

                            importance_sum = Conversion.sum(importance);
                            weight = 1d / importance_sum;

                            pixel_intensity = 0d;
                            for (int i = 0; i < importance.size(); i++) {
                                current_aqi = affecting_stations.get(i).getMaxAqi() * importance.get(i) * weight;
                                pixel_intensity += current_aqi;
                            }

                            setPixel(x, y_img_size - 1 - y, AQI.getLinearColor(pixel_intensity.intValue(), mContext));
                        }
                    }
                }
            }
        }
    }
}
