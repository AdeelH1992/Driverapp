package com.example.lenovossd.driverapp;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lenovossd.driverapp.Common.Common;
import com.example.lenovossd.driverapp.Model.FCMResponse;
import com.example.lenovossd.driverapp.Model.Notification;
import com.example.lenovossd.driverapp.Model.Sender;
import com.example.lenovossd.driverapp.Model.Token;
import com.example.lenovossd.driverapp.Remote.IFCMService;
import com.example.lenovossd.driverapp.Remote.IGoogleAPI;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerCall extends AppCompatActivity {

    TextView txtTime,txtAddress,txtDistance;

    Button btnCancel, btnAccept;

    MediaPlayer mediaPlayer;

    IGoogleAPI mServices;

    IFCMService mFCMService;

    String customerId;

    double lat,lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_customer_call );

        mServices = Common.getGooogleAPI();

        mFCMService = Common.getFCMService();

        // init View

        txtTime = (TextView) findViewById( R.id.txtTime);
        txtAddress = (TextView) findViewById( R.id.txtAddress);
        txtDistance = (TextView) findViewById( R.id.txtDistance);
        btnAccept = (Button) findViewById( R.id.btn_Accept );
        btnCancel = (Button) findViewById( R.id.btn_Decline );

        btnCancel.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    if (!TextUtils.isEmpty( customerId ))
                        cancelBooking(customerId);



            }
        } );

        btnAccept.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendOntheWayNotification(customerId);
                Intent intent = new Intent( CustomerCall.this,DriverTracking.class );
                intent .putExtra( "lat",lat );
                intent.putExtra( "lng",lng );
                intent.putExtra( "customerId",customerId );
                startActivity( intent );
                finish();
            }
        } );






        mediaPlayer = MediaPlayer.create( this,R.raw.clock_tick_sound );
        mediaPlayer.setLooping( true );
        mediaPlayer.start();

        if (getIntent() != null)
        {
             lat = getIntent().getDoubleExtra( "lat",-1.0 );
             lng = getIntent().getDoubleExtra( "lng",-1.0 );

            customerId = getIntent().getStringExtra( "customer" );


          // getDirection(lat,lng);

            //get address
           String address = customeraddress(lat,lng);
           txtAddress.setText( address );
            // getting distance between driver and rider

            distancebetween();




        }



    }

    private void sendOntheWayNotification(String customerId) {
        Token token = new Token( customerId );
        Notification notification = new Notification( "OnTheWay",String.format("The driver %s is on his Way",Common.currentUser.getName()) );
        Sender sender = new Sender( token.getToken(),notification );

        mFCMService.sendMessage( sender ).enqueue( new Callback <FCMResponse>() {
            @Override
            public void onResponse(Call <FCMResponse> call, Response <FCMResponse> response) {
                if(response.body().success != 1)
                    Toasty.error(CustomerCall.this, "Failed ! .", Toast.LENGTH_SHORT, true).show();
            }

            @Override
            public void onFailure(Call <FCMResponse> call, Throwable t) {

            }
        } );
    }

    // calculate the distance between starting point and ending point
    private void distancebetween() {
        LatLng Driver = new LatLng( Common.mLastLocation.getLatitude(),Common.mLastLocation.getLongitude() );
        LatLng rider = new LatLng( lat,lng );
        Location driverA = new Location("point A");
        driverA.setLatitude( Driver.latitude);
        driverA.setLongitude(Driver.longitude  );
        Location riderB = new Location("point B");
        riderB.setLatitude(rider.latitude);
        riderB.setLongitude(rider.longitude);

        double distance = driverA.distanceTo(riderB);
        double roundvalue =  Math.round(distance );
        int i = (int) roundvalue;

        if(roundvalue<=1000) {
            txtDistance.setText( Double.toString( roundvalue ) + " m" );
        }
        else
        {
            txtDistance.setText( Double.toString( roundvalue/1000 ) + " km" );
        }
    }

    // convert the lat lng to string address
    private String customeraddress(double lat, double lng) {
        String myCity="";

        Geocoder geocoder = new Geocoder( CustomerCall.this, Locale.getDefault() );
        try {
            List<Address> addresses = geocoder.getFromLocation( lat,lng,3);
            String address = addresses.get( 0 ).getAddressLine( 0 );
            myCity = address;


            Log.d("complete address","Address : " +address.toString() );

        } catch (IOException e) {
            e.printStackTrace();
        }
        return myCity;
    }

    private void cancelBooking(String customerId) {

        Token token = new Token( customerId );

        Notification notification = new Notification( "Cancel","Driver has cancelled your Ruquest" );
        Sender sender = new Sender( token.getToken(),notification );

        mFCMService.sendMessage( sender )
                .enqueue( new Callback <FCMResponse>() {
                    @Override
                    public void onResponse(Call <FCMResponse> call, Response <FCMResponse> response) {
                        if (response.body().success == 1)
                        {
                            Toasty.error(CustomerCall.this, "You Cancelled the Request", Toast.LENGTH_SHORT, true).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call <FCMResponse> call, Throwable t) {

                    }
                } );
    }

    private void getDirection(double lat,double lng) {



        String requestApi = null;
        try{
            requestApi =  "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin="+ Common.mLastLocation.getLatitude()+","+ Common.mLastLocation.getLongitude()+"&"+
                    "destination="+lat+","+lng+"&"+
                    "key="+getResources().getString( R.string.google_direction_api);

            Log.d("direction",requestApi.toString());

            mServices.getPath( requestApi )
                    .enqueue( new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try
                            {
                                JSONObject jsonObject = new JSONObject( response.body().toString());

                                JSONArray routes = jsonObject.getJSONArray( "routes" );

                                JSONObject object = routes.getJSONObject( 0 );

                                JSONArray legs = object.getJSONArray( "legs" );

                                JSONObject legsObject = legs.getJSONObject( 0);

                                // get distance

                                JSONObject distance = legsObject .getJSONObject( "distance" );

                                txtDistance.setText( distance.getString( "text" ) );

                                // get time

                                JSONObject time = legsObject .getJSONObject( "duration" );

                                txtTime.setText( time.getString( "text" ) );

                                // get address

                                String address = legsObject .getString( "end_address" );

                                txtAddress.setText( address );







                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call <String> call, Throwable t) {
                            Toasty.error( CustomerCall.this, ""+t.getMessage(), Toast.LENGTH_SHORT, true).show();

                        }
                    } );
        }catch (Exception e )
        {
            e.printStackTrace();

        }

    }

    @Override
    protected void onStop() {
        mediaPlayer.release();
        super.onStop();
    }

    @Override
    protected void onPause() {
        mediaPlayer.release();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mediaPlayer.start();
    }
}
