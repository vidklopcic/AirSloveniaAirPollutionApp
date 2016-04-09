package com.citisense.vidklopcic.citisense;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.data.DataAPI;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.data.entities.SavedState;
import com.citisense.vidklopcic.citisense.fragments.AqiOverview;
import com.citisense.vidklopcic.citisense.util.AQI;
import com.citisense.vidklopcic.citisense.util.LocationHelper;
import com.citisense.vidklopcic.citisense.util.UI;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class MainActivity extends FragmentActivity implements LocationHelper.LocationHelperListener, DataAPI.DataUpdateListener {
    AqiOverview mAqiOverviewFragment;
    private SlidingMenu mMenu;
    private LocationHelper mLocation;
    private List<CitiSenseStation> mStations;
    private String mCity;
    private DataAPI mDataAPI;
    private SavedState mSavedState;

    private TextView mCityText;
    private TextView mTemperatureText;
    private TextView mHumidityText;
    private LinearLayout mSubtitleContainer;
    private TextView mAqiNameSubtitle;
    private SwipeRefreshLayout mSwipeRefresh;
    private Realm mRealm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RealmConfiguration config = new RealmConfiguration.Builder(this).build();
        Realm.setDefaultConfiguration(config);
        mRealm = Realm.getDefaultInstance();
        setContentView(R.layout.activity_main);
        mAqiOverviewFragment = (AqiOverview) getSupportFragmentManager().findFragmentById(R.id.overview_fragment);
        mMenu = UI.getSlidingMenu(getWindowManager(), this);
        mLocation = new LocationHelper(this);
        mLocation.setLocationHelperListener(this);
        mDataAPI = new DataAPI();

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
            }
        });

        mAqiOverviewFragment.setOnLoadedListener(new AqiOverview.OnFragmentLoadedListener() {
            @Override
            public void onLoaded() {
                mSavedState = SavedState.getSavedState(mRealm);
                if (mLocation.isLocationEnabled() && mSavedState.getCity() != null && mCity == null) {
                    onCityChange(mSavedState.getCity());
                }
            }
        });
        mSwipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefresh.setRefreshing(true);
            }
        });
    }

    private void restoreSavedState() {
        mSavedState = SavedState.getSavedState(mRealm);
        if (mSavedState.getCity() != null) {
            onCityChange(mSavedState.getCity());
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        mDataAPI.setObservedStations(new ArrayList<CitiSenseStation>());
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
    public void onCityChange(String city) {
        if (city == null)
            return;
        mCity = city;
        if ((mSavedState.getCity() != null && !mSavedState.getCity().equals(city)) || mSavedState.getCity() == null) {
            mSavedState.setCity(mRealm, city);
        }
        mStations = mRealm.where(CitiSenseStation.class).equalTo("city", mCity).findAll();
        if (mStations.size() == 0) {
            setNoData();
            return;
        }
        mDataAPI.setObservedStations(mStations);
        mDataAPI.setDataUpdateListener(this);
        ArrayList<HashMap<String, Integer>> averages = mAqiOverviewFragment.updateGraph(mStations);
        updateDashboard(averages);
    }


    @Override
    public void onDataReady() {
        mStations = mRealm.where(CitiSenseStation.class).equalTo("city", mCity).findAll();
        mDataAPI.setObservedStations(mStations);
        if (mStations.size() == 0) {
            setNoData();
            return;
        }
        ArrayList<HashMap<String, Integer>> averages = mAqiOverviewFragment.updateGraph(mStations);
        setFetchingData();
        updateDashboard(averages);
    }

    @Override
    public void onDataUpdate() {
        ArrayList<HashMap<String, Integer>> averages = mAqiOverviewFragment.updateGraph(mStations);
        mSwipeRefresh.setRefreshing(false);
        updateDashboard(averages);
    }

    @Override
    public void onStationUpdate(CitiSenseStation station) {
    }

    private void updateDashboard(ArrayList<HashMap<String, Integer>> averages) {
        if (averages == null) return;
        mSwipeRefresh.setRefreshing(false);
        HashMap<String, Integer> other = averages.get(CitiSenseStation.AVERAGES_OTHER);
        String temp = other.get(Constants.CitiSenseStation.TEMPERATURE_KEY).toString() + Constants.TEMPERATURE_UNIT;
        String hum = other.get(Constants.CitiSenseStation.HUMIDITY_KEY).toString() + Constants.HUMIDITY_UNIT;
        mSubtitleContainer.setVisibility(View.VISIBLE);
        mCityText.setText(mCity);
        mTemperatureText.setText(temp);
        mHumidityText.setText(hum);
        int max_aqi_val = Collections.max(averages.get(CitiSenseStation.AVERAGES_POLLUTANTS).values());
        mAqiNameSubtitle.setText(AQI.toText(Collections.max(averages.get(CitiSenseStation.AVERAGES_POLLUTANTS).values())));
        mAqiNameSubtitle.setTextColor(getResources().getColor(AQI.getColor(max_aqi_val)));
    }

    private void setNoData() {
        mCityText.setText(getString(R.string.no_data_available));
        mSwipeRefresh.setRefreshing(false);
    }

    private void setFetchingData() {
        mCityText.setText(getString(R.string.fetching_data));
    }
}
