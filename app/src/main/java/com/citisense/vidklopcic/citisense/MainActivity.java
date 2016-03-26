package com.citisense.vidklopcic.citisense;

import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.citisense.vidklopcic.citisense.fragments.AqiOverviewGraph;
import com.citisense.vidklopcic.citisense.util.AQI;
import com.citisense.vidklopcic.citisense.util.LocationHelper;
import com.citisense.vidklopcic.citisense.util.UI;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class MainActivity extends FragmentActivity implements LocationHelper.LocationHelperListener, DataAPI.DataUpdateListener {
    AqiOverviewGraph mChartFragment;
    private SlidingMenu mMenu;
    private UI.AQISummary mAQISummary;
    private LocationHelper mLocation;
    private ArrayList<CitiSenseStation> mStations;
    private String mCity;
    private DataAPI mDataAPI;
    private SavedState mSavedState;

    private TextView mCityText;
    private TextView mTemperatureText;
    private TextView mHumidityText;
    private LinearLayout mSubtitleContainer;
    private TextView mAqiNameSubtitle;
    private SwipeRefreshLayout mSwipeRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mChartFragment = (AqiOverviewGraph) getFragmentManager().findFragmentById(R.id.dashboard_bar_chart_fragment);
        mMenu = UI.getSlidingMenu(getWindowManager(), this);
        mAQISummary = new UI.AQISummary(this, R.id.dashboard_aqi_summary_layout);
        mLocation = new LocationHelper(this);
        mLocation.setLocationHelperListener(this);
        mDataAPI = new DataAPI();

        mSubtitleContainer = (LinearLayout) findViewById(R.id.dashboard_aqi_subtitle_container);
        mCityText = (TextView) findViewById(R.id.dashboard_city_text);
        mTemperatureText = (TextView) findViewById(R.id.dashboard_temperature_text);
        mHumidityText = (TextView) findViewById(R.id.dashboard_humidity_text);
        mAqiNameSubtitle = (TextView)findViewById(R.id.dashboard_aqi_name_subtitle);
        mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mDataAPI.updateData();
            }
        });

        mSavedState = new SavedState().getSavedState();
        if (mSavedState.getCity() != null) {
            onCityChange(mSavedState.getCity());
        }
        mSwipeRefresh.post(new Runnable() {
            @Override public void run() {
                mSwipeRefresh.setRefreshing(true);
            }
        });
    }

    public void fragmentClicked(View view) {
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
    public void onLocationChanged() {

    }

    @Override
    public void onCityChange(String city) {
        mCity = city;
        if ((mSavedState.getCity() != null && !mSavedState.getCity().equals(city)) || mSavedState.getCity() == null) {
            mSavedState.setCity(city);
        }
        mStations = (ArrayList<CitiSenseStation>)
                CitiSenseStation.find(CitiSenseStation.class, "city = ?", mCity);
        mDataAPI.setDataUpdateListener(this);
        ArrayList<HashMap<String, Integer>> averages = mChartFragment.updateGraph(mStations);
        if (averages == null) return;
        updateDashboard(averages);
    }


    @Override
    public void onDataReady() {
        ArrayList<CitiSenseStation> city_stations = (ArrayList<CitiSenseStation>)
                CitiSenseStation.find(CitiSenseStation.class, "city = ?", mCity);
        mStations = city_stations;
        mDataAPI.setObservedStations(city_stations);
        ArrayList<HashMap<String, Integer>> averages = mChartFragment.updateGraph(mStations);
        if (averages == null) return;
        mSwipeRefresh.setRefreshing(false);
        updateDashboard(averages);
    }

    @Override
    public void onDataUpdate() {
        ArrayList<HashMap<String, Integer>> averages = mChartFragment.updateGraph(mStations);
        if (averages == null) return;
        mSwipeRefresh.setRefreshing(false);
        updateDashboard(averages);
    }

    @Override
    public void onStationUpdate(CitiSenseStation station) {
    }

    private void updateDashboard(ArrayList<HashMap<String, Integer>> averages) {
        HashMap<String, Integer> other = averages.get(1);
        String temp = other.get(Constants.CitiSenseStation.TEMPERATURE_KEY).toString() + "°C";
        String hum = other.get(Constants.CitiSenseStation.HUMIDITY_KEY).toString() + "%";
        mSubtitleContainer.setVisibility(View.VISIBLE);
        mCityText.setText(mCity);
        mTemperatureText.setText(temp);
        mHumidityText.setText(hum);
        int max_aqi_val = Collections.max(averages.get(0).values());
        mAqiNameSubtitle.setText(AQI.toText(Collections.max(averages.get(0).values())));
        mAqiNameSubtitle.setTextColor(getResources().getColor(AQI.getColor(max_aqi_val)));
        mAQISummary.setAqi(max_aqi_val);
    }
}
