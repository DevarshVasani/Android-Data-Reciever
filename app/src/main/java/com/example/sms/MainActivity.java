package com.example.sms;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

     startService(new Intent(this, BackgroundRun.class));

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