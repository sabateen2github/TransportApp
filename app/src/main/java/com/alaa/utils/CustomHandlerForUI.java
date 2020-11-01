package com.alaa.utils;

import android.os.Handler;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.alaa.viewmodels.ActivityModel;


public class CustomHandlerForUI {


    private Handler handler;

    public void init() {
        handler = new Handler();
    }

    public void post(LifecycleOwner owner, Runnable runnable) {

        handler.post(() -> {
            if (owner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED || owner.getLifecycle().getCurrentState() == Lifecycle.State.INITIALIZED) {
                return;
            }
            runnable.run();
        });

    }


    public void postDelayed(LifecycleOwner owner, Runnable runnable, long delayInMillis) {
        handler.postDelayed(() -> {
            if (owner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED || owner.getLifecycle().getCurrentState() == Lifecycle.State.INITIALIZED) {
                return;
            }
            runnable.run();
        }, ActivityModel.isSimulation ? delayInMillis / 60 : delayInMillis);
    }
}
