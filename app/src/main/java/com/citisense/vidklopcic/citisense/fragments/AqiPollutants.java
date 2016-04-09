package com.citisense.vidklopcic.citisense.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.citisense.vidklopcic.citisense.R;
import com.citisense.vidklopcic.citisense.data.Constants;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;

import java.util.ArrayList;


public class AqiPollutants extends Fragment implements PullUpBase {
    private static final int DATA_LEN_MILLIS = 6 * Constants.MINUTES * Constants.SECONDS * Constants.MILLIS;
    public AqiPollutants() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_aqi_cards, container, false);
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void update(ArrayList<CitiSenseStation> stations) {

    }
}
