package com.android.sample.exoplayer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

class MainUtil {

    private static final String TAG = MainUtil.class.getSimpleName();

    public static boolean isServiceRunning(Class<?> serviceClass, Context context) {
        return getRunningServiceInfo(context, serviceClass) != null;
    }

    public static boolean isServiceRunningInForeground(Class<?> serviceClass, Context context) {
        ActivityManager.RunningServiceInfo service = getRunningServiceInfo(context, serviceClass);
        return (service != null && service.foreground);
    }

    private static ActivityManager.RunningServiceInfo getRunningServiceInfo(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return service;
            }
        }
        return null;
    }

    public static void startMainService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "startForegroundService()");
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

}
