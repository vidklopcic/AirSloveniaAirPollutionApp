package com.citisense.vidklopcic.citisense.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.citisense.vidklopcic.citisense.R;
import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.util.AQI;
import com.citisense.vidklopcic.citisense.util.anim.AqiBarAnimation;

import java.util.ArrayList;
import java.util.HashMap;

public class AqiOverviewGraph extends Fragment {
    // the fragment initialization parameters
    private static final String ARG_SQL_VALUES_ID = "sql_vals_id";
    public static final String LOG_ID = "aqi_fragment";

    private int mAQIValsDbId;
    LinearLayout mAQILabelsContainer;
    private RelativeLayout mAqiChartContainer;
    private LinearLayout mAQIBarsContainer;
    private HashMap<String, ArrayList<LinearLayout>> mAQIBars;
    private Context mContext;
    private LayoutInflater mInflater;
    private int mChartRange = 0;
    private Integer mAQIBarsContainerHeight;


    /**
     * Factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param AQIVals id of sql entry for aqi vals to display.
     * @return A new instance of fragment AqiOverviewGraph.
     */
    public static AqiOverviewGraph newInstance(int AQIVals) {
        AqiOverviewGraph fragment = new AqiOverviewGraph();
        Bundle args = new Bundle();
        args.putInt(ARG_SQL_VALUES_ID, AQIVals);
        fragment.setArguments(args);
        return fragment;
    }

    public AqiOverviewGraph() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mAQIValsDbId = getArguments().getInt(ARG_SQL_VALUES_ID);
        }
        mAQIBars = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.aqi_overview_fragment, container, false);
        mAqiChartContainer = (RelativeLayout) view.findViewById(R.id.aqi_overview_chart_container);
        mAQIBarsContainer = (LinearLayout) view.findViewById(R.id.aqi_chart_bars_layout);
        mAQILabelsContainer = (LinearLayout) view.findViewById(R.id.aqi_overview_x_labels_container);
        mContext = view.getContext();
        mInflater = inflater;
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mAQIBarsContainer.post(new Runnable() {
                    public void run() {
                        if (mAQIBarsContainerHeight == null) {
                            mAQIBarsContainerHeight = mAQIBarsContainer.getHeight();
                            addBar(86, "CO");
                            addBar(122, "PM2.5");
                            addBar(124, "PM10");
                            addBar(25, "O3");
                            addBar(41, "NO2");
                            addBar(34, "NO");
                        }
                    }
                });
            }
        });
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void addBar(Integer aqi, String label_text) {
        if (aqi > mChartRange) {
            mChartRange = aqi;
            setChartRange(mChartRange);
        }
        ArrayList<LinearLayout> bar_label = new ArrayList<>();
        LinearLayout bar = createBar(0);
        LinearLayout label = createXLabel(label_text, 0);
        mAQIBarsContainer.addView(bar);
        mAQILabelsContainer.addView(label);
        bar.startAnimation(
                new AqiBarAnimation(
                        this, label, bar.findViewById(R.id.aqi_bar_content), getAqiFromLabel(label), aqi));
        bar_label.add(bar);
        bar_label.add(label);
        mAQIBars.put(label_text, bar_label);
    }

    public void removeBar(String key) {
        mAQIBarsContainer.removeView(mAQIBars.get(key).get(0));
        mAQILabelsContainer.removeView(mAQIBars.get(key).get(1));
        mAQIBars.remove(key);
    }

    public void setBarAqi(String key, Integer aqi) {
        LinearLayout bar = mAQIBars.get(key).get(0);
        LinearLayout label = mAQIBars.get(key).get(1);
        View bar_content = bar.findViewById(R.id.aqi_bar_content);
        bar.startAnimation(new AqiBarAnimation(this, label, bar_content, getAqiFromLabel(label), aqi));
    }

    private LinearLayout createXLabel(String name, Integer aqi) {
        LinearLayout label = (LinearLayout) mInflater.inflate(R.layout.aqi_overview_fragment_label, mAQILabelsContainer, false);
        TextView label_text = (TextView) label.findViewById(R.id.aqi_text);
        label_text.setText(name);
        TextView label_subtitle = (TextView) label.findViewById(R.id.aqi_subtitle);
        label_subtitle.setText(aqi.toString());
        return label;
    }

    private LinearLayout createBar(int aqi) {
        LinearLayout bar = (LinearLayout) mInflater.inflate(R.layout.aqi_overview_fragment_bar, mAQIBarsContainer, false);
        View bar_content = bar.findViewById(R.id.aqi_bar_content);
        setBarAqi(bar_content, aqi);
        return bar;
    }

    public void setBarAqi(View bar, Integer aqi) {
        Float percentage = aqi / Constants.AQI.SUM;
        bar.setBackgroundColor(ContextCompat.getColor(mContext, AQI.getColor(aqi)));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bar.getLayoutParams();
        params.weight = percentage;
        bar.setLayoutParams(params);
        if (aqi > mChartRange) {
            setChartRange(mChartRange);
        }
    }

    public void setLabelAqi(LinearLayout label, Integer aqi) {
        int old_aqi = getAqiFromLabel(label);
        if (getMaxAqi() ==  old_aqi && old_aqi > aqi) {
            setChartRange(aqi);
        }
        TextView subtitle = (TextView)label.findViewById(R.id.aqi_subtitle);
        subtitle.setText(aqi.toString());
    }

    private void setChartRange(int max_aqi) {
        mChartRange = max_aqi;
        Float height_increase = Constants.AQI.SUM / (max_aqi+Constants.AQI.BAR_OFFSET);    // max_axi+x.. x = margin
        Integer top_margin = (int) -(height_increase * mAQIBarsContainerHeight - mAQIBarsContainerHeight);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mAqiChartContainer.getLayoutParams();
        params.setMargins(0, top_margin, 0, 0);
        mAqiChartContainer.setLayoutParams(params);
    }

    private int getAqiFromLabel(LinearLayout label) {
        return Integer.valueOf((String) ((TextView) label.findViewById(R.id.aqi_subtitle)).getText());
    }

    private int getMaxAqi() {
        int max_aqi = 0;
        for(ArrayList<LinearLayout> l: mAQIBars.values()) {
            int aqi = getAqiFromLabel(l.get(1));
            if (aqi > max_aqi) max_aqi = aqi;
        }
        return max_aqi;
    }
}
