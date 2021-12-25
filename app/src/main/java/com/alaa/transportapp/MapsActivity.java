package com.alaa.transportapp;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.alaa.fragments.ChooseRouteFragment;
import com.alaa.fragments.ChooseServiceFragment;
import com.alaa.utils.GetAssets;
import com.alaa.viewmodels.ActivityModel;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
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
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

public class MapsActivity extends FragmentActivity {


    private ActivityModel viewModel;

    private static final int SETTINGS_CODE = 1999;
    private static final int PERMISSION_CODE = 1998;
    public static final int AUTO_COMPLETE_MAP = 1997;

    public static final String FRAGMENT_CLASS_KEY = "Fragment Class";
    public static final String FRAGMENT_EXTRA_DATA_KEY = "Fragment Extra";

    public static final String UNIQUE_JOB_ID = "AnalyticsWorkID";
    public static final String INIT_GEOFENCE_REGISTER = "InitRegisterGeofences";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Google Play will install latest OpenSSL
            ProviderInstaller.installIfNeeded(getApplicationContext());
            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, null);
            sslContext.createSSLEngine();
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException
                | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }


        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(AnalyticsJob.class, 3, TimeUnit.MINUTES).build();
        Operation operation = WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(UNIQUE_JOB_ID, ExistingPeriodicWorkPolicy.REPLACE, workRequest);


        findViewById(android.R.id.content).setBackgroundColor(getColor(R.color.background_color));

        ViewModelProvider provider = new ViewModelProvider(this);
        viewModel = provider.get(ActivityModel.class);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.maps_api_key), Locale.US);
        }
        viewModel.mainHandelr.init();
        if (savedInstanceState == null) {
            viewModel.callbacks_settings = new LinkedList<>();
            viewModel.callbacks_permission = new LinkedList<>();
            viewModel.exe = Executors.newSingleThreadExecutor();
            Application app = getApplication();
            viewModel.exe.execute(() -> {
                try {
                    InputStreamReader reader = new InputStreamReader(GetAssets.open(app, "final_schedule.json"));
                    Gson gson = new Gson();
                    final ActivityModel.PointsStructure points = gson.fromJson(reader, ActivityModel.PointsStructure.class);
                    viewModel.index.postValue(points);
                    reader.close();
                    getCurrentLocation((LatLng) -> {
                        new GeoEventsHelper().initRegisterGeoFences(MapsActivity.this, points, LatLng.latitude, LatLng.longitude);
                    }, false, MapsActivity.this);
                } catch (IOException e) {

                    e.printStackTrace();
                }
            });

            getSupportFragmentManager().beginTransaction().add(android.R.id.content, new ChooseServiceFragment()).commit();
        }

        if (getIntent().getExtras() != null && getIntent().getExtras().getBoolean(GeoEventsHelper.INTENT_EXTRA_GEOEVENT)) {
            ChooseServiceFragment chooseServiceFragment = new ChooseServiceFragment();

            Bundle fragmentBundle = Bundle.EMPTY;
            fragmentBundle.putSerializable(FRAGMENT_CLASS_KEY, ChooseRouteFragment.class);
            fragmentBundle.putLong(FRAGMENT_EXTRA_DATA_KEY, getIntent().getExtras().getLong(GeoEventsHelper.ACTIVITY_INTENT_ROUTE_ID));
            chooseServiceFragment.setArguments(fragmentBundle);
            Log.e("Alaa", "Start notification choose route");
            getSupportFragmentManager().beginTransaction().replace(android.R.id.content, chooseServiceFragment).commit();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_CODE) {

            ActivityModel.CallbackWrapper wrapper = viewModel.callbacks_settings.removeFirst();
            getCurrentLocation(wrapper.callback, wrapper.continuous, wrapper.owner);
        } else if (requestCode == AUTO_COMPLETE_MAP) {
            if (resultCode == RESULT_OK) {

                Place place = Autocomplete.getPlaceFromIntent(data);
                ActivityModel.getSearchMap c = viewModel.callback_search;

                viewModel.callback_search = null;
                c.onSelect(place.getLatLng(), place.getViewport());
            } else {
                ActivityModel.getSearchMap c = viewModel.callback_search;
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

            ActivityModel.CallbackWrapper wrapper = viewModel.callbacks_permission.removeFirst();
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation(wrapper.callback, wrapper.continuous, wrapper.owner);
            } else {
                getCurrentLocation(wrapper.callback, wrapper.continuous, wrapper.owner);
            }
        }
    }

    public void getCurrentLocation(ActivityModel.getCurrentLocationCallback callback, boolean continuous, LifecycleOwner owner) {

        if (owner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
            return;
        }

        //get Current location
        LocationRequest request = LocationRequest.create();
        if (!continuous)
            request.setNumUpdates(1);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(request);

        FusedLocationProviderClient fusedClient = LocationServices.getFusedLocationProviderClient(this);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, locationSettingsResponse -> {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityModel.CallbackWrapper wrapper = new ActivityModel.CallbackWrapper();
                wrapper.callback = callback;
                wrapper.continuous = continuous;
                wrapper.owner = owner;
                viewModel.callbacks_permission.addLast(wrapper);
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, PERMISSION_CODE);
                return;
            }


            LocationCallback locationCallback;
            fusedClient.requestLocationUpdates(request, locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null) {
                        Location loc = locationResult.getLastLocation();
                        callback.onUpdate(new LatLng(loc.getLatitude(), loc.getLongitude()));
                    }
                }
            }, Looper.getMainLooper());
            owner.getLifecycle().addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {

                    if (source.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
                        Log.e("Alaa", "Observer location destroyed");
                        owner.getLifecycle().removeObserver(this);
                        fusedClient.removeLocationUpdates(locationCallback);
                    }
                }
            });
        });
        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {

                try {
                    ActivityModel.CallbackWrapper wrapper = new ActivityModel.CallbackWrapper();
                    wrapper.callback = callback;
                    wrapper.continuous = continuous;
                    wrapper.owner = owner;
                    viewModel.callbacks_settings.addLast(wrapper);
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


    public void getSearchResult(ActivityModel.getSearchMap callback) {
        List<Place.Field> fields = Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.VIEWPORT);

        Autocomplete.IntentBuilder builder = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields);
        Intent intent = builder.build(this);
        viewModel.callback_search = callback;
        startActivityForResult(intent, MapsActivity.AUTO_COMPLETE_MAP);

    }

}