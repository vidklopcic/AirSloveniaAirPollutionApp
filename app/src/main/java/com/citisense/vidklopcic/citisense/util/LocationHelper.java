package com.citisense.vidklopcic.citisense.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

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

    public interface LocationHelperListener {
        void onLocationChanged(Location location);
        void onCityChange(String city);
    }

    public interface AddressFromLatLngListener {
        void onResult(Address address);
    }

    public LocationHelper(Activity context) {
        mContext = context;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        startLocationReading();
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
    public void startLocationReading() {
        if (!hasPermission()) return;
        mBestLocation = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        ArrayList<String> providers = new ArrayList<>(mLocationManager.getProviders(true));
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 100, this);
        }
        if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 100, this);
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
        @Override
        protected String doInBackground(LatLng... params) {
            Geocoder gcd = new Geocoder(mContext, Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(params[0].latitude, params[0].longitude, 1);
                if (addresses.size() > 0)
                    return addresses.get(0).getLocality();
            } catch (IOException e) {
                return "Ljubljana";
            }
            return "";
        }

        protected void onPostExecute(String city) {
            if (city == null) return;
            if (mListener != null && !city.isEmpty() && !mCity.equals(city)) {
                mCity = city;
                mListener.onCityChange(mCity);
            }
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