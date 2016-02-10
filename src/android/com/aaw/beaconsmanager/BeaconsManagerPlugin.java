package com.aaw.beaconsmanager;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;
import org.json.JSONObject;

import java.util.*;

public class BeaconsManagerPlugin extends CordovaPlugin {
    public static final String TAG = "BeaconsManager";
    public Intent altMonitoringService;
    //public Intent altBeaconService;
    public AltMonitoring altMonitoringInstance = null;

    private ServiceConnection serviceConnection;

    private CallbackContext monitoringCallbackContext;
    private CallbackContext rangingCallbackContext;

    private boolean monitoringFunctionSet = false;



    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.w(TAG, "=======onNewIntent====" );
        try {
            Bundle b = intent.getExtras();
            if(b!=null) {
                Log.w(TAG, "=======saved extras====" + b.size() + "========" + b.toString());
                //sendNotifyToJavascript(b);
                addObjectToCallbackQueue(b);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }



    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        //AltMonitoring.applicationContext = this.cordova.getActivity();//.getApplicationContext();

        altMonitoringService = new Intent(this.cordova.getActivity()/*.getApplicationContext()*/, AltMonitoring.class);




        //==================
        try {
//            Intent launchIntent = this.cordova.getActivity().getPackageManager().getLaunchIntentForPackage("com.appplg2.MainActivity");
//            MainApplication.getContext().getPackageManager().getLaunchIntentForPackage("com.appplg2.MainActivity");

//            Intent launchIntent = this.cordova.getActivity().getPackageManager().getLaunchIntentForPackage(MainApplication.getContext().getPackageName());
            Bundle cordovaIntentBundle = this.cordova.getActivity().getIntent().getExtras();
            if(cordovaIntentBundle!=null){
                Log.w(TAG, "=======cordovaIntentBundle:" + cordovaIntentBundle.size());
                //sendNotifyToJavascript(cordovaIntentBundle);
                addObjectToCallbackQueue(cordovaIntentBundle);
            }



//            Bundle b = launchIntent.getExtras();
//            if(b!=null) {
//                Log.w(TAG, "=======initialize plugin extras====" + b.size() + "========" + b.toString());
//            }
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }

    }


    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        //=== service
        if(action.equals("startService")){
            this.startService(new Callback());
            return true;
        }

        else
        if(action.equals("stopService")){
            this.stopService();
            return true;
        }


        //=== monitoring
        else
        if (action.equals("startMonitoring")) {
            try {
                JSONArray extBeacons = data.getJSONArray(0);

                this.startMonitoring(extBeacons, callbackContext);

            } catch (JSONException e) {
                Log.e(TAG, action + " execute: Got JSON Exception " + e.getMessage());
                callbackContext.error(e.getMessage());
            }
            return true;
        }
        else
        if (action.equals("stopMonitoring")) {
            try {
                this.stopMonitoring(callbackContext);
            } catch (Exception e) {
                Log.e(TAG, action + " execute: Got JSON Exception " + e.getMessage());
                callbackContext.error(e.getMessage());
            }
            return true;
        }


        //===ranging
        else
        if (action.equals("startRanging")) {
            try {
                JSONArray extBeacons = new JSONArray();
                if(data.getJSONArray(0)!=null){
                    extBeacons = data.getJSONArray(0);
                }


                this.startRanging(extBeacons, callbackContext);

            } catch (JSONException e) {
                Log.e(TAG, action + " execute: Got JSON Exception " + e.getMessage());
                callbackContext.error(e.getMessage());
            }
            return true;
        }
        else
        if (action.equals("stopRanging")) {
            try {
                this.stopRanging(callbackContext);

            } catch (Exception e) {
                Log.e(TAG, action + " " + e.getMessage());
                callbackContext.error(e.getMessage());
            }
            return true;
        }


        //=== setParameters
        else
        if (action.equals("applyParameters")) {
            try {
                Map<String, String> map = new HashMap<String, String>();
                JSONObject jso =  data.getJSONObject(0);
                Iterator names = jso.keys();
                while(names.hasNext()){
                    String key = (String) names.next();
                    String value = jso.get(key).toString();
                    map.put(key, value);
                }

                //Map<String, String> map = (Map<String, String>) data.getJSONObject(0).nameValuePairs[0];

                this.applyParameters(callbackContext, map);

            } catch (Exception e) {
                Log.e(TAG, action + "  " + e.getMessage());
                callbackContext.error(e.getMessage());
            }
            return true;
        }


//        else
//        if(action.equals("readLaunchData")){
//            this.readLaunchData();
//            return true;
//        }

