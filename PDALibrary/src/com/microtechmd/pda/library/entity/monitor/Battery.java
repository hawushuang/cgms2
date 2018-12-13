package com.microtechmd.pda.library.entity.monitor;

import com.microtechmd.pda.library.entity.DataBundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Battery extends DataBundle {
    public static final int BYTE_ARRAY_LENGTH = 2;

    private static final String IDENTIFIER = "battery";
    private static final String KEY_BATTERY_VALUE = IDENTIFIER + "_value";
    private static final String KEY_ELAPSED_TIME = IDENTIFIER + "_elapsed_time";


    public Battery() {
        super();
    }

    public Battery(byte[] byteArray) {
        super(byteArray);
    }

    public Battery(int value) {
        super();
        setBatteryValue(value);
    }

    public Battery(int value, int value2) {
        super();
        setBatteryValue(value);
        setElapsedtime(value2);
    }


    public int getBatteryValue() {
        return (int) getByte(KEY_BATTERY_VALUE);
    }

    public int getElapsedtime() {
        return (int) getByte(KEY_ELAPSED_TIME);
    }

    public void setBatteryValue(int value) {
        setByte(KEY_BATTERY_VALUE, (byte) value);
    }

    public void setElapsedtime(int value) {
        setByte(KEY_ELAPSED_TIME, (byte) value);
    }

    @Override
    public byte[] getByteArray() {
        final DataOutputStreamLittleEndian dataOutputStream;
        final ByteArrayOutputStream byteArrayOutputStream;

        byteArrayOutputStream = new ByteArrayOutputStream();
        dataOutputStream = new DataOutputStreamLittleEndian(byteArrayOutputStream);

        try {
            byteArrayOutputStream.reset();
            dataOutputStream.writeByte((byte) getBatteryValue());
            dataOutputStream.writeByte((byte) getElapsedtime());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public void setByteArray(byte[] byteArray) {
        if (byteArray == null) {
            return;
        }

        if (byteArray.length >= BYTE_ARRAY_LENGTH) {
            final DataInputStreamLittleEndian dataInputStream;
            final ByteArrayInputStream byteArrayInputStream;

            byteArrayInputStream = new ByteArrayInputStream(byteArray);
            dataInputStream = new DataInputStreamLittleEndian(byteArrayInputStream);

            try {
                clearBundle();
                setBatteryValue((int) dataInputStream.readByte());
                setElapsedtime((int) dataInputStream.readByte());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
