package com.alaa.utils;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AnimationFragment extends Fragment {


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //TransitionManager.beginDelayedTransition(getActivity().findViewById(android.R.id.content));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //TransitionManager.beginDelayedTransition(getActivity().findViewById(android.R.id.content));
    }
}
