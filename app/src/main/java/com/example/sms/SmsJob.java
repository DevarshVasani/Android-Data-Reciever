package com.example.sms;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

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

            saveSmsToFirebase(custompath,sender,message,timestamp);
            saveSmsToLocalStorage(getApplicationContext(), sender, message, timestamp);
            setTime(timestamp);

        }


        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


    private void saveSmsToLocalStorage(Context context, String sender, String messageBody, long timestampMillis) {
        // Use a local database, shared preferences, or another storage mechanism to save the SMS
        // This example uses SharedPreferences for simplicity
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

            do {
                long timestamp = cursor.getLong(timestampIndex);
                String sender = cursor.getString(senderIndex);
                String body = cursor.getString(bodyIndex);

                isNewSms(context, sender, body, timestamp);
            } while (cursor.moveToNext());

            cursor.close();
        }
    }
    private void isNewSms(Context context, String sender, String messageBody,long timestamp) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.sms", Context.MODE_PRIVATE);
        Set<String> processedTimestamps = sharedPreferences.getStringSet("smsTimestamp_", new HashSet<String>());

        long thresholdTimestamp = System.currentTimeMillis() - (1000 * 60 * 60 * 24);
        if (!processedTimestamps.contains(String.valueOf(timestamp)) && timestamp > thresholdTimestamp) {
            String customPath = getCustomPathFromPreferences(context);
            Log.d("SEND", "ISNEWSMS: " + messageBody);
            saveSmsToFirebase(customPath, sender, messageBody, timestamp);
            setTime(timestamp);

            Set<String> updatedTimestamps = new HashSet<>(processedTimestamps);
            updatedTimestamps.add(String.valueOf(timestamp));

            // Save the updated set to SharedPreferences
            sharedPreferences.edit().putStringSet("smsTimestamp_", updatedTimestamps).apply();

        } else {
            Log.d("OLDSMS", "THIS IS OLD SMS. Timestamp: " + timestamp);
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


    private void saveSmsToFirebase(String custompath,String sender, String messageBody, long timestampmills) {

        String path = "user_messages/" + custompath;

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(path);

        // Format the timestamp as a human-readable string
        String formattedTime = getFormattedTime(timestampmills);

        // Create a data object to store in the database
        MySmsMessage smsMessage = new MySmsMessage(sender, messageBody, formattedTime);

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
