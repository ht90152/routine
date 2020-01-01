package com.example.routine;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class SettingActivity extends NavActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        initializeToolbar();

        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(SettingActivity.this);
        Switch swVibrate = (Switch) findViewById(R.id.switchVibrate);
        swVibrate.setChecked(p.getBoolean("vibrate", false));
        swVibrate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    p.edit().putBoolean("vibrate", true).commit();
                } else {
                    p.edit().putBoolean("vibrate", false).commit();
                }
            }
        });
        Switch swSound = (Switch) findViewById(R.id.switchSound);
        swSound.setChecked(p.getBoolean("sound", false));
        swSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    p.edit().putBoolean("sound", true).commit();
                } else {
                    p.edit().putBoolean("sound", false).commit();
                }
            }
        });

        TextView textViewAlarm = findViewById(R.id.textViewAlarm);
        textViewAlarm.setText(getString(R.string.msg_avg_start)+p.getString("avgStart", "")+"\n"+getString(R.string.msg_alarm)+p.getString("alarm3", ""));
    }
}
