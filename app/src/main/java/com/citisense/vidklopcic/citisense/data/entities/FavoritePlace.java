package com.citisense.vidklopcic.citisense.data.entities;


import com.google.android.gms.maps.model.LatLng;
import com.orm.SugarRecord;

public class FavoritePlace extends SugarRecord {
    Double latitude;
    Double longitude;
    String address;
    String nickname;

    public FavoritePlace() {}

    public FavoritePlace(LatLng location, String street_name, String nickname) {
        this.latitude = location.latitude;
        this.longitude = location.longitude;
        this.address = street_name;
        this.nickname = nickname;
        save();
    }

    public void setNickname(String nickaname) {
        this.nickname = nickaname;
        save();
    }

    public String getNickname() {
        return nickname;
    }

    public String getAddress() {
        return address;
    }
}
