package com.example.acer.taxiapp;

import java.util.List;

public interface MessageListProvider {
    List<PopupMessage> getMessages();
    void notifyChange();
}
