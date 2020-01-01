package com.example.routine;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class MainActivity extends NavActivity {
    public static String APP_TAG = "MyWakelockTag";
    private boolean run = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeToolbar();

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                //1.进入系统电池优化设置界面,把当前APP加入白名单
//                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));

                //2.弹出系统对话框,把当前APP加入白名单(无需进入设置界面)
                //在manifest添加权限 <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"/>
                @SuppressLint("BatteryLife") Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
//        p.edit().putBoolean("vibrate", true).apply();

        boolean exist = UpdateService.fileExist(MainActivity.this, "pro.txt");
//        Log.d("fileExist", exist+"");
        if(!exist)
            FireFunction.savePro(MainActivity.this);
//        Log.d("readFile", UpdateService.readFromFile(MainActivity.this, "pro.txt"));

//        boolean alarmUp = (PendingIntent.getBroadcast(MainActivity.this, 0,  new Intent(MainActivity.this, AlarmReceiver.class), PendingIntent.FLAG_NO_CREATE) != null);
//        if(!alarmUp) {
            String avgStart = UpdateService.readLastAvgStart(MainActivity.this);
//            Log.d("lastStart", lastStart);
            p.edit().putString("avgStart", avgStart).apply();
            FireFunction.setPathVal(UpdateService.getMacAddr()+"/avgStart", avgStart);
            String strSchedule = ScheduleActivity.defaultSchedule(MainActivity.this, avgStart);
//            Log.d("schedule", strSchedule);
            p.edit().putString("alarm", strSchedule).apply();
            FireFunction.setPathVal(UpdateService.getMacAddr()+"/strSchedule", strSchedule);
            String strSchedule3 = ScheduleActivity.defaultSchedule3(MainActivity.this, avgStart);
            p.edit().putString("alarm3", strSchedule3).apply();
            FireFunction.setPathVal(UpdateService.getMacAddr()+"/strSchedule3", strSchedule3);
//            AlarmActivity.cancelAll(MainActivity.this);
            AlarmActivity.setAlarm(MainActivity.this, false, false, 0, 0, 0, "");
//        }

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("getId", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                        // Log and toast
//                        String msg = getString(R.string.msg_token_fmt, token);
//                        Log.d("getId", msg);
//                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });

        String msg = getIntent().getStringExtra("msg");
        String ver = getIntent().getStringExtra("ver");
        if (getIntent().getBooleanExtra("EXIT", false)) {
            finishAffinity();
        }
        else if(ver != null) {
            final String url = getIntent().getStringExtra("url");
            SpannableString s = new SpannableString(msg);
            Linkify.addLinks(s, Linkify.WEB_URLS);

            String currentVersion = verNow();
            if (Float.parseFloat(ver) > Float.parseFloat(currentVersion)) {
                dialogUpdate(s, url);
            }
        }
        else {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

            FireFunction.getVersion(new FireFunction.MyCallback() {
                @Override
                public void onCallback(String value) {
//                    Log.d("getVer", value);
                    if (Float.parseFloat(value) > Float.parseFloat(verNow())) {
                        dialogUpdate(new SpannableString("APP has new version: " + value + "\nClick OK to download"), "");
                    }
                }
            });
        }

        FireFunction.setTimeout(MainActivity.this);
        FireFunction.setVer(verNow());
    }

    protected void onStart() {
        super.onStart();
    }

    public void onResume(){
        super.onResume();
        if(run){
            Button startButton = (Button) findViewById(R.id.button_start);
            Button stopButton = (Button) findViewById(R.id.button_stop);
            startButton.setOnClickListener(startClickListener);
            stopButton.setOnClickListener(stopClickListener);

//            stopButton.setClickable(false);
            startButton.performClick();
            Toast.makeText(MainActivity.this, "開始記錄，新紀錄將會在下次開啟APP時載入", Toast.LENGTH_LONG).show();



            final TextView textView = findViewById(R.id.textView);
            textView.setText("Loading...");
            new Thread(new Runnable() {
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // read file
//                            String str = UpdateService.asciiShift(UpdateService.readFromFile(MainActivity.this), 64);
                            String str = UpdateService.readWeek(MainActivity.this);
                            textView.setText(str);
                        }
                    });
                }
            }).start();
        }
    }

    public void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.v("main","Permission: "+permissions[0]+ " was "+grantResults[0]);
            //resume tasks needing this permission
            run = true;
        }
    }

    private Button.OnClickListener startClickListener = new Button.OnClickListener() {
        public void onClick(View arg0) {
            //啟動服務
            Intent intent = new Intent(MainActivity.this, UpdateService.class);
//            intent.putExtra("activity", this.toString());
            intent.putExtra("screen_state", "run");
            startService(intent);
        }
    };

    private Button.OnClickListener stopClickListener = new Button.OnClickListener() {
        public void onClick(View arg0) {
            Intent i = new Intent(MainActivity.this, UpdateService.class);
            i.putExtra("screen_state", "stop");
            startService(i);
            //停止服務
            Intent intent = new Intent(MainActivity.this, UpdateService.class);
            stopService(intent);
        }
    };

    private void dialogUpdate(SpannableString s, final String strUrl){
        final AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("APP update")
                .setMessage(s)
                .setPositiveButton("確認", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse("https://drive.google.com/file/d/1uy-_tyFvhN5ZgHhFwRPDrr9U50N-KiYL/view?usp=sharing");
                        if(!strUrl.equals("")) uri = Uri.parse(strUrl);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
        ((TextView)dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }
}
