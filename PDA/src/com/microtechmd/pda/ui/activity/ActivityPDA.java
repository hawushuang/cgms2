package com.microtechmd.pda.ui.activity;


import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Toast;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.database.DbHistory;
import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.DataStorage;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.ParameterSystem;
import com.microtechmd.pda.library.entity.ValueInt;
import com.microtechmd.pda.library.entity.comm.RFAddress;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.LogPDA;
import com.microtechmd.pda.library.utility.SPUtils;
import com.microtechmd.pda.manager.ActivityPathManager;
import com.microtechmd.pda.manager.ActivityStackManager;
import com.microtechmd.pda.ui.activity.fragment.FragmentDialog;
import com.microtechmd.pda.ui.activity.fragment.FragmentInput;
import com.microtechmd.pda.ui.activity.fragment.FragmentMessage;
import com.microtechmd.pda.ui.activity.fragment.FragmentNewProgress;
import com.microtechmd.pda.ui.widget.WidgetStatusBar;
import com.microtechmd.pda.util.KeyNavigation;
import com.microtechmd.pda.util.MediaUtil;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Stack;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingUtilities.setCustomDialogStyle;


public class ActivityPDA extends AppCompatActivity
        implements
        KeyNavigation.OnClickViewListener {
    private Context context;
    private int[] alarmHour = {7, 9, 11, 13, 17, 19, 21};
    private int[] alarmMin = {30, 0, 30, 0, 0, 0, 0};
    //    private int[] alarmHour = {9, 9, 9, 9, 9, 9, 9};
//    private int[] alarmMin = {33, 34, 35, 36, 37, 38, 39};
    public static final int GLUCOSE_UNIT_MMOL = 0;
    public static final int GLUCOSE_UNIT_MG = 1;
    public static final int COUNT_GLUCOSE_UNIT = 2;
    public static final int GLUCOSE_UNIT_MG_STEP = 10;
    public static final int GLUCOSE_UNIT_MMOL_STEP = 18;
    public static final String SETTING_GLUCOSE_UNIT = "glucose_unit";
    public static final String CALIBRATION_HISTORY = "calibration_history";
    public static final String HIMESSAGETIPS = "hi_messagetips";
    public static final String LOMESSAGETIPS = "low_messagetips";
    public static final String COMMMESSAGETIPS = "comm_messagetips";
    public static final String GLUCOSEMESSAGETIPS = "glucose_messagetips";
    protected static final String SETTING_STATUS_BAR = "status_bar";
    public static final String COMM_CLOSE = "comm_close";

    public static final String DATE_CHANGE = "date_change";

    // 定义广播Action
    private static final String GLUCOSE_ALARM_ACTION = "app.action.GLUCOSE_ALARM_ACTION";
    private static final int INVALID = -1;
    private static final int TRANSMITTER_STARTUP = 0;
    public static final int TRANSMITTER_ERROR = 1;
    private static final int BATTERY_LOW = 2;
    private static final int BATTERY_EXHAUSTED = 3;
    public static final int SENSOR_NEW = 4;
    public static final int SENSOR_ERROR = 5;
    public static final int SENSOR_EXPIRATION = 6;
    public static final int GLUCOSE = 7;
    public static final int GLUCOSE_RECOMMEND_CAL = 8;
    public static final int GLUCOSE_INVALID = 9;
    public static final int HYPO = 10;
    public static final int HYPER = 11;
    public static final int IMPENDANCE = 12;
    public static final int BLOOD_GLUCOSE = 13;
    public static final int CALIBRATION = 14;
    private static final int PDA_BATTERY_LOW = 21;
    public static final int PDA_ERROR = 22;
    private static final int PDA_COMM_ERROR = 23;
    private static final int GLUCOSE_CHECK = 24;


    private static final int COMM_ERR_TIMEOUT = 20 * 1000;
    private static final int NOSIGNAL_TIMEOUT = 40 * 1000;
    public static final String SETTING_TIME_FORMAT = "setting_time_format";
    public static final String SETTING_RF_ADDRESS = "setting_rf_address";
    public static final String GET_RF_MAC_ADDRESS = "get_rf_mac_address";
    public static final String IS_PAIRED = "isPaired";
    public static final String RFSIGNAL = "rfsignal";
    public static int YEAR_MIN = 2019;

    protected ActivityPDA mBaseActivity;
    protected LayoutInflater mLayoutInflater;
    protected boolean mLandscape;

    protected boolean commErrorFlag;

    private long mToastLastShowTime;
    private String mToastLastShowString;


    private static boolean sIsPowerdown = false;
    private static boolean sIsPDABatteryLow = false;
    private static boolean sIsPDABatteryCharging = false;
    protected static PowerManager.WakeLock sWakeLock = null;
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
    //    private FragmentProgress mFragmentProgress = null;
    private FragmentNewProgress mFragmentProgress = null;

    private int mDialogLoadingCount = 0;
    private boolean newSensorFlag = true;

    private FragmentDialog powerDownFramentDialog;

    private FragmentDialog newSensorFragmentDialog;
    private FragmentDialog commErrFragmentDialog;
    private FragmentDialog pdaLowBatteryFragmentDialog;
    private FragmentDialog pdaErrFragmentDialog;

    private FragmentDialog sensorErrFragmentDialog;
    private FragmentDialog sensorExpirationFragmentDialog;
    private FragmentDialog lowGlucoseFragmentDialog;
    private FragmentDialog highGlucoseFragmentDialog;
    private FragmentDialog glucoseCheckFragmentDialog;

    private boolean newFlag = false;
    private static final int COMM_ERR_TYPE = 101;
    private static final int PAD_LOWBATTERY_ERR_TYPE = 102;
    private static final int PDA_ERR_TYPE = 103;

    private String addTime;
    private CountDownTimer noSignaltimer;
    private CountDownTimer conn_timer;

    public String getAddTime() {
        return addTime;
    }

    public void setAddTime(String addTime) {
        this.addTime = addTime;
    }


    private CountDownTimer comm_countDownTimer;

    private ArrayList<DbHistory> dataErrListAll = null;
    private ApplicationPDA applicationPDA;

    private long noSignalTag;
    private int signal = 0;
    @SuppressLint("HandlerLeak")
    private Handler signalHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                if (signal > 5) {
                    signal -= 5;
                } else {
                    signal = 0;
                }
                getStatusBar().setRFSignal(signal);
                SPUtils.put(ActivityPDA.this, RFSIGNAL, signal);
            }

        }
    };
    private List<Integer> batteryList;
    private int batteryMode = 0;
    private Timer signalTimer;

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


