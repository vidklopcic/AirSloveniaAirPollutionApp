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

import com.citisense.vidklopcic.citisense.R;
import com.citisense.vidklopcic.citisense.data.entities.CitiSenseStation;
import com.citisense.vidklopcic.citisense.fragments.AqiPollutants;
import com.citisense.vidklopcic.citisense.fragments.AqiGraph;
import com.citisense.vidklopcic.citisense.fragments.AqiOverview;
import com.citisense.vidklopcic.citisense.fragments.PullUpBase;

import java.util.ArrayList;

public class MapPullUpPager {
    enum CurrentFragment { OVERVIEW, GRAPH, CARDS}
    CurrentFragment mCurrentFragmentType;
    PullUpBase mCurrentFragment;
    FragmentActivity mContext;
    AqiOverview mAqiOverviewFragment;
    AqiPollutants mAqiPollutantsFragment;
    AqiGraph mAqiGraphFragment;
    android.support.v4.app.FragmentManager mFragmentManager;
    ArrayList<CitiSenseStation> mDataSource;
    Buttons mButtons;

    public MapPullUpPager(FragmentActivity context) {
        mContext = context;
        mAqiOverviewFragment = new AqiOverview();
        mAqiOverviewFragment.setOnLoadedListener(new AqiOverview.OnFragmentLoadedListener() {
            @Override
            public void onLoaded() {
                update();
            }
        });

        mAqiPollutantsFragment = new AqiPollutants();
        mAqiGraphFragment = new AqiGraph();
        mFragmentManager = mContext.getSupportFragmentManager();
        mButtons = new Buttons();
    }

    public void setOverviewFragment() {
        if (mCurrentFragmentType == CurrentFragment.OVERVIEW) return;
        mButtons.setButton(mButtons.mButtonOverviewContainer);
        if (mFragmentManager.findFragmentByTag(CurrentFragment.OVERVIEW.toString()) != null)
            showFragment((PullUpBase) mFragmentManager.findFragmentByTag(CurrentFragment.OVERVIEW.toString()));
        else {
            FragmentTransaction transaction = hide();
            transaction.add(R.id.maps_pullup_fragment_container, mAqiOverviewFragment, CurrentFragment.OVERVIEW.toString());
            transaction.commit();
        }
        mCurrentFragmentType = CurrentFragment.OVERVIEW;
        mCurrentFragment = mAqiOverviewFragment;
        update();
    }

    public void setPollutantsFragment() {
        if (mCurrentFragmentType == CurrentFragment.CARDS) return;
        mButtons.setButton(mButtons.mButtonCardsContainer);
        if (mFragmentManager.findFragmentByTag(CurrentFragment.CARDS.toString()) != null)
            showFragment((PullUpBase) mFragmentManager.findFragmentByTag(CurrentFragment.CARDS.toString()));
        else {
            FragmentTransaction transaction = hide();
            transaction.add(R.id.maps_pullup_fragment_container, mAqiPollutantsFragment, CurrentFragment.CARDS.toString());
            transaction.commit();
        }
        mCurrentFragmentType = CurrentFragment.CARDS;
        mCurrentFragment = mAqiPollutantsFragment;
        update();
    }

    public void setGraphFragment() {
        if (mCurrentFragmentType == CurrentFragment.GRAPH) return;
        mButtons.setButton(mButtons.mButtonGraphContainer);
        if (mFragmentManager.findFragmentByTag(CurrentFragment.GRAPH.toString()) != null)
            showFragment((PullUpBase) mFragmentManager.findFragmentByTag(CurrentFragment.GRAPH.toString()));
        else {
            FragmentTransaction transaction = hide();
            transaction.add(R.id.maps_pullup_fragment_container, mAqiGraphFragment, CurrentFragment.GRAPH.toString());
            transaction.commit();
        }
        mCurrentFragmentType = CurrentFragment.GRAPH;
        mCurrentFragment = mAqiGraphFragment;
        update();
    }

    public void showFragment(PullUpBase fragment) {
        FragmentTransaction transaction = hide();
        transaction.show((Fragment) fragment);
        transaction.commit();
    }

