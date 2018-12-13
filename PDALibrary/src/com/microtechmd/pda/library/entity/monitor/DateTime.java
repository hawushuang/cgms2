package com.microtechmd.pda.library.entity.monitor;


import com.microtechmd.pda.library.entity.DataBundle;
import com.microtechmd.pda.library.utility.ByteUtil;

import java.util.Calendar;
import java.util.Date;


public class DateTime extends DataBundle {

    public static final long BASE_TIME = 946656000000L;

    public static final int BYTE_ARRAY_LENGTH = 4;
    public static final int SECOND_PER_HOUR = 864;
    public static final int MILLISECOND_PER_SECOND = 1000;
    public static final int SECOND_PER_MINUTE = 60;
    public static final int MINUTE_PER_HOUR = 60;
    public static final int HOUR_PER_DAY = 24;
    public static final int DAY_PER_WEEK = 7;
    public static final int DAY_PER_YEAR = 365;
    public static final int MONTH_PER_YEAR = 12;
    public static final int YEAR_BASE = 2000;

    private static final String IDENTIFIER = "datetime";
    private static final String KEY_YEAR = IDENTIFIER + "_year";
    private static final String KEY_MONTH = IDENTIFIER + "_month";
    private static final String KEY_DAY = IDENTIFIER + "_day";
    private static final String KEY_HOUR = IDENTIFIER + "_hour";
    private static final String KEY_MINUTE = IDENTIFIER + "_minute";
    private static final String KEY_SECOND = IDENTIFIER + "_second";


    public DateTime() {
        super();
    }


    public DateTime(byte[] byteArray) {
        super(byteArray);
    }


    public DateTime(int year, int month, int day, int hour, int minute,
                    int second) {
        super();
        setYear(year);
        setMonth(month);
        setDay(day);
        setHour(hour);
        setMinute(minute);
        setSecond(second);
    }


    public DateTime(Calendar calendar) {
        super();
        setCalendar(calendar);
    }


    public DateTime(long bcd) {
        super();
        setBCD(bcd);
    }


    public Calendar getCalendar() {
        final Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, YEAR_BASE + getYear());
        calendar.set(Calendar.MONTH, getMonth() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, getDay());
        calendar.set(Calendar.HOUR_OF_DAY, getHour());
        calendar.set(Calendar.MINUTE, getMinute());
        calendar.set(Calendar.SECOND, getSecond());
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }


    public long getBCD() {
        return (long) getSecond() + ((long) getMinute() * 100L) +
                ((long) getHour() * 10000L) + ((long) getDay() * 1000000L) +
                ((long) getMonth() * 100000000L) +
                ((long) (getYear() + YEAR_BASE) * 10000000000L);
    }


    public int getYear() {
        return (int) getByte(KEY_YEAR);
    }


    public int getMonth() {
        return (int) getByte(KEY_MONTH);
    }


    public int getDay() {
        return (int) getByte(KEY_DAY);
    }


    public int getHour() {
        return (int) getByte(KEY_HOUR);
    }


    public int getMinute() {
        return (int) getByte(KEY_MINUTE);
    }


    public int getSecond() {
        return (int) getByte(KEY_SECOND);
    }


    public void setCalendar(Calendar calendar) {
        setYear(calendar.get(Calendar.YEAR) -
                ((calendar.get(Calendar.YEAR) / 100) * 100));
        setMonth(calendar.get(Calendar.MONTH) + 1);
        setDay(calendar.get(Calendar.DAY_OF_MONTH));
        setHour(calendar.get(Calendar.HOUR_OF_DAY));
        setMinute(calendar.get(Calendar.MINUTE));
        setSecond(calendar.get(Calendar.SECOND));
    }


    public void setBCD(long bcd) {
        if (bcd < (long) YEAR_BASE * 10000000000L) {
            setYear(0);
        } else {
            setYear((int) (bcd / 10000000000L) - YEAR_BASE);
        }

        bcd %= 10000000000L;
        setMonth((int) (bcd / 100000000L));
        bcd %= 100000000L;
        setDay((int) (bcd / 1000000L));
        bcd %= 1000000L;
        setHour((int) (bcd / 10000L));
        bcd %= 10000L;
        setMinute((int) (bcd / 100L));
        bcd %= 100L;
        setSecond((int) bcd);
    }


    public void setYear(int year) {
        setByte(KEY_YEAR, (byte) year);
    }


    public void setMonth(int month) {
        setByte(KEY_MONTH, (byte) month);
    }


    public void setDay(int day) {
        setByte(KEY_DAY, (byte) day);
    }


    public void setHour(int hour) {
        setByte(KEY_HOUR, (byte) hour);
    }


    public void setMinute(int minute) {
        setByte(KEY_MINUTE, (byte) minute);
    }


    public void setSecond(int second) {
        setByte(KEY_SECOND, (byte) second);
    }


    @Override
    public byte[] getByteArray() {
        long time = getCalendar().getTimeInMillis() - BASE_TIME;

        return ByteUtil.intToBytes((int) (time/1000));
//        final DataOutputStreamLittleEndian dataOutputStream;
//        final ByteArrayOutputStream byteArrayOutputStream;
//
//        byteArrayOutputStream = new ByteArrayOutputStream();
//        dataOutputStream =
//                new DataOutputStreamLittleEndian(byteArrayOutputStream);
//
//        try {
//            byteArrayOutputStream.reset();
//            dataOutputStream.writeByte((byte) getYear());
//            dataOutputStream.writeByte((byte) getMonth());
//            dataOutputStream.writeByte((byte) getDay());
//            dataOutputStream.writeByte((byte) getHour());
//            dataOutputStream.writeByte((byte) getMinute());
//            dataOutputStream.writeByte((byte) getSecond());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return byteArrayOutputStream.toByteArray();
    }


    @Override
    public void setByteArray(byte[] byteArray) {
        if (byteArray == null) {
            return;
        }

        if (byteArray.length >= BYTE_ARRAY_LENGTH) {
            long addTime = ByteUtil.bytesToInt(byteArray) * 1000L;
            Date date = new Date(BASE_TIME + addTime);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            clearBundle();
            setYear(calendar.get(Calendar.YEAR) -
                    ((calendar.get(Calendar.YEAR) / 100) * 100));
            setMonth(calendar.get(Calendar.MONTH) + 1);
            setDay(calendar.get(Calendar.DAY_OF_MONTH));
            setHour(calendar.get(Calendar.HOUR_OF_DAY));
            setMinute(calendar.get(Calendar.MINUTE));
            setSecond(calendar.get(Calendar.SECOND));
        }
    }
}
