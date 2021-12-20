package com.alaa.roomdb.dao.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.alaa.roomdb.utils.InstantConverters;

import java.time.Instant;

@Entity
public class Entering {

    @NonNull
    @PrimaryKey
    public String userId;
    @NonNull
    public double latitude;
    @NonNull
    public double longitude;

    @TypeConverters(InstantConverters.class)
    public Instant instant;

}
