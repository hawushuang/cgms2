package com.microtechmd.pda.ui.activity;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.DataStorage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.ParameterSystem;
import com.microtechmd.pda.library.entity.ValueInt;
import com.microtechmd.pda.library.entity.ValueShort;
import com.microtechmd.pda.library.entity.comm.RFAddress;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.entity.monitor.StatusPump;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.LogPDA;
import com.microtechmd.pda.ui.activity.fragment.FragmentDialog;
import com.microtechmd.pda.ui.activity.fragment.FragmentInput;
import com.microtechmd.pda.ui.activity.fragment.FragmentMessage;
import com.microtechmd.pda.ui.activity.fragment.FragmentProgress;
import com.microtechmd.pda.ui.widget.WidgetStatusBar;
import com.microtechmd.pda.util.KeyNavigation;
import com.microtechmd.pda.util.MediaUtil;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Stack;
import java.util.TimeZone;


public class ActivityPDA extends AppCompatActivity
        implements
        KeyNavigation.OnClickViewListener {
    public static final int GLUCOSE_UNIT_MMOL = 0;
    public static final int GLUCOSE_UNIT_MG = 1;
    public static final int COUNT_GLUCOSE_UNIT = 2;
    public static final int GLUCOSE_UNIT_MG_STEP = 10;
    public static final int GLUCOSE_UNIT_MMOL_STEP = 18;
    public static final String SETTING_GLUCOSE_UNIT = "glucose_unit";
    protected static final String SETTING_STATUS_BAR = "status_bar";

    private static final int GLUCOSE_EVENT_HYPO = 2;
    private static final int GLUCOSE_EVENT_HYPER = 3;
    private static final int GLUCOSE_EVENT_SENSOR_ERROR = 4;
    private static final int GLUCOSE_EVENT_EXPIRATION = 5;
    private static final int GLUCOSE_EVENT_NEW_SENSOR = 6;
    public static final String SETTING_TIME_FORMAT = "setting_time_format";
    public static final String SETTING_RF_ADDRESS = "setting_rf_address";
    public static int YEAR_MIN = 2017;

    protected ActivityPDA mBaseActivity;
    protected LayoutInflater mLayoutInflater;
    protected boolean mLandscape;

    private long mToastLastShowTime;
    private String mToastLastShowString;


    private static boolean sIsPowerdown = false;
    private static boolean sIsPDABatteryLow = false;
    private static boolean sIsPDABatteryCharging = false;
    private static PowerManager.WakeLock sWakeLock = null;
    private static WidgetStatusBar sStatusBar = null;

    protected LogPDA mLog = null;
    protected MessageListener mMessageListener = null;

    private boolean mIsForeground = false;
    private DataStorage mDataStorage = null;
    private KeyNavigation mKeyNavigation = null;
    private BroadcastReceiver mBroadcastReceiver = null;
    private Handler mHandlerBrightness = null;
    private Runnable mRunnableBrightness = null;
    private Stack<Window> mScreenWindowStack = null;

    private FragmentDialog mFragmentDialog = null;
    private FragmentMessage mFragmentAlarm = null;
    private FragmentProgress mFragmentProgress = null;

    private int mDialogLoadingCount = 0;


    protected class MessageListener
            implements
            EntityMessage.Listener {
        @Override
        public void onReceive(EntityMessage message) {
            mLog.Debug(getClass(),
                    "Handle message: " + "Source Address:" +
                            message.getSourceAddress() + " Target Address:" +
                            message.getTargetAddress() + " Source Port:" +
                            message.getSourcePort() + " Target Port:" +
                            message.getTargetPort() + " Operation:" +
                            message.getOperation() + " Parameter:" +
                            message.getParameter());

            switch (message.getOperation()) {
                case EntityMessage.OPERATION_SET:
                    setParameter(message);
                    break;

                case EntityMessage.OPERATION_GET:
                    getParameter(message);
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
    }


    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(R.layout.activity_pda);

        ViewStub viewStub = (ViewStub) findViewById(R.id.stub_activity);

        if (viewStub != null) {
            viewStub.setLayoutResource(layoutResID);
            viewStub.inflate();
            resetKeyNavigation();
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            updateScreenBrightness();
        }

        return super.dispatchTouchEvent(ev);
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            updateScreenBrightness();
        }

        return super.dispatchKeyEvent(event);
    }


    @Override
    public void onAttachedToWindow() {
        this.getWindow()
                .setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        super.onAttachedToWindow();
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            updateScreenBrightness();
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                return true;

            case KeyEvent.KEYCODE_POWER:
                return showDialogPower();

            default:
                return super.onKeyDown(keyCode, event);
        }
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return mKeyNavigation.onKeyNext();

            case KeyEvent.KEYCODE_HOME:
                onHomePressed();
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
                return mKeyNavigation.onKeyPrevious();

            case ApplicationPDA.KEY_CODE_BOLUS:
                if (mLandscape)
                    return true;
                return mKeyNavigation.onKeyConfirm();

            case KeyEvent.KEYCODE_POWER:
                return true;

            default:
                return super.onKeyUp(keyCode, event);
        }
    }


    @Override
    public void onClick(View v) {
        onClickView(v);
    }


    public void handleMessage(final EntityMessage message) {
        if (message.getTargetAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER) {
            if (message.getOperation() == EntityMessage.OPERATION_SET) {
                showDialogProgress();
            }
        }

        ((ApplicationPDA) getApplication()).handleMessage(message);
    }


    public void updateScreenBrightness() {
        final int SCREEN_BRIGHTNESS_MAX = 255;
        final int SCREEN_BRIGHTNESS_MIN = 15;

        int reduceBrightnessCycle = 0;


        if (reduceBrightnessCycle > 0) {
            if (mHandlerBrightness == null) {
                mHandlerBrightness = new Handler();
            }

            if (mRunnableBrightness != null) {
                mHandlerBrightness.removeCallbacks(mRunnableBrightness);
            }

            mRunnableBrightness = new Runnable() {

                @Override
                public void run() {
                    if ((mScreenWindowStack != null) &&
                            (!mScreenWindowStack.isEmpty())) {
                        // Set screen brightness to minimum
                        WindowManager.LayoutParams layoutParams =
                                mScreenWindowStack.peek().getAttributes();
                        layoutParams.screenBrightness =
                                (float) SCREEN_BRIGHTNESS_MIN /
                                        (float) SCREEN_BRIGHTNESS_MAX;
                        mScreenWindowStack.peek().setAttributes(layoutParams);
                    }
                }
            };

            mHandlerBrightness.postDelayed(mRunnableBrightness,
                    reduceBrightnessCycle);
        }

        if ((mScreenWindowStack != null) && (!mScreenWindowStack.isEmpty())) {
            // Restore screen brightness to system setting
            WindowManager.LayoutParams layoutParams =
                    mScreenWindowStack.peek().getAttributes();
            layoutParams.screenBrightness =
                    (float) Settings.System.getInt(getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS, SCREEN_BRIGHTNESS_MAX) /
                            (float) SCREEN_BRIGHTNESS_MAX;
            mScreenWindowStack.peek().setAttributes(layoutParams);
        }
    }


    public void pushScreenWindow(final Window window) {
        if (mScreenWindowStack == null) {
            mScreenWindowStack = new Stack<Window>();
        }

        mScreenWindowStack.push(window);
    }


    public void popScreenWindow() {
        if (mScreenWindowStack == null) {
            mScreenWindowStack = new Stack<Window>();
        }

        if (!mScreenWindowStack.isEmpty()) {
            mScreenWindowStack.pop();
        }
    }


    public DataStorage getDataStorage(String name) {
        if (name == null) {
            name = getClass().getSimpleName();
        }

        if (mDataStorage == null) {
            mDataStorage = new DataStorage(this, name);
        }

        if (!mDataStorage.getName().equals(name)) {
            mDataStorage = new DataStorage(this, name);
        }

        return mDataStorage;
    }


    public int getGlucoseUnit() {
        return getDataStorage(ActivityPDA.class.getSimpleName())
                .getInt(SETTING_GLUCOSE_UNIT, GLUCOSE_UNIT_MMOL);
    }


    public String getGlucoseValue(int value, boolean unit) {
        String result;


        if (getGlucoseUnit() == GLUCOSE_UNIT_MMOL) {
            DecimalFormat decimalFormat = new DecimalFormat("0.00");
            result = decimalFormat
                    .format((double) value / (double) (GLUCOSE_UNIT_MG_STEP * 100));

            if (unit) {
                result += getResources().getString(R.string.unit_mmol_l);
            }
        } else {
            result =
                    ((value + GLUCOSE_UNIT_MG_STEP - 1) / GLUCOSE_UNIT_MG_STEP) +
                            "";

            if (unit) {
                result += getResources().getString(R.string.unit_mg_dl);
            }
        }

        return result;
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
        String template;

        if (getDataStorage(ActivityPDA.class.getSimpleName())
                .getBoolean(SETTING_TIME_FORMAT, true)) {
            template = "H:mm";
        } else {
            template = "h:mm a";
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(template);

        if (timeZone != null) {
            simpleDateFormat.setTimeZone(timeZone);
        }

        return simpleDateFormat.format(new Date(dateTime));
    }


    public WidgetStatusBar getStatusBar() {
        return sStatusBar;
    }


    public void resetKeyNavigation() {
        mKeyNavigation.resetNavigation(getWindow().getDecorView());
    }


    public FragmentDialog showDialogConfirm(String title,
                                            String buttonTextPositive, String buttonTextNegative, Fragment content,
                                            boolean isCancelable, FragmentDialog.ListenerDialog listener) {
        FragmentDialog fragmentDialog = new FragmentDialog();
        fragmentDialog.setTitle(title);
        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
                buttonTextPositive);
        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
                buttonTextNegative);
        fragmentDialog.setContent(content);
        fragmentDialog.setCancelable(isCancelable);
        fragmentDialog.setListener(listener);
        fragmentDialog.show(getSupportFragmentManager(), null);

        return fragmentDialog;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLog = new LogPDA();
        mKeyNavigation = new KeyNavigation(this, this);
        mMessageListener = new MessageListener();
        ((ApplicationPDA) getApplication()).registerMessageListener(
                ParameterGlobal.PORT_COMM, mMessageListener);
        ((ApplicationPDA) getApplication()).registerMessageListener(
                ParameterGlobal.PORT_MONITOR, mMessageListener);

        if (sWakeLock == null) {
            PowerManager powerManager =
                    (PowerManager) getSystemService(Context.POWER_SERVICE);
            sWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getSimpleName());
        }

        if (sStatusBar == null) {
            sStatusBar = new WidgetStatusBar();
            sStatusBar
                    .setByteArray(getDataStorage(ActivityPDA.class.getSimpleName())
                            .getExtras(SETTING_STATUS_BAR, null));

            final int reaction;

            if (sStatusBar.getAlarm() != null) {
                reaction = ParameterSystem.REACTION_ALARM;
            } else if (sStatusBar.getAlertList().size() > 0) {
                reaction = ParameterSystem.REACTION_ALERT;
            } else {
                reaction = ParameterSystem.REACTION_NORMAL;
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    triggerReaction(reaction);
                }
            }, 2000);
        }

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                    onBatteryChanged(
                            intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0),
                            intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1));
                } else if (action.equals(Intent.ACTION_TIME_TICK)) {
                    onTimeTick();
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        registerReceiver(mBroadcastReceiver, intentFilter);
        pushScreenWindow(getWindow());

        mBaseActivity = this;
        mToastLastShowTime = 0;
        mLayoutInflater =
                (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    protected void onResume() {
        super.onResume();

        getStatusBar().setView(getWindow().getDecorView());
        ArrayList<History> alertList = getStatusBar().getAlertList();

        if (getStatusBar().getAlarm() != null) {
            showDialogAlarm(getStatusBar().getAlarm());
        } else if (alertList.size() > 0) {
            showDialogAlarm(alertList.get(alertList.size() - 1));
        }

        onTimeTick();

        mIsForeground = true;
    }


    @Override
    protected void onPause() {
        super.onPause();

        mIsForeground = false;
    }


    @Override
    protected void onDestroy() {
        mDialogLoadingCount = 0;
        ((ApplicationPDA) getApplication()).unregisterMessageListener(
                ParameterGlobal.PORT_COMM, mMessageListener);
        ((ApplicationPDA) getApplication()).unregisterMessageListener(
                ParameterGlobal.PORT_MONITOR, mMessageListener);
        unregisterReceiver(mBroadcastReceiver);
        dismissDialogProgress();
        popScreenWindow();

        super.onDestroy();
    }


    protected void onBatteryChanged(int level, final int status) {
        final int BATTERY_LEVEL_LOW = 5;
        final int BATTERY_LEVEL_RECOVER = 10;
        final int BATTERY_UPDATE_CYCLE = 1000;


        if ((status == BatteryManager.BATTERY_STATUS_CHARGING) ||
                (status == BatteryManager.BATTERY_STATUS_FULL)) {
            if (!sIsPDABatteryCharging) {
                sIsPDABatteryCharging = true;
                final Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        WidgetStatusBar statusBar = getStatusBar();

                        if (sIsPDABatteryCharging) {
                            statusBar.setPDACharger(!statusBar.getPDACharger());
                            handler.postDelayed(this, BATTERY_UPDATE_CYCLE);
                        } else {
                            statusBar.setPDACharger(false);
                        }
                    }
                });
            }
        } else {
            if (sIsPDABatteryCharging) {
                sIsPDABatteryCharging = false;
                getStatusBar().setPDACharger(false);
            }
        }

        getStatusBar().setPDABattery(level);

        if ((!sIsPDABatteryLow) && (level < BATTERY_LEVEL_LOW)) {
            sIsPDABatteryLow = true;

            final Event event = new Event(0, ParameterGlobal.PORT_MONITOR,
                    ParameterMonitor.EVENT_PDA_BATTERY, Event.URGENCY_ALERT, 0);
            final History history = new History(
                    new DateTime(Calendar.getInstance()), new Status(), event);
            notifyEventAlert(history);
        }

        if ((sIsPDABatteryLow) && (level > BATTERY_LEVEL_RECOVER)) {
            sIsPDABatteryLow = false;
        }
    }


    protected void onTimeTick() {
        getStatusBar().setDateTime(System.currentTimeMillis(),
                getDataStorage(ActivityPDA.class.getSimpleName())
                        .getBoolean(SETTING_TIME_FORMAT, true));
    }


    protected void onHomePressed() {
        Intent intent = new Intent(this, ActivityMain.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }


    protected void onClickView(View v) {
    }


    protected void setStatusButtonVisibility(boolean isVisible) {
        Button button = (Button) findViewById(R.id.btn_status_down);

        if (isVisible) {
            button.setVisibility(View.VISIBLE);
        } else {
            button.setVisibility(View.INVISIBLE);
        }
    }


    public void setProgressContent(final String content) {
        if (mFragmentProgress == null) {
            mFragmentProgress = new FragmentProgress();
        }

        mFragmentProgress.setComment(content);

        if (mDialogLoadingCount <= 0) {
            showDialogProgress();
        }
    }


    public void showDialogProgress() {
        mLog.Debug(getClass(), "Show progress dialog");

        if (mFragmentProgress == null) {
            mFragmentProgress = new FragmentProgress();
            mFragmentProgress.setComment(getString(R.string.connecting));
        }

        if (mFragmentDialog == null) {
            mFragmentDialog = new FragmentDialog();
            mFragmentDialog.setBottom(false);
            mFragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
                    null);
            mFragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
                    null);
            mFragmentDialog.setContent(mFragmentProgress);
            mFragmentDialog.setCancelable(false);
        }

        if (mDialogLoadingCount <= 0) {
            mFragmentDialog.show(getSupportFragmentManager(), null);
        }

        mDialogLoadingCount++;
    }


    public void dismissDialogProgress() {
        mLog.Debug(getClass(), "Dismiss progress dialog");

        if (mDialogLoadingCount > 0) {
            mDialogLoadingCount--;
        }

        if (mDialogLoadingCount <= 0) {
            mDialogLoadingCount = 0;

            if (mFragmentProgress != null) {
                mFragmentProgress.setComment(getString(R.string.connecting));
            }

            if (mFragmentDialog != null) {
                mFragmentDialog.dismissAllowingStateLoss();
            }
        }
    }


    protected void setParameter(final EntityMessage message) {
        mLog.Debug(getClass(), "Set Parameter: " + message.getParameter());
    }


    protected void getParameter(final EntityMessage message) {
        mLog.Debug(getClass(), "Get Parameter: " + message.getParameter());
    }


    protected void handleEvent(final EntityMessage message) {
        mLog.Debug(getClass(), "Handle Event: " + message.getEvent());

        if (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER) {
            if (message.getEvent() == EntityMessage.EVENT_TIMEOUT) {
                dismissDialogProgress();

                Toast.makeText(this,
                        getResources().getString(R.string.connect_fail),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }


    protected void handleNotification(EntityMessage message) {
        mLog.Debug(getClass(), "Notify Port: " + message.getSourcePort() +
                " Parameter: " + message.getParameter());

        if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
                (message.getParameter() == ParameterComm.PARAM_RF_SIGNAL)) {
            getStatusBar().setRFSignal((int) message.getData()[0]);
        }

        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
                (message.getParameter() == ParameterMonitor.PARAM_STATUS)) {
            final StatusPump status = new StatusPump(message.getData());
            getStatusBar().setPumpBattery(status.getBatteryCapacity());
            getStatusBar().setPumpReservoir(status.getReservoirAmount());
        }

        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
                (message.getParameter() == ParameterMonitor.PARAM_HISTORY)) {
            if (message
                    .getSourceAddress() != ParameterGlobal.ADDRESS_LOCAL_MODEL) {
                if (mIsForeground || hasWindowFocus() ||
                        (mFragmentAlarm != null)) {
                    final History history = new History(message.getData());
                    final Event event = history.getEvent();

                    mLog.Debug(getClass(),
                            "Notify event: " + getEventContent(event));

                    if (event.getUrgency() == Event.URGENCY_ALERT) {
                        notifyEventAlert(history);
                    } else if (event.getUrgency() == Event.URGENCY_ALARM) {
                        notifyEventAlarm(history);
                    }
                    switch (event.getEvent()) {
                        case GLUCOSE_EVENT_NEW_SENSOR:
                            FragmentInput fragmentInput = new FragmentInput();
                            fragmentInput
                                    .setComment(getString(R.string.alarm_new_sensor));
                            showDialogConfirm("", "",
                                    "", fragmentInput, false, new FragmentDialog.ListenerDialog() {
                                        @Override
                                        public boolean onButtonClick(int buttonID, Fragment content) {
                                            switch (buttonID) {
                                                case FragmentDialog.BUTTON_ID_POSITIVE:
                                                    handleMessage(new EntityMessage(
                                                            ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                            ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                                            ParameterGlobal.PORT_GLUCOSE,
                                                            ParameterGlobal.PORT_GLUCOSE,
                                                            EntityMessage.OPERATION_SET,
                                                            ParameterGlucose.PARAM_NEW_SENSOR,
                                                            new ValueShort((short) 0)
                                                                    .getByteArray()));
                                                    break;
                                                case FragmentDialog.BUTTON_ID_NEGATIVE:
                                                    handleMessage(new EntityMessage(
                                                            ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                            ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                                            ParameterGlobal.PORT_GLUCOSE,
                                                            ParameterGlobal.PORT_GLUCOSE,
                                                            EntityMessage.OPERATION_SET,
                                                            ParameterGlucose.PARAM_NEW_SENSOR,
                                                            new ValueShort((short) 1)
                                                                    .getByteArray()));
                                                    break;
                                                default:
                                                    break;
                                            }
                                            return true;
                                        }
                                    });
                            break;
                    }
                }
            }
        }
    }


    protected void handleAcknowledgement(final EntityMessage message) {
        mLog.Debug(getClass(), "Acknowledge Port: " + message.getSourcePort() +
                " Parameter: " + message.getParameter());

        if (message.getData()[0] == EntityMessage.FUNCTION_OK) {
            mLog.Debug(getClass(), "Acknowledge OK");
        } else {
            mLog.Debug(getClass(), "Acknowledge Fail");

            Toast
                    .makeText(this, getResources().getString(R.string.connect_fail),
                            Toast.LENGTH_SHORT)
                    .show();
        }

        if (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER) {
            dismissDialogProgress();
        }
    }


    protected String getEventContent(final Event event) {
        String content = "";

        if (event.getUrgency() == Event.URGENCY_ALARM) {
            if (event.getPort() == ParameterGlobal.PORT_GLUCOSE) {
                switch (event.getEvent()) {
                    case GLUCOSE_EVENT_HYPO:
                        content = getString(R.string.alarm_hypo);
                        break;

                    default:
                        break;
                }
            }
        }

        if (event.getUrgency() == Event.URGENCY_ALERT) {
            if (event.getPort() == ParameterGlobal.PORT_GLUCOSE) {
                switch (event.getEvent()) {
                    case GLUCOSE_EVENT_HYPER:
                        content = getString(R.string.alarm_hyper);
                        break;

                    case GLUCOSE_EVENT_SENSOR_ERROR:
                        content = getString(R.string.alarm_sensor_error);
                        break;

                    case GLUCOSE_EVENT_EXPIRATION:
                        content = getString(R.string.alarm_expiration);
                        break;

                    default:
                        break;
                }
            }

            if (event.getPort() == ParameterGlobal.PORT_MONITOR) {
                switch (event.getEvent()) {
                    case ParameterMonitor.EVENT_PDA_BATTERY:
                        content = getString(R.string.alarm_pda_battery);
                        break;

                    case ParameterMonitor.EVENT_PDA_ERROR:
                        content = getString(R.string.alarm_pda_error);
                        break;

                    default:
                        break;
                }
            }
        }

        return content;
    }


    private static synchronized void acquireWakeLock() {
        if (sWakeLock.isHeld() == false) {
            sWakeLock.acquire();
        }
    }


    private static synchronized void releaseWakeLock() {
        if (sWakeLock.isHeld() == true) {
            sWakeLock.release();
        }
    }


    private void notifyEventAlert(final History history) {
        ArrayList<History> alertList = getStatusBar().getAlertList();

        int i;
        final Event event = history.getEvent();

        for (i = 0; i < alertList.size(); i++) {
            final Event alertEvent = alertList.get(i).getEvent();

            // Update event if there is the same event in alert list
            if ((alertEvent.getPort() == event.getPort()) &&
                    (alertEvent.getEvent() == event.getEvent())) {
                alertList.set(i, history);
                break;
            }
        }

        // Add event to alert list if it's new alert
        if (i >= alertList.size()) {
            alertList.add(history);
        }

        getStatusBar().setAlertList(alertList);

        getDataStorage(ActivityPDA.class.getSimpleName())
                .setExtras(SETTING_STATUS_BAR, getStatusBar().getByteArray());

        if (getStatusBar().getAlarm() == null) {
            triggerReaction(ParameterSystem.REACTION_ALERT);
            showDialogAlarm(history);
        }
    }


    private void notifyEventAlarm(final History history) {
        if (getStatusBar().getAlarm() == null) {
            acquireWakeLock();
            triggerReaction(ParameterSystem.REACTION_ALARM);
        }

        getStatusBar().setAlarm(history);
        getDataStorage(ActivityPDA.class.getSimpleName())
                .setExtras(SETTING_STATUS_BAR, getStatusBar().getByteArray());
        showDialogAlarm(history);

        int i;
        final Event event = history.getEvent();

        final ArrayList<History> alertList = getStatusBar().getAlertList();

        // Clear alert with the same event type in alert list
        for (i = 0; i < alertList.size(); i++) {
            final Event alertEvent = alertList.get(i).getEvent();

            if ((alertEvent.getPort() == event.getPort()) &&
                    (alertEvent.getEvent() == event.getEvent()))

            {
                alertList.remove(i);
                getStatusBar().setAlertList(alertList);
                break;
            }
        }

        getDataStorage(ActivityPDA.class.getSimpleName())
                .setExtras(SETTING_STATUS_BAR, getStatusBar().getByteArray());
    }


    private boolean showDialogPower() {
        if (sIsPowerdown) {
            return false;
        }

        sIsPowerdown = true;
        FragmentMessage fragmentMessage = new FragmentMessage();
        fragmentMessage.setComment(getString(R.string.shutdown_content));
        fragmentMessage.setIcon(false);
        showDialogConfirm(getString(R.string.shutdown_title), "", "",
                fragmentMessage, false, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        if (buttonID == FragmentDialog.BUTTON_ID_POSITIVE) {
                            showToast(R.string.Toast_shutdown);

                            try {
                                Intent shutdown = new Intent(
                                        "android.intent.action.ACTION_REQUEST_SHUTDOWN");
                                startActivity(shutdown);
                            } catch (Exception e) {
                                showToast("Shut Down failed.", 1000);
                            }
                        } else if (buttonID == FragmentDialog.BUTTON_ID_NEGATIVE) {
                            sIsPowerdown = false;
                        }

                        return true;
                    }
                });

        return true;
    }


    private void showDialogAlarm(final History history) {
        if (mFragmentAlarm == null) {
            mFragmentAlarm = new FragmentMessage();
            mFragmentAlarm.setComment(getEventContent(history.getEvent()));
            showDialogConfirm(getString(R.string.alarm_dialog_title), "", null,
                    mFragmentAlarm, false, new FragmentDialog.ListenerDialog() {
                        @Override
                        public boolean onButtonClick(int buttonID, Fragment content) {
                            switch (buttonID) {
                                case FragmentDialog.BUTTON_ID_POSITIVE:
                                    return confirmAlarm(history);

                                default:
                                    break;
                            }

                            return true;
                        }
                    });
        } else {
            mFragmentAlarm.setComment(getEventContent(history.getEvent()));
        }
    }


    private boolean confirmAlarm(final History history) {
        ArrayList<History> alertList = getStatusBar().getAlertList();

        if (getStatusBar().getAlarm() != null) {
            getStatusBar().setAlarm(null);
            releaseWakeLock();
        } else {
            if (alertList.size() > 0) {
                alertList.remove(alertList.size() - 1);

                getStatusBar().setAlertList(alertList);
            }
        }

        getDataStorage(ActivityPDA.class.getSimpleName())
                .setExtras(SETTING_STATUS_BAR, getStatusBar().getByteArray());

        if (alertList.size() == 0) {
            triggerReaction(ParameterSystem.REACTION_NORMAL);
            mFragmentAlarm = null;
            return true;
        } else {
            triggerReaction(ParameterSystem.REACTION_ALERT);
            showDialogAlarm(alertList.get(alertList.size() - 1));
            return false;
        }
    }


    private void triggerReaction(int reaction) {
        final int BEEP_ALARM_CYCLE = 28000;


        switch (reaction) {
            case ParameterSystem.REACTION_ALARM:
                MediaUtil.playMp3ByType(ActivityPDA.this, "beep_alarm.mp3",
                        false);

                final Handler handlerSound = new Handler();

                handlerSound.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (getStatusBar().getAlarm() != null) {
                            handlerSound.postDelayed(this, BEEP_ALARM_CYCLE);
                            MediaUtil.playMp3ByType(ActivityPDA.this,
                                    "beep_alarm.mp3", false);
                        } else {
                            handlerSound.removeCallbacks(this);
                        }
                    }
                }, BEEP_ALARM_CYCLE);

                break;

            case ParameterSystem.REACTION_ALERT:
                MediaUtil.playMp3ByType(this, "beep_alert.mp3", false);
                break;

            case ParameterSystem.REACTION_NORMAL:
                break;

            default:
                return;
        }

        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_LOCAL_CONTROL, ParameterGlobal.PORT_SYSTEM,
                ParameterGlobal.PORT_SYSTEM, EntityMessage.OPERATION_SET,
                ParameterSystem.PARAM_REACTION,
                new ValueInt(reaction).getByteArray()));
    }


    protected void showToast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }


    protected void showToast(String txt, int interval) {

        if (mToastLastShowString != txt) {
            mToastLastShowString = txt;
            mToastLastShowTime = 0;
        }

        long NowTime = System.currentTimeMillis();
        if (NowTime - mToastLastShowTime > interval) {
            mToastLastShowTime = NowTime;
            Toast.makeText(this, txt, Toast.LENGTH_SHORT).show();
        }
    }
}