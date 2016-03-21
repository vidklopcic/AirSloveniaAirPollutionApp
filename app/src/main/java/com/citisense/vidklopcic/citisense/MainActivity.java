package com.citisense.vidklopcic.citisense;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.data.DataAPI;
import com.citisense.vidklopcic.citisense.fragments.AqiOverviewGraph;
import com.citisense.vidklopcic.citisense.util.LocationHelper;
import com.citisense.vidklopcic.citisense.util.SlidingMenuHelper;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

public class MainActivity extends FragmentActivity implements LocationHelper.LocationHelperListener {
    AqiOverviewGraph mChartFragment;
    private SlidingMenu mMenu;
    private LinearLayout mAQISummary;
    private LocationHelper mLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mChartFragment = (AqiOverviewGraph) getFragmentManager().findFragmentById(R.id.dashboard_bar_chart_fragment);
        mMenu = SlidingMenuHelper.attach(getWindowManager(), this);
        LinearLayout aqiSummaryContainer = (LinearLayout) findViewById(R.id.dashboard_aqi_summary_layout);
        mAQISummary = (LinearLayout) getLayoutInflater().inflate(R.layout.dashboard_aqi_summary,
                aqiSummaryContainer, false);
        aqiSummaryContainer.addView(mAQISummary);
        setAQISummary(200);
        mLocation = new LocationHelper(this);
        mLocation.setLocationHelperListener(this);
        DataAPI d = new DataAPI();
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

    public void setAQISummary(int aqi) {
        int aqi_title;
        int aqi_text;
        int aqi_icon;
        int color;
        if (aqi > Constants.AQI.HAZARDOUS) {
            color = R.color.aqi_hazardous;
            aqi_title = R.string.aqi_summary_hazardous_title;
            aqi_text = R.string.aqi_summary_hazardous_text;
            aqi_icon = R.drawable.hazardous_sign;
        } else if (aqi > Constants.AQI.VERY_UNHEALTHY) {
            color = R.color.aqi_very_unhealthy;
            aqi_title = R.string.aqi_summary_very_unhealthy_title;
            aqi_text = R.string.aqi_summary_very_unhealthy_text ;
            aqi_icon = R.drawable.very_unhealthy_sign ;
        } else if (aqi > Constants.AQI.UNHEALTHY) {
            color = R.color.aqi_unhealthy;
            aqi_title = R.string.aqi_summary_unhealthy_title;
            aqi_text = R.string.aqi_summary_unhealthy_text;
            aqi_icon = R.drawable.unhealthy_sign;
        } else if (aqi > Constants.AQI.UNHEALTHY_SENSITIVE) {
            color = R.color.aqi_unhealthy_for_sensitive;
            aqi_title = R.string.aqi_summary_unhealthy_for_sensitive_title;
            aqi_text = R.string.aqi_summary_unhealthy_for_sensitive_text;
            aqi_icon = R.drawable.unhealthy_for_sens_sign;
        } else if (aqi > Constants.AQI.MODERATE) {
            color = R.color.aqi_moderate;
            aqi_title = R.string.aqi_summary_moderate_title;
            aqi_text = R.string.aqi_summary_moderate_text;
            aqi_icon = R.drawable.moderate_sign;
        } else {
            color = R.color.aqi_good;
            aqi_title = R.string.aqi_summary_good_title;
            aqi_text = R.string.aqi_summary_good_text;
            aqi_icon = R.drawable.good_sign;
        }
        findViewById(R.id.loading_aqi_summary).setVisibility(View.GONE);
        ((TextView)mAQISummary.findViewById(R.id.aqi_summary_text)).setText(aqi_text);
        ((TextView)mAQISummary.findViewById(R.id.aqi_summary_title)).setText(aqi_title);
        ((TextView)mAQISummary.findViewById(R.id.aqi_summary_title)).setTextColor(getResources().getColor(color));
        ((ImageView)mAQISummary.findViewById(R.id.aqi_summary_icon)).setImageResource(aqi_icon);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LocationHelper.LOCATION_PERMISSION_RESULT: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocation.startLocationReading();
                    Log.d("aqi_fragment", "granted");
                }
            }
        }
    }

    @Override
    public void onLocationChanged() {

    }

    @Override
    public void onCityChange(String city) {
        Log.d("aqi_fragment", city);
    }
}
