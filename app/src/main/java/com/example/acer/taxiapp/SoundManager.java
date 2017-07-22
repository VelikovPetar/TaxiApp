package com.example.acer.taxiapp;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;

public class SoundManager {

    private Context context;
    private SoundPool soundPool;
    private int numberLoadedSounds = 0;
    private boolean canPlay;
    private int messageSound, shortOfferSound, longOfferSound;

    public SoundManager(Context context) {
        this.context = context;
        if(Build.VERSION.SDK_INT >= 21) {
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(1)
                    .build();
        } else {
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        }
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                numberLoadedSounds ++;
                if(numberLoadedSounds == 3) {
                    canPlay = true;
                }
            }
        });
        messageSound = soundPool.load(this.context, R.raw.message_sound, 1);
        shortOfferSound = soundPool.load(this.context, R.raw.short_offer_sound, 1);
        longOfferSound = soundPool.load(this.context, R.raw.long_offer_sound, 1);
    }

    public void playMessageSound() {
        if(canPlay) {
            soundPool.play(messageSound, 1, 1, 1, 0, 1);
        }
    }

    public void playShortOfferSound() {
        if(canPlay) {
            soundPool.play(shortOfferSound, 1, 1, 1, 0, 1);
        }
    }

    public void playLongOfferSound() {
        if(canPlay) {
            soundPool.play(longOfferSound, 1, 1, 1, 0, 1);
        }
    }

    public void release() {
        soundPool.release();
        soundPool = null;
    }
}
