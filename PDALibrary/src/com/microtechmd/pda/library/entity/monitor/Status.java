package com.microtechmd.pda.library.entity.monitor;

import com.microtechmd.pda.library.entity.DataBundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Status extends DataBundle {
    public static final int BYTE_ARRAY_LENGTH = 2;

    private static final String IDENTIFIER = "status";
    private static final String KEY_SHORT_VALUE1 = IDENTIFIER + "_short_value1";
    private static final String KEY_SHORT_VALUE1_SUPPLEMENT = IDENTIFIER + "_short_value1_supplement";


    public Status() {
        super();
    }

    public Status(byte[] byteArray) {
        super(byteArray);
    }

    public Status(int shortValue1) {
        super();
        setShortValue1(shortValue1);
    }

    public Status(int shortValue1, int shortValue1Supplement) {
        super();
        setShortValue1(shortValue1);
        setShortValue1Supplement(shortValue1Supplement);
    }

    public int getShortValue1() {
        return getInt(KEY_SHORT_VALUE1);
    }

    public int getShortValue1Supplement() {
        return (int) (getByte(KEY_SHORT_VALUE1_SUPPLEMENT) & 0xFF);
    }

    public void setShortValue1(int value) {
        setInt(KEY_SHORT_VALUE1, (short) value);
    }

    public void setShortValue1Supplement(int value) {
        setByte(KEY_SHORT_VALUE1_SUPPLEMENT, (byte) value);
    }

    @Override
    public byte[] getByteArray() {
        final DataOutputStreamLittleEndian dataOutputStream;
        final ByteArrayOutputStream byteArrayOutputStream;

        byteArrayOutputStream = new ByteArrayOutputStream();
        dataOutputStream = new DataOutputStreamLittleEndian(byteArrayOutputStream);

        try {
            byteArrayOutputStream.reset();
//            dataOutputStream.writeByte((byte) getShortValue1());
//            dataOutputStream.writeByte((byte) getShortValue1Supplement());
            dataOutputStream.writeShortLittleEndian((short) getShortValue1());
//            dataOutputStream.writeShortLittleEndian((short) getShortValue1Supplement());
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
//                setShortValue1((int) dataInputStream.readByte());
//                setShortValue1Supplement((int) dataInputStream.readByte());
                int value = dataInputStream.readShortLittleEndian();
                setShortValue1(value);
//                setShortValue1Supplement((int) dataInputStream.readShortLittleEndian());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
