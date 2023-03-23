package com.craftmaster2190.automyqhubspace;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.craftmaster2190.automyqhubspace.databinding.ActivityMainPhoneVerticalBinding;
import com.craftmaster2190.automyqhubspace.ui.login.AppCredentials;
import com.craftmaster2190.automyqhubspace.ui.login.LoginActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainPhoneVerticalBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AppCredentials.fromContext(this) == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            binding = ActivityMainPhoneVerticalBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        }
    }
}