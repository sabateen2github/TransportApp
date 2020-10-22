package com.alaa.utils;

import java.text.MessageFormat;

public class getTimeString {


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
}
