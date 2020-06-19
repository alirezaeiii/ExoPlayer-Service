package com.android.sample.exoplayer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

public class Utils {

    static void startService(Class<?> serviceClass, Context context) {
        if (!isServiceRunning(serviceClass, context)) {
            context.startService(new Intent(context, MainService.class));
        }
    }

    private static boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
