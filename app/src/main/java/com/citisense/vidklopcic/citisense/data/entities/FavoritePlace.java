package com.citisense.vidklopcic.citisense.data.entities;


import com.google.android.gms.maps.model.LatLng;

import io.realm.Realm;
import io.realm.RealmObject;

public class FavoritePlace extends RealmObject {
    Double latitude;
    Double longitude;
    String address;
    String nickname;

    public FavoritePlace() {}

    public static FavoritePlace create(Realm r, LatLng location, String street_name, String nickname) {
        r.beginTransaction();
        FavoritePlace place = r.createObject(FavoritePlace.class);
        place.latitude = location.latitude;
        place.longitude = location.longitude;
        place.address = street_name;
        place.nickname = nickname;
        r.commitTransaction();
        return place;
    }

    public void setNickname(Realm r, String nickaname) {
        r.beginTransaction();
        this.nickname = nickaname;
        r.commitTransaction();
    }

    public String getNickname() {
        return nickname;
    }

    public String getAddress() {
        return address;
    }
}
