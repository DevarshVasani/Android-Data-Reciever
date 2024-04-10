package com.example.sms;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ActivityResultLauncher<String[]> permissionResultLanucher;
    private boolean isReadSms=false;
    private boolean isReceiveSms=false;
    private boolean isForeGroundService=false;
    private boolean isNotification=false;
    private boolean isReadPhone=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionResultLanucher=registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {

                if(result.get(Manifest.permission.READ_SMS)!=null){

                    isReadSms= Boolean.TRUE.equals(result.get(Manifest.permission.READ_SMS));
                }

                if(result.get(Manifest.permission.RECEIVE_SMS)!=null){

                    isReceiveSms= Boolean.TRUE.equals(result.get(Manifest.permission.RECEIVE_SMS));
                }
                if(result.get(Manifest.permission.FOREGROUND_SERVICE)!=null){

                    isForeGroundService= Boolean.TRUE.equals(result.get(Manifest.permission.FOREGROUND_SERVICE));
                }

                if(result.get(Manifest.permission.POST_NOTIFICATIONS)!=null){

                    isNotification= Boolean.TRUE.equals(result.get(Manifest.permission.POST_NOTIFICATIONS));
                }

                if(result.get(Manifest.permission.READ_PHONE_STATE)!=null){

                    isReadPhone= Boolean.TRUE.equals(result.get(Manifest.permission.READ_PHONE_STATE));
                }


              if(isReadSms && isReceiveSms && isForeGroundService && isNotification && isReadPhone){
                  firstTime();
              }

            }
        }

        );

        checkPermission();

        startService(new Intent(this, BackgroundRun.class));
    }

    private void firstTime(){

        Log.d("helo", "service started: ");
        SharedPreferences sharedPreferences=getPreferences(Context.MODE_PRIVATE);
        boolean isFirstTime=sharedPreferences.getBoolean("isFirstTime",true);

        if (isFirstTime) {
            // If it's the first time, prompt the user for their custom path
            showCustomPathDialog();

            // Mark that the app has been opened
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isFirstTime", false);
            editor.apply();
        }
        else {

          //smsSync();

        }



    }

    public void smsSync(){
        SmsJob s1=new SmsJob();
        s1.compareStoredSms(getApplicationContext());
        Log.d("method called", "onCreate: ");
    }

    private void showCustomPathDialog() {
        // Display a dialog or activity to get the user's custom path
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Your Custom Path");

        // Add an EditText for user input
        final EditText input = new EditText(this);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            String customPath = input.getText().toString().trim();
            Log.d("CustomPath", "User Input: " + customPath);
            if (customPath.isEmpty()) {
                // Save the custom path for future use
                showCustomPathDialog();
            } else {
               saveCustomPath(customPath);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // Handle cancel button click
        });

        // Show the dialog

        builder.show();
    }
    private void saveCustomPath(String customPath) {
        // Store the custom path in SharedPreferences or any other persistent storage
        SharedPreferences sharedPreferences = getSharedPreferences("com.example.sms", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("customPath", customPath);
        editor.apply();


        // Now you can use customPath when saving SMS messages to Firebase
    }





        public void checkPermission(){

        isReadSms= ContextCompat.checkSelfPermission(this,Manifest.permission.READ_SMS)==PackageManager.PERMISSION_GRANTED;

        isReceiveSms= ContextCompat.checkSelfPermission(this,Manifest.permission.RECEIVE_SMS)==PackageManager.PERMISSION_GRANTED;

        isForeGroundService= ContextCompat.checkSelfPermission(this,Manifest.permission.FOREGROUND_SERVICE)==PackageManager.PERMISSION_GRANTED;

        isNotification= ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;

        isReadPhone= ContextCompat.checkSelfPermission(this,Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED;


            List<String> permission=new ArrayList<String>();
            if(!isReadSms){
                permission.add(Manifest.permission.READ_SMS);
            }

            if(!isReceiveSms){
                permission.add(Manifest.permission.RECEIVE_SMS);
            }

            if(!isForeGroundService){
                permission.add(Manifest.permission.FOREGROUND_SERVICE);
            }

            if(!isReadPhone){
                permission.add(Manifest.permission.READ_PHONE_STATE);
            }

            if(!isNotification){
                permission.add(Manifest.permission.POST_NOTIFICATIONS);
            }

            if(!permission.isEmpty()){

                permissionResultLanucher.launch(permission.toArray(new String[0]));

            }
            else{
                smsSync();
            }



        }


    public void onRequestPermissionsResult(int requestcode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestcode,permissions,grantResults);
        if(requestcode==1&&grantResults[0]==PackageManager.PERMISSION_GRANTED){

        }
    }


}