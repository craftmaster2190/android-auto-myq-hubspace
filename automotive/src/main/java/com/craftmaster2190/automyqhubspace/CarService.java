package com.craftmaster2190.automyqhubspace;

import android.util.Log;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;

public class CarService extends CarActivityService {

    @Override
    public Class<? extends CarActivity> getCarActivity() {
        Log.i(this.getClass().getSimpleName(), "getCarActivity start");
        return MainCarActivity.class;
    }

}
