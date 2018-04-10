package com.microtechmd.pda.library.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ValueByte extends DataBundle {
    private static final int VALUE_LENGTH = 1;
    private static final String IDENTIFIER = "btye";
    private static final String KEY_VALUE = IDENTIFIER + "_value";


    public ValueByte() {
        super();
    }

    public ValueByte(byte[] byteArray) {
        super(byteArray);
    }

    public ValueByte(int value) {
        super();
        setValue(value);
    }

    public byte getValue() {
        return getByte(KEY_VALUE);
    }

    public void setValue(int value) {
        setByte(KEY_VALUE, (byte) value);
    }

    @Override
    public byte[] getByteArray() {
        final DataOutputStreamLittleEndian dataOutputStream;
        final ByteArrayOutputStream byteArrayOutputStream;

        byteArrayOutputStream = new ByteArrayOutputStream();
        dataOutputStream = new DataOutputStreamLittleEndian(byteArrayOutputStream);

        try {
            byteArrayOutputStream.reset();
            dataOutputStream.writeByte(getValue());
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

        if (byteArray.length >= VALUE_LENGTH) {
            final DataInputStreamLittleEndian dataInputStream;
            final ByteArrayInputStream byteArrayInputStream;

            byteArrayInputStream = new ByteArrayInputStream(byteArray);
            dataInputStream = new DataInputStreamLittleEndian(byteArrayInputStream);

            try {
                clearBundle();
                setValue(dataInputStream.readByte());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
