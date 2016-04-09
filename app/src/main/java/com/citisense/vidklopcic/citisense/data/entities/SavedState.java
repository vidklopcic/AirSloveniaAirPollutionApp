package com.citisense.vidklopcic.citisense.data.entities;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;

public class SavedState extends RealmObject{
    String city;
    Integer configversion;
    Double top_lat;
    Double top_lng;
    Double bot_lat;
    Double bot_lng;

    public SavedState() {}

    public void setCity(Realm r, String city) {
        r.beginTransaction();
        this.city = city;
        r.commitTransaction();
    }

    public String getCity() {
        return city;
    }

    public Integer getConfigVersion() {
        if (configversion == null) return 0;
        return configversion;
    }

    public void setConfigVersion(Realm r, Integer config_version) {
        r.beginTransaction();
        this.configversion = config_version;
        r.commitTransaction();
    }

    public static SavedState getSavedState(Realm r) {
        RealmResults<SavedState> iterator = r.allObjects(SavedState.class);;
        if (iterator.size() == 0) {
            r.beginTransaction();
            r.createObject(SavedState.class);
            r.commitTransaction();
        }
        iterator = r.allObjects(SavedState.class);;
        return iterator.get(0);
    }

    public LatLngBounds getLastViewport() {
        if (top_lat == null) return null;
        return new LatLngBounds(
                new LatLng(top_lat, top_lng),
                new LatLng(bot_lat, bot_lng)
        );
    }

    public void setLastViewport(Realm r, LatLngBounds bounds) {
        r.beginTransaction();
        LatLng tmp = bounds.southwest;
        top_lat = tmp.latitude;
        top_lng = tmp.longitude;
        tmp = bounds.northeast;
        bot_lat = tmp.latitude;
        bot_lng = tmp.longitude;
        r.commitTransaction();
    }
}
