package com.example.lenovossd.driverapp.Remote;

import com.example.lenovossd.driverapp.Model.FCMResponse;
import com.example.lenovossd.driverapp.Model.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {

    @Headers( {
            "Content-Type:application/json",
            "Authorization:key=AAAACnhrhKM:APA91bF8G9nEEla4iNz7k37-Awdof4V7J4ZzFBMr4-px0J0bWI5w_b4blk--OgY6yW-c-90Oy6wx0hQEm8sqQn_k7c86frRKtc7MOoNRSr5mTyOJzgiEO2PEt4xbn9C8TTK8y-_R0228"
    } )
    @POST("fcm/send")
    Call<FCMResponse> sendMessage (@Body Sender body);
}
