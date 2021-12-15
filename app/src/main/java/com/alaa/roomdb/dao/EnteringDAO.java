package com.alaa.roomdb.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.alaa.roomdb.dao.entities.Entering;

import java.util.List;

@Dao
public interface EnteringDAO {

    @Query("SELECT * from Entering")
    public List<Entering> getAll();

    @Insert
    public void insert(Entering... entering);

    @Delete
    public void delete(Entering... entering);

}
