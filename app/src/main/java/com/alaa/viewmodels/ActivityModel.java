package com.alaa.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.alaa.utils.CustomHandlerForUI;
import com.alaa.utils.Geometry;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;


public class ActivityModel extends ViewModel {
    public LinkedList<CallbackWrapper> callbacks_settings;
    public LinkedList<CallbackWrapper> callbacks_permission;
    public getSearchMap callback_search;
    public MutableLiveData<PointsStructure> index;
    public Executor exe;
    public CustomHandlerForUI mainHandelr;


    {
        index = new MutableLiveData<>();
        mainHandelr = new CustomHandlerForUI();
    }

    @FunctionalInterface
    public static interface getSearchMap {
        public void onSelect(@Nullable LatLng center, @Nullable LatLngBounds viewPort);
    }

    @FunctionalInterface
    public static interface getCurrentLocationCallback {
        public void onUpdate(@Nullable LatLng center);
    }

    public static class CallbackWrapper {
        public getCurrentLocationCallback callback;
        public boolean continuous;
        public LifecycleOwner owner;
    }

    public static class PointsStructure {
        public String type;
        public Feature[] features;

        public static class Feature {
            public String type;
            public long id;
            public Geometry geometry;

            public static class Geometry {
                public String type;
                public double[] coordinates;

                @NonNull
                @Override
                public String toString() {
                    return MessageFormat.format("Latitude: {1} Longitude: {0}", coordinates[0], coordinates[1]);
                }
            }
        }


        public Set<Feature> nearestKthElements(int k, double latitude, double longitude) {

            Feature[] features = Arrays.copyOf(this.features, this.features.length);

            CRSFactory crsFactory = new CRSFactory();
            CoordinateReferenceSystem wgs84 = crsFactory.createFromName("epsg:4326");
            CoordinateReferenceSystem tmerc = crsFactory.createFromParameters(null, "+proj=tmerc +lat_0=31.977400791234 +lon_0=35.9141891981517 +k=1 +x_0=500000 +y_0=200000 +datum=WGS84 +units=m +no_defs");

            CoordinateTransformFactory coordinateTransformFactory = new CoordinateTransformFactory();
            CoordinateTransform transform = coordinateTransformFactory.createTransform(wgs84, tmerc);


            Arrays.sort(features, (item1, item2) -> {
                double error = (getDistance(item1, latitude, longitude, transform) - getDistance(item2, latitude, longitude, transform));
                if (error == 0) return 0;
                return error > 0 ? 1 : -1;
            });

            return Arrays.stream(features).limit(k).collect(Collectors.toSet());
        }


        private static double getDistance(Feature feature, double latitude, double longitude, CoordinateTransform transform) {
            ProjCoordinate coordinate = new ProjCoordinate();
            ProjCoordinate srcCoordinates = new ProjCoordinate();
            srcCoordinates.x = longitude;
            srcCoordinates.y = latitude;
            transform.transform(srcCoordinates, coordinate);

            ProjCoordinate fCoordinates = new ProjCoordinate();
            srcCoordinates.x = feature.geometry.coordinates[0];
            srcCoordinates.y = feature.geometry.coordinates[1];
            transform.transform(srcCoordinates, fCoordinates);
            return Geometry.getDistance(fCoordinates, coordinate);
        }

        @WorkerThread
        public Feature getNearest(double latitude, double longitude) {


            CRSFactory crsFactory = new CRSFactory();
            CoordinateReferenceSystem wgs84 = crsFactory.createFromName("epsg:4326");
            CoordinateReferenceSystem tmerc = crsFactory.createFromParameters(null, "+proj=tmerc +lat_0=31.977400791234 +lon_0=35.9141891981517 +k=1 +x_0=500000 +y_0=200000 +datum=WGS84 +units=m +no_defs");

            CoordinateTransformFactory coordinateTransformFactory = new CoordinateTransformFactory();
            CoordinateTransform transform = coordinateTransformFactory.createTransform(wgs84, tmerc);

            ProjCoordinate coordinate = new ProjCoordinate();
            ProjCoordinate srcCoordinates = new ProjCoordinate();
            srcCoordinates.x = longitude;
            srcCoordinates.y = latitude;
            transform.transform(srcCoordinates, coordinate);

            Feature min = null;
            double minDistance = Double.MAX_VALUE;
            for (int i = 0; i < features.length; i++) {

                ProjCoordinate fCoordinates = new ProjCoordinate();
                srcCoordinates.x = features[i].geometry.coordinates[0];
                srcCoordinates.y = features[i].geometry.coordinates[1];
                transform.transform(srcCoordinates, fCoordinates);
                double distance = Geometry.getDistance(fCoordinates, coordinate);
                if (distance < minDistance) {
                    min = features[i];
                    minDistance = distance;
                }
            }
            return min;
        }
    }
}