//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
//            updateScreenBrightness();
//        }
//
//        return super.dispatchTouchEvent(ev);
//    }
//
//
//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//        if (event.getAction() == KeyEvent.ACTION_UP) {
//            updateScreenBrightness();
//        }
//
//        return super.dispatchKeyEvent(event);
//    }


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
                onVolumeDownPressed();
                return true;

            case KeyEvent.KEYCODE_HOME:
                onHomePressed();
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
                onVolumeUpPressed();
                return true;

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
        if (message.getParameter() == ParameterComm.UNPAIRNOSIGNAL) {
            signal = 0;
            getStatusBar().setRFSignal(0);
            return;
        }
        if (message.getTargetAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER) {
            if (message.getOperation() == EntityMessage.OPERATION_SET) {
                showDialogProgress();
            }
        }

        if (conn_timer == null) {
            conn_timer = new CountDownTimer(25 * 1000, 25 * 1000) {
                @Override
                public void onTick(long l) {
                }

                @Override
                public void onFinish() {
                    dismissDialogProgress();
                }
            }.start();
        } else {
            conn_timer.cancel();
            conn_timer.start();
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
            DecimalFormat decimalFormat = new DecimalFormat("0.0");
            result = decimalFormat
                    .format((double) value / (double) (GLUCOSE_UNIT_MG_STEP * 10));

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

    private FragmentDialog showNewSensorDialogConfirm(String title,
                                                      String buttonTextPositive, String buttonTextNegative, Fragment content,
                                                      boolean isCancelable, FragmentDialog.ListenerDialog listener) {
        newSensorFragmentDialog = new FragmentDialog();
        newSensorFragmentDialog.setTitle(title);
        newSensorFragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
                buttonTextPositive);
        newSensorFragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
                buttonTextNegative);
        newSensorFragmentDialog.setContent(content);
        newSensorFragmentDialog.setCancelable(isCancelable);
        newSensorFragmentDialog.setListener(listener);
        newSensorFragmentDialog.show(getSupportFragmentManager(), null);

        return newSensorFragmentDialog;
    }

    private FragmentDialog showErrConfirm(int errType, String title,
                                          String buttonTextPositive, String buttonTextNegative,
                                          Fragment content, FragmentDialog.ListenerDialog listener) {
        FragmentDialog errFragmentDialog;
        switch (errType) {
            case COMM_ERR_TYPE:
                commErrFragmentDialog = new FragmentDialog();
                errFragmentDialog = commErrFragmentDialog;
                break;
            case PAD_LOWBATTERY_ERR_TYPE:
                pdaLowBatteryFragmentDialog = new FragmentDialog();
                errFragmentDialog = pdaLowBatteryFragmentDialog;
                break;
            case PDA_ERR_TYPE:
                pdaErrFragmentDialog = new FragmentDialog();
                errFragmentDialog = pdaErrFragmentDialog;
                break;
            default:
                errFragmentDialog = new FragmentDialog();
                break;
        }
        errFragmentDialog.setTitle(title);
        errFragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
                buttonTextPositive);
        errFragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
                buttonTextNegative);
        errFragmentDialog.setContent(content);
        errFragmentDialog.setCancelable(false);
        errFragmentDialog.setListener(listener);
        errFragmentDialog.show(getSupportFragmentManager(), null);

        return errFragmentDialog;
    }

    int i = 0;
    int typ = 10;

    @SuppressLint("ShortAlarm")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        triggerReaction(ParameterSystem.REACTION_NORMAL);
//        setDefaultInputMethod(this);
        setAddTime(String.valueOf(SystemClock.currentThreadTimeMillis()));
        ActivityStackManager.getInstance().addActivity(this);
        context = this;
        mLog = new LogPDA();
        //添加Activity到堆栈
        applicationPDA = (ApplicationPDA) getApplication();
        dataErrListAll = applicationPDA.getDataErrListAll();
        if (dataErrListAll == null) {
            dataErrListAll = new ArrayList<>();
        }
        commErrorFlag = (boolean) SPUtils.get(this, COMM_CLOSE, false);
        mKeyNavigation = new KeyNavigation(this, this);
        mMessageListener = new MessageListener();
        ((ApplicationPDA) getApplication()).registerMessageListener(
                ParameterGlobal.PORT_COMM, mMessageListener);
        ((ApplicationPDA) getApplication()).registerMessageListener(
                ParameterGlobal.PORT_MONITOR, mMessageListener);

        if (sWakeLock == null) {
            PowerManager powerManager =
                    (PowerManager) getSystemService(Context.POWER_SERVICE);
            assert powerManager != null;
            sWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getSimpleName());
        }

        if (sStatusBar == null) {
            sStatusBar = new WidgetStatusBar();
            sStatusBar
                    .setByteArray(getDataStorage(ActivityPDA.class.getSimpleName())
                            .getExtras(SETTING_STATUS_BAR, null));

//            final int reaction;
//
//            if (sStatusBar.getAlarm() != null) {
//                reaction = ParameterSystem.REACTION_ALARM;
//            } else if (sStatusBar.getAlertList().size() > 0) {
//                reaction = ParameterSystem.REACTION_ALERT;
//            } else {
//                reaction = ParameterSystem.REACTION_NORMAL;
//            }
//
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    triggerReaction(reaction);
//                }
//            }, 2000);
//            new CountDownTimer(10 * 1000, 5 * 1000) {
//
//                @Override
//                public void onTick(long l) {
//                    History h = new History();
//                    Event e = new Event();
//                    if (typ == 12) {
//                        typ = 10;
//                    }
//                    e.setEvent(typ);
//                    typ++;
//                    h.setEvent(e);
//                    alertTips(h);
//                }
//
//                @Override
//                public void onFinish() {
//
//                }
//            }.start();
        }
        batteryList = new ArrayList<>();
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                assert action != null;
                switch (action) {
                    case Intent.ACTION_BATTERY_CHANGED:
                        onBatteryChanged(
                                intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0),
                                intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1));
                        break;
                    case Intent.ACTION_POWER_DISCONNECTED:
                        sIsPDABatteryCharging = false;
                        getStatusBar().setPDACharger(false);
                        break;
                    case Intent.ACTION_TIME_TICK:
                        onTimeTick();
                        //刷新血糖时间
                        handleMessage(
                                new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                        ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                        ParameterGlobal.PORT_MONITOR,
                                        ParameterGlobal.PORT_MONITOR,
                                        EntityMessage.OPERATION_SET,
                                        ParameterMonitor.GLUCOSE_DISPLAY,
                                        null));
                        //计算是否断线15分钟
//                        mLog.Error(ActivityPDA.this.getClass(), "时钟改变：noSignalTag" + noSignalTag);
//                        if (noSignalTag != 0) {
//                            long space = System.currentTimeMillis() - noSignalTag;
//                            int minute = (int) (space / 60000);
//                            mLog.Error(ActivityPDA.this.getClass(), "断线时间：" + minute);
//                            if (minute == 14 || minute == 15 || minute == 16) {
//                                noSignalTag = 0;
//                                Event event = new Event(0, PDA_COMM_ERROR, 0);
//                                History history = new History(
//                                        new DateTime(Calendar.getInstance()), new Status(), event);
//                                if (commErrorFlag) {
//                                    boolean comm_messageFlag = (boolean) SPUtils.get(context, COMMMESSAGETIPS, true);
////                                    if (mIsForeground || hasWindowFocus()) {
//                                    if (comm_messageFlag) {
//                                        notifyErrEventAlert(history, COMM_ERR_TYPE);
//                                    }
////                                    }
//
//                                }
//                            }
//                        }
                        break;
                    case Intent.ACTION_SCREEN_ON:
                        onScreenOn();
                        break;
                    case Intent.ACTION_SCREEN_OFF:
                        onScreenOff();
                        break;
                    case GLUCOSE_ALARM_ACTION:
                        boolean alarmclock = (boolean) SPUtils.get(ActivityPDA.this, GLUCOSEMESSAGETIPS, true);
