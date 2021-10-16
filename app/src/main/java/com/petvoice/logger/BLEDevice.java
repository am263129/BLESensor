package com.petvoice.logger;

public class BLEDevice {
    private String name, address;

    public BLEDevice(String name, String address){
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}
