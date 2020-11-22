package com.alaa.utils;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.alaa.transportapp.MapsActivity;
import com.alaa.transportapp.R;
import com.alaa.viewmodels.ActivityModel;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class MarkerUtils {


    public static void addMarker(LifecycleOwner owner, ActivityModel activityModel, GoogleMap map, Activity activity) {


        SensorManager sensorManager;
        final float[] accelerometerReading = new float[3];
        final float[] magnetometerReading = new float[3];

        final float[] rotationMatrix = new float[9];
        final float[] orientationAngles = new float[3];
        final com.google.android.gms.maps.model.Marker[] currentLocation = new com.google.android.gms.maps.model.Marker[1];
        class SensorListener implements SensorEventListener {

            CompassAnimator compassAnimator;

            @Override
            public void onSensorChanged(SensorEvent event) {

                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, accelerometerReading,
                            0, accelerometerReading.length);
                } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, magnetometerReading,
                            0, magnetometerReading.length);
                }


                // Update rotation matrix, which is needed to update orientation angles.
                SensorManager.getRotationMatrix(rotationMatrix, null,
                        accelerometerReading, magnetometerReading);
                // "rotationMatrix" now has up-to-date information.
                SensorManager.getOrientation(rotationMatrix, orientationAngles);
                // "orientationAngles" now has up-to-date information.


                // "orientationAngles" now has up-to-date information.

                float angle = (float) ((orientationAngles[0]) / (2 * Math.PI) * 360);
                if (currentLocation[0] != null) {
                    CameraPosition cameraPosition = map.getCameraPosition();
                    if (compassAnimator == null) {
                        compassAnimator = new CompassAnimator(currentLocation[0], activityModel, owner);
                        compassAnimator.setPhysical(CompassAnimator.INERTIA_MOMENT_DEFAULT, CompassAnimator.ALPHA_DEFAULT, 1500);
                        compassAnimator.rotationUpdate(angle - cameraPosition.bearing, false);
                        compassAnimator.start();
                    } else {
                        compassAnimator.rotationUpdate(angle - cameraPosition.bearing, true);
                    }
                }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        }


        SensorListener listener = new SensorListener();


        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(listener, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }

        owner.getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (source.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
                    sensorManager.unregisterListener(listener);
                    if (currentLocation[0] != null)
                        currentLocation[0].remove();
                }
            }
        });

        ((MapsActivity) activity).getCurrentLocation((loc) -> {

            Log.e("Alaa", "Current location");


            if (currentLocation[0] == null) {
                currentLocation[0] = map.addMarker(new MarkerOptions().position(loc).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_compass)).anchor(0.5f, 0.5f));
            }
            currentLocation[0].setPosition(loc);

        }, true, owner);
    }


    static class CompassAnimator {


        Marker mMarker;
        ActivityModel mActivityModel;
        LifecycleOwner mLifecycleOwner;

        public CompassAnimator(Marker marker, ActivityModel activityModel, LifecycleOwner lifecycleOwner) {
            mMarker = marker;
            mActivityModel = activityModel;
            mLifecycleOwner = lifecycleOwner;

        }

        public void start() {
            ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    onAnimate();
                }
            });
            animator.start();
            mLifecycleOwner.getLifecycle().addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                    if (source.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
                        animator.cancel();
                    }
                }
            });
        }


        static final public float TIME_DELTA_THRESHOLD = 0.25f; // maximum time difference between iterations, s
        static final public float ANGLE_DELTA_THRESHOLD = 0.1f; // minimum rotation change to be redrawn, deg

        static final public float INERTIA_MOMENT_DEFAULT = 0.1f;    // default physical properties
        static final public float ALPHA_DEFAULT = 10;
        static final public float MB_DEFAULT = 1000;

        long time1, time2;              // timestamps of previous iterations--used in numerical integration
        float angle1, angle2, angle0;   // angles of previous iterations
        float angleLastDrawn;           // last drawn anglular position
        boolean animationOn = false;    // if animation should be performed

        float inertiaMoment = INERTIA_MOMENT_DEFAULT;   // moment of inertia
        float alpha = ALPHA_DEFAULT;    // damping coefficient
        float mB = MB_DEFAULT;  // magnetic field coefficient


        public void onAnimate() {
            if (animationOn) {
                if (angleRecalculate(System.currentTimeMillis())) {
                    mMarker.setRotation(angle1);
                }
            } else {
                mMarker.setRotation(angle1);
            }

        }

        /**
         * Use this to set physical properties.
         * Negative values will be replaced by default values
         *
         * @param inertiaMoment Moment of inertia (default 0.1)
         * @param alpha         Damping coefficient (default 10)
         * @param mB            Magnetic field coefficient (default 1000)
         */
        public void setPhysical(float inertiaMoment, float alpha, float mB) {
            this.inertiaMoment = inertiaMoment >= 0 ? inertiaMoment : this.INERTIA_MOMENT_DEFAULT;
            this.alpha = alpha >= 0 ? alpha : ALPHA_DEFAULT;
            this.mB = mB >= 0 ? mB : MB_DEFAULT;
        }


        /**
         * Use this to set new "magnetic field" angle at which image should rotate
         *
         * @param angleNew new magnetic field angle, deg., relative to vertical axis.
         * @param animate  true, if image shoud rotate using animation, false to set new rotation instantly
         */
        public void rotationUpdate(final float angleNew, final boolean animate) {
            if (animate) {
                if (Math.abs(angle0 - angleNew) > ANGLE_DELTA_THRESHOLD) {
                    angle0 = angleNew;
                }
                animationOn = true;
            } else {
                angle1 = angleNew;
                angle2 = angleNew;
                angle0 = angleNew;
                angleLastDrawn = angleNew;
                onAnimate();
                animationOn = false;
            }
        }

        /**
         * Recalculate angles using equation of dipole circular motion
         *
         * @param timeNew timestamp of method invoke
         * @return if there is a need to redraw rotation
         */
        protected boolean angleRecalculate(final long timeNew) {

            // recalculate angle using simple numerical integration of motion equation
            float deltaT1 = (timeNew - time1) / 1000f;
            if (deltaT1 == 0) {
                return false;
            }
            if (deltaT1 > TIME_DELTA_THRESHOLD) {
                deltaT1 = TIME_DELTA_THRESHOLD;
                time1 = timeNew + Math.round(TIME_DELTA_THRESHOLD * 1000);
            }
            float deltaT2 = (time1 - time2) / 1000f;
            if (deltaT2 > TIME_DELTA_THRESHOLD) {
                deltaT2 = TIME_DELTA_THRESHOLD;
            }
            if (deltaT2 == 0) {
                return false;
            }

            // circular acceleration coefficient
            float koefI = inertiaMoment / deltaT1 / deltaT2;

            // circular velocity coefficient
            float koefAlpha = alpha / deltaT1;

            // angular momentum coefficient
            float koefk = mB * (float) (Math.sin(Math.toRadians(angle0)) * Math.cos(Math.toRadians(angle1)) -
                    (Math.sin(Math.toRadians(angle1)) * Math.cos(Math.toRadians(angle0))));

            float angleNew = (koefI * (angle1 * 2f - angle2) + koefAlpha * angle1 + koefk) / (koefI + koefAlpha);

            // reassign previous iteration variables
            angle2 = angle1;
            angle1 = angleNew;
            time2 = time1;
            time1 = timeNew;

            // if angles changed less then threshold, return false - no need to redraw the view
            if (Math.abs(angleLastDrawn - angle1) < ANGLE_DELTA_THRESHOLD) {
                return false;
            } else {
                angleLastDrawn = angle1;
                return true;
            }
        }

    }
}
