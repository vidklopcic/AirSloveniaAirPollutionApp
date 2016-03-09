package com.citisense.vidklopcic.citisense.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.citisense.vidklopcic.citisense.R;

public class AqiOverview extends Fragment {
    // the fragment initialization parameters
    private static final String ARG_SQL_VALUES_ID = "sql_vals_id";

    private int mAQIVals;


    /**
     * Factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param AQIVals id of sql entry for aqi vals to display.
     * @return A new instance of fragment AqiOverview.
     */
    public static AqiOverview newInstance(int AQIVals) {
        AqiOverview fragment = new AqiOverview();
        Bundle args = new Bundle();
        args.putInt(ARG_SQL_VALUES_ID, AQIVals);
        fragment.setArguments(args);
        return fragment;
    }

    public AqiOverview() {
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
        return inflater.inflate(R.layout.fragment_aqi_overview, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
