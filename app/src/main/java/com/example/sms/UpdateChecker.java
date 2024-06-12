package com.example.sms;

import android.app.DownloadManager;
import android.content.Context;
import android.content.BroadcastReceiver;

import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;
import android.security.NetworkSecurityPolicy;



public class UpdateChecker {

    private static final String TAG = "UpdateChecker";

    public static String UPDATE_URL;


    public static void data_firebase(Context context){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("database");
//
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UPDATE_URL = snapshot.child("url").getValue().toString();
                Log.d(TAG, "onDataChange: " + UPDATE_URL);
                UPDATE_URL = UPDATE_URL.replace("\"", "");

                checkForUpdate(context, UPDATE_URL);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }




    public static void checkForUpdate(Context context, String UPDATE_URL) {
//        CountDownLatch latch = new CountDownLatch(1); // Create a CountDownLatch with a count of 1
//
//
//        try {
//            latch.await(); // Current thread waits here until latch count reaches zero
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//            return;
//        }
//
//        // Rest of your code...
//
//

        new Thread(() -> {


            try {

                Log.d(TAG, "checkForUpdate: from outside " + UPDATE_URL);

                URL url = new URL("http://192.168.190.250:8000/update.json");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() == 200) {
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    int latestVersionCode = jsonResponse.getInt("versionCode");
                    String apkUrl = jsonResponse.getString("apkUrl");

                    int currentVersionCode = context.getPackageManager()
                            .getPackageInfo(context.getPackageName(), 0).versionCode;

                    if (latestVersionCode > currentVersionCode) {
                        downloadAndInstallUpdate(context, apkUrl);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking for update", e);
            }
        }).start();
    }


    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private static void downloadAndInstallUpdate(Context context, String apkUrl) {
        try {

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle("Downloading Update");
            request.setDescription("Downloading update for the application.");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update.apk");

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = downloadManager.enqueue(request);

            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (id == downloadId) {
                        int status = getDownloadStatus(context, downloadId);
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            Log.e(TAG, "onReceive: ");
                            Intent installIntent = new Intent(Intent.ACTION_VIEW);
                            installIntent.setDataAndType(downloadManager.getUriForDownloadedFile(downloadId),
                                    "application/vnd.android.package-archive");
                            installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            context.startActivity(installIntent);
                        } else {
                            Log.e(TAG, "Download failed with status: " + status);
                            int reason = getDownloadReason(context, downloadId);
                            if (reason == DownloadManager.ERROR_CANNOT_RESUME) {
                                // Retry the download
                                downloadAndInstallUpdate(context, apkUrl);
                            } else {
                                Log.e(TAG, "Download failed with reason: " + reason);
                            }                        }
                        context.unregisterReceiver(this);
                    }
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
            }
            return;
        } catch (Exception e) {
            Log.e(TAG, "Error downloading update", e);
            return;
        }
    }

    private static int getDownloadStatus(Context context, long downloadId) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting download status", e);
        }
        return -1; // Unknown status
    }

    private static int getDownloadReason(Context context, long downloadId) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting download reason"+ e.getMessage());
        }
        return -1; // Unknown reason
    }

}
