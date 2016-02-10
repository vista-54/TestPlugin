package com.aaw.beaconsmanager;

import android.app.*;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import org.altbeacon.beacon.*;
import org.altbeacon.beacon.simulator.BeaconSimulator;
import org.altbeacon.beacon.startup.RegionBootstrap;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;


public class AltMonitoring  extends Service implements BeaconConsumer/*, RangeNotifier*/ {
    protected static final String TAG = "AltMonitoring";


    public static final String PREFS_NAME = "BeaconsStore";

    //private static ArrayList<Beacon> visibleBeacons = new ArrayList();

    //public static Context applicationContext;

    public static BeaconManager altBeaconManager;

    private RegionBootstrap mRegionBootstrap;

    private static SharedPreferences sharedPreferences;

    private ArrayList<ExtBeacon> extBeaconsList;

    public static CallbackContext monitoringCallbackContext;

    public static boolean runInForeground = false;

    private BroadcastReceiver broadcastReceiver;



    public static BeaconManager getAltBeaconManagerInstance(){
        return BeaconManager.getInstanceForApplication( MainApplication.getContext() /*this*/  );
    }


    public static void setBackgroundMode(boolean mode){
        getAltBeaconManagerInstance().setBackgroundMode(mode);
        runInForeground = !mode;
    }



    private final IBinder altBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        AltMonitoring getService() {
            return AltMonitoring.this;
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.w(TAG, "=== AltBeacon : onCreate ===");

        try {
            altBeaconManager =  BeaconManager.getInstanceForApplication( MainApplication.getContext() /*this*/  );
            altBeaconManager.setRegionExitPeriod(8*1000L); // todo new
            if(!runInForeground){
                //altBeaconManager.setBackgroundMode(true);
            }
            altBeaconManager.setBackgroundScanPeriod(400L);
            altBeaconManager.setBackgroundBetweenScanPeriod(2000L);

            altBeaconManager.setForegroundBetweenScanPeriod(0L);
            altBeaconManager.setForegroundScanPeriod(333L);


        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }

        initBroadcastReceiver();

    }

    @Override
    public void onDestroy() {
        __stop();

    }


    //===================  private start-stop ====================


