package com.vidklopcic.airsense.data.Gson;

import com.vidklopcic.airsense.util.Conversion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by vidklopcic on 19/03/2017.
 */

public class Measurement {
    String time;
    List<PollutionMeasurement> pollutants;
    List<OtherMeasurement> others;

    public void setTime(Date t) {
        time = Conversion.Time.dateToString(t);
    }

    public Date getTime() {
        return Conversion.Time.stringToDate(time);
    }

    public List<PollutionMeasurement> getPollutants() {
        return pollutants;
    }

    public List<OtherMeasurement> getOthers() {
        return others;
    }
}
