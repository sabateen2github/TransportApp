package com.alaa.viewmodels;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;


public class FindPathModel extends ViewModel {

    public volatile LatLng To = new LatLng(31.97164183082986d, 35.833652222827524d);
    public volatile LatLng From = new LatLng(31.97164183082986d, 35.833652222827524d);
    public MutableLiveData<List<StepSteroid>> steps = new MutableLiveData<>();

    //used for prev-next navigator Feature
    public int current_step;
    // used for bottom sheet fragment
    public StepSteroid selectedFeature;

    @MainThread
    public void refresh(ActivityModel activityModel, Application context, LifecycleOwner owner) {
        if (To == null || From == null) return;
        steps.setValue(null);
        LatLng _To = To;
        LatLng _From = From;
        activityModel.exe.execute(() -> {

            long id_from = activityModel.index.getValue().getNearest(_From.latitude, _From.longitude).id;
            long id_to = activityModel.index.getValue().getNearest(_To.latitude, _To.longitude).id;
            if (id_from == id_to) {

                activityModel.mainHandelr.post(owner, () -> {
                    Toast.makeText(context, "وجهتك ونقطة البدء متقاربات جدا ويمكن الذهاب مشيا", Toast.LENGTH_LONG).show();
                });
                if (From == _From && To == _To)
                    steps.postValue(new ArrayList<>());
                return;
            }

            File cacheDir = new File(context.getCacheDir(), "/to_from_dir");
            if (!cacheDir.exists()) {
                cacheDir.mkdir();
            }
            Gson gson = new Gson();
            Step[] s = null;
            File requestedFile = new File(cacheDir, MessageFormat.format("{0}_{1}.json", id_from, id_to));
            if (requestedFile.exists()) {
                try {
                    FileReader reader = new FileReader(requestedFile);
                    s = gson.fromJson(reader, Step[].class);
                    reader.close();
                    Log.e("Alaa", "Found in cache");

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    s = null;
                } catch (IOException e) {
                    e.printStackTrace();
                    s = null;
                }
            }
            if (s == null) {
                int errors = 0;
                while (errors < 5) {
                    Log.e("Alaa", "looping");
                    try {
                        URL url = new URL(MessageFormat.format(getFromToServiceURL() + "/?from={0}&to={1}", id_from, id_to));
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        s = gson.fromJson(reader, Step[].class);
                        connection.disconnect();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        Log.e("Alaa", "Error Malformed url");
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                        errors++;
                        continue;
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("Alaa", "IO exception");
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                        errors++;
                        continue;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("Alaa", "Exception unknown" + e.getMessage());
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                        errors++;
                        continue;
                    }
                    break;
                }
                if (errors >= 5) {
                    activityModel.mainHandelr.post(owner, () -> {
                        Toast.makeText(context, "errors occurred 5 times", Toast.LENGTH_LONG).show();
                    });
                    if (From == _From && To == _To)
                        steps.postValue(new ArrayList<>());
                    return;
                }

            }
            if (s != null) {

                //save to cache
                try {
                    if (requestedFile.createNewFile()) {
                        Writer writer = new FileWriter(requestedFile);
                        writer.write(gson.toJson(s));
                        writer.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                List<StepSteroid> features = new ArrayList<>();
                for (int i = 0; i < s.length; i++) {
                    StepSteroid steroid = new StepSteroid();
                    steroid.step = s[i];
                    for (ActivityModel.PointsStructure.Feature feature : activityModel.index.getValue().features) {
                        if (feature.id == s[i].id) {
                            steroid.feature = feature;
                            break;
                        }
                    }
                    features.add(steroid);
                }
                for (int i = 0; i < features.size(); i++) {
                    String[] names = features.get(i).step.receive;
                    for (int x = 0; x < names.length; x++) {
                        String[] pair = names[x].substring(0, names[x].indexOf(".response")).split("-");
                        names[x] = MessageFormat.format("من {0} الى {1}", pair[0], pair[1]);
                    }
                    features.get(i).stepIndex = i + 1;
                }

                if (From == _From && To == _To) {

                    current_step = 0;
                    for (int i = 1; i < features.size(); i++) {
                        features.get(i - 1).step.receive = features.get(i).step.receive;
                    }
                    steps.postValue(features);
                }
            } else {
                activityModel.mainHandelr.post(owner, () -> {
                    Toast.makeText(context, "An Error occurred", Toast.LENGTH_LONG).show();
                });
            }


        });

    }


    private static String cachedURL = null;

    private static String getFromToServiceURL() {
        if (cachedURL != null) return cachedURL;
        try {
            URL url = new URL("https://myeloid-professions.000webhostapp.com/AppEngineURL.id");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String AppEngineURL = reader.readLine();
            connection.disconnect();
            cachedURL = AppEngineURL;
            return AppEngineURL;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e("Alaa", "Get AppEngine URL Failed");

        return "https://new-age-192017.uc.r.appspot.com";
    }

    public static class Step {
        public int id;
        public String[] receive;
    }

    public static class StepSteroid {
        public ActivityModel.PointsStructure.Feature feature;
        public Step step;
        public int stepIndex;
    }

}
