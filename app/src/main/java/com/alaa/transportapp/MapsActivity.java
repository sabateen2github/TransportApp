package com.alaa.transportapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.alaa.fragments.MainFragment;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.gson.Gson;
import com.google.maps.android.SphericalUtil;


import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MapsActivity extends FragmentActivity {


    private ActivityModel viewModel;

    private static final int SETTINGS_CODE = 1999;
    private static final int PERMISSION_CODE = 1998;
    public static final int AUTO_COMPLETE_MAP = 1997;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ActivityModel.class);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.maps_api_key), Locale.US);
        }
        viewModel.mainHandelr = new Handler();
        if (savedInstanceState == null) {
            viewModel.callbacks_settings = new LinkedList<>();
            viewModel.callbacks_permission = new LinkedList<>();
            viewModel.exe = Executors.newSingleThreadExecutor();

            viewModel.exe.execute(() -> {
                try {
                    InputStreamReader reader = new InputStreamReader(getAssets().open("final_schedule.json"));
                    Gson gson = new Gson();
                    final PointsStructure points = gson.fromJson(reader, PointsStructure.class);
                    viewModel.index.postValue(points);
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            getSupportFragmentManager().beginTransaction().add(android.R.id.content, new MainFragment()).commit();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_CODE) {

            getCurrentLocationCallback callback = viewModel.callbacks_settings.removeFirst();
            getCurrentLocation(callback);
        } else if (requestCode == AUTO_COMPLETE_MAP) {
            if (resultCode == RESULT_OK) {

                Place place = Autocomplete.getPlaceFromIntent(data);
                getSearchMap c = viewModel.callback_search;

                viewModel.callback_search = null;
                c.onSelect(place.getLatLng(), place.getViewport());
            } else {
                getSearchMap c = viewModel.callback_search;
                viewModel.callback_search = null;
                if (c != null)
                    c.onSelect(null, null);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            getCurrentLocationCallback callback = viewModel.callbacks_permission.removeFirst();
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation(callback);
            } else {
                callback.onUpdate(null);
            }
        }
    }

    public void getCurrentLocation(getCurrentLocationCallback callback) {

        //get Current location
        LocationRequest request = LocationRequest.create();
        request.setNumUpdates(1);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(request);

        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(this);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, locationSettingsResponse -> {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                viewModel.callbacks_permission.addLast(callback);
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
                return;
            }

            fusedClient.requestLocationUpdates(request, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null) {
                        Location loc = locationResult.getLastLocation();
                        callback.onUpdate(new LatLng(loc.getLatitude(), loc.getLongitude()));
                    }
                }
            }, Looper.getMainLooper());

        });
        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {

                try {
                    viewModel.callbacks_settings.addLast(callback);
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(MapsActivity.this,
                            SETTINGS_CODE);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                    Log.e("alaa", "Cannot Resolve Location Settings Issue   MapsActivity.java");
                }
            }
        });

    }


    public void getSearchResult(getSearchMap callback) {
        List<Place.Field> fields = Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.VIEWPORT);

        Autocomplete.IntentBuilder builder = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields);
        Intent intent = builder.build(this);
        viewModel.callback_search = callback;
        startActivityForResult(intent, MapsActivity.AUTO_COMPLETE_MAP);

    }


    @FunctionalInterface
    public static interface getSearchMap {
        public void onSelect(@Nullable LatLng center, @Nullable LatLngBounds viewPort);
    }

    @FunctionalInterface
    public static interface getCurrentLocationCallback {
        public void onUpdate(@Nullable LatLng center);
    }

    public static class ActivityModel extends ViewModel {
        private LinkedList<getCurrentLocationCallback> callbacks_settings;
        private LinkedList<getCurrentLocationCallback> callbacks_permission;
        private getSearchMap callback_search;
        public MutableLiveData<PointsStructure> index;
        public Executor exe;
        public Handler mainHandelr;

        {
            index = new MutableLiveData<>();
        }
    }

    public static class PointsStructure {
        public String type;
        public Feature[] features;

        public static class Feature {
            public String type;
            public long id;
            public Geometry geometry;

            public static class Geometry {
                public String type;
                public double[] coordinates;
            }
        }

        public Feature getNearest(double latitude, double longitude) {
            double east_start = 35.485461d; // we used as a reference line for latitude by mistake :< and we don't want to recompile data again because it actually does not affect shit!
            double east_end = 36.736622d;

            Feature f = new Feature();
            f.geometry = new Feature.Geometry();
            f.geometry.coordinates = new double[]{longitude, latitude};

            int index = Arrays.binarySearch(features, f, (a, b) -> {
                double a_key = (a.geometry.coordinates[0] - east_start) + (a.geometry.coordinates[1] - east_start) * (east_end - east_start);
                double b_key = (b.geometry.coordinates[0] - east_start) + (b.geometry.coordinates[1] - east_start) * (east_end - east_start);
                return (int) ((a_key - b_key) * 1000000);
            });


            double distance_first = Double.MAX_VALUE;
            double distance_second = Double.MAX_VALUE;
            double distance_third = Double.MAX_VALUE;
            if (index > 0) {

                distance_first = SphericalUtil.computeDistanceBetween(new LatLng(latitude, longitude), new LatLng(features[index - 1].geometry.coordinates[1], features[index - 1].geometry.coordinates[0]));
            }

            distance_second = SphericalUtil.computeDistanceBetween(new LatLng(latitude, longitude), new LatLng(features[index].geometry.coordinates[1], features[index].geometry.coordinates[0]));

            if (index < features.length - 1) {
                distance_third = SphericalUtil.computeDistanceBetween(new LatLng(latitude, longitude), new LatLng(features[index + 1].geometry.coordinates[1], features[index + 1].geometry.coordinates[0]));
            }

            double min = Math.min(Math.min(distance_first, distance_second), distance_third);
            if (min == distance_first) {
                return features[index - 1];
            } else if (min == distance_second) {
                return features[index];
            } else {
                return features[index + 1];
            }


        }
    }

}