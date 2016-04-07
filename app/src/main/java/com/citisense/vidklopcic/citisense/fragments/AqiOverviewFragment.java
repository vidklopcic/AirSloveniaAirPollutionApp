package com.citisense.vidklopcic.citisense.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.citisense.vidklopcic.citisense.R;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.util.UI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


public class AqiOverviewFragment extends Fragment implements MeasuringStationDataFragment {
    public interface OnFragmentLoadedListener {
        void onLoaded();
    }

    private OnFragmentLoadedListener mOnLoadedListener;
    Context mContext;
    private UI.AQISummary mAQISummary;
    AqiOverviewGraph mGraphFragment;
    public AqiOverviewFragment() {
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
        mGraphFragment.setOnLoadedListener(new AqiOverviewGraph.OnFragmentLoadedListener() {
            @Override
            public void onLoaded() {
                if (mOnLoadedListener != null)
                    mOnLoadedListener.onLoaded();
            }
        });
        transaction.add(R.id.graph_fragment_container, mGraphFragment).commit();

        mContext = view.getContext();
        mAQISummary = new UI.AQISummary(mContext, view, inflater, R.id.aqi_summary_layout);

        return view;
    }

    public void setOnLoadedListener(OnFragmentLoadedListener listener) {
        mOnLoadedListener = listener;
    }

    public ArrayList<HashMap<String, Integer>> updateGraph(ArrayList<CitiSenseStation> stations) {
        if (mGraphFragment == null) return null;
        ArrayList<HashMap<String, Integer>> averages = mGraphFragment.updateGraph(stations);
        if (averages != null) {
            int max_aqi_val = Collections.max(averages.get(CitiSenseStation.AVERAGES_POLLUTANTS).values());
            mAQISummary.setAqi(max_aqi_val);
        }
        return averages;
    }

    @Override
    public void update(ArrayList<CitiSenseStation> stations) {
        updateGraph(stations);
    }
}
