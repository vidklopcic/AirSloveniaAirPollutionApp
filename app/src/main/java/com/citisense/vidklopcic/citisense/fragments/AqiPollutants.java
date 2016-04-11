package com.citisense.vidklopcic.citisense.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.citisense.vidklopcic.citisense.R;
import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.data.DataAPI;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.data.entities.StationMeasurement;
import com.citisense.vidklopcic.citisense.util.AQI;
import com.citisense.vidklopcic.citisense.util.PollutantsChart;
import com.citisense.vidklopcic.citisense.util.UI;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.XAxisValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import io.realm.Realm;


public class AqiPollutants extends Fragment implements PullUpBase {
    private static final int DATA_LEN_MINS = 12 * Constants.MINUTES;
    private static final int DATA_LEN_MILLIS = DATA_LEN_MINS * Constants.SECONDS * Constants.MILLIS;
    private static final int TICK_INTERVAL_MINS = 15;
    private static final int TICK_INTERVAL_MILLIS = TICK_INTERVAL_MINS * Constants.SECONDS * Constants.MILLIS;
    private Realm mRealm;
    private LayoutInflater mInflater;
    private LinearLayout mContainer;
    private Context mContext;
    private long mStartDate;
    private HashMap<String, LinearLayout> mPollutantCards;
    private HashMap<String, ArrayList<Entry>> mYData;
    private ArrayList<String> mXData;
    private SwipeRefreshLayout mRefreshLayout;
    private ArrayList<CitiSenseStation> mStations;

    public AqiPollutants() {
        mXData = new ArrayList<>();
        for (int i=0;i<DATA_LEN_MINS;i+=TICK_INTERVAL_MINS) mXData.add("");
        mRealm = Realm.getDefaultInstance();
        mPollutantCards = new HashMap<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRealm = Realm.getDefaultInstance();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        mRealm.close();
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pollutant_cards, container, false);
        mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.pollutants_refresh_layout);
        mContext = view.getContext();
        mInflater = inflater;
        mContainer = (LinearLayout) view.findViewById(R.id.pollutant_cards_container);
        mRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                if (mStations != null)
                    update(mStations);
            }
        });
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                update(mStations);
            }
        });
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void update(ArrayList<CitiSenseStation> stations) {
        mStations = stations;
        if (stations == null || stations.size() != 1 || mRefreshLayout == null) return;
        mRefreshLayout.setRefreshing(true);
        mStartDate = new Date().getTime() - DATA_LEN_MILLIS;
        DataAPI.getMeasurementsInRange(stations, mStartDate, new DataAPI.DataRangeListener() {
            @Override
            public void onDataRetrieved(List<String> station_ids, Long limit) {
                List<StationMeasurement> measurements = CitiSenseStation.idListToStations(mRealm, station_ids)
                        .get(0)
                        .getMeasurementsInRange(mRealm, mStartDate, mStartDate + DATA_LEN_MILLIS);
                mYData = PollutantsChart.measurementsToYData(mStartDate, TICK_INTERVAL_MILLIS, measurements);
                for (String pollutant : Constants.AQI.supported_pollutants) {
                    if (mYData.containsKey(pollutant)) {
                        ArrayList<Entry> m = mYData.get(pollutant);
                        Collections.sort(m, new PollutantsChart.EntryComparator());
                        if (m.size() != 0)
                            updatePollutant(pollutant, (int) m.get(m.size() - 1).getVal());
                    }
                }
                mContainer.forceLayout();
                mRefreshLayout.setRefreshing(false);
            }
        });
    }

    public void addPollutant(String name) {
        LinearLayout pollutant_card = (LinearLayout) mInflater.inflate(
                R.layout.fragment_pollutant_cards_pollutant_layout, mContainer, false);
        mContainer.addView(pollutant_card);
        mPollutantCards.put(name, pollutant_card);
    }

    public void updatePollutant(String name, Integer aqi) {
        if (!mPollutantCards.containsKey(name))
            addPollutant(name);
        LinearLayout pollutant_card = mPollutantCards.get(name);
        if (!ViewCompat.isAttachedToWindow(pollutant_card)) {
            ((LinearLayout)pollutant_card.getParent()).removeView(pollutant_card);
            mContainer.addView(pollutant_card);
        }
        setPollutantTopBar(name, aqi);
        setPollutantGraph(name);
        UI.setViewBackground(mContext, mPollutantCards.get(name), AQI.getLinearColor(aqi, mContext));
    }

    private void setPollutantTopBar(String pollutant, Integer aqi) {
        String long_aqi = mContext.getString(AQI.toText(aqi));
        LinearLayout pollutant_card = mPollutantCards.get(pollutant);
        ((TextView) pollutant_card.findViewById(R.id.pollutant_name)).setText(pollutant);
        ((TextView) pollutant_card.findViewById(R.id.pollutant_aqi_text)).setText(long_aqi);
        ((TextView) pollutant_card.findViewById(R.id.pollutant_aqi_value)).setText(aqi.toString());
    }

    public void setPollutantGraph(String name) {
        LineChart chart = (LineChart) mPollutantCards.get(name).findViewById(R.id.pollutant_graph_card_chart);
        LineDataSet ydata = new LineDataSet(mYData.get(name), "");

        int white = ContextCompat.getColor(mContext, R.color.white);
        ydata.setCircleColor(white);
        ydata.setCircleRadius(3);
        ydata.setDrawCubic(true);
        ydata.setLineWidth(2);
        ydata.setColor(white);
        ydata.setDrawValues(false);

        LineData data = new LineData(mXData);
        data.addDataSet(ydata);

        chart.setDescription(mContext.getString(R.string.measurements_from_past_12_hours));
        chart.setDescriptionColor(white);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.setDrawGridBackground(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setEnabled(true);
        chart.getAxisLeft().setAxisLineWidth(1);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setAxisLineColor(white);
        chart.getAxisLeft().setTextColor(white);
        chart.getXAxis().setAxisLineColor(white);
        chart.getXAxis().setTextColor(white);
        chart.getXAxis().setDrawGridLines(false);
        chart.setTouchEnabled(false);
        chart.setData(data);

        chart.getXAxis().setValueFormatter(new XAxisValueFormatter() {
            @Override
            public String getXValue(String original, int index, ViewPortHandler viewPortHandler) {
                return PollutantsChart.xAxisValueFormatter(index, mStartDate, TICK_INTERVAL_MILLIS);
            }
        });
    }
}
