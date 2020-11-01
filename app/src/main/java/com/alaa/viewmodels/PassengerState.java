package com.alaa.viewmodels;

import android.app.Activity;
import android.app.Application;

import androidx.lifecycle.MutableLiveData;

import com.alaa.fragments.PassengerMainFragment;
import com.alaa.transportapp.MapsActivity;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;

public class PassengerState extends androidx.lifecycle.AndroidViewModel {

    public MutableLiveData<State> state;
    public MutableLiveData<List<PassengerMainFragment.MarkerItem>> markers;

    public PassengerState(Application context) {
        super(context);
        markers = new MutableLiveData<>();
        state = new MutableLiveData<State>();
        state.setValue(new State());
        state.getValue().selectedBusStop = null;
        state.getValue().currentLocation = new LatLng(31.97164183082986d, 35.833652222827524d);
        state.getValue().mapZoom = 15;
        state.getValue().mapCenter = new LatLng(31.97164183082986d, 35.833652222827524d);
        state.getValue().init = true;
        update();
    }

    public void setCurrentLocation(LatLng location) {
        state.getValue().currentLocation = location;
    }

    public void sendRequest(ActivityModel.PointsStructure.Feature feature, String route_id) {
        state.getValue().reqFeature = feature;
        state.getValue().reqRoute = route_id;
    }

    public void removeRequest() {
        state.getValue().reqFeature = null;
        state.getValue().reqRoute = null;
    }

    public void setMapCenter(LatLng center) {
        state.getValue().mapCenter = center;
    }

    public void setMapZoom(int zoom) {
        state.getValue().mapZoom = zoom;
    }

    public void setSelectedBusStop(ActivityModel.PointsStructure.Feature object) {
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


    public class State {
        public LatLng currentLocation;
        public LatLng mapCenter;
        public LatLng searchResult;
        public LatLngBounds searchViewPort;
        public float mapZoom;
        public ActivityModel.PointsStructure.Feature selectedBusStop;
        public boolean init = false;
        public boolean is_getting_location_done = true;
        public ActivityModel.PointsStructure.Feature reqFeature;
        public String reqRoute;
    }


}

