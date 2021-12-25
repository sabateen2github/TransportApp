package com.alaa.roomdb.dao.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.alaa.roomdb.utils.InstantConverters;

import java.time.Instant;

@Entity
public class Notification {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String userId;

    @NonNull
    public double latitude;
    @NonNull
    public double longitude;
    @NonNull
    public String routeId;

    @NonNull
    @TypeConverters(InstantConverters.class)
    public Instant instant;
}
