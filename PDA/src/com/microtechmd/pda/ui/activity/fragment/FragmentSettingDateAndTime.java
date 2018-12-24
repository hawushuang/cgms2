package com.microtechmd.pda.ui.activity.fragment;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.liangmutian.mypicker.DatePickerDialog;
import com.example.liangmutian.mypicker.TimePickerDialog;
import com.example.liangmutian.mypicker.TimezonePickerDialog;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.SPUtils;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.ui.widget.WidgetSettingItem;
import com.microtechmd.pda.util.TimeUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.microtechmd.pda.ui.activity.ActivityPDA.DATE_CHANGE;
import static com.microtechmd.pda.ui.activity.ActivityPDA.SETTING_TIME_FORMAT;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer.TYPE_SETTING;

public class FragmentSettingDateAndTime extends FragmentBase
        implements
        EntityMessage.Listener {
    private BroadcastReceiver mBroadcastReceiver;
    private View mRootView;
    private Dialog dateDialog, timeDialog, timezoneDialog;
    private Activity mCtx;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_setting_date_time, container, false);
        return mRootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCtx = activity;//mCtx 是成员变量，上下文引用
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCtx = null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                assert action != null;
                if (action.equals(Intent.ACTION_TIME_TICK)) {
                    if (getActivity() != null) {
                        updateDateTimeSetting(true);
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        getActivity().registerReceiver(mBroadcastReceiver, intentFilter);

        updateDateTimeSetting(true);

        if (Calendar.getInstance()
                .get(Calendar.YEAR) < ActivityPDA.YEAR_MIN) {
            setTime();
            setDate();
            setTimezone();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        switch (v.getId()) {
            case R.id.ibt_back:
                ((ActivityPDA) getActivity())
                        .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                                ParameterComm.SETTING_TYPE,
                                new byte[]{(byte) TYPE_SETTING}));
                break;
            case R.id.item_date:
                setDate();
                break;

            case R.id.item_time:
                setTime();
                break;
            case R.id.item_timezone:
                setTimezone();
                break;

            default:
                break;
        }
    }

    @Override
    public void onReceive(EntityMessage message) {
        switch (message.getOperation()) {
            case EntityMessage.OPERATION_SET:
                handleSet(message);
                break;

            case EntityMessage.OPERATION_GET:
                break;

            case EntityMessage.OPERATION_EVENT:
                handleEvent(message);
                break;

            case EntityMessage.OPERATION_NOTIFY:
                handleNotification(message);
                break;

            case EntityMessage.OPERATION_ACKNOWLEDGE:
                handleAcknowledgement(message);
                break;

            default:
                break;
        }
    }

    private void handleSet(EntityMessage message) {
    }


    protected void handleEvent(EntityMessage message) {
    }

    protected void handleNotification(EntityMessage message) {
    }


    protected void handleAcknowledgement(final EntityMessage message) {
    }

    private void setDate() {
        Calendar calendar = Calendar.getInstance();
        List<Integer> date = new ArrayList<>();
        date.add(calendar.get(Calendar.YEAR));
        date.add(calendar.get(Calendar.MONTH));
        date.add(calendar.get(Calendar.DAY_OF_MONTH));
        showDateDialog(date);
    }

    private void showDateDialog(List<Integer> date) {
        DatePickerDialog.Builder builder = new DatePickerDialog.Builder(getActivity())
                .setSelectYear(date.get(0) - 1)
                .setSelectMonth(date.get(1))
                .setSelectDay(date.get(2) - 1);
        builder.setOnDateSelectedListener(new DatePickerDialog.OnDateSelectedListener() {
            @Override
            public void onDateSelected(int[] dates) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, dates[0]);
                calendar.set(Calendar.MONTH, dates[1] - 1);
                calendar.set(Calendar.DAY_OF_MONTH, dates[2]);
                SystemClock.setCurrentTimeMillis(
                        calendar.getTimeInMillis());
                dateOrTimeChanged();
            }

            @Override
            public void onCancel() {
            }
        });

        dateDialog = builder.create();
        dateDialog.show();
        dateDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_HOME:
                        dateDialog.dismiss();
                        return true;
                }
                return false;
            }
        });
        dateDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);

    }

    private void setTime() {
        Calendar calendar = Calendar.getInstance();
        List<Integer> time = new ArrayList<>();
        time.add(calendar.get(Calendar.HOUR_OF_DAY));
        time.add(calendar.get(Calendar.MINUTE));
        showTimePick(time);
    }

    private void showTimePick(List<Integer> time) {
        TimePickerDialog.Builder builder = new TimePickerDialog.Builder(getActivity())
                .setSelectHour(time.get(0))
                .setSelectMinute(time.get(1));
        timeDialog = builder.setOnTimeSelectedListener(new TimePickerDialog.OnTimeSelectedListener() {
            @Override
            public void onTimeSelected(int[] times) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, times[0]);
                calendar.set(Calendar.MINUTE, times[1]);
                calendar.set(Calendar.SECOND, 0);
                SystemClock.setCurrentTimeMillis(
                        calendar.getTimeInMillis());
                dateOrTimeChanged();
            }
        }).create();

        timeDialog.show();
        timeDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_HOME:
                        timeDialog.dismiss();
                        return true;
                }
                return false;
            }
        });
        timeDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
    }

    private void setTimezone() {
        String zone = TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT);
        if (zone.contains("格林尼治标准时间")) {
            zone = zone.replace("格林尼治标准时间", "GMT");
        }
        char[] strs = zone.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strs.length; i++) {
            sb.append(strs[i]);
            if (i == 5) {
                sb.append(":");
            }
        }
        zone = sb.toString();
        TimezonePickerDialog.Builder builder = new TimezonePickerDialog.Builder(getActivity())
                .setsSlectValue(zone);
        timezoneDialog = builder.setOnTimeSelectedListener(new TimezonePickerDialog.OnTimeSelectedListener() {
            @Override
            public void onTimeSelected(String value) {
                AlarmManager mAlarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
                assert mAlarmManager != null;
                mAlarmManager.setTimeZone(value);
                dateOrTimeChanged();
            }
        }).create();
        timezoneDialog.show();
        timezoneDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_HOME:
                        timezoneDialog.dismiss();
                        return true;
                }
                return false;
            }
        });
        timezoneDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);

        timezoneDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
