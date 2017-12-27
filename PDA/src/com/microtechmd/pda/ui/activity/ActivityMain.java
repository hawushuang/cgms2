package com.microtechmd.pda.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.widget.RadioGroup;

import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.StatusPump;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.ui.activity.fragment.FragmentBase;
import com.microtechmd.pda.ui.activity.fragment.FragmentCalibration;
import com.microtechmd.pda.ui.activity.fragment.FragmentHome;
import com.microtechmd.pda.ui.activity.fragment.FragmentSettings;

import java.util.Calendar;


public class ActivityMain extends ActivityPDA {
    public static final String SETTING_STARTUP_TIME = "startup_time";

    public static final long STARTUP_DELAY = 1 * DateTime.SECOND_PER_MINUTE *
            DateTime.MILLISECOND_PER_SECOND / 10;

    private static StatusPump sStatus = null;
    private FragmentBase mFragmentHome = null;
    private FragmentBase mFragmentCalibration = null;
    private FragmentBase mFragmentSettings = null;

    private BroadcastReceiver mBroadcastReceiver = null;

    private Handler mHandlerTimer = null;
    private Runnable mRunnableTimer = null;
    private long mStartupTime = 0;


    public static StatusPump getStatus() {
        return sStatus;
    }


    public static void setStatus(StatusPump status) {
        if (status == null) {
            sStatus = null;
        } else {
            sStatus = new StatusPump(status.getByteArray());
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            updateTimer();
        }
    }

    public void switchContent(FragmentBase to) {
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .hide(mFragmentHome).commit();
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .hide(mFragmentCalibration).commit();
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .hide(mFragmentSettings).commit();
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .show(to).commit();
    }

    public void add(FragmentBase from) {
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.layout_fragment, from).commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateTimer();
        mFragmentHome = new FragmentHome();
        mFragmentCalibration = new FragmentCalibration();
        mFragmentSettings = new FragmentSettings();

        add(mFragmentHome);
        add(mFragmentCalibration);
        add(mFragmentSettings);

        switchContent(mFragmentHome);

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radio_group_tab);
        radioGroup
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    FragmentBase fragMent = null;


                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        switch (group.getCheckedRadioButtonId()) {
                            case R.id.radio_button_tab_home:

                                if (mFragmentHome == null) {
                                    mFragmentHome = new FragmentHome();
                                }

                                fragMent = mFragmentHome;
                                switchContent(mFragmentHome);
                                break;

                            case R.id.radio_button_tab_calibration:

                                if (mFragmentCalibration == null) {
                                    mFragmentCalibration = new FragmentCalibration();
                                }

                                fragMent = mFragmentCalibration;
                                switchContent(mFragmentCalibration);
                                break;

                            case R.id.radio_button_tab_settings:

                                if (mFragmentSettings == null) {
                                    mFragmentSettings = new FragmentSettings();
                                }

                                fragMent = mFragmentSettings;
                                switchContent(mFragmentSettings);
                                break;

                            default:
                                break;
                        }

//                        if (fragMent != null) {
//                            ActivityMain.this.getSupportFragmentManager()
//                                    .beginTransaction()
//                                    .replace(R.id.layout_fragment, fragMent).commit();
//                        }
                    }
                });

        if (radioGroup.getCheckedRadioButtonId() < 0) {
            radioGroup.check(R.id.radio_button_tab_home);
        }

        ((ApplicationPDA) getApplication()).registerMessageListener(
                ParameterGlobal.PORT_GLUCOSE, mMessageListener);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(Intent.ACTION_SCREEN_ON)) {
                    onScreenOn();
                } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    onScreenOff();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }


    @Override
    protected void onResume() {
        super.onResume();

        getStatusBar().setGlucose(true);

        if (Calendar.getInstance()
                .get(Calendar.YEAR) < YEAR_MIN) {
            RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radio_group_tab);
            radioGroup.check(R.id.radio_button_tab_settings);
        }
    }


    @Override
    protected void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        ((ApplicationPDA) getApplication()).unregisterMessageListener(
                ParameterGlobal.PORT_GLUCOSE, mMessageListener);

        super.onDestroy();
    }


    @Override
    public void onBackPressed() {
    }


    @Override
    protected void onHomePressed() {
    }


    @Override
    protected void handleNotification(final EntityMessage message) {
        super.handleNotification(message);

        if ((message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE) &&
                (message.getParameter() == ParameterGlucose.PARAM_SIGNAL_PRESENT)) {
            if (getStatusBar().getGlucose()) {
                Intent intent = new Intent(this, ActivityBgApply.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }
    }


    private void onScreenOn() {
        mLog.Debug(getClass(), "Screen on");

        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.PARAM_BROADCAST_OFFSET, new byte[]
                {
                        ParameterComm.BROADCAST_OFFSET_ALL
                }));
    }

    private void onScreenOff() {
        mLog.Debug(getClass(), "Screen off");

        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.PARAM_BROADCAST_OFFSET, new byte[]
                {
                        ParameterComm.BROADCAST_OFFSET_STATUS
                }));

        Intent lockIntent = new Intent(this, ActivityUnlock.class);
        startActivity(lockIntent);
    }


    private void updateTimer() {
        final int STARTUP_DELAY_TICK_CYCLE = 1000;


        mStartupTime = getDataStorage(null).getLong(SETTING_STARTUP_TIME, 0);

        if (mStartupTime <= 0) {
            return;
        }

        if (mHandlerTimer == null) {
            mHandlerTimer = new Handler();
        }

        if (mRunnableTimer == null) {
            mRunnableTimer = new Runnable() {
                @Override
                public void run() {
                    updateStartupDelay();

                    if (mStartupTime > 0) {
                        mHandlerTimer.removeCallbacks(mRunnableTimer);
                        mHandlerTimer.postDelayed(mRunnableTimer,
                                STARTUP_DELAY_TICK_CYCLE);
                    }
                }
            };
        }

        mHandlerTimer.removeCallbacks(mRunnableTimer);
        mHandlerTimer.post(mRunnableTimer);
    }


    private void updateStartupDelay() {
        long interval = System.currentTimeMillis() - mStartupTime;

        if ((interval < 0) || (interval >= STARTUP_DELAY)) {
            mStartupTime = 0;
            getDataStorage(ActivityMain.class.getSimpleName()).setLong(SETTING_STARTUP_TIME,
                    mStartupTime);
            handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                    ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                    ParameterGlobal.PORT_COMM,
                    ParameterGlobal.PORT_COMM,
                    EntityMessage.OPERATION_SET,
                    ParameterComm.PARAM_RF_BROADCAST_SWITCH, new byte[]
                    {
                            (byte) 1
                    }));
            dismissDialogProgress();
        } else {
            setProgressContent(getString(R.string.fragment_settings_startup) +
                    "\n" + DateUtils.formatElapsedTime((STARTUP_DELAY - interval) /
                    DateTime.MILLISECOND_PER_SECOND));
        }
    }
}
