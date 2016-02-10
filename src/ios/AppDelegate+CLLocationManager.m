

#import <Foundation/Foundation.h>

#import "AppDelegate+CLLocationManager.h"
#import <objc/runtime.h>


@implementation AppDelegate (CLLocationManager)


@dynamic bManagerAO;

-(void)setBManagerAO: (id) bMan{
    objc_setAssociatedObject(self, @selector(bManagerAO), bMan, OBJC_ASSOCIATION_RETAIN_NONATOMIC );
}

-(id)bManagerAO{
    return objc_getAssociatedObject(self, @selector(bManagerAO));
}


//============================

+ (void)load {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        
        Class class = [self class];
        
        SEL originalSelector = @selector(application:didFinishLaunchingWithOptions:);
        SEL swizzledSelector = @selector(bm_application:didFinishLaunchingWithOptions:);
        
        Method originalMethod = class_getInstanceMethod(class, originalSelector);
        Method swizzledMethod = class_getInstanceMethod(class, swizzledSelector);
        
        BOOL didAddMethod = class_addMethod(class, originalSelector, method_getImplementation(swizzledMethod), method_getTypeEncoding(swizzledMethod));
        
        if (didAddMethod) {
            class_replaceMethod(class, swizzledSelector, method_getImplementation(originalMethod), method_getTypeEncoding(originalMethod));
        } else {
            method_exchangeImplementations(originalMethod, swizzledMethod);
        }
        
    });


}


- (BOOL) bm_application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    
    
    return [self bm_application:application didFinishLaunchingWithOptions:launchOptions];
    
}



@end