        else
        if (action.equals("onDeviceReady")) {
            monitoringCallbackContext = callbackContext;
            onDeviceReady();
            return true;
        }

        else
        if(action.equals("monitoringFunctionSet"))  {
            monitoringFunctionSet = true;
            sendBundles();
            return true;
        }


        return false;
    }


    private void startService(final Callback callback){

        Context appContext = getMainAppContext();  // getContext()

        //String thisAppPackageName = appContext.getPackageName();
        //List thisAppServices = BeaconsUtils.getRunningServices(thisAppPackageName);

        boolean altMonitoringRunning = isServiceRunning();


        if( AltMonitoring.monitoringCallbackContext==null){
            AltMonitoring.monitoringCallbackContext = monitoringCallbackContext;
        }

        if(!altMonitoringRunning){

            AltMonitoring.runInForeground = true;

            ComponentName cn = appContext.startService(altMonitoringService);
        }

        if(serviceConnection == null) {
            serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
                    altMonitoringInstance = ((AltMonitoring.LocalBinder) serviceBinder).getService();
                    Log.w(TAG, "service connected " + name.getClassName());
                    callback.call();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.w(TAG, "service disconnected " + name.getClassName());
                }
            };


            boolean connected = appContext.bindService(altMonitoringService, serviceConnection, Context.BIND_AUTO_CREATE);

            if (connected) {
                asyncSuccess(monitoringCallbackContext, "Connected to service success ");
            } else {
                asyncSuccess(monitoringCallbackContext, "Service already running. Not connected to service ");
            }
        }else{
            asyncSuccess(monitoringCallbackContext, "Service already started and bound");
            callback.call();
        }


    }


    private void stopService(){
        boolean altMonitoringRunning = isServiceRunning();
        if(altMonitoringRunning){
            if(serviceConnection!=null) {
                getMainAppContext().unbindService(serviceConnection);
                serviceConnection = null;
            }
            boolean stopped = getMainAppContext().stopService(altMonitoringService);
            asyncSuccess(monitoringCallbackContext, "Service has been stopped success: "+stopped);
        }else{
            asyncSuccess(monitoringCallbackContext, "Service is not running");
        }
    }


    public void startMonitoring(JSONArray extBeaconsArr, final CallbackContext callbackContext) {


//        AltMonitoring.monitoringCallbackContext = monitoringCallbackContext;//callbackContext;
//        AltMonitoring.runInForeground = true;

        if(extBeaconsArr != null){
            ArrayList<ExtBeacon> extBeaconsList = new ArrayList();
            try {
                for (int i = 0; i < extBeaconsArr.length(); i++) {
                    JSONObject extBcnJso = extBeaconsArr.getJSONObject(i);
                    ExtBeacon incBeacon = new ExtBeacon();
                    incBeacon.setId(extBcnJso.getInt("id"));
                    incBeacon.setUuid(extBcnJso.getString("uuid"));
                    incBeacon.setMajor(extBcnJso.getString("major"));
                    incBeacon.setMinor(extBcnJso.getString("minor"));
                    //incBeacon.setActionType(extBcnJso.getInt("actionType"));
                    //incBeacon.setMsg(extBcnJso.getString("msg"));
                    incBeacon.setData(extBcnJso.getString("data"));

                    JSONObject msgForEnterJso = extBcnJso.getJSONObject("msgForEnter");
                    incBeacon.setMsgForEnter(new ExtBeacon.MsgForType(
                            msgForEnterJso.getString("msg"),
                            msgForEnterJso.getBoolean("show"),
                            ExtBeacon.ActionType.enter
                    ));
                    JSONObject msgForExitJso = extBcnJso.getJSONObject("msgForExit");
                    incBeacon.setMsgForExit(new ExtBeacon.MsgForType(
                            msgForExitJso.getString("msg"),
                            msgForExitJso.getBoolean("show"),
                            ExtBeacon.ActionType.exit
                    ));


                    extBeaconsList.add(incBeacon);
                }
            }catch (Exception e){
                Log.e(TAG, e.getMessage());
                callbackContext.error(e.getMessage());
            }

            String extBeaconsListStr = BeaconsUtils.objectToString(extBeaconsList);
            BeaconsUtils.writeStringVariableToAppContext("extBeaconsListStr", extBeaconsListStr);
        }


        //=======================

        Callback callback = new Callback(){
            @Override
            public void call(){
                try {
                    altMonitoringInstance.startMonitoring();
                    if(callbackContext!=null) {
                        asyncSuccess(callbackContext, "Monitoring started");
                    }
                    BeaconsUtils.writeStringVariableToAppContext("monitoringRunning", "true");
                }catch (Exception e){
                    if(callbackContext != null) {
                        callbackContext.error(e.getMessage());
                    }
                }
            }
        };

        startServiceIfNotRunning(callback);


    }

    private void stopMonitoring(CallbackContext callbackContext) {
        try{
            altMonitoringInstance.stopMonitoring();
            callbackContext.success("Monitoring was stopped");
            BeaconsUtils.writeStringVariableToAppContext("monitoringRunning", "false");
        }catch (Exception e){
            callbackContext.error(e.getMessage());
        }
    }




    private void startRanging(JSONArray extBeaconsArr, final CallbackContext callbackContext){

        Callback callback = new Callback(){
            @Override
            public void call(){
                RangeNotifier rangeNotifier = new RangeNotifier() {
                    @Override
                    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                        for (Beacon beacon: beacons) {

                            Log.w(TAG, "===  didRangeBeaconsInRegion ===size:"+beacons.size());
                            //showNotification( "===  didRangeBeaconsInRegion ===size: "+beacons.size(), new HashMap<String, String>());

                            //if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
                            // This is a Eddystone-UID frame
                            Identifier namespaceId = beacon.getId1();
                            Identifier instanceId = beacon.getId2();
                            Log.d(TAG, "I see a beacon transmitting namespace id: "+namespaceId+
                                    " and instance id: "+instanceId+
                                    " approximately "+beacon.getDistance()+" meters away.");

                        }

                        if(callbackContext!=null){
                            PluginResult result;
                            try{
                                JSONObject data = new JSONObject();
                                JSONArray beaconData = new JSONArray();
                                for (Beacon beacon : beacons) {
                                    beaconData.put(BeaconsUtils.mapOfBeacon(beacon));
                                }
                                data.put("eventType", "didRangeBeaconsInRegion");
                                //data.put("region", mapOfRegion(region));
                                data.put("beacons", beaconData);


                                //send and keep reference to callback
                                result = new PluginResult(PluginResult.Status.OK, data);
                                result.setKeepCallback(true);
                            }catch (Exception je){
                                result = new PluginResult(PluginResult.Status.JSON_EXCEPTION, je.getMessage());

                            }
                            callbackContext.sendPluginResult(result);

                        }
                    }
                };



//                altMonitoringInstance.getAltBeaconManagerInstance().setRangeNotifier(new RangeNotifier() {
//                    @Override
//                    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
//                        for (Beacon beacon: beacons) {
//
//                            Log.w(TAG, "===  didRangeBeaconsInRegion ===size:"+beacons.size());
//                            //showNotification( "===  didRangeBeaconsInRegion ===size: "+beacons.size(), new HashMap<String, String>());
//
//                            //if (beacon.getServiceUuid() == 0xfeaa && beacon.getBeaconTypeCode() == 0x00) {
//                            // This is a Eddystone-UID frame
//                            Identifier namespaceId = beacon.getId1();
//                            Identifier instanceId = beacon.getId2();
//                            Log.d(TAG, "I see a beacon transmitting namespace id: "+namespaceId+
//                                    " and instance id: "+instanceId+
//                                    " approximately "+beacon.getDistance()+" meters away.");
//
//                        }
//
//                        if(callbackContext!=null){
//                            PluginResult result;
//                            try{
//                                JSONObject data = new JSONObject();
//                                JSONArray beaconData = new JSONArray();
//                                for (Beacon beacon : beacons) {
//                                    beaconData.put(BeaconsUtils.mapOfBeacon(beacon));
//                                }
//                                data.put("eventType", "didRangeBeaconsInRegion");
//                                //data.put("region", mapOfRegion(region));
//                                data.put("beacons", beaconData);
//
//
//                                //send and keep reference to callback
//                                result = new PluginResult(PluginResult.Status.OK, data);
//                                result.setKeepCallback(true);
//                            }catch (Exception je){
//                                result = new PluginResult(PluginResult.Status.JSON_EXCEPTION, je.getMessage());
//
//                            }
//                            callbackContext.sendPluginResult(result);
//
//                        }
//                    }
//                });
                altMonitoringInstance.startRanging(callbackContext, rangeNotifier);
                asyncSuccess(monitoringCallbackContext, "Ranging started");
            }
        };

        startServiceIfNotRunning(callback);


    }

    private void stopRanging(CallbackContext callbackContext){
        try{
            altMonitoringInstance.stopRanging();
            asyncSuccess(monitoringCallbackContext, "Ranging stopped");
        }catch(Exception e){
            callbackContext.error(e.getMessage());
        }

    }



