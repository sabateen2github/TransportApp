package com.alaa.fragments;

import android.app.Activity;
import android.app.Application;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.alaa.transportapp.MapsActivity;
import com.alaa.transportapp.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.List;

public class PassengerMainFragment extends Fragment implements OnMapReadyCallback {


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
            selectedBusStop.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.silver, getActivity().getTheme())));
            selectedBusStop.setOnClickListener(null);
        } else {
            selectedBusStop.setText(R.string.bus_stop_selected);
            selectedBusStop.setTextColor(getResources().getColor(R.color.white, getActivity().getTheme()));
            selectedBusStop.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.teal_700, getActivity().getTheme())));
            selectedBusStop.setOnClickListener(v -> {
                //show schedule of the selected busStop
                getParentFragmentManager().beginTransaction().replace(android.R.id.content, new BusStopFragment()).addToBackStack(null).commit();
            });
        }


    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        stateModel = new ViewModelProvider(requireActivity()).get(PassengerState.class);
        PassengerState.State state = stateModel.state.getValue();

        if (state.reqSent) {
            view.findViewById(R.id.passenger_request_text).setVisibility(View.VISIBLE);
            ((AppCompatButton) view.findViewById(R.id.passenger_action_button)).setText(R.string.passenger_action_text_req_sent);
        } else {
            view.findViewById(R.id.passenger_request_text).setVisibility(View.GONE);
            ((AppCompatButton) view.findViewById(R.id.passenger_action_button)).setText(R.string.passenger_action_text_req_unsent);
        }

        ((AppCompatButton) view.findViewById(R.id.passenger_action_button)).setOnClickListener(v -> {
            //show related data
            if (state.reqSent) {

            } else {

            }
        });

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

                final MapsActivity.ActivityModel model = new ViewModelProvider(getActivity()).get(MapsActivity.ActivityModel.class);
                model.exe.execute(() -> {

                    MapsActivity.PointsStructure.Feature feature = model.index.getValue().getNearest(loc.latitude, loc.longitude);
                    LatLng busStop_loc = new LatLng(feature.geometry.coordinates[1], feature.geometry.coordinates[0]);
                    model.mainHandelr.post(() -> {
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

    private void generateMarkers(MapsActivity.ActivityModel model) {

        model.exe.execute(() -> {
            List<MarkerItem> items = new ArrayList<>();
            for (MapsActivity.PointsStructure.Feature feature : model.index.getValue().features) {
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
            getParentFragmentManager().beginTransaction().replace(android.R.id.content, new BusStopFragment()).addToBackStack(null).commit();
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
        final MapsActivity.ActivityModel model = new ViewModelProvider(getActivity()).get(MapsActivity.ActivityModel.class);

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

    public static class PassengerState extends androidx.lifecycle.AndroidViewModel {

        MutableLiveData<State> state;
        MutableLiveData<List<MarkerItem>> markers;

        public PassengerState(Application context) {
            super(context);
            markers = new MutableLiveData<>();
            state = new MutableLiveData<State>();
            state.setValue(new State());
            state.getValue().selectedBusStop = null;
            state.getValue().reqSent = false;
            state.getValue().currentLocation = new LatLng(31.97164183082986d, 35.833652222827524d);
            state.getValue().mapZoom = 15;
            state.getValue().mapCenter = new LatLng(31.97164183082986d, 35.833652222827524d);
            state.getValue().init = true;
            update();
        }

        public void setCurrentLocation(LatLng location) {
            state.getValue().currentLocation = location;
        }

        public void sendRequest(Object dontKnow) {
            state.getValue().reqSent = true;
        }

        public void setMapCenter(LatLng center) {
            state.getValue().mapCenter = center;
        }

        public void setMapZoom(int zoom) {
            state.getValue().mapZoom = zoom;
        }

        public void setSelectedBusStop(MapsActivity.PointsStructure.Feature object) {
            state.getValue().selectedBusStop = object;
        }

        private void setSearchResult(LatLng result, LatLngBounds viewPort) {
            state.getValue().searchResult = result;
            state.getValue().searchViewPort = viewPort;
        }

        public void queryResult(Activity activity) {
            ((MapsActivity) activity).getSearchResult((LatLng selection, LatLngBounds viewPort) -> {
                if (selection == null) return;
                setSearchResult(selection, viewPort);
                update();
            });
        }

        public void update() {
            state.setValue(state.getValue());
        }


        class State {
            LatLng currentLocation;
            boolean reqSent = false;
            LatLng mapCenter;
            LatLng searchResult;
            LatLngBounds searchViewPort;
            float mapZoom;
            MapsActivity.PointsStructure.Feature selectedBusStop;
            boolean init = false;
            boolean is_getting_location_done = true;

        }


    }


    public static class MarkerItem implements ClusterItem {

        private MapsActivity.PointsStructure.Feature mFeature;

        public MarkerItem(MapsActivity.PointsStructure.Feature feature) {
            mFeature = feature;
        }

        public MapsActivity.PointsStructure.Feature getFeature() {
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
