package com.vidklopcic.airsense.fragments;

import com.vidklopcic.airsense.data.entities.MeasuringStation;

import java.util.ArrayList;

public interface PullUpBase {
    void update(ArrayList<MeasuringStation> stations);
}
