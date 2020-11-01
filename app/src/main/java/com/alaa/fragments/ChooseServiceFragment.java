package com.alaa.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.alaa.transportapp.R;
import com.alaa.utils.AnimationFragment;
import com.alaa.viewmodels.PassengerRequestModel;

public class ChooseServiceFragment extends AnimationFragment {


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.passenger_choose_service, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        view.findViewById(R.id.arrow_back).setOnClickListener((v) -> {
            getActivity().onBackPressed();
        });

        ViewGroup scheduleService = view.findViewById(R.id.bus_stop_schedule_service);
        ViewGroup requestTripService = view.findViewById(R.id.request_lift_service);
        ViewGroup getRoutesService = view.findViewById(R.id.get_routes_service);

        ((TextView) scheduleService.findViewById(R.id.route_name)).setText("معرفة جدول مواعيد الحافلات");
        ((TextView) requestTripService.findViewById(R.id.route_name)).setText("طلب توصيلة");
        ((TextView) getRoutesService.findViewById(R.id.route_name)).setText("معرفة كيفية الوصول الى وجهتك باستخدام حافلات النقل العام");

        scheduleService.setOnClickListener((v) -> {
            getParentFragmentManager().beginTransaction().replace(android.R.id.content, new PassengerMainFragment()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).commit();
        });

        requestTripService.setOnClickListener((v) -> {

            ViewModelProvider provider = new ViewModelProvider(requireActivity());
            if (provider.get(PassengerRequestModel.class).status.getValue() == PassengerRequestModel.STATUS_NOT_SENT)
                getParentFragmentManager().beginTransaction().replace(android.R.id.content, new PassengerRequestFragment()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).commit();
            else
                getParentFragmentManager().beginTransaction().replace(android.R.id.content, new PassengerRequestStatusFragment()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).commit();

        });
        getRoutesService.setOnClickListener((v) -> {
            getParentFragmentManager().beginTransaction().replace(android.R.id.content, new ToFragment()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).commit();
        });
    }
}
