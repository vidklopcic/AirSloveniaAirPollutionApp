package com.citisense.vidklopcic.citisense.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.citisense.vidklopcic.citisense.R;
import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.data.DataAPI;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.data.entities.StationMeasurement;
import com.citisense.vidklopcic.citisense.util.Conversion;
import com.citisense.vidklopcic.citisense.util.PollutantsChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.XAxisValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import io.realm.Realm;

public class AqiGraph extends Fragment implements PullUpBase, DataAPI.DataRangeListener {
    static final Integer TICK_INTERVAL_MINS = 15;
    static final Integer TICK_INTERVAL_MILLIS = TICK_INTERVAL_MINS * Constants.SECONDS * Constants.MILLIS;
    static final Integer DATA_SET_LEN_MINS = Constants.HOURS * 3 * Constants.MINUTES; // min
    static final Integer DATA_SET_LEN_MILLIS = DATA_SET_LEN_MINS * Constants.SECONDS * Constants.MILLIS;

    boolean mLockUpdate = false;
    LineChart mChart;
    LineData mChartData;
    ArrayList<String> mXdata;
    Long mStartDate;
    ArrayList<CitiSenseStation> mStations;
    Realm mRealm;

    public AqiGraph() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mXdata = new ArrayList<>();
        for (int i=0;i<DATA_SET_LEN_MINS;i+=TICK_INTERVAL_MINS) mXdata.add("");
        mChartData = new LineData(mXdata);
        mRealm = Realm.getDefaultInstance();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        mRealm.close();
        super.onSaveInstanceState(bundle);
    }

    public void onDetach() {
        mStartDate = null;
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aqi_graph, container, false);

        mChart = (LineChart) view.findViewById(R.id.aqi_line_comparison_chart);
        mChart.setData(mChartData);
        mChart.moveViewTo(mXdata.size() - 1, 0, YAxis.AxisDependency.LEFT);
        mChart.setScaleMinima(3f, 1f);
        mChart.getAxisRight().setEnabled(false);
        mChart.getXAxis().setValueFormatter(new XAxisValueFormatter() {
            @Override
            public String getXValue(String original, int index, ViewPortHandler viewPortHandler) {
                Calendar cal = Calendar.getInstance();
                if (mStartDate == null) return "";
                cal.setTimeInMillis(mStartDate + TICK_INTERVAL_MILLIS * index);
                cal.setTimeZone(TimeZone.getDefault());
                return String.valueOf(Conversion.zfill(cal.get(Calendar.HOUR_OF_DAY), 2)) + ":"
                        + String.valueOf(Conversion.zfill(cal.get(Calendar.MINUTE), 2));
            }
        });
        return view;
    }

    @Override
    public void update(ArrayList<CitiSenseStation> stations) {
        if (mStartDate != null &&
                new Date().getTime() - DATA_SET_LEN_MILLIS < mStartDate+DATA_SET_LEN_MILLIS) return;
        mStartDate = new Date().getTime() - DATA_SET_LEN_MILLIS;
        if (stations != null && stations.size() == 1) {
            mStations = stations;
        }
        if (!mLockUpdate)
            DataAPI.getMeasurementsInRange(mStations, mStartDate, this);
        mLockUpdate = true;
    }

    @Override
    public void onDataRetrieved(Long limit) {
        mLockUpdate = false;
        mStations.get(0).getMeasurementsInRange(mRealm, limit, limit + DATA_SET_LEN_MILLIS, new CitiSenseStation.MeasurementsTransactionListener() {
            @Override
            public void onTransactionFinished(List<StationMeasurement> measurements) {
                setYData(PollutantsChart.measurementsToYData(mStartDate, TICK_INTERVAL_MILLIS, measurements));
            }
        });
    }

    private void setYData(HashMap<String, ArrayList<Entry>> ydata) {
        mChartData.clearValues();
        for (String pollutant : Constants.AQI.supported_pollutants) {
            if (ydata.keySet().contains(pollutant)) {
                Collections.sort(ydata.get(pollutant), new EntryComparator());
                LineDataSet set = new LineDataSet(ydata.get(pollutant), pollutant);
                set.setColor(Conversion.getPollutant(pollutant).getColor());
                set.setCircleColor(Conversion.getPollutant(pollutant).getColor());
                set.setDrawCubic(true);
                set.setLineWidth(2);
                mChartData.addDataSet(set);
            }
        }
        mChart.notifyDataSetChanged();
        mChart.invalidate();
    }

    private class EntryComparator implements Comparator<Entry> {

        @Override
        public int compare(Entry lhs, Entry rhs) {
            return Integer.valueOf(lhs.getXIndex()).compareTo(rhs.getXIndex());
        }
    }
}
