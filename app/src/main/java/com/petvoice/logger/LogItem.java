package com.petvoice.logger;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class LogItem {
    private String message, datetime;

    public LogItem(String message){
        this.message = message;
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE, dd-MMM-yyyy hh-mm-ss a");
        this.datetime = simpleDateFormat.format(calendar.getTime());
    }

    public void setName(String name) {
        this.message = name;
    }

    public void setAddress(String address) {
        this.datetime = address;
    }

    public String getDateTime() {
        return datetime;
    }

    public String getLog() {
        return message;
    }
}