    private void __start(){
        Log.w(TAG, "=== start ===");

        // all alt beacons parser already parsed

        // kontakt.io
        BeaconManager.getInstanceForApplication(this).getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));


        if(isEmulator()) {
            BeaconManager.setBeaconSimulator(new BeaconSimulator() {
                @Override
                public List<Beacon> getBeacons() {
                    ArrayList<Beacon> beacons = new ArrayList<Beacon>();
                    Beacon beacon1 = new AltBeacon.Builder().setId1("DF7E1C79-43E9-44FF-886F-1D1F7DA6997A")
                            .setId2("1").setId3("1").setRssi(-55).setTxPower(-55).build();
                    Beacon beacon2 = new AltBeacon.Builder().setId1("DF7E1C79-43E9-44FF-886F-1D1F7DA6997A")
                            .setId2("1").setId3("2").setRssi(-55).setTxPower(-55).build();
                    Beacon beacon3 = new AltBeacon.Builder().setId1("DF7E1C79-43E9-44FF-886F-1D1F7DA6997A")
                            .setId2("1").setId3("3").setRssi(-55).setTxPower(-55).build();
                    Beacon beacon4 = new AltBeacon.Builder().setId1("DF7E1C79-43E9-44FF-886F-1D1F7DA6997A")
                            .setId2("1").setId3("4").setRssi(-55).setTxPower(-55).build();
                    beacons.add(beacon1);
                    beacons.add(beacon2);
                    beacons.add(beacon3);
                    beacons.add(beacon4);
                    return beacons;
                }
            });
        }


        altBeaconManager.bind(this);





    }

    private void __stop(){
        if(altBeaconManager != null){

            //altBeaconManager.setRangeNotifier(null);
            altBeaconManager.unbind(this);
            //altBeaconManager = null;

        }

        if(broadcastReceiver!=null){
            this.getApplicationContext().unregisterReceiver(broadcastReceiver);
        }
        Log.w(TAG, "=== Destroy iBeacon service ===");
    }





    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w(TAG, "=== onStartCommand ===");

        __start();
        return  START_STICKY;
    }



    @Override
    public Context getApplicationContext(){
            return MainApplication.getContext();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return altBinder;
    }


    //===== bind-unbind

    @Override
    public boolean bindService(Intent intent, ServiceConnection connection, int mode) {
        Log.w(TAG, "=== Bind to IBeacon service ===");
        return  getApplicationContext().bindService(intent, connection, mode);
    }
    @Override
    public void unbindService(ServiceConnection connection) {
        Log.w(TAG,"=== Unbind from IBeacon service ===");
        getApplicationContext().unbindService(connection);

    }



    private boolean beaconServiceConnected = false;

    @Override
    public void onBeaconServiceConnect() {

        beaconServiceConnected = true;

        Log.w(TAG, "=== onBeaconServiceConnect : BeaconConsumer ===");
        altBeaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                monitoringStateProcess(ExtBeacon.ActionType.enter, region);
                //Log.w(TAG, "I just saw an beacon for the first time!");
                //showNotification("entered allBeaconsRegion.  starting ranging");

            }

            @Override
            public void didExitRegion(Region region) {
                monitoringStateProcess(ExtBeacon.ActionType.exit, region);
                //Log.w(TAG, "I no longer see an beacon");
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                //Log.w(TAG, "RegionBootstrap  I have just switched from seeing/not seeing beacons: "+state);
            }
        });


        // start monitoring

        if(!runInForeground  || needMonitorEnable) {
            Log.w(TAG, "Service running in Background");
            try {
                startMonitoring();
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        }


        try {
            tryToStartMonitoring();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }


        // starting ranging

        tryToStartRanging();



    }


    private boolean needMonitorEnable = false;

    public void startMonitoring() throws Exception{
        needMonitorEnable = true;
        extBeaconsList = loadBeacons();

        tryToStartMonitoring();

    }


    private void tryToStartMonitoring() throws Exception{
        if(beaconServiceConnected && needMonitorEnable){
            try {
                for(ExtBeacon eb: extBeaconsList){
                    if(!altBeaconManager.getMonitoredRegions().contains(eb.getRegion())) {
                        altBeaconManager.startMonitoringBeaconsInRegion(eb.getRegion());
                    }
                }
                // altBeaconManager.startMonitoringBeaconsInRegion(new Region("all-beacons-allBeaconsRegion", null, null, null));
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
                throw e;
            }
        }
    }


    public static void stopMonitoring() throws Exception{

        try {
            Collection<Region> allMonitoredRegions = altBeaconManager.getMonitoredRegions();
            for(Region r: allMonitoredRegions){
                altBeaconManager.stopMonitoringBeaconsInRegion(r);
            }
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage());
            throw e;

        }
    }




    //============================ RANGING =====================

    private boolean needToStartRanging = false;
    private RangeNotifier rangeNotifier;
    public void startRanging(CallbackContext callbackContext, RangeNotifier rangeNotifier){
        needToStartRanging = true;
        this.rangeNotifier = rangeNotifier;
        tryToStartRanging();
    }

    private void tryToStartRanging(){
        if(needToStartRanging && beaconServiceConnected){
            //if(runInForeground) {
            // === from site
            Region allBeaconsRegion = new Region("all-beacons-region", null, null, null);


            try {

                altBeaconManager.startRangingBeaconsInRegion(allBeaconsRegion);
            } catch (RemoteException e) {
                Log.e(TAG, e.getMessage());
            }
            altBeaconManager.setRangeNotifier( rangeNotifier );
            //}
        }
    }

    public static void stopRanging()throws Exception{
        try {
            Collection<Region> allRangedRegions = altBeaconManager.getRangedRegions();
            for(Region r: allRangedRegions){
                altBeaconManager.stopRangingBeaconsInRegion(r);
            }
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage());
            throw e;
        }

    }




    /**
     * if  monitoring callback is connected, then not show
     */
    public void monitoringStateProcess(ExtBeacon.ActionType type, Region region){

        ExtBeacon eb = findBeaconInArray(extBeaconsList, region);
        if(eb==null){
            Log.w(TAG, "=== Saved beacon not found ===");
            return;
        }

        HashMap<String, Object> returnMap = new HashMap();
        returnMap.put("actionLocationType", type.name());
        returnMap.put("parametersMap", eb.getData());
        returnMap.put("region", new BeaconsUtils.ExtRegion(region));

        if(monitoringCallbackContext!=null) {




            JSONObject jso = BeaconsUtils.bundleToJsonObject(returnMap);

            PluginResult result;
            result = new PluginResult(PluginResult.Status.OK, jso);
            result.setKeepCallback(true);
            monitoringCallbackContext.sendPluginResult(result);

            return;
        }

        ExtBeacon.MsgForType msgForType = eb.getMsgForType(type);
        if(msgForType.isShow()){
            showNotification(msgForType.getMsg(), returnMap);
        }


//        switch (eb.getActionType()){
//            case 0: break;
//            case 1: showNotification(eb.getMsg(), returnMap);
//        }


    }


    private ExtBeacon findBeaconInArray(ArrayList<ExtBeacon> arr, Region region){
        for(ExtBeacon eb : arr){
            if(eb.getRegion().equals(region)){
                return eb;
            }
        }
        return null;
    }




    private NotificationManager mManager;

    private void showNotification(String notificationStr, HashMap<String, Object> backParam) {
        Log.w(TAG, notificationStr);


        mManager = (NotificationManager) this.getApplicationContext().getSystemService(this.getApplicationContext().NOTIFICATION_SERVICE);


        Intent li = getLaunchAppIntent();
        String launchClassName = li.getComponent().getClassName();
        Class cl = null;
        try {
            cl = Class.forName(launchClassName);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, e.getMessage());
            return;
        }
        Intent runAppWithParamsIntent = new Intent(this.getApplicationContext(), cl);
        runAppWithParamsIntent.putExtra("backParams", BeaconsUtils.objectToString(backParam));
        for(String key: backParam.keySet()){
            //runAppWithParamsIntent.putExtra(key, backParam.get(key));


        }
        //runAppWithParamsIntent.putExtra("bmPlugin", "Exist Always");
        //runAppWithParamsIntent.putExtra("backParam", (Serializable) backParam);

        int appIcon =  getAppIcon();
        Notification notification = new Notification( appIcon , notificationStr, System.currentTimeMillis());
        runAppWithParamsIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingNotificationIntent = PendingIntent.getActivity(this.getApplicationContext(), 0, runAppWithParamsIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(this.getApplicationContext(), "BeaconsPlugin", notificationStr, pendingNotificationIntent);

        mManager.notify(0, notification);
    }

    public int getAppIcon(){
        PackageManager pm = getPackageManager();
        String pkg = MainApplication.getContext().getPackageName();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return ai.icon;
            //return iconId;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return 0;
    }




    private ArrayList<ExtBeacon> loadBeacons(){
        ArrayList<ExtBeacon> extBeaconsList = null;
//        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(/*AltMonitoring.applicationContext*/ MainApplication.getContext());
        String beaconsArrString = BeaconsUtils.readStringVariableFromAppContext("extBeaconsListStr"); //sharedPreferences.getString("extBeaconsListStr" , "");

        extBeaconsList = (ArrayList<ExtBeacon>) BeaconsUtils.stringToObject(beaconsArrString);

        for(ExtBeacon eb: extBeaconsList){
            String major = eb.getMajor();
            Identifier majorId = (major==null  ||  major.equals("")  ||  major.equals("null")) ? null : Identifier.parse(major);
            String minor = eb.getMinor();
            Identifier minorId = (minor==null  ||  minor.equals("")  ||  minor.equals("null")) ? null : Identifier.parse(minor);

            Region currExtBeaconRegion = new Region("region-"+eb.getId(), Identifier.parse(eb.getUuid()), majorId, minorId);
            altBeaconManager.getMonitoredRegions().add(currExtBeaconRegion);
            eb.setRegion(currExtBeaconRegion);
        }
        return extBeaconsList;
    }






     public Intent getLaunchAppIntent(){
         Intent launchIntent = this.getApplicationContext().getPackageManager().getLaunchIntentForPackage(MainApplication.getContext().getPackageName());
         //launchIntent
         return launchIntent;

     }


    public boolean isEmulator(){
        return Build.FINGERPRINT.startsWith("generic");
//        return "google_sdk".equals( Build.PRODUCT );
    }





    //=======================  bluetooth  ===================

