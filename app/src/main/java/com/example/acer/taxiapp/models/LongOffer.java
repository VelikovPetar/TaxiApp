package com.example.acer.taxiapp.models;

public class LongOffer {

    private long idPhoneCall;
    private float latitude;
    private float longitude;
    private byte offerSource;
    private String textMessage;

    public LongOffer(long idPhoneCall, float latitude, float longitude, byte offerSource, String textMessage) {
        this.idPhoneCall = idPhoneCall;
        this.latitude = latitude;
        this.longitude = longitude;
        this.offerSource = offerSource;
        this.textMessage = textMessage;
    }

    public long getIdPhoneCall() {
        return idPhoneCall;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public byte getOfferSource() {
        return offerSource;
    }

    public String getTextMessage() {
        return textMessage;
    }
}
