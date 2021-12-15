package com.alaa.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.alaa.transportapp.GeoEventsHelper;
import com.alaa.transportapp.MapsActivity;
import com.alaa.transportapp.R;
import com.alaa.utils.AnimationFragment;
import com.alaa.viewmodels.ActivityModel;
import com.alaa.viewmodels.BusStopViewModel;
import com.alaa.viewmodels.ChooseRouteViewModel;

public class ChooseRouteFragment extends AnimationFragment {


    private BusStopViewModel mState;
    private ChooseRouteViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.choose_route_fragment, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewModelProvider provider = new ViewModelProvider(requireActivity());
        mState = provider.get(BusStopViewModel.class);
        viewModel = provider.get(ChooseRouteViewModel.class);
        ActivityModel activityModel = provider.get(ActivityModel.class);

        View progressBar = view.findViewById(R.id.progress_bar);
        View arrowBack = view.findViewById(R.id.arrow_back);
        RecyclerView recyclerView = view.findViewById(R.id.choose_route_fragment_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new Adapter());
        arrowBack.setOnClickListener((v) -> {
            getParentFragmentManager().popBackStack();
        });


        activityModel.exe.execute(() -> {
            viewModel.Properties = BusStopViewModel.Properties.getProperties(getActivity().getApplication(), mState.SelectedFeature);

            activityModel.mainHandelr.post(getViewLifecycleOwner(), () -> {

                TransitionManager.beginDelayedTransition((ViewGroup) getView());
                recyclerView.getAdapter().notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

            });
        });

    }

    private class Holder extends RecyclerView.ViewHolder {
        TextView RouteName;

        public Holder(@NonNull View itemView) {
            super(itemView);
            RouteName = itemView.findViewById(R.id.route_name);
        }
    }

    private class Adapter extends RecyclerView.Adapter<Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(getLayoutInflater().inflate(R.layout.lift_rquest_route_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.RouteName.setText(viewModel.Properties.fromTos[position + 1].toString());
            holder.itemView.setOnClickListener((v) -> {
                if (getArguments() != null && getArguments().containsKey(MapsActivity.FRAGMENT_CLASS_KEY)) {
                    new GeoEventsHelper().registerNotificationResponse(getContext(), viewModel.Properties.mFeature, viewModel.Properties.names[position]);
                    getActivity().onBackPressed();
                    return;
                }
                mState.Filter = viewModel.Properties.names[position];
                getParentFragmentManager().beginTransaction().replace(android.R.id.content, new BusStopFragment()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).commit();
            });
        }

        @Override
        public int getItemCount() {
            if (viewModel.Properties == null) return 0;
            return viewModel.Properties.names.length;
        }
    }

}
