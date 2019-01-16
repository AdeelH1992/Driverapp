package com.example.lenovossd.driverapp;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Toast;

import com.example.lenovossd.driverapp.Common.Common;
import com.example.lenovossd.driverapp.Helper.DirectionJSONParser;
import com.example.lenovossd.driverapp.Model.FCMResponse;
import com.example.lenovossd.driverapp.Model.Notification;
import com.example.lenovossd.driverapp.Model.Sender;
import com.example.lenovossd.driverapp.Model.Token;
import com.example.lenovossd.driverapp.Remote.IFCMService;
import com.example.lenovossd.driverapp.Remote.IGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverTracking extends FragmentActivity implements OnMapReadyCallback,
GoogleApiClient.OnConnectionFailedListener,
GoogleApiClient.ConnectionCallbacks,
        LocationListener{

    private GoogleMap mMap;

    double riderLat,riderLng;

    // play service

    private static final int PLAY_SERVICE_RES_REQUEST = 300193;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;



    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    private Circle riderMarker;

    private Marker driverMarker;

    private Polyline direction;

    String customerId;
    IGoogleAPI mServices;

    IFCMService mFCMService;
    GeoFire geoFire;
    Button btnStartTrip;
    Location pickupLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_driver_tracking );
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById( R.id.map );
        mapFragment.getMapAsync( this );

        if (getIntent() != null)
        {
            riderLat = getIntent().getDoubleExtra( "lat",-1.0 );
            riderLng = getIntent().getDoubleExtra( "lng",-1.0 );
            customerId = getIntent().getStringExtra( "customerId" );
        }
        mServices = Common.getGooogleAPI();
        mFCMService = Common.getFCMService();
        setUpLoaction();

        btnStartTrip = findViewById( R.id.btnStartTrip );
        btnStartTrip.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btnStartTrip.getText().equals( "START TRIP" ))
                {
                    pickupLocation = Common.mLastLocation;
                    btnStartTrip.setText( "DROP OFF HERE" );
                }
                else if(btnStartTrip.getText().equals( "DROP OFF HERE" ))
                {
                    calculateCashFee(pickupLocation,Common.mLastLocation);
                }
            }
        } );
    }

    private void calculateCashFee(Location pickupLocation, Location mLastLocation) {

        String d_value;
        int distance_value;
        String start_address = customeraddress( pickupLocation.getLatitude(),pickupLocation.getLongitude() ) ;
        String end_address = customeraddress( Common.mLastLocation.getLatitude(),Common.mLastLocation.getLongitude() ) ;

        if (distancebetween()<1000)
        {
             distance_value = distancebetween();
             d_value = distance_value+" m";

        }
        else
        {
             distance_value = distancebetween();
             d_value = distance_value +" km";

        }



        sendDropoffNotification(customerId);

        Intent intent = new Intent( DriverTracking.this,TripDetail.class );
        intent.putExtra( "start_address",start_address );
        intent.putExtra( "end_address",end_address );
        intent.putExtra( "distance",d_value );// distance betweeen
        intent.putExtra( "total",(int)Common.formulaPrice(distance_value ) );
        intent.putExtra( "location_start",String.format( "%f,%f", pickupLocation.getLatitude(),pickupLocation.getLongitude()) );
        intent.putExtra( "location_end",String.format( "%f,%f", Common.mLastLocation.getLatitude(),Common.mLastLocation.getLongitude()) );
        startActivity( intent );
        finish();


    }

    private int distancebetween() {
        LatLng start_address = new LatLng( pickupLocation.getLatitude(),pickupLocation.getLongitude() );
        LatLng end_address = new LatLng( Common.mLastLocation.getLatitude(),Common.mLastLocation.getLongitude() );
        Location driverA = new Location("point A");
        driverA.setLatitude( start_address.latitude);
        driverA.setLongitude(start_address.longitude  );
        Location riderB = new Location("point B");
        riderB.setLatitude(end_address.latitude);
        riderB.setLongitude(end_address.longitude);

        double distance = driverA.distanceTo(riderB);
        double roundvalue =  Math.round(distance );
        int i = (int) roundvalue;
        return  i;

    }
    private String customeraddress(double lat, double lng) {
        String myCity="";

        Geocoder geocoder = new Geocoder(DriverTracking.this, Locale.getDefault() );
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
    private void setUpLoaction() {


            if (checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
                displayLocation();


            }

    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval( UPDATE_INTERVAL );
        mLocationRequest.setFastestInterval( FASTEST_INTERVAL );
        mLocationRequest.setPriority( LocationRequest.PRIORITY_HIGH_ACCURACY );
        mLocationRequest.setSmallestDisplacement( DISPLACEMENT );

    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder( this )
                .addConnectionCallbacks( this )
                .addOnConnectionFailedListener( this )
                .addApi( LocationServices.API )
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable( this );

        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError( resultCode ))
                GooglePlayServicesUtil.getErrorDialog( resultCode, this, PLAY_SERVICE_RES_REQUEST ).show();
            else {
                Toasty.error(DriverTracking.this, "This device is not supported ", Toast.LENGTH_SHORT, true).show();
                finish();
            }
            return false;
        }
        return true;

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        riderMarker = mMap.addCircle( new CircleOptions()
        .center( new LatLng( riderLat,riderLng ) )
        .radius( 50 )
        .strokeColor( Color.BLACK )
        .fillColor( 0x22000FF )
        .strokeWidth( 7.0f ) );

        geoFire = new GeoFire( FirebaseDatabase.getInstance().getReference(Common.driver_tb1) );
        GeoQuery geoQuery = geoFire.queryAtLocation( new GeoLocation( riderLat,riderLng ),0.05f );
        geoQuery.addGeoQueryEventListener( new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                sendArrivedNotification(customerId);
                btnStartTrip.setEnabled( true );


            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        } );




    }

    private void sendArrivedNotification(String customerId) {
        Token token = new Token( customerId );
        Notification notification = new Notification( "Arrived",String.format("The driver %s has arrived at your location",Common.currentUser.getName()) );
        Sender sender = new Sender( token.getToken(),notification );

        mFCMService.sendMessage( sender ).enqueue( new Callback <FCMResponse>() {
            @Override
            public void onResponse(Call <FCMResponse> call, Response <FCMResponse> response) {
                if(response.body().success != 1)
                    Toasty.error(DriverTracking.this, "Failed ! .", Toast.LENGTH_SHORT, true).show();
            }

            @Override
            public void onFailure(Call <FCMResponse> call, Throwable t) {

            }
        } );
    }

    private void sendDropoffNotification(String customerId) {
        Token token = new Token( customerId );
        Notification notification = new Notification( "DropOff",customerId );
        Sender sender = new Sender( token.getToken(),notification );

        mFCMService.sendMessage( sender ).enqueue( new Callback <FCMResponse>() {
            @Override
            public void onResponse(Call <FCMResponse> call, Response <FCMResponse> response) {
                if(response.body().success != 1)
                    Toasty.error(DriverTracking.this, "Failed ! .", Toast.LENGTH_SHORT, true).show();
            }

            @Override
            public void onFailure(Call <FCMResponse> call, Throwable t) {

            }
        } );
    }



    private void displayLocation() {

        if(ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_COARSE_LOCATION  )!= PackageManager.PERMISSION_GRANTED&&
                ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION   )!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        Common.mLastLocation = LocationServices.FusedLocationApi.getLastLocation( mGoogleApiClient );
        if (Common.mLastLocation != null)
        {

                final double latitude = Common.mLastLocation.getLatitude();
                final double longitude = Common.mLastLocation.getLongitude();

                if(driverMarker!= null)

                    driverMarker.remove();
                    driverMarker= mMap.addMarker( new MarkerOptions().position( new LatLng( latitude,longitude ) )
                    .title( "YOU" )
                    .icon( BitmapDescriptorFactory.defaultMarker() ));
                    mMap.animateCamera( CameraUpdateFactory.newLatLngZoom( new LatLng( latitude,longitude ),17.0f ) );
                    if(direction != null)
                        direction.remove();
                   // getDirection();

        }
        else {
            Log.d("Error","Cannot get your Location");
        }
    }

    private void getDirection() {

      LatLng  currentPosition = new LatLng( Common.mLastLocation.getLatitude(),Common.mLastLocation.getLongitude());

        String requestApi = null;
        try{
            requestApi =  "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin="+currentPosition.latitude+","+ currentPosition.longitude+"&"+
                    "destination="+riderLat+","+ riderLng+"&"+
                    "key="+getResources().getString( R.string.google_direction_api);

            Log.d("direction",requestApi.toString());

            mServices.getPath( requestApi )
                    .enqueue( new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try
                            {
                                new ParserTask().execute(response.body().toString());

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call <String> call, Throwable t) {
                            Toasty.error( DriverTracking.this, ""+t.getMessage(), Toast.LENGTH_SHORT, true).show();

                        }
                    } );
        }catch (Exception e )
        {
            e.printStackTrace();

        }
    }


    private void startLocationUpdate() {

        if(ActivityCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION  )!= PackageManager.PERMISSION_GRANTED&&
                ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION   )!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates( mGoogleApiClient,mLocationRequest,this );


    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdate();

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Common.mLastLocation = location;
        displayLocation();

    }

    private class ParserTask extends AsyncTask<String,Integer,List<List<HashMap<String,String>>>>
    {
        ProgressDialog mDialog = new ProgressDialog( DriverTracking.this );

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog.setMessage( "Please Waiting ..." );
            mDialog.show();
        }

        @Override
        protected List <List <HashMap <String, String>>> doInBackground(String... strings) {
            JSONObject jobject ;
            List <List <HashMap <String, String>>> routes = null ;

            try{
                jobject = new JSONObject( strings[0]);
                DirectionJSONParser parser = new DirectionJSONParser();
                routes = parser.parse( jobject );
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List <List <HashMap <String, String>>> lists) {
            mDialog.dismiss();
            ArrayList points = null;

            PolylineOptions polylineOptions = null;

            for(int i = 0 ;i<lists.size();i++)
            {
                points = new ArrayList(  );

                polylineOptions = new PolylineOptions();

                List<HashMap<String,String>> path = lists.get( i );

                for(int j = 0 ;i<path.size();j++)
                {
                    HashMap<String,String> point = path.get( j );
                    double lat = Double.parseDouble( point.get("lat") );
                    double lng = Double.parseDouble( point.get("lng") );

                    LatLng position = new LatLng( lat,lng );

                    points.add(position);

                }
                polylineOptions.addAll( points );
                polylineOptions.width( 18 );
                polylineOptions.color( Color.BLACK );
                polylineOptions.geodesic( true );
            }
           direction= mMap.addPolyline( polylineOptions );
        }
    }
}
