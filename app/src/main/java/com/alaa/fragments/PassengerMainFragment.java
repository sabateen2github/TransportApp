package com.alaa.fragments;


import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.alaa.transportapp.MapsActivity;
import com.alaa.transportapp.R;
import com.alaa.utils.AnimationFragment;
import com.alaa.viewmodels.ActivityModel;
import com.alaa.viewmodels.PassengerState;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.List;

public class PassengerMainFragment extends AnimationFragment implements OnMapReadyCallback {


    private PassengerState stateModel;
    private GoogleMap mMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.passenger_main_layout, container, false);
    }

    private void updateBusStopButtton(View view, PassengerState.State state) {


        AppCompatButton selectedBusStop = view.findViewById(R.id.passenger_show_schedule);


        if (state.selectedBusStop == null) {
            selectedBusStop.setText(R.string.bus_stop_deselected);
            selectedBusStop.setTextColor(getResources().getColor(R.color.black, getActivity().getTheme()));
            selectedBusStop.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white, getActivity().getTheme())));
            selectedBusStop.setOnClickListener(null);
        } else {
            selectedBusStop.setText(R.string.bus_stop_selected);
            selectedBusStop.setTextColor(getResources().getColor(R.color.white, getActivity().getTheme()));
            selectedBusStop.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF4CAF50")));
            selectedBusStop.setOnClickListener(v -> {
                //show schedule of the selected busStop
                getParentFragmentManager().beginTransaction().replace(android.R.id.content, new BusStopFragment()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).commit();
            });
        }


    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {


        long elapes = System.currentTimeMillis();

        ViewModelProvider provider = new ViewModelProvider(requireActivity());
        stateModel = provider.get(PassengerState.class);
        PassengerState.State state = stateModel.state.getValue();


        AppCompatImageView arrowBack = view.findViewById(R.id.passenger_back);
        arrowBack.setOnClickListener(v -> {
            getActivity().onBackPressed();
        });

        updateBusStopButtton(view, state);
        View goToMyLocation = view.findViewById(R.id.passenger_current_bus_stop);

        goToMyLocation.setOnClickListener(v -> {

            if (!stateModel.state.getValue().is_getting_location_done) {
                return;
            }

            stateModel.state.getValue().is_getting_location_done = false;

            ((MapsActivity) getActivity()).getCurrentLocation((LatLng loc) -> {

                final ActivityModel model = new ViewModelProvider(getActivity()).get(ActivityModel.class);
                model.exe.execute(() -> {

                    ActivityModel.PointsStructure.Feature feature = model.index.getValue().getNearest(loc.latitude, loc.longitude);
                    LatLng busStop_loc = new LatLng(feature.geometry.coordinates[1], feature.geometry.coordinates[0]);
                    model.mainHandelr.post(this, () -> {
                        stateModel.setCurrentLocation(loc);
                        stateModel.setMapCenter(busStop_loc);
                        stateModel.setMapZoom(16);
                        stateModel.state.getValue().is_getting_location_done = true;
                        stateModel.update();

                    });
                });

            });

        });


        View searchView = view.findViewById(R.id.passenger_search);
        searchView.setOnClickListener((v) -> {
            stateModel.queryResult(requireActivity());
        });

        SupportMapFragment fragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.passenger_map);
        fragment.getMapAsync(this);


        stateModel.state.observe(getViewLifecycleOwner(), (newState) -> {
            if (mMap != null) {

                updateBusStopButtton(getView(), state);

                if (stateModel.state.getValue().searchViewPort == null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stateModel.state.getValue().mapCenter, stateModel.state.getValue().mapZoom));
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(stateModel.state.getValue().searchViewPort, 0));
                    CameraPosition pos = mMap.getCameraPosition();
                    stateModel.state.getValue().mapCenter = pos.target;
                    stateModel.state.getValue().mapZoom = pos.zoom;
                    stateModel.state.getValue().searchViewPort = null;
                }
            }
        });


    }

    private void generateMarkers(ActivityModel model) {

        model.exe.execute(() -> {
            List<MarkerItem> items = new ArrayList<>();
            for (ActivityModel.PointsStructure.Feature feature : model.index.getValue().features) {
                MarkerItem mItem = new MarkerItem(feature);
                items.add(mItem);
            }
            stateModel.markers.postValue(items);
        });
    }


    private void continueBuidlingMarkerCluster() {

        ClusterManager<MarkerItem> manager = new ClusterManager<>(getActivity(), mMap);
        manager.setOnClusterItemInfoWindowClickListener((item) -> {
            //show schedule of the selected busStop
            getParentFragmentManager().beginTransaction().replace(android.R.id.content, new BusStopFragment()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).commit();
        });
        mMap.setOnCameraIdleListener(manager);
        mMap.setOnMarkerClickListener(manager);
        manager.setAnimation(true);
        manager.addItems(stateModel.markers.getValue());
        manager.setOnClusterItemClickListener((item) -> {
            stateModel.state.getValue().selectedBusStop = item.mFeature;
            stateModel.update();
            return false;
        });
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                stateModel.state.getValue().selectedBusStop = null;
                stateModel.update();
            }
        });
        manager.getAlgorithm().setMaxDistanceBetweenClusteredItems(300);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        if (mMap != null) return;

        mMap = googleMap;
        mMap.setOnCameraMoveListener(() -> {
            CameraPosition pos = mMap.getCameraPosition();
            stateModel.state.getValue().mapZoom = pos.zoom;
            stateModel.state.getValue().mapCenter = pos.target;
        });
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stateModel.state.getValue().mapCenter, stateModel.state.getValue().mapZoom));
        final ActivityModel model = new ViewModelProvider(getActivity()).get(ActivityModel.class);

        if (model.index.getValue() == null) {
            model.index.observe(getViewLifecycleOwner(), (item) -> {
                generateMarkers(model);
            });
        } else if (stateModel.markers.getValue() == null) {
            generateMarkers(model);
        }

        if (stateModel.markers.getValue() != null) {
            continueBuidlingMarkerCluster();
        } else {
            stateModel.markers.observe(getViewLifecycleOwner(), (item) -> {
                continueBuidlingMarkerCluster();
            });
        }
    }


    public static class MarkerItem implements ClusterItem {

        private ActivityModel.PointsStructure.Feature mFeature;

        public MarkerItem(ActivityModel.PointsStructure.Feature feature) {
            mFeature = feature;
        }

        public ActivityModel.PointsStructure.Feature getFeature() {
            return mFeature;
        }

        @NonNull
        @Override
        public LatLng getPosition() {
            return new LatLng(mFeature.geometry.coordinates[1], mFeature.geometry.coordinates[0]);
        }

        @Nullable
        @Override
        public String getTitle() {
            return "نقطة انتظار ";
        }

        @Nullable
        @Override
        public String getSnippet() {
            return "اضغط لعرض جدول الرحلات";
        }
    }
}
