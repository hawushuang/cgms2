package com.microtechmd.pda.util;


import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;

import com.microtechmd.pda.manager.SharePreferenceManager;


public class TimeUtil {

    public static final String getStatusTimeByTimeMillis(long timeMills,
                                                         String formatType) {
        Date date = new Date();
        date.setTime(timeMills);
        SimpleDateFormat formatter = new SimpleDateFormat(formatType,
                AndroidSystemInfoUtil.getLanguage());
        String statusTime = formatter.format(date);
        return statusTime;
    }


    public static final String getCurrentDate(Context context) {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(
                SharePreferenceManager.getDateFormat(context),
                AndroidSystemInfoUtil.getLanguage());
        String dateStr = formatter.format(date);
        return dateStr;
    }


    public static final long getDayTimestamp(long timestamp) {
        Date date = new Date();
        date.setTime(timestamp);
        int day = date.getDate();
        int month = date.getMonth();
        int year = date.getYear();
        date.setTime(0);
        date.setYear(year);
        date.setMonth(month);
        date.setDate(day);
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
        return date.getTime();
    }


    public static final long getHourTimestamp(long timestamp) {
        Date date = new Date();
        date.setTime(timestamp);
        int day = date.getDate();
        int month = date.getMonth();
        int year = date.getYear();
        int hour = date.getHours();
        date.setTime(0);
        date.setYear(year);
        date.setMonth(month);
        date.setDate(day);
        date.setHours(hour + 1);
        date.setMinutes(0);
        date.setSeconds(0);
        return date.getTime();
    }


    public static final String getHistoryTime(Context context, long timestamp) {
        Date date = new Date();
        SimpleDateFormat formatter;
        date.setTime(timestamp);
        if (TimeUtil.is24HourFormat(context)) {
            formatter = new SimpleDateFormat("HH:mm:ss",
                    AndroidSystemInfoUtil.getLanguage());
            return formatter.format(date);
        } else {
            formatter = new SimpleDateFormat("hh:mm:ssa",
                    AndroidSystemInfoUtil.getLanguage());
            return formatter.format(date);
            /*
             * if (str.equals("12:00AM")) { return "00:00AM"; } else if
			 * (str.equals("12:00����")) { return "00:00����"; } else { return
			 * str; }
			 */
        }
    }


    public static final String getCurrentTime(Context context) {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(
                getTimeFormat(context), AndroidSystemInfoUtil.getLanguage());
        String str = formatter.format(date);
        return str;
    }


    public static final String getTimeFormat(Context context) {
        String str = SharePreferenceManager.getTimeFormat(context);
        if ("hh:mm".endsWith(str)) {
            str += " a";
        }
        return str;
    }


    public static final String getTimeHour(Context context, float hour) {
        DecimalFormat fnum = new DecimalFormat("##0.0");
        String[] strs = fnum.format(hour).split("\\.");
        String str = strs[0];
        String str1 = "0." + strs[1];
        int a = Integer.parseInt(str);
        if (!is24HourFormat(context)) {
            if (a == 0 || a == 12 || a == 24) {
                str = "12";
            } else if (a < 10) {
                str = "0" + a;
            } else if (a > 12) {
                str = a - 12 + "";
            } else {
                str = a + "";
            }
        } else {
            if (a < 10) {
                str = "0" + a;
            }
        }

        int b = (int) (Float.parseFloat(str1) * 60);
        if (b < 10) {
            str1 = "0" + b;
        } else {
            str1 = b + "";
        }
        return str + ":" + str1;
    }


    public static final String getTimeHourChart(Context context, float hour) {
        String str = "" + hour;
        if (!is24HourFormat(context)) {
            if (hour > 12) {
                hour -= 12;
                str = hour + "p";
            } else {
                str = hour + "a";
            }
        }
        return str;
    }


    public static final String getTimeHourBasal(Context context, float hour) {
        DecimalFormat fnum = new DecimalFormat("##0.0");
        String[] strs = fnum.format(hour).split("\\.");
        String str = strs[0];
        String str1 = "0." + strs[1];
        String str3 = "";
        int a = Integer.parseInt(str);
        if (a < 12) {
            if (a == 0 && !is24HourFormat(context)) {
                str = "12";
            } else if (a < 10) {
                str = "0" + a;
            } else {
                str = a + "";
            }
            if (!is24HourFormat(context)) {
                str3 = "a";
            }
        } else if (a == 24) {
            if (is24HourFormat(context)) {
                str = a + "";
            } else {
                str = "12";
                str3 = "a";
            }
        } else {
            if (!is24HourFormat(context)) {
                str3 = "p";
                a -= 12;
                if (a == 0) {
                    str = "12";
                } else if (a < 10) {
                    str = "0" + a;
                } else {
                    str = a + "";
                }
            } else {
                str = a + "";
            }
        }

        int b = (int) (Float.parseFloat(str1) * 60);
        if (b < 10) {
            str1 = "0" + b;
        } else {
            str1 = b + "";
        }
        return str + ":" + str1 + str3;
    }


    public static boolean is24HourFormat(Context context) {
        boolean result = false;
        String strTimeFormat = SharePreferenceManager.getTimeFormat(context);
        if (strTimeFormat.equals(SharePreferenceManager.TIME_FORMAT_24)) {
            result = true;
        }

        return result;
    }


    public static void set24HourFormat(boolean value, Context context) {
        if (value) {
            SharePreferenceManager.setTimeFormat(context,
                    SharePreferenceManager.TIME_FORMAT_24);
        } else {
            SharePreferenceManager.setTimeFormat(context,
                    SharePreferenceManager.TIME_FORMAT_12);
        }

    }


    public static long getFirstWeekday(long timestamp) {
        Date date = new Date();
        date.setTime(timestamp);
        return timestamp - (date.getDay()) * 24l * 60l * 60l * 1000l;
    }

}
