package com.example.sms;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class BackgroundRun extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ForegroundServiceType")
    public void onCreate()
    {
        super.onCreate();
        createNotificationChannel();
        startForeground(1,createNotification());


    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle any additional actions here
        return START_STICKY;
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
                .setContentTitle("App Running yes")
                .setContentText("Performing john wick")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build();
    }
}
