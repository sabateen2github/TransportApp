package com.alaa.fragments;


import android.app.Application;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;

import com.alaa.transportapp.MapsActivity;
import com.alaa.transportapp.R;
import com.alaa.utils.AnimationFragment;
import com.alaa.utils.ClickHintHandler;
import com.alaa.utils.getTimeUtils;
import com.alaa.viewmodels.ActivityModel;
import com.alaa.viewmodels.BusStopViewModel;
import com.alaa.viewmodels.PassengerRequestModel;
import com.alaa.viewmodels.PassengerState;
import com.google.android.material.snackbar.Snackbar;


public class PassengerRequestStatusFragment extends AnimationFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.request_status_layout, container, false);
    }

    private void refreshTextStatus(PassengerRequestModel passengerModel) {


        TextView textView = getView().findViewById(R.id.status_text);

        TransitionManager.beginDelayedTransition((ViewGroup) getView());
        if (passengerModel.status.getValue() > PassengerRequestModel.STATUS_NOT_SENT) {
            String value = "";
            ForegroundColorSpan span = null;

            switch (passengerModel.status.getValue()) {
                case PassengerRequestModel.STATUS_ACCEPTED:
                    value = "حالة الطلب : تم تزويد حافلة بنجاح";
                    span = new ForegroundColorSpan(Color.GREEN);
                    break;
                case PassengerRequestModel.STATUS_DENIED:
                    value = "حالة الطلب : لم يتم تزويد حافلة لعدم توافر الشروط المذكورة في الاسفل";
                    span = new ForegroundColorSpan(Color.RED);
                    break;
                case PassengerRequestModel.STATUS_PENDING:
                    value = "حالة الطلب : جاري تحليل الوضع الراهن للحافلات قد يستغرق الأمر خمس دقائق";
                    span = new ForegroundColorSpan(Color.rgb(0xFF, 0xDE, 0x03));
                    break;
            }
            Spannable string = new SpannableString(value);
            string.setSpan(span, 11, value.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(string);
            textView.setVisibility(View.VISIBLE);


        } else {
            textView.setVisibility(View.GONE);
        }


    }

    private static boolean Done = false;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {


        view.findViewById(R.id.arrow_back).setOnClickListener((v) -> {
            getActivity().onBackPressed();
        });

        ClickHintHandler handler = new ClickHintHandler(view.findViewById(R.id.help_button));

        view.findViewById(R.id.help_button).setOnClickListener((v) -> {
            handler.stop();
            Done = true;
            PopupMenu menu = new PopupMenu(requireActivity(), v);
            menu.inflate(R.menu.help);
            menu.show();

            menu.setOnMenuItemClickListener(item -> {

                switch (item.getItemId()) {
                    case R.id.advanceTime:
                        ((MapsActivity) requireActivity()).runSimulation();
                        return true;

                }

                return false;
            });
        });

        ViewModelProvider provider = new ViewModelProvider(requireActivity());
        PassengerRequestModel passengerModel = provider.get(PassengerRequestModel.class);


        passengerModel.status.observe(getViewLifecycleOwner(), (item) -> {
            refreshTextStatus(passengerModel);
            if (provider.get(PassengerRequestModel.class).status.getValue() == PassengerRequestModel.STATUS_PENDING) {
                if (!Done)
                    handler.start();
            }
            if (provider.get(PassengerRequestModel.class).status.getValue() != PassengerRequestModel.STATUS_NOT_SENT) {
                getView().findViewById(R.id.req_status_done).setVisibility(View.GONE);
            } else {
                getView().findViewById(R.id.req_status_done).setVisibility(View.VISIBLE);
            }

        });

        updateTrackTime(provider, true);

        if (passengerModel.status.getValue() != PassengerRequestModel.STATUS_NOT_SENT) {
            view.findViewById(R.id.req_status_done).setVisibility(View.GONE);
        } else {
            view.findViewById(R.id.req_status_done).setOnClickListener((v) -> {

                if (!ActivityModel.isSimulation)
                    Snackbar.make(getActivity().findViewById(android.R.id.content), "تم ارسال طلبك", Snackbar.LENGTH_SHORT).show();

                passengerModel.status.setValue(PassengerRequestModel.STATUS_PENDING);

                provider.get(ActivityModel.class).instant = getTimeUtils.getInstant();
                PassengerState state = provider.get(PassengerState.class);
                state.sendRequest(passengerModel.Feature, passengerModel.Route_ID);


                state.update();
                updateBackstack(true);
            });
        }


        view.findViewById(R.id.req_status_cancel).setOnClickListener((v) -> {

            if (!ActivityModel.isSimulation)
                Snackbar.make(getActivity().findViewById(android.R.id.content), "تم الغاء طلبك", Snackbar.LENGTH_SHORT).show();
            else Toast.makeText(getActivity(), "تم الغاء طلبك", Toast.LENGTH_LONG).show();

            passengerModel.status.setValue(PassengerRequestModel.STATUS_NOT_SENT);
            provider.get(ActivityModel.class).instant = null;

            provider.get(PassengerState.class).removeRequest();
            provider.get(PassengerState.class).update();

            updateBackstack(false);
        });


    }


    private void updateTrackTime(ViewModelProvider provider, boolean first_time) {

        PassengerRequestModel passengerModel = provider.get(PassengerRequestModel.class);
        ActivityModel activityModel = provider.get(ActivityModel.class);

        BusStopViewModel busStopViewModel = provider.get(BusStopViewModel.class);


        Application application = getActivity().getApplication();

        activityModel.exe.execute(() -> {

            String period = getTimeUtils.getTimePeriodString(busStopViewModel.getNearestBusInMinutes(passengerModel.Route_ID, passengerModel.Feature, application));

            if (first_time) {
                activityModel.mainHandelr.post(getViewLifecycleOwner(), () -> {
                    TransitionManager.beginDelayedTransition((ViewGroup) getView());
                    ((TextView) getView().findViewById(R.id.req_status_est_time)).setText(period);
                    updateTrackTime(provider, false);
                });
            } else {
                activityModel.mainHandelr.postDelayed(this, () -> {
                    TransitionManager.beginDelayedTransition((ViewGroup) getView());
                    ((TextView) getView().findViewById(R.id.req_status_est_time)).setText(period);
                    updateTrackTime(provider, false);
                }, 10000);
            }

        });
    }


    public void updateBackstack(boolean reinstantiate) {
        if (getParentFragmentManager().getBackStackEntryCount() == 3) {
            getParentFragmentManager().popBackStack();
            getParentFragmentManager().popBackStack();
        } else {
            getParentFragmentManager().popBackStack();
        }
        if (reinstantiate)
            getParentFragmentManager().beginTransaction().replace(android.R.id.content, new PassengerRequestStatusFragment()).addToBackStack(null).commit();
        getParentFragmentManager().executePendingTransactions();

    }


}
