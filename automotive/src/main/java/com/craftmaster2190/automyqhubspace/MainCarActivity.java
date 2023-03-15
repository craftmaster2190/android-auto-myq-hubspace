package com.craftmaster2190.automyqhubspace;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import com.craftmaster2190.automyqhubspace.R;
import com.craftmaster2190.automyqhubspace.databinding.ActivityMainVerticalBinding;
import com.craftmaster2190.automyqhubspace.ui.login.AppCredentials;
import com.craftmaster2190.automyqhubspace.ui.login.LoginActivity;
import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;

public class MainCarActivity extends CarActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(this.getClass().getSimpleName(), "onCreate start");
        super.onCreate(savedInstanceState);
        setIgnoreConfigChanges(0xFFFFFFFF);
        CarUiController ctrl = getCarUiController();
        ctrl.getStatusBarController().hideAppHeader();
        ctrl.getStatusBarController().hideTitle();
        ctrl.getMenuController().hideMenuButton();
        setTheme(R.style.Theme_AutoMyqHubspace);

        if (AppCredentials.fromContext(this) == null) {
            setContentView(R.layout.activity_car_please_login);
        } else {
            setContentView(R.layout.activity_main_horizontal);
        }

        Log.i(this.getClass().getSimpleName(), "onCreate finish");
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        Log.i(this.getClass().getSimpleName(), "Configuration changed: " + configuration);
        super.onConfigurationChanged(configuration);
    }
}
