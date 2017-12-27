package com.microtechmd.pda.util;


import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.microtechmd.pda.library.entity.monitor.DateTime;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FormatterUtil implements IAxisValueFormatter {


    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return secToTime((int) (value * 100));
    }

    @NonNull
    private String bcdToTime(float t) {
        if (t < 0) {
            DateTime todayDateTime = new DateTime(Calendar.getInstance());
            todayDateTime.setDay(todayDateTime.getDay() - 1);
            todayDateTime.setHour(24);
            todayDateTime.setMinute(0);
            todayDateTime.setSecond(0);
            long aa = todayDateTime.getBCD();
            long datet = (long) t + todayDateTime.getBCD();
            DateTime dateTime = new DateTime();
            dateTime.setBCD((long) datet);
            return unitFormat(dateTime.getHour()) + ":" + unitFormat(dateTime.getMinute());
        } else {
            DateTime dateTime = new DateTime();
            dateTime.setBCD((long) t);
            return unitFormat(dateTime.getHour()) + ":" + unitFormat(dateTime.getMinute());
        }
    }

    private String secToTime(int time) {
        String timeStr = null;
        int hour;
        int minute;
        int second;
        if (time < 0) {
            int index = Math.abs(time) / 86400;
            time += 86400 * (index + 1);
        } else if (time == 0) {
            return "0:00";
        }
        minute = time / 60;
        if (minute < 60) {
            second = time % 60;
            timeStr = unitFormat(minute) + ":" + unitFormat(second);
        } else {
            hour = minute / 60;
            if (hour > 99)
                return "99:59:59";
            minute = minute % 60;
            second = time - hour * 3600 - minute * 60;
            timeStr = unitFormat(hour) + ":" + unitFormat(minute);
        }
        return timeStr;
    }

    private String unitFormat(int i) {
        String retStr = null;
        if (i >= 0 && i < 10) {
            retStr = "0" + Integer.toString(i);
        } else {
            retStr = "" + i;
        }
        return retStr;
    }

    /**
     * 将时间转化成毫秒
     * 时间格式: yyyy-MM-dd HH:mm:ss
     *
     * @param time
     * @return
     */
    @NonNull
    public static Long timeStrToSecond(String time) {
        try {
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return format.parse(time).getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0L;
    }

    @NonNull
    public static String dateToString(DateTime dateTime) {
        int year = dateTime.getYear();
        int mon = dateTime.getMonth();
        int day = dateTime.getDay();
        int hour = dateTime.getHour();
        int min = dateTime.getMinute();
        int second = dateTime.getSecond();
        return year + "-" + mon + "-" + day + " " + hour + ":" + min + ":" + second;
    }
}
