package com.alaa.utils;

import java.text.MessageFormat;
import java.util.Calendar;

public class getTimeUtils {

    private static final char[] ENGLISH_NUMBERS = {'\u0030', '\u0031', '\u0032', '\u0033', '\u0034', '\u0035', '\u0036', '\u0037', '\u0038', '\u0039'};
    private static final char[] ARABIC_NUMBERS = {'\u0660', '\u0661', '\u0662', '\u0663', '\u0664', '\u0665', '\u0666', '\u0667', '\u0668', '\u0669'};

    public static String getTimePeriodString(int m) {

        String msg = null;
        if (m >= 0) {

            if (m >= 60) {

                msg = MessageFormat.format("{0} ساعة و {1} دقيقة", m / 60, m % 60);

            } else {

                msg = MessageFormat.format("{0} دقيقة", m);
            }
        } else {
            msg = "لا يوجد حافلات قادمة لهذا اليوم";
        }
        msg = msg.replace(ENGLISH_NUMBERS[0], ARABIC_NUMBERS[0])
                .replace(ENGLISH_NUMBERS[1], ARABIC_NUMBERS[1])
                .replace(ENGLISH_NUMBERS[2], ARABIC_NUMBERS[2])
                .replace(ENGLISH_NUMBERS[3], ARABIC_NUMBERS[3])
                .replace(ENGLISH_NUMBERS[4], ARABIC_NUMBERS[4])
                .replace(ENGLISH_NUMBERS[5], ARABIC_NUMBERS[5])
                .replace(ENGLISH_NUMBERS[6], ARABIC_NUMBERS[6])
                .replace(ENGLISH_NUMBERS[7], ARABIC_NUMBERS[7])
                .replace(ENGLISH_NUMBERS[8], ARABIC_NUMBERS[8])
                .replace(ENGLISH_NUMBERS[9], ARABIC_NUMBERS[9]);
        return msg;
    }


    public static String getTime(int h, int m) {

        int hours = h;
        String period = "صباحاً";
        if (hours >= 12) {
            hours -= 12;
            period = "مساءً";
        }
        if (m < 10) {
            return MessageFormat.format("{2} {1}:0{0} ", m, hours, period).replace(ENGLISH_NUMBERS[0], ARABIC_NUMBERS[0])
                    .replace(ENGLISH_NUMBERS[1], ARABIC_NUMBERS[1])
                    .replace(ENGLISH_NUMBERS[2], ARABIC_NUMBERS[2])
                    .replace(ENGLISH_NUMBERS[3], ARABIC_NUMBERS[3])
                    .replace(ENGLISH_NUMBERS[4], ARABIC_NUMBERS[4])
                    .replace(ENGLISH_NUMBERS[5], ARABIC_NUMBERS[5])
                    .replace(ENGLISH_NUMBERS[6], ARABIC_NUMBERS[6])
                    .replace(ENGLISH_NUMBERS[7], ARABIC_NUMBERS[7])
                    .replace(ENGLISH_NUMBERS[8], ARABIC_NUMBERS[8])
                    .replace(ENGLISH_NUMBERS[9], ARABIC_NUMBERS[9]);
        } else {
            return MessageFormat.format("{2} {1}:{0} ", m, hours, period).replace(ENGLISH_NUMBERS[0], ARABIC_NUMBERS[0])
                    .replace(ENGLISH_NUMBERS[1], ARABIC_NUMBERS[1])
                    .replace(ENGLISH_NUMBERS[2], ARABIC_NUMBERS[2])
                    .replace(ENGLISH_NUMBERS[3], ARABIC_NUMBERS[3])
                    .replace(ENGLISH_NUMBERS[4], ARABIC_NUMBERS[4])
                    .replace(ENGLISH_NUMBERS[5], ARABIC_NUMBERS[5])
                    .replace(ENGLISH_NUMBERS[6], ARABIC_NUMBERS[6])
                    .replace(ENGLISH_NUMBERS[7], ARABIC_NUMBERS[7])
                    .replace(ENGLISH_NUMBERS[8], ARABIC_NUMBERS[8])
                    .replace(ENGLISH_NUMBERS[9], ARABIC_NUMBERS[9]);
        }

    }

    private static Calendar calendar;
    private static long elapsed = -1;

    public static String getCurrentTime() {

        calendar = Calendar.getInstance();
        elapsed = -1;

        return getTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    }


    public static int[] getInstant() {

        calendar = Calendar.getInstance();
        elapsed = -1;

        return new int[]{calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)};
    }

    public static int getPeriodFromNow(int h, int m) {

        calendar = Calendar.getInstance();
        elapsed = -1;

        int hours = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);


        if (hours == 0 && h < 0) {
            hours = 24;
        }

        int dHours = h - hours;
        int dminutes = m - minutes;

        int total = dHours * 60 + dminutes;


        return total;
    }
}
