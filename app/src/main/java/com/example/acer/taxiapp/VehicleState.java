package com.example.acer.taxiapp;

public enum VehicleState {
    NEDEFINIRANO(1),
    SLOBODEN(2),
    CHEKA_NARACHKA(3),
    CHEKA_ODGOVOR(4),
    ODI_KON_KLIENT(5),
    ZONA_NA_KLIENT(6),
    ZAFATEN(7),
    PAUZA(8),
    GOVOR(9),
    ALARM(10),
    POTVRDEN_ALARM(11),
    KRAJ_NA_SMENA(12),
    PREDADENI_KLUCHEVI(13),
    MOVE_TO_CLIENT_NEW_PHONE_CALL(50),
    WAIT_CLIENT_NEW_PHONE_CALL(60),
    BUSY_NEXT_PHONE_CALL(70),
    FISCAL_BEFORE_IDLE(71);

    private int stateId;

    VehicleState(int stateId) {
        this.stateId = stateId;
    }

    public static VehicleState getByValue(int value) {
        switch(value) {
            case 1:
                return NEDEFINIRANO;
            case 2:
                return SLOBODEN;
            case 3:
                return CHEKA_NARACHKA;
            case 4:
                return CHEKA_ODGOVOR;
            case 5:
                return ODI_KON_KLIENT;
            case 6:
                return ZONA_NA_KLIENT;
            case 7:
                return ZAFATEN;
            case 8:
                return PAUZA;
            case 9:
                return GOVOR;
            case 10:
                return ALARM;
            case 11:
                return POTVRDEN_ALARM;
            case 12:
                return KRAJ_NA_SMENA;
            case 13:
                return PREDADENI_KLUCHEVI;
            case 50:
                return MOVE_TO_CLIENT_NEW_PHONE_CALL;
            case 60:
                return WAIT_CLIENT_NEW_PHONE_CALL;
            case 70:
                return BUSY_NEXT_PHONE_CALL;
            case 71:
                return FISCAL_BEFORE_IDLE;
            default:
                return null;
        }
    }


}
