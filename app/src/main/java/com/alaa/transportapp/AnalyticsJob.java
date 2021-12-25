package com.alaa.transportapp;

import static com.alaa.transportapp.GeoEventsHelper.EVENTS_DB;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.alaa.roomdb.EventsDB;
import com.alaa.roomdb.dao.entities.Entering;
import com.alaa.roomdb.dao.entities.Notification;
import com.transportapp.apis.NotificationsApi;
import com.transportapp.apis.PassengersApi;
import com.transportapp.models.LongtermRequestBody;
import com.transportapp.models.NotificationRequestBody;

import java.time.OffsetDateTime;
import java.util.List;

public class AnalyticsJob extends Worker {
    private EventsDB appDatabase;
    private PassengersApi passengersApi;
    private NotificationsApi notificationsApi;

    public AnalyticsJob(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        appDatabase = Room.databaseBuilder(context, EventsDB.class, EVENTS_DB).build();
        passengersApi = new PassengersApi();
        notificationsApi = new NotificationsApi();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.e("Alaa","Starting Background Job");
        sendEntering();
        sendNotifications();
        Log.e("Alaa","Finished Background Job");

        return Result.success();
    }

    private void sendEntering() {

        List<Entering> entering = appDatabase.getEnteringDAO().getAll();

        entering.stream().forEachOrdered((ent) -> {
            LongtermRequestBody longtermRequestBody = new LongtermRequestBody(ent.longitude, ent.latitude, OffsetDateTime.from(ent.instant), ent.userId);
            try {
                passengersApi.collectPassengerData(longtermRequestBody);
                appDatabase.getEnteringDAO().delete(ent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    private void sendNotifications() {

        List<Notification> notifications = appDatabase.getNotificationDAO().getAll();

        notifications.stream().forEachOrdered((ent) -> {
            NotificationRequestBody longtermRequestBody = new NotificationRequestBody(ent.longitude, ent.latitude, OffsetDateTime.from(ent.instant), ent.userId, ent.routeId);
            try {
                notificationsApi.collectNotificationsData(longtermRequestBody);
                appDatabase.getNotificationDAO().delete(ent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }
}
