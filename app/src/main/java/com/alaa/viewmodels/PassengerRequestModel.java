package com.alaa.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PassengerRequestModel extends ViewModel {
    public ActivityModel.PointsStructure.Feature Feature;
    public String Route_ID;
    public MutableLiveData<Integer> status = new MutableLiveData<>(STATUS_NOT_SENT);

    public static final int STATUS_NOT_SENT = 1;
    public static final int STATUS_PENDING = 2;
    public static final int STATUS_DENIED = 3;
    public static final int STATUS_ACCEPTED = 4;

}
