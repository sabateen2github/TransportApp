package com.alaa.transportapp;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;

public class RequestPermissionsActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = 0x1;

    @Override
    public void onStart() {
        super.onStart();
        this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            boolean isGrandted = Arrays.stream(grantResults).allMatch((item) -> item == PackageManager.PERMISSION_GRANTED);
            if (isGrandted) {
            } else {
                this.requestPermissions(permissions, REQUEST_CODE);
            }
        }
    }

    private void handleBroadcastEvent() {
        new GeoEventsHelper().handleGeoEvent(this, getIntent().getExtras().getParcelable(GeoEventsHelper.INTENT_EXTRA_GEOEVENT));
    }
}