//                        if (alarmclock) {
//                            if (mIsForeground || hasWindowFocus()) {
//                                History history = new History();
//                                Event event = new Event();
//                                event.setEvent(GLUCOSE_CHECK);
//                                history.setEvent(event);
//                                alertTips(history);
//                            }
//                        }
                        break;
                    case "comm_err":
//                        Event event = new Event(0, PDA_COMM_ERROR, 0);
//                        History history = new History(
//                                new DateTime(Calendar.getInstance()), new Status(), event);
//                        if (commErrorFlag) {
//                            boolean comm_messageFlag = (boolean) SPUtils.get(context, COMMMESSAGETIPS, true);
//                            if (mIsForeground || hasWindowFocus()) {
//                                if (comm_messageFlag) {
//                                    notifyErrEventAlert(history, COMM_ERR_TYPE);
//                                }
//                            }
//
//                        }
                        break;
                    case "glucose_display":
                        break;
                    default:
                        break;
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(GLUCOSE_ALARM_ACTION);
        intentFilter.addAction("comm_err");
        intentFilter.addAction("glucose_display");
        registerReceiver(mBroadcastReceiver, intentFilter);
//        for (int j = 0; j < alarmHour.length; j++) {
//            // 实例化Intent
//            Intent intent = new Intent();
//            // 设置Intent action属性
//            intent.setAction(GLUCOSE_ALARM_ACTION);
//            // 实例化PendingIntent
//            PendingIntent sender = PendingIntent.getBroadcast(this, j,
//                    intent, 0);
//            long systemTime = System.currentTimeMillis();
//
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTimeInMillis(System.currentTimeMillis());
//// 这里时区需要设置一下，不然会有8个小时的时间差
//            calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
//            calendar.set(Calendar.HOUR_OF_DAY, alarmHour[j]);
//            calendar.set(Calendar.MINUTE, alarmMin[j]);
//            calendar.set(Calendar.SECOND, 0);
//            calendar.set(Calendar.MILLISECOND, 0);
//// 选择的定时时间
//            long selectTime = calendar.getTimeInMillis();
//// 如果当前时间大于设置的时间，那么就从第二天的设定时间开始
//            if (systemTime > selectTime) {
//                calendar.add(Calendar.DAY_OF_MONTH, 1);
//                selectTime = calendar.getTimeInMillis();
//            }
//// 进行闹铃注册
//            AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
//            assert manager != null;
//            manager.setRepeating(AlarmManager.RTC_WAKEUP,
//                    selectTime, 24 * 60 * 60 * 1000, sender);
//        }
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
//        ArrayList<History> alertList = getStatusBar().getAlertList();
//
//        if (getStatusBar().getAlarm() != null) {
//            showDialogAlarm(getStatusBar().getAlarm());
//        } else if (alertList.size() > 0) {
//            showDialogAlarm(alertList.get(alertList.size() - 1));
//        }

        onTimeTick();

        mIsForeground = true;
    }


    @Override
    protected void onPause() {
        super.onPause();

        mIsForeground = false;
        triggerReaction(ParameterSystem.REACTION_NORMAL);
        getStatusBar().setPDACharger(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityStackManager.getInstance().removeActivity(this);
        //结束Activity&从堆栈中移除
        mDialogLoadingCount = 0;
        ((ApplicationPDA) getApplication()).unregisterMessageListener(
                ParameterGlobal.PORT_COMM, mMessageListener);
        ((ApplicationPDA) getApplication()).unregisterMessageListener(
                ParameterGlobal.PORT_MONITOR, mMessageListener);
        unregisterReceiver(mBroadcastReceiver);
        dismissDialogProgress();
        popScreenWindow();

        if (signalTimer != null) {
            signalTimer.cancel();
            signalTimer = null;
        }
    }


    protected void onBatteryChanged(int level, final int status) {
        final int BATTERY_LEVEL_LOW = 5;
        final int BATTERY_LEVEL_RECOVER = 30;
        final int BATTERY_UPDATE_CYCLE = 1000;

        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
            case BatteryManager.BATTERY_STATUS_FULL:
                if (pdaLowBatteryFragmentDialog != null) {
                    Event event = new Event(0, PDA_BATTERY_LOW, 0);
                    History history = new History(
                            new DateTime(Calendar.getInstance()), new Status(), event);
                    confirmAlarm(history);
                    pdaLowBatteryFragmentDialog.dismissAllowingStateLoss();
                }

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
                break;
            default:
                if (sIsPDABatteryCharging) {
                    sIsPDABatteryCharging = false;
                    getStatusBar().setPDACharger(false);
                }
                break;
        }

        mLog.Error(getClass(), "电量上报：" + level + "电量模式：" + batteryMode);

        batteryList.add(level);

        switch (batteryMode) {
            case 0:
                if (batteryList.size() < 25) {
                    return;
                }
                for (int j = 0; j < 5; j++) {
                    batteryList.remove(Collections.max(batteryList));
                }
                for (int j = 0; j < 5; j++) {
                    batteryList.remove(Collections.min(batteryList));
                }
                int sum = 0;
                for (int i1 = 0; i1 < batteryList.size(); i1++) {
                    sum += batteryList.get(i);
                }
                level = sum / batteryList.size();
                break;
            case 1:
                if (batteryList.size() < 5) {
                    return;
                }
                level = batteryList.get(batteryList.size() - 1);
                batteryMode = 0;
                break;
            default:

                break;
        }

        mLog.Error(getClass(), "电量集合数量：" + batteryList.size() + Arrays.toString(new List[]{batteryList}));
        batteryList.clear();
        getStatusBar().setPDABattery(level);

        if ((!sIsPDABatteryLow) && (level <= BATTERY_LEVEL_LOW)) {
            sIsPDABatteryLow = true;
            if (ActivityStackManager.containActivity("ActivityBgEnter")) {
                if ("ActivityMain".equals(getClass().getSimpleName())) {
                    return;
                }
            }
            Event event = new Event(0, PDA_BATTERY_LOW, 0);
            History history = new History(
                    new DateTime(Calendar.getInstance()), new Status(), event);
//            if (mIsForeground || hasWindowFocus()) {
            notifyErrEventAlert(history, PAD_LOWBATTERY_ERR_TYPE);
//            }
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

    protected void onScreenOn() {
        mLog.Debug(getClass(), "Screen on");
        batteryMode = 1;
        batteryList.clear();

        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.PARAM_BROADCAST_OFFSET, new byte[]
                {
                        ParameterComm.BROADCAST_OFFSET_ALL
                }));
        Intent lockIntent = new Intent(ActivityPDA.this, ActivityUnlockNew.class);
        startActivity(lockIntent);
        ActivityPathManager.getInstance().registerSourceActivity(getAddTime());
    }

    protected void onScreenOff() {
//        mLog.Debug(getClass(), "Screen off");
//
//        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
//                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
//                ParameterComm.PARAM_BROADCAST_OFFSET, new byte[]
//                {
//                        ParameterComm.BROADCAST_OFFSET_STATUS
//                }));

//        ActivityStackManager.getInstance().back2TargetActivity()
        String topActivity_1 = ActivityStackManager.getTopActivity(this);
        Log.e("topActivity_1", topActivity_1);
//
        Intent lockIntent = new Intent(ActivityPDA.this, ActivityUnlockNew.class);
        startActivity(lockIntent);
        ActivityPathManager.getInstance().registerSourceActivity(getAddTime());
//        Intent intent = new Intent(this, ActivityMain.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);

//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        }, 1000);

    }

    protected void onHomePressed() {
//        Intent intent = new Intent(this, ActivityMain.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
    }

    protected void onVolumeUpPressed() {
//        mKeyNavigation.onKeyPrevious();
    }

    protected void onVolumeDownPressed() {
//        mKeyNavigation.onKeyNext();
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
            mFragmentProgress = new FragmentNewProgress(this,
                    com.example.liangmutian.mypicker.R.style.Theme_Light_NoTitle_Dialog);
        }

        mFragmentProgress.setComment(content);

        if (mDialogLoadingCount <= 0) {
            showDialogProgress();
        }
    }


    public void showDialogProgress() {
        mLog.Debug(getClass(), "Show progress dialog");

        if (mFragmentProgress == null) {
            mFragmentProgress = new FragmentNewProgress(this,
                    com.example.liangmutian.mypicker.R.style.Theme_Light_NoTitle_Dialog);
            mFragmentProgress.setCancelable(false);
            mFragmentProgress.setComment(getString(R.string.connecting));
        }

//        if (mFragmentDialog == null) {
//            mFragmentDialog = new FragmentDialog();
//            mFragmentDialog.setBottom(false);
//            mFragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
//                    null);
//            mFragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
//                    null);
//            mFragmentDialog.setContent(mFragmentProgress);
//            mFragmentDialog.setCancelable(false);
//        }

        if (mDialogLoadingCount <= 0) {
            mFragmentProgress.show();
        }
        mFragmentProgress.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_HOME:
                        return true;
                }
                return false;
            }
        });
        mFragmentProgress.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        mDialogLoadingCount++;
    }


    public void dismissDialogProgress() {
        mLog.Debug(getClass(), "Dismiss progress dialog");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
//                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (mDialogLoadingCount > 0) {
                    mDialogLoadingCount--;
                }

                if (mDialogLoadingCount <= 0) {
                    mDialogLoadingCount = 0;

                    if (mFragmentProgress != null) {
                        mFragmentProgress.setComment(getString(R.string.connecting));
                        mFragmentProgress.dismiss();
                        mFragmentProgress = null;
                    }

//            if (mFragmentDialog != null) {
//                mFragmentDialog.dismissAllowingStateLoss();
//            }
                }
            }
        }, 500);
    }

    public void showDialogLoading() {
        mLog.Debug(getClass(), "Show progress dialog");

        if (mFragmentProgress == null) {
            mFragmentProgress = new FragmentNewProgress(this,
                    com.example.liangmutian.mypicker.R.style.Theme_Light_NoTitle_Dialog);
            mFragmentProgress.setCancelable(false);
            mFragmentProgress.setComment(getString(R.string.loading));
        }

//        if (mFragmentDialog == null) {
//            mFragmentDialog = new FragmentDialog();
//            mFragmentDialog.setBottom(false);
//            mFragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
//                    null);
//            mFragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
//                    null);
//            mFragmentDialog.setContent(mFragmentProgress);
//            mFragmentDialog.setCancelable(false);
//        }

        if (mDialogLoadingCount <= 0) {
            mFragmentProgress.show();
        }
        mFragmentProgress.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_HOME:
                        return true;
                }
                return false;
            }
        });
        mFragmentProgress.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        mDialogLoadingCount++;
    }


    public void dismissDialogLoading() {
        mLog.Debug(getClass(), "Dismiss progress dialog");

        if (mDialogLoadingCount > 0) {
            mDialogLoadingCount--;
        }

        if (mDialogLoadingCount <= 0) {
            mDialogLoadingCount = 0;

            if (mFragmentProgress != null) {
                mFragmentProgress.setComment(getString(R.string.loading));
                mFragmentProgress.dismiss();
                mFragmentProgress = null;
            }

//            if (mFragmentDialog != null) {
//                mFragmentDialog.dismissAllowingStateLoss();
//            }
        }
    }


    protected void setParameter(final EntityMessage message) {
        mLog.Debug(getClass(), "Set Parameter: " + message.getParameter());
        if (message.getParameter() == ParameterComm.CLOSE_COMM) {
            commErrorFlag = message.getData()[0] != 0;
        }

        if (message.getParameter() == ParameterComm.PAIRAGAIN) {
            pair(RFAddress.RF_ADDRESS_UNPAIR);
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.fragment_settings_pairing)
                    .setMessage(R.string.pair_data_err)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
            final AlertDialog dialog = builder.create();
            dialog.show();
            setCustomDialogStyle(dialog);
            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_HOME:
                            dialog.dismiss();
                            return true;
                    }
                    return false;
                }
            });
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        }
    }

    private void pair(String addressString) {
        if (addressString.equals(RFAddress.RF_ADDRESS_UNPAIR)) {
            getDataStorage(
                    ActivityPDA.class.getSimpleName())
                    .setExtras(ActivityPDA.SETTING_RF_ADDRESS,
                            null);
        } else {
            getDataStorage(
                    ActivityPDA.class.getSimpleName())
                    .setExtras(ActivityPDA.SETTING_RF_ADDRESS,
                            new RFAddress(addressString).getByteArray());
        }

        // Set remote address
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.PARAM_RF_LOCAL_ADDRESS,
                new RFAddress(addressString).getByteArray()));

        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_LOCAL_MODEL, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.PARAM_RF_REMOTE_ADDRESS,
                new RFAddress(addressString).getByteArray()));
    }

    protected void getParameter(final EntityMessage message) {
        mLog.Debug(getClass(), "Get Parameter: " + message.getParameter());
    }


    protected void handleEvent(final EntityMessage message) {
        mLog.Debug(getClass(), "Handle Event: " + message.getEvent());

        if (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER) {
            dismissDialogProgress();
            if (message.getEvent() == EntityMessage.EVENT_TIMEOUT) {
                Toast.makeText(this,
                        getResources().getString(R.string.connect_timeout),
                        Toast.LENGTH_SHORT).show();
            }
        }
        if (message.getParameter() == ParameterMonitor.PARAM_EVENT_COMM) {
            History history = new History(message.getData());
            if (commErrFragmentDialog != null) {
                confirmAlarm(history);
                commErrFragmentDialog.dismissAllowingStateLoss();
            }
        }
        if (message.getParameter() == ParameterMonitor.PDA_ERROR_RECOVER) {
            History history = new History(message.getData());
            if (pdaErrFragmentDialog != null) {
                confirmAlarm(history);
                pdaErrFragmentDialog.dismissAllowingStateLoss();
            }
        }

        if (message.getParameter() == ParameterMonitor.PARAM_NEW) {
            History history = new History(message.getData());
            Status status = history.getStatus();
            int value = status.getShortValue1();
            mLog.Error(getClass(), "初始化：" + history.getDateTime().getBCD()
                    + "index  :" + history.getEvent().getIndex() + "type :" + history.getEvent().getEvent()
                    + "value  :" + value);
            if (history.getEvent().getEvent() == SENSOR_NEW) {
                if (value == 0xFF) {
                    if (newSensorFlag) {
//                        if (mIsForeground || hasWindowFocus()) {
                        if (ActivityStackManager.containActivity("ActivityBgEnter")) {
                            if ("ActivityMain".equals(getClass().getSimpleName())) {
                                return;
                            }
                        }
                        notifyNewSensorEventAlert(history);
                        newSensorFlag = false;
//                        }
                    }
                } else {
                    newSensorFlag = true;
                    if (newSensorFragmentDialog != null) {
                        confirmNewSensorAlarm(history);
                        newSensorFragmentDialog.dismissAllowingStateLoss();
                    }
                    handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.PORT_MONITOR,
                                    ParameterGlobal.PORT_MONITOR,
                                    EntityMessage.OPERATION_SET,
                                    ParameterMonitor.COUNTDOWNVIEW_VISIBLE,
                                    new ValueInt(value).getByteArray()));
                }

            }
        }
    }


    protected void handleNotification(EntityMessage message) {
        mLog.Debug(getClass(), "Notify Port: " + message.getSourcePort() +
                " Parameter: " + message.getParameter());

        if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
                (message.getParameter() == ParameterComm.PARAM_RF_SIGNAL)) {
            boolean isPaired = (boolean) SPUtils.get(this, IS_PAIRED, false);
            if (!isPaired) {
                return;
            }
            SPUtils.put(this, RFSIGNAL, signal);
            if ((int) message.getData()[0] > 0) {
                signal = (int) message.getData()[0];
                noSignalTag = 0;
            } else {
                noSignalTag = System.currentTimeMillis();
            }
            if (signalTimer != null) {
                signalTimer.cancel();
                signalTimer = null;
            }
            signalTimer = new Timer();
            signalTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Message message = new Message();
                    message.what = 0;
                    signalHandler.sendMessage(message);
                }
            }, 0, 3000);

