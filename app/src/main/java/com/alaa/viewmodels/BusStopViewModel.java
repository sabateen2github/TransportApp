package com.alaa.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.alaa.utils.getTimeUtils;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class BusStopViewModel extends ViewModel {
    public MutableLiveData<Properties> properties;


    public BusStopViewModel() {
        properties = new MutableLiveData<>();
    }


    @WorkerThread
    public static int getNearestBusInMinutes(String Route_id, ActivityModel.PointsStructure.Feature feature, Application application) {

        Properties properties = Properties.getProperties(application, feature);

        String[] arr = Route_id.substring(0, Route_id.indexOf(".response")).split("-");

        Properties.TimeStamp[] stamps = properties.schedule;

        for (Properties.TimeStamp stamp : stamps) {

            if (arr[0].equals(stamp.from) && arr[1].equals(stamp.to)) {

                int period = getTimeUtils.getPeriodFromNow(stamp.hours, stamp.minutes);
                if (period < 0) continue;
                return period;
            }
        }

        return -1;

    }

    public boolean updateByFilter(Properties.FromTo chosen, ViewModelStoreOwner activity) {
        if (properties.getValue().chosen == chosen) {
            return false;
        }

        ViewModelProvider provider = new ViewModelProvider(activity);
        ActivityModel activityModel = provider.get(ActivityModel.class);
        activityModel.exe.execute(() -> {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            properties.getValue().chosen = chosen;
            properties.getValue().refresh();
            properties.postValue(properties.getValue());
        });
        return true;
    }

    public boolean update(ActivityModel.PointsStructure.Feature feature, ActivityModel activityModel, Application application) {

        activityModel.exe.execute(() -> {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Properties prop;
            prop = Properties.getProperties(application, feature);
            properties.postValue(prop);

        });
        return true;
    }

    public static class Properties {

        FromTo all;
        public String[] names;
        public TimeStamp[] schedule;
        public TimeStamp[] chosen_schedule;
        public ActivityModel.PointsStructure.Feature mFeature;
        public String nearest;
        public int nearestI = -1;
        public FromTo[] fromTos;
        public FromTo chosen;
        public volatile boolean isRefreshing = false;

        public void refreshTime() {
            if (isRefreshing) return;
            if (chosen_schedule.length > 0) {

                int total = getTimeUtils.getPeriodFromNow(chosen_schedule[0].hours, chosen_schedule[0].minutes);
                nearest = getTimeUtils.getTimePeriodString(total);
                nearestI = total;
            } else {
                nearest = "لا يوجد حافلات قادمة لهذا اليوم";
                nearestI = -1;
            }
        }

        @WorkerThread
        public static Properties getProperties(Application application, ActivityModel.PointsStructure.Feature feature) {
            Properties prop = null;
            try {
                InputStreamReader reader = new InputStreamReader(application.getAssets().open("schedules/" + feature.id + ".json"));
                Gson gson = new Gson();
                prop = gson.fromJson(reader, Properties.class);
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
            } catch (IOException e) {

                e.printStackTrace();
            }
            return prop;

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