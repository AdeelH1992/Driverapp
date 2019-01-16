package com.example.lenovossd.driverapp;

import android.app.AlertDialog;
import android.content.DialogInterface;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lenovossd.driverapp.Common.Common;
import com.example.lenovossd.driverapp.Model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import dmax.dialog.SpotsDialog;
import es.dmoral.toasty.Toasty;
import io.paperdb.Paper;

public class MainActivity extends AppCompatActivity {

    Button btnSignIn, btnRegister;

    FirebaseAuth auth;

    FirebaseDatabase db;

    DatabaseReference users;

    RelativeLayout rootlayout;

     AlertDialog waitingDialog;

     TextView txt_forgot_pwd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );


        //  init PaperDb

        Paper.init( this );
        auth = FirebaseAuth.getInstance();

        db = FirebaseDatabase.getInstance();

        users = db.getReference( Common.user_driver_tb1);


        btnSignIn = (Button) findViewById( R.id.btn_sign_in );
        btnRegister = (Button) findViewById( R.id.btn_Register );
        rootlayout = (RelativeLayout) findViewById( R.id.root_layout );
        txt_forgot_pwd = (TextView) findViewById( R.id.txt_forgot_password );
        waitingDialog = new SpotsDialog( MainActivity.this );
        txt_forgot_pwd.setOnTouchListener( new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                showDialogForgotpwd();
                return false;
            }
        } );

        btnRegister.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 showRegisterDialog();
            }
        } );

        btnSignIn.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginDialog();
            }
        } );

        // Auto Login System


    }

   
    private void showDialogForgotpwd() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder( this );
        alertDialog.setTitle( "FORGOT PASSWORD" );
        alertDialog.setMessage( "Please enter your email Address" );

        LayoutInflater inflater = LayoutInflater.from( MainActivity.this );
        View forgot_psw_layout = inflater.inflate( R.layout.forgot_pwd,null );
        final MaterialEditText edtEmail = (MaterialEditText)forgot_psw_layout.findViewById( R.id.edt_email );
        alertDialog.setView( forgot_psw_layout );
        //set button

        alertDialog.setPositiveButton( "RESET", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int i) {
                waitingDialog.show();

                auth.sendPasswordResetEmail( edtEmail.getText().toString().trim() )
                        .addOnCompleteListener( new OnCompleteListener <Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialogInterface.dismiss();
                                waitingDialog.dismiss();
                                Toasty.success( MainActivity.this,"Reset password link has been sent",Toast.LENGTH_LONG,true ).show();
                            }
                        } ).addOnFailureListener( new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialogInterface.dismiss();
                        waitingDialog.dismiss();
                        Toasty.error( MainActivity.this," "+e.getMessage(),Toast.LENGTH_LONG,true ).show();

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

    private void showLoginDialog() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this  );
        dialog.setTitle( "SIGN IN" );
        dialog.setMessage("Please Use Email To sign in");

        LayoutInflater inflater= LayoutInflater.from(this);

        View login_layout=inflater.inflate(R.layout.layout_signin,null);

        final MaterialEditText edtEmail = login_layout.findViewById( R.id.edt_email );
        final MaterialEditText edtpassword = login_layout.findViewById( R.id.edt_password );


        dialog.setView( login_layout );

        dialog.setPositiveButton( "SIGN IN", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                btnSignIn.setEnabled( false );
                if (TextUtils.isEmpty( edtEmail.getText().toString() ))
                {
                    Toasty.error( MainActivity.this, "Please Enter Your Email address", Toast.LENGTH_LONG, true ).show();
                    btnSignIn.setEnabled( true );
                    return;
                }
                if (TextUtils.isEmpty( edtpassword.getText().toString() ))
                {
                    Toasty.error( MainActivity.this, "Please Enter Your Password", Toast.LENGTH_LONG, true ).show();
                    btnSignIn.setEnabled( true );
                    return;
                }

                if (edtpassword.getText().toString().length() < 6)
                {
                    Toasty.error( MainActivity.this, "Your Password is too Short", Toast.LENGTH_LONG, true ).show();
                    btnSignIn.setEnabled( true );
                    return;
                }
                if((!Patterns.EMAIL_ADDRESS.matcher(edtEmail.getText().toString()).matches()))
                {                       Toasty.error( MainActivity.this, "Please enter a valid Email (youremail@gmail.com)", Toast.LENGTH_LONG, true ).show();
                    btnSignIn.setEnabled( true );
                    return;
                }
                //Login


                     waitingDialog.show();


                auth.signInWithEmailAndPassword( edtEmail.getText().toString() ,edtpassword.getText().toString() )
                        .addOnSuccessListener( new OnSuccessListener <AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                waitingDialog.dismiss();

                                FirebaseDatabase.getInstance().getReference(Common.user_driver_tb1)
                                        .child( FirebaseAuth.getInstance().getCurrentUser().getUid() )
                                        .addListenerForSingleValueEvent( new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {

                                                Log.e( "datasnpshot",dataSnapshot.toString() );
                                                Paper.book().write( Common.user_field,edtEmail.getText().toString() );
                                                Paper.book().write( Common.pwd_field,edtpassword.getText().toString() );
                                                Intent intent = new Intent( MainActivity.this,DriverHome.class );
                                                Toasty.success(MainActivity.this, "Login SucessFully", Toast.LENGTH_SHORT, true).show();
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
                                waitingDialog.dismiss();
                                Toasty.error( MainActivity.this, "Failed !!" + e.getMessage(), Toast.LENGTH_LONG, true ).show();
                        btnSignIn.setEnabled( true );
                            }
                        } );



            }
        });

        dialog.setNegativeButton( "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
            }
        } );





        dialog.show();

    }

    private void showRegisterDialog() {

        final AlertDialog.Builder dialog = new AlertDialog.Builder(this  );
        dialog.setTitle( "REGISTER" );
        dialog.setMessage("Please Use Email To Register");

        LayoutInflater inflater= LayoutInflater.from(this);

        View register_layout=inflater.inflate(R.layout.layout_register,null);

        final MaterialEditText edtEmail = register_layout.findViewById( R.id.edt_email );
        final MaterialEditText edtpassword = register_layout.findViewById( R.id.edt_password );
        final MaterialEditText edtName = register_layout.findViewById( R.id.edt_name );
        final MaterialEditText edtPhone = register_layout.findViewById( R.id.edt_phone );



        dialog.setView( register_layout );

        dialog.setPositiveButton( "Register", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                if(TextUtils.isEmpty( edtEmail.getText().toString() ))
                {
                    Toasty.error(MainActivity.this, "Please Enter Your Email address", Toast.LENGTH_LONG, true).show();

                    return; }
                if(TextUtils.isEmpty( edtpassword.getText().toString() ))
                {
                    Toasty.error(MainActivity.this, "Please Enter Your Password", Toast.LENGTH_LONG, true).show();
                    return;
                }
                if(TextUtils.isEmpty( edtName.getText().toString() ))
                {
                    Toasty.error(MainActivity.this, "Please Enter Your Name", Toast.LENGTH_LONG, true).show();
                    return;
                }
                if(TextUtils.isEmpty( edtPhone.getText().toString() ))
                {
                    Toasty.error(MainActivity.this, "Please Enter Your Phone Number", Toast.LENGTH_LONG, true).show();
                    return;
                }
                if((!Patterns.EMAIL_ADDRESS.matcher(edtEmail.getText().toString()).matches()))
                {                       Toasty.error( MainActivity.this, "Please enter a valid Email (youremail@gmail.com)", Toast.LENGTH_LONG, true ).show();

                    return;
                }

                if(edtpassword.getText().toString().length()<6)
                {
                    Toasty.error(MainActivity.this, "Your Password is too Short", Toast.LENGTH_LONG, true).show();
                    return;

                }

                // Register User

                waitingDialog.show();
                auth.createUserWithEmailAndPassword( edtEmail.getText().toString(),edtpassword.getText().toString() )
                        .addOnSuccessListener( new OnSuccessListener <AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                              // save user to db

                                User user = new User(  );

                                user.setEmail( edtEmail.getText().toString());
                                user.setName(edtName.getText().toString());
                                user.setPassword( edtpassword.getText().toString() );
                                user.setPhone( (edtPhone.getText().toString()) );


                                users.child( FirebaseAuth.getInstance( ).getCurrentUser().getUid())
                                        .setValue( user )
                                        .addOnSuccessListener( new OnSuccessListener <Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                            waitingDialog.dismiss();
                                                Toasty.success(MainActivity.this, "Register SucessFully", Toast.LENGTH_SHORT, true).show();

                                            }
                                        } )
                                        .addOnFailureListener( new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                waitingDialog.dismiss();
                                                Toasty.error(MainActivity.this, "Failed ! "+ e.getMessage(), Toast.LENGTH_LONG, true).show();

                                            }
                                        } );


                            }
                        } )

                .addOnFailureListener( new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        waitingDialog.dismiss();
                        Toasty.error(MainActivity.this, "Failed ! "+ e.getMessage(), Toast.LENGTH_LONG, true).show();

                    }
                } );

            }
        } );

        dialog.setNegativeButton( "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        } );

        dialog.show();


    }


}
