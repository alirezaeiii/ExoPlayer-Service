package com.android.sample.exoplayer;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ServiceUtils {

    private ServiceUtils() {
    }

    public static void startMainService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        }
        context.startService(intent);
    }
}

