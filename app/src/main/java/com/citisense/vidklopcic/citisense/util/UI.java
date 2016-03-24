package com.citisense.vidklopcic.citisense.util;

import android.app.Activity;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.citisense.vidklopcic.citisense.R;
import com.citisense.vidklopcic.citisense.data.Constants;

public abstract class UI {
    public static com.jeremyfeinstein.slidingmenu.lib.SlidingMenu getSlidingMenu(WindowManager windowManager, Activity context) {
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        com.jeremyfeinstein.slidingmenu.lib.SlidingMenu menu = new com.jeremyfeinstein.slidingmenu.lib.SlidingMenu(context);
        menu.setMode(com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.LEFT);
        menu.setTouchModeAbove(com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.TOUCHMODE_MARGIN);
        menu.setShadowWidthRes(R.dimen.shadow_width);
        menu.setShadowDrawable(R.drawable.shadow);
        menu.setFadeDegree(0.35f);
        menu.attachToActivity(context, com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.SLIDING_CONTENT);
        menu.setBehindOffset((int) (size.x * 0.1));
        menu.setMenu(R.layout.sliding_menu);
        return menu;
    }

    public static class AQISummary {
        Activity mContext;
        LinearLayout mContainer;
        LinearLayout mContent;
        TextView mTitle;
        TextView mText;
        ImageView mIcon;

        public AQISummary(Activity context, int container) {
            mContext = context;
            mContainer = (LinearLayout) mContext.findViewById(container);
            mContent = (LinearLayout) mContext.getLayoutInflater().inflate(R.layout.dashboard_aqi_summary,
                    mContainer, false);
            mContainer.addView(mContent);

            mTitle = (TextView) mContent.findViewById(R.id.aqi_summary_title);
            mText = (TextView) mContent.findViewById(R.id.aqi_summary_text);
            mIcon = (ImageView) mContent.findViewById(R.id.aqi_summary_icon);
        }

        public void setAqi(int aqi) {
            int aqi_title;
            int aqi_text;
            int aqi_icon;
            int color;
            if (aqi > Constants.AQI.HAZARDOUS) {
                color = R.color.aqi_hazardous;
                aqi_title = R.string.aqi_summary_hazardous_title;
                aqi_text = R.string.aqi_summary_hazardous_text;
                aqi_icon = R.drawable.hazardous_sign;
            } else if (aqi > Constants.AQI.VERY_UNHEALTHY) {
                color = R.color.aqi_very_unhealthy;
                aqi_title = R.string.aqi_summary_very_unhealthy_title;
                aqi_text = R.string.aqi_summary_very_unhealthy_text ;
                aqi_icon = R.drawable.very_unhealthy_sign ;
            } else if (aqi > Constants.AQI.UNHEALTHY) {
                color = R.color.aqi_unhealthy;
                aqi_title = R.string.aqi_summary_unhealthy_title;
                aqi_text = R.string.aqi_summary_unhealthy_text;
                aqi_icon = R.drawable.unhealthy_sign;
            } else if (aqi > Constants.AQI.UNHEALTHY_SENSITIVE) {
                color = R.color.aqi_unhealthy_for_sensitive;
                aqi_title = R.string.aqi_summary_unhealthy_for_sensitive_title;
                aqi_text = R.string.aqi_summary_unhealthy_for_sensitive_text;
                aqi_icon = R.drawable.unhealthy_for_sens_sign;
            } else if (aqi > Constants.AQI.MODERATE) {
                color = R.color.aqi_moderate;
                aqi_title = R.string.aqi_summary_moderate_title;
                aqi_text = R.string.aqi_summary_moderate_text;
                aqi_icon = R.drawable.moderate_sign;
            } else {
                color = R.color.aqi_good;
                aqi_title = R.string.aqi_summary_good_title;
                aqi_text = R.string.aqi_summary_good_text;
                aqi_icon = R.drawable.good_sign;
            }
            mContainer.setVisibility(View.VISIBLE);
            mText.setText(aqi_text);
            mTitle.setText(aqi_title);
            mTitle.setTextColor(mContext.getResources().getColor(color));
            mIcon.setImageResource(aqi_icon);
        }
    }
}
