package com.citisense.vidklopcic.citisense.util;

import android.graphics.PorterDuff;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.citisense.vidklopcic.citisense.AqiCardsFragment;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.fragments.AqiGraphFragment;
import com.citisense.vidklopcic.citisense.fragments.AqiOverviewGraph;
import com.citisense.vidklopcic.citisense.fragments.MeasuringStationDataFragment;
import com.citisense.vidklopcic.citisense.fragments.AqiOverviewFragment;

import com.citisense.vidklopcic.citisense.R;

import java.util.ArrayList;

public class MapPullUpPager {
    enum CurrentFragment { OVERVIEW, GRAPH, CARDS}
    CurrentFragment mCurrentFragmentType;
    MeasuringStationDataFragment mCurrentFragment;
    FragmentActivity mContext;
    AqiOverviewFragment mAqiOverviewFragment;
    AqiCardsFragment mAqiCardsFragment;
    AqiGraphFragment mAqiGraphFragment;
    android.support.v4.app.FragmentManager mFragmentManager;
    ArrayList<CitiSenseStation> mDataSource;
    Buttons mButtons;

    public MapPullUpPager(FragmentActivity context) {
        mContext = context;
        mAqiOverviewFragment = new AqiOverviewFragment();
        mAqiOverviewFragment.setOnLoadedListener(new AqiOverviewFragment.OnFragmentLoadedListener() {
            @Override
            public void onLoaded() {
                update();
            }
        });

        mAqiCardsFragment = new AqiCardsFragment();
        mAqiGraphFragment = new AqiGraphFragment();
        mFragmentManager = mContext.getSupportFragmentManager();
        mButtons = new Buttons();
    }

    public void setOverviewFragment() {
        if (mCurrentFragmentType == CurrentFragment.OVERVIEW) return;
        close();
        mCurrentFragmentType = CurrentFragment.OVERVIEW;
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.add(R.id.maps_pullup_fragment_container, mAqiOverviewFragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.commit();
        mCurrentFragment = mAqiOverviewFragment;
        update();
    }

    public void setCardsFragment() {
        if (mCurrentFragmentType == CurrentFragment.CARDS) return;
        close();
        mCurrentFragmentType = CurrentFragment.CARDS;
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.add(R.id.maps_pullup_fragment_container, mAqiCardsFragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.commit();
        mCurrentFragment = mAqiCardsFragment;
        update();
    }

    public void setGraphFragment() {
        if (mCurrentFragmentType == CurrentFragment.GRAPH) return;
        close();
        mCurrentFragmentType = CurrentFragment.GRAPH;
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.add(R.id.maps_pullup_fragment_container, mAqiGraphFragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.commit();
        mCurrentFragment = mAqiGraphFragment;
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

    class Buttons {
        static final float ANIMATION_SCALE = 1.1f;
        static final int ANIMATION_DURATION = 200;
        LinearLayout mButtonCardsContainer;
        LinearLayout mButtonOverviewContainer;
        LinearLayout mButtonHistoryContainer;
        int mNormalColor;
        int mSelectedColor;

        LinearLayout mSelectedButton;

        public Buttons() {
            mNormalColor = ContextCompat.getColor(mContext, R.color.button_gray);
            mSelectedColor = ContextCompat.getColor(mContext, R.color.button_selected_blue);
            mButtonCardsContainer = (LinearLayout) mContext.findViewById(R.id.pullup_cards_button_container);
            mButtonOverviewContainer = (LinearLayout) mContext.findViewById(R.id.pullup_overview_button_container);
            mButtonHistoryContainer = (LinearLayout) mContext.findViewById(R.id.pullup_graph_history_button_container);
            ((ImageView)mButtonCardsContainer.findViewById(R.id.pullup_button_icon)).setColorFilter(mNormalColor, PorterDuff.Mode.MULTIPLY);
            ((ImageView)mButtonOverviewContainer.findViewById(R.id.pullup_button_icon)).setColorFilter(mNormalColor, PorterDuff.Mode.MULTIPLY);
            ((ImageView)mButtonHistoryContainer.findViewById(R.id.pullup_button_icon)).setColorFilter(mNormalColor, PorterDuff.Mode.MULTIPLY);
            setButton(mButtonOverviewContainer);

            mButtonCardsContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setButton(mButtonCardsContainer);
                    setCardsFragment();
                }
            });

            mButtonOverviewContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setButton(mButtonOverviewContainer);
                    setOverviewFragment();
                }
            });

            mButtonHistoryContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setButton(mButtonHistoryContainer);
                    setGraphFragment();
                }
            });
        }

        public void setButton(LinearLayout button_container) {
            unsetCurrentButton();
            ImageView icon = (ImageView) button_container.findViewById(R.id.pullup_button_icon);
            icon.setColorFilter(mSelectedColor, PorterDuff.Mode.MULTIPLY);
            TextView text = (TextView) button_container.findViewById(R.id.pullup_button_text);
            icon.animate().scaleX(ANIMATION_SCALE).scaleY(ANIMATION_SCALE).setDuration(ANIMATION_DURATION).start();
            text.animate().scaleX(ANIMATION_SCALE).scaleY(ANIMATION_SCALE).setDuration(ANIMATION_DURATION).start();
            text.setTextColor(ContextCompat.getColor(mContext, R.color.button_selected_blue));
            mSelectedButton = button_container;
        }

        private void unsetCurrentButton() {
            if (mSelectedButton == null) return;
            ImageView icon = (ImageView) mSelectedButton.findViewById(R.id.pullup_button_icon);
            icon.setColorFilter(mNormalColor, PorterDuff.Mode.MULTIPLY);
            TextView text = (TextView) mSelectedButton.findViewById(R.id.pullup_button_text);
            icon.animate().scaleX(1).scaleY(1).setDuration(ANIMATION_DURATION).start();
            text.animate().scaleX(1).scaleY(1).setDuration(ANIMATION_DURATION).start();
            text.setTextColor(ContextCompat.getColor(mContext, R.color.button_gray));
        }
    }
}
