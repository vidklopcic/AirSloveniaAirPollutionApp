package com.vidklopcic.airsense;

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

import com.google.android.gms.maps.model.Circle;
import com.vidklopcic.airsense.data.Constants;
import com.vidklopcic.airsense.data.DataAPI;
import com.vidklopcic.airsense.data.entities.MeasuringStation;
import com.vidklopcic.airsense.data.entities.FavoritePlace;
import com.vidklopcic.airsense.data.entities.SavedState;
import com.vidklopcic.airsense.fragments.MapCards;
import com.vidklopcic.airsense.util.AQI;
import com.vidklopcic.airsense.util.FABPollutants;
import com.vidklopcic.airsense.util.LocationHelper;
import com.vidklopcic.airsense.util.MapPullUpPager;
import com.vidklopcic.airsense.util.Overlay.MapOverlay;
import com.vidklopcic.airsense.util.UI;
import com.vidklopcic.airsense.util.anim.BackgroundColorAnimation;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;


public class MapsActivity extends FragmentActivity implements LocationHelper.LocationHelperListener, PlaceSelectionListener, OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener, GoogleMap.OnCameraChangeListener, ClusterManager.OnClusterItemClickListener<MapsActivity.ClusterStation>, DataAPI.DataUpdateListener, FABPollutants.FABPollutantsListener {
    public static final float DASHBOARD_HEIGHT = 0.4f;
    private MapOverlay mOverlay;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationHelper mLocation;
    private SlidingMenu mMenu;
    private Marker mCurrentMarker;
    private LatLng mPointOfInterest;
    private Place mCurrentPlace;
    private ClusterManager<ClusterStation> mClusterManager;
    private SavedState mSavedState;
    private HashMap<MeasuringStation, ClusterStation> mStationsOnMap;
    private DataAPI mDataApi;
    private Float mCurrentZoom;
    private SlidingUpPanelLayout mSlidingPane;
    private MapCards mMapCardsFragment;
    private FABPollutants mFABPollutants;
    private String mPollutantFilter;
    private LinearLayout mActionBarContainer;
    private Integer mActionBarHeight;
    private Integer mDashboardBarHeight;
    private EditText mActionBarTitle;
    private String mPOIAddress;
    private boolean mActionBarHasAddress = false;
    private RelativeLayout mSearchContaienr;
    private MapPullUpPager mPullUpPager;
    private ImageView mFavoritesStar;
    private boolean mPOIIsFavorite = false;
    private Realm mRealm;
    private boolean mMapPositioned = false;
    private boolean mMapRestored = false;
    private ArrayList<Circle> mCircles;
    private boolean mDashboardVisible = false;
    private TextView mCityText;
    private TextView mTemperatureText;
    private TextView mHumidityText;
    private TextView mAqiNameSubtitle;
    private RelativeLayout mDashboardBarContainer;
    private boolean mDashboardDismissed = false;
    private int mAnchoredHeight = 0;
    private List<MeasuringStation> mRegionStations;
    private boolean mIsDuringChange = false;
    private LatLngBounds mPrevBounds;
    private View mTouchBlockView;
    private View mMapTouchBlockView;
    private LatLngBounds mDashboardBounds;
    private float mZoom = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mCityText = (TextView) findViewById(R.id.dashboard_title_text);
        mTemperatureText = (TextView) findViewById(R.id.actionbar_temperature_text);
        mHumidityText = (TextView) findViewById(R.id.actionbar_humidity_text);
        mAqiNameSubtitle = (TextView)findViewById(R.id.actionbar_aqi_text);

        mTouchBlockView = (View) findViewById(R.id.touch_block_view);
        mTouchBlockView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
        unblockTouch();

