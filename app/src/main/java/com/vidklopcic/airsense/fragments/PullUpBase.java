package com.vidklopcic.airsense.fragments;

import com.vidklopcic.airsense.data.entities.MeasuringStation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface PullUpBase {
    ArrayList<HashMap<String, Integer>> update(List<MeasuringStation> stations);
}
