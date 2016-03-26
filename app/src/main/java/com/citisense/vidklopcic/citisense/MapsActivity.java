package com.citisense.vidklopcic.citisense;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.data.entities.SavedState;
import com.citisense.vidklopcic.citisense.util.LocationHelper;
import com.citisense.vidklopcic.citisense.util.UI;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.algo.Algorithm;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import java.util.List;

public class MapsActivity extends FragmentActivity implements LocationHelper.LocationHelperListener, PlaceSelectionListener, OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraChangeListener, ClusterManager.OnClusterItemClickListener<MapsActivity.ClusterStation> {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationHelper mLocation;
    private SlidingMenu mMenu;
    private Marker mCurrentMarker;
    private Place mCurrentPlace;
    private ClusterManager<ClusterStation> mClusterManager;
    private SavedState mSavedState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSavedState = new SavedState().getSavedState();
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        mMenu = UI.getSlidingMenu(getWindowManager(), this);
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(this);
        autocompleteFragment.getActivity().findViewById(R.id.place_autocomplete_clear_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mCurrentMarker != null) {
                            mCurrentMarker.remove();
                        }
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMapAsync(this);
        }
    }

    private void setUpClusterer() {

        // Position the map.
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.503186, -0.126446), 10));

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<ClusterStation>(this, mMap);

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mMap.setOnMarkerClickListener(mClusterManager);
        mClusterManager.setOnClusterItemClickListener(this);
    }

    private void setUpMap() {
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnCameraChangeListener(this);
        mLocation = new LocationHelper(this);
        if (mLocation.hasPermission()) mMap.setMyLocationEnabled(true);
        mLocation.setLocationHelperListener(this);
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onCityChange(String city) {

    }

    public void onMicClicked(View view) {

    }

    public void onMenuClicked(View view) {
        mMenu.showMenu();
    }

    @Override
    public void onPlaceSelected(Place place) {
        if (mCurrentMarker != null) {
            mCurrentMarker.remove();
        }

        mCurrentPlace = place;
        mCurrentMarker = mMap.addMarker(new MarkerOptions().position(place.getLatLng()));
        LatLngBounds bounds = mCurrentPlace.getViewport();
        if (bounds != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mCurrentPlace.getViewport(), 0));
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), Constants.Map.default_zoom));
        }
    }

    @Override
    public void onError(Status status) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case LocationHelper.LOCATION_PERMISSION_RESULT: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setUpMap();
        setUpClusterer();
        populateMap();
        LatLngBounds lastviewport = mSavedState.getLastViewport();
        if (lastviewport != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(lastviewport, 0));
        }
    }

    private void populateMap() {
        List<CitiSenseStation> stations = CitiSenseStation.listAll(CitiSenseStation.class);
        for (CitiSenseStation station: stations) {
            mClusterManager.addItem(new ClusterStation(station.getLocation(), station));
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (mCurrentMarker != null) mCurrentMarker.remove();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (mCurrentMarker != null) mCurrentMarker.remove();
        mCurrentMarker = mMap.addMarker(new MarkerOptions().position(latLng));
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        mSavedState.setLastViewport(mMap.getProjection().getVisibleRegion().latLngBounds);
        mClusterManager.onCameraChange(cameraPosition);
    }

    @Override
    public boolean onClusterItemClick(ClusterStation clusterStation) {
        return false;
    }

    public class ClusterStation implements ClusterItem {
        private final LatLng mPosition;
        CitiSenseStation station;
        public ClusterStation(LatLng position, CitiSenseStation station) {
            mPosition = position;
            this.station = station;
        }

        @Override
        public LatLng getPosition() {
            return mPosition;
        }
    }
}
