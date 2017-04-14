package com.vidklopcic.airsense.fragments;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.formatter.XAxisValueFormatter;
import com.vidklopcic.airsense.R;
import com.vidklopcic.airsense.data.Constants;
import com.vidklopcic.airsense.data.DataAPI;
import com.vidklopcic.airsense.data.entities.MeasuringStation;
import com.vidklopcic.airsense.data.entities.StationMeasurement;
import com.vidklopcic.airsense.util.Conversion;
import com.vidklopcic.airsense.util.PollutantsChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;

public class AqiGraph extends Fragment implements PullUpBase, DataAPI.DataRangeListener {
    static final Integer TICK_INTERVAL_MINS = 15;
    static final Integer TICK_INTERVAL_MILLIS = TICK_INTERVAL_MINS * Constants.SECONDS * Constants.MILLIS;
    static final Integer DATA_SET_LEN_MINS = Constants.HOURS * 3 * Constants.MINUTES; // min
    static final Integer DATA_SET_LEN_MILLIS = DATA_SET_LEN_MINS * Constants.SECONDS * Constants.MILLIS;

    boolean mLockUpdate = false;
    LineChart mChart;
    LineData mChartData;
    ArrayList<String> mXData;
    Long mStartDate;
    List<MeasuringStation> mStations;
    Realm mRealm;
    SwipeRefreshLayout mRefreshLayout;

    public AqiGraph() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mXData = new ArrayList<>();
        for (int i=0;i<DATA_SET_LEN_MINS;i+=TICK_INTERVAL_MINS) mXData.add("");
        mChartData = new LineData(mXData);
    }

    @Override
    public void onAttach(Activity activity) {
        mRealm = DataAPI.getRealmOrCreateInstance(activity);
        super.onAttach(activity);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        mRealm.close();
        super.onSaveInstanceState(bundle);
    }


    @Override
    public void onDetach() {
        mStartDate = null;
        super.onDetach();
    }

    public void updateLimitLines() {
        if (mChart == null)
            return;
        mChart.getXAxis().removeAllLimitLines();
        LimitLine[] ll = PollutantsChart.getLimitLines(mStartDate, mXData.size(), TICK_INTERVAL_MILLIS);
        if (ll[0] == null)
            return;
        ll[0].setLineColor(Color.RED);
        ll[1].setLineColor(Color.RED);
        ll[2].setLineColor(Color.RED);
        mChart.getXAxis().addLimitLine(ll[0]);
        mChart.getXAxis().addLimitLine(ll[1]);
        mChart.getXAxis().addLimitLine(ll[2]);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aqi_graph, container, false);

        mChart = (LineChart) view.findViewById(R.id.aqi_line_comparison_chart);
        mChart.setData(mChartData);
        mChart.moveViewTo(mXData.size() - 1, 0, YAxis.AxisDependency.LEFT);
        mChart.setScaleMinima(3f, 1f);
        mChart.getAxisRight().setEnabled(false);
        updateLimitLines();
        mChart.getXAxis().setValueFormatter(new XAxisValueFormatter() {
            @Override
            public String getXValue(String original, int index, ViewPortHandler viewPortHandler) {
                return PollutantsChart.xAxisValueFormatter(index, mStartDate, TICK_INTERVAL_MILLIS);
            }
        });
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
        mRefreshLayout.setEnabled(false);
        mRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                if (mLockUpdate) {
                    mRefreshLayout.setRefreshing(true);
                }
            }
        });
        return view;
    }

    @Override
    public ArrayList<HashMap<String, Integer>> update(List<MeasuringStation> stations) {
        if (mStartDate != null &&
                new Date().getTime() - DATA_SET_LEN_MILLIS < mStartDate+DATA_SET_LEN_MILLIS) return null;
        mStartDate = new Date().getTime() - DATA_SET_LEN_MILLIS;
        mStartDate += 3600000 - (mStartDate % 3600000);
        updateLimitLines();
        if (stations != null && stations.size() == 1) {
            mStations = stations;
        }
        if (!mLockUpdate)
            DataAPI.getMeasurementsInRange(mStations, mStartDate, this);
        mLockUpdate = true;
        if (mRefreshLayout != null)
            mRefreshLayout.setRefreshing(true);
        return null;
    }

    @Override
    public void onDataRetrieved(List<String> station_ids, Long limit) {
        mLockUpdate = false;
        List<StationMeasurement> measurements = mStations.get(0).getMeasurementsInRange(mRealm, limit, limit + DATA_SET_LEN_MILLIS);
        setYData(PollutantsChart.measurementsToYData(mStartDate, TICK_INTERVAL_MILLIS, measurements));
    }

    private void setYData(HashMap<String, ArrayList<Entry>> ydata) {
        mChartData.clearValues();
        for (String pollutant : Constants.AQI.supported_pollutants) {
            if (ydata.containsKey(pollutant)) {
                Collections.sort(ydata.get(pollutant), new PollutantsChart.EntryComparator());
                LineDataSet set = new LineDataSet(ydata.get(pollutant), pollutant);
                set.setColor(Conversion.getPollutant(pollutant).getColor());
                set.setCircleColor(Conversion.getPollutant(pollutant).getColor());
                set.setLineWidth(2);
                mChartData.addDataSet(set);
            }
        }
        mChart.notifyDataSetChanged();
        mChart.invalidate();
        if (mRefreshLayout != null)
            mRefreshLayout.setRefreshing(false);
    }
}
