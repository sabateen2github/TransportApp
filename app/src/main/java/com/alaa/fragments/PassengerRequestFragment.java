package com.alaa.fragments;

import android.app.Application;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.alaa.transportapp.MapsActivity;
import com.alaa.transportapp.R;
import com.alaa.utils.AnimationFragment;
import com.alaa.viewmodels.ActivityModel;
import com.alaa.viewmodels.BusStopViewModel;
import com.alaa.viewmodels.PassengerRequestModel;

public class PassengerRequestFragment extends AnimationFragment {


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.need_lift_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {


        ActivityModel activityModel = new ViewModelProvider(requireActivity()).get(ActivityModel.class);
        ViewGroup root = (ViewGroup) view;

        View arrowBack = view.findViewById(R.id.arrow_back);
        ProgressBar progressBar = view.findViewById(R.id.need_lift_progressbar);
        ViewGroup routesContainer = view.findViewById(R.id.routes_container);

        arrowBack.setOnClickListener((v) -> {
            getActivity().onBackPressed();
        });


        Application application = getActivity().getApplication();
        ((MapsActivity) getActivity()).getCurrentLocation(center -> {
            activityModel.exe.execute(() -> {
                ActivityModel.PointsStructure.Feature nearest = activityModel.index.getValue().getNearest(center.latitude, center.longitude);
                BusStopViewModel.Properties properties = BusStopViewModel.Properties.getProperties(application, nearest);

                activityModel.mainHandelr.post(getViewLifecycleOwner(), () -> {
                    TransitionManager.beginDelayedTransition((ViewGroup) view);
                    ViewGroup container = ((ViewGroup) root.getChildAt(0));
                    for (int i = 0; i < container.getChildCount(); i++) {
                        container.getChildAt(i).setVisibility(View.VISIBLE);
                    }
                    progressBar.setVisibility(View.GONE);
                    for (int i = 1; i < properties.names.length; i++) {
                        View item = getLayoutInflater().inflate(R.layout.lift_rquest_route_item, routesContainer, false);
                        ((TextView) item.findViewById(R.id.route_name)).setText(properties.fromTos[i].toString());
                        routesContainer.addView(item);
                        int finalI = i;
                        item.setOnClickListener((v) -> {
                            //  send to this route
                            PassengerRequestModel passengerModel = new ViewModelProvider(requireActivity()).get(PassengerRequestModel.class);
                            passengerModel.Feature = nearest;
                            passengerModel.Route_ID = properties.names[finalI];

                            getParentFragmentManager().beginTransaction().replace(android.R.id.content, new PassengerRequestStatusFragment()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).commit();


                        });
                    }
                });
            });
        });


    }
}
