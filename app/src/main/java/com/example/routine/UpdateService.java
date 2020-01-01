package com.example.routine;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.NetworkInterface;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class UpdateService extends Service {
    private ScreenReceiver mReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String[] keyword = {"first", "run", "stop", "on", "off", "unlock", "booted", "shutdown"};
        final String[] keyAlarm = {"alarmOn", "alarmOff", "alarmClose", "AlarmTake", "take"};

//        Log.d("intent", intent.getStringExtra("activity"));
        final String screen;
        String screenI = intent == null?"":intent.getStringExtra("screen_state");
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstRun = p.getBoolean("first_run", true);
        p.edit().putBoolean("first_run", false).apply();
        Log.d("first", firstRun+"");
        if(firstRun) {
            screen = screenI.equals("run") ? "first" : screenI;
        } else
            screen = screenI == null ? "":screenI;
        Log.d("state", screen+" "+screenI);
        if(Arrays.asList(keyword).contains(screen))
            p.edit().putString("screen", screen).commit();

        if (Arrays.asList(keyword).contains(screen) || Arrays.asList(keyAlarm).contains(screen)) {
            if(screen.equals("unlock")){
                long count = p.getLong("count", 0);
                p.edit().putLong("count", count+1).apply();
//                Log.d("count", String.valueOf(count+1));
                foregroundNotification();
            }

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateformat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss_SSS");
            final String strTime = dateformat.format(calendar.getTime());
            final int strDay = calendar.get(Calendar.DAY_OF_WEEK);
            String str = strTime.split("_")[0] + " (" + strDay + ") " + screen + "\n";
            writeToFile(this, str, "routine.txt", true, false);

//            Log.d("mac", getMacAddr());
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            final DatabaseReference myRef = database.getReference(getMacAddr());
            if(Arrays.asList(keyAlarm).contains(screen)){
                if(UpdateService.fileExist(UpdateService.this, "pro.txt"))
                    myRef.child("alarm3/"+strTime.split(" ")[0]+" "+strDay+"/"+strTime.split(" ")[1]+"/state").setValue(screen);
                else myRef.child("alarm/"+strTime.split(" ")[0]+" "+strDay+"/"+strTime.split(" ")[1]+"/state").setValue(screen);
            } else{
                myRef.child(strTime.split(" ")[0]+" "+strDay+"/"+strTime.split(" ")[1]+"/state").setValue(screen);
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // REGISTER RECEIVER THAT HANDLES SCREEN ON AND SCREEN OFF LOGIC
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SHUTDOWN);
        filter.addAction(Intent.ACTION_REBOOT);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
        Log.d("broadcast", "service onCreate");

        foregroundNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mReceiver != null) {
                unregisterReceiver(mReceiver);
                stopForeground(true);
                Log.d("broadcast", "service onDestroy");
            }
        } catch (IllegalArgumentException e) {}
    }

    private void foregroundNotification(){
        long count = PreferenceManager.getDefaultSharedPreferences(this).getLong("count", 0);
        // foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            String channelId = getString(R.string.app_name);
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription(channelId);
            notificationChannel.setSound(null, null);

            notificationManager.createNotificationChannel(notificationChannel);
            Notification notification = new Notification.Builder(this, channelId)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Service on. 今天解鎖: "+count+"次")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .build();
            startForeground(110, notification);
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            Notification.Builder builder = new Notification.Builder(this.getApplicationContext());
            Intent nfIntent = new Intent(this, MainActivity.class);
            builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0))
                    .setContentTitle(getString(R.string.app_name))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentText("Service on. 今天解鎖: "+count+"次")
                    .setWhen(System.currentTimeMillis());
            Notification notification = builder.build();
            startForeground(110, notification);
        }
    }

    public static boolean fileExist(Context context, String name){
        File file = context.getFileStreamPath(name);
        if(file.exists())
            return true;
        return false;
    }

    public static void writeToFile(Context context, String data, String name, boolean append, boolean external) {
        int mode = Context.MODE_APPEND;
        if(!append)
            mode = Context.MODE_PRIVATE;
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(name, mode));
            if(external) outputStreamWriter = new OutputStreamWriter(new FileOutputStream(new File(Environment.getExternalStorageDirectory()+"/"+name), append));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    public static String readFromFile(Context context, String name) {
        String ret = "";
        try {
//            InputStream inputStream = new FileInputStream(new File(Environment.getExternalStorageDirectory()+"/"+name));
            InputStream inputStream = context.openFileInput(name);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

    public static String getTxtLine(Context context, String name, int textLine) {
        try {
            InputStream inputStream = context.openFileInput(name);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(inputStream));
            String aDataRow = "";

            int lineCount = 0;
            while ((aDataRow = myReader.readLine()) != null) {
                if(lineCount == textLine) return aDataRow;
                lineCount++;
            }
        } catch (Exception e) {
        }
        return "";
    }

    public static String readWeek(Context context) {
        String ret = "";
        try {
            InputStream inputStream = context.openFileInput("routine.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder stringBuilder = new StringBuilder();

                for (String s : bufferedReader.readLine().split("\n")){
//                    Log.d("s", s);
                    try {
                        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy/MM/dd");
                        Calendar calendar = Calendar.getInstance();
                        calendar.getTime();
                        calendar.add(Calendar.DAY_OF_YEAR, -8);
                        Date date = dateformat.parse(s.split(" ")[0]);
                        if(date.after(calendar.getTime())){
                            stringBuilder.append(s+"\n");
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }


        return ret;
    }


    public static String readLastAvgStart(Context context){
        String ret = "06:00:00";
        try {
            InputStream inputStream = context.openFileInput("routine.txt");
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                SimpleDateFormat timeformat = new SimpleDateFormat("HH:mm:ss");
                String preDate = "";
                int avg = 0, count = 0;
                String pre = "00:00:00";
                long max = 0;
                for (String s : bufferedReader.readLine().split("\n")){
//                    Log.d("s", s);
                    try {
                        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy/MM/dd");
                        Calendar calendar = Calendar.getInstance();
                        calendar.getTime();
                        calendar.add(Calendar.DAY_OF_YEAR, -8);
                        Date date = dateformat.parse(s.split(" ")[0]);
                        if(date.after(calendar.getTime())){
                            if(preDate.equals("")) preDate = dateformat.format(date);
                            if(date.after(dateformat.parse(preDate))){
                                pre = "00:00:00";
                                max = 0;
                                avg += Integer.parseInt(ret.split(":")[0])*60+Integer.parseInt(ret.split(":")[1]);
                                count++;
                                preDate = dateformat.format(date);
                            }

                            if(s.split(" ")[3].equals("on")){
                                long diff = timeformat.parse(s.split(" ")[1]).getTime()-timeformat.parse(pre).getTime();
                                if(diff > max) {
                                    ret = s.split(" ")[1];
                                    max = diff;
                                }
                            } else if(s.split(" ")[3].equals("off")){
                                pre = s.split(" ")[1];
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                inputStream.close();
                if(count != 0) {
                    avg /= count;
                    ret = avg / 60 + ":" + avg % 60;
                }
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    //res1.append(Integer.toHexString(b & 0xFF) + ":");
                    res1.append(String.format("%02X:",b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "";
    }
}
