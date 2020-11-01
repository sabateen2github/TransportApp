package com.alaa.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.alaa.transportapp.R;
import com.alaa.utils.AnimationFragment;

public class DriverEnterIDFragment extends AnimationFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.driver_enter_id, container, false);

    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.arrow_back).setOnClickListener(v -> getActivity().onBackPressed());

        Button nextBtn = getView().findViewById(R.id.driver_enter_id_next);

        SimpleModel model = new ViewModelProvider(this).get(SimpleModel.class);

        if (model.ID.length() == 8) {
            nextBtn.setText("التالي");
        } else {
            nextBtn.setText("رقم التسجيل يتكون من سبع خانات");
        }
        nextBtn.setOnClickListener(v -> {
            if (model.ID.length() != 8) return;
            getParentFragmentManager().popBackStack();
            getParentFragmentManager().beginTransaction().replace(android.R.id.content, new DriverPageFragment()).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).addToBackStack(null).commit();
        });


        EditText editText = view.findViewById(R.id.driver_enter_id_edit_text);
        editText.setText(model.ID);
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            boolean editing = false;

            @Override
            public void afterTextChanged(Editable editable) {
                if (editing) {
                    return;
                }

                editing = true;
                String value = editable.toString().replace("-", "");

                if (value.length() >= 3) {
                    value = value.substring(0, 2) + '-' + value.substring(2);
                }
                editable.clear();
                editable.append(value);

                if (value.length() == 8) {
                    if (!nextBtn.isEnabled()) {
                        TransitionManager.beginDelayedTransition((ViewGroup) view);
                        nextBtn.setEnabled(true);
                        nextBtn.setText("التالي");
                    }
                } else {
                    if (nextBtn.isEnabled()) {
                        TransitionManager.beginDelayedTransition((ViewGroup) view);
                        nextBtn.setEnabled(false);
                        nextBtn.setText("رقم التسجيل يتكون من سبع خانات");

                    }
                }

                model.ID = value;

                editing = false;
            }
        });
    }

    public static class SimpleModel extends ViewModel {
        String ID = "";
    }
}
