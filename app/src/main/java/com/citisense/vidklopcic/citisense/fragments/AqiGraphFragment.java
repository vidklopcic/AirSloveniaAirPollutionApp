package com.citisense.vidklopcic.citisense.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.citisense.vidklopcic.citisense.R;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;

import java.util.ArrayList;

public class AqiGraphFragment extends Fragment implements MeasuringStationDataFragment {
    String FRAGMENT_RESTORED = "prev_fragment_state";
    public AqiGraphFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aqi_graph, container, false);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putBoolean(FRAGMENT_RESTORED, true);
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void update(ArrayList<CitiSenseStation> stations) {

    }
}
