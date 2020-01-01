package com.example.routine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int code = intent.getIntExtra("requestCode", 0);
        int hr = intent.getIntExtra("hr", 0);
        int min = intent.getIntExtra("min", 0);
        String medI = intent.getStringExtra("med");
        Log.d("code", code+"");
        if(code == 0) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("count", 0).apply();

            int count = 1;

            String med = "";
            String strSchedule3 = PreferenceManager.getDefaultSharedPreferences(context).getString("alarm3", "");
            for (String string : strSchedule3.split(", ")) {
                if(!string.contains(":")) {
                    med = string;
                    continue;
                }
                if(string.contains(";")) continue;
                String[] strings = string.split(":");
                int minOfHr = Integer.parseInt(strings[0])*60+Integer.parseInt(strings[1]);
                AlarmActivity.setAlarm(context, true, false, count++, (minOfHr+21)/60, (minOfHr+21)%60, med+"&"+string);
                AlarmActivity.setAlarm(context, true, true, count++, (minOfHr)/60, (minOfHr)%60, med+"&"+string);
                AlarmActivity.setAlarm(context, true, false, count++, (minOfHr+20)/60, (minOfHr+20)%60, med+"&"+string);
            }
        } else if(code%3 == 1){
        } else if(code%3 == 2){
            Intent i = new Intent(context, AlarmActivity.class);
            i.putExtra("code", code);
            i.putExtra("hr", hr);
            i.putExtra("min", min);
            i.putExtra("med", medI);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            context.startActivity(i);
        } else if(code%3 == 0){
            AlarmActivity.cancelAlarm(context, code-1);
            AlarmActivity.stopVibrate(context);
            Intent i = new Intent(context, UpdateService.class);
            i.putExtra("screen_state", "alarmOff");
            context.startService(i);
        }
    }
}
