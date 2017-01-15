package com.vidklopcic.airsense.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import com.vidklopcic.airsense.R;
import com.vidklopcic.airsense.data.Constants;
import com.vidklopcic.airsense.data.entities.MeasuringStation;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class FABPollutants {
    public interface FABPollutantsListener {
        void onFABPollutantSelected(String pollutant);
    }
    private Integer mCurrentPollutantIcon = R.drawable.ic_sum;
    private FloatingActionMenu mFABPollutants;
    private HashMap<String, FloatingActionButton> mButtons;
    private String mSelectedPollutant;
    private List<MeasuringStation> mObservedStations;
    private FABPollutantsListener mListener;
    private FloatingActionButton mSumFab;
    private boolean mFABPollutantsIsOpened = false;
    private AnimatorSet mSet;
    Activity mContext;
    public FABPollutants(Activity context, FloatingActionMenu fab, FABPollutantsListener listener) {
        mContext = context;
        mFABPollutants = fab;
        setAnimation();
        mListener = listener;
        mButtons = new HashMap<>();

        mFABPollutants.getMenuIconView().setImageResource(mCurrentPollutantIcon);
        mFABPollutants.getMenuIconView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFABPollutantsIsOpened = !mFABPollutantsIsOpened;
                if (mFABPollutantsIsOpened)
                    mFABPollutants.open(true);
                else
                    mFABPollutants.close(true);
            }
        });

        mSumFab = new FloatingActionButton(mContext);
        mSumFab.setImageResource(R.drawable.ic_sum);
        mSumFab.setButtonSize(FloatingActionButton.SIZE_MINI);
        mSumFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentPollutantIcon = R.drawable.ic_sum;
                mSelectedPollutant = null;
                mSet.cancel();
                mFABPollutantsIsOpened = false;
                mFABPollutants.close(true);
                mListener.onFABPollutantSelected(null);
            }
        });
        mSumFab.setLabelText(mContext.getString(R.string.overview));

        mFABPollutants.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean b) {
                if (!b) {
                    mFABPollutantsIsOpened = false;
                    mFABPollutants.getMenuIconView().setImageResource(mCurrentPollutantIcon);
                }
            }
        });
    }

    private Integer getDrawableForPollutant(String pollutant) {
        if (pollutant.equals(Constants.ARSOStation.CO_KEY))
            return R.drawable.ic_co;
        else if (pollutant.equals(Constants.ARSOStation.SO2_KEY))
            return R.drawable.ic_so2;
        else if (pollutant.equals(Constants.ARSOStation.PM10_KEY))
            return R.drawable.ic_pm10;
        else if (pollutant.equals(Constants.ARSOStation.NO2_KEY))
            return R.drawable.ic_no;
        else if (pollutant.equals(Constants.ARSOStation.O3_KEY))
            return R.drawable.ic_o3;
        else
            return null;
    }

    public void update(List<MeasuringStation> stations) {
        clear();
        mObservedStations = stations;
        ArrayList<HashMap<String, Integer>> averages = MeasuringStation.getAverages(stations);
        if (averages != null && averages.get(MeasuringStation.AVERAGES_POLLUTANTS).size() != 0) {
            HashMap<String, Integer> pollutants = averages.get(MeasuringStation.AVERAGES_POLLUTANTS);
            for (String pollutant : pollutants.keySet()) {
                if (mSelectedPollutant != null && pollutant.equals(mSelectedPollutant))
                    setSelectedPollutant(pollutant, pollutants.get(pollutant));
                addPollutant(pollutant, pollutants.get(pollutant));
            }
            int max_color = AQI.getLinearColor(Collections.max(pollutants.values()), mContext);
            mSumFab.setColorPressed(max_color);
            mSumFab.setColorNormal(max_color);
            if (mSelectedPollutant == null) {
                mFABPollutants.setMenuButtonColorNormal(max_color);
                mFABPollutants.setMenuButtonColorPressed(max_color);
            }
        } else {
            mFABPollutants.setMenuButtonColorNormalResId(R.color.gray);
            mFABPollutants.setMenuButtonColorPressedResId(R.color.gray);
            mSumFab.setColorNormalResId(R.color.gray);
            mSumFab.setColorPressedResId(R.color.gray);
        }
    }

    private void clear() {
        mFABPollutants.removeAllMenuButtons();
        mButtons.clear();
        mFABPollutants.addMenuButton(mSumFab);
    }

    private void addPollutant(String pollutant, Integer aqi) {
        if (mButtons.containsKey(pollutant)) return;
        Integer icon = getDrawableForPollutant(pollutant);
        if (icon == null) return;
        FloatingActionButton fab = new FloatingActionButton(mContext);
        fab.setImageResource(icon);
        fab.setColorNormal(AQI.getLinearColor(aqi, mContext));
        fab.setColorPressed(AQI.getLinearColor(aqi, mContext));
        fab.setLabelText("AQI is " + aqi.toString());
        fab.setTag(R.id.pollutant_tag, pollutant);
        fab.setTag(R.id.aqi_tag, aqi);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pollutant = (String) v.getTag(R.id.pollutant_tag);
                Integer aqi = (Integer) v.getTag(R.id.aqi_tag);
                setSelectedPollutant(pollutant, aqi);
                mFABPollutantsIsOpened = false;
                mFABPollutants.close(true);
                mListener.onFABPollutantSelected(pollutant);
            }
        });
        fab.setButtonSize(FloatingActionButton.SIZE_MINI);
        mButtons.put(pollutant, fab);
        mFABPollutants.addMenuButton(fab);
    }

    public void refresh() {
        update(mObservedStations);
    }

    private void setSelectedPollutant(String pollutant, int aqi) {
        mCurrentPollutantIcon = getDrawableForPollutant(pollutant);
        mSelectedPollutant = pollutant;
        mFABPollutants.setMenuButtonColorNormal(AQI.getLinearColor(aqi, mContext));
    }
    private void setAnimation() {
        mSet = new AnimatorSet();

        ObjectAnimator scaleOutX = ObjectAnimator.ofFloat(mFABPollutants.getMenuIconView(), "scaleX", 1.0f, 0.2f);
        ObjectAnimator scaleOutY = ObjectAnimator.ofFloat(mFABPollutants.getMenuIconView(), "scaleY", 1.0f, 0.2f);

        ObjectAnimator scaleInX = ObjectAnimator.ofFloat(mFABPollutants.getMenuIconView(), "scaleX", 0.2f, 1.0f);
        ObjectAnimator scaleInY = ObjectAnimator.ofFloat(mFABPollutants.getMenuIconView(), "scaleY", 0.2f, 1.0f);

        scaleOutX.setDuration(50);
        scaleOutY.setDuration(50);

        scaleInX.setDuration(150);
        scaleInY.setDuration(150);

        scaleInX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mFABPollutants.getMenuIconView().setImageResource(mFABPollutantsIsOpened
                        ? R.drawable.ic_close : mCurrentPollutantIcon);
            }
        });

        mSet.play(scaleOutX).with(scaleOutY);
        mSet.play(scaleInX).with(scaleInY).after(scaleOutX);
        mSet.setInterpolator(new OvershootInterpolator(2));
        mFABPollutants.setIconToggleAnimatorSet(mSet);
    }
}