//            if (noSignaltimer == null) {
//                noSignaltimer = new CountDownTimer(NOSIGNAL_TIMEOUT, NOSIGNAL_TIMEOUT) {
//                    @Override
//                    public void onTick(long l) {
//                    }
//
//                    @Override
//                    public void onFinish() {
//                        getStatusBar().setRFSignal(0);
//                    }
//                };
//            } else {
//                noSignaltimer.cancel();
//            }
//            noSignaltimer.start();

//            if ((int) message.getData()[0] <= 0) {
//                if (comm_countDownTimer == null) {
//                    comm_countDownTimer = new CountDownTimer(COMM_ERR_TIMEOUT, COMM_ERR_TIMEOUT) {
//                        @Override
//                        public void onTick(long l) {
//
//                        }
//
//                        @Override
//                        public void onFinish() {
//                            Event event = new Event(0, PDA_COMM_ERROR, 0);
//                            History history = new History(
//                                    new DateTime(Calendar.getInstance()), new Status(), event);
//                            if (mIsForeground || hasWindowFocus() ||
//                                    (mFragmentAlarm != null)) {
//                                if (commErrorFlag) {
//                                    boolean comm_messageFlag = (boolean) SPUtils.get(context, COMMMESSAGETIPS, true);
//                                    if (comm_messageFlag) {
//                                        notifyErrEventAlert(history, COMM_ERR_TYPE);
//                                    }
//                                }
//                            }
//                        }
//                    };
//                }
//                comm_countDownTimer.start();
//            } else {
//                if (comm_countDownTimer != null) {
//                    comm_countDownTimer.cancel();
//                    comm_countDownTimer = null;
//                    Event event = new Event(0, PDA_COMM_ERROR, 0);
//                    History history = new History(
//                            new DateTime(Calendar.getInstance()), new Status(), event);
//                    if (commErrFragmentDialog != null) {
//                        confirmAlarm(history);
//                        commErrFragmentDialog.dismissAllowingStateLoss();
//                    }
//                }
//            }
        }

