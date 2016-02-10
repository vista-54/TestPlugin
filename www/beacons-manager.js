
    var exec    = require('cordova/exec'),
    channel = require('cordova/channel');

    function BeaconsManager() {
    };
    if (!window.plugins) {
        window.plugins = {};
    }
    if (!window.plugins.BeaconsManager) {
        window.plugins.BeaconsManager = new BeaconsManager();
    }


    //=================   SERVICE  ===================

    BeaconsManager.prototype.startService = function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "BeaconsManagerPlugin", "startService", []);
    };

    BeaconsManager.prototype.stopService = function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "BeaconsManagerPlugin", "stopService", []);
    };


    //=================   MONITORING  ===================


    BeaconsManager.prototype.startMonitoring = function (successCallback, errorCallback, array) {
        cordova.exec(successCallback, errorCallback, "BeaconsManagerPlugin", "startMonitoring", [array]);
    };

    BeaconsManager.prototype.stopMonitoring = function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "BeaconsManagerPlugin", "stopMonitoring", []);
    };

    //===================   RANGING   ====================

    BeaconsManager.prototype.startRanging = function (successCallback, errorCallback, array) {
        cordova.exec(successCallback, errorCallback, "BeaconsManagerPlugin", "startRanging", [array]);
    };

    BeaconsManager.prototype.stopRanging = function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "BeaconsManagerPlugin", "stopRanging", []);
    };


    //=====
    BeaconsManager.prototype.applyParameters = function (successCallback, errorCallback, params) {
        cordova.exec(successCallback, errorCallback, "BeaconsManagerPlugin", "applyParameters", [params]);
    };

    var monitoringAsyncFunc;

    monitoringAsyncFunc = function (result){
        console.log('result: '+JSON.stringify(result) );
    }

    BeaconsManager.prototype.setMonitoringFunction = function(func){
        //monitoringAsyncFunc = func;
        cordova.exec(
            func,//function(res){},
            function(e){alert('error sent onDeviceReady: '+e);},
            'BeaconsManagerPlugin', 'monitoringFunctionSet', []);
    }



    channel.deviceready.subscribe(function () {
        // Device is ready now, the listeners are registered
        // and all queued events can be executed.
        cordova.exec(
            monitoringAsyncFunc,
            function(e){alert('error sent onDeviceReady: '+e);},
            'BeaconsManagerPlugin', 'onDeviceReady', []);
    });


    module.exports = BeaconsManager;

