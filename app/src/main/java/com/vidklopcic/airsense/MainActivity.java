package com.vidklopcic.airsense;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLngBounds;
import com.vidklopcic.airsense.data.Constants;
import com.vidklopcic.airsense.data.DataAPI;
import com.vidklopcic.airsense.data.entities.MeasuringStation;
import com.vidklopcic.airsense.data.entities.SavedState;
import com.vidklopcic.airsense.fragments.AqiOverview;
import com.vidklopcic.airsense.util.AQI;
import com.vidklopcic.airsense.util.LocationHelper;
import com.vidklopcic.airsense.util.UI;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;

public class MainActivity extends FragmentActivity implements LocationHelper.LocationHelperListener, DataAPI.DataUpdateListener {
    AqiOverview mAqiOverviewFragment;
    private SlidingMenu mMenu;
    private LocationHelper mLocation;
    private List<MeasuringStation> mStations;
    private String mCity;
    private LatLngBounds mBounds;
    private DataAPI mDataAPI;
    private SavedState mSavedState;

    private TextView mCityText;
    private TextView mTemperatureText;
    private TextView mHumidityText;
    private LinearLayout mSubtitleContainer;
    private TextView mAqiNameSubtitle;
    private SwipeRefreshLayout mSwipeRefresh;
    private Realm mRealm;
    private SlidingUpPanelLayout mFavoritesSlidingMenu;
    private boolean mNoData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStations = new ArrayList<>();
        mRealm = DataAPI.getRealmOrCreateInstance(this);
        setContentView(R.layout.activity_main);
        mAqiOverviewFragment = (AqiOverview) getSupportFragmentManager().findFragmentById(R.id.overview_fragment);
        mMenu = UI.getSlidingMenu(getWindowManager(), this);
        mLocation = new LocationHelper(this);
        mLocation.setLocationHelperListener(this);
        mDataAPI = new DataAPI(this);

        mSubtitleContainer = (LinearLayout) findViewById(R.id.actionbar_aqi_subtitle_container);
        mCityText = (TextView) findViewById(R.id.actionbar_title_text);
        mTemperatureText = (TextView) findViewById(R.id.actionbar_temperature_text);
        mHumidityText = (TextView) findViewById(R.id.actionbar_humidity_text);
        mAqiNameSubtitle = (TextView)findViewById(R.id.actionbar_aqi_text);
        mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mDataAPI.updateData();
                if (mNoData) {
                    mSwipeRefresh.setRefreshing(false);
                }
            }
        });
        mSwipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefresh.setRefreshing(true);
            }
        });

        mAqiOverviewFragment.setOnLoadedListener(new AqiOverview.OnFragmentLoadedListener() {
            @Override
            public void onLoaded() {
                mSavedState = SavedState.getSavedState(mRealm);
                if (mLocation.isLocationEnabled() && mSavedState.getCity() != null && mBounds == null) {
                    onCityChange(mSavedState.getCity(), mSavedState.getBounds());
                }
                mDataAPI.setDataUpdateListener(MainActivity.this);
            }
        });
        mFavoritesSlidingMenu = (SlidingUpPanelLayout) findViewById(R.id.favorites_sliding_layout);
    }

    private void restoreSavedState() {
        mSavedState = SavedState.getSavedState(mRealm);
        if (mSavedState.getCity() != null) {
            onCityChange(mSavedState.getCity(), mSavedState.getBounds());
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        mDataAPI.setObservedStations(new ArrayList<MeasuringStation>());
        mDataAPI.pauseUpdateTask();
    }

    @Override
    public void onResume() {
        super.onResume();
        mLocation.startLocationReading();
        if (!mLocation.isLocationEnabled()) {
            setLocationDisabled();
        } else if (mCity == null) {
            restoreSavedState();
            if (mCity == null) {
                setWaitingForLocation();
            }
        }
        mDataAPI.setObservedStations(mStations);
        mDataAPI.resumeUpdateTask();
    }

    private void setLocationDisabled() {
        mCityText.setText(getString(R.string.location_disabled));
        mSwipeRefresh.setRefreshing(false);
    }

    private void setWaitingForLocation() {
        mCityText.setText(getString(R.string.waiting_for_location));
    }

    public void openSlidingMenu(View view) {
        mMenu.showMenu();
    }

    public void startMaps(View view) {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case LocationHelper.LOCATION_PERMISSION_RESULT: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocation.startLocationReading();
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onCityChange(String city, LatLngBounds bounds) {
        if (bounds == null) {
            if (city != null) {
                setNoData();
            }
            return;
        }

        mBounds = bounds;
        mCity = city;
        if ((mSavedState.getCity() != null && !mSavedState.getBounds().equals(bounds)) || mSavedState.getCity() == null) {
            mSavedState.setCity(mRealm, city, bounds);
        }

        mStations = MeasuringStation.getStationsInArea(mRealm, bounds);
        mDataAPI.setObservedStations(mStations);
        ArrayList<HashMap<String, Integer>> averages = mAqiOverviewFragment.updateGraph(mStations);
        updateDashboard(averages);
        if (averages == null) {
            setNoData();
        } else {
            mNoData = false;
        }
    }


    @Override
    public void onDataReady() {
        onCityChange(mCity, mBounds);
        if (mStations.size() == 0) {
            return;
        }
        ArrayList<HashMap<String, Integer>> averages = mAqiOverviewFragment.updateGraph(mStations);
        setFetchingData();
        updateDashboard(averages);
    }

    @Override
    public void onDataUpdate() {
        if (mStations.size() == 0)
            return;
        ArrayList<HashMap<String, Integer>> averages = mAqiOverviewFragment.updateGraph(mStations);
        mSwipeRefresh.setRefreshing(false);
        updateDashboard(averages);
    }

    @Override
    public void onStationUpdate(MeasuringStation station) {
    }

    private void updateDashboard(ArrayList<HashMap<String, Integer>> averages) {
        if (averages == null) return;
        mSwipeRefresh.setRefreshing(false);
        HashMap<String, Integer> other = averages.get(MeasuringStation.AVERAGES_OTHER);
        if (other.keySet().contains(Constants.ARSOStation.TEMPERATURE_KEY)) {
            String temp = other.get(Constants.ARSOStation.TEMPERATURE_KEY).toString() + Constants.TEMPERATURE_UNIT;
            mTemperatureText.setText(temp);
        }

        if (other.keySet().contains(Constants.ARSOStation.HUMIDITY_KEY)) {
            String hum = other.get(Constants.ARSOStation.HUMIDITY_KEY).toString() + Constants.HUMIDITY_UNIT;
            mHumidityText.setText(hum);
        }

        mSubtitleContainer.setVisibility(View.VISIBLE);
        mCityText.setText(mCity);
        int max_aqi_val = Collections.max(averages.get(MeasuringStation.AVERAGES_POLLUTANTS).values());
        mAqiNameSubtitle.setText(AQI.toText(Collections.max(averages.get(MeasuringStation.AVERAGES_POLLUTANTS).values())));
        mAqiNameSubtitle.setTextColor(getResources().getColor(AQI.getColor(max_aqi_val)));
    }

    private void setNoData() {
        mCityText.setText(getString(R.string.no_data_available));
        mSwipeRefresh.setRefreshing(false);
        mNoData = true;
    }

    private void setFetchingData() {
        mCityText.setText(getString(R.string.fetching_data));
    }

    public void openFavorites(View view) {
        if (mFavoritesSlidingMenu.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)
            mFavoritesSlidingMenu.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        if (mFavoritesSlidingMenu.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED)
            mFavoritesSlidingMenu.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
    }
}
