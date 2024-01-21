package com.example.sms;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        startService(new Intent(this, BackgroundRun.class));

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
            if (!TextUtils.isEmpty(customPath)) {
                // Save the custom path for future use
                saveCustomPath(customPath);
            } else {
                Log.d("CustomPath", "Empty or Invalid Input");
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
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
        != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS},1);
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS},1);
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.FOREGROUND_SERVICE},1);
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS},1);
        }



    }

    public void onRequestPermissionsResult(int requestcode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestcode,permissions,grantResults);
        if(requestcode==1&&grantResults[0]==PackageManager.PERMISSION_GRANTED){

        }
    }


}