        mMapTouchBlockView = (View) findViewById(R.id.map_touch_block_view);
        mMapTouchBlockView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (mPrevBounds == null) {
                        positionMap(false);
                    }
                    hideDashboard();
                    unblockMapTouch();
                }
                return true;
            }
        });
        unblockMapTouch();

        mRealm = DataAPI.getRealmOrCreateInstance(this);
        mStationsOnMap = new HashMap<>();
        mCircles = new ArrayList<>();
        mSavedState = SavedState.getSavedState(mRealm);
        mDataApi = new DataAPI(this);
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
                mIsDuringChange = true;
                blockTouch();
            }

            @Override
            public void onPanelStateChanged(View view, SlidingUpPanelLayout.PanelState panelState, SlidingUpPanelLayout.PanelState panelState1) {
                mIsDuringChange = false;
                unblockTouch();
                if (panelState1 == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    mMapCardsFragment.hide();
                    mActionBarContainer.startAnimation(new BackgroundColorAnimation(
                            mActionBarContainer,
                            ContextCompat.getColor(getContext(), R.color.maps_pullup_actionbar)));
                    mPullUpPager.setOverviewFragment();
                    mSlidingPane.setEnabled(false);
                } else if (panelState1 == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    mSlidingPane.setAnchorPoint(1f);
                    mMapCardsFragment.show();
                    mPullUpPager.close();
                    mActionBarContainer.startAnimation(new BackgroundColorAnimation(
                            mActionBarContainer,
                            ContextCompat.getColor(getContext(), R.color.maps_blue)));
                } else if (panelState1 == SlidingUpPanelLayout.PanelState.ANCHORED) {
                    mSlidingPane.setEnabled(false);
                } else if (panelState1 == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    mSlidingPane.setAnchorPoint(1f);
                }
            }
        });

        mSlidingPane.setCoveredFadeColor(ContextCompat.getColor(this, R.color.transparent));

        mActionBarContainer = (LinearLayout) findViewById(R.id.maps_action_bar);
        mDashboardBarContainer = (RelativeLayout) findViewById(R.id.dashboard_bar_container);

        mDashboardBarHeight = (int) getResources().getDimension(R.dimen.dashboard_action_bar_height);
        mDashboardBarContainer.animate().translationY(-mDashboardBarHeight).setDuration(0).start();
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
        mPullUpPager.setDashboardOverviewFragment();
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                mAnchoredHeight = (int) (DASHBOARD_HEIGHT*(mSlidingPane.getMeasuredHeight()-mSlidingPane.getPanelHeight()-getResources().getDimension(R.dimen.action_bar_height))+mSlidingPane.getPanelHeight());
                mPullUpPager.setDashboardBarHeight(mAnchoredHeight);
                mSavedState = SavedState.getSavedState(mRealm);
                if (mLocation.isLocationEnabled() && mSavedState.getCity() != null) {
                    onCityChange(mSavedState.getCity(), mSavedState.getBounds());
                    mDashboardDismissed = false;
                }
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mDataApi == null || mLocation == null) {
            return;
        }
        mDataApi.pauseUpdateTask();
        mLocation.stopLocationReading();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        mDataApi.resumeUpdateTask();
        if (mLocation != null)
            mLocation.startLocationReading();
    }

    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
    }

    private void removePointOfInterest() {
        if (mIsDuringChange)
            return;
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
        if (mIsDuringChange)
            return;
        if (mCurrentMarker != null) {
            mCurrentMarker.remove();
        }
        mPointOfInterest = poi;
        mCurrentMarker = marker;
        mSlidingPane.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        showActionBar();
    }

    public void setActionBar(String title) {
        mPOIAddress = title;
        clearActionBarData();
        if (title == null) return;
        mActionBarTitle.setText(title);
        mActionBarHasAddress = true;
        mActionBarTitle.setEnabled(true);
        RealmResults<FavoritePlace> places = mRealm.where(FavoritePlace.class).equalTo("address", mPOIAddress).findAll();
        if (places.size() != 0) {
            setFavorite(true);
            mActionBarTitle.setText(places.get(0).getNickname());
        }
    }


    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void blockTouch() {
        mTouchBlockView.setVisibility(View.VISIBLE);
    }

    private void unblockTouch() {
        mTouchBlockView.setVisibility(View.GONE);
    }

    private void blockMapTouch() {
        mMapTouchBlockView.setVisibility(View.VISIBLE);
    }

    private void unblockMapTouch() {
        mMapTouchBlockView.setVisibility(View.GONE);
    }

    public void clearActionBarData() {
        mActionBarTitle.setText("...");
        mActionBarHasAddress = false;
    }

    public void showDashboard() {
        if (mDashboardVisible || mDashboardDismissed || !mLocation.isLocationEnabled() || mIsDuringChange)
            return;
        blockMapTouch();
        mDashboardVisible = true;
        mDashboardBarContainer.animate().translationY(0).setDuration(400).start();
        mSearchContaienr.animate().alpha(0).setDuration(400).start();
        mSlidingPane.setAnchorPoint(DASHBOARD_HEIGHT);
        mPullUpPager.setDashboardOverviewFragment();
        mSlidingPane.post(new Runnable() {
            @Override
            public void run() {
                if (mRegionStations != null && mRegionStations.size() > 0) {
                    mSlidingPane.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                    mPullUpPager.setDataSource(mRegionStations);
                }
            }
        });
    }

    public void hideDashboard() {
        unblockMapTouch();
        mDashboardDismissed = true;
        if (!mDashboardVisible || mIsDuringChange)
            return;
        if (mPrevBounds != null) {
            blockTouch();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mPrevBounds, 0), new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    unblockTouch();
                }

                @Override
                public void onCancel() {
                    unblockTouch();
                }
            });
            mPrevBounds = null;
        }
        mDashboardVisible = false;
        mDashboardBarContainer.animate().translationY(-mDashboardBarHeight).setDuration(200).start();
        mSearchContaienr.animate().alpha(1).setDuration(400).start();
        mSlidingPane.setEnabled(true);
        mSlidingPane.post(new Runnable() {
            @Override
            public void run() {
                mSlidingPane.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                mPullUpPager.close();
            }
        });
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
                matches.deleteAllFromRealm();
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
        if (mIsDuringChange)
            return;
        if (mSlidingPane.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            removePointOfInterest();
        } else if (mSlidingPane.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            mSlidingPane.setEnabled(true);
            mSlidingPane.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else if (!mDashboardVisible && mRegionStations != null && mRegionStations.size() > 0) {
            if (mLocation.isLocationEnabled()) {
                mPrevBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                mDashboardDismissed = false;
                showDashboard();
                mMapPositioned = false;
                positionMap(true);
            } else if (mLocation.locationIsTurnedOn()) {
                mPrevBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                mLocation.startLocationReading();
                mDashboardDismissed = false;
                showDashboard();
                mMapPositioned = false;
                positionMap(true);
            } else {
                    mDashboardVisible = true;
                    mDashboardBarContainer.animate().translationY(0).setDuration(400).start();
                    mSearchContaienr.animate().alpha(0).setDuration(400).start();
                    mCityText.setText("Location is disabled");
            }
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
            SupportMapFragment map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
            map.getMapAsync(this);
            View v = map.getView();
            if (v != null) {
                v.post(new Runnable() {
                    @Override
                    public void run() {
                        positionMap(true);
                    }
                });
            }
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
        if (mLocation.hasPermission()) {
            mLocation.startLocationReading();
            mMap.setMyLocationEnabled(true);
        }
        mLocation.setLocationHelperListener(this);
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onCityChange(String city, LatLngBounds bounds) {
        mRegionStations = new ArrayList<>();
        if (bounds != null && MeasuringStation.getStationsInArea(mRealm, bounds).size() > 0) {
            mRegionStations = MeasuringStation.getStationsInArea(mRealm, bounds);
        } else if (mLocation.getRegion().length() > 0 && MeasuringStation.getStationsInArea(mRealm, mLocation.getRegionBounds()).size() > 0) {
            mRegionStations = MeasuringStation.getStationsInArea(mRealm, mLocation.getRegionBounds());
            city = mLocation.getRegion();
            bounds = mLocation.getRegionBounds();
        } else if (mLocation.getCountry().length() > 0 && MeasuringStation.getStationsInArea(mRealm, mLocation.getCountryBounds()).size() > 0) {
            mRegionStations = MeasuringStation.getStationsInArea(mRealm, mLocation.getCountryBounds());
            city = mLocation.getCountry();
            bounds = mLocation.getCountryBounds();
        } else {
            hideDashboard();
        }

        if (mDashboardDismissed) {
            return;
        }

        if ((mSavedState.getCity() != null && !mSavedState.getBounds().equals(bounds)) || mSavedState.getCity() == null) {
            mSavedState.setCity(mRealm, city, bounds);
        }
        mPullUpPager.setDashboardOverviewFragment();
        mPullUpPager.setDataSource(mRegionStations);
        ArrayList<HashMap<String, Integer>> averages = mPullUpPager.update();
        if (averages == null) {
            positionMap(false);
            if (mRegionStations.size() == 0) {
                hideDashboard();
            }
            mCityText.setText("No data for your location");
        } else {
            updateDashboard(averages, city);
            positionMap(true);
            showDashboard();
        }
    }

    private void updateDashboard(ArrayList<HashMap<String, Integer>> averages, String city) {
        if (averages == null) return;
        HashMap<String, Integer> other = averages.get(MeasuringStation.AVERAGES_OTHER);
        if (other.keySet().contains(Constants.ARSOStation.TEMPERATURE_KEY)) {
            String temp = other.get(Constants.ARSOStation.TEMPERATURE_KEY).toString() + Constants.TEMPERATURE_UNIT;
            mTemperatureText.setText(temp);
        }

        if (other.keySet().contains(Constants.ARSOStation.HUMIDITY_KEY)) {
            String hum = other.get(Constants.ARSOStation.HUMIDITY_KEY).toString() + Constants.HUMIDITY_UNIT;
            mHumidityText.setText(hum);
        }

        mCityText.setText(city);
        int max_aqi_val = Collections.max(averages.get(MeasuringStation.AVERAGES_POLLUTANTS).values());
        mAqiNameSubtitle.setText(AQI.toText(Collections.max(averages.get(MeasuringStation.AVERAGES_POLLUTANTS).values())));
        mAqiNameSubtitle.setTextColor(getResources().getColor(AQI.getColor(max_aqi_val)));
    }

    public void onMenuClicked(View view) {
        mMenu.showMenu();
    }

    @Override
    public void onPlaceSelected(Place place) {
        mCurrentPlace = place;
        LatLngBounds bounds = mCurrentPlace.getViewport();
        if (bounds != null) {
            blockTouch();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mCurrentPlace.getViewport(), 0), new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    unblockTouch();
                }

                @Override
                public void onCancel() {
                    unblockTouch();
                }
            });
        } else {
            blockTouch();
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), Constants.Map.default_zoom), new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    unblockTouch();
                }

                @Override
                public void onCancel() {
                    unblockTouch();
                }
            });
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
                    mLocation.startLocationReading();
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
    }


    private void positionMap(boolean dashboard) {
        if ((mMapPositioned || mDashboardDismissed) && dashboard)
            return;
        try {
            if (dashboard) {
                mMap.setPadding(0, mDashboardBarHeight, 0, mAnchoredHeight);
            }
            LatLngBounds lastviewport = mSavedState.getLastViewport();
            if (mLocation.getRegionBounds() != null) {
                blockTouch();
                mSavedState.setLastViewport(mRealm, mLocation.getRegionBounds());
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mLocation.getRegionBounds(), 0), 1000, new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        unblockTouch();
                    }

                    @Override
                    public void onCancel() {
                        unblockTouch();
                    }
                });
                mMapPositioned = true;
            } else if (mLocation.getCountryBounds() != null) {
                blockTouch();
                mSavedState.setLastViewport(mRealm, mLocation.getCountryBounds());
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mLocation.getCountryBounds(), 0), 1000, new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        unblockTouch();
                    }

                    @Override
                    public void onCancel() {
                        unblockTouch();
                    }
                });
                mMapPositioned = true;
            } else if (mSavedState.getLastViewport() != null && !dashboard) {
                blockTouch();
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mSavedState.getLastViewport(), 0), 1000, new GoogleMap.CancelableCallback() {
                    @Override
                    public void onFinish() {
                        unblockTouch();
                    }

                    @Override
                    public void onCancel() {
                        unblockTouch();
                    }
                });
                mMapPositioned = true;
            } else if (lastviewport != null && !mMapRestored) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(lastviewport, 0));
                mMapRestored = true;
            }
        } catch (Exception ignored) {
        }
        mMap.setPadding(0, 0, 0, 0);
    }


    public void addStationToMap(MeasuringStation station) {
        if (mPollutantFilter == null || station.hasPollutant(mPollutantFilter)) {
            ClusterStation new_c_station = new ClusterStation(station.getLocation(), station);
            Integer linear_color;
//            if (!station.hasUpdatedData()) {
//                linear_color = ContextCompat.getColor(this, R.color.gray);
//            } else if (mPollutantFilter != null) {
//                linear_color = AQI.getLinearColor(station.getAqi(mPollutantFilter), this);
//            } else {
//                linear_color = AQI.getLinearColor(station.getMaxAqi(), this);
//            }
//            if (station.hasUpdatedData()) {
//                CircleOptions circleOptions = new CircleOptions()
//                        .center(station.getLocation())   //set center
//                        .radius(1000)   //set radius in meters
//                        .fillColor(Conversion.adjustAlpha(linear_color, 0.3f))  //default
//                        .strokeColor(Conversion.adjustAlpha(linear_color, 0.6f))
//                        .strokeWidth(5);
//
//                mCircles.add(mMap.addCircle(circleOptions));
//            }
            if (!mStationsOnMap.containsKey(station)) {
                Log.d("MapsActivity", "added " + new_c_station.station.getStationId());
                mStationsOnMap.put(station, new_c_station);
                mClusterManager.addItem(new_c_station);
                mClusterManager.cluster();
            }
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
//        ArrayList<MeasuringStation> affecting_stations = (ArrayList<MeasuringStation>) MeasuringStation.getStationsAroundPoint(mRealm, latLng, Constants.Map.station_radius_meters);
//        mPullUpPager.setDataSource(affecting_stations);
//        mPollutantCardsFragment.setSourceStations(affecting_stations);
//        setActionBar(latLng);
//        setPointOfInterest(latLng, mMap.addMarker(new MarkerOptions().position(latLng)));
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        mZoom = cameraPosition.zoom;
        List<MeasuringStation> viewport_stations = MeasuringStation.getStationsInArea(
                mRealm, mMap.getProjection().getVisibleRegion().latLngBounds
        );
        mFABPollutants.update(viewport_stations);
//        if (cameraPosition.zoom >= Constants.Map.max_overlay_zoom)
//            mOverlay.draw(new ArrayList<>(viewport_stations), mMap.getProjection());
        mDataApi.setObservedStations(viewport_stations);
        List<String> stations_on_map = MeasuringStation.stationsToIdList(new ArrayList<>(mStationsOnMap.keySet()));
        List<String> viewport_stations_ids = MeasuringStation.stationsToIdList(viewport_stations);
        viewport_stations_ids.removeAll(stations_on_map);

        for (MeasuringStation station : viewport_stations) {
            addStationToMap(station);
        }

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
        setFavorite(false);
        setActionBar(clusterStation.station.getCity());
        setPointOfInterest(clusterStation.getPosition());
        mMapCardsFragment.setSourceStations(clusterStation.station);
        ArrayList<MeasuringStation> list = new ArrayList<>();
        list.add(clusterStation.station);
        mPullUpPager.setDataSource(list);
        return false;
    }

    @Override
    public void onDataReady() {
    }

    @Override
    public void onDataUpdate() {
        mClusterManager.clearItems();
        for (Circle circle : mCircles) {
            circle.remove();
        }
        mCircles.clear();
        mClusterManager.cluster();
        mStationsOnMap.clear();
        mPullUpPager.update();
        onCameraChange(mMap.getCameraPosition());
    }

    @Override
    public void onStationUpdate(MeasuringStation station) {
    }

    @Override
    public void onFABPollutantSelected(String pollutant) {
        mOverlay.setPollutant(pollutant);
        mPollutantFilter = pollutant;
        onDataUpdate();
    }

    public class ClusterStation implements ClusterItem {
        private final LatLng mPosition;
        MeasuringStation station;

        public ClusterStation(LatLng position, MeasuringStation station) {
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
            Integer linear_color;
            if (!station.hasCachedData()) {
                linear_color = ContextCompat.getColor(getContext(), R.color.dark_gray);
            } else if (mPollutantFilter != null) {
                linear_color = AQI.getLinearColor(station.getAqi(mPollutantFilter), getContext());
            } else {
                linear_color = AQI.getLinearColor(station.getMaxAqi(), getContext());
            }
            shapeDrawable.setColorFilter(linear_color, PorterDuff.Mode.MULTIPLY);
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
                    aqi = station.station.getAqi(mPollutantFilter);
                else
                    aqi = station.station.getMaxAqi();
                if (average_aqi == null) average_aqi = aqi;
                average_aqi = (average_aqi + aqi) / 2;
            }
            if (average_aqi == null) average_aqi = 0;
            int clusterColor = AQI.getColor(average_aqi, mContext);

            int bucket = this.getBucket(cluster);
            BitmapDescriptor descriptor = this.mIcons.get(bucket);
            if (descriptor == null) {
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
            int twelveDpi = (int) (12.0F * this.mDensity);
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
            int strokeWidth = (int) (this.mDensity * 3.0F);
            background.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth);
            return background;
        }


        @Override
        protected boolean shouldRenderAsCluster(Cluster<ClusterStation> cluster) {
            return false;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mPullUpPager.backPressed()) {
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }
}
