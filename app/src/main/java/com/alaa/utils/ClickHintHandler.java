package com.alaa.utils;

import android.animation.ValueAnimator;
import android.view.View;

public class ClickHintHandler {


    private View mView;
    private boolean mCancelled = false;

    public ClickHintHandler(View view) {
        mView = view;

    }

    public void start() {
        mCancelled = false;
        mView.animate().alpha(0).setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            boolean reverse = false;

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (!reverse && valueAnimator.getAnimatedFraction() == 1) {
                    reverse = true;
                    mView.animate().alpha(1).setUpdateListener(this).start();
                } else if (reverse && valueAnimator.getAnimatedFraction() == 1) {
                    reverse = false;
                    if (mCancelled) return;

                    mView.animate().alpha(0).setUpdateListener(this).start();
                }
            }
        }).start();
    }

    public void stop() {
        mCancelled = true;
    }
}
