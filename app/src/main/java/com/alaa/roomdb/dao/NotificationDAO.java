package com.alaa.roomdb.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.alaa.roomdb.dao.entities.Notification;

import java.util.List;

@Dao
public interface NotificationDAO {

    @Query("SELECT * from Notification")
    public List<Notification> getAll();

    @Insert
    public void insert(Notification... notifications);

    @Delete
    public void delete(Notification... notifications);
}
