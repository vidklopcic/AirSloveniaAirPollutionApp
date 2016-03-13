package com.citisense.vidklopcic.citisense;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.citisense.vidklopcic.citisense.fragments.AqiOverviewGraph;

public class MainActivity extends FragmentActivity {
    AqiOverviewGraph mChartFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mChartFragment = (AqiOverviewGraph) getFragmentManager().findFragmentById(R.id.dashboard_bar_chart_fragment);
    }

    public void fragmentClicked(View view) {
        mChartFragment.addBar(110, "CO");
        mChartFragment.addBar(205, "CO2");
        mChartFragment.addBar(125, "PM2.5");
        mChartFragment.addBar(94, "PM10");
        mChartFragment.addBar(57, "O3");
        mChartFragment.addBar(420, "NO");
    }

    public void titleClicked(View view) {
        mChartFragment.setBarAqi("NO", 20);
    }
}
