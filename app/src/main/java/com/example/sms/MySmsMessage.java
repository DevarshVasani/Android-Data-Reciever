package com.example.sms;

import com.google.firebase.database.Exclude;

public class MySmsMessage {

    private String sender;
    private String messageBody;
    private String timestamp;
    public MySmsMessage() {
        // Default constructor required for Firebase
    }
    public MySmsMessage(String sender, String messageBody, String timestamp) {

        this.sender = sender;
        this.messageBody = messageBody;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public String getTimestamp() {
        return timestamp;
    }

}
