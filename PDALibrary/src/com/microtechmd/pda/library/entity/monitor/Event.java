package com.microtechmd.pda.library.entity.monitor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.microtechmd.pda.library.entity.DataBundle;

public class Event extends DataBundle {
    public static final int BYTE_ARRAY_LENGTH = 4;

    public static final int OBSOLETE_EVENT_MASK = 0x8000;

    private static final String IDENTIFIER = "event";
    private static final String KEY_INDEX = IDENTIFIER + "_index";
    private static final String KEY_EVENT = IDENTIFIER + "_event";
    private static final String KEY_SENSOR_INDEX = IDENTIFIER + "sensor_index";


    public Event() {
        super();
    }

    public Event(byte[] byteArray) {
        super(byteArray);
    }

    public Event(int index, int event, int value) {
        super();
        setIndex(index);
        setEvent(event);
        setSensorIndex(value);
    }

    public int getIndex() {
        return (int) getShort(KEY_INDEX);
    }

    public int getEvent() {
        return ((int) getByte(KEY_EVENT)) & 0x1F;
    }

    public int getEventFlag() {
        return (int) getByte(KEY_EVENT);
    }

    public int getSensorIndex() {
        return (int) getByte(KEY_SENSOR_INDEX);
    }

    public void setIndex(int index) {
        setShort(KEY_INDEX, (short) index);
    }

    public void setEvent(int event) {
        setByte(KEY_EVENT, (byte) event);
    }

    public void setSensorIndex(int value) {
        setByte(KEY_SENSOR_INDEX, (byte) value);
    }

    @Override
    public byte[] getByteArray() {
        final DataOutputStreamLittleEndian dataOutputStream;
        final ByteArrayOutputStream byteArrayOutputStream;

        byteArrayOutputStream = new ByteArrayOutputStream();
        dataOutputStream = new DataOutputStreamLittleEndian(byteArrayOutputStream);

        try {
            byteArrayOutputStream.reset();
            dataOutputStream.writeShortLittleEndian((short) getIndex());
            dataOutputStream.writeByte((byte) getSensorIndex());
            dataOutputStream.writeByte((byte) getEvent());
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
                setIndex((int) dataInputStream.readShortLittleEndian());
                setSensorIndex((int) dataInputStream.readByte());
                setEvent((int) dataInputStream.readByte());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
