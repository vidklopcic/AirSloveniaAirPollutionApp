package com.citisense.vidklopcic.citisense.util.anim;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.TextView;

import com.citisense.vidklopcic.citisense.fragments.AqiOverviewGraph;

public class AqiBarAnimation extends Animation implements Animation.AnimationListener {
    private static final long DURATION = 300;
    AqiOverviewGraph mFragment;
    TextView mLabel;
    View mBarContent;
    Integer start_aqi;
    Integer add;

    AqiBarAnimation(AqiOverviewGraph fragment, TextView label, View bar_content, Integer aqi_start, Integer aqi_end) {
        mFragment = fragment;
        mLabel = label;
        mBarContent = bar_content;
        setRepeatCount(Math.abs(aqi_start - aqi_end));
        setDuration(DURATION);

        if (aqi_start < aqi_end) {
            add = 1;
        } else {
            add = -1;
        }

        setAnimationListener(this);
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {

    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        int aqi = start_aqi+(getRepeatCount()*add);
        mFragment.setBarAqi(mBarContent, aqi);
        mLabel.setText(aqi);
    }
}
