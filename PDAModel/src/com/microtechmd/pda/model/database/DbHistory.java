package com.microtechmd.pda.model.database;

import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.enums.AssignType;

/**
 * Created by Administrator on 2017/12/29.
 */

public class DbHistory {
    @PrimaryKey(AssignType.AUTO_INCREMENT)
    private long id;

    private String rf_address;

    private long date_time;

    private int event_index;

    private int event_type;

    private int supplement_value;

    private int sensorIndex;

    private int value;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRf_address() {
        return rf_address;
    }

    public void setRf_address(String rf_address) {
        this.rf_address = rf_address;
    }

    public long getDate_time() {
        return date_time;
    }

    public void setDate_time(long date_time) {
        this.date_time = date_time;
    }

    public int getEvent_index() {
        return event_index;
    }

    public void setEvent_index(int event_index) {
        this.event_index = event_index;
    }

    public int getEvent_type() {
        return event_type;
    }

    public void setEvent_type(int event_type) {
        this.event_type = event_type;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }


    public int getSupplement_value() {
        return supplement_value;
    }

    public void setSupplement_value(int supplement_value) {
        this.supplement_value = supplement_value;
    }

    public int getSensorIndex() {
        return sensorIndex;
    }

    public void setSensorIndex(int sensorIndex) {
        this.sensorIndex = sensorIndex;
    }
}
