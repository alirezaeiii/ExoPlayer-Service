package com.android.sample.exoplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ui.PlayerView;

import static com.android.sample.exoplayer.MainService.SAMPLE_ID;
import static com.android.sample.exoplayer.MainService.STR_RECEIVER;

public class MainActivity extends AppCompatActivity {

    private PlayerView mPlayerView;

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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the player view.
        mPlayerView = findViewById(R.id.playerView);

        //Start the service up with video playback information.
        Intent intent = new Intent(this, MainService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReceiver, new IntentFilter(STR_RECEIVER));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }
}
