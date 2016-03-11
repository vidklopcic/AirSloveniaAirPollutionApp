package com.citisense.vidklopcic.citisense.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.citisense.vidklopcic.citisense.R;
import com.citisense.vidklopcic.citisense.data.Constants;

import java.util.ArrayList;
import java.util.HashMap;

public class AqiOverviewGraph extends Fragment {
    // the fragment initialization parameters
    private static final String ARG_SQL_VALUES_ID = "sql_vals_id";

    private int mAQIVals;
    private LinearLayout mAQIBarsContainer;
    private LinearLayout mAQILabelsContainer;
    private HashMap<String, ArrayList<LinearLayout>> mAQIBars;
    public Context mContext;

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
            mAQIVals = getArguments().getInt(ARG_SQL_VALUES_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_aqi_overview, container, false);
        mAQIBarsContainer = (LinearLayout) view.findViewById(R.id.aqi_chart_bars_layout);
        mAQILabelsContainer = (LinearLayout) view.findViewById(R.id.aqi_overview_x_labels_container);
        mContext = view.getContext();
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private LinearLayout createXLabel(String name, Integer aqi) {
        LinearLayout label = new LinearLayout(mContext, null, R.style.AqiChartLabel_layout);
        TextView label_text = new TextView(mContext, null, R.style.AqiChartLabel);
        label_text.setText(name);
        TextView label_subtitle = new TextView(mContext, null, R.style.AqiChartLabel_subtitle);
        label_subtitle.setText(aqi.toString());
        label.addView(label_text);
        label.addView(label_subtitle);
        return label;
    }

    private LinearLayout createBar(Integer val) {
        LinearLayout bar = new LinearLayout(mContext, null, R.style.AqiChartBar);
        View bar_content = new View(mContext, null, R.style.AqiChartBar_content);
        return bar;
    }
}
