package com.alaa.utils;

import org.locationtech.proj4j.ProjCoordinate;

public class Geometry {


    public static double getDistance(ProjCoordinate p1, ProjCoordinate p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }
}