//    public void readLaunchData(){
//       //IntegetContext().getPackageManager().getLaunchIntentForPackage(MainApplication.getContext().getPackageName());
//        this.cordova.getActivity().getIntent().getExtras();
//
//    }



    public Context getContext(){
        return this.cordova.getActivity();//.getApplicationContext();
    }

    public Context getMainAppContext(){
        return MainApplication.getContext();
    }


    public void onDestroy() {
        Log.w(TAG, "Main Activity destroyed!!!");
        //Activity activity = this.cordova.getActivity();
        AltMonitoring.monitoringCallbackContext = null;  // todo experimental
        AltMonitoring.runInForeground = false;
        AltMonitoring.setBackgroundMode(true);
        if(serviceConnection!=null) {
                getMainAppContext().unbindService(serviceConnection);
        }
        stopRanging(null);


    }


    //=================================

    private void applyParameters(CallbackContext callbackContext, Map<String, String> map){

        Set<String> keySet = map.keySet();
        for(String key: keySet ) {
            String val = map.get(key);

            if (key.equals("backgroundMode")) {
                if (val.equals("true")) {
                    AltMonitoring.altBeaconManager.setBackgroundMode(true);

                } else if (val.equals("false")) {
                    AltMonitoring.altBeaconManager.setBackgroundMode(false);
                } else {
                    callbackContext.error("Unknown action for val: " + val + " on key: " + key);
                }
            } else if (key.equals("foregroundScanPeriod")) {
                long period = Long.parseLong(val);
                AltMonitoring.altBeaconManager.setForegroundScanPeriod(period);
            } else if (key.equals("foregroundBetweenScanPeriod")) {
                long period = Long.parseLong(val);
                AltMonitoring.altBeaconManager.setForegroundBetweenScanPeriod(period);
            } else if (key.equals("backgroundScanPeriod")) {
                long period = Long.parseLong(val);
                AltMonitoring.altBeaconManager.setBackgroundScanPeriod(period);
            } else if (key.equals("backgroundBetweenScanPeriod")) {
                long period = Long.parseLong(val);
                AltMonitoring.altBeaconManager.setBackgroundBetweenScanPeriod(period);
            } else {
                callbackContext.error("Unknown action for key:" + key);
            }

        }

        asyncSuccess(callbackContext, "OK");
    }



    //============== bluetooth listener ==============
    private void initBluetoothListener() {

        //check access
        if (!hasBlueToothPermission()) {
            Log.w(TAG, "Cannot listen to Bluetooth service when BLUETOOTH permission is not added");
            return;
        }

        //check device support
//        try {
//            altBeaconManager.checkAvailability();
//        } catch (Exception e) {
//            //if device does not support iBeacons an error is thrown
//            debugWarn("Cannot listen to Bluetooth service: "+e.getMessage());
//            return;
//        }




        // Register for broadcasts on BluetoothAdapter state change
//        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
//        cordova.getActivity().registerReceiver(broadcastReceiver, filter);
    }

    private boolean hasBlueToothPermission()
    {
        Context context = cordova.getActivity();
        int access = context.checkCallingOrSelfPermission(Manifest.permission.BLUETOOTH);
        int adminAccess = context.checkCallingOrSelfPermission(Manifest.permission.BLUETOOTH_ADMIN);

        return (access == PackageManager.PERMISSION_GRANTED) && (adminAccess == PackageManager.PERMISSION_GRANTED);
    }







    /*=================================     sending event   ===============================*/
    static boolean deviceReady = false;
    ArrayList<JSONObject> eventQueue = new ArrayList();


    private synchronized void addObjectToCallbackQueue(Bundle b){
        String backParams = b.getString("backParams");
        HashMap<String, Object> paramsMap = (HashMap<String, Object>) BeaconsUtils.stringToObject(backParams);
        JSONObject jso = BeaconsUtils.bundleToJsonObject(paramsMap);
        eventQueue.add(jso);
        sendBundles();
    }

    private synchronized  void sendBundles(){
        if(deviceReady &&  monitoringFunctionSet && monitoringCallbackContext!=null){

            for (JSONObject msgObject : eventQueue) {

                asyncSuccess(monitoringCallbackContext, msgObject);
            }
            eventQueue.clear();
        }

    }




    private synchronized void onDeviceReady() {
        Log.w(TAG, "=============onDeviceReady=======eventQueue.size: "+eventQueue.size());
        //isInBackground = false;
        deviceReady = true;
        sendBundles();

//        for (String js : eventQueue) {
//            sendJavascript(js);
//        }
//
//        eventQueue.clear();
    }


