//
//  ExtBeacon.m
//  iBeaconTemplate
//
//  Created by 1 on 02.10.15.
//  Copyright © 2015 iBeaconModules.us. All rights reserved.
//

#import <Foundation/Foundation.h>


@interface ExtBeacon : NSObject{
    int id;
    NSString *uuid;
    int actionType;
    NSString *msg;
    NSString *data;
    CLBeaconRegion *region;
}

@property int id ;
@property NSString *uuid;
@property int actionType;
@property NSString *data;
@property NSString *msg;
@property CLBeaconRegion *region;

@end



@implementation ExtBeacon


@synthesize id;
@synthesize uuid;
@synthesize actionType;
@synthesize msg;
@synthesize data;
@synthesize region;


-(void)encodeWithCoder:(NSCoder *)encoder
{
    [encoder encodeObject: [NSNumber numberWithInt: self.id] forKey:@"id"];
    [encoder encodeObject:  self.uuid forKey:@"uuid"];
    [encoder encodeObject: [NSNumber numberWithInt: self.actionType] forKey:@"actionType"];
    [encoder encodeObject:  self.msg forKey:@"msg"];
    [encoder encodeObject:  self.data forKey:@"data"];
    
    [encoder encodeObject:  self.region forKey:@"region"];
}

-(id)initWithCoder:(NSCoder *)decoder
{
    self.id = [[decoder decodeObjectForKey:@"id"] intValue];
    self.uuid = [decoder decodeObjectForKey:@"uuid"];
    self.actionType = [[decoder decodeObjectForKey:@"actionType"] intValue];
    self.msg = [decoder decodeObjectForKey:@"msg"];
    self.data = [decoder decodeObjectForKey:@"data"];
    self.region = [decoder decodeObjectForKey:@"region"];
    

    return self;
}


+(ExtBeacon*)fillBeaconFromDictionary : (NSDictionary *) dict{
    
    ExtBeacon *eBeacon = [[ExtBeacon alloc] init];
    
    eBeacon.id = [[dict valueForKey:@"id" ] integerValue];
    eBeacon.uuid = (NSString*)[dict objectForKey:@"uuid" ];
    eBeacon.actionType = [[dict valueForKey:@"actionType" ]integerValue];
    eBeacon.msg = (NSString*)[dict objectForKey:@"msg" ];
    eBeacon.data = (NSString*)[dict objectForKey:@"data" ];


    return eBeacon;
    
}



@end