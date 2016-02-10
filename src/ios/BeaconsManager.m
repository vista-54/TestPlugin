

#import <Foundation/Foundation.h>

#import "BeaconsManager.h"
#import <CoreLocation/CoreLocation.h>
#import "ExtBeacon.h" 

NSArray *staticEBA = nil;

@implementation BeaconsManager




+ (NSArray*)extBeaconsArray{
    return staticEBA;
}
+ (void)setExtBeaconsArray:(NSArray*)newEBA{
    staticEBA = newEBA;
}




+ (CLLocationManager*)lm {
    return staticLM;
}

+ (void)setLM:(CLLocationManager*)newLM {
    staticLM = newLM;
}








- (void)onDomDelegateReady:(CDVInvokedUrlCommand*)command{
    NSLog(@"=== BeaconsManager onDomDelegateReady");
}

- (void)pluginInitialize
{
    NSLog(@"=== BeaconsManager pluginInitialize");

    
    // [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(finishLaunching) name:UIApplicationDidFinishLaunchingNotification object:nil];
    
    BeaconsManager *bManager = [[BeaconsManager alloc]init];
    AppDelegate *appDelegate = (AppDelegate*)[[UIApplication sharedApplication]delegate];
    
    appDelegate.bManagerAO = bManager;
    
    //[self init];
}


-(id)init{
    self = [super init];
    
    
    self.locationManager = [[CLLocationManager alloc] init];
    if([self.locationManager respondsToSelector:@selector(requestAlwaysAuthorization)]) {
        [self.locationManager requestAlwaysAuthorization];
    }
    
    if ([UIApplication instancesRespondToSelector:@selector(registerUserNotificationSettings:)]) {
        [[UIApplication sharedApplication] registerUserNotificationSettings:[UIUserNotificationSettings settingsForTypes:UIUserNotificationTypeAlert|UIUserNotificationTypeSound|UIUserNotificationTypeBadge
                                                                                                              categories:nil]];
    }
    
    self.locationManager.delegate = self;
    self.locationManager.pausesLocationUpdatesAutomatically = NO;
    
    
    [BeaconsManager setLM:self.locationManager];
    
    [BeaconsManager setExtBeaconsArray:@[]];
    
    
    
    return self;
}

//=== cordova plugins fuctions


-(void)startScan:(CDVInvokedUrlCommand *)command{
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"OK"];
    @try {
        [self startScanInner:command.arguments];
    }
    @catch (NSException *exception) {
        NSString *errMsg = [NSString stringWithFormat:@"Error:: %@  by Reason :: %@", exception.name, exception.reason ];
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errMsg];
    }
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}


-(void)stopScan:(CDVInvokedUrlCommand *)command{
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@"OK"];
    @try {
        [self stopScanInner];
    }
    @catch (NSException *exception) {
        NSString *errMsg = [NSString stringWithFormat:@"Error:: %@  by Reason :: %@", exception.name, exception.reason ];
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errMsg];
    }
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}




