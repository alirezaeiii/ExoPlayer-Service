package com.android.sample.exoplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;

public class MainActivity extends AppCompatActivity implements ExoPlayer.EventListener, MainService.ComposerCallback {

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
                myService.setComposerCallback(MainActivity.this);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
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
    public void setComposerDrawable(Drawable d) {
        mPlayerView.setDefaultArtwork(d);
    }
}
