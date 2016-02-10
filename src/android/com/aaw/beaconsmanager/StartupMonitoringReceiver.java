package com.aaw.beaconsmanager;

import android.annotation.TargetApi;
import android.content.*;
import android.nfc.Tag;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.logging.LogManager;

import java.util.List;


//import android.annotation.TargetApi;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.os.Build.VERSION;
//import org.altbeacon.beacon.BeaconManager;
//import org.altbeacon.beacon.logging.LogManager;




@TargetApi(4)
public class StartupMonitoringReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupMonitoring";

    public StartupMonitoringReceiver() {
    }

    public void onReceive(Context context, Intent intent) {
        LogManager.d(TAG, "onReceive called in startup broadcast receiver", new Object[0]);
        if(Build.VERSION.SDK_INT < 18) {
            LogManager.w(TAG, "Not starting up beacon service because we do not have API version 18 (Android 4.3).  We have: %s", new Object[]{Integer.valueOf(Build.VERSION.SDK_INT)});
        } else {
//            BeaconManager beaconManager = BeaconManager.getInstanceForApplication(context.getApplicationContext());
//            if(beaconManager.isAnyConsumerBound()) {
//                if(intent.getBooleanExtra("wakeup", false)) {
//                    LogManager.d("StartupBroadcastReceiver", "got wake up intent", new Object[0]);
//                } else {
//                    LogManager.d("StartupBroadcastReceiver", "Already started.  Ignoring intent: %s of type: %s", new Object[]{intent, intent.getStringExtra("wakeup")});
//                }
//            }

            String monitoringRunning = BeaconsUtils.readStringVariableFromAppContext("monitoringRunning");
            Log.d(TAG, "monitoringRunning: "+monitoringRunning);
            if(monitoringRunning != null  &&  monitoringRunning.equals("true") ){
                BeaconsManagerPlugin beaconsManagerPlugin = new BeaconsManagerPlugin();

                Intent altMonitoringService = new Intent(MainApplication.getContext(), AltMonitoring.class);
                beaconsManagerPlugin.altMonitoringService = altMonitoringService;

                beaconsManagerPlugin.startMonitoring(null, null);



            }

        }
    }
}
