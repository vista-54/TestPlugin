

#import "AppDelegate.h"
#import <objc/runtime.h>

@interface AppDelegate (CLLocationManager)

@property(nonatomic, strong) id bManagerAO;

- (BOOL) bm_application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions;


@end