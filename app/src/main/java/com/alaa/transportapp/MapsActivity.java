package com.alaa.transportapp;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.alaa.fragments.MainFragment;
import com.alaa.utils.getTimeUtils;
import com.alaa.viewmodels.ActivityModel;
import com.alaa.viewmodels.PassengerRequestModel;
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
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MapsActivity extends FragmentActivity {


    private ActivityModel viewModel;

    private static final int SETTINGS_CODE = 1999;
    private static final int PERMISSION_CODE = 1998;
    public static final int AUTO_COMPLETE_MAP = 1997;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

            viewModel.exe.execute(() -> {
                try {
                    InputStreamReader reader = new InputStreamReader(getAssets().open("final_schedule.json"));
                    Gson gson = new Gson();
                    final ActivityModel.PointsStructure points = gson.fromJson(reader, ActivityModel.PointsStructure.class);
                    viewModel.index.postValue(points);
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            getSupportFragmentManager().beginTransaction().add(android.R.id.content, new MainFragment()).commit();
        }

        if (ActivityModel.isSimulation) {
            runSimulation();
        }


        provider.get(PassengerRequestModel.class).status.observe(this, (item) -> {

            switch (item) {

                case PassengerRequestModel.STATUS_PENDING:
                    if (viewModel.already_set) break;
                    viewModel.instant = getTimeUtils.getInstant();
                    viewModel.already_set = true;
                    keepchecking();
                    break;
                default:
                    viewModel.already_set = false;
                    break;
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SETTINGS_CODE) {

            ActivityModel.getCurrentLocationCallback callback = viewModel.callbacks_settings.removeFirst();
            getCurrentLocation(callback);
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
            ActivityModel.getCurrentLocationCallback callback = viewModel.callbacks_permission.removeFirst();
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation(callback);
            } else {
                callback.onUpdate(null);
            }
        }
    }

    public void getCurrentLocation(ActivityModel.getCurrentLocationCallback callback) {

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


    public void getSearchResult(ActivityModel.getSearchMap callback) {
        List<Place.Field> fields = Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.VIEWPORT);

        Autocomplete.IntentBuilder builder = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields);
        Intent intent = builder.build(this);
        viewModel.callback_search = callback;
        startActivityForResult(intent, MapsActivity.AUTO_COMPLETE_MAP);

    }


    public void runSimulation() {


        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), MessageFormat.format("Simulation is running {0}", getTimeUtils.getCurrentTime()), Snackbar.LENGTH_INDEFINITE);

        snackbar.setAction("توقف", (v) -> {
            ActivityModel.isSimulation = false;
            snackbar.dismiss();
        });
        snackbar.setActionTextColor(Color.CYAN);

        ViewModelProvider provider = new ViewModelProvider(this);
        provider.get(ActivityModel.class).mainHandelr.postDelayed(this, new Runnable() {
            @Override
            public void run() {
                snackbar.setText(MessageFormat.format("simulation is running {0}", getTimeUtils.getCurrentTime()));
                provider.get(ActivityModel.class).mainHandelr.postDelayed(MapsActivity.this, this, 1000);
            }
        }, 1000);
        snackbar.show();
        ActivityModel.isSimulation = true;
    }


    public void handleAboutClick() {
    }


    public void keepchecking() {

        viewModel.mainHandelr.post(this, new Runnable() {
            @Override
            public void run() {
                PassengerRequestModel passengerModel = new ViewModelProvider(MapsActivity.this).get(PassengerRequestModel.class);
                switch (passengerModel.status.getValue()) {
                    case PassengerRequestModel.STATUS_NOT_SENT:
                        break;
                    case PassengerRequestModel.STATUS_PENDING: {
                        if (getTimeUtils.getPeriodFromNow(viewModel.instant[0], viewModel.instant[1]) * -1 >= 5) {
                            passengerModel.status.setValue(Math.random() > 0.5d ? PassengerRequestModel.STATUS_ACCEPTED : PassengerRequestModel.STATUS_DENIED);
                        } else {
                            viewModel.mainHandelr.postDelayed(MapsActivity.this, this, 1000);
                        }
                    }
                    break;
                    case PassengerRequestModel.STATUS_DENIED:
                        break;
                    case PassengerRequestModel.STATUS_ACCEPTED:
                        break;
                }
            }
        });
    }
}