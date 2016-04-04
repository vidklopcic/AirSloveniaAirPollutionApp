package com.citisense.vidklopcic.citisense.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.citisense.vidklopcic.citisense.R;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.util.UI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class OverviewFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    public interface SwipeRefreshListener {
        void onRefresh();
    }

    Context mContext;
    private UI.AQISummary mAQISummary;
    AqiOverviewGraph mGraphFragment;
    SwipeRefreshLayout mSwipeRefresh;
    SwipeRefreshListener mListener;
    public OverviewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_overview, container, false);

        android.support.v4.app.FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        mGraphFragment = new AqiOverviewGraph();
        transaction.add(R.id.graph_fragment_container, mGraphFragment).commit();

        mContext = view.getContext();
        mAQISummary = new UI.AQISummary(mContext, view, inflater, R.id.aqi_summary_layout);

        mSwipeRefresh = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
        mSwipeRefresh.setOnRefreshListener(this);
        mSwipeRefresh.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefresh.setRefreshing(true);
            }
        });
        return view;
    }

    @Override
    public void onRefresh() {
        if (mListener != null) {
            mListener.onRefresh();
        } else {
            mSwipeRefresh.setRefreshing(false);
        }
    }

    public void setSwipeRefreshListener(SwipeRefreshListener listener) {
        mListener = listener;
    }

    public void setRefreshing(boolean refreshing) {
        mSwipeRefresh.setRefreshing(refreshing);
    }

    public ArrayList<HashMap<String, Integer>> updateGraph(ArrayList<CitiSenseStation> stations) {
        mSwipeRefresh.setRefreshing(false);
        ArrayList<HashMap<String, Integer>> averages = mGraphFragment.updateGraph(stations);
        if (averages != null) {
            mSwipeRefresh.setRefreshing(false);
            int max_aqi_val = Collections.max(averages.get(CitiSenseStation.AVERAGES_POLLUTANTS).values());
            mAQISummary.setAqi(max_aqi_val);
        }
        return averages;
    }
}
