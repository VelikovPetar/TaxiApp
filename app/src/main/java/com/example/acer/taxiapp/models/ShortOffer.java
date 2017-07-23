package com.example.acer.taxiapp.models;

public class ShortOffer {

    private long idPhoneCall;
    private byte offerSource;
    private String textMessage;
    private boolean isCanceled;
    private boolean isAccepted;
    private boolean isRead;

    public ShortOffer(long idPhoneCall, byte offerSource, String textMessage) {
        this.idPhoneCall = idPhoneCall;
        this.offerSource = offerSource;
        this.textMessage = textMessage;
        this.isCanceled = false;
        this.isAccepted = false;
        this.isRead = false;
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
        return isCanceled;
    }

    public void cancel(String cancelText) {
        textMessage = cancelText;
        isCanceled = true;
    }

    public void read() {
        isRead = true;
    }

    public boolean isRead() {
        return isRead;
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
