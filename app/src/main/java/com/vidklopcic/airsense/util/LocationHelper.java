package com.vidklopcic.airsense.util;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.bricolsoftconsulting.geocoderplus.Area;
import com.bricolsoftconsulting.geocoderplus.Position;
import com.google.android.gms.maps.model.LatLngBounds;
import com.vidklopcic.airsense.R;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationHelper implements LocationListener {
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    public static final int LOCATION_PERMISSION_RESULT = 0;
    private String mCity = "";
    Activity mContext;
    LocationManager mLocationManager;
    Location mBestLocation;
    LocationHelperListener mListener;
    boolean mLocationIsEnabled = false;
    boolean mDialogWasShown = false;

    public interface LocationHelperListener {
        void onLocationChanged(Location location);
        void onCityChange(String city, LatLngBounds bounds);
    }

    public interface AddressFromLatLngListener {
        void onResult(Address address);
    }

    public LocationHelper(final Activity context) {
        mContext = context;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public Location getLocation() {
        return mBestLocation;
    }

    public String getCity() {
        return mCity;
    }

    public LatLng getLatLng() {
        return new LatLng(mBestLocation.getLatitude(), mBestLocation.getLongitude());
    }

    public boolean hasPermission() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mContext,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    LocationHelper.LOCATION_PERMISSION_RESULT);
            return false;
        }
        return true;
    }

    public boolean isLocationEnabled() {
        return mLocationIsEnabled;
    }


    public boolean locationIsTurnedOn() {
        boolean gps_enabled = false;
        boolean network_enabled = false;
        try {
            gps_enabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}
        try {
            network_enabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}
        return gps_enabled || network_enabled;
    }

    public void askToTurnOnLocation() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setMessage(mContext.getResources().getString(R.string.location_not_available_text));
        dialog.setPositiveButton(mContext.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(myIntent);
            }
        });
        dialog.setNegativeButton(mContext.getString(R.string.Cancel), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {

            }
        });
        dialog.show();
    }

    public void startLocationReading() {
        if (!hasPermission() || isLocationEnabled()) return;
        if (!locationIsTurnedOn()) {
            if (!mDialogWasShown)
                askToTurnOnLocation();
            mDialogWasShown = true;
        } else {
            mLocationIsEnabled = true;
            mBestLocation = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            ArrayList<String> providers = new ArrayList<>(mLocationManager.getProviders(true));
            if (providers.contains(LocationManager.GPS_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 100, this);
            }
            if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 100, this);
            }
        }
    }

    public void setLocationHelperListener(LocationHelperListener listener) {
        mListener = listener;
    }

    public static void getAddressFromLatLng(LatLng location, Context context, AddressFromLatLngListener listener) {
        new GetAddressFromLatLngTask(context, listener).execute(location);
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isBetterLocation(location, mBestLocation)) {
            mBestLocation = location;
            if (mListener != null) mListener.onLocationChanged(location);
            updateCity(new LatLng(location.getLatitude(), location.getLongitude()));
        }
    }

    private void updateCity(LatLng latLng) {
        if (mListener == null) return;
        new GetCityTask().execute(latLng);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    // todo join tasks below into one
    class GetCityTask extends AsyncTask<LatLng, Void, String> {
        LatLngBounds mBounds;
        @Override
        protected String doInBackground(LatLng... params) {
            Geocoder gcd = new Geocoder(mContext, Locale.getDefault());
            com.bricolsoftconsulting.geocoderplus.Geocoder gcdp = new com.bricolsoftconsulting.geocoderplus.Geocoder();
            List<Address> addresses;
            List<com.bricolsoftconsulting.geocoderplus.Address> addresses_plus;
            try {
                addresses = gcd.getFromLocation(params[0].latitude, params[0].longitude, 1);
                if (addresses.size() > 0) {
                    String city = addresses.get(0).getLocality();
                    if (city == null) {
                        return "";
                    }
                    addresses_plus = gcdp.getFromLocationName(city);
                    if (addresses_plus.size() > 0) {
                        Area v = addresses_plus.get(0).getViewPort();
                        Position sw = v.getSouthWest();
                        Position ne = v.getNorthEast();
                        mBounds = new LatLngBounds(new LatLng(sw.getLatitude(), sw.getLongitude()), new LatLng(ne.getLatitude(), ne.getLongitude()));
                    }
                    return city;
                }
            } catch (Exception e) {
                return "Ljubljana";
            }
            return null;
        }

        protected void onPostExecute(String city) {
            if (city != null && !mCity.equals(city))
                mCity = city;
            if (mListener != null)
                mListener.onCityChange(mCity, mBounds);
        }
    }

    static class GetAddressFromLatLngTask extends AsyncTask<LatLng, Void, Address> {
        private static final String LOG_TAG = "AddressFromLatLng";
        Context mContext;
        AddressFromLatLngListener mListener;
        public GetAddressFromLatLngTask(Context context, AddressFromLatLngListener listener) {
            mContext = context;
            mListener = listener;
        }

        @Override
        protected Address doInBackground(LatLng... params) {
            Geocoder gcd = new Geocoder(mContext, Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(params[0].latitude, params[0].longitude, 1);
                if (addresses.size() > 0)
                    return addresses.get(0);
            } catch (IOException e) {
                Log.d(LOG_TAG, "Geocoder exception");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Address address) {
            mListener.onResult(address);
        }
    }
}