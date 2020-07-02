package com.android.sample.exoplayer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

class MainUtil {

    private static final String TAG = MainUtil.class.getSimpleName();
    public static final long ONE_SECOND = 1000;

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void startMainService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "startForegroundService()");
            context.startForegroundService(intent);
            Intent myIntent = new Intent(context, MainService.class);
            context.startService(myIntent);
        } else {
            context.startService(intent);
        }
    }
}
