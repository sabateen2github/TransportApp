package com.alaa.transportapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class LocationChangeReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Showing", Toast.LENGTH_SHORT).show();
        new GeoEventsHelper().handleGeoEvent(context, intent);
    }

}
