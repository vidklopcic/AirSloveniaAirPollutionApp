package com.citisense.vidklopcic.citisense.data.entities;

import com.orm.SugarRecord;

import java.util.Iterator;

public class SavedState extends SugarRecord{
    String city;
    Integer configversion;

    public SavedState() {}

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public Integer getConfigVersion() {
        return configversion;
    }

    public void setConfigVersion(int config_version) {
        this.configversion = config_version;
    }

    public SavedState getSavedState() {
        Iterator<SavedState> iterator = SavedState.findAll(SavedState.class);
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return this;
    }
}
