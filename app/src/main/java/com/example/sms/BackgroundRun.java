package com.example.sms;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class BackgroundRun extends Service implements LifecycleObserver {

    private static final int INTERVAL = 1 * 60 * 1000;
    private  boolean isAppForeground = false;
    SmsJob time=new SmsJob();

    public String signal;
    public String network="null";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    public void onCreate()
    {
        super.onCreate();
        createNotificationChannel();
        startForeground(1,createNotification());
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null){
            String action=intent.getAction();
            if("UPDATE_TIME".equals(action)){
                Log.d("INTENT", "Executed Becuase of intent: ");
                checksignal();
                updateStatusInFirebase(this);
            }
        }
        new Handler().postDelayed(() -> {
            checksignal();
            updateStatusInFirebase(this);

            startService(new Intent(this, BackgroundRun.class)); // Restart the service to repeat
        }, INTERVAL); // Delay in milliseconds (15 seconds)
        return START_STICKY;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onEnterForeground() {
        isAppForeground = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onEnterBackground() {
        isAppForeground = true;
    }

    @SuppressLint("MissingPermission")
    //using suppresssline because permission will be asked at the time of running app for first time.
    private boolean isNetworkAvailable() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = telephonyManager.getNetworkType();
        boolean isSignalAvailable = networkType != TelephonyManager.NETWORK_TYPE_UNKNOWN;

        Log.d("NetworkUtils", "Network Type: " + networkType);

        return isSignalAvailable;
    }



    public void setnetworkstatus(String status){
          signal=status;
    }

    public String getnetworkstatus(){
        return signal;
    }


    public void checksignal(){
        if (isNetworkAvailable()) {
            network="SIGNAL AVAILABLE";
        } else {
            network="NO SIGNAL";
        }
        setnetworkstatus(network);
    }


    public  void updateStatusInFirebase(Context context) {
        Sms username=new Sms();
        String custompath=username.getCustomPathFromPreferences(context);
        String path = "user_messages/" + custompath;
        String statustime="";
        long timestamp=0;


        Intent batteryintent=context.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int batterylevel=batteryintent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
        int batteryscale=batteryintent.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
        float batterypercentage=batterylevel*100/(float)batteryscale;



        String status = isAppForeground  ? "active" : "inactive";

        if(time.getTime()==0){
            Log.d("CHECK", "smstime: "+time.getTime());
            timestamp=System.currentTimeMillis();
            statustime=time.getFormattedTime(timestamp);
            Log.d("statustime", "NormalTime: "+statustime);

        }
        else {

            timestamp= time.getTime();
            Log.d("Aftersms", "aftersms:" +timestamp);
            statustime=time.getFormattedTime(timestamp);
            Log.d("aftersms", ": "+statustime);
        }

        String devicenetwork=getnetworkstatus();
        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("status", status);
        statusMap.put("timestamp", statustime);
        statusMap.put("battery",batterypercentage);
        statusMap.put("signal",devicenetwork);
        // Access your Firebase database reference
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);

        // Update the status based on the combined state
        databaseReference.child(custompath).updateChildren(statusMap);
    }






    private void createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "my_channel",
                    "My Background Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, "my_channel")
                .setContentTitle("SMS")
                .setContentText("SMS Sent To Firebase")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build();
    }

}
