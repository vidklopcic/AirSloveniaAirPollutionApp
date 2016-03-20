package com.citisense.vidklopcic.citisense.util;

import android.app.Activity;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import com.citisense.vidklopcic.citisense.R;

public abstract class SlidingMenuHelper {
    public static com.jeremyfeinstein.slidingmenu.lib.SlidingMenu attach(WindowManager windowManager, Activity context) {
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
}
