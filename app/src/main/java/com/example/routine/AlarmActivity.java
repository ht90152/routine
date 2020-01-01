package com.example.routine;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class AlarmActivity extends AppCompatActivity {
    private TextView textView;
    private boolean mActive = true;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        final Boolean vibrate = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("vibrate", true);
        final Boolean sound = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("sound", false);

        if (vibrate) {
            startVibrate();
        }

        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        final MediaPlayer player = MediaPlayer.create(AlarmActivity.this, alarmUri);
        if (sound) {
            Ringtone ringtone = RingtoneManager.getRingtone(AlarmActivity.this, alarmUri);
            ringtone.play();
            player.setLooping(true);
            player.start();
//            Log.d("play", player.isPlaying()+"");
        }

        int code = getIntent().getIntExtra("code", 0);
        if(code%3 == 2) {
            Intent intent = new Intent(AlarmActivity.this, UpdateService.class);
            intent.putExtra("screen_state", "alarmOn");
            startService(intent);

            new CountDownTimer(1000 * 60, 1000) { // adjust the milli seconds here
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    if (mActive) {
                        if (vibrate) {
                            stopVibrate(AlarmActivity.this);
                        }
                        if (sound) {
                            player.stop();
                            player.release();
                        }
                        finishAffinity();
                    }
                }
            }.start();

            Button button = findViewById(R.id.button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (vibrate) {
                        stopVibrate(AlarmActivity.this);
                    }
                    if (sound) {
                        player.stop();
                        player.release();
                    }
                    int code = getIntent().getIntExtra("code", 0);
                    Log.d("code", code + "");
                    cancelAlarm(AlarmActivity.this, code);

                    Intent intent = new Intent(AlarmActivity.this, UpdateService.class);
                    intent.putExtra("screen_state", "alarmClose");
                    startService(intent);

                    mActive = false;
                    finishAffinity();
                }
            });
        } else if(code%3 == 1){
            Intent intent = new Intent(AlarmActivity.this, UpdateService.class);
            intent.putExtra("screen_state", "AlarmTake");
            startService(intent);

            new CountDownTimer(1000 * 60, 1000) { // adjust the milli seconds here
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                    if (mActive) {
                        if (vibrate) {
                            stopVibrate(AlarmActivity.this);
                        }
                        if (sound) {
                            player.stop();
                            player.release();
                        }
                    }
                }
            }.start();

            Button button = findViewById(R.id.button);
            button.setText("服藥完畢");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        if (vibrate) {
                            stopVibrate(AlarmActivity.this);
                        }
                        if (sound) {
                            player.stop();
                            player.release();
                        }
                    } catch (Exception ex){
                        Log.d("ex", ex.toString());
                    }

                    Intent intent = new Intent(AlarmActivity.this, UpdateService.class);
                    intent.putExtra("screen_state", "take");
                    startService(intent);

                    finishAffinity();
                }
            });
        }
        textView = findViewById(R.id.textViewTime);
        int hr = getIntent().getIntExtra("hr", 0);
        int min = getIntent().getIntExtra("min", 0);
        String meds = getIntent().getStringExtra("med");
        if(meds.contains("&")) textView.setText(meds.split("&")[0] + "服藥時間: " + meds.split("&")[1]);
        else textView.setText("服藥時間: " + meds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mActive = false;
    }

    private final Runnable mRunnable = new Runnable() {
        public void run() {
            if (mActive) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                textView.setText(sdf.format(new Date(System.currentTimeMillis())));
                mHandler.postDelayed(mRunnable, 1000*60);
            }
        }
    };


    public static void setAlarm(Context context, boolean after, boolean repeat, int code, int hr, int min, String med) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Intent myIntent = new Intent(context, AlarmReceiver.class);
        myIntent.putExtra("requestCode", code);
        myIntent.putExtra("hr", hr);
        myIntent.putExtra("min", min);
        myIntent.putExtra("med", med);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, code, myIntent, 0);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        if(hr >= 24) calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, hr);
        calendar.set(Calendar.MINUTE, min);

        Date now = new Date();
        if(!after || calendar.getTime().after(now)) {
            Log.d("alarm", calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE));
            if (repeat) {
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 1000 * 60 * 5, pendingIntent);
            } else {
                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
            }
        }
    }

    public static void cancelAlarm(Context context, int code){
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, code, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.cancel(sender);
    }


    private void startVibrate(){
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 200, 500, 200, 0};
        if(vibrator != null){
            vibrator.vibrate(pattern, 0);
        }
    }
    public static void stopVibrate(Context context){
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if(vibrator != null){
            vibrator.cancel();
        }
    }

}
