package com.citisense.vidklopcic.citisense;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.data.DataAPI;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.data.entities.FavoritePlace;
import com.citisense.vidklopcic.citisense.data.entities.SavedState;
import com.citisense.vidklopcic.citisense.fragments.MapCards;
import com.citisense.vidklopcic.citisense.util.AQI;
import com.citisense.vidklopcic.citisense.util.FABPollutants;
import com.citisense.vidklopcic.citisense.util.LocationHelper;
import com.citisense.vidklopcic.citisense.util.MapPullUpPager;
import com.citisense.vidklopcic.citisense.util.Overlay.MapOverlay;
import com.citisense.vidklopcic.citisense.util.UI;
import com.citisense.vidklopcic.citisense.util.anim.BackgroundColorAnimation;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.google.maps.android.ui.SquareTextView;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;


public class MapsActivity extends FragmentActivity implements LocationHelper.LocationHelperListener, PlaceSelectionListener, OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraChangeListener, ClusterManager.OnClusterItemClickListener<MapsActivity.ClusterStation>,DataAPI.DataUpdateListener, FABPollutants.FABPollutantsListener {
    private MapOverlay mOverlay;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationHelper mLocation;
    private SlidingMenu mMenu;
    private Marker mCurrentMarker;
    private LatLng mPointOfInterest;
    private Place mCurrentPlace;
    private ClusterManager<ClusterStation> mClusterManager;
    private SavedState mSavedState;
    private HashMap<CitiSenseStation, ClusterStation> mStationsOnMap;
    private DataAPI mDataApi;
    private Float mCurrentZoom;
    private SlidingUpPanelLayout mSlidingPane;
    private MapCards mMapCardsFragment;
    private FABPollutants mFABPollutants;
    private String mPollutantFilter;
    private LinearLayout mActionBarContainer;
    private Integer mActionBarHeight;
    private EditText mActionBarTitle;
    private String mPOIAddress;
    private boolean mActionBarHasAddress = false;
    private RelativeLayout mSearchContaienr;
    private MapPullUpPager mPullUpPager;
    private ImageView mFavoritesStar;
    private boolean mPOIIsFavorite = false;
    private Realm mRealm;
    private boolean mMapPositioned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRealm = DataAPI.getRealmOrCreateInstance(this);
        mStationsOnMap = new HashMap<>();
        mSavedState = SavedState.getSavedState(mRealm);
        mDataApi = new DataAPI(this);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        mMenu = UI.getSlidingMenu(getWindowManager(), this);
        final PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(this);
        autocompleteFragment.getActivity().findViewById(R.id.place_autocomplete_clear_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removePointOfInterest();
                        autocompleteFragment.setText("");
                    }
                }
        );
        mSlidingPane = (SlidingUpPanelLayout) findViewById(R.id.map_sliding_pane);
        mMapCardsFragment = (MapCards)
                getFragmentManager().findFragmentById(R.id.map_pollutant_cards_fragment);

        mFABPollutants = new FABPollutants(this, (FloatingActionMenu) findViewById(R.id.fab_pollutants), this);
        mSlidingPane.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float v) {

            }

            @Override
            public void onPanelStateChanged(View view, SlidingUpPanelLayout.PanelState panelState, SlidingUpPanelLayout.PanelState panelState1) {
                if (panelState1 == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    mMapCardsFragment.hide();
                    mActionBarContainer.startAnimation(new BackgroundColorAnimation(
                            mActionBarContainer,
                            ContextCompat.getColor(getContext(), R.color.maps_pullup_actionbar)));
                    mPullUpPager.setOverviewFragment();
                    mSlidingPane.setEnabled(false);
                } else if (panelState1 == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    mMapCardsFragment.show();
                    mPullUpPager.close();
                    mActionBarContainer.startAnimation(new BackgroundColorAnimation(
                            mActionBarContainer,
                            ContextCompat.getColor(getContext(), R.color.maps_blue)));
                }
            }
        });

        mActionBarContainer = (LinearLayout) findViewById(R.id.maps_action_bar);
        mActionBarHeight = (int) getResources().getDimension(R.dimen.action_bar_height);
        mActionBarContainer.animate().translationY(-mActionBarHeight).setDuration(0).start();
        mActionBarTitle = (EditText) findViewById(R.id.actionbar_title_text);
        mActionBarTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && !mPOIIsFavorite && mActionBarTitle.isEnabled()) {
                        addToFavorites(null);
                }
                if (!mActionBarTitle.isEnabled())
                    mActionBarTitle.clearFocus();

                if (!hasFocus && mActionBarTitle.isEnabled()) {
                    RealmResults<FavoritePlace> places = mRealm.where(FavoritePlace.class).equalTo("address", mPOIAddress).findAll();
                    if (places.size() != 0) {
                        places.get(0).setNickname(mRealm, mActionBarTitle.getText().toString());
                    }
                }
            }
        });

        mActionBarTitle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mActionBarTitle.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mActionBarTitle.getWindowToken(), 0);
                }
                return false;
            }
        });
        mSearchContaienr = (RelativeLayout) findViewById(R.id.maps_search_container);

        mPullUpPager = new MapPullUpPager(this);
        mFavoritesStar = (ImageView) findViewById(R.id.actionbar_favorites_button);
        mActionBarTitle.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        mRealm.close();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
    }

    private void removePointOfInterest() {
        if (mCurrentMarker != null)
            mCurrentMarker.remove();
        mActionBarTitle.setEnabled(false);
        mPointOfInterest = null;
        mSlidingPane.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
        hideActionBar();
        setFavorite(false);
        mPullUpPager.close();
        mActionBarContainer.startAnimation(new BackgroundColorAnimation(
                mActionBarContainer,
                ContextCompat.getColor(getContext(), R.color.maps_blue)));
    }

    private void setPointOfInterest(LatLng poi, Marker marker) {
        if (mCurrentMarker != null)
            mCurrentMarker.remove();
        mPointOfInterest = poi;
        mCurrentMarker = marker;
        mSlidingPane.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        showActionBar();
    }

    public void setActionBar(LatLng location) {
        clearActionBarData();
        LocationHelper.getAddressFromLatLng(location, this, new LocationHelper.AddressFromLatLngListener() {
            @Override
            public void onResult(Address address) {
                if (address == null) return;
                mPOIAddress = address.getAddressLine(0);
                mActionBarTitle.setText(mPOIAddress);
                mActionBarHasAddress = true;
                mActionBarTitle.setEnabled(true);
                RealmResults<FavoritePlace> places = mRealm.where(FavoritePlace.class).equalTo("address", mPOIAddress).findAll();
                if (places.size() != 0) {
                    setFavorite(true);
                    mActionBarTitle.setText(places.get(0).getNickname());
                }
            }
        });
    }


    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }

    public void clearActionBarData() {
        mActionBarTitle.setText("...");
        mActionBarHasAddress = false;
    }

    public void showActionBar() {
        mActionBarContainer.animate().translationY(0).setDuration(400).start();
        mSearchContaienr.animate().alpha(0).setDuration(400).start();
    }

    public void hideActionBar() {
        mActionBarContainer.animate().translationY(-mActionBarHeight).setDuration(200).start();
        mSearchContaienr.animate().alpha(1).setDuration(400).start();
    }

    public void addToFavorites(View view) {
        if (!mActionBarHasAddress) return;
        mPOIIsFavorite = !mPOIIsFavorite;
        setFavorite(mPOIIsFavorite);
        if (mPOIIsFavorite) {
            FavoritePlace.create(
                    mRealm, mPointOfInterest, mPOIAddress, mActionBarTitle.getText().toString());
        } else {
            RealmResults<FavoritePlace> matches = mRealm.where(FavoritePlace.class).equalTo("address", mPOIAddress).findAll();
            mActionBarTitle.setText(mPOIAddress);
            if (matches.size() != 0) {
                mRealm.beginTransaction();
                while (matches.iterator().hasNext()) {
                    FavoritePlace match = matches.iterator().next();
                    match.removeFromRealm();
                }
                mRealm.commitTransaction();
            }
        }
    }

    public void setFavorite(boolean is_favorite) {
        if (is_favorite) {
            mFavoritesStar.setImageResource(R.drawable.ic_star_full);
        } else {
            mFavoritesStar.setImageResource(R.drawable.ic_star_outline);
        }
        mPOIIsFavorite = is_favorite;
    }

    public void onActionBarBack(View view) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if (mSlidingPane.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            removePointOfInterest();
        } else if (mSlidingPane.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            mSlidingPane.setEnabled(true);
            mSlidingPane.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    private void setPointOfInterest(LatLng poi) {
        setPointOfInterest(poi, null);
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
        mClusterManager = new ClusterManager<>(this, mMap);
        mMap.setOnMarkerClickListener(mClusterManager);
        mClusterManager.setOnClusterItemClickListener(this);
        mClusterManager.setRenderer(new ClusterRenderer(this, mMap, mClusterManager));
    }

    private void setUpMap() {
        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnCameraChangeListener(this);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mDataApi.setDataUpdateListener(this);
        mLocation = new LocationHelper(this);
        mLocation.startLocationReading();
        if (mLocation.hasPermission()) mMap.setMyLocationEnabled(true);
        mLocation.setLocationHelperListener(this);
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onCityChange(String city) {

    }

    public void onMenuClicked(View view) {
        mMenu.showMenu();
    }

    @Override
    public void onPlaceSelected(Place place) {
        mCurrentPlace = place;
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
        mOverlay = new MapOverlay(this, mMap);
        setUpMap();
        setUpClusterer();
        try {
            positionMap();
            mMapPositioned = true;
        } catch (IllegalStateException ignored) {}
    }


    private void positionMap() {
        LatLngBounds lastviewport = mSavedState.getLastViewport();
        if (lastviewport != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(lastviewport, 0));
        } else {
            Location location = mLocation.getLocation();
            if (location != null)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(location.getLatitude(), location.getLongitude()), Constants.Map.default_zoom));
        }
    }


    public void addStationToMap(CitiSenseStation station) {
        if (station.getLastMeasurement() != null && mPollutantFilter == null || station.hasPollutant(mPollutantFilter)) {
            ClusterStation new_c_station = new ClusterStation(station.getLocation(), station);
            Log.d("MapsActivity", "added " + new_c_station.station.getStationId());
            mStationsOnMap.put(station, new_c_station);
            mClusterManager.addItem(new_c_station);
            mClusterManager.cluster();
        }
    }

    public Activity getContext() {
        return this;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        removePointOfInterest();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
//        if (latLng == mPointOfInterest) return;
//        ArrayList<CitiSenseStation> affecting_stations = (ArrayList<CitiSenseStation>) CitiSenseStation.getStationsAroundPoint(mRealm, latLng, Constants.Map.station_radius_meters);
//        mPullUpPager.setDataSource(affecting_stations);
//        mPollutantCardsFragment.setSourceStations(affecting_stations);
//        setActionBar(latLng);
//        setPointOfInterest(latLng, mMap.addMarker(new MarkerOptions().position(latLng)));
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        List<CitiSenseStation> viewport_stations = CitiSenseStation.getStationsInArea(
                 mRealm, mMap.getProjection().getVisibleRegion().latLngBounds
        );
        mFABPollutants.update(viewport_stations);
        if (cameraPosition.zoom >= Constants.Map.max_overlay_zoom)
            mOverlay.draw(new ArrayList<>(viewport_stations), mMap.getProjection());
        mDataApi.setObservedStations(viewport_stations);
        List<String> stations_on_map = CitiSenseStation.stationsToIdList(new ArrayList<>(mStationsOnMap.keySet()));
        List<String> viewport_stations_ids = CitiSenseStation.stationsToIdList(viewport_stations);
        viewport_stations_ids.removeAll(stations_on_map);

        for (CitiSenseStation station : CitiSenseStation.idListToStations(mRealm, viewport_stations_ids)) {
            addStationToMap(station);
        }

        if (!mMapPositioned) {
            positionMap();
            mMapPositioned = true;
        }
        mSavedState.setLastViewport(mRealm, mMap.getProjection().getVisibleRegion().latLngBounds);
        mRealm.refresh();
        mClusterManager.onCameraChange(cameraPosition);

        mCurrentZoom = mMap.getCameraPosition().zoom;
        if (mPointOfInterest != null && !mMap.getProjection().getVisibleRegion().latLngBounds.contains(mPointOfInterest)) {
            removePointOfInterest();
        }
    }

    @Override
    public boolean onClusterItemClick(ClusterStation clusterStation) {
        if (mPointOfInterest == clusterStation.getPosition()) return false;
        setActionBar(clusterStation.getPosition());
        setPointOfInterest(clusterStation.getPosition());
        mMapCardsFragment.setSourceStations(clusterStation.station);
        ArrayList<CitiSenseStation> list = new ArrayList<>();
        list.add(clusterStation.station);
        mPullUpPager.setDataSource(list);
        setFavorite(false);
        return false;
    }

    @Override
    public void onDataReady() {
    }

    @Override
    public void onDataUpdate() {
        mClusterManager.clearItems();
        mClusterManager.cluster();
        mStationsOnMap.clear();
        mPullUpPager.update();
        onCameraChange(mMap.getCameraPosition());
    }

    @Override
    public void onStationUpdate(CitiSenseStation station) {
    }

    @Override
    public void onFABPollutantSelected(String pollutant) {
        mOverlay.setPollutant(pollutant);
        mPollutantFilter = pollutant;
        onDataUpdate();
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

        public BitmapDescriptor getIcon() {
            IconGenerator iconGen = new IconGenerator(getContext());

            int shapeSize = getResources().getDimensionPixelSize(R.dimen.marker_size);

            Drawable shapeDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.station_marker, null);
            assert shapeDrawable != null;
            Integer aqi;
            if (mPollutantFilter != null)
                aqi = station.getPollutantAqi(mPollutantFilter, station.getLastMeasurement());
            else
                aqi = station.getMaxAqi();
            shapeDrawable.setColorFilter(AQI.getLinearColor(aqi, getContext()), PorterDuff.Mode.MULTIPLY);
            iconGen.setBackground(shapeDrawable);

            View view = new View(getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(shapeSize, shapeSize));
            iconGen.setContentView(view);

            Bitmap bitmap = iconGen.makeIcon();
            return BitmapDescriptorFactory.fromBitmap(bitmap);
        }
    }

    class ClusterRenderer extends DefaultClusterRenderer<ClusterStation> {
        private final IconGenerator mIconGenerator;
        private ShapeDrawable mColoredCircleBackground;
        private SparseArray<BitmapDescriptor> mIcons = new SparseArray();
        private final float mDensity;
        private Activity mContext;

        public ClusterRenderer(Activity context, GoogleMap map,
                               ClusterManager<ClusterStation> clusterManager) {
            super(context.getApplicationContext(), map, clusterManager);
            this.mContext = context;
            this.mDensity = context.getResources().getDisplayMetrics().density;
            this.mIconGenerator = new IconGenerator(context);
            this.mIconGenerator.setContentView(this.makeSquareTextView(context));
            this.mIconGenerator.setTextAppearance(
                    com.google.maps.android.R.style.ClusterIcon_TextAppearance);
            this.mIconGenerator.setBackground(this.makeClusterBackground());
        }

        @Override
        protected void onBeforeClusterItemRendered(ClusterStation item, MarkerOptions markerOptions) {
            markerOptions.icon(item.getIcon());
            markerOptions.anchor(0.5f, 0.5f);
            super.onBeforeClusterItemRendered(item, markerOptions);
        }

        @Override
        protected void onBeforeClusterRendered(Cluster<ClusterStation> cluster,
                                               MarkerOptions markerOptions) {
            // Main color
            Collection<ClusterStation> stations = cluster.getItems();
            Integer average_aqi = null;
            for (ClusterStation station : stations) {
                Integer aqi;
                if (mPollutantFilter != null)
                    aqi = station.station.getPollutantAqi(mPollutantFilter, station.station.getLastMeasurement());
                else
                    aqi = station.station.getMaxAqi();
                if (average_aqi == null) average_aqi = aqi;
                average_aqi = (average_aqi + aqi)/2;
            }
            if(average_aqi == null) average_aqi = 0;
            int clusterColor = AQI.getColor(average_aqi, mContext);

            int bucket = this.getBucket(cluster);
            BitmapDescriptor descriptor = this.mIcons.get(bucket);
            if(descriptor == null) {
                this.mColoredCircleBackground.getPaint().setColor(clusterColor);
                descriptor = BitmapDescriptorFactory.fromBitmap(
                        this.mIconGenerator.makeIcon(this.getClusterText(bucket)));
                this.mIcons.put(bucket, descriptor);
            }

            markerOptions.icon(descriptor);
        }

        private SquareTextView makeSquareTextView(Context context) {
            SquareTextView squareTextView = new SquareTextView(context);
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(-2, -2);
            squareTextView.setLayoutParams(layoutParams);
            squareTextView.setId(com.google.maps.android.R.id.text);
            int twelveDpi = (int)(12.0F * this.mDensity);
            squareTextView.setPadding(twelveDpi, twelveDpi, twelveDpi, twelveDpi);
            return squareTextView;
        }

        private LayerDrawable makeClusterBackground() {
            // Outline color
            int clusterOutlineColor = ContextCompat.getColor(mContext, R.color.white);

            this.mColoredCircleBackground = new ShapeDrawable(new OvalShape());
            ShapeDrawable outline = new ShapeDrawable(new OvalShape());
            outline.getPaint().setColor(clusterOutlineColor);
            LayerDrawable background = new LayerDrawable(
                    new Drawable[]{outline, this.mColoredCircleBackground});
            int strokeWidth = (int)(this.mDensity * 3.0F);
            background.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth);
            return background;
        }


        @Override
        protected boolean shouldRenderAsCluster(Cluster<ClusterStation> cluster) {
            return mCurrentZoom < 10;
        }
    }
}
