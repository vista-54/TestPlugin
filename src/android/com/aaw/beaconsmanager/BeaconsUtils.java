package com.aaw.beaconsmanager;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.util.Log;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;


public class BeaconsUtils {
    public static final String TAG = "BeaconsUtils";

    public static String objectToString(Serializable obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(
                    new Base64OutputStream(baos, Base64.NO_PADDING | Base64.NO_WRAP));
            oos.writeObject(obj);
            oos.close();
            return baos.toString("UTF-8");
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public static Object stringToObject(String str) {
        try {
            return new ObjectInputStream(new Base64InputStream(
                    new ByteArrayInputStream(str.getBytes()), Base64.NO_PADDING
                    | Base64.NO_WRAP)).readObject();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    //    private static String LOG_TAG = "utils";
//
    public static boolean isServiceRunning(String serviceClassName) {
        final ActivityManager activityManager = (ActivityManager) MainApplication.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)) {
                return true;
            }
        }
        return false;
    }

    public static List<ActivityManager.RunningServiceInfo> getRunningServices() {
        final ActivityManager activityManager = (ActivityManager) MainApplication.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        return services;
    }

    public static List<ActivityManager.RunningServiceInfo> getRunningServices(String appPackageName) {
        final ActivityManager activityManager = (ActivityManager) MainApplication.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        List<ActivityManager.RunningServiceInfo> appServices = new ArrayList<ActivityManager.RunningServiceInfo>();

        for (ActivityManager.RunningServiceInfo rsi : services) {
            if (rsi.process.equals(appPackageName)) {
                appServices.add(rsi);
            }
        }
        return appServices;
    }

    public static List<ActivityManager.RunningServiceInfo> getRunningServicesByWord(String appPackageNamePart) {
        final ActivityManager activityManager = (ActivityManager) MainApplication.getContext().getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);

        List<ActivityManager.RunningServiceInfo> appServices = new ArrayList<ActivityManager.RunningServiceInfo>();

        for (ActivityManager.RunningServiceInfo rsi : services) {
            if ((rsi.service.getClassName()).toLowerCase().contains(appPackageNamePart.toLowerCase())) {
                appServices.add(rsi);
            }
        }
        return appServices;
    }




    public static void writeStringVariableToAppContext(String varName, String value){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(/*this.getContext().getApplicationContext()*/ MainApplication.getContext());
        SharedPreferences.Editor spe = sharedPreferences.edit();
        spe.putString(varName, value);
        spe.commit();
    }

    public static String readStringVariableFromAppContext(String varName){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext());
        String val = sharedPreferences.getString(varName , null);
        return val;
    }


    public static JSONObject mapOfBeacon(Beacon beacon) throws JSONException {
        JSONObject dict = new JSONObject();

        //beacon id
        dict.put("uuid", beacon.getId1());
        dict.put("major", beacon.getId2());
        dict.put("minor", beacon.getId3());

        // proximity
        dict.put("proximity", nameOfProximity(beacon.getDistance()));

        // signal strength and transmission power
        dict.put("rssi", beacon.getRssi());
        dict.put("tx", beacon.getTxPower());

        // accuracy = rough distance estimate limited to two decimal places (in metres)
        // NO NOT ASSUME THIS IS ACCURATE - it is effected by radio interference and obstacles
        dict.put("accuracy", Math.round(beacon.getDistance() * 100.0) / 100.0);

        return dict;
    }

    public static JSONObject mapOfRegion(ExtRegion region) throws JSONException{
        JSONObject dict = new JSONObject();
        dict.put("uuid", region.getUuid());
        dict.put("major", region.getMajor());
        dict.put("minor", region.getMinor());
        return dict;
    }


    public static JSONObject mapOfExtBeacon(ExtBeacon extBeacon) throws JSONException {
        JSONObject dict = new JSONObject();

        //beacon id
        dict.put("uuid", extBeacon.getUuid());
        dict.put("major", extBeacon.getMajor());
        dict.put("minor", extBeacon.getMinor());

        // proximity
        //dict.put("proximity", nameOfProximity((extBeacon.getRegion()).getDistance()) );

        // signal strength and transmission power
//        dict.put("rssi", extBeacon.getRegion().getRssi());
//        dict.put("tx", region.getTxPower());

        // accuracy = rough distance estimate limited to two decimal places (in metres)
        // NO NOT ASSUME THIS IS ACCURATE - it is effected by radio interference and obstacles
//        dict.put("accuracy", Math.round(region.getDistance()*100.0)/100.0);


        dict.put("data", extBeacon.getData());

        return dict;
    }


    public static String nameOfProximity(double accuracy) {

        if (accuracy < 0) {
            return "ProximityUnknown";
            // is this correct?  does proximity only show unknown when accuracy is negative?  I have seen cases where it returns unknown when
            // accuracy is -1;
        }
        if (accuracy < 0.5) {
            return "ProximityImmediate";
        }
        // forums say 3.0 is the near/far threshold, but it looks to be based on experience that this is 4.0
        if (accuracy <= 4.0) {
            return "ProximityNear";
        }
        // if it is > 4.0 meters, call it far
        return "ProximityFar";
    }

/**

    example of data
    {
        eventType: 'didDetermineStateForRegion',
        data: {
            actionLocationType: 'enter|exit',
            parametersMap: '{key1:val1, key2:val2, key3:val3}',
            region: {
                uuid:'null|qweer-qwweer-qweerr-qwert',
                major: null|12345,
                minor: null|12345
            }
        }
    }
*/

    public static JSONObject bundleToJsonObject(Object bundleOrMap) {

        HashMap<String, Object> map ;
        if(   bundleOrMap instanceof Bundle ){
            map = new HashMap<String, Object>();
            for(String key: ((Bundle) bundleOrMap).keySet()){
                map.put(key, ((Bundle) bundleOrMap).get(key));
            }
        }else{
            map = (HashMap<String, Object>) bundleOrMap;
        }
        JSONObject resJso = new JSONObject();
        try {

            JSONObject data = new JSONObject();
            for (String key : map.keySet()) {
                if (key.equals("parametersMap")) {
                    Object dataBundle = map.get(key);
                    data.put("parametersMap", dataBundle.toString());
                }
                if (key.equals("actionLocationType")) {
                    data.put("actionLocationType", map.get(key));
                }
                if(key.equals("region")){
                    data.put("region", mapOfRegion((ExtRegion)map.get("region")));
                }

            }
            resJso.put("eventType", "didDetermineStateForRegion");
            //data.put("region", mapOfRegion(region));
            resJso.put("data", data);
        } catch (JSONException je) {
            Log.e(TAG, je.getMessage());
        }

        return resJso;
    }


    static class ExtRegion implements Serializable{
        private String uuid;
        private String major;
        private String minor;

        public ExtRegion(Region region){
            this.uuid = region.getId1().toUuid().toString();
            this.major = region.getId2() != null ? region.getId2().toInt()+"" : null;
            this.minor = region.getId3() != null ? region.getId3().toInt()+"" : null;

        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getMajor() {
            return major;
        }

        public void setMajor(String major) {
            this.major = major;
        }

        public String getMinor() {
            return minor;
        }

        public void setMinor(String minor) {
            this.minor = minor;
        }
    }

}