package com.example.routine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ScreenReceiver extends BroadcastReceiver {
    private String screen;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            screen = "off";
//            Log.d("broadcast", "off");
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            screen = "on";
//            Log.d("broadcast", "on");
        } else if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
            screen = "unlock";
//            Log.d("broadcast", "unlock");
        } else if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            screen = "booted";
        } else if(intent.getAction().equals(Intent.ACTION_SHUTDOWN) || intent.getAction().equals(Intent.ACTION_REBOOT)) {
            screen = "shutdown";
        }
        Intent i = new Intent(context, UpdateService.class);
//        i.putExtra("activity", this.toString());
        i.putExtra("screen_state", screen);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i);
        } else {
            context.startService(i);
        }
    }
}
