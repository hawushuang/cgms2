package com.microtechmd.pda.library.utility;

import java.util.Arrays;

/**
 * Created by Administrator on 2017/12/28.
 */

public class ByteUtil {

    /**
     * 将int数值转换为占四个字节的byte数组，本方法适用于(低位在前，高位在后)的顺序。 和bytesToInt（）配套使用
     *
     * @param value 要转换的int值
     * @return byte数组
     */
    public static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
     *
     * @param src byte数组
     * @return int数值
     */
    public static int bytesToInt(byte[] src) {
        int value;
        if (src.length > 2) {
            value = (src[0] & 0xFF)
                    | ((src[1] & 0xFF) << 8)
                    | ((src[2] & 0xFF) << 16)
                    | ((src[3] & 0xFF) << 24);
        } else {
            value = (src[0] & 0xFF)
                    | ((src[1] & 0xFF) << 8);
        }
        return value;
    }

    //byte 与 int 的相互转换
    public static byte intToByte(int x) {
        return (byte) x;
    }

    public static int byteToInt(byte b) {
        //Java 总是把 byte 当做有符处理；我们可以通过将其和 0xFF 进行二进制与得到它的无符值
        return b & 0xFF;
    }


    /****************************************/
    public static long[] Bytes2Longs(byte[] buf) {
        byte bLength = 8;
        long[] s = new long[buf.length / bLength];

        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = new byte[bLength];

            for (int jLoop = 0; jLoop < bLength; jLoop++) {
                temp[jLoop] = buf[iLoop * bLength + jLoop];
            }

            s[iLoop] = getLong(temp);
        }

        return s;
    }

    public static byte[] Longs2Bytes(long[] s) {
        byte bLength = 8;
        byte[] buf = new byte[s.length * bLength];

        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = getBytes(s[iLoop]);

            for (int jLoop = 0; jLoop < bLength; jLoop++) {
                buf[iLoop * bLength + jLoop] = temp[jLoop];
            }
        }

        return buf;
    }

    private static long getLong(byte[] buf) {
        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        }

        if (buf.length > 8) {
            throw new IllegalArgumentException("byte array size > 8 !");
        }

        long r = 0;
        for (byte aBuf : buf) {
            r <<= 8;
            r |= (aBuf & 0x00000000000000ff);
        }

        return r;
    }

    private static byte[] getBytes(long s) {
        byte[] buf = new byte[8];

        for (int i = buf.length - 1; i >= 0; i--) {
            buf[i] = (byte) (s & 0x00000000000000ff);
            s >>= 8;
        }

        return buf;
    }

    //合并两个数组
    public static byte[] concat(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;

    }
}
