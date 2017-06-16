package com.example.acer.taxiapp;

public class ShortOffer {

    private long idPhoneCall;
    private byte offerSource;
    private String textMessage;
    private boolean isCancelled;
    private boolean isAccepted;

    ShortOffer(long idPhoneCall, byte offerSource, String textMessage) {
        this.idPhoneCall = idPhoneCall;
        this.offerSource = offerSource;
        this.textMessage = textMessage;
        this.isCancelled = false;
        this.isAccepted = false;
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

    public boolean isCanceled() {
        return isCancelled;
    }

    public void cancel(String cancelText) {
        textMessage = cancelText;
        isCancelled = true;
    }

    public void accept() {
        isAccepted = true;
    }

    public boolean isAccepted() {
        return isAccepted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShortOffer that = (ShortOffer) o;

        return idPhoneCall == that.idPhoneCall;

    }

    @Override
    public int hashCode() {
        return (int) (idPhoneCall ^ (idPhoneCall >>> 32));
    }
}
