package com.android.sample.exoplayer;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

class MainStorage {

    private static final String MY_PREFERENCES = "MyPrefs";
    private static final String CURRENT_POSITION = "position";
    private static MainStorage sInstance;
    private SharedPreferences mSharedPref;

    private MainStorage(Context context) {
        mSharedPref = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
    }

    public synchronized static MainStorage getInstance(Context context) {
        if (sInstance == null) {
            //Use application Context to prevent leak.
            sInstance = new MainStorage(context.getApplicationContext());
        }
        return sInstance;
    }

    public void storePosition(MainPosition currentPosition) {
        String json = new Gson().toJson(currentPosition);
        SharedPreferences.Editor editor = mSharedPref.edit();
        editor.putString(CURRENT_POSITION, json);
        editor.apply();
    }

    public MainPosition getPosition() {
        String json = mSharedPref.getString(CURRENT_POSITION, null);
        return new Gson().fromJson(json, MainPosition.class);
    }
}
