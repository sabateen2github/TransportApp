package com.alaa.roomdb.dao.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.Instant;

@Entity
public class Notification {

    @PrimaryKey
    public String userId;

    public double latitude;
    public double longitude;
    public String routeId;
    public Instant instant;
}