//        if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
//                (message.getParameter() == ParameterComm.COMM_ERR)) {
//            mLog.Error(getClass(), "mIsForeground" + mIsForeground);
//            mLog.Error(getClass(), "hasWindowFocus()" + hasWindowFocus());
//            Event event = new Event(0, PDA_COMM_ERROR, 0);
//            History history = new History(
//                    new DateTime(Calendar.getInstance()), new Status(), event);
//            if (commErrorFlag) {
//                boolean comm_messageFlag = (boolean) SPUtils.get(context, COMMMESSAGETIPS, true);
//                if (mIsForeground || hasWindowFocus()) {
//                    if (comm_messageFlag) {
//                        notifyErrEventAlert(history, COMM_ERR_TYPE);
//                    }
//                }
//
//            }
//        }
        if (ActivityStackManager.containActivity("ActivityBgEnter")) {
            if ("ActivityMain".equals(getClass().getSimpleName())) {
                return;
            }
        }
        if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
                (message.getParameter() == ParameterComm.COMM_ERR_RECOVERY)) {
            Event event = new Event(0, PDA_COMM_ERROR, 0);
            History history = new History(
                    new DateTime(Calendar.getInstance()), new Status(), event);
            if (commErrFragmentDialog != null) {
                confirmAlarm(history);
                commErrFragmentDialog.dismissAllowingStateLoss();
            }
        }
        if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
                (message.getParameter() == ParameterComm.COMM_ERR)) {
            Event event = new Event(0, PDA_COMM_ERROR, 0);
            History history = new History(
                    new DateTime(Calendar.getInstance()), new Status(), event);
            if (commErrorFlag) {
                boolean comm_messageFlag = (boolean) SPUtils.get(context, COMMMESSAGETIPS, false);
                if (comm_messageFlag) {
                    notifyErrEventAlert(history, COMM_ERR_TYPE);
                }

            }
        }
        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
                (message.getParameter() == ParameterMonitor.PARAM_HISTORY)) {
            if (message
                    .getSourceAddress() != ParameterGlobal.ADDRESS_LOCAL_MODEL) {
//                if (mIsForeground || hasWindowFocus()) {
                if (message.getData() != null) {
                    DataList dataList = new DataList(message.getData());
                    for (int i = 0; i < dataList.getCount(); i++) {
                        History history = new History(dataList.getData(i));
                        Event event = history.getEvent();
                        if (event.getEvent() == TRANSMITTER_STARTUP || event.getEvent() == INVALID || event.getEvent() == GLUCOSE) {
                            continue;
                        }

                        long timeChange = System.currentTimeMillis() - history.getDateTime().getCalendar().getTimeInMillis();
                        if (Math.abs(timeChange) > 15 * 60 * 1000) {
                            continue;
                        }
                        boolean low_messageFlag = (boolean) SPUtils.get(this, LOMESSAGETIPS, true);
                        boolean hi_messageFlag = (boolean) SPUtils.get(this, HIMESSAGETIPS, true);
                        switch (event.getEvent()) {
                            case HYPO:
                                if (low_messageFlag) {
                                    alertTips(history);
                                }
                                break;
                            case HYPER:
                                if (hi_messageFlag) {
                                    alertTips(history);
                                }
                                break;
                            case SENSOR_ERROR:
                            case SENSOR_EXPIRATION:
                                alertTips(history);
                                break;
                            case PDA_ERROR:
                                notifyErrEventAlert(history, PDA_ERR_TYPE);
                                break;
                            default:

                                break;
                        }
                    }
                }
//                }
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

//            Toast.makeText(this, getResources().getString(R.string.connect_fail),
//                    Toast.LENGTH_SHORT)
//                    .show();
        }

        if (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER) {
            dismissDialogProgress();
        }

        if (message.getParameter() == ParameterGlucose.TASK_GLUCOSE_PARAM_NEW_SENSOR) {
            if (message.getData()[0] == EntityMessage.FUNCTION_OK) {
                if (newFlag) {
                    handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.PORT_MONITOR,
                                    ParameterGlobal.PORT_MONITOR,
                                    EntityMessage.OPERATION_SET,
                                    ParameterMonitor.COUNTDOWNVIEW_VISIBLE,
                                    new ValueInt(60).getByteArray()));

                    handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                    ParameterGlobal.PORT_MONITOR,
                                    ParameterGlobal.PORT_MONITOR,
                                    EntityMessage.OPERATION_SET,
                                    ParameterComm.FORCESYNCHRONIZEFLAG, new byte[]{}));
                }
            }
        }
    }


    protected String getEventContent(final Event event) {
        String content = "";

        switch (event.getEvent()) {
            case SENSOR_ERROR:
                content = getString(R.string.alarm_sensor_error);
                break;

            case SENSOR_EXPIRATION:
                content = getString(R.string.alarm_expiration);
                break;

            case HYPO:
                content = getString(R.string.alarm_hypo);
                break;

            case HYPER:
                content = getString(R.string.alarm_hyper);
                break;

            case PDA_BATTERY_LOW:
                content = getString(R.string.alarm_pda_battery);
                break;

            case PDA_ERROR:
                content = getString(R.string.alarm_pda_error);
                break;

            case PDA_COMM_ERROR:
                content = getString(R.string.alarm_pda_comm_error);
                break;
            case GLUCOSE_CHECK:
                content = getString(R.string.glucose_messagetips);
                break;
            case BLOOD_GLUCOSE:
                content = getString(R.string.blood_glucose);
                break;

            case CALIBRATION:
                content = getString(R.string.calibrate_blood);
                break;
            default:
                break;
        }
        return content;
    }


    private static synchronized void acquireWakeLock() {
        if (!sWakeLock.isHeld()) {
//            sWakeLock.acquire();
        }
    }


    private static synchronized void releaseWakeLock() {
        if (sWakeLock.isHeld()) {
            sWakeLock.release();
        }
    }


    private void notifyNewSensorEventAlert(final History history) {
        ArrayList<History> alertList = getStatusBar().getAlertList();
        if (history.getEvent().getEvent() > 0) {
            if (getStatusBar().getAlarm() == null) {
                for (int i = 0; i < alertList.size(); i++) {
                    if (history.getEvent().getEvent() == alertList.get(i).getEvent().getEvent()) {
                        alertList.remove(i);
                    }
                }
                triggerReaction(ParameterSystem.REACTION_ALERT);
                showNewSensorDialog(history);
                alertList.add(history);

                getStatusBar().setAlertList(alertList);
                mLog.Error(getClass(), "添加报警1：" + alertList.size() + "类型：" + history.getEvent().getEvent());
                getDataStorage(ActivityPDA.class.getSimpleName())
                        .setExtras(SETTING_STATUS_BAR, getStatusBar().getByteArray());
            }
        }
    }

    private void notifyErrEventAlert(final History history, int errType) {
        ArrayList<History> alertList = getStatusBar().getAlertList();
        if (history.getEvent().getEvent() > 0) {
            if (getStatusBar().getAlarm() == null) {
                if (!TextUtils.isEmpty(getEventContent(history.getEvent()))) {
                    for (int i = 0; i < alertList.size(); i++) {
                        if (history.getEvent().getEvent() == alertList.get(i).getEvent().getEvent()) {
                            alertList.remove(i);
                        }
                    }
                    triggerReaction(ParameterSystem.REACTION_ALERT);
                    showErrDialog(history, errType);
                    alertList.add(history);

                    getStatusBar().setAlertList(alertList);
                    mLog.Error(getClass(), "添加报警2：" + alertList.size() + "类型：" + history.getEvent().getEvent());

                    getDataStorage(ActivityPDA.class.getSimpleName())
                            .setExtras(SETTING_STATUS_BAR, getStatusBar().getByteArray());
                }
            }
        }
    }

    private void alertTips(History history) {
        switch (history.getEvent().getEvent()) {
            case SENSOR_ERROR:
                triggerReaction(ParameterSystem.REACTION_ALERT);
                if (sensorErrFragmentDialog == null) {
                    sensorErrFragmentDialog = new FragmentDialog();
                    newAlertTips(sensorErrFragmentDialog, history);
                } else {
                    reshowAlertTips(sensorErrFragmentDialog);
                }
                break;
            case SENSOR_EXPIRATION:
                triggerReaction(ParameterSystem.REACTION_ALERT);
                if (sensorExpirationFragmentDialog == null) {
                    sensorExpirationFragmentDialog = new FragmentDialog();
                    newAlertTips(sensorExpirationFragmentDialog, history);
                } else {
                    reshowAlertTips(sensorExpirationFragmentDialog);
                }
                break;
            case HYPO:
                triggerReaction(ParameterSystem.REACTION_ALARM);
                if (lowGlucoseFragmentDialog == null) {
                    lowGlucoseFragmentDialog = new FragmentDialog();
                    newAlertTips(lowGlucoseFragmentDialog, history);
                } else {
                    reshowAlertTips(lowGlucoseFragmentDialog);
                }
                break;
            case HYPER:
                triggerReaction(ParameterSystem.REACTION_ALERT);

                if (highGlucoseFragmentDialog == null) {
                    highGlucoseFragmentDialog = new FragmentDialog();
                    newAlertTips(highGlucoseFragmentDialog, history);
                } else {
                    reshowAlertTips(highGlucoseFragmentDialog);
                }
                break;
            case GLUCOSE_CHECK:
                triggerReaction(ParameterSystem.REACTION_ALERT);

                if (glucoseCheckFragmentDialog == null) {
                    glucoseCheckFragmentDialog = new FragmentDialog();
                    newAlertTips(glucoseCheckFragmentDialog, history);
                } else {
                    reshowAlertTips(glucoseCheckFragmentDialog);
                }
                break;
            default:

                break;
        }
    }

    private void newAlertTips(FragmentDialog fragmentDialog, History history) {
        FragmentMessage message = new FragmentMessage();
        message.setComment(getEventContent(history.getEvent()));

        fragmentDialog.setTitle(getString(R.string.alarm_dialog_title));
        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
                "");
        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
                null);
        fragmentDialog.setContent(message);
        fragmentDialog.setCancelable(false);
        fragmentDialog.setListener(new FragmentDialog.ListenerDialog() {
            @Override
            public boolean onButtonClick(int buttonID, Fragment content) {
                switch (buttonID) {
                    case FragmentDialog.BUTTON_ID_POSITIVE:
                        releaseWakeLock();
                        triggerReaction(ParameterSystem.REACTION_NORMAL);
                    default:
                        break;
                }

                return true;
            }
        });
        fragmentDialog.show(getSupportFragmentManager(), null);
    }

    private void reshowAlertTips(FragmentDialog fragmentDialog) {
        fragmentDialog.dismissAllowingStateLoss();
        fragmentDialog.show(getSupportFragmentManager(), null);
    }