    public FragmentTransaction hide() {
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        if (mFragmentManager.findFragmentByTag(CurrentFragment.OVERVIEW.toString()) != null)
            transaction.hide(mFragmentManager.findFragmentByTag(CurrentFragment.OVERVIEW.toString()));
        if (mFragmentManager.findFragmentByTag(CurrentFragment.CARDS.toString()) != null)
            transaction.hide(mFragmentManager.findFragmentByTag(CurrentFragment.CARDS.toString()));
        if (mFragmentManager.findFragmentByTag(CurrentFragment.GRAPH.toString()) != null)
            transaction.hide(mFragmentManager.findFragmentByTag(CurrentFragment.GRAPH.toString()));
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        return transaction;
    }

    public void close() {
        mButtons.setButton(mButtons.mButtonOverviewContainer);
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        if (mFragmentManager.findFragmentByTag(CurrentFragment.OVERVIEW.toString()) != null)
            transaction.remove(mFragmentManager.findFragmentByTag(CurrentFragment.OVERVIEW.toString()));
        if (mFragmentManager.findFragmentByTag(CurrentFragment.CARDS.toString()) != null)
            transaction.remove(mFragmentManager.findFragmentByTag(CurrentFragment.CARDS.toString()));
        if (mFragmentManager.findFragmentByTag(CurrentFragment.GRAPH.toString()) != null)
            transaction.remove(mFragmentManager.findFragmentByTag(CurrentFragment.GRAPH.toString()));
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.commit();
        mCurrentFragment = null;
        mCurrentFragmentType = null;
    }



    public void setDataSource(ArrayList<CitiSenseStation> stations) {
        mDataSource = stations;
        if (mDataSource != null && mDataSource.size() == 1)
            mButtons.setVisibility(View.VISIBLE);
        else
            mButtons.setVisibility(View.GONE);
        update();
    }

    public void update() {
        if (mDataSource == null)
            close();
        if (mCurrentFragment != null)
            mCurrentFragment.update(mDataSource);
    }

    class Buttons {
        static final float ANIMATION_SCALE = 1.1f;
        static final int ANIMATION_DURATION = 200;
        public LinearLayout mButtonCardsContainer;
        public LinearLayout mButtonOverviewContainer;
        public LinearLayout mButtonGraphContainer;
        int mNormalColor;
        int mSelectedColor;

        LinearLayout mSelectedButton;

        public Buttons() {
            mNormalColor = ContextCompat.getColor(mContext, R.color.button_gray);
            mSelectedColor = ContextCompat.getColor(mContext, R.color.button_selected_blue);
            mButtonCardsContainer = (LinearLayout) mContext.findViewById(R.id.pullup_cards_button_container);
            mButtonOverviewContainer = (LinearLayout) mContext.findViewById(R.id.pullup_overview_button_container);
            mButtonGraphContainer = (LinearLayout) mContext.findViewById(R.id.pullup_graph_history_button_container);
            ((ImageView)mButtonCardsContainer.findViewById(R.id.pullup_button_icon)).setColorFilter(mNormalColor, PorterDuff.Mode.MULTIPLY);
            ((ImageView)mButtonOverviewContainer.findViewById(R.id.pullup_button_icon)).setColorFilter(mNormalColor, PorterDuff.Mode.MULTIPLY);
            ((ImageView) mButtonGraphContainer.findViewById(R.id.pullup_button_icon)).setColorFilter(mNormalColor, PorterDuff.Mode.MULTIPLY);
            setButton(mButtonOverviewContainer);

            mButtonCardsContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mAqiPollutantsFragment.isRemoving()) return;
                    setPollutantsFragment();
                }
            });

            mButtonOverviewContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mAqiOverviewFragment.isRemoving()) return;
                    setOverviewFragment();
                }
            });

            mButtonGraphContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mAqiGraphFragment.isRemoving()) return;
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

        public void setVisibility(int visibility) {
            mContext.findViewById(R.id.maps_pullup_buttons).setVisibility(visibility);
            mContext.findViewById(R.id.maps_pullup_buttons_shadow).setVisibility(visibility);
        }
    }
}
