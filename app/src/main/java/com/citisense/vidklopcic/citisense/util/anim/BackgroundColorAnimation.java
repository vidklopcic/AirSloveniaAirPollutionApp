package com.citisense.vidklopcic.citisense.util.anim;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import com.citisense.vidklopcic.citisense.util.Conversion;

public class BackgroundColorAnimation extends Animation {
    private static final long DURATION = 400;
    View mView;
    int mStartColor;
    int mEndColor;

    public BackgroundColorAnimation(View view, int end_color) {
        mView = view;
        mStartColor = ((ColorDrawable)view.getBackground()).getColor();
        mEndColor = end_color;
        setDuration(DURATION);
    }

    @Override
    protected void applyTransformation(float percentage, Transformation t) {
        mView.setBackgroundColor(Conversion.interpolateColor(mStartColor, mEndColor, percentage));
    }
}
