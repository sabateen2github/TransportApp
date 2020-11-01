package com.alaa.utils;

import com.alaa.viewmodels.ActivityModel;

import java.text.MessageFormat;
import java.util.Calendar;

public class getTimeUtils {


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
            return MessageFormat.format("{2} {1}:0{0} ", m, hours, period);
        } else {
            return MessageFormat.format("{2} {1}:{0} ", m, hours, period);
        }

    }

    private static Calendar calendar;
    private static long elapsed = -1;

    public static String getCurrentTime() {

        if (ActivityModel.isSimulation) {
            if (elapsed == -1) {
                elapsed = System.currentTimeMillis();
                calendar = Calendar.getInstance();
            } else {
                calendar.add(Calendar.MILLISECOND, (int) (System.currentTimeMillis() - elapsed) * 60);
                elapsed = System.currentTimeMillis();
            }
        } else {
            calendar = Calendar.getInstance();
            elapsed = -1;
        }

        return getTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    }


    public static int[] getInstant() {
        if (ActivityModel.isSimulation) {
            if (elapsed == -1) {
                elapsed = System.currentTimeMillis();
                calendar = Calendar.getInstance();
            } else {
                calendar.add(Calendar.MILLISECOND, (int) (System.currentTimeMillis() - elapsed) * 60);
                elapsed = System.currentTimeMillis();
            }
        } else {
            calendar = Calendar.getInstance();
            elapsed = -1;
        }
        return new int[]{calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)};
    }

    public static int getPeriodFromNow(int h, int m) {
        if (ActivityModel.isSimulation) {
            if (elapsed == -1) {
                elapsed = System.currentTimeMillis();
                calendar = Calendar.getInstance();
            } else {
                calendar.add(Calendar.MILLISECOND, (int) (System.currentTimeMillis() - elapsed) * 60);
                elapsed = System.currentTimeMillis();
            }
        } else {
            calendar = Calendar.getInstance();
            elapsed = -1;
        }
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
