package com.example.routine;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Type;

public class FireFunction {
    private static FirebaseDatabase database;
    private FireFunction(){
        database = FirebaseDatabase.getInstance();
    }

    private static void dbInit(){
        database = FirebaseDatabase.getInstance();
    }

    public static void setTimeout(Context context){
        try {
            int time = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
//            Log.d("timeout", time+"");
            if(database == null) dbInit();
            database.getReference(UpdateService.getMacAddr() + "/timeout").setValue(time);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void setVer(String strVer){
        if(database == null) dbInit();
        database.getReference(UpdateService.getMacAddr() + "/ver").setValue(strVer);
    }

    public static void setPathVal(String path, Object val){
//        Log.d("setVal", val.getClass()+" "+(val instanceof Boolean)+" "+val);
        if(database == null) dbInit();
        database.getReference(path).setValue(val);
    }

    public interface MyCallback {
        void onCallback(String value);
    }
    public static void getVersion(final MyCallback myCallback){
        if(database == null) dbInit();
        database.getReference("version").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                Log.d("readVer", dataSnapshot.getValue().toString());
                myCallback.onCallback(dataSnapshot.getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private static void getData2Callback(String path, final MyCallback myCallback){
        if(database == null) dbInit();
        database.getReference(path).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                Log.d("readVer", dataSnapshot.getValue().toString());
                if(dataSnapshot.getValue() != null)
                myCallback.onCallback(dataSnapshot.getValue().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }
    public static void savePro(final Context context){
        if(database == null) dbInit();
        getData2Callback(UpdateService.getMacAddr() + "/pro/avg", new MyCallback() {
            @Override
            public void onCallback(String value) {
//                Log.d("dataPro", value);
                UpdateService.writeToFile(context, "avg:"+value+"\n", "pro.txt", false, false);
            }
        });
        getData2Callback(UpdateService.getMacAddr() + "/pro/sd", new MyCallback() {
            @Override
            public void onCallback(String value) {
//                Log.d("dataPro", value);
                UpdateService.writeToFile(context, "sd:"+value+"\n", "pro.txt", true, false);
            }
        });
        database.getReference(UpdateService.getMacAddr()+"/pro").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
//                    Log.d("dataPro", child.toString());
                    UpdateService.writeToFile(context, child.getValue()+"\n", "pro.txt", true, false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

}
