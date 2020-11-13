package com.alaa.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

import com.alaa.transportapp.R;
import com.alaa.utils.AnimationFragment;

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
        ViewGroup getRoutesService = view.findViewById(R.id.get_routes_service);

        ((TextView) scheduleService.findViewById(R.id.get_schedule_text)).setText("مواعيد الحافلات");
        ((TextView) getRoutesService.findViewById(R.id.get_route_text)).setText("كيف أصل إلى وجهتي ؟");

        scheduleService.setOnClickListener((v) -> {
            getParentFragmentManager().beginTransaction().replace(android.R.id.content, new PassengerMainFragment()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).commit();
        });


        getRoutesService.setOnClickListener((v) -> {
            getParentFragmentManager().beginTransaction().replace(android.R.id.content, new ToFragment()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).commit();
        });
    }
}
