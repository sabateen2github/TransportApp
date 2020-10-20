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
import androidx.lifecycle.LiveData;
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

import org.json.JSONObject;

public class PassengerMainFragment extends Fragment implements OnMapReadyCallback {


    private PassengerState stateModel;
    private GoogleMap mMap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.passenger_main_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        stateModel = new ViewModelProvider(requireActivity()).get(PassengerState.class);
        PassengerState.State state = stateModel.getState();

        if (state.reqSent) {
            view.findViewById(R.id.passenger_request_text).setVisibility(View.VISIBLE);
            ((AppCompatButton) view.findViewById(R.id.passenger_action_button)).setText(R.string.passenger_action_text_req_sent);

        } else {
            view.findViewById(R.id.passenger_request_text).setVisibility(View.GONE);
            ((AppCompatButton) view.findViewById(R.id.passenger_action_button)).setText(R.string.passenger_action_text_req_unsent);
        }

        ((AppCompatButton) view.findViewById(R.id.passenger_action_button)).setOnClickListener(v -> {
            //show related data
        });

        AppCompatImageView arrowBack = view.findViewById(R.id.passenger_back);
        arrowBack.setOnClickListener(v -> {
            getActivity().onBackPressed();
        });


        AppCompatButton selectedBusStop = view.findViewById(R.id.passenger_show_schedule);

        if (state.selectedBusStop == null) {
            selectedBusStop.setText(R.string.bus_stop_deselected);
            selectedBusStop.setClickable(false);
            selectedBusStop.setEnabled(false);
            selectedBusStop.setTextColor(getResources().getColor(R.color.black, getActivity().getTheme()));
            selectedBusStop.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.silver, getActivity().getTheme())));
            selectedBusStop.setOnClickListener(null);
        } else {
            selectedBusStop.setText(R.string.bus_stop_selected);
            selectedBusStop.setClickable(true);
            selectedBusStop.setEnabled(true);
            selectedBusStop.setTextColor(getResources().getColor(R.color.white, getActivity().getTheme()));
            selectedBusStop.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.teal_700, getActivity().getTheme())));
            selectedBusStop.setOnClickListener(v -> {
                //show schedule of the selected busStop
            });
        }

        View goToMyLocation = view.findViewById(R.id.passenger_current_bus_stop);

        goToMyLocation.setOnClickListener(v -> {

            if (!stateModel.getState().is_getting_location_done) {
                return;
            }

            stateModel.getState().is_getting_location_done = false;

            ((MapsActivity) getActivity()).getCurrentLocation((LatLng loc) -> {

                final MapsActivity.ActivityModel model = new ViewModelProvider(getActivity()).get(MapsActivity.ActivityModel.class);
                model.exe.execute(() -> {

                    MapsActivity.PointsStructure.Feature feature = model.index.getValue().getNearest(loc.latitude, loc.longitude);
                    LatLng busStop_loc = new LatLng(feature.geometry.coordinates[1], feature.geometry.coordinates[0]);
                    model.mainHandelr.post(() -> {
                        stateModel.setCurrentLocation(loc);
                        stateModel.setMapCenter(busStop_loc);
                        stateModel.setMapZoom(16);
                        stateModel.getState().is_getting_location_done = true;
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


        stateModel.getStateMonitor().observe(requireActivity(), newState -> {
            if (mMap != null) {

                if (stateModel.state.getValue().searchViewPort == null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stateModel.getState().mapCenter, stateModel.getState().mapZoom));
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(stateModel.getState().searchViewPort, 0));
                    CameraPosition pos = mMap.getCameraPosition();
                    stateModel.getState().mapCenter = pos.target;
                    stateModel.getState().mapZoom = pos.zoom;
                    stateModel.getState().searchViewPort = null;

                }

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraMoveListener(() -> {
            CameraPosition pos = mMap.getCameraPosition();
            stateModel.getState().mapZoom = pos.zoom;
            stateModel.getState().mapCenter = pos.target;
        });
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stateModel.getState().mapCenter, stateModel.getState().mapZoom));
    }

    public static class PassengerState extends androidx.lifecycle.AndroidViewModel {

        private MutableLiveData<State> state;

        public PassengerState(Application context) {
            super(context);
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

        public State getState() {
            return state.getValue();
        }

        public LiveData<State> getStateMonitor() {
            return state;
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

        public void setSelectedBusStop(JSONObject object) {
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
            JSONObject selectedBusStop;
            boolean init = false;
            boolean is_getting_location_done = true;

        }


    }
}
