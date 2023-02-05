package com.android.sample.exoplayer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.Objects;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static com.android.sample.exoplayer.Constants.ONE_SECOND;
import static com.android.sample.exoplayer.MainService.EXO_PLAYER_PLAYING_SUBJECT;
import static com.android.sample.exoplayer.RxMainSubject.unsubscribe;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final long UPDATE_PROGRESS_DELAY = ONE_SECOND >> 3;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
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
    static final RxMainSubject<Boolean> PLAYING_SUBJECT = new RxMainSubject<>();
    static final RxMainSubject<Sample> SAMPLE_SUBJECT = new RxMainSubject<>();

    /**
     * Create our connection to the service to be used in our bindService call.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
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
                        long duration = myService.getExoPlayerInstance().getDuration();
                        if (duration != C.TIME_UNSET) {
                            mProgressBar.setMax((int) duration);
                            mProgressBar.setProgress((int) myService.getExoPlayerInstance().getCurrentPosition());
                        }
                        mHandler.postDelayed(this, isPlaying ? 0 : UPDATE_PROGRESS_DELAY);
                    }
                });
            }
        }

        public void onServiceDisconnected(ComponentName className) {
        }
    };

    private final Disposable mPlayingDisposable = PLAYING_SUBJECT.subscribe(new Consumer<Boolean>() {
        @Override
        public void accept(Boolean isPlaying) {
            MainActivity.this.isPlaying = isPlaying;
            mBtnPlayPause.setImageDrawable(isPlaying ? mPauseDrawable : mPlayDrawable);
        }
    });

    private final Disposable mSampleDisposable = SAMPLE_SUBJECT.subscribe(new Consumer<Sample>() {
        @Override
        public void accept(Sample sample) {
            mPlayerView.setDefaultArtwork(Sample.getComposerArtBySampleID(
                    MainActivity.this,
                    Objects.requireNonNull(sample).getSampleID()));
            mTxtSong.setText(sample.getTitle());
            mTxtComposer.setText(sample.getComposer());
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        super.onStart();
        Intent intent = new Intent(this, MainService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHandler.removeCallbacksAndMessages(null);
        unbindService(mConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unsubscribe(mPlayingDisposable, mSampleDisposable);
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
        EXO_PLAYER_PLAYING_SUBJECT.publish(isPlaying);
    }
}