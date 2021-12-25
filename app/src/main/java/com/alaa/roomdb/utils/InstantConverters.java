package com.alaa.roomdb.utils;

import androidx.room.TypeConverter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class InstantConverters {

    @TypeConverter
    public static Instant getInstant(String instantS) {
        return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(instantS));
    }

    @TypeConverter
    public static String serializeInstant(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

}
