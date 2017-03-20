package com.vidklopcic.airsense.data.Gson;

import com.vidklopcic.airsense.util.Conversion;

import java.util.Date;

/**
 * Created by vidklopcic on 19/03/2017.
 */

public class OtherMeasurement {
    public String property;
    public String time;
    public String text;
    public Double value;

    public void setTime(Date t) {
        time = Conversion.Time.dateToString(t);
    }

    public Date getTime() {
        return Conversion.Time.stringToDate(time);
    }
}
