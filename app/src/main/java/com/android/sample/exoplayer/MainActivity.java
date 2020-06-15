package com.android.sample.exoplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import static com.android.sample.exoplayer.MainService.SAMPLE_ID;
import static com.android.sample.exoplayer.MainService.STR_RECEIVER;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private PlayerView mPlayerView;
    private BottomSheetBehavior bottomSheetBehavior;

    /**
     * Create our connection to the service to be used in our bindService call.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            //We expect the service binder to be the main services binder.
            //As such we cast.
            if (service instanceof MainService.MainServiceBinder) {
                MainService.MainServiceBinder myService = (MainService.MainServiceBinder) service;
                //Then we simply set the exoplayer instance on this view.
                mPlayerView.setPlayer(myService.getExoPlayerInstance());
                mPlayerView.setDefaultArtwork(Sample.getComposerArtBySampleID(MainActivity.this, myService.getSampleId()));
            }
        }

        public void onServiceDisconnected(ComponentName className) {
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int sampleId = intent.getIntExtra(SAMPLE_ID, -1);
            mPlayerView.setDefaultArtwork(Sample.getComposerArtBySampleID(MainActivity.this, sampleId));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the player view.
        mPlayerView = findViewById(R.id.playerView);

        View bottomNavigationContainer = findViewById(R.id.bottom_navigation_container);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomNavigationContainer);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    // do stuff when the drawer is expanded
                }

                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    // do stuff when the drawer is collapsed
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // do stuff during the actual drag event for example
                // animating a background color change based on the offset
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();
        //Start the service up with video playback information.
        Intent intent = new Intent(this, MainService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        registerReceiver(mBroadcastReceiver, new IntentFilter(STR_RECEIVER));
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        super.onStop();
        unbindService(mConnection);
    }
}