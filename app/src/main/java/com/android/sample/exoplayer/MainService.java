package com.android.sample.exoplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.List;

public class MainService extends Service implements ExoPlayer.EventListener {

    private static final String TAG = MainService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 1;
    private static final long MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000;
    static final String STR_RECEIVER_ACTIVITY = "com.MainService.receiver.activity";
    static final String STR_RECEIVER_SERVICE = "com.MainService.receiver.service";
    static final String STR_RECEIVER_SERVICE_STORAGE = "com.MainService.receiver.service.storage";
    static final String SAMPLE = "sample";
    static final String IS_PLAYING = "isPlaying";
    private SimpleExoPlayer mExoPlayer;
    private static MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;
    private List<Sample> mSamples = new ArrayList<>();

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isPlaying = intent.getBooleanExtra(IS_PLAYING, false);
            mExoPlayer.setPlayWhenReady(isPlaying);
        }
    };

    private BroadcastReceiver mStorageBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!mExoPlayer.getPlayWhenReady()) {
                MainPosition mainPosition = new MainPosition(mExoPlayer.getCurrentWindowIndex(),
                        mExoPlayer.getCurrentPosition());
                Storage.getInstance(MainService.this).storePosition(mainPosition);
            }
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();

        // Initialize the Media Session.
        initializeMediaSession();

        // Initialize the player.
        initializePlayer();

        registerReceiver(mBroadcastReceiver, new IntentFilter(STR_RECEIVER_SERVICE));
        registerReceiver(mStorageBroadcastReceiver, new IntentFilter(STR_RECEIVER_SERVICE_STORAGE));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MainServiceBinder();
    }

    /**
     * Initializes the Media Session to be enabled with media buttons, transport controls, callbacks
     * and media controller.
     */
    private void initializeMediaSession() {

        // Create a MediaSessionCompat.
        mMediaSession = new MediaSessionCompat(this, TAG);

        // Enable callbacks from MediaButtons and TransportControls.
        // noinspection deprecation
        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Do not let MediaButtons restart the player when the app is not visible.
        mMediaSession.setMediaButtonReceiver(null);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player.
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);

        mMediaSession.setPlaybackState(mStateBuilder.build());


        // MySessionCallback has methods that handle callbacks from a media controller.
        mMediaSession.setCallback(new MySessionCallback());

        // Start the Media Session since the activity is active.
        mMediaSession.setActive(true);
    }

    /**
     * Shows Media Style notification, with an action that depends on the current MediaSession
     * PlaybackState.
     *
     * @param state  The PlaybackState of the MediaSession.
     * @param sample The Sample object to display title and composer on Notification.
     */
    private void showNotification(PlaybackStateCompat state, Sample sample) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "Id");

        int icon;
        String play_pause;
        if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
            icon = R.drawable.exo_controls_pause;
            play_pause = getString(R.string.pause);
        } else {
            icon = R.drawable.exo_controls_play;
            play_pause = getString(R.string.play);
        }

        NotificationCompat.Action playPauseAction = new NotificationCompat.Action(
                icon, play_pause,
                MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE));

        NotificationCompat.Action restartAction = new NotificationCompat
                .Action(R.drawable.exo_controls_previous, getString(R.string.restart),
                MediaButtonReceiver.buildMediaButtonPendingIntent
                        (this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));

        NotificationCompat.Action nextAction = new NotificationCompat
                .Action(R.drawable.exo_controls_next, getString(R.string.next),
                MediaButtonReceiver.buildMediaButtonPendingIntent
                        (this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));

        PendingIntent contentPendingIntent = PendingIntent.getActivity
                (this, 0, new Intent(this, MainActivity.class), 0);

        Intent intent = new Intent(this, StopServiceBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0, intent, 0);

        builder.setContentTitle(sample.getTitle())
                .setContentText(sample.getComposer())
                .setContentIntent(contentPendingIntent)
                .setDeleteIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_music_note)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(restartAction)
                .addAction(playPauseAction)
                .addAction(nextAction).setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mMediaSession.getSessionToken())
                .setShowActionsInCompactView(1, 2));

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "Id";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channelId);
        }

        Notification notificationCompat = builder.build();
        if (state.getState() == PlaybackStateCompat.STATE_PAUSED) {
            stopForeground(false);
            notificationManager.notify(NOTIFICATION_ID, notificationCompat);
        } else {
            startForeground(NOTIFICATION_ID, notificationCompat);
        }
    }

    /**
     * Initialize ExoPlayer.
     */
    private void initializePlayer() {
        if (mExoPlayer == null) {
            // Create an instance of the ExoPlayer.
            mExoPlayer = new SimpleExoPlayer.Builder(this).build();

            List<Integer> sampleIDs = Sample.getAllSampleIDs(this);
            MediaSource[] mediaSourcesToLoad = new MediaSource[sampleIDs.size()];

            for (int i = 0; i < sampleIDs.size(); i++) {
                Sample sample = Sample.getSampleByID(this, sampleIDs.get(i));
                if (sample == null) {
                    Toast.makeText(this, getString(R.string.sample_not_found_error),
                            Toast.LENGTH_SHORT).show();
                } else {
                    mSamples.add(sample);
                    // Prepare the MediaSource.
                    String userAgent = Util.getUserAgent(this, "ExoPlayer");
                    MediaSource mediaSource = new ProgressiveMediaSource.Factory(new DefaultDataSourceFactory(
                            this, userAgent)).createMediaSource(Uri.parse(sample.getUri()));
                    mediaSourcesToLoad[i] = mediaSource;
                }
            }
            mExoPlayer.prepare(new ConcatenatingMediaSource(mediaSourcesToLoad));

            MainPosition currentPosition = Storage.getInstance(this).getPosition();
            if(currentPosition == null) {
                mExoPlayer.setPlayWhenReady(true);
            } else {
                mExoPlayer.seekTo(currentPosition.getCurrentWindowIndex(), currentPosition.getCurrentPosition());
                mExoPlayer.setPlayWhenReady(false);
            }

            // Set the ExoPlayer.EventListener to this service.
            mExoPlayer.addListener(this);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved()");
        super.onTaskRemoved(rootIntent);
        if (!mExoPlayer.getPlayWhenReady()) {
            stopSelf();
        }
    }

    /**
     * Release ExoPlayer.
     */
    private void releasePlayer() {
        mExoPlayer.removeListener(this);
        mExoPlayer.stop();
        mExoPlayer.release();
        mExoPlayer = null;
    }

    /**
     * Release the player when the service is destroyed.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        MainPosition mainPosition = new MainPosition(mExoPlayer.getCurrentWindowIndex(),
                mExoPlayer.getCurrentPosition());
        Storage.getInstance(this).storePosition(mainPosition);
        releasePlayer();
        unregisterReceiver(mBroadcastReceiver);
        unregisterReceiver(mStorageBroadcastReceiver);
        mMediaSession.setActive(false);
    }

    // ExoPlayer Event Listeners

    /**
     * Method that is called when the ExoPlayer state changes. Used to update the MediaSession
     * PlayBackState to keep in sync.
     *
     * @param playWhenReady true if ExoPlayer is playing, false if it's paused.
     * @param playbackState int describing the state of ExoPlayer. Can be STATE_READY, STATE_IDLE,
     *                      STATE_BUFFERING, or STATE_ENDED.
     */
    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if ((playbackState == ExoPlayer.STATE_READY) && playWhenReady) {
            mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                    mExoPlayer.getCurrentPosition(), 1f);
        } else if ((playbackState == ExoPlayer.STATE_READY)) {
            mStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                    mExoPlayer.getCurrentPosition(), 1f);
        }
        updateNotificationAndDrawable();
    }

    @Override
    public void onPositionDiscontinuity(int reason) {
        updateNotificationAndDrawable();
    }

    private void updateNotificationAndDrawable() {
        mMediaSession.setPlaybackState(mStateBuilder.build());
        Sample sample = mSamples.get(mExoPlayer.getCurrentWindowIndex());
        Intent intent = new Intent(STR_RECEIVER_ACTIVITY);
        intent.putExtra(SAMPLE, sample);
        intent.putExtra(IS_PLAYING, mExoPlayer.getPlayWhenReady());
        sendBroadcast(intent);
        showNotification(mStateBuilder.build(), sample);
    }


    /**
     * Media Session Callbacks, where all external clients control the player.
     */
    private class MySessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            Utils.startService(MainService.class, MainService.this);
            mExoPlayer.setPlayWhenReady(true);
        }

        @Override
        public void onPause() {
            Utils.startService(MainService.class, MainService.this);
            mExoPlayer.setPlayWhenReady(false);
        }

        @Override
        public void onSkipToPrevious() {
            Utils.startService(MainService.class, MainService.this);
            if (mExoPlayer.getCurrentPosition() <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS) {
                mExoPlayer.previous();
            } else {
                mExoPlayer.seekTo(0);
            }
        }

        @Override
        public void onSkipToNext() {
            Utils.startService(MainService.class, MainService.this);
            mExoPlayer.next();
        }
    }

    /**
     * This class will be what is returned when an activity binds to this service.
     * The activity will also use this to know what it can get from our service to know
     * about the video playback.
     */
    class MainServiceBinder extends Binder {

        /**
         * This method should be used only for setting the exoplayer instance.
         * If exoplayer's internal are altered or accessed we can not guarantee
         * things will work correctly.
         */
        SimpleExoPlayer getExoPlayerInstance() {
            return mExoPlayer;
        }

        Sample getSample() {
            return mSamples.get(mExoPlayer.getCurrentWindowIndex());
        }
    }

    /**
     * Broadcast Receiver registered to receive the MEDIA_BUTTON intent coming from clients.
     */
    public static class MediaReceiver extends BroadcastReceiver {

        public MediaReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "MediaReceiver$onReceive()");
            Utils.startService(MainService.class, context);
            MediaButtonReceiver.handleIntent(mMediaSession, intent);
        }
    }

    public static class StopServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "StopServiceBroadcastReceiver$onReceive()");
            Intent stopIntent = new Intent(context, MainService.class);
            context.stopService(stopIntent);
        }
    }
}
