package com.example.lenovossd.driverapp;

import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.example.lenovossd.driverapp.Common.Common;
import com.example.lenovossd.driverapp.Model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import es.dmoral.toasty.Toasty;
import io.paperdb.Paper;

public class SplashScreen extends AppCompatActivity {

    private  static int SPLASH_TIME_OUT = 4000;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_splash_screen );

        Paper.init( this );
        final String user = Paper.book().read( Common.user_field );
        final String pwd = Paper.book().read( Common.pwd_field );

        auth = FirebaseAuth.getInstance();
        new Handler(  ).postDelayed( new Runnable() {
            @Override
            public void run() {
                if (user != null && pwd != null)
                {
                    if (!TextUtils.isEmpty(user )
                            && !TextUtils.isEmpty(pwd ))
                    {
                        autoLogin(user,pwd);
                    }
                }
                else
                {
                    Intent homeIntent = new Intent( SplashScreen.this,MainActivity.class );
                    startActivity( homeIntent );
                    finish();
                }

            }
        },SPLASH_TIME_OUT );

    }
    private void autoLogin(String user, String pwd) {


        //Login

        auth.signInWithEmailAndPassword(user, pwd )
                .addOnSuccessListener( new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {


                        FirebaseDatabase.getInstance().getReference( Common.user_driver_tb1 )
                                .child( FirebaseAuth.getInstance().getCurrentUser().getUid() )
                                .addListenerForSingleValueEvent( new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        Common.currentUser = dataSnapshot.getValue( User.class );

                                        Intent intent = new Intent( SplashScreen.this, DriverHome.class );
                                        Toasty.success( SplashScreen.this, "Login SucessFully", Toast.LENGTH_SHORT, true ).show();
                                        startActivity( intent );

                                        finish();
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                } );


                    }
                } )
                .addOnFailureListener( new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toasty.error( SplashScreen.this, "Failed !!" + e.getMessage(), Toast.LENGTH_LONG, true ).show();

                    }
                } );
    }
}
