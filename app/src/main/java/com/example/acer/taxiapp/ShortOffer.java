package com.example.acer.taxiapp;

public class ShortOffer {
    private long idPhoneCall;
    private byte offerSource;
    private String textMessage;


    ShortOffer(long idPhoneCall, byte offerSource, String textMessage) {
        this.idPhoneCall = idPhoneCall;
        this.offerSource = offerSource;
        this.textMessage = textMessage;
    }

    public long getIdPhoneCall() {
        return idPhoneCall;
    }

    public byte getOfferSource() {
        return offerSource;
    }

    public String getTextMessage() {
        return textMessage;
    }
}