//    private void notifyEventAlert(final History history) {
//        ArrayList<History> alertList = getStatusBar().getAlertList();
//
//        if (history.getEvent().getEvent() > 0) {
//            if (getStatusBar().getAlarm() == null) {
//                if (!TextUtils.isEmpty(getEventContent(history.getEvent()))) {
//                    for (int i = 0; i < alertList.size(); i++) {
//                        if (history.getEvent().getEvent() == alertList.get(i).getEvent().getEvent()) {
//                            alertList.remove(i);
//                        }
//                    }
//                    triggerReaction(ParameterSystem.REACTION_ALERT);
//                    showDialogAlarm(history);
//                    alertList.add(history);
//                    getStatusBar().setAlertList(alertList);
//                    mLog.Error(getClass(), "添加报警3：" + alertList.size() + "类型：" + history.getEvent().getEvent());
//                    getDataStorage(ActivityPDA.class.getSimpleName())
//                            .setExtras(SETTING_STATUS_BAR, getStatusBar().getByteArray());
//                }
//            }
//        }
//    }
//
//
//    private void notifyEventAlarm(final History history) {
//        acquireWakeLock();
//        ArrayList<History> alertList = getStatusBar().getAlertList();
//
//        if (history.getEvent().getEvent() > 0) {
//            if (getStatusBar().getAlarm() == null) {
//                if (!TextUtils.isEmpty(getEventContent(history.getEvent()))) {
//                    for (int i = 0; i < alertList.size(); i++) {
//                        if (history.getEvent().getEvent() == alertList.get(i).getEvent().getEvent()) {
//                            alertList.remove(i);
//                        }
//                    }
//                    triggerReaction(ParameterSystem.REACTION_ALARM);
//                    showDialogAlarm(history);
//                    alertList.add(history);
//
//                    getStatusBar().setAlertList(alertList);
//                    mLog.Error(getClass(), "添加报警4：" + alertList.size() + "类型：" + history.getEvent().getEvent());
//                    getDataStorage(ActivityPDA.class.getSimpleName())
//                            .setExtras(SETTING_STATUS_BAR, getStatusBar().getByteArray());
//                }
//            }
//        }
//    }


    private boolean showDialogPower() {
        if (sIsPowerdown) {
            return false;
        }

        sIsPowerdown = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setTitle(getString(R.string.shutdown_title))
                .setMessage(getString(R.string.shutdown_content))
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            Intent shutdown = new Intent(
                                    "android.intent.action.ACTION_REQUEST_SHUTDOWN");
                            startActivity(shutdown);
                        } catch (Exception e) {
                            Toast.makeText(mBaseActivity, "Shut Down failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sIsPowerdown = false;
                    }
                });
        AlertDialog dialog = builder.create();
