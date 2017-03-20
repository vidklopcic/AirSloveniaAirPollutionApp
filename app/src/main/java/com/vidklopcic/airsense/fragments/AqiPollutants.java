package com.vidklopcic.airsense.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.mikephil.charting.components.YAxis;
import com.vidklopcic.airsense.R;
import com.vidklopcic.airsense.anim.HeightResizeAnimation;
import com.vidklopcic.airsense.data.Constants;
import com.vidklopcic.airsense.data.DataAPI;
import com.vidklopcic.airsense.data.entities.MeasuringStation;
import com.vidklopcic.airsense.data.entities.StationMeasurement;
import com.vidklopcic.airsense.util.AQI;
import com.vidklopcic.airsense.util.PollutantsChart;
import com.vidklopcic.airsense.util.UI;
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
    private static final int DATA_LEN_MINS = 3 * 24 * Constants.MINUTES;
    private static final int DATA_LEN_MILLIS = DATA_LEN_MINS * Constants.SECONDS * Constants.MILLIS;
    private static final int TICK_INTERVAL_MINS = 15;
    private static final int TICK_INTERVAL_MILLIS = TICK_INTERVAL_MINS * Constants.SECONDS * Constants.MILLIS;
    private Realm mRealm;
    private LayoutInflater mInflater;
    private LinearLayout mContainer;
    private ScrollView mScrollContainer;
    private Context mContext;
    private Long mStartDate;
    private HashMap<String, LinearLayout> mPollutantCards;
    private HashMap<String, ArrayList<Entry>> mYData;
    private ArrayList<String> mXData;
    private SwipeRefreshLayout mRefreshLayout;
    private ArrayList<MeasuringStation> mStations;
    private boolean mShouldUpdate = false;
    private Integer mOriginalCardHeight;
    boolean mScrollIsLocked = false;
    View mExpandedCard;

    public AqiPollutants() {
        mXData = new ArrayList<>();
        for (int i=0;i<DATA_LEN_MINS;i+=TICK_INTERVAL_MINS) mXData.add("");
        mPollutantCards = new HashMap<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        mRealm = DataAPI.getRealmOrCreateInstance(getActivity());
        super.onAttach(activity);
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
        mScrollContainer = (ScrollView) view.findViewById(R.id.pollutant_cards_scroll_container);
        mScrollContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mScrollIsLocked;
            }
        });
        mRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                if (mShouldUpdate && mStations != null)
                    update(mStations);
            }
        });

        mContainer.post(new Runnable() {
            @Override
            public void run() {
                if (mShouldUpdate && mStations != null)
                    update(mStations);
            }
        });
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                update(mStations);
            }
        });
        if (mExpandedCard != null) {
            collapseCard(mExpandedCard);
        }
        return view;
    }

    public boolean backPressed() {
        if (mExpandedCard == null) {
            return false;
        }
        collapseCard(mExpandedCard);
        return true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void update(final ArrayList<MeasuringStation> stations) {
        mStations = stations;
        mShouldUpdate = true;
        if (stations == null || stations.size() != 1 || mRefreshLayout == null || mRealm == null || mContainer == null)
            return;
        if (!ViewCompat.isAttachedToWindow(mContainer))
            return;
        mShouldUpdate = false;

        mRefreshLayout.setRefreshing(true);
        mStartDate = new Date().getTime()-DATA_LEN_MILLIS;
        mStartDate += 3600000 - (mStartDate % 3600000);
        DataAPI.getMeasurementsInRange(stations, mStartDate, new DataAPI.DataRangeListener() {
            @Override
            public void onDataRetrieved(List<String> station_ids, Long limit) {
                List<StationMeasurement> measurements = MeasuringStation.idListToStations(mRealm, station_ids)
                        .get(0)
                        .getMeasurementsInRange(mRealm, mStartDate, mStartDate + DATA_LEN_MILLIS);
                mStartDate = stations.get(0).getLastMeasurementTime();
                if (mStartDate == null) {
                    mStartDate = new Date().getTime();
                }
                mStartDate -= DATA_LEN_MILLIS - TICK_INTERVAL_MILLIS*4;
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


    public void expandCard(View view) {
        Integer target_height = (int) (mScrollContainer.getHeight()/1.3);
        int[] view_location = new int[2];
        view.getLocationOnScreen(view_location);
        int[] scroll_location = new int[2];
        mScrollContainer.getLocationOnScreen(scroll_location);
        int target = mScrollContainer.getScrollY()+view_location[1]-scroll_location[1]-mScrollContainer.getHeight()/2+target_height/2;
        HeightResizeAnimation resizeAnimation = new HeightResizeAnimation(
                view,
                target_height,
                mScrollContainer,
                target
        );

        resizeAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        resizeAnimation.setDuration(200);
        view.startAnimation(resizeAnimation);
        LineChart chart = (LineChart) view.findViewById(R.id.pollutant_graph_card_chart);
        chart.setTouchEnabled(true);
        chart.setScaleMinima(1f, 1f);
        mScrollIsLocked = true;
        mExpandedCard = view;
    }

    public void collapseCard(View view) {
        LineChart chart = (LineChart) view.findViewById(R.id.pollutant_graph_card_chart);
        chart.setTouchEnabled(false);
        chart.setScaleMinima(6f, 1f);
        chart.setScaleX(6f);
        chart.moveViewToX(mXData.size() - 1);
        HeightResizeAnimation resizeAnimation = new HeightResizeAnimation(
                view,
                mOriginalCardHeight,
                null,
                null
        );
        resizeAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        resizeAnimation.setDuration(200);
        view.startAnimation(resizeAnimation);
        mExpandedCard = null;
        mScrollIsLocked = false;
    }

    public void addPollutant(String name) {
        final LinearLayout pollutant_card = (LinearLayout) mInflater.inflate(
                R.layout.fragment_pollutant_cards_pollutant_layout, mContainer, false);
        pollutant_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getHeight() == mOriginalCardHeight) {
                    if (mExpandedCard != null) {
                        collapseCard(mExpandedCard);
                    } else {
                        expandCard(view);
                    }
                } else {
                    collapseCard(view);
                }
            }
        });
        pollutant_card.post(new Runnable() {
            @Override
            public void run() {
                if (mOriginalCardHeight == null) {
                    mOriginalCardHeight = pollutant_card.getHeight();
                }
            }
        });
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

    public LineChart setPollutantGraph(String name) {
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
        chart.moveViewTo(mXData.size() - 1, 0, YAxis.AxisDependency.LEFT);
        chart.setScaleMinima(6f, 1f);
        chart.setData(data);
        chart.setHighlightPerTapEnabled(false);
        chart.setHighlightPerDragEnabled(false);

        chart.getXAxis().setValueFormatter(new XAxisValueFormatter() {
            @Override
            public String getXValue(String original, int index, ViewPortHandler viewPortHandler) {
                return PollutantsChart.xAxisValueFormatter(index, mStartDate, TICK_INTERVAL_MILLIS);
            }
        });
        chart.invalidate();
        return chart;
    }
}