//                mLog.Error(FragmentSettingDateAndTime.this.getClass(), "时区dialog消失");
            }
        });
    }

    private void dateOrTimeChanged() {
        SPUtils.put(app, DATE_CHANGE, true);
        if (mCtx != null) {
//            ((ActivityPDA) mCtx)
//                    .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                            ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
//                            ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_SET,
//                            ParameterComm.RESET_DATA,
//                            new byte[]{(byte) 2}));

            ((ActivityPDA) mCtx).handleMessage(
                    new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                            ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                            ParameterGlobal.PORT_MONITOR,
                            ParameterGlobal.PORT_MONITOR,
                            EntityMessage.OPERATION_SET,
                            ParameterComm.FORCESYNCHRONIZEFLAG, new byte[]{}));
        }

        updateDateTimeSetting(true);
    }

    public void updateDateTimeSetting(boolean timeFormat) {
        WidgetSettingItem settingItem =
                (WidgetSettingItem) mRootView.findViewById(R.id.item_date);

        if (settingItem != null) {
            settingItem.setItemValue(getDateString(System.currentTimeMillis(), TimeZone.getDefault()));
        }

        settingItem = (WidgetSettingItem) mRootView.findViewById(R.id.item_time);
        if (settingItem != null) {
            settingItem.setItemValue(getTimeString(System.currentTimeMillis(), TimeZone.getDefault()));
        }

        settingItem = (WidgetSettingItem) mRootView.findViewById(R.id.item_timezone);
        if (settingItem != null) {
//            settingItem.setItemValue(TimeZone.getDefault().getDisplayName());
            String zone = TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT);
            if (zone.contains("标准")) {
                zone = zone.replace("标准", "");
            }
            settingItem.setItemValue(zone);


        }
        if (mCtx != null) {
            TimeUtil.set24HourFormat(timeFormat, mCtx);
            ((ActivityPDA) mCtx)
                    .getDataStorage(ActivityPDA.class.getSimpleName())
                    .setBoolean(SETTING_TIME_FORMAT,
                            timeFormat);
            ((ActivityPDA) mCtx).getStatusBar()
                    .setDateTime(System.currentTimeMillis(), timeFormat);
        }


//        long start = System.currentTimeMillis();
//        settingItem = (WidgetSettingItem) mRootView.findViewById(R.id.item_timezone);
//        if (settingItem != null) {
//            settingItem.setItemValue(TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT));
//        }
//        long end = System.currentTimeMillis();
//        mLog.Error(getClass(), "初始化时区：" + (end - start));
    }

    public String getDateString(long dateTime, TimeZone timeZone) {
        String template = "yyyy-M-d";

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(template);

        if (timeZone != null) {
            simpleDateFormat.setTimeZone(timeZone);
        }

        return simpleDateFormat.format(new Date(dateTime));
    }

    public String getTimeString(long dateTime, TimeZone timeZone) {
        String template = "H:mm";

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(template);

        if (timeZone != null) {
            simpleDateFormat.setTimeZone(timeZone);
        }

        return simpleDateFormat.format(new Date(dateTime));
    }
}
