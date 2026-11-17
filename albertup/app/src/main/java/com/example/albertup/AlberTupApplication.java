package com.example.albertup;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.albertup.data.AppSettings;

/** Applies the saved theme before the first screen is drawn. */
public class AlberTupApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(new AppSettings(this).getNightMode());
    }
}
