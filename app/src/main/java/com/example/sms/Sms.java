package com.example.sms;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Sms extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();


            int slot = bundle.getInt("slot", -1);
            int sub = bundle.getInt("subscription", -1);

            SubscriptionManager manager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            SubscriptionInfo i = manager.getActiveSubscriptionInfo(sub);//
            Log.d("Extra Information", "onReceive: " + i);
            int simSlotIndex = i.getSimSlotIndex();
            Log.d("sim slot", "onReceive: " + simSlotIndex);

            boolean network  =manager.isNetworkRoaming(sub);
            String phonenumber = manager.getPhoneNumber(sub);//if api is greater than 33
            Log.d("network", "onReceive: "+network);

            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String number = tm.getLine1Number();//if api is less than 33
            

            String final_number;//add this number to the database
            
            if (phonenumber == "" && number != ""){//
                final_number = number;
            } else if (number == "" && phonenumber != "") {
                final_number = phonenumber;
            } else if (phonenumber!= "") {
                final_number = phonenumber;

            } else {
                final_number = "Unable to find recievers's number";
            }

            Log.d("phone number", "onReceive: " + final_number);//this is the number that will be added to the database


            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    // Process each SMS message
                    for (Object pdu : pdus) {
                        SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu);

                        // Extract message details
                        String senderNumber = message.getDisplayOriginatingAddress();
                        String messageBody = message.getMessageBody();
                        long timestampMillis = System.currentTimeMillis();

                        // Log the details
                        String smsInfo = "From: " + senderNumber +
                                "\nMessage: " + messageBody + "\nReceived at: " + timestampMillis;
                        Log.d("OPPO", "onReceive: " + smsInfo);

                        String fullsms=concatenateSms(intent);

                        Bundle smsinfo=new Bundle();



                        smsinfo.putString("sendernumber",senderNumber);
                        smsinfo.putString("message",messageBody);
                        smsinfo.putLong("timestamp",timestampMillis);
                        smsinfo.putString("full",fullsms);

                        
                        ComponentName componentName=new ComponentName(context, SmsJob.class);
                        JobInfo jobInfo = null;

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            int jobid=(senderNumber+timestampMillis).hashCode();
                            jobInfo = new JobInfo.Builder(jobid,componentName)
                                    .setTransientExtras(smsinfo)
                                    .setMinimumLatency(1000)
                                    .setOverrideDeadline(10*1000)
                                    .build();
                        }

                        JobScheduler jobScheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                        jobScheduler.schedule(jobInfo);









                       // saveSmsToFirebase(custompath,senderNumber,messageBody,timestampMillis);
                       // saveSmsToLocalStorage(context, senderNumber, messageBody, timestampMillis);

                    }
                }
            }
        }
    }
    public String concatenateSms(Intent intent) {
        StringBuilder content = new StringBuilder();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null) {
                final SmsMessage[] messages = new SmsMessage[pdus.length];
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    content.append(messages[i].getMessageBody());
                }
            }
        }
        return content.toString();
    }
    public String getCustomPathFromPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("com.example.sms", Context.MODE_PRIVATE);
        String customPath = sharedPreferences.getString("customPath", "");
        return sharedPreferences.getString("customPath", "");
    }

}


