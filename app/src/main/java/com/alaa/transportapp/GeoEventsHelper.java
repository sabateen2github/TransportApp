package com.alaa.transportapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.room.Room;

import com.alaa.roomdb.EventsDB;
import com.alaa.roomdb.dao.entities.Entering;
import com.alaa.roomdb.dao.entities.Notification;
import com.alaa.utils.GetAssets;
import com.alaa.viewmodels.ActivityModel;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import org.apache.commons.collections4.SetUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GeoEventsHelper {


    public static final String INTENT_EXTRA_GEOEVENT = "geoEvent";
    public static final String ACTIVITY_INTENT_CHOOSE_ROUTE_DWELLING = "dwelling";
    public static final String ACTIVITY_INTENT_ROUTE_ID = "Route ID";

    private static final String REGISTERED_GEOFENCES = "Registered Geofences";
    private static final String REQ_ID_KEY = "REQ_ID";
    public static final String EVENTS_DB = "eventDB";

    public static final String CHANNEL_ID = "a5e556ff788b6c688445cc5648723";
    public static final int NOTIFICATION_ID = 0x1;

    private static final int DWELLING_PERIOD_SECONDS = 10;

    private EventsDB appDatabase;

    public void handleGeoEvent(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        Log.e("Alaa", "Handle GeoEvent " + event.getGeofenceTransition());
        if (event.hasError()) {
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(GetAssets.open(context.getApplicationContext(), "final_schedule.json"))) {
            Gson gson = new Gson();
            ActivityModel.PointsStructure points = gson.fromJson(reader, ActivityModel.PointsStructure.class);
            appDatabase = Room.databaseBuilder(context, EventsDB.class, EVENTS_DB).build();
            if (event.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_DWELL) {
                handleDwelling(points, event, context.getApplicationContext(), intent);
            } else if (event.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_ENTER) {
                handleEntering(points, event, context.getApplicationContext(), intent);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

    }

    public void registerNotificationResponse(Context context, ActivityModel.PointsStructure.Feature feature, String routeId) {

        AsyncTask.execute(() -> {
            Notification notification = new Notification();
            notification.instant = Instant.now();
            notification.latitude = feature.geometry.coordinates[1];
            notification.longitude = feature.geometry.coordinates[0];
            notification.userId = GetAssets.generateUserId(context);
            notification.routeId = routeId;

            appDatabase.getNotificationDAO().insert(notification);
        });


    }


    private void handleEntering(ActivityModel.PointsStructure pointsStructure, GeofencingEvent event, Context context, Intent intent) {

        AsyncTask.execute(() -> {
            Entering entering = new Entering();
            entering.userId = GetAssets.generateUserId(context);
            entering.instant = Instant.now();
            entering.latitude = event.getTriggeringLocation().getLatitude();
            entering.longitude = event.getTriggeringLocation().getLongitude();
            appDatabase.getEnteringDAO().insert(entering);
        });

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Intent activityIntent = new Intent(context, RequestPermissionsActivity.class);
            activityIntent.putExtra(INTENT_EXTRA_GEOEVENT, intent);
            context.startActivity(activityIntent);
            return;
        } else {
            Set<String> oldSet = context.getSharedPreferences(REGISTERED_GEOFENCES, Context.MODE_PRIVATE).getStringSet(REQ_ID_KEY, new HashSet<>());
            Set<ActivityModel.PointsStructure.Feature> featureSet = pointsStructure.nearestKthElements(100, event.getTriggeringLocation().getLatitude(), event.getTriggeringLocation().getLongitude());
            Set<String> newSet = featureSet.stream().map((item) -> item.geometry.toString()).collect(Collectors.toSet());

            SetUtils.SetView<String> toRemove = SetUtils.difference(oldSet, newSet);
            if (toRemove.isEmpty()) return;

            SetUtils.SetView<String> toAdd = SetUtils.difference(newSet, oldSet);
            List<Geofence> geofenceList = featureSet.stream().map(feature -> {

                        if (!toAdd.contains(feature.geometry.toString())) {
                            return null;
                        }

                        return new Geofence.Builder().setRequestId(feature.geometry.toString())
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_ENTER)
                                .setLoiteringDelay(DWELLING_PERIOD_SECONDS * 1000)
                                .setExpirationDuration(24 * 60 * 60 * 1000)
                                .setCircularRegion(feature.geometry.coordinates[1], feature.geometry.coordinates[0], 150)
                                .build();
                    }
            ).filter((item) -> item != null).collect(Collectors.toList());

            GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                    .addGeofences(geofenceList)
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .build();

            Intent broadcastIntent = new Intent(context, LocationChangeReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);

            geofencingClient.removeGeofences(toRemove.stream().collect(Collectors.toList())).addOnSuccessListener((res) -> {
                geofencingClient.addGeofences(geofencingRequest, pendingIntent).addOnSuccessListener((result) -> {
                    Set<String> stringSet = geofenceList.stream().map((item) -> item.getRequestId()).collect(Collectors.toSet());
                    context.getSharedPreferences(REGISTERED_GEOFENCES, Context.MODE_PRIVATE).edit().putStringSet(REQ_ID_KEY, stringSet).commit();
                });
            });

        }
    }

    private void handleDwelling(ActivityModel.PointsStructure pointsStructure, GeofencingEvent event, Context context, Intent intent) {
        ActivityModel.PointsStructure.Feature feature = pointsStructure.getNearest(event.getTriggeringLocation().getLatitude(),
                event.getTriggeringLocation().getLongitude());

        Intent activityIntent = new Intent(context, MapsActivity.class);
        activityIntent.putExtra(ACTIVITY_INTENT_CHOOSE_ROUTE_DWELLING, true);
        activityIntent.putExtra(ACTIVITY_INTENT_ROUTE_ID, feature.id);

        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, activityIntent, 0);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_app_logo)
                .setContentTitle(context.getResources().getString(R.string.are_you_waiting_notification_title))
                .setContentText(context.getResources().getString(R.string.are_you_waiting_notification_body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setAllowSystemGeneratedContextualActions(false);

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String description = "N/A";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Dwelling Channel", importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
    }

    public void initRegisterGeoFences(Context context, ActivityModel.PointsStructure pointsStructure, double lat, double log) {

        if (context.getSharedPreferences(REGISTERED_GEOFENCES, Context.MODE_PRIVATE).contains(REQ_ID_KEY)) {
            return;
        }

        Log.e("Alaa", "Added init Geofences : " + lat + "   " + log);
        Set<ActivityModel.PointsStructure.Feature> featureSet = pointsStructure.nearestKthElements(100, lat, log);
        List<Geofence> geofenceList = featureSet.stream().map(feature -> new Geofence.Builder().setRequestId(feature.geometry.toString())
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_ENTER)
                .setLoiteringDelay(DWELLING_PERIOD_SECONDS * 1000)
                .setExpirationDuration(24 * 60 * 60 * 1000)
                .setCircularRegion(feature.geometry.coordinates[1], feature.geometry.coordinates[0], 150)
                .build()).collect(Collectors.toList());

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .addGeofences(geofenceList)
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL)
                .build();

        Intent broadcastIntent = new Intent(context, LocationChangeReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, broadcastIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            throw new IllegalStateException("Permissions should have been granted by this point!");
        }
        geofencingClient.addGeofences(geofencingRequest, pendingIntent).addOnSuccessListener((result) -> {
            Set<String> stringSet = geofenceList.stream().map((item) -> item.getRequestId()).collect(Collectors.toSet());
            Log.e("Alaa", "Added Geofences : " + stringSet);
            context.getSharedPreferences(REGISTERED_GEOFENCES, Context.MODE_PRIVATE).edit().putStringSet(REQ_ID_KEY, stringSet).commit();
        });
    }

}
