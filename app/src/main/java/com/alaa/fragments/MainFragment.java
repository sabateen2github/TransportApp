package com.alaa.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

import com.alaa.transportapp.R;
import com.alaa.utils.AnimationFragment;

public class MainFragment extends AnimationFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        View passenger = view.findViewById(R.id.main_passenger_button);
        View driver = view.findViewById(R.id.main_driver_button);

        //main_driver_button
        passenger.setOnClickListener((View v) -> {
            getParentFragmentManager().beginTransaction().replace(android.R.id.content, new ChooseServiceFragment()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).addToBackStack(null).commit();
        });

        driver.setOnClickListener((View v) -> {
            getParentFragmentManager().beginTransaction().replace(android.R.id.content, new DriverEnterIDFragment()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).addToBackStack(null).commit();
        });

    }


}
