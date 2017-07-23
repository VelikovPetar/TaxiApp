package com.example.acer.taxiapp;

import com.example.acer.taxiapp.models.PopupMessage;

import java.util.List;

public interface MessageListProvider {
    List<PopupMessage> getMessages();
    void notifyChange();
}
