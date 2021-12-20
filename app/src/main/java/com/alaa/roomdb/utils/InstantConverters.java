package com.alaa.roomdb.utils;

import androidx.room.TypeConverter;

import java.time.Instant;
import java.time.LocalDateTime;

public class InstantConverters {

    @TypeConverter
    public static Instant getInstant(String instantS) {
        return Instant.from(LocalDateTime.parse(instantS));
    }

    @TypeConverter
    public static String serializeInstant(Instant instant) {
        return instant.toString();
    }

}
