package com.example.lenovossd.driverapp;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lenovossd.driverapp.Common.Common;
import com.example.lenovossd.driverapp.Model.Token;
import com.example.lenovossd.driverapp.Model.User;
import com.example.lenovossd.driverapp.Remote.IGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.maps.android.SphericalUtil;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.Inflater;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;
import es.dmoral.toasty.Toasty;
import io.paperdb.Paper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DriverHome extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;


    // play servies

    private static final int MY_PERMISSION_REQUEST_CODE =7192;
    private static final int PLAY_SERVICE_RES_REQUEST = 300193;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;



    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;


    DatabaseReference drivers,userDrivers;

    GeoFire geoFire;

    Marker mcurrent;


    MaterialAnimatedSwitch location_switch;
    SupportMapFragment mapFragment;


    // Add car animation

    private List<LatLng> polyLineList;

    private Marker carMarker;
    private float v;
    private  double lat,lng;
    private Handler handler;

    private LatLng startPosition,endPosition,currentPosition;
    private int index,next;
    private PlaceAutocompleteFragment places;
    AutocompleteFilter typeFilter;
    private String destination;
    private PolylineOptions polylineOptions,blackpolylineOptions;
    private Polyline blackPolyline,greyPolyline;


    private IGoogleAPI mServices;
    CircleImageView imageAvatar;

    //Firebase Storage

    FirebaseStorage firebaseStorage;
    StorageReference storageReference;


    // presence system

    DatabaseReference onlineRef , CurrentUserRef;

    Runnable drawPathRunnable = new Runnable() {
        @Override
        public void run() {

            if(index<polyLineList.size()-1)
            {
                index++;
                next = index+1;
            }
            if(index<polyLineList.size()-1)
            {
                startPosition = polyLineList.get( index );
                endPosition = polyLineList.get( next );
            }
            final ValueAnimator valueAnimator = ValueAnimator.ofFloat( 0,1 );
            valueAnimator.setDuration( 3000 );
            valueAnimator.setInterpolator( new LinearInterpolator(  ) );
            valueAnimator.addUpdateListener( new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    v = valueAnimator.getAnimatedFraction();
                    lng = v*endPosition.longitude+(1-v)*startPosition.longitude;
                    lat = v*endPosition.latitude+(1-v)*startPosition.latitude;

                    LatLng newPos = new LatLng( lat,lng );
                    carMarker.setPosition( newPos );
                    carMarker.setAnchor( 0.5f,0.5f );
                    carMarker.setRotation( getBearing(startPosition,newPos) );
                    mMap.moveCamera( CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target( newPos )
                                    .zoom( 15.0f )
                                    .build()
                    ) );


                }
            } );
            valueAnimator.start();
            handler.postDelayed( this,3000 );


        }
    };

    private float getBearing(LatLng startPosition, LatLng endPosition) {
        double lat = Math.abs(startPosition.latitude - endPosition.latitude);
        double lng = Math.abs( startPosition.longitude - endPosition.longitude );

        if (startPosition.latitude < endPosition.latitude && startPosition.longitude < endPosition.longitude) {
            return (float) (Math.toDegrees( Math.atan( lng / lat ) ));
        } else if (startPosition.latitude >= endPosition.latitude && startPosition.longitude < endPosition.longitude) {
            return (float) ((90 - Math.toDegrees( Math.atan( lng / lat ) )) + 90);
        } else if (startPosition.latitude >= endPosition.latitude && startPosition.longitude >= endPosition.longitude) {
            return (float) ((Math.toDegrees( Math.atan( lng / lat ) )) + 180);
        } else if (startPosition.latitude < endPosition.latitude && startPosition.longitude >= endPosition.longitude) {
            return (float) ((90 - Math.toDegrees( Math.atan( lng / lat ) )) + 270);
        }
        return -1;


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_driver_home );
        Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );
        setSupportActionBar( toolbar );


        //init View

        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();


        userDrivers = FirebaseDatabase.getInstance().getReference( Common.user_driver_tb1);
        DrawerLayout drawer = (DrawerLayout) findViewById( R.id.drawer_layout );
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close );
        drawer.addDrawerListener( toggle );
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById( R.id.nav_view );
        navigationView.setNavigationItemSelectedListener( this );

        View navigationHeaderView = navigationView.getHeaderView( 0 );
        TextView txtName  = (TextView) navigationHeaderView.findViewById( R.id.txtDriverName );
        final TextView txtStars = (TextView) navigationHeaderView.findViewById( R.id.txtStars );
        imageAvatar = (CircleImageView) navigationHeaderView.findViewById( R.id.image_avatar );

        txtName.setText( Common.currentUser.getName() );
        txtStars.setText( Common.currentUser.getRates() );

        userDrivers.child( FirebaseAuth.getInstance().getCurrentUser().getUid() )
                .addValueEventListener( new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Common.currentUser = dataSnapshot.getValue( User.class );

                        txtStars.setText( Common.currentUser.getRates() );
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                } );



        if (Common.currentUser.getAvatarUrl() != null
                && !TextUtils.isEmpty(Common.currentUser.getAvatarUrl()))
        {
            Picasso.with( this )
                   .load(Common.currentUser.getAvatarUrl() )
                    .into(imageAvatar);
        }


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById( R.id.map );
        mapFragment.getMapAsync( this );


        // presence system

        onlineRef = FirebaseDatabase.getInstance().getReference().child( ".info/connected" );
        CurrentUserRef = FirebaseDatabase.getInstance().getReference( Common.driver_tb1)
                .child( FirebaseAuth.getInstance().getCurrentUser().getUid() );

        onlineRef.addValueEventListener( new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // we will remove value from driver tb1 when driver disconnected

                CurrentUserRef.onDisconnect().removeValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        } );

        location_switch =(MaterialAnimatedSwitch) findViewById( R.id.location_switch );
        location_switch.setOnCheckedChangeListener( new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isOnline) {

                if(isOnline)
                {
                    FirebaseDatabase.getInstance().goOnline(); // set connected when switch is on
                    startLocationUpdate();
                    displayLocation();
                    Toasty.success(DriverHome.this, "You Are Online Now You will Receive Job Request", Toast.LENGTH_SHORT, true).show();
                }
                else

                {
                    FirebaseDatabase.getInstance().goOffline();  // set disconnected when switch is off
                    stopLocationUpdate();
                    mcurrent.remove();
                    mMap.clear();
                    // handler.removeCallbacks( drawPathRunnable );
                    Toasty.error(DriverHome.this, "You Are Offline , You will not get any job Now,Please go online to get job Request ", Toast.LENGTH_SHORT, true).show();


                }

            }
        } );

        polyLineList = new ArrayList<>(  );

        typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter( AutocompleteFilter.TYPE_FILTER_ADDRESS )
                .setTypeFilter( 3 )
                .build();
        places = (PlaceAutocompleteFragment)getFragmentManager().findFragmentById( R.id.places_autoComplete_fragment );
        places.setOnPlaceSelectedListener( new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                if(location_switch.isChecked())
                {
                    destination = place.getAddress().toString();
                    destination = destination.replace( " " ,"+" );
                    //getDirection();
                }
                else
                {
                    Toasty.error(DriverHome.this, "Please Change Your status to online  " , Toast.LENGTH_SHORT, true).show();

                }

            }

            @Override
            public void onError(Status status) {
                Toasty.error(DriverHome.this, " " + status.toString(), Toast.LENGTH_SHORT, true).show();

            }
        } );

        // Geo Fire
        drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tb1);
        geoFire = new GeoFire( drivers );

        setUpLoaction();

        mServices = Common.getGooogleAPI();

        UpdateFirebaseToken();
    }

    private void UpdateFirebaseToken() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();

        DatabaseReference tokens = db.getReference( Common.token_tb1);


        Token token = new Token( FirebaseInstanceId.getInstance().getToken() );

        tokens.child( FirebaseAuth.getInstance().getCurrentUser().getUid() )
                .setValue( token );

    }

    private void getDirection() {

        currentPosition = new LatLng( Common.mLastLocation.getLatitude(),Common.mLastLocation.getLongitude());

        String requestApi = null;
        try{
            requestApi =  "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin="+currentPosition.latitude+","+ currentPosition.longitude+"&"+
                    "destination="+destination +"&"+
                    "key="+getResources().getString( R.string.google_direction_api);

            Log.d("direction",requestApi.toString());

            mServices.getPath( requestApi )
                    .enqueue( new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            try
                            {
                                JSONObject jsonObject = new JSONObject( response.body().toString());
                                JSONArray jsonArray = jsonObject.getJSONArray( "routes" );
                                for(int i=0;i<jsonArray.length();i++)
                                {
                                    JSONObject route =jsonArray.getJSONObject( i );
                                    JSONObject poly = route.getJSONObject( "overveiwpolyline" );
                                    String polyline = poly.getString( "points" );
                                    polyLineList = decodePoly(polyline);

                                    // Adjusting Bounds

                                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                    for(LatLng latLng:polyLineList)
                                        builder.include( latLng );
                                    LatLngBounds bounds = builder.build();
                                    CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds( bounds,2 );
                                    mMap.animateCamera( mCameraUpdate );

                                    polylineOptions = new PolylineOptions();
                                    polylineOptions.color( Color.GRAY );
                                    polylineOptions.width( 10);
                                    polylineOptions.startCap( new SquareCap() );
                                    polylineOptions.endCap( new SquareCap() );
                                    polylineOptions.jointType( JointType.ROUND );
                                    polylineOptions.addAll( polyLineList );
                                    greyPolyline = mMap.addPolyline( polylineOptions );

                                    blackpolylineOptions = new PolylineOptions();
                                    blackpolylineOptions.color( Color.BLACK );
                                    blackpolylineOptions.width( 5 );
                                    blackpolylineOptions.startCap( new SquareCap() );
                                    blackpolylineOptions.endCap( new SquareCap() );
                                    blackpolylineOptions.jointType( JointType.ROUND );
                                    blackPolyline = mMap.addPolyline( blackpolylineOptions );


                                    mMap.addMarker( new MarkerOptions()
                                            .position( polyLineList.get( polyLineList.size()-1 ) )
                                            .title( "Pickup Location" ));

                                    //Animation


                                    ValueAnimator polyLineAnimator = ValueAnimator.ofInt( 0,100 );
                                    polyLineAnimator.setDuration( 2000 );
                                    polyLineAnimator.setInterpolator( new LinearInterpolator(  ) );
                                    polyLineAnimator.addUpdateListener( new ValueAnimator.AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                            List<LatLng> points = greyPolyline.getPoints();
                                            int percentValue = (int) valueAnimator.getAnimatedValue();
                                            int size = points.size();
                                            int newPoints = (int)(size * (percentValue/100.0f));
                                            List <LatLng> p = points.subList( 0,newPoints );
                                            blackPolyline.setPoints(p);
                                        }
                                    });
                                    polyLineAnimator.start();

                                    carMarker = mMap.addMarker(new MarkerOptions().position( currentPosition )
                                            .flat( true )
                                            .icon( BitmapDescriptorFactory.fromResource( R.drawable.mylocation ))) ;


                                    handler = new Handler(  );
                                    index=-1;
                                    next=1;
                                    handler.postDelayed(drawPathRunnable,3000 );

                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call <String> call, Throwable t) {
                            Toasty.error( DriverHome.this, ""+t.getMessage(), Toast.LENGTH_SHORT, true).show();

                        }
                    } );
        }catch (Exception e )
        {
            e.printStackTrace();

        }

    }

    private List decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {              case MY_PERMISSION_REQUEST_CODE:
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                if (checkPlayServices()) {
                    buildGoogleApiClient();
                    createLocationRequest();
                    if (location_switch.isChecked())
                        displayLocation();


                }

            }
            break;

        }
    }

    private void setUpLoaction() {

        if (ActivityCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
            //Request Runtime permission
            ActivityCompat.requestPermissions( this, new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            }, MY_PERMISSION_REQUEST_CODE );
        } else {
            if (checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
                if (location_switch.isChecked())
                    displayLocation();


            }
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
                Toasty.error(DriverHome.this, "This device is not supported ", Toast.LENGTH_SHORT, true).show();
                finish();
            }
            return false;
        }
        return true;

    }

    private void stopLocationUpdate() {

        if(ActivityCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION  )!= PackageManager.PERMISSION_GRANTED&&
                ActivityCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION   )!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates( mGoogleApiClient,  this );

    }

    private void startLocationUpdate() {

        if(ActivityCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION  )!= PackageManager.PERMISSION_GRANTED&&
                ActivityCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION   )!= PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates( mGoogleApiClient,mLocationRequest,this );


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
            if(location_switch.isChecked())
            {
                final double latitude = Common.mLastLocation.getLatitude();
                final double longitude = Common.mLastLocation.getLongitude();

                LatLng center = new LatLng( latitude,longitude );
                LatLng northSide = SphericalUtil.computeOffset( center,10000,0 );
                LatLng southSide = SphericalUtil.computeOffset( center,10000,180 );

                LatLngBounds bounds = LatLngBounds.builder()
                        .include( northSide )
                        .include( southSide )
                        .build();

                places.setBoundsBias( bounds );
                places.setFilter( typeFilter );

                //update to firebase

                geoFire.setLocation( FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation( latitude, longitude ), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        // Add Marker

                        if(mcurrent != null)
                            mcurrent.remove();  // remove already marker

                        mcurrent = mMap.addMarker( new MarkerOptions()
                                .position( new LatLng( latitude,longitude ) )
                                .title( String.format( "Your Location" ) )
                        );

                        // Move Camera to this positon
                        mMap.animateCamera( CameraUpdateFactory.newLatLngZoom( new LatLng( latitude,longitude ) ,15.5f) );
                        // Draw animation to rotate marker

                        rotateMarker(mcurrent,360,mMap);

                    }
                } );
            }
        }
        else {
            Log.d("Error","Cannot get your Location");
        }
    }

    private void rotateMarker(final Marker mcurrent, final float i, GoogleMap mMap) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotation = mcurrent.getRotation();
        final long duration = 1500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post( new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation( (float) elapsed / duration );
                float rot = t * i + (1 - t) * startRotation;
                mcurrent.setRotation( -rot > 180 ? rot / 2 : rot );

                if (t < 1.0) {
                    handler.postDelayed( this, 16 );
                }
            }
        } );

    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById( R.id.drawer_layout );
        if (drawer.isDrawerOpen( GravityCompat.START )) {
            drawer.closeDrawer( GravityCompat.START );
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate( R.menu.driver_home, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement


        return super.onOptionsItemSelected( item );
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_trip_history) {
            // Handle the camera action
        } else if (id == R.id.nav_way_bill) {

        } else if (id == R.id.nav_help) {

        }  else if (id == R.id.nav_settings) {

        }
        else if (id == R.id.nav_change_pwd) {
            showDialogChangePwd();

        }
        else if (id == R.id.nav_update_info) {
           showDialogUpdateInfo();

        }
        else if (id == R.id.nav_signOut) {
            signOut();
            }

        DrawerLayout drawer = (DrawerLayout) findViewById( R.id.drawer_layout );
        drawer.closeDrawer( GravityCompat.START );
        return true;
    }


    private void showDialogUpdateInfo() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder( DriverHome.this );
        alertDialog.setTitle( "UPDATE INFORMATION " );
        alertDialog.setMessage( "Please Enter  Information to Update" );
        LayoutInflater inflater = this.getLayoutInflater();
        View layout_pwd = inflater.inflate( R.layout.layout_update_information,null );


        final MaterialEditText edtName = (MaterialEditText) layout_pwd.findViewById( R.id.edt_Name );
        final MaterialEditText edtPhone = (MaterialEditText) layout_pwd.findViewById( R.id.edt_phone );
        final ImageView image_upload = (ImageView) layout_pwd.findViewById( R.id.image_upload );

     image_upload.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChooseImage();
            }
        } );


        alertDialog.setView(layout_pwd);

        alertDialog.setPositiveButton( "UPDATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                final AlertDialog waitingDialog = new SpotsDialog( DriverHome.this );
                waitingDialog.show();

                String name = edtName.getText().toString();
                String phone = edtPhone.getText().toString();

                Map<String,Object> updateInfo = new HashMap <>(  );
                if (!TextUtils.isEmpty( name ))
                    updateInfo.put( "name",name );
                if (!TextUtils.isEmpty( phone ))
                    updateInfo.put( "phone",phone );

                DatabaseReference driverInforamtion = FirebaseDatabase.getInstance().getReference(Common.user_driver_tb1);
                driverInforamtion.child( FirebaseAuth.getInstance().getCurrentUser().getUid() )
                        .updateChildren( updateInfo )
                        .addOnCompleteListener( new OnCompleteListener <Void>() {
                            @Override
                            public void onComplete(@NonNull Task <Void> task) {
                                if (task.isSuccessful())
                                    Toasty.success( DriverHome.this,"Information Updated ",Toast.LENGTH_LONG,true ).show();
                                else
                                    Toasty.success( DriverHome.this,"Information Update Failed !",Toast.LENGTH_LONG,true ).show();
                                    waitingDialog.dismiss();

                            }
                        } );
            }
        } );

        alertDialog.setNegativeButton( "CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        } );

        alertDialog.show();

    }

    private void ChooseImage() {
        Intent intent = new Intent(  );
       intent.setType( "image/*" );
        intent.setAction( Intent.ACTION_GET_CONTENT);
        startActivityForResult( Intent.createChooser( intent,"Select Picture" ),Common.PICK_IMAGE_REQUEST );

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult( requestCode, resultCode, data );
        if(requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData()!= null)
        {
           final Uri saveUri = data.getData();

            if (saveUri != null)
            {
                final ProgressDialog mDialog = new ProgressDialog( this );
                mDialog.setMessage("Uploading...");
                mDialog.show();
                String imageName = UUID.randomUUID().toString();
                final StorageReference imageFolder =  storageReference.child( "images/"+imageName );

                imageFolder.putFile( saveUri )
                        .addOnSuccessListener( new OnSuccessListener <UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                               mDialog.dismiss();

                               imageFolder.getDownloadUrl().addOnSuccessListener( new OnSuccessListener <Uri>() {
                                   @Override
                                   public void onSuccess(Uri uri) {
                                       Map<String,Object> avatarUpdate = new HashMap <>(  );

                                       avatarUpdate.put( "avatarUrl",uri.toString() );

                                       DatabaseReference driverInformations = FirebaseDatabase.getInstance().getReference(Common.user_driver_tb1);
                                       driverInformations.child( FirebaseAuth.getInstance().getCurrentUser().getUid() )
                                               .updateChildren( avatarUpdate )
                                               .addOnCompleteListener( new OnCompleteListener <Void>() {
                                                   @Override
                                                   public void onComplete(@NonNull Task <Void> task) {
                                                       if (task.isSuccessful()) {
                                                           imageAvatar.setImageURI( saveUri );
                                                           Toasty.success( DriverHome.this, "Uploaded SucessFully !", Toast.LENGTH_LONG, true ).show();
                                                       }
                                                   else
                                                           Toasty.error( DriverHome.this,"Uploaded Error !",Toast.LENGTH_LONG,true).show();
                                                   }
                                               } );

                                   }
                               } );
                            }
                        } )
                        .addOnProgressListener( new OnProgressListener <UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());

                                mDialog.setMessage( "Uploaded " +progress +" %" );


                            }
                        } );

            }

        }
    }

    private void showDialogChangePwd(){


        AlertDialog.Builder alertDialog = new AlertDialog.Builder( DriverHome.this );
        alertDialog.setTitle( "CHANGE PASSWORD " );
        alertDialog.setMessage( "Please Enter Your Information" );
        LayoutInflater inflater = this.getLayoutInflater();
        View layout_pwd = inflater.inflate( R.layout.layout_change_pwd,null );


        final MaterialEditText password = (MaterialEditText) layout_pwd.findViewById( R.id.edt_password );
        final MaterialEditText NewPassword = (MaterialEditText) layout_pwd.findViewById( R.id.edt_Newpassword );
        final MaterialEditText RepeatNewPassword = (MaterialEditText) layout_pwd.findViewById( R.id.edtRepeatNewpassword );


        alertDialog.setView(layout_pwd);
        alertDialog.setPositiveButton( "CHANGE PASSWORD", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
              final android.app.AlertDialog waitingDialog = new SpotsDialog( DriverHome.this );
              waitingDialog.show();



              if (NewPassword.getText().toString().equals(RepeatNewPassword.getText().toString() ))
              {
                  String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                  AuthCredential credential = EmailAuthProvider.getCredential( email,password.getText().toString() );
                  FirebaseAuth.getInstance().getCurrentUser()
                          .reauthenticate( credential )
                          .addOnCompleteListener( new OnCompleteListener <Void>() {
                              @Override
                              public void onComplete(@NonNull Task<Void> task) {
                                  if (task.isSuccessful()) {

                                      FirebaseAuth.getInstance().getCurrentUser()
                                              .updatePassword( RepeatNewPassword.getText().toString().trim() )
                                              .addOnCompleteListener( new OnCompleteListener <Void>() {
                                                  @Override
                                                  public void onComplete(@NonNull Task <Void> task) {
                                                  if (task.isSuccessful())
                                                  {
                                                      //update driver information password coloum

                                                      Map<String,Object> password = new HashMap <>(  );
                                                      password.put( "password",RepeatNewPassword.getText().toString() );

                                                      DatabaseReference driverInformation = FirebaseDatabase.getInstance().getReference(Common.user_driver_tb1);
                                                      driverInformation.child( FirebaseAuth.getInstance().getCurrentUser().getUid() )
                                                              .updateChildren( password )
                                                              .addOnCompleteListener( new OnCompleteListener <Void>() {
                                                                  @Override
                                                                  public void onComplete(@NonNull Task <Void> task) {
                                                                    if (task.isSuccessful())
                                                                    {waitingDialog.dismiss();
                                                                        Toasty.success( DriverHome.this,"Password Change Sucessfully ! ",Toast.LENGTH_LONG,true ).show();

                                                                    }
                                                                    else{
                                                                        waitingDialog.dismiss();
                                                                        Toasty.error( DriverHome.this,"password was change but not Update to Driver Information ",Toast.LENGTH_LONG,true ).show();

                                                                    }
                                                                  }
                                                              } );

                                                  }
                                                  else
                                                      {
                                                          waitingDialog.dismiss();
                                                          Toasty.error( DriverHome.this,"Password Doesn't change ",Toast.LENGTH_LONG,true ).show();

                                                      }
                                                  }
                                              } );

                                  }
                                  else
                                      {
                                          Toasty.error( DriverHome.this,"Wrong Password ",Toast.LENGTH_LONG,true ).show();
                                          waitingDialog.dismiss();
                                      }
                              }
                          } );

              }
              else
              {
                  waitingDialog.dismiss();
                  Toasty.error( DriverHome.this,"Your Password Does't match",Toast.LENGTH_LONG,true ).show();

              }

            }
        } );
        alertDialog.setNegativeButton( "CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        } );
        alertDialog.show();


    }


    private void signOut() {

        // Reset Remeber VALue
        Paper.init( this );
        Paper.book().destroy();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent( DriverHome.this,MainActivity.class );
        startActivity( intent );
        finish();
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

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMapType( GoogleMap.MAP_TYPE_NORMAL );
        mMap.setTrafficEnabled( false );
        mMap.setIndoorEnabled( false );
        mMap.setBuildingsEnabled( false );
        mMap.getUiSettings().setZoomControlsEnabled( true );
        mMap.getUiSettings().setMyLocationButtonEnabled( true );
    }
}
