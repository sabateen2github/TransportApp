package com.alaa.roomdb;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.alaa.roomdb.dao.EnteringDAO;
import com.alaa.roomdb.dao.NotificationDAO;
import com.alaa.roomdb.dao.entities.Entering;
import com.alaa.roomdb.dao.entities.Notification;

@Database(entities = {Entering.class, Notification.class}, version = 1)
public abstract class EventsDB extends RoomDatabase {
    public abstract EnteringDAO getEnteringDAO();

    public abstract NotificationDAO getNotificationDAO();
}