//        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
//        if (powerDownFramentDialog == null) {
//            powerDownFramentDialog = new FragmentDialog();
//            FragmentMessage fragmentMessage = new FragmentMessage();
//            fragmentMessage.setComment(getString(R.string.shutdown_content));
//            fragmentMessage.setIcon(false);
//            powerDownFramentDialog.setTitle(getString(R.string.shutdown_title));
//            powerDownFramentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
//                    "");
//            powerDownFramentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
//                    "");
//            powerDownFramentDialog.setContent(fragmentMessage);
//            powerDownFramentDialog.setCancelable(false);
//            powerDownFramentDialog.setListener(new FragmentDialog.ListenerDialog() {
//                @Override
//                public boolean onButtonClick(int buttonID, Fragment content) {
//                    if (buttonID == FragmentDialog.BUTTON_ID_POSITIVE) {
//                        showToast(R.string.Toast_shutdown);
//                        try {
//                            Intent shutdown = new Intent(
//                                    "android.intent.action.ACTION_REQUEST_SHUTDOWN");
//                            startActivity(shutdown);
//                        } catch (Exception e) {
//                            showToast("Shut Down failed.", 1000);
//                        }
//                    } else if (buttonID == FragmentDialog.BUTTON_ID_NEGATIVE) {
//                        sIsPowerdown = false;
//                    }
//
//                    return true;
//                }
//            });
//            powerDownFramentDialog.show(getSupportFragmentManager(), null);
//        } else {
//            reshowAlertTips(powerDownFramentDialog);
//        }

