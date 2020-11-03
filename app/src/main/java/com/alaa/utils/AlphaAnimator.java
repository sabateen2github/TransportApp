package com.alaa.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

public class AlphaAnimator {

    private View mView;


    public AlphaAnimator(View view) {
        mView = view;
    }

    public void run() {
        runnable.run();
    }

    Runnable runnable = new Runnable() {
        int start = 0;
        long elapsed;

        @Override
        public void run() {

            if (start == 0) {
                elapsed = System.currentTimeMillis();
            }

            if (start % 2 == 0) {
                if (System.currentTimeMillis() - elapsed >= 5000) {
                    return;
                }
                mView.animate().alpha(0.9f).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        start++;
                        update();
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        start++;
                        update();
                    }

                    private void update() {
                        run();
                    }
                });
            } else {
                mView.animate().alpha(1).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        start++;
                        update();
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        start++;
                        update();
                    }

                    private void update() {
                        mView.setAlpha(1);
                        run();
                    }
                });
            }

        }
    };
}
