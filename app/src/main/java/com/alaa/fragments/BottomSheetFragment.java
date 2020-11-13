package com.alaa.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.alaa.transportapp.R;
import com.alaa.viewmodels.BusStopViewModel;
import com.alaa.viewmodels.FindPathModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;


public class BottomSheetFragment extends BottomSheetDialogFragment {


    public static final String BusStopArgumentKey = "bus_Stop_argument_key";

    FindPathModel mViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.from_routes, container, false);
    }

    private static final String[] array = {
            "نقطة ركوب الحافلة الأولى",
            "نقطة ركوب الحافلة الثانية",
            "نقطة ركوب الحافلة الثالثة",
            "نقطة ركوب الحافلة الرابعة",
            "نقطة ركوب الحافلة الخامسة",
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(requireActivity()).get(FindPathModel.class);


        ViewModelProvider provider = new ViewModelProvider(requireActivity());
        if (mViewModel.selectedFeature.stepIndex == mViewModel.steps.getValue().size()) {

            ((TextView) view.findViewById(R.id.find_path_step_id)).setText("آخر نقطة نزول");
            ((TextView) view.findViewById(R.id.find_path_hint)).setText("هذه اقرب نقطة ممكنة لوجهتك يمكن لخدمة النقل العام ايصالك لها");
            view.findViewById(R.id.find_path_show_schedule).setVisibility(View.GONE);


        } else {
            ((TextView) view.findViewById(R.id.find_path_step_id)).setText(array[mViewModel.selectedFeature.stepIndex - 1]);
            LinearLayout items_container = view.findViewById(R.id.items_container);
            for (String route : mViewModel.selectedFeature.step.receive) {
                View item = getActivity().getLayoutInflater().inflate(R.layout.find_path_route_item, items_container, false);
                ((TextView) item.findViewById(R.id.item_id)).setText(route);
                items_container.addView(item);
            }
        }

        view.findViewById(R.id.find_path_show_schedule).setOnClickListener((v) -> {
            BusStopFragment fragment = new BusStopFragment();
            Bundle args = new Bundle();
            args.putBoolean(BusStopArgumentKey, true);
            fragment.setArguments(args);
            dismiss();
            provider.get(BusStopViewModel.class).SelectedFeature = mViewModel.selectedFeature.feature;
            getParentFragmentManager().beginTransaction().replace(android.R.id.content, fragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).commit();
        });


    }
}
