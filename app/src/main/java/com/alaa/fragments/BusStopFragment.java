package com.alaa.fragments;


import android.app.Application;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alaa.transportapp.MapsActivity;
import com.alaa.transportapp.R;
import com.alaa.utils.getTimeString;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;


public class BusStopFragment extends Fragment {

    private BusStopViewModel model;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bus_stop_layout, container, false);
    }


    private void collapseFilter(boolean call_begin_transition, BusStopViewModel.Properties.FromTo chosen) {
        ViewGroup group = getView().findViewById(R.id.filter_card);

        if (call_begin_transition) {
            TransitionManager.beginDelayedTransition((ViewGroup) getView());
        }
        group.getChildAt(0).setVisibility(View.GONE);
        group.getChildAt(1).setVisibility(View.VISIBLE);
        ((TextView) group.getChildAt(1).findViewById(R.id.routes_filter_text)).setText(chosen.toString());
        View touchHolder = getView().findViewById(R.id.filter_touch_holder);
        touchHolder.setVisibility(View.GONE);
    }


    private void updateTrackTime() {

        View view = getView();
        if (view == null || isDetached()) {
            return;
        }
        model.properties.getValue().refreshTime();
        if (model.properties.getValue().nearestI <= 30) {
            view.findViewById(R.id.bus_stop_track).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.bus_stop_track).setVisibility(View.GONE);
        }
        ((AppCompatTextView) view.findViewById(R.id.bus_stop_arrival)).setText(model.properties.getValue().nearest);

        //new ViewModelProvider(getActivity()).get(MapsActivity.ActivityModel.class).mainHandelr.postDelayed(() -> {
        //    updateTrackTime();
        // }, 10000);
    }


    private void handleRouteFilterClickCollapsed() {

        ViewGroup group = getView().findViewById(R.id.filter_card);
        TransitionManager.beginDelayedTransition((ViewGroup) getView());
        group.getChildAt(0).setVisibility(View.VISIBLE);
        group.getChildAt(1).setVisibility(View.GONE);
        View touchHolder = getView().findViewById(R.id.filter_touch_holder);
        touchHolder.setVisibility(View.VISIBLE);

        touchHolder.setOnClickListener((v) -> {
            TransitionManager.beginDelayedTransition((ViewGroup) getView());
            touchHolder.setVisibility(View.GONE);
            group.getChildAt(0).setVisibility(View.GONE);
            group.getChildAt(1).setVisibility(View.VISIBLE);
        });


    }

    private void handleRouteFilterRouteChosen(BusStopViewModel.Properties.FromTo chosen) {

        collapseFilter(true, chosen);
        //update ViewModel
        if (model.updateByFilter(chosen, getActivity())) {
            prepareForLoading(getView(), false);
        }

    }

    private void initViews(View view) {

        RecyclerView list = view.findViewById(R.id.bus_stop_schedule);

        if (list.getAdapter() == null) {
            list.setItemAnimator(null);
            list.setAdapter(new ItemsAdapter());
            list.setLayoutManager(new LinearLayoutManager(view.getContext()));
        } else {
            list.setAdapter(list.getAdapter());
        }

        ViewGroup viewGroup = (ViewGroup) view;
        TransitionManager.beginDelayedTransition(viewGroup);
        ViewGroup nestedScroll = (ViewGroup) ((ViewGroup) viewGroup.getChildAt(0)).getChildAt(0);
        for (int i = 0; i < nestedScroll.getChildCount(); i++) {
            nestedScroll.getChildAt(i).setVisibility(View.VISIBLE);
        }
        nestedScroll.findViewById(R.id.progressBar).setVisibility(View.GONE);


        viewGroup.findViewById(R.id.filter_button).setOnClickListener((v) -> {
            handleRouteFilterClickCollapsed();
        });

        ListView listView = viewGroup.findViewById(R.id.filter_list);
        if (listView.getAdapter() == null) {
            listView.setAdapter(new ArrayAdapter<BusStopViewModel.Properties.FromTo>(view.getContext(), R.layout.route_select_item, R.id.route_filter_text, model.properties.getValue().fromTos));
            listView.setOnItemClickListener((v1, v2, index, id) -> {
                handleRouteFilterRouteChosen(model.properties.getValue().fromTos[index]);
            });
        }

        viewGroup.getChildAt(2).setVisibility(View.VISIBLE);
        ((TextView) viewGroup.getChildAt(2).findViewById(R.id.routes_filter_text)).setText(model.properties.getValue().chosen.toString());

        updateTrackTime();

    }

    private void prepareForLoading(View view, boolean hide_filter) {


        ViewGroup viewGroup = (ViewGroup) view;
        TransitionManager.beginDelayedTransition(viewGroup);
        if (hide_filter) {
            viewGroup.getChildAt(2).setVisibility(View.GONE);
        } else {
            viewGroup.getChildAt(2).setVisibility(View.VISIBLE);
        }
        ViewGroup nestedScroll = (ViewGroup) ((ViewGroup) ((ViewGroup) view).getChildAt(0)).getChildAt(0);
        for (int i = 0; i < nestedScroll.getChildCount(); i++) {
            nestedScroll.getChildAt(i).setVisibility(View.GONE);
        }
        nestedScroll.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        view.findViewById(R.id.arrow_back).setVisibility(View.VISIBLE);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        ViewModelProvider provider = new ViewModelProvider(getActivity());
        MapsActivity.ActivityModel activityModel = provider.get(MapsActivity.ActivityModel.class);
        model = provider.get(BusStopViewModel.class);

        prepareForLoading(view, true);
        view.findViewById(R.id.arrow_back).setOnClickListener(v -> {
            getActivity().onBackPressed();
        });


        if (model.properties.getValue() == null) {
            model.update(provider.get(PassengerMainFragment.PassengerState.class).state.getValue().selectedBusStop, activityModel, getActivity().getApplication());

        } else if (provider.get(PassengerMainFragment.PassengerState.class).state.getValue().selectedBusStop == model.properties.getValue().mFeature) {
            //do nothing
        } else {
            model.properties.setValue(null);
            model.update(provider.get(PassengerMainFragment.PassengerState.class).state.getValue().selectedBusStop, activityModel, getActivity().getApplication());
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


    public static class BusStopViewModel extends ViewModel {
        MutableLiveData<Properties> properties;


        public BusStopViewModel() {
            properties = new MutableLiveData<>();
        }


        public boolean updateByFilter(Properties.FromTo chosen, ViewModelStoreOwner activity) {
            if (properties.getValue().chosen == chosen) {
                return false;
            }

            ViewModelProvider provider = new ViewModelProvider(activity);
            MapsActivity.ActivityModel activityModel = provider.get(MapsActivity.ActivityModel.class);
            activityModel.exe.execute(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                properties.getValue().chosen = chosen;
                properties.getValue().refresh();
                properties.postValue(properties.getValue());
            });
            return true;
        }

        public boolean update(MapsActivity.PointsStructure.Feature feature, MapsActivity.ActivityModel activityModel, Application application) {

            activityModel.exe.execute(() -> {

                Properties prop;
                try {
                    InputStreamReader reader = new InputStreamReader(application.getAssets().open("schedules/" + feature.id + ".json"));
                    prop = new Gson().fromJson(reader, Properties.class);
                    prop.chosen_schedule = prop.schedule;
                    prop.all = new Properties.FromTo(null, "جميع الخطوط");
                    prop.chosen = prop.all;

                    prop.fromTos = new Properties.FromTo[prop.names.length + 1];
                    prop.fromTos[0] = prop.all;
                    for (int i = 1; i < prop.fromTos.length; i++) {
                        String name = prop.names[i - 1];
                        String[] split = name.substring(0, name.indexOf(".response")).split("-");
                        Properties.FromTo fromto = new Properties.FromTo();
                        fromto.from = split[0];
                        fromto.to = split[1];
                        prop.fromTos[i] = fromto;
                    }

                    reader.close();
                    prop.refresh();
                    prop.mFeature = feature;
                    properties.postValue(prop);
                } catch (IOException e) {

                    e.printStackTrace();
                }

            });
            return true;
        }

        public static class Properties {

            FromTo all;
            protected String[] names;
            public TimeStamp[] schedule;
            public TimeStamp[] chosen_schedule;
            public MapsActivity.PointsStructure.Feature mFeature;
            public String nearest;
            public int nearestI = -1;
            public FromTo[] fromTos;
            public FromTo chosen;
            public volatile boolean isRefreshing = false;

            public void refreshTime() {
                if (isRefreshing) return;
                if (chosen_schedule.length > 0) {

                    Calendar calendar = Calendar.getInstance();
                    int hours = calendar.get(Calendar.HOUR_OF_DAY);
                    int minutes = calendar.get(Calendar.MINUTE);

                    int dHours = chosen_schedule[0].hours - hours;
                    int dminutes = chosen_schedule[0].minutes - minutes;

                    int total = dHours * 60 + dminutes;
                    if (total >= 60) {

                        nearest = MessageFormat.format("{0} ساعة و {1} قيقة", total / 60, total % 60);

                    } else {

                        nearest = MessageFormat.format("{0} دقيقة", total);
                    }
                    nearestI = total;
                } else {
                    nearest = "لا يوجد حافلات قادمة لهذا اليوم";
                    nearestI = -1;
                }
            }

            @WorkerThread
            public void refresh() {
                isRefreshing = true;
                if (chosen != null) {
                    if (chosen.from == null) {
                        chosen_schedule = schedule;
                    } else {

                        List<TimeStamp> ch = new LinkedList<>();
                        for (TimeStamp fr : schedule) {
                            if (fr.from.equals(chosen.from) && fr.to.equals(chosen.to)) {
                                ch.add(fr);
                            }
                        }
                        chosen_schedule = new TimeStamp[ch.size()];
                        chosen_schedule = ch.toArray(chosen_schedule);
                    }
                }

                Calendar calendar = Calendar.getInstance();
                int hours = calendar.get(Calendar.HOUR_OF_DAY);
                int minutes = calendar.get(Calendar.MINUTE);
                int start = 0;
                for (int i = 0; i < chosen_schedule.length; i++) {
                    if (chosen_schedule[i].hours < hours) continue;
                    if (chosen_schedule[i].hours == hours) {
                        if (minutes <= chosen_schedule[i].minutes) {
                            start = i;
                            break;
                        }
                    }
                }

                TimeStamp[] sc = new TimeStamp[chosen_schedule.length - start];
                for (int i = start; i < chosen_schedule.length; i++) {
                    sc[i - start] = chosen_schedule[i];
                    chosen_schedule[i].getID();// just to initialize the hash calculation in background so to save some lags
                }
                chosen_schedule = sc;


                isRefreshing = false;
                refreshTime();
            }


            public static class FromTo {
                String from;
                String to;

                public FromTo() {

                }

                public FromTo(String from, String to) {
                    this.from = from;
                    this.to = to;
                }

                @NonNull
                @Override
                public String toString() {
                    if (from == null) {
                        return to;
                    }
                    return MessageFormat.format("من {0} الى {1}", from, to);
                }
            }

            public class TimeStamp {
                public int hours;
                public int minutes;
                public int count;
                public String from;
                public String to;
                private long hash = 0;


                public long getID() {
                    if (hash == 0) {
                        hash = to.hashCode();
                        hash <<= 16;
                        hash |= from.hashCode();
                        hash ^= hours * 1000;
                        hash ^= minutes;
                        hash += count;
                    }
                    return hash;
                }

            }
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

            holder.bus_stop_item_arrival.setText(getTimeString.getTime(model.properties.getValue().chosen_schedule[position].hours, model.properties.getValue().chosen_schedule[position].minutes));
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
