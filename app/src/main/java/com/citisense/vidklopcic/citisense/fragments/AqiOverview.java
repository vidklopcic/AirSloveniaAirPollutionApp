package com.citisense.vidklopcic.citisense.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.citisense.vidklopcic.citisense.R;
import com.citisense.vidklopcic.citisense.data.entities.MeasuringStation;
import com.citisense.vidklopcic.citisense.util.UI;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class AqiOverview extends Fragment implements PullUpBase {
    public interface OnFragmentLoadedListener {
        void onLoaded();
    }

    OnFragmentLoadedListener mOnLoadedListener;
    Context mContext;
    UI.AQISummary mAQISummary;
    OverviewBarChart mGraphFragment;
    FragmentManager mFragmentManager;

    public AqiOverview() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFragmentManager = getChildFragmentManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_overview, container, false);

        android.support.v4.app.FragmentTransaction transaction = mFragmentManager.beginTransaction();
        mGraphFragment = new OverviewBarChart();
        mGraphFragment.setOnLoadedListener(new OverviewBarChart.OnFragmentLoadedListener() {
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

    public ArrayList<HashMap<String, Integer>> updateGraph(List<MeasuringStation> stations) {
        if (mGraphFragment == null) return null;
        ArrayList<HashMap<String, Integer>> averages = mGraphFragment.updateGraph(stations);
        if (averages != null && averages.get(MeasuringStation.AVERAGES_POLLUTANTS).size() > 0) {
            int max_aqi_val = Collections.max(averages.get(MeasuringStation.AVERAGES_POLLUTANTS).values());
            mAQISummary.setAqi(max_aqi_val);
        }
        return averages;
    }

    @Override
    public void update(ArrayList<MeasuringStation> stations) {
        updateGraph(stations);
    }


    // fix for a bug in support fragment library which can cause java.lang.IllegalStateException: Activity has been destroyed
    @Override
    public void onDetach() {
        super.onDetach();
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);

        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
