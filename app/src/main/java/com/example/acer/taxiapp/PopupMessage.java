package com.example.acer.taxiapp;

public class PopupMessage {

    private byte messageSource;
    private String textMessage;
    private String timestamp;

    public PopupMessage(byte messageSource, String textMessage, String timestamp) {
        this.messageSource = messageSource;
        this.textMessage = textMessage;
        this.timestamp = timestamp;
    }

    public byte getMessageSource() {
        return messageSource;
    }

    public String getTextMessage() {
        return textMessage;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
