package com.alaa.utils;

import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;


public class CustomHandlerForUI {


    private Handler handler;

    public void init() {
        handler = new Handler();
    }

    public void post(LifecycleOwner owner, Runnable runnable) {

        handler.post(() -> {
            if (owner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
                return;
            }
            if (owner.getLifecycle().getCurrentState() == Lifecycle.State.INITIALIZED) {
                postDelayed(owner, runnable, 500);
                return;
            }
            runnable.run();
        });

    }


    public void postDelayed(LifecycleOwner owner, Runnable runnable, long delayInMillis) {
        handler.postDelayed(() -> {
            if (owner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
                return;
            }
            if (owner.getLifecycle().getCurrentState() == Lifecycle.State.INITIALIZED) {
                postDelayed(owner, runnable, delayInMillis);
                return;
            }
            Log.e("Alaa", "State " + owner.getLifecycle().getCurrentState());
            runnable.run();
        }, delayInMillis);
    }
}
