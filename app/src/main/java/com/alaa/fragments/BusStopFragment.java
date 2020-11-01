package com.alaa.fragments;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
import com.alaa.viewmodels.PassengerState;

import java.text.MessageFormat;


public class BusStopFragment extends AnimationFragment {

    private BusStopViewModel model;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bus_stop_layout, container, false);
    }


    private void collapseFilter(boolean call_begin_transition, BusStopViewModel.Properties.FromTo chosen) {
        ViewGroup group = (ViewGroup) ((ViewGroup) getView().findViewById(R.id.filter_card)).getChildAt(0);

        if (call_begin_transition) {
            TransitionManager.beginDelayedTransition(getView().findViewById(R.id.bus_stop_nav_container));
        }
        group.getChildAt(0).setVisibility(View.GONE);
        group.getChildAt(1).setVisibility(View.VISIBLE);
        ((TextView) group.findViewById(R.id.routes_filter_text)).setText(chosen.toString());
        View touchHolder = getView().findViewById(R.id.filter_touch_holder);
        touchHolder.setVisibility(View.GONE);
    }

    private void updateTrackTime() {

        View view = getView();

        model.properties.getValue().refreshTime();

        ((AppCompatTextView) view.findViewById(R.id.bus_stop_arrival)).setText(model.properties.getValue().nearest);

        new ViewModelProvider(getActivity()).get(ActivityModel.class).mainHandelr.postDelayed(this, () -> {
            updateTrackTime();
        }, 10000);

    }


    private void handleRouteFilterClickCollapsed() {

        ViewGroup group = (ViewGroup) ((ViewGroup) getView().findViewById(R.id.filter_card)).getChildAt(0);
        View touchHolder = getView().findViewById(R.id.filter_touch_holder);
        touchHolder.setVisibility(View.VISIBLE);

        TransitionManager.beginDelayedTransition(getView().findViewById(R.id.bus_stop_nav_container));
        group.getChildAt(0).setVisibility(View.VISIBLE);
        group.getChildAt(1).setVisibility(View.GONE);


        touchHolder.setOnClickListener((v) -> {
            TransitionManager.beginDelayedTransition(getView().findViewById(R.id.bus_stop_nav_container));
            touchHolder.setVisibility(View.GONE);
            group.getChildAt(0).setVisibility(View.GONE);
            group.getChildAt(1).setVisibility(View.VISIBLE);
        });

    }

    private void handleRouteFilterRouteChosen(BusStopViewModel.Properties.FromTo chosen) {


        //update ViewModel
        if (model.updateByFilter(chosen, getActivity())) {
            ((TextView) getView().findViewById(R.id.routes_filter_text)).setText(chosen.toString());
            prepareForLoading(getView(), false, true);
        }
        collapseFilter(true, chosen);

    }

    private void initViews(View view) {


        RecyclerView list = view.findViewById(R.id.bus_stop_schedule);
        ListView listView = view.findViewById(R.id.filter_list);
        list.setFocusable(false);


        TransitionManager.beginDelayedTransition(view.findViewById(R.id.bus_stop_loaded_container));

        view.findViewById(R.id.bus_stop_loaded_container).setVisibility(View.VISIBLE);
        view.findViewById(R.id.progressBar).setVisibility(View.GONE);
        view.findViewById(R.id.filter_card).setVisibility(View.VISIBLE);


        if (list.getAdapter() == null) {
            list.setAdapter(new ItemsAdapter());
            list.setLayoutManager(new LinearLayoutManager(view.getContext()));
        } else {
            list.getAdapter().notifyDataSetChanged();
        }


        view.findViewById(R.id.filter_button).setOnClickListener((v) -> {
            handleRouteFilterClickCollapsed();
        });

        if (listView.getAdapter() == null) {
            listView.setAdapter(new ArrayAdapter<BusStopViewModel.Properties.FromTo>(view.getContext(), R.layout.route_select_item, R.id.route_filter_text, model.properties.getValue().fromTos));
            listView.setOnItemClickListener((v1, v2, index, id) -> {
                handleRouteFilterRouteChosen(model.properties.getValue().fromTos[index]);
            });
        }
        updateTrackTime();
    }

    private void prepareForLoading(View view, boolean hide_filter, boolean transition) {

        if (transition) {
            TransitionManager.beginDelayedTransition(view.findViewById(R.id.bus_stop_loaded_container));
            TransitionManager.beginDelayedTransition(view.findViewById(R.id.bus_stop_nav_container));
        }

        if (hide_filter) {
            view.findViewById(R.id.filter_card).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.filter_card).setVisibility(View.VISIBLE);
        }

        view.findViewById(R.id.bus_stop_loaded_container).setVisibility(View.GONE);
        view.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        ViewModelProvider provider = new ViewModelProvider(getActivity());
        ActivityModel activityModel = provider.get(ActivityModel.class);

        model = provider.get(BusStopViewModel.class);

        view.findViewById(R.id.arrow_back).setOnClickListener(v -> {
            getActivity().onBackPressed();
        });


        if (model.properties.getValue() == null) {
            model.update(provider.get(PassengerState.class).state.getValue().selectedBusStop, activityModel, getActivity().getApplication());

        } else if (provider.get(PassengerState.class).state.getValue().selectedBusStop == model.properties.getValue().mFeature) {
            //do nothing or refresh so recycler view does not go full screen
            //refresh (update : did not work :<)
            //model.properties.setValue(null);
            //model.update(provider.get(PassengerState.class).state.getValue().selectedBusStop, activityModel, getActivity().getApplication());
        } else {
            model.properties.setValue(null);
            model.update(provider.get(PassengerState.class).state.getValue().selectedBusStop, activityModel, getActivity().getApplication());
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
            holder.bus_stop_item_count.setText(MessageFormat.format("{0} حافلات", model.properties.getValue().chosen_schedule[position].count));
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
