package com.example.sms;

import com.google.firebase.database.Exclude;

public class MySmsMessage {
    private String deviceId;
    private String sender;
    private String messageBody;
    private long timestamp;
    public MySmsMessage() {
        // Default constructor required for Firebase
    }
    public MySmsMessage(String deviceId,String sender, String messageBody, long timestamp) {
        this.deviceId=deviceId;
        this.sender = sender;
        this.messageBody = messageBody;
        this.timestamp = timestamp;
    }
    public String getDeviceId() {
        return deviceId;
    }
    public String getSender() {
        return sender;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public long getTimestamp() {
        return timestamp;
    }
    @Exclude
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
