package com.alaa.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alaa.transportapp.R;

public class MainFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.main_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        Button passenger = view.findViewById(R.id.main_passenger_button);
        Button driver = view.findViewById(R.id.main_driver_button);

        passenger.setOnClickListener((View v) -> {
            getParentFragmentManager().beginTransaction().replace(android.R.id.content, new PassengerMainFragment()).addToBackStack(null).commit();
        });

        driver.setOnClickListener((View v) -> {
            getParentFragmentManager().beginTransaction().replace(android.R.id.content, new DriverEnterIDFragment()).addToBackStack(null).commit();
        });

    }


}
