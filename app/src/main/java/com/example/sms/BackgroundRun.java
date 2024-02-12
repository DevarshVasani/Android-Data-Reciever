package com.example.sms;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class BackgroundRun extends Service {
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


    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Handler().postDelayed(() -> {
            updateStatusInFirebase(this);
            startService(new Intent(this, BackgroundRun.class)); // Restart the service to repeat
        }, 15 * 1000); // Delay in milliseconds (15 seconds)





        return START_STICKY;
    }

    private void updateStatusInFirebase(Context context) {
        boolean isAppForeground = isAppForeground(context); // Using the Foreground library for convenience

        String custompath=getCustomPathFromPreferences(context);
        String path = "user_messages/" + custompath;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = networkInfo != null && networkInfo.isConnected();

        String status = isAppForeground && isConnected ? "active" : "inactive";

        // Access your Firebase database reference
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);

        // Update the status based on the combined state
        databaseReference.child(custompath).setValue(status);
    }

    boolean isAppForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            if (processInfo.processName.equals(context.getPackageName()) && processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

    private String getCustomPathFromPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.sms", Context.MODE_PRIVATE);
        String customPath = sharedPreferences.getString("customPath", "");
        return sharedPreferences.getString("customPath", "");
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
