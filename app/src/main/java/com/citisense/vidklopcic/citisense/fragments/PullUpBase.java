package com.citisense.vidklopcic.citisense.fragments;

import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;

import java.util.ArrayList;

public interface PullUpBase {
    void update(ArrayList<CitiSenseStation> stations);
}
