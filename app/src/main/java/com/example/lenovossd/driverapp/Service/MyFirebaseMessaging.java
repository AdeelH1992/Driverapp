package com.example.lenovossd.driverapp.Service;

import android.content.Intent;

import com.example.lenovossd.driverapp.CustomerCall;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

public class MyFirebaseMessaging extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        LatLng customer_location = new Gson().fromJson( remoteMessage.getNotification().getBody(),LatLng.class);

        Intent intent = new Intent( getBaseContext(),CustomerCall.class );
        intent.putExtra( "lat",customer_location.latitude );
        intent.putExtra( "lng",customer_location.longitude );
        intent.putExtra( "customer",remoteMessage.getNotification().getTitle() );
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

       startActivity( intent );
    }
}