-(void)startScanInner: (NSArray*)beaconsArr{
    [self stopScanInner];

        switch ([CLLocationManager authorizationStatus]) {
            case kCLAuthorizationStatusAuthorizedAlways:
                NSLog(@"Authorized Always");
                break;
            case kCLAuthorizationStatusAuthorizedWhenInUse:
                NSLog(@"Authorized when in use");
                break;
            case kCLAuthorizationStatusDenied:
                NSLog(@"Denied");
                break;
            case kCLAuthorizationStatusNotDetermined:
                NSLog(@"Not determined");
                break;
            case kCLAuthorizationStatusRestricted:
                NSLog(@"Restricted");
                break;
                
            default:
                break;
        }
    
    
    AppDelegate *appDelegate = (AppDelegate*)[[UIApplication sharedApplication]delegate];
    //self.locationManager = ((BeaconsManager*)[appDelegate bManager ]).locationManager ;
    self.locationManager = BeaconsManager.lm;
    
    NSMutableArray *incomingArr = [NSMutableArray array];
    
    
    for (int i=0; i<[beaconsArr count]; i++) {
        NSDictionary *beaconDict = [beaconsArr objectAtIndex:i];

        
        ExtBeacon *currBeacon =  [ExtBeacon fillBeaconFromDictionary :beaconDict ];
        NSString *uuid = currBeacon.uuid;
        
        // Override point for customization after application launch.
        NSUUID *beaconUUID = [[NSUUID alloc] initWithUUIDString: uuid];
        NSString *idStr = [[NSNumber numberWithInt: currBeacon.id] stringValue];
        NSString *regionIdentifier = [@"us.iBeaconModules_" stringByAppendingString : idStr ];
        CLBeaconRegion *beaconRegion = [[CLBeaconRegion alloc] initWithProximityUUID:beaconUUID identifier:regionIdentifier];
        beaconRegion.notifyEntryStateOnDisplay = YES;
        
        currBeacon.region = beaconRegion;
        
        
        [self.locationManager startMonitoringForRegion:beaconRegion];
        [self.locationManager startRangingBeaconsInRegion:beaconRegion];
        

        [incomingArr addObject: currBeacon];


    }
    
    [BeaconsManager setExtBeaconsArray:incomingArr]; //staticEBA = incomingArr;
    //[appDelegate setGlobalArray:incomingArr];
    
    NSData *data = [NSKeyedArchiver archivedDataWithRootObject:incomingArr];
    [[NSUserDefaults standardUserDefaults] setObject : data forKey:@"incomingArray"];
    [[NSUserDefaults standardUserDefaults]synchronize];
    
    
    [self.locationManager startUpdatingLocation];
    
    

}


-(void)stopScanInner{
    
    //AppDelegate *appDelegate = (AppDelegate*)[[UIApplication sharedApplication]delegate];
    //BeaconsManager *bm = appDelegate.bManagerAO;

    
    self.locationManager = BeaconsManager.lm ;
    
   
    if (self.locationManager != nil)
    {
        NSSet *monitoredRegionsSet = [self.locationManager monitoredRegions];
        NSArray *monitoredRegionsArr = [monitoredRegionsSet allObjects];
        
        for (int i=0; i<[monitoredRegionsArr count]; i++) {
            CLBeaconRegion *currRegion = [monitoredRegionsArr objectAtIndex:i];
            [self.locationManager stopMonitoringForRegion: currRegion];
            [self.locationManager stopRangingBeaconsInRegion: currRegion];
        }
        
        [self.locationManager stopUpdatingLocation];
    }
    
}




-(void)locationManagerRRRRRR:(CLLocationManager *)manager didRangeBeacons:(NSArray *)beacons inRegion:(CLBeaconRegion *)region {
    NSString *message = @"";
    
    int gProximFar = 0;
    int gBeaconM = 0;

    
    if(beacons.count > 0) {
        CLBeacon *nearestBeacon = beacons.firstObject;
        //        if(nearestBeacon.proximity == self.lastProximity ||
        //           nearestBeacon.proximity == CLProximityUnknown) {
        //            return;
        //        }
        self.lastProximity = nearestBeacon.proximity;
        
        if((nearestBeacon.proximity== CLProximityNear)||(nearestBeacon.proximity == CLProximityImmediate)){
            gProximFar = 0;
            if(gBeaconM == 0){
                gBeaconM = 1;
                message = [NSString stringWithFormat:@" nearestBeacon.proximityUUID: %@, major: %@, minor %@",
                           nearestBeacon.proximityUUID , nearestBeacon.major, nearestBeacon.minor];
            }
        } else {
            gProximFar ++;
        }
        
        if(gProximFar >= 4){
            if(gBeaconM == 1){
                gBeaconM = 0;
                message = @"Thank you for stopping by Percy! gProximFar";
            }
        }
        
        switch(nearestBeacon.proximity) {
            case CLProximityFar:
                message = @"You are far away from the beacon";
                break;
            case CLProximityNear:
                message = @"You are near the beacon";
                break;
            case CLProximityImmediate:
                message = @"You are in the immediate proximity of the beacon";
                break;
            case CLProximityUnknown:
                return;
        }
        
        
    }
    /*else {
     message = @"No beacons are nearby";
     }*/
    
    if(message.length >2){
        NSLog(@"%@", message);
        //[BeaconsManager sendLocalNotificationWithMessage: message];
    }
}




