package com.example.lenovossd.driverapp.Common;

import android.content.IntentSender;
import android.location.Location;

import com.example.lenovossd.driverapp.Model.User;
import com.example.lenovossd.driverapp.Remote.FCMClient;
import com.example.lenovossd.driverapp.Remote.IFCMService;
import com.example.lenovossd.driverapp.Remote.IGoogleAPI;
import com.example.lenovossd.driverapp.Remote.RetrofitClient;

public class Common {


    public static final String driver_tb1 = "Drivers";
    public static final String user_driver_tb1 = "DriversInformation";
    public static final String user_rider_tb1 = "RidersInformation";
    public static final String pickup_request_tb1 = "PickupRequets";
    public static final String token_tb1 = "Tokens";
    public static final String baseURL = "https://maps.googleapis.com";
    public static final String fcmURL = "https://fcm.googleapis.com";
    public static final String user_field ="usr";
    public static final String pwd_field ="pwd";


    public static final int PICK_IMAGE_REQUEST = 9999;
    public static User currentUser = new User(  ) ;
    public static double base_fare=100;
    public static double time_rate=4.67;
    public static double distance_rate=12;

    public static Location mLastLocation=null;


    public static double formulaPrice(double km)
{
    return (base_fare+(distance_rate*km));

}

    public static IGoogleAPI getGooogleAPI()
    {
        return RetrofitClient.getClient( baseURL ).create(IGoogleAPI .class );

    }

      public static IFCMService getFCMService()
    {
        return FCMClient.getClient( fcmURL ).create(IFCMService .class );


    }

}
