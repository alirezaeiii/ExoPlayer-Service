package com.android.sample.exoplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.Objects;

import static com.android.sample.exoplayer.MainService.IS_PLAYING;
import static com.android.sample.exoplayer.MainService.SAMPLE;
import static com.android.sample.exoplayer.MainService.STR_RECEIVER_ACTIVITY;
import static com.android.sample.exoplayer.MainService.STR_RECEIVER_SERVICE;
import static com.android.sample.exoplayer.MainUtil.ONE_SECOND;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final long DELAY = ONE_SECOND >> 1;
    private PlayerView mPlayerView;
    private BottomSheetBehavior<FrameLayout> mBottomSheetBehavior;
    private ImageButton mBtnPlayPause;
    private VectorDrawable mPlayDrawable;
    private VectorDrawable mPauseDrawable;
    private TextView mTxtSong;
    private TextView mTxtComposer;
    private ConstraintLayout mBottomBar;
    private ProgressBar mProgressBar;
    private ImageView mArrow;
    private boolean isPlaying = true;
    private boolean shouldAnimate;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Create our connection to the service to be used in our bindService call.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            //We expect the service binder to be the main services binder.
            //As such we cast.
            if (service instanceof MainService.MainServiceBinder) {
                final MainService.MainServiceBinder myService = (MainService.MainServiceBinder) service;
                //Then we simply set the exoplayer instance on this view.
                mPlayerView.setPlayer(myService.getExoPlayerInstance());
                mPlayerView.setDefaultArtwork(Sample.getComposerArtBySampleID(
                        MainActivity.this,
                        myService.getSample().getSampleID()));
                mTxtSong.setText(myService.getSample().getTitle());
                mTxtComposer.setText(myService.getSample().getComposer());
                isPlaying = myService.getExoPlayerInstance().getPlayWhenReady();
                mBtnPlayPause.setImageDrawable(isPlaying ? mPauseDrawable : mPlayDrawable);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (shouldAnimate && mProgressBar.getProgress() < myService.getExoPlayerInstance().getCurrentPosition()) {
                            ProgressBarAnimation anim = new ProgressBarAnimation(mProgressBar,
                                    mProgressBar.getProgress(), myService.getExoPlayerInstance().getCurrentPosition());
                            anim.setDuration(DELAY);
                            mProgressBar.startAnimation(anim);
                            shouldAnimate = false;
                        } else {
                            mProgressBar.setProgress((int) myService.getExoPlayerInstance().getCurrentPosition());
                        }
                        mHandler.postDelayed(this, isPlaying ? 0 : ONE_SECOND);
                    }
                });
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mProgressBar.setMax((int) myService.getExoPlayerInstance().getDuration());
                        mHandler.postDelayed(this, isPlaying ? 0 : ONE_SECOND);
                    }
                }, DELAY);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
        }
    };

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(IS_PLAYING)) {
                isPlaying = intent.getBooleanExtra(IS_PLAYING, false);
                mBtnPlayPause.setImageDrawable(isPlaying ? mPauseDrawable : mPlayDrawable);
            } else if (intent.hasExtra(SAMPLE)) {
                Sample sample = intent.getParcelableExtra(SAMPLE);
                mPlayerView.setDefaultArtwork(Sample.getComposerArtBySampleID(
                        MainActivity.this,
                        Objects.requireNonNull(sample).getSampleID()));
                mTxtSong.setText(sample.getTitle());
                mTxtComposer.setText(sample.getComposer());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the player view.
        mPlayerView = findViewById(R.id.playerView);

        mBtnPlayPause = findViewById(R.id.btn_play_pause);
        mPlayDrawable = (VectorDrawable) getDrawable(R.drawable.exo_controls_play);
        mPauseDrawable = (VectorDrawable) getDrawable(R.drawable.exo_controls_pause);
        mTxtSong = findViewById(R.id.txt_song);
        mTxtComposer = findViewById(R.id.txt_composer);
        mBottomBar = findViewById(R.id.bottom_bar);
        mProgressBar = findViewById(R.id.progress);
        mArrow = findViewById(R.id.arrow);

        FrameLayout bottomNavigationContainer = findViewById(R.id.bottom_navigation_container);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomNavigationContainer);
        mBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                float alpha = (float) (1 - (slideOffset * 2.8));
                mBtnPlayPause.setEnabled(alpha > 0);
                mBottomBar.setAlpha(alpha);
                mProgressBar.setAlpha(alpha);
                mArrow.setRotation(slideOffset * -180);
            }
        });
        FrameLayout bottomLayout = findViewById(R.id.bottom_layout);
        bottomLayout.setOnClickListener(this);
    }

    @Override
    public void onBackPressed() {
        if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
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
        this.shouldAnimate = true;
        registerReceiver(mBroadcastReceiver, new IntentFilter(STR_RECEIVER_ACTIVITY));
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
        mHandler.removeCallbacksAndMessages(null);
        unbindService(mConnection);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.bottom_layout) {
            if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        }
    }

    public void playPauseClick(View view) {
        isPlaying = !isPlaying;
        mBtnPlayPause.setImageDrawable(isPlaying ? mPauseDrawable : mPlayDrawable);
        Intent intent = new Intent(STR_RECEIVER_SERVICE);
        intent.putExtra(IS_PLAYING, isPlaying);
        sendBroadcast(intent);
    }
}