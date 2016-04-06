package com.citisense.vidklopcic.citisense.data.entities;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.orm.SugarRecord;

import java.util.Iterator;

public class SavedState extends SugarRecord{
    String city;
    Integer configversion;
    Double top_lat;
    Double top_lng;
    Double bot_lat;
    Double bot_lng;

    public SavedState() {}

    public void setCity(String city) {
        this.city = city;
        save();
    }

    public String getCity() {
        return city;
    }

    public Integer getConfigVersion() {
        if (configversion == null) return 0;
        return configversion;
    }

    public void setConfigVersion(Integer config_version) {
        this.configversion = config_version;
        save();
    }

    public SavedState getSavedState() {
        Iterator<SavedState> iterator = SavedState.findAll(SavedState.class);
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return this;
    }

    public LatLngBounds getLastViewport() {
        if (top_lat == null) return null;
        return new LatLngBounds(
                new LatLng(top_lat, top_lng),
                new LatLng(bot_lat, bot_lng)
        );
    }

    public void setLastViewport(LatLngBounds bounds) {
        LatLng tmp = bounds.southwest;
        top_lat = tmp.latitude;
        top_lng = tmp.longitude;
        tmp = bounds.northeast;
        bot_lat = tmp.latitude;
        bot_lng = tmp.longitude;
        save();
    }
}
