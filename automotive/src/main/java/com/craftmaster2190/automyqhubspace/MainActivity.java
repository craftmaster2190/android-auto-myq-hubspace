package com.craftmaster2190.automyqhubspace;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.craftmaster2190.automyqhubspace.databinding.ActivityMainVerticalBinding;
import com.craftmaster2190.automyqhubspace.ui.login.AppCredentials;
import com.craftmaster2190.automyqhubspace.ui.login.LoginActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainVerticalBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AppCredentials.fromContext(this) == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            binding = ActivityMainVerticalBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        }
    }
}