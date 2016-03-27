package com.citisense.vidklopcic.citisense.util.Overlay;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

import java.util.HashMap;
import java.util.List;

public class MapOverlay {
    Activity mContext;
    GoogleMap mMap;
    Projection mDefaultProjection;
    HashMap<LatLngBounds, BitmapDescriptor> buffer;

    public MapOverlay(Activity context, GoogleMap map) {
        mContext = context;
        mMap = map;
        mDefaultProjection = mMap.getProjection();
    }

    public void update() {

    }

    public void draw(List<CitiSenseStation> stations, Projection projection) {
        DrawImageTask task = new DrawImageTask(stations, projection);
        task.execute();
    }

    class DrawImageTask extends AsyncTask<Void, Void, BitmapDescriptor> {
        List<CitiSenseStation> mStations;
        Projection mProjection;
        LatLng mPixelSize;
        public DrawImageTask(List<CitiSenseStation> stations, Projection projection) {
            mStations = stations;
            mProjection = projection;
            Double distance = SphericalUtil.computeDistanceBetween(
                    mProjection.fromScreenLocation(new Point(0, 0)),
                    mProjection.fromScreenLocation(new Point(Constants.Map.overlay_resolution, Constants.Map.overlay_resolution)));

            Double pixel_side = Math.sqrt((distance*distance)/2);
            mPixelSize = SphericalUtil.computeOffset(SphericalUtil.computeOffset(new LatLng(0, 0), pixel_side, 0), pixel_side, 90);
        }

        @Override
        protected BitmapDescriptor doInBackground(Void... params) {
            LatLngBounds bounds = CitiSenseStation.getBounds(mStations);
            if (bounds == null) return null;
            Integer x_img_size = (int) (Math.abs(bounds.southwest.longitude - bounds.northeast.longitude)/mPixelSize.longitude);
            Integer y_img_size = (int) (Math.abs(bounds.southwest.latitude - bounds.northeast.latitude)/mPixelSize.latitude);
            Log.d("asdfg", x_img_size.toString() + " x " + y_img_size.toString());
            if (y_img_size < 1 || x_img_size < 1) return null;
            Bitmap bitmap = Bitmap.createBitmap(x_img_size, y_img_size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            Integer max_affecting= 0;
            List<CitiSenseStation> candidates = CitiSenseStation.getStationsInArea(bounds);
            for (int y=0;y<y_img_size;y++) {
                for (int x=0;x<x_img_size;x++) {
                    LatLng center = new LatLng(
                            bounds.southwest.latitude+y*mPixelSize.latitude,
                            bounds.southwest.longitude+x*mPixelSize.longitude);
                    List<CitiSenseStation> affecting_stations =  CitiSenseStation.getStationsInRadius(
                            center, Constants.Map.station_radius_meters, candidates);
                    if (affecting_stations.size() > max_affecting) max_affecting = affecting_stations.size();
                }
            }
            Log.d("asdfg", max_affecting.toString());
            return null;
        }

        protected void onPostExecute(BitmapDescriptor params) {
        }
    }
}
