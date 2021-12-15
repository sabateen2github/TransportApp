package com.alaa.transportapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LocationChangeReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        new GeoEventsHelper().handleGeoEvent(context, intent);
    }

}
