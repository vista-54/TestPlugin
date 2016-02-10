package com.aaw.beaconsmanager;

import android.app.Application;
import android.content.Context;
import android.nfc.Tag;
import android.util.Log;

public class MainApplication extends Application {

    private static Context sContext;
    private static String TAG = "MAIN_APPLICATION";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG, "--------------------onCreate-------------------");
        sContext = getApplicationContext();

        //startService(new Intent(this, AltMonitoring.class));
    }

    public static Context getContext() {
        Log.w(TAG, "Called getContext: "+ sContext.toString());
        return sContext;
    }
}