//        FragmentMessage fragmentMessage = new FragmentMessage();
//        fragmentMessage.setComment(getString(R.string.shutdown_content));
//        fragmentMessage.setIcon(false);
//        showDialogConfirm(getString(R.string.shutdown_title), "", "",
//                fragmentMessage, false, new FragmentDialog.ListenerDialog() {
//                    @Override
//                    public boolean onButtonClick(int buttonID, Fragment content) {
//                        if (buttonID == FragmentDialog.BUTTON_ID_POSITIVE) {
//                            showToast(R.string.Toast_shutdown);
//
//                            try {
//                                Intent shutdown = new Intent(
//                                        "android.intent.action.ACTION_REQUEST_SHUTDOWN");
//                                startActivity(shutdown);
//                            } catch (Exception e) {
//                                showToast("Shut Down failed.", 1000);
//                            }
//                        } else if (buttonID == FragmentDialog.BUTTON_ID_NEGATIVE) {
//                            sIsPowerdown = false;
//                        }
//
//                        return true;
//                    }
//                });

        return true;
    }


    private void showDialogAlarm(final History history) {
        mLog.Error(getClass(), "messageType:  " + history.getEvent().getEvent());
        if (!TextUtils.isEmpty(getEventContent(history.getEvent()))) {
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
    }

    private void showNewSensorDialog(final History history) {
        FragmentInput fragmentInput = new FragmentInput();
        fragmentInput
                .setComment(getString(R.string.alarm_new_sensor));
        showNewSensorDialogConfirm("", "",
                "", fragmentInput, false, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        confirmNewSensorAlarm(history);
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                sendNewSensorMessage(1);
                                newFlag = true;
                                newSensorFlag = true;
                                break;
                            case FragmentDialog.BUTTON_ID_NEGATIVE:
                                FragmentDialog fragmentDialog = new FragmentDialog();
                                fragmentDialog.setTitle(getString(R.string.sure_cancel));
                                fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
                                        "");
                                fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
                                        "");

                                FragmentMessage mFragmentEnsure = new FragmentMessage();
                                mFragmentEnsure.setComment(getString(R.string.ensure));
                                fragmentDialog.setContent(mFragmentEnsure);
                                fragmentDialog.setCancelable(false);
                                fragmentDialog.setListener(new FragmentDialog.ListenerDialog() {
                                    @Override
                                    public boolean onButtonClick(int buttonID, Fragment content) {
                                        switch (buttonID) {
                                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                                sendNewSensorMessage(0);
                                                newFlag = false;
                                                newSensorFlag = true;
                                                if (newSensorFragmentDialog != null) {
                                                    confirmNewSensorAlarm(history);
                                                    newSensorFragmentDialog.dismissAllowingStateLoss();
                                                }
                                                break;
                                            case FragmentDialog.BUTTON_ID_NEGATIVE:

                                                break;
                                            default:

                                                break;
                                        }
                                        return true;
                                    }
                                });
                                fragmentDialog.show(getSupportFragmentManager(), null);

                                return false;
                            default:
                                break;
                        }
                        return true;
                    }
                });
    }

    private void showErrDialog(final History history, int errType) {
        if (!TextUtils.isEmpty(getEventContent(history.getEvent()))) {
            if (mFragmentAlarm == null) {
                mFragmentAlarm = new FragmentMessage();
                mFragmentAlarm.setComment(getEventContent(history.getEvent()));
                showErrConfirm(errType, getString(R.string.alarm_dialog_title), "", null,
                        mFragmentAlarm, new FragmentDialog.ListenerDialog() {
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
    }

    private void sendNewSensorMessage(int i) {
        handleMessage(new EntityMessage(
                ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_REMOTE_MASTER,
                ParameterGlobal.PORT_GLUCOSE,
                ParameterGlobal.PORT_GLUCOSE,
                EntityMessage.OPERATION_SET,
                ParameterGlucose.TASK_GLUCOSE_PARAM_NEW_SENSOR,
                new byte[]{(byte) i}));
        dismissDialogProgress();
    }


    private boolean confirmAlarm(final History history) {
        ArrayList<History> alertList = getStatusBar().getAlertList();

        if (alertList.size() > 0) {
            alertList.remove(alertList.size() - 1);

            getStatusBar().setAlertList(alertList);
        }

        getDataStorage(ActivityPDA.class.getSimpleName())
                .setExtras(SETTING_STATUS_BAR, getStatusBar().getByteArray());
        triggerReaction(ParameterSystem.REACTION_NORMAL);
        mLog.Error(getClass(), "报警数量：" + alertList.size());
        if (alertList.size() == 0) {
            triggerReaction(ParameterSystem.REACTION_NORMAL);
            mFragmentAlarm = null;
            return true;
        } else {
            if (alertList.get(alertList.size() - 1).getEvent().getEvent() > 0) {
                if (!TextUtils.isEmpty(getEventContent(alertList.get(alertList.size() - 1).getEvent()))) {
//                    if (alertList.get(alertList.size() - 1).getEvent().getEvent() == HYPO) {
//                        triggerReaction(ParameterSystem.REACTION_ALARM);
//                    } else {
//                        triggerReaction(ParameterSystem.REACTION_ALERT);
//                    }
                    if (alertList.get(alertList.size() - 1).getEvent().getEvent() == SENSOR_NEW) {
                        showNewSensorDialog(alertList.get(alertList.size() - 1));
                    } else {
                        showDialogAlarm(alertList.get(alertList.size() - 1));
                    }
                }
            }
            return false;
        }
    }

    private boolean confirmNewSensorAlarm(final History history) {
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
        mLog.Error(getClass(), "报警数量：" + alertList.size());
        triggerReaction(ParameterSystem.REACTION_NORMAL);
        if (alertList.size() == 0) {
            triggerReaction(ParameterSystem.REACTION_NORMAL);
            mFragmentAlarm = null;
            return true;
        } else {
            if (alertList.get(alertList.size() - 1).getEvent().getEvent() > 0) {
//                triggerReaction(ParameterSystem.REACTION_ALERT);
                if (alertList.get(alertList.size() - 1).getEvent().getEvent() == SENSOR_NEW) {
                    showNewSensorDialog(alertList.get(alertList.size() - 1));
                } else {
                    showDialogAlarm(alertList.get(alertList.size() - 1));
                }
            }
            return false;
        }
    }


    private void triggerReaction(int reaction) {
        final int BEEP_ALARM_CYCLE = 28000;


        switch (reaction) {
            case ParameterSystem.REACTION_ALARM:
                mLog.Error(getClass(), "报警声");
                acquireWakeLock();
                MediaUtil.playMp3ByType(this, "beep_alarm.mp3", false);

//                final Handler handlerSound = new Handler();
//
//                handlerSound.postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (getStatusBar().getAlarm() != null) {
//                            handlerSound.postDelayed(this, BEEP_ALARM_CYCLE);
//                            MediaUtil.playMp3ByType(ActivityPDA.this,
//                                    "beep_alarm.mp3", false);
//                        } else {
//                            handlerSound.removeCallbacks(this);
//                        }
//                    }
//                }, BEEP_ALARM_CYCLE);

                break;

            case ParameterSystem.REACTION_ALERT:
                mLog.Error(getClass(), "报警声");
                acquireWakeLock();
                MediaUtil.playMp3ByType(this, "beep_alert.mp3", false);
                break;

            case ParameterSystem.REACTION_NORMAL:
                MediaUtil.stop();
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

    public static void setDefaultInputMethod(Context context) {
        //获取系统已安装的输入法ID
        String[] methods = getInputMethodIdList(context);
        if (methods == null || methods.length == 0) {
            return;
        }

        //默认输入法ID "com.android.inputmethod.latin/.LatinIME";
        String targetKeyword = "LatinIME";
        String value = "";
        for (String m : methods) {
            Log.e("输入法：", m);
            if (m.toLowerCase().contains(targetKeyword.toLowerCase())) {
                value = m;//Android键盘
            }
        }
        if (TextUtils.isEmpty(value)) {
            return;
        }

        //设置默认输入法
        String key = Settings.Secure.DEFAULT_INPUT_METHOD;
        boolean success = Settings.Secure.putString(context.getContentResolver(), key, value);

        //读取默认输入法
        String current = Settings.Secure.getString(context.getContentResolver(), key);
    }

    /**
     * 获取系统已安装的输入法ID
     *
     * @param context
     * @return
     */
    public static String[] getInputMethodIdList(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && imm.getInputMethodList() != null) {
            String[] methodIds = new String[imm.getInputMethodList().size()];
            for (int i = 0; i < imm.getInputMethodList().size(); i++) {
                methodIds[i] = imm.getInputMethodList().get(i).getId();
            }
            return methodIds;
        }
        return new String[]{};
    }

}