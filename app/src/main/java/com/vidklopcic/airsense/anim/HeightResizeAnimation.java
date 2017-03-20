package com.vidklopcic.airsense.anim;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ScrollView;

/**
 * Created by vidklopcic on 20/03/2017.
 */

public class HeightResizeAnimation extends Animation {
    public interface AnimationEndCallback {
        void onEnd(View view);
    }

    final int targetHeight;
    View view;
    int startHeight;
    ScrollView mScrollView;
    AnimationEndCallback callback;
    boolean called = false;
    Integer scroll_diff;
    Integer scroll_start;
    public HeightResizeAnimation(View view, int targetHeight, ScrollView scrollView, Integer scroll) {
        this.view = view;
        this.targetHeight = targetHeight;
        this.startHeight = view.getHeight();
        mScrollView = scrollView;
        if (mScrollView != null) {
            scroll_start = mScrollView.getScrollY();
            scroll_diff = scroll_start - scroll;
        }
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        Integer newHeight = (int) (startHeight + (targetHeight - startHeight) * interpolatedTime);
        view.getLayoutParams().height = newHeight;
        view.requestLayout();
        if (mScrollView != null) {
            mScrollView.scrollTo(0, (int) (scroll_start-scroll_diff*interpolatedTime));
        }
        if (interpolatedTime == 1f && callback != null && !called) {
            callback.onEnd(view);
            called = true;
            view.invalidate();
        }
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
    }

    @Override
    public boolean willChangeBounds() {
        return true;
    }

    public void setOnEndCallback(AnimationEndCallback callback) {
        this.callback = callback;
    }
}