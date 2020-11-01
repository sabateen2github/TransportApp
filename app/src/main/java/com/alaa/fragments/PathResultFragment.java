package com.alaa.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.TransitionManager;

import com.alaa.transportapp.R;
import com.alaa.utils.AnimationFragment;
import com.alaa.viewmodels.FindPathModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;


public class PathResultFragment extends AnimationFragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FindPathModel viewModel;
    private boolean mPendingUpdateBounds;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.path_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(FindPathModel.class);
        SupportMapFragment fragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        fragment.getMapAsync(this);


        view.findViewById(R.id.arrow_back).setOnClickListener((v) -> {
            getActivity().onBackPressed();
        });

        View laodingContainer = view.findViewById(R.id.loading_container);


        viewModel.steps.observe(getViewLifecycleOwner(), (item) -> {
            if (item == null) return;
            if (item.size() == 0) {
                getActivity().onBackPressed();
                return;
            }

            if (mMap != null) {
                updateMapBasedOnResult();
            } else {
                mPendingUpdateBounds = true;

            }
            TransitionManager.beginDelayedTransition((ViewGroup) view);
            laodingContainer.setVisibility(View.GONE);


        });

    }

    private void updateMapBasedOnResult() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(viewModel.Bounds, (int) (150 * getResources().getDisplayMetrics().density)));
        List<FindPathModel.StepSteroid> features = viewModel.steps.getValue();
        for (FindPathModel.StepSteroid feature : features) {
            mMap.addMarker(new MarkerOptions().position(new LatLng(feature.feature.geometry.coordinates[1], feature.feature.geometry.coordinates[0])));
        }
        mMap.addMarker(new MarkerOptions().position(viewModel.From).alpha(0.5f).title("بداية"));
        mMap.addMarker(new MarkerOptions().position(viewModel.To).alpha(0.5f).title("نهاية"));


    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (mMap != null) return;
        mMap = googleMap;
        if (mPendingUpdateBounds) {
            updateMapBasedOnResult();
            mPendingUpdateBounds = false;
        }
    }
}
