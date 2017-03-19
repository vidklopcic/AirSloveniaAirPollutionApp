package com.vidklopcic.airsense.data.Gson;

import com.vidklopcic.airsense.util.Conversion;

import java.util.Date;

/**
 * Created by vidklopcic on 19/03/2017.
 */

public class MeasurementRangeParams {
    public MeasurementRangeParams(Date start, Date end) {
        setStart(start);
        setEnd(end);
    }

    String start;
    String end;

    public void setStart(Date time) {
        start = Conversion.Time.dateToString(time);
    }

    public void setEnd(Date time) {
        end = Conversion.Time.dateToString(time);
    }
}
