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
import com.alaa.utils.MarkerUtils;
import com.alaa.viewmodels.ActivityModel;
import com.alaa.viewmodels.FindPathModel;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;


public class PathResultFragment extends AnimationFragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FindPathModel viewModel;
    private boolean mPendingUpdate;
    private List<Marker> mMarkers;
    private ActivityModel activityModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.path_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        ViewModelProvider provider = new ViewModelProvider(requireActivity());
        viewModel = provider.get(FindPathModel.class);
        activityModel = provider.get(ActivityModel.class);
        SupportMapFragment fragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        fragment.getMapAsync(this);
        mMarkers = new ArrayList<>();
        view.findViewById(R.id.arrow_back).setOnClickListener((v) -> {
            getActivity().onBackPressed();
        });

        View laodingContainer = view.findViewById(R.id.loading_container);

        view.findViewById(R.id.find_path_previous).setOnClickListener((v) -> {
            previousClicked();
        });

        view.findViewById(R.id.find_path_next).setOnClickListener((v) -> {
            nextClicked();
        });


        viewModel.steps.observe(getViewLifecycleOwner(), (item) -> {
            if (item == null) return;
            if (item.size() == 0) {
                getActivity().onBackPressed();
                return;
            }

            if (mMap != null) {
                updateMapBasedOnResult();
                updateInfoWindow();
            } else {
                mPendingUpdate = true;

            }
            TransitionManager.beginDelayedTransition((ViewGroup) view);
            laodingContainer.setVisibility(View.GONE);
            view.findViewById(R.id.steps_navigator_container).setVisibility(View.VISIBLE);
            if (viewModel.current_step == 0) {
                getView().findViewById(R.id.find_path_previous).setEnabled(false);
            } else if (viewModel.current_step == mMarkers.size() - 1) {
                getView().findViewById(R.id.find_path_next).setEnabled(false);
            }
        });
    }

    private void nextClicked() {
        if (viewModel.current_step == 0) {
            getView().findViewById(R.id.find_path_previous).setEnabled(true);
        } else if (viewModel.current_step == mMarkers.size() - 2) {
            getView().findViewById(R.id.find_path_next).setEnabled(false);
        }
        viewModel.current_step++;
        updateInfoWindow();
        updateCamera(true);
    }

    private void previousClicked() {

        if (viewModel.current_step == 1) {
            getView().findViewById(R.id.find_path_previous).setEnabled(false);

        } else if (viewModel.current_step == mMarkers.size() - 1) {
            getView().findViewById(R.id.find_path_next).setEnabled(true);
        }
        viewModel.current_step--;
        updateInfoWindow();
        updateCamera(true);
    }

    private void updateInfoWindow() {

        mMarkers.get(viewModel.current_step).showInfoWindow();
    }

    private void updateCamera(boolean animate) {
        CameraUpdate update = null;
        if (viewModel.current_step == 0) {
            update = CameraUpdateFactory.newLatLngZoom(viewModel.From, 16);
        } else if (viewModel.current_step == mMarkers.size() - 1) {
            update = CameraUpdateFactory.newLatLngZoom(viewModel.To, 16);
        } else if (viewModel.current_step == mMarkers.size() - 2) {
            LatLng latlng = new LatLng(viewModel.steps.getValue().get(viewModel.current_step - 1).feature.geometry.coordinates[1], viewModel.steps.getValue().get(viewModel.current_step - 1).feature.geometry.coordinates[0]);
            update = CameraUpdateFactory.newLatLngZoom(latlng, 16);
        } else {
            LatLng latlng = new LatLng(viewModel.steps.getValue().get(viewModel.current_step - 1).feature.geometry.coordinates[1], viewModel.steps.getValue().get(viewModel.current_step - 1).feature.geometry.coordinates[0]);
            update = CameraUpdateFactory.newLatLngZoom(latlng, 16);
        }
        if (animate)
            mMap.animateCamera(update);
        else
            mMap.moveCamera(update);
    }

    private void showBottomSheet() {

        BottomSheetFragment fr = new BottomSheetFragment();
        fr.show(getParentFragmentManager(), null);
    }

    private static final String[] array = {
            "نقطة ركوب الحافلة الأولى",
            "نقطة ركوب الحافلة الثانية",
            "نقطة ركوب الحافلة الثالثة",
            "نقطة ركوب الحافلة الرابعة",
            "نقطة ركوب الحافلة الخامسة",
    };

    private void updateMapBasedOnResult() {

        mMarkers.add(mMap.addMarker(new MarkerOptions().position(viewModel.From).alpha(0.5f).title("موقعك الحالي")));
        List<FindPathModel.StepSteroid> features = viewModel.steps.getValue();
        int i = 0;
        for (FindPathModel.StepSteroid feature : features) {
            Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(feature.feature.geometry.coordinates[1], feature.feature.geometry.coordinates[0])));
            mMarkers.add(marker);
            marker.setTitle(array[i]);
            marker.setSnippet("إضغط هنا             ");
            marker.setTag(feature);
            i++;
        }
        mMarkers.add(mMap.addMarker(new MarkerOptions().position(viewModel.To).alpha(0.5f).title("وجهتك المطلوية")));

        mMap.setOnMarkerClickListener((marker -> {
            viewModel.current_step = mMarkers.indexOf(marker);
            if (viewModel.current_step == 0) {
                getView().findViewById(R.id.find_path_previous).setEnabled(false);
                getView().findViewById(R.id.find_path_next).setEnabled(true);
            } else if (viewModel.current_step == mMarkers.size() - 1) {
                getView().findViewById(R.id.find_path_previous).setEnabled(true);
                getView().findViewById(R.id.find_path_next).setEnabled(false);
            } else {
                getView().findViewById(R.id.find_path_previous).setEnabled(true);
                getView().findViewById(R.id.find_path_next).setEnabled(true);
            }
            return false;
        }));
        mMap.setOnInfoWindowClickListener((marker) -> {
            if (marker.getTag() == null || !(marker.getTag() instanceof FindPathModel.StepSteroid))
                return;
            viewModel.selectedFeature = (FindPathModel.StepSteroid) marker.getTag();
            showBottomSheet();
        });
        mMap.setOnInfoWindowCloseListener((marker) -> {
            activityModel.mainHandelr.post(getViewLifecycleOwner(), () -> {
                int step = mMarkers.indexOf(marker);
                if (viewModel.current_step == step) {
                    marker.showInfoWindow();
                }
            });
        });
        updateCamera(false);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (mMap != null) return;
        mMap = googleMap;
        if (mPendingUpdate) {
            updateMapBasedOnResult();
            updateInfoWindow();
            mPendingUpdate = false;
        }

        MarkerUtils.addMarker(getViewLifecycleOwner(), activityModel, mMap, requireActivity());

    }
}
