package com.alaa.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.alaa.transportapp.R;
import com.alaa.utils.AnimationFragment;
import com.alaa.utils.getTimeUtils;
import com.alaa.viewmodels.ActivityModel;
import com.alaa.viewmodels.BusStopViewModel;

import java.text.MessageFormat;


public class BusStopFragment extends AnimationFragment {

    private BusStopViewModel model;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bus_stop_layout, container, false);
    }


    private void updateTrackTime() {

        View view = getView();

        model.properties.getValue().refreshTime();

        ((AppCompatTextView) view.findViewById(R.id.bus_stop_arrival)).setText(model.properties.getValue().nearest);

        new ViewModelProvider(getActivity()).get(ActivityModel.class).mainHandelr.postDelayed(this, () -> {
            updateTrackTime();
        }, 10000);

    }


    private void initViews(View view) {


        RecyclerView list = view.findViewById(R.id.bus_stop_schedule);


        TransitionManager.beginDelayedTransition((ViewGroup) getView());


        view.findViewById(R.id.bus_stop_loaded_container).setVisibility(View.VISIBLE);
        view.findViewById(R.id.progressBar).setVisibility(View.GONE);


        if (list.getAdapter() == null) {
            list.setAdapter(new ItemsAdapter());
            list.setLayoutManager(new LinearLayoutManager(view.getContext()));
        } else {
            list.getAdapter().notifyDataSetChanged();
        }
        updateTrackTime();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        ViewModelProvider provider = new ViewModelProvider(requireActivity());
        ActivityModel activityModel = provider.get(ActivityModel.class);

        model = provider.get(BusStopViewModel.class);

        view.findViewById(R.id.arrow_back).setOnClickListener(v -> {
            getActivity().onBackPressed();
        });


        /*
        boolean isBottomSheetFragment = false;
        if (getArguments() != null)
            isBottomSheetFragment = getArguments().getBoolean(BottomSheetFragment.BusStopArgumentKey);

        if (!isBottomSheetFragment && model.properties.getValue() == null) {
            model.update(provider.get(PassengerState.class).state.getValue().selectedBusStop, activityModel, getActivity().getApplication());

        } else if (isBottomSheetFragment && model.properties.getValue() == null) {
            model.update(provider.get(FindPathModel.class).selectedFeature.feature, activityModel, getActivity().getApplication());
        } else if (!isBottomSheetFragment && provider.get(PassengerState.class).state.getValue().selectedBusStop == model.properties.getValue().mFeature) {
            //do nothing or refresh so recycler view does not go full screen
            //refresh (update : did not work :<)
            //model.properties.setValue(null);
            //model.update(provider.get(PassengerState.class).state.getValue().selectedBusStop, activityModel, getActivity().getApplication());
        } else if (!isBottomSheetFragment) {
            Log.e("Alaa", "1 is called");
            model.properties.setValue(null);
            model.update(provider.get(PassengerState.class).state.getValue().selectedBusStop, activityModel, getActivity().getApplication());
        } else if (isBottomSheetFragment && provider.get(FindPathModel.class).selectedFeature.feature == model.properties.getValue().mFeature) {
            //do nothing or refresh so recycler view does not go full screen
            //refresh (update : did not work :<)
            //model.properties.setValue(null);
            //model.update(provider.get(PassengerState.class).state.getValue().selectedBusStop, activityModel, getActivity().getApplication());

        } else {
            Log.e("Alaa", "2 is called");
            model.properties.setValue(null);
            model.update(provider.get(FindPathModel.class).selectedFeature.feature, activityModel, getActivity().getApplication());
        }

        */

        if (model.properties.getValue() == null) {
            model.update(model.SelectedFeature, activityModel, requireActivity());

        } else if (model.SelectedFeature == model.properties.getValue().mFeature && model.Filter == null) {
            //do nothing or refresh so recycler view does not go full screen
            //refresh (update : did not work :<)
            //model.properties.setValue(null);
            //model.update(provider.get(PassengerState.class).state.getValue().selectedBusStop, activityModel, getActivity().getApplication());
        } else {
            model.properties.setValue(null);
            model.update(model.SelectedFeature, activityModel, requireActivity());
        }

        model.properties.observe(getViewLifecycleOwner(), (item) -> {
            if (item == null) return;
            initViews(getView());
        });
    }

    private class ScheduleViewHolder extends RecyclerView.ViewHolder {

        TextView bus_stop_item_route;
        TextView bus_stop_item_arrival;
        TextView bus_stop_item_count;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            bus_stop_item_arrival = itemView.findViewById(R.id.bus_stop_item_arrival);
            bus_stop_item_route = itemView.findViewById(R.id.bus_stop_item_route);
            bus_stop_item_count = itemView.findViewById(R.id.bus_stop_item_count);
        }
    }


    private class ItemsAdapter extends RecyclerView.Adapter<ScheduleViewHolder> {


        private final char[] ENGLISH_NUMBERS = {'\u0030', '\u0031', '\u0032', '\u0033', '\u0034', '\u0035', '\u0036', '\u0037', '\u0038', '\u0039'};
        private final char[] ARABIC_NUMBERS = {'\u0660', '\u0661', '\u0662', '\u0663', '\u0664', '\u0665', '\u0666', '\u0667', '\u0668', '\u0669'};

        public ItemsAdapter() {
            super();
            setHasStableIds(false);
        }

        @NonNull
        @Override
        public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ScheduleViewHolder(getLayoutInflater().inflate(R.layout.bus_stop_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
            holder.bus_stop_item_count.setText(MessageFormat.format("{0} حافلات", model.properties.getValue().chosen_schedule[position].count).replace(ENGLISH_NUMBERS[0], ARABIC_NUMBERS[0])
                    .replace(ENGLISH_NUMBERS[1], ARABIC_NUMBERS[1])
                    .replace(ENGLISH_NUMBERS[2], ARABIC_NUMBERS[2])
                    .replace(ENGLISH_NUMBERS[3], ARABIC_NUMBERS[3])
                    .replace(ENGLISH_NUMBERS[4], ARABIC_NUMBERS[4])
                    .replace(ENGLISH_NUMBERS[5], ARABIC_NUMBERS[5])
                    .replace(ENGLISH_NUMBERS[6], ARABIC_NUMBERS[6])
                    .replace(ENGLISH_NUMBERS[7], ARABIC_NUMBERS[7])
                    .replace(ENGLISH_NUMBERS[8], ARABIC_NUMBERS[8])
                    .replace(ENGLISH_NUMBERS[9], ARABIC_NUMBERS[9]));
            holder.bus_stop_item_route.setText(MessageFormat.format("من {0} الى {1}", model.properties.getValue().chosen_schedule[position].from, model.properties.getValue().chosen_schedule[position].to));

            holder.bus_stop_item_arrival.setText(getTimeUtils.getTime(model.properties.getValue().chosen_schedule[position].hours, model.properties.getValue().chosen_schedule[position].minutes));
        }

        @Override
        public int getItemCount() {
            return model.properties.getValue().chosen_schedule.length;
        }

        @Override
        public long getItemId(int position) {
            return model.properties.getValue().chosen_schedule[position].getID();
        }

    }
}
