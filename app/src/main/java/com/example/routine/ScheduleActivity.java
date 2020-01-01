package com.example.routine;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScheduleActivity {
    private static int ir = 24;
    static String defaultSchedule(Context context, String strTime){
        groupMed(context);

        Log.d("existInfo", UpdateService.fileExist(context, "group.json")+"");
        JSONObject jsonObject = getJSONObj(UpdateService.readFromFile(context, "group.json"));
//        Log.d("readInfo", jsonObject.toString());
        for(int i = 0;i < jsonObject.length();i++){
            String interval = findLongInterval(ir, Integer.parseInt(getJSONVal(jsonObject, i+"", "min")), Integer.parseInt(getJSONVal(jsonObject, i+"", "max")));
            Log.d("medInfo", interval);
            putJSONToObj(jsonObject, i+"", putJSONToObj(getObjInJSONObj(jsonObject, i+""), "interval", interval));
//                Log.d("medInfo", jsonObject.toString());
//                UpdateService.writeToFile(context, jsonObject.toString(), "group.json", false, false);

            int count = 0;
            ArrayList<String> arr = new ArrayList<String>();
            for(String s:interval.split(", ")) {
                count += Integer.parseInt(s);
                String strT = strTimeAdd(strTime, count);
                if(Integer.parseInt(strT.split(":")[0])%ir == Integer.parseInt(strTime.split(":")[0])) strT = strTime;
                if(arr.contains(strT)) continue;
                arr.add(strT);
//                Log.d("strTInfo", s+" "+arr.toString());
            }
            String alarm = splitBrackets(arr.toString());
            putJSONToObj(jsonObject, i+"", putJSONToObj(getObjInJSONObj(jsonObject, i+""), "alarm", alarm));
//            Log.d("medInfo", jsonObject.toString());
            UpdateService.writeToFile(context, jsonObject.toString(), "group.json", false, false);
        }

        String strSchedule = "";
        for(int i = 0;i < jsonObject.length();i++) {
            strSchedule += getJSONVal(jsonObject, i+"", "name")+", "+getJSONVal(jsonObject, i+"", "alarm");
            if(i != jsonObject.length()-1) strSchedule += ", ";
        }
        return strSchedule;
    }

    private static String readFromAssets(Context context, String name){
        String string = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(name)));
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                string += mLine;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return string;
    }

    private static JSONObject getJSONObj(String string){
        try {
            JSONObject json = new JSONObject(string);
            return json;
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("exInfo", e.toString());
        }
        return null;
    }

    private static String getJSONVal(JSONObject json, String index, String name){
        try {
            return json.getJSONObject(index).getString(name);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("exInfo", e.toString());
        }
        return null;
    }

    private static JSONObject getObjInJSONObj(JSONObject json, String index){
        try {
            return json.getJSONObject(index);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("exInfo", e.toString());
        }
        return null;
    }

    private static JSONObject putJSONToObj(JSONObject json, String name, Object val){
        try {
            json.put(name, val);
            return json;
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("exInfo", e.toString());
        }
        return null;
    }

    private static void groupMed(Context context){
        JSONObject jsonObject = getJSONObj(readFromAssets(context, "med.json"));
        if (jsonObject != null) {
//            Log.d("medInfo", jsonObject.toString());

            JSONObject json = new JSONObject();
            ArrayList<Integer> finished = new ArrayList<>();
            ArrayList<Integer> isCount = new ArrayList<>();
            int count = 0;
            for (int i = 1; i <= ir; i++) {
                try {
                    boolean over = false;
                    for (int j = 0; j < jsonObject.length() && finished.size() < jsonObject.length(); j++) {
//                Log.d("j", i+" "+j+" "+count);
                        if (!finished.contains(j)) {
                            if (i == Integer.parseInt(jsonObject.getJSONObject(j + "").getString("max"))) {
                                over = true;
                                isCount.add(j);
                                continue;
                            }
                            if (i >= Integer.parseInt(jsonObject.getJSONObject(j + "").getString("min")) && i <= Integer.parseInt(jsonObject.getJSONObject(j + "").getString("max"))) {
                                if (over && i > (Integer.parseInt(jsonObject.getJSONObject(j + "").getString("min")) + Integer.parseInt(jsonObject.getJSONObject(j + "").getString("max"))) / 2) {
                                    isCount.add(j);
                                }
                            }
                        }
                    }
//
                    if (over) {
                        int maxOfMin = 0;
                        for (int j : isCount) {
                            int tmp = Integer.parseInt(jsonObject.getJSONObject(j + "").getString("min"));
                            if (tmp > maxOfMin) {
                                maxOfMin = tmp;
                            }
                        }
//
                        finished.addAll(isCount);
                        String str = "";
                        for (int member : isCount) {
                            str += jsonObject.getJSONObject(member + "").getString("name");
                            if(isCount.indexOf(member) != isCount.size()-1) str += " ";
//                        Log.d("medInfo", member+"");
                        }
                        JSONObject obj = new JSONObject();
                        obj.put("name", str);
                        obj.put("min", maxOfMin);
                        obj.put("max", i);
                        json.put(count + "", obj);
                        count++;
                        isCount.clear();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d("exInfo", e.toString());
                }
            }
//        Log.d("groupInfo", json.toString());
            UpdateService.writeToFile(context, json.toString(), "group.json", false, false);
        }
    }

    private static String splitBrackets(String string){
        return string.split("\\[")[1].split("]")[0];
    }

    private static String findLongInterval(int ir, int imin, int imax){
        int quotient = ir/imax;
        int remainder = ir%imax;
        ArrayList<Integer> list = new ArrayList<Integer>();
        if (remainder < imin) {
            for (int j = 0; j < quotient-1; j++) {
                list.add(imax);
            }
            list.add(imax - imin + remainder);
            list.add(imin);
        } else{
            for (int j = 0; j < quotient; j++) {
                list.add(imax);
            }
            list.add(remainder);
        }

        Collections.reverse(list);
        return splitBrackets(list.toString());
    }

    private static String strTimeAdd(String strTime, int hr){
        String[] strings = strTime.split(":");
        return (Integer.parseInt(strings[0])+hr)+":"+strings[1];
    }


    static String defaultSchedule3(Context context, String strTime){
        JSONObject jsonObject = getJSONObj(UpdateService.readFromFile(context, "group.json"));
        for(int i = 0;i < jsonObject.length();i++) {
            int min = Integer.parseInt(getJSONVal(jsonObject, i+"", "min"));
            int max = Integer.parseInt(getJSONVal(jsonObject, i+"", "max"));

            String alarm3 = "";
            if(UpdateService.fileExist(context, "pro.txt")) {
                FireFunction.setPathVal(UpdateService.getMacAddr()+"/filePro", true);

                String interval = getJSONVal(jsonObject, i+"", "interval");
                String[] strArr = interval.split(", ");
                ArrayList<String> arr = new ArrayList<String>();
                int count = Integer.parseInt(strArr[strArr.length-1])*60;
                int tmp = valInIr(time2int(strTime)-count);
                if(tmp < time2int(strTime)) tmp += ir*60;
                arr.add(strTime);
                arr.add(int2time(tmp, false, false));
                int num = 0;
                for(int j = strArr.length-2;j >= 0;j--) {
                    int ii = Integer.parseInt(strArr[j])*60;
                    if(num != 0){
                        ii += num;
                        num = 0;
                    }
                    tmp = valInIr(time2int(strTime)-count-ii);
//                    Log.d("count_info",ii+" "+tmp);
                    double pro = getPro(context, tmp);
                    boolean greater = greaterCmp(context, pro);
                    Log.d("getPro_info", pro+" "+greater);
                    if(!greater){
                        boolean found = false;
                        for(int k = ii;k >= min*60;k--){
                            tmp = valInIr(time2int(strTime)-count-k);
                            if(getPro(context, tmp) > pro){
                                found = true;
                                count += k;
                                num = ii-k;
                                Log.d("found_info", count + " " + k+" "+ii);

                                if(tmp < time2int(strTime)) tmp += ir*60;
                                if(!arr.contains(int2time(tmp, false, false))) arr.add(int2time(tmp, false, false));
                                break;
                            }
                        }
                        if(!found){
                            tmp = valInIr(time2int(strTime)-count-ii);
                            count += ii;
                            Log.d("notFound_info", count + " " + ii);

                            if(tmp < time2int(strTime)) tmp += ir*60;
                            if(!arr.contains(int2time(tmp, false, false))) arr.add(int2time(tmp, false, false));
                        }
                    } else{
                        count += ii;
                        Log.d("greater_info", count + " " + ii);

                        if(tmp < time2int(strTime)) tmp += ir*60;
                        if(!arr.contains(int2time(tmp, false, false))) arr.add(int2time(tmp, false, false));
                    }
                }
//                Log.d("arr_info", arr.toString());
                Collections.reverse(arr);
                alarm3 = splitBrackets(arr.toString());
            } else{
                FireFunction.setPathVal(UpdateService.getMacAddr()+"/filePro", false);
                FireFunction.savePro(context);

                alarm3 = getJSONVal(jsonObject, i+"", "alarm");
            }

            Log.d("alarm3_info", alarm3);
            putJSONToObj(jsonObject, i+"", putJSONToObj(getObjInJSONObj(jsonObject, i+""), "alarm3", alarm3));
//            Log.d("medInfo", jsonObject.toString());
            UpdateService.writeToFile(context, jsonObject.toString(), "group.json", false, false);
        }

        String strSchedule = "";
        for(int i = 0;i < jsonObject.length();i++) {
            strSchedule += getJSONVal(jsonObject, i+"", "name")+", "+getJSONVal(jsonObject, i+"", "alarm3");
            if(i != jsonObject.length()-1) strSchedule += ", ";
        }
        return strSchedule;
    }



    private static double getCmpNum(Context context){
        String string = UpdateService.getTxtLine(context, "pro.txt", 0);
        double avg, sd;
        if(string.split(":")[0].equals("avg")) {
            avg = Double.parseDouble(string.split(":")[1]);
            sd = Double.parseDouble(UpdateService.getTxtLine(context, "pro.txt", 1).split(":")[1]);
        } else{
            avg = Double.parseDouble(UpdateService.getTxtLine(context, "pro.txt", 1).split(":")[1]);
            sd = Double.parseDouble(string.split(":")[1]);
        }
        return avg-sd/2;
    }

    private static boolean greaterCmp(Context context, double value){
        double cmp = getCmpNum(context);
        return value >= cmp;
    }

    private static double getPro(Context context, int intTime){
        String string = UpdateService.getTxtLine(context, "pro.txt", intTime+2);
//        Log.d("readStr", intTime+" "+string);
        if(intTime < 0 || string.equals("")) return-1;
        return Double.parseDouble(string);
    }

    public static int findLessCmpTime(Context context, int intTime){
        for(int i = intTime;i >= 0;i--){
            if(greaterCmp(context, getPro(context, i))){
                return i;
            }
        }
        for(int i = 1439;i > intTime;i--){
            if(greaterCmp(context, getPro(context, i))){
                return i;
            }
        }
        return -1;
    }

    private static int time2int(String string){
        String[] strings = string.split(":");
        return Integer.parseInt(strings[0]) * 60 + Integer.parseInt(strings[1]);
    }

    private static String int2time(int i, boolean modIr, boolean day){
        if(day)
            return i / 60 / 24 + " " + i / 60 % 24 + ":" + i % 60;
        else if(!modIr)
            return i/60+":"+i%60;
        return i/60%24+":"+i%60;
    }

    private static int valInIr(int i){
        if(i < 0) return i+ir*60;
        return i%(ir*60);
    }



    private static String splitReverse(String string, String delimiter){
        List<String> arr = Arrays.asList(string.split(delimiter));
        Collections.reverse(arr);
        return arr.toString();
    }







}
