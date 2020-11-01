package com.alaa.fragments;

import android.app.Application;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.TransitionManager;

import com.alaa.transportapp.R;
import com.alaa.utils.AnimationFragment;
import com.alaa.utils.GetAssets;
import com.alaa.utils.getTimeUtils;
import com.alaa.viewmodels.ActivityModel;
import com.google.android.gms.maps.model.LatLng;
import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.List;

public class DriverPageFragment extends AnimationFragment {


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.driver_layout, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        ViewModelState state = new ViewModelProvider(this).get(ViewModelState.class);

        if (state.trips == null) {
            state.trips = new MutableLiveData<>();
            fetch(state, new ViewModelProvider(requireActivity()).get(ActivityModel.class));
            state.trips.observe(this.getViewLifecycleOwner(), (item) -> {
                TransitionManager.beginDelayedTransition((ViewGroup) getView());

                view.findViewById(R.id.driver_ask_for_trip).setVisibility(View.VISIBLE);
                view.findViewById(R.id.hint_next_trip).setVisibility(View.VISIBLE);
                view.findViewById(R.id.hint_next_trip_2).setVisibility(View.VISIBLE);
                view.findViewById(R.id.driver_show_on_map).setVisibility(View.VISIBLE);
                view.findViewById(R.id.driver_current_trip_container).setVisibility(View.VISIBLE);


                view.findViewById(R.id.progressBar2).setVisibility(View.GONE);
                updateState(state);

            });
        } else {

            view.findViewById(R.id.driver_ask_for_trip).setVisibility(View.VISIBLE);
            view.findViewById(R.id.hint_next_trip).setVisibility(View.VISIBLE);
            view.findViewById(R.id.hint_next_trip_2).setVisibility(View.VISIBLE);
            view.findViewById(R.id.driver_show_on_map).setVisibility(View.VISIBLE);
            view.findViewById(R.id.driver_current_trip_container).setVisibility(View.VISIBLE);

            view.findViewById(R.id.progressBar2).setVisibility(View.GONE);
            updateState(state);
        }

        view.findViewById(R.id.arrow_back_driver).setOnClickListener(v -> {
            getActivity().onBackPressed();
        });


        view.findViewById(R.id.driver_ask_for_trip).setOnClickListener((v) -> {
            if (state.mCurrent >= state.trips.getValue().length - 1) {
                return;
            }
            state.mCurrent++;
            if (state.mCurrent == state.trips.getValue().length - 1) {
                SpannableString spannedString = new SpannableString("لا يوجد رحلات متوافرة");
                spannedString.setSpan(new ForegroundColorSpan(Color.RED), 0, spannedString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                ((Button) view.findViewById(R.id.driver_ask_for_trip)).setText(spannedString);
            } else {
                // setTrip data
                updateState(state);
            }
        });
        view.findViewById(R.id.driver_show_on_map).setOnClickListener((v) -> {

            String url = MessageFormat.format("https://www.google.com/maps/search/?api=1&query={0},{1}", state.trips.getValue()[state.mCurrent].busStopCor.latitude, state.trips.getValue()[state.mCurrent].busStopCor.longitude);

            Uri destination = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, destination);
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(getActivity(), "You should install google maps to enable this feature!", Toast.LENGTH_LONG).show();
            }


        });

    }

    private void updateState(ViewModelState state) {
        View container = getView().findViewById(R.id.driver_current_trip);
        Trip currentTrip = state.trips.getValue()[state.mCurrent];
        ((TextView) container.findViewById(R.id.driver_bus_stop)).setText(currentTrip.busStop);
        ((TextView) container.findViewById(R.id.driver_arrival_time)).setText(MessageFormat.format("وقت التواجد {0} ", getTimeUtils.getTime(currentTrip.arrivalTimeH, currentTrip.arrivalTimeM)));
        ((TextView) container.findViewById(R.id.driver_waiting_time)).setText(MessageFormat.format("وقت الانتظار {0} دقائق", currentTrip.waitingTime));
        ((TextView) container.findViewById(R.id.driver_working_line)).setText(currentTrip.Route);

    }

    private void fetch(ViewModelState state, ActivityModel model) {

        Application app = getActivity().getApplication();
        model.exe.execute(() -> {

            try {
                CSVReader reader = new CSVReader(new InputStreamReader(GetAssets.open(app, "fake_schedule.csv")));
                List<String[]> entries = reader.readAll();

                Trip[] trips = new Trip[entries.size()];
                for (int i = 0; i < trips.length; i++) {
                    Trip trip = new Trip();
                    trip.busStop = entries.get(i)[0];
                    trip.Route = entries.get(i)[1];
                    trip.waitingTime = Integer.parseInt(entries.get(i)[2]);
                    trip.arrivalTimeH = Integer.parseInt(entries.get(i)[3]);
                    trip.arrivalTimeM = Integer.parseInt(entries.get(i)[4]);
                    trip.busStopCor = new LatLng(Double.parseDouble(entries.get(i)[5]), Double.parseDouble(entries.get(i)[6]));
                    trips[i] = trip;
                }
                reader.close();

                state.mCurrent = 0;
                state.trips.postValue(trips);
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }

    private static class Trip {
        String busStop;
        String Route;
        int waitingTime; // minutes
        int arrivalTimeH;
        int arrivalTimeM;
        LatLng busStopCor;

    }


    public static final class ViewModelState extends ViewModel {

        // index of current Trip
        private int mCurrent = 0;
        MutableLiveData<Trip[]> trips = null;
    }

}
