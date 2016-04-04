package com.citisense.vidklopcic.citisense.util;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.fragments.MeasuringStationDataFragment;
import com.citisense.vidklopcic.citisense.fragments.OverviewFragment;

import com.citisense.vidklopcic.citisense.R;

import java.util.ArrayList;

public class MapPullUpPager {
    enum CurrentFragment { OVERVIEW, GRAPH, CARDS}
    CurrentFragment mCurrentFragmentType;
    MeasuringStationDataFragment mCurrentFragment;
    FragmentActivity mContext;
    OverviewFragment mOverviewFragment;
    android.support.v4.app.FragmentManager mFragmentManager;
    ArrayList<CitiSenseStation> mDataSource;

    public MapPullUpPager(FragmentActivity context) {
        mContext = context;
        mOverviewFragment = new OverviewFragment();
        mOverviewFragment.setOnLoadedListener(new OverviewFragment.OnFragmentLoadedListener() {
            @Override
            public void onLoaded() {
                update();
            }
        });
        mFragmentManager = mContext.getSupportFragmentManager();
    }

    public void setOverviewFragment() {
        if (mCurrentFragmentType == CurrentFragment.OVERVIEW) return;
        mCurrentFragmentType = CurrentFragment.OVERVIEW;
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.add(R.id.maps_pullup_fragment_container, mOverviewFragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.commit();
        mCurrentFragment = mOverviewFragment;
        update();
    }

    public void close() {
        if (mCurrentFragment == null) return;
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.remove((Fragment) mCurrentFragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.commit();
        mCurrentFragment = null;
        mCurrentFragmentType = null;
    }

    public void setDataSource(ArrayList<CitiSenseStation> stations) {
        mDataSource = stations;
        update();
    }

    public void update() {
        if (mCurrentFragment != null)
            mCurrentFragment.update(mDataSource);
    }
}