//    private boolean checkBluetooth()throws Exception{
//        //check device support
//        try {
//            boolean available = altBeaconManager.checkAvailability();
//            return
//        } catch (Exception e) {
//            throw e;
//            //if device does not support iBeacons an error is thrown
//            //Log.w("Cannot listen to Bluetooth service: "+e.getMessage());
//
//        }
//        return false
//    }


    private void initBroadcastReceiver(){
        if (broadcastReceiver != null) {
            return;
        }
        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);



        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                // Only listen for Bluetooth server changes
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
                    final int oldState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE,BluetoothAdapter.ERROR);

                    Log.w(TAG, "Bluetooth Service state changed from "+getStateDescription(oldState)+" to " + getStateDescription(state));
                    if(state==BluetoothAdapter.STATE_TURNING_OFF || state==BluetoothAdapter.STATE_TURNING_ON){
                        // no need duplicate liked events
                        return;
                    }
                    notifyAboutChangedBluetoothStatus(getStateDescription(oldState), getStateDescription(state));


                }
            }

            private String getStateDescription(int state) {
                switch (state) {
                    case BluetoothAdapter.ERROR:
                        return "ERROR";
                    case BluetoothAdapter.STATE_OFF:
                        return "STATE_OFF";
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        return "STATE_TURNING_OFF";
                    case BluetoothAdapter.STATE_ON:
                        return "STATE_ON";
                    case BluetoothAdapter.STATE_TURNING_ON:
                        return "STATE_TURNING_ON";
                }
                return "ERROR"+state;
            }
        };

        this.getApplicationContext().registerReceiver(broadcastReceiver, filter);

    }


    private void notifyAboutChangedBluetoothStatus(String oldStatus, String newStatus){
        if(monitoringCallbackContext!=null){

            JSONObject resJso = new JSONObject();
            try {
                resJso.put("eventType", "didChangeBluetoothStatus");
                JSONObject data = new JSONObject();
                data.put("oldStatus", oldStatus);
                data.put("newStatus", newStatus);
                resJso.put("data", data);
            }catch (JSONException je){
                Log.e(TAG, je.getMessage());
            }

            PluginResult pluginResult;

            pluginResult = new PluginResult(PluginResult.Status.OK, resJso);
            pluginResult.setKeepCallback(true);
            monitoringCallbackContext.sendPluginResult(pluginResult);
        }else{
            //Log.i(TAG, msg);
        }
    }


    private void sendResultToApplication(String msg){
        if(monitoringCallbackContext!=null){
            PluginResult pluginResult;
            pluginResult = new PluginResult(PluginResult.Status.OK, msg);
            pluginResult.setKeepCallback(true);
            monitoringCallbackContext.sendPluginResult(pluginResult);
        }else{
            Log.i(TAG, msg);
        }
    }





}
