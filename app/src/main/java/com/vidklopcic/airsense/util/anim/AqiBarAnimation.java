package com.vidklopcic.airsense.util.anim;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import com.vidklopcic.airsense.fragments.OverviewBarChart;

public class AqiBarAnimation extends Animation {
    private static final long DURATION = 1000;
    OverviewBarChart mFragment;
    LinearLayout mLabel;
    View mBarContent;
    Integer start_aqi;
    Integer repeat_count;
    Integer add;

    public AqiBarAnimation(OverviewBarChart fragment, LinearLayout label, View bar_content, Integer aqi_start, Integer aqi_end) {
        mFragment = fragment;
        mLabel = label;
        mBarContent = bar_content;
        repeat_count = Math.abs(aqi_start - aqi_end);
        start_aqi = aqi_start;

        setDuration(DURATION);
        setInterpolator(new AccelerateDecelerateInterpolator());

        if (aqi_start < aqi_end) {
            add = 1;
        } else {
            add = -1;
        }
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        Float aqi = start_aqi+repeat_count*interpolatedTime*add;
        mFragment.setBarAqi(mBarContent, aqi);
        mFragment.setLabelAqi(mLabel, aqi.intValue());
    }
}
