package com.microtechmd.pda.control.util;

import com.microtechmd.pda.library.utility.ByteUtil;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.microtechmd.pda.library.entity.monitor.DateTime.BASE_TIME;

/**
 * Created by Administrator on 2017/12/29.
 */

public class DbHistory {
    private long id;

    private byte[] byteArray;

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

    public DbHistory() {
    }

    public DbHistory(byte[] byteArray) {
        this.byteArray = byteArray;
        setByteArray(byteArray);
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

    public byte[] getByteArray() {
        return byteArray;
    }

    public void setByteArray(byte[] byteArray) {
        this.byteArray = byteArray;

        try {
            DataInputStreamLittleEndian dataInputStream;
            ByteArrayInputStream byteArrayInputStream;
            byteArrayInputStream = new ByteArrayInputStream(byteArray);
            dataInputStream = new DataInputStreamLittleEndian(byteArrayInputStream);

            byte[] battery = new byte[2];
            dataInputStream.read(battery, 0, 2);

            byte[] dateTime = new byte[4];
            dataInputStream.read(dateTime, 0, 4);
            long addTime = ByteUtil.bytesToInt(dateTime) * 1000L;
            setDate_time(BASE_TIME + addTime);


            byte[] event = new byte[4];
            dataInputStream.read(event, 0, 4);
            ByteArrayInputStream byteArrayInputStreamEvent = new ByteArrayInputStream(event);
            DataInputStreamLittleEndian dataInputStreamEvent = new DataInputStreamLittleEndian(byteArrayInputStreamEvent);
            setEvent_index((int) dataInputStreamEvent.readShortLittleEndian());
            setSensorIndex(dataInputStreamEvent.readByteLittleEndian());
            setEvent_type(dataInputStreamEvent.readByteLittleEndian());


            byte[] status = new byte[2];
            dataInputStream.read(status, 0, 2);
            ByteArrayInputStream byteArrayInputStreamStatus = new ByteArrayInputStream(status);
            DataInputStreamLittleEndian dataInputStreamStatus = new DataInputStreamLittleEndian(byteArrayInputStreamStatus);
            setValue(dataInputStreamStatus.readByteLittleEndian());
            setSupplement_value(dataInputStreamStatus.readByteLittleEndian());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class DataInputStreamLittleEndian extends DataInputStream {
        DataInputStreamLittleEndian(InputStream out) {
            super(out);
        }

        final short readShortLittleEndian() throws IOException {
            short value;
            int result;
            value = readShort();
            result = ((int) value << 8) & 0x0000FF00;
            result |= ((int) value >> 8) & 0x000000FF;

            return (short) result;
        }

        final int readByteLittleEndian() throws IOException {
            int value;
            int result;
            value = readByte();
            result = value & 0x000000FF;

            return result;
        }

        public final int readIntLittleEndian() throws IOException {
            int value;
            int result = 0;

            value = readInt();
            result = ((int) value << 24) & 0xFF000000;
            result |= ((int) value << 8) & 0x00FF0000;
            result |= ((int) value >> 8) & 0x0000FF00;
            result |= ((int) value >> 24) & 0x000000FF;

            return result;
        }
    }
}
