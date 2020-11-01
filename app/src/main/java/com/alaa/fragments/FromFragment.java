package com.alaa.fragments;

import android.app.Application;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.alaa.transportapp.MapsActivity;
import com.alaa.transportapp.R;
import com.alaa.utils.AnimationFragment;
import com.alaa.viewmodels.ActivityModel;
import com.alaa.viewmodels.FindPathModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class FromFragment extends AnimationFragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FindPathModel viewModel;
    private Marker marker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.from_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(FindPathModel.class);

        SupportMapFragment fragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        fragment.getMapAsync(this);


        view.findViewById(R.id.arrow_back).setOnClickListener((v) -> {
            getActivity().onBackPressed();
        });

        view.findViewById(R.id.from_search).setOnClickListener((v) -> {
            ((MapsActivity) getActivity()).getSearchResult((LatLng selection, LatLngBounds viewPort) -> {
                if (selection == null) return;
                handleUpdateMap(selection, viewPort);
            });
        });

        view.findViewById(R.id.current_location).setOnClickListener((v) -> {
            ((MapsActivity) getActivity()).getCurrentLocation((LatLng loc) -> {
                if (loc == null) return;
                handleUpdateMap(loc, 15);
            });
        });

        view.findViewById(R.id.next).setOnClickListener((v) -> {
            viewModel.refresh(new ViewModelProvider(requireActivity()).get(ActivityModel.class), (Application) requireContext().getApplicationContext(), this);
            getParentFragmentManager().beginTransaction().replace(android.R.id.content, new PathResultFragment()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).commit();

        });

    }

    private void handleUpdateMap(LatLng selection, LatLngBounds viewPort) {
        if (getViewLifecycleOwner().getLifecycle().getCurrentState().name().equals(Lifecycle.State.DESTROYED))
            return;
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(viewPort, 0));
        CameraPosition pos = mMap.getCameraPosition();
        marker.setPosition(pos.target);
        viewModel.From = pos.target;

    }

    private void handleUpdateMap(LatLng selection, int zoom) {
        if (getViewLifecycleOwner().getLifecycle().getCurrentState().name().equals(Lifecycle.State.DESTROYED))
            return;
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selection, zoom));
        CameraPosition pos = mMap.getCameraPosition();
        marker.setPosition(pos.target);
        viewModel.From = pos.target;

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (mMap != null) return;
        mMap = googleMap;

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(viewModel.From
                , 15));

        marker = googleMap.addMarker(new MarkerOptions().visible(true).position(viewModel.From));

        googleMap.setOnCameraMoveListener(() -> {
            CameraPosition pos = googleMap.getCameraPosition();
            marker.setPosition(pos.target);
            viewModel.From = pos.target;

        });
    }
}