-(void)locationManager:(CLLocationManager *)manager didEnterRegion:(CLRegion *)region {
    [manager startRangingBeaconsInRegion:(CLBeaconRegion*)region];
    NSLog(@"=======You entered the region.========");
    [self.locationManager startUpdatingLocation];
    
    AppDelegate *appDelegate = (AppDelegate*)[[UIApplication sharedApplication]delegate];

    
    
    //NSLog(@"=== staticEBA size is: %d", [[BeaconsManager extBeaconsArray] count] );
    
    
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSData *data = [defaults objectForKey:@"incomingArray"];
    NSArray *incomingArray = [NSKeyedUnarchiver unarchiveObjectWithData:data];
    
    NSLog(@"=== incomingArray size is: %d", [incomingArray count] );

    [BeaconsManager process:@"enter" : (CLBeaconRegion*)region];
    
//    ExtBeacon *searchedExtBeacon = [BeaconsManager findBeaconFromArray: incomingArray :region];
//    
//    
//    
//    if(searchedExtBeacon != nil){
//        NSLog(@"=== region is: %@", ((CLBeaconRegion*)region).proximityUUID );
//        [BeaconsManager sendLocalNotificationWithMessage: searchedExtBeacon.msg ];
//    }
    
    
 }
 
 -(void)locationManager:(CLLocationManager *)manager didExitRegion:(CLRegion *)region {
     [manager stopRangingBeaconsInRegion:(CLBeaconRegion*)region];
     [self.locationManager stopUpdatingLocation];
     
     
     
 
     NSLog(@"You exited the region.");
     //[BeaconsManager sendLocalNotificationWithMessage:@"Thank you for stopping by Percy! Check back for our next offer!"];
     [BeaconsManager process:@"exit" : (CLBeaconRegion*)region];
 }







-(void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray *)locations
{
    [self.locationManager stopUpdatingLocation];
    self.locationManager = nil;
}

- (void)locationManager:(CLLocationManager *)manager didUpdateToLocation:(CLLocation *)newLocation fromLocation:(CLLocation *)oldLocation
{//Use if you are supporting iOS 5
    [self.locationManager stopUpdatingLocation];
    self.locationManager = nil;
}



+(void)process: (NSString*) actionLocationType : (CLBeaconRegion*)beaconRegion {
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSData *data = [defaults objectForKey:@"incomingArray"];
    NSArray *incomingArray = [NSKeyedUnarchiver unarchiveObjectWithData:data];
    
    //AppDelegate *appDelegate = (AppDelegate*)[[UIApplication sharedApplication]delegate];
    
    ExtBeacon *searchedExtBeacon = [BeaconsManager findBeaconFromArray: incomingArray : beaconRegion];

    if(searchedExtBeacon == nil){
        NSLog(@"=== Saved beacon not found ===");
        // todo return;
    }
    
    NSString *msg = searchedExtBeacon.msg;
    int actionType = searchedExtBeacon.actionType;
    NSString *dataStr = searchedExtBeacon.data;
    
    NSDictionary *backParams = @{@"data": dataStr, @"actionLocationType": actionLocationType};
    
    switch (actionType) {
        case 0:
            break;
    
        case 1:
            
        [BeaconsManager sendLocalNotificationWithMessage: msg : dataStr : backParams];
            break;

    }
        
}


+(void)sendLocalNotificationWithMessage:(NSString*)message : (NSString*)actionJson : (NSDictionary*) userInfo  {
    UILocalNotification *notification = [[UILocalNotification alloc] init];
    notification.alertBody = message;
    notification.soundName = UILocalNotificationDefaultSoundName;
    notification.alertAction = @"More info";
    notification.userInfo = userInfo;
    // [[UIApplication sharedApplication] scheduleLocalNotification:notification];
    [[UIApplication sharedApplication] presentLocalNotificationNow:notification];
}


//============  utils ===========

+(ExtBeacon*)findBeaconFromArray : (NSArray*)arr : (CLBeaconRegion*)region {
    
    for (int i=0; i<[arr count]; i++) {
        ExtBeacon *currBeacon = [arr objectAtIndex:i];
        CLBeaconRegion *currRegion= currBeacon.region;
        if([region isEqual: currRegion]){
            return currBeacon;
        }
    }
    return nil;
}



@end