//    private  synchronized void sendJavascript(final String js) {
//        Log.w(TAG, "====== sendJavascript Called ======="+js);
//
//        if (!deviceReady) {
//            Log.w(TAG, "====== device not ready  ====== add to queue next:"+js);
//            eventQueue.add(js);
//            return;
//        }
//        Runnable jsLoader = new Runnable() {
//            public void run() {
//                webView.loadUrl("javascript:" + js);
//            }
//        };
//        try {
//            Method post = webView.getClass().getMethod("post",Runnable.class);
//            post.invoke(webView,jsLoader);
//        } catch(Exception e) {
//
//            ((Activity)(webView.getContext())).runOnUiThread(jsLoader);
//        }
//    }





//    public void sendNotifyToJavascript(Bundle b){
//        Log.w(TAG, "=============sendNotifyToJavascript=======Bundle b.size: "+b.size());
//        Object beaconData = "";
//        Object actionLocationType ="";
//        for(String key: b.keySet()){
//            if(key.equals("data")){
//                beaconData = b.get(key);
//            }
//            if(key.equals("actionLocationType")){
//                actionLocationType = b.get(key);
//            }
//
//        }
//        String jsStr = "if(handleIncomingNotification){handleIncomingNotification('"+actionLocationType+"', "+beaconData+")}";
//        sendJavascript(jsStr);
//    }






    public void asyncSuccess(CallbackContext callbackContext, JSONObject msgObject){
        if(callbackContext == null){
            return;
        }
        PluginResult pluginResult = new KeepPluginResult(PluginResult.Status.OK, msgObject);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }

    public void asyncSuccess(CallbackContext callbackContext, String msgObject){
        if(callbackContext == null){
            return;
        }
        JSONObject jso = new JSONObject();
        try {
            jso.put("eventType", "log");
            jso.put("msg", msgObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PluginResult pluginResult = new KeepPluginResult(PluginResult.Status.OK, jso);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }


    private boolean isServiceRunning(){
        return BeaconsUtils.isServiceRunning("com.aaw.beaconsmanager.AltMonitoring");
    }

    private void startServiceIfNotRunning(Callback callback){
        //if(isServiceRunning() == false){
            startService(callback);
//        }else{
//            callback.call();
//        }
    }

}

class KeepPluginResult extends PluginResult{

    {
        super.setKeepCallback(true);
    }

    public KeepPluginResult(Status status) {
        super(status);
    }

    public KeepPluginResult(Status status, String message) {
        super(status, message);
    }

    public KeepPluginResult(Status status, JSONArray message) {
        super(status, message);
    }

    public KeepPluginResult(Status status, JSONObject message) {
        super(status, message);
    }



}



class Callback{
    public void call(){}
}

