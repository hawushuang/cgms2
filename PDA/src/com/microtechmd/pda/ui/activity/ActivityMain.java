package com.microtechmd.pda.ui.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.database.DataSetHistory;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.ParameterSystem;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.SPUtils;
import com.microtechmd.pda.manager.ActivityPathManager;
import com.microtechmd.pda.ui.activity.fragment.FragmentBase;
import com.microtechmd.pda.ui.activity.fragment.FragmentCalibration;
import com.microtechmd.pda.ui.activity.fragment.FragmentHome;
import com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer;
import com.microtechmd.pda.ui.widget.highlight.HighLight;
import com.microtechmd.pda.util.ToastUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer.SETTING_TIME_CORRECT;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer.TYPE_DATE_TIME;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer.TYPE_SETTING;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettings.REALTIMEFLAG;


public class ActivityMain extends ActivityPDA {
    public static final String SETTING_STARTUP_TIME = "startup_time";
    public static final String FIRST_SETUP = "first_setup";

    public static final long STARTUP_DELAY = 1 * DateTime.SECOND_PER_MINUTE *
            DateTime.MILLISECOND_PER_SECOND / 10;

    private static History history = null;
    private FragmentHome mFragmentHome = null;
    private FragmentBase mFragmentCalibration = null;
    private FragmentBase mFragmentSettings = null;

    private Handler mHandlerTimer = null;
    private Handler mHandler = null;
    private Runnable mRunnableTimer = null;
    private long mStartupTime = 0;
    private RadioGroup radioGroup;
    private HighLight mHightLight;
    private AlertDialog timeDialog;
    private AlertDialog dialog;

    private boolean isTimeSure = false;

    public static History getStatus() {
        return history;
    }


    public static void setStatus(History history) {
        if (history == null) {
            ActivityMain.history = null;
        } else {
            ActivityMain.history = new History(history.getByteArray());
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
//            updateTimer();
        }
    }

    public void switchContent(FragmentBase to) {
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .hide(mFragmentHome).commitAllowingStateLoss();
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .hide(mFragmentCalibration).commitAllowingStateLoss();
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .hide(mFragmentSettings).commitAllowingStateLoss();
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .show(to).commitAllowingStateLoss();
    }

    public void add(FragmentBase from) {
        ActivityMain.this.getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.layout_fragment, from).commit();
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (mHandler == null) {
            mHandler = new Handler();
        }
//        updateTimer();
        mFragmentHome = new FragmentHome();
        mFragmentCalibration = new FragmentCalibration();
        mFragmentSettings = new FragmentSettingContainer();

        add(mFragmentHome);
        add(mFragmentCalibration);
        add(mFragmentSettings);

        switchContent(mFragmentHome);

        radioGroup = (RadioGroup) findViewById(R.id.radio_group_tab);
        radioGroup
                .setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    FragmentBase fragMent = null;


                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        switch (group.getCheckedRadioButtonId()) {
                            case R.id.radio_button_tab_home:
                                dismissDialog();
                                if (mFragmentHome == null) {
                                    mFragmentHome = new FragmentHome();
                                }

                                fragMent = mFragmentHome;
                                switchContent(mFragmentHome);
                                mFragmentHome.setDateChange();

                                if (Calendar.getInstance()
                                        .get(Calendar.YEAR) >= YEAR_MIN) {
                                    changToMainSetting();
                                }

//                                handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                                        ParameterGlobal.ADDRESS_LOCAL_CONTROL, ParameterGlobal.PORT_SYSTEM,
//                                        ParameterGlobal.PORT_SYSTEM, EntityMessage.OPERATION_SET,
//                                        ParameterSystem.PARAM_BATTERY,
//                                        null));
                                break;

                            case R.id.radio_button_tab_calibration:
                                dismissDialog();
                                if (mFragmentCalibration == null) {
                                    mFragmentCalibration = new FragmentCalibration();
                                }

                                fragMent = mFragmentCalibration;
                                switchContent(mFragmentCalibration);

                                if (Calendar.getInstance()
                                        .get(Calendar.YEAR) >= YEAR_MIN) {
                                    changToMainSetting();
                                }
//                                handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                                        ParameterGlobal.ADDRESS_LOCAL_CONTROL, ParameterGlobal.PORT_SYSTEM,
//                                        ParameterGlobal.PORT_SYSTEM, EntityMessage.OPERATION_SET,
//                                        ParameterSystem.PARAM_BATTERY,
//                                        null));
                                break;

                            case R.id.radio_button_tab_settings:

                                if (mFragmentSettings == null) {
                                    mFragmentSettings = new FragmentSettingContainer();
                                }

                                fragMent = mFragmentSettings;
                                switchContent(mFragmentSettings);
//                                changToMainSetting();
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

        ((ApplicationPDA)

                getApplication()).

                registerMessageListener(
                        ParameterGlobal.PORT_GLUCOSE, mMessageListener);
//        if (Calendar.getInstance()
//                .get(Calendar.YEAR) >= YEAR_MIN) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showTimeSetting();
            }
        }, 300);
//        }
    }

    private void dismissDialog() {
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.DISMISS_DIALOG,
                null));
    }

    private void changToMainSetting() {
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.SETTING_TYPE,
                new byte[]{(byte) TYPE_SETTING}));
    }


    @Override
    protected void onResume() {
        super.onResume();

        getStatusBar().setGlucose(true);

        if (Calendar.getInstance()
                .get(Calendar.YEAR) < YEAR_MIN) {
            radioGroup.check(R.id.radio_button_tab_settings);
        }
    }

    private void showTimeSetting() {
        isTimeSure = false;
        handleMessage(
                new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                        ParameterGlobal.PORT_MONITOR,
                        ParameterGlobal.PORT_MONITOR,
                        EntityMessage.OPERATION_SET,
                        ParameterComm.TIME_CORRECTED, new byte[]
                        {
                                (byte) 1
                        }));
        mLog.Error(getClass(), "时间设置发送：1");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.time_sure_title))
                .setMessage(getString(R.string.time_current) + format.format(new Date()) + "\n" + getString(R.string.time_sure_content))
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        handleMessage(
                                new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                        ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                        ParameterGlobal.PORT_MONITOR,
                                        ParameterGlobal.PORT_MONITOR,
                                        EntityMessage.OPERATION_SET,
                                        ParameterComm.TIME_CORRECTED, new byte[]
                                        {
                                                (byte) 0
                                        }));
                        isTimeSure = true;
                    }
                })
                .setNegativeButton(R.string.setting, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SPUtils.put(ActivityMain.this, SETTING_TIME_CORRECT, true);
                        radioGroup.check(R.id.radio_button_tab_settings);
                        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                                ParameterComm.SETTING_TYPE,
                                new byte[]{(byte) TYPE_DATE_TIME}));
                        isTimeSure = true;
                    }
                });
        timeDialog = builder.create();
//        timeDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        timeDialog.show();
        timeDialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.WHITE);
        timeDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(Color.WHITE);
        setMargins(timeDialog.getButton(AlertDialog.BUTTON_POSITIVE), 0, 0, 2, 0);
    }

    public static void setMargins(View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            p.setMargins(l, t, r, b);
            v.requestLayout();
        }
    }

    @Override
    protected void onTimeTick() {
        super.onTimeTick();
        if (timeDialog == null) {
            return;
        }
        if (timeDialog.isShowing()) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            timeDialog.setMessage(getString(R.string.time_current) + format.format(new Date()) + "\n" + getString(R.string.time_sure_content));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        radioGroup.check(R.id.radio_button_tab_home);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((ApplicationPDA) getApplication()).unregisterMessageListener(
                ParameterGlobal.PORT_GLUCOSE, mMessageListener);
        new DataSetHistory(this).close();
    }


    @Override
    public void onBackPressed() {
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.SETTING_TYPE_BACK,
                null));
    }


    @Override
    protected void onHomePressed() {
        radioGroup.check(R.id.radio_button_tab_home);
    }

    @Override
    protected void handleEvent(EntityMessage message) {
        super.handleEvent(message);
        if ((message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE) &&
                (message.getParameter() == ParameterGlucose.HOME_PRESS)) {
            radioGroup.check(R.id.radio_button_tab_home);
        }

    }

    @Override
    protected void handleNotification(final EntityMessage message) {
        super.handleNotification(message);

        if ((message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE) &&
                (message.getParameter() == ParameterGlucose.PARAM_SIGNAL_PRESENT)) {
            if (getStatusBar().getGlucose()) {
                if (isTimeSure) {
                    if (hasWindowFocus()) {
                        Intent intent = new Intent(this, ActivityBgApply.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                } else {
                    ToastUtils.showToast(this, R.string.time_sure_title);
                }
            }
        }
    }

    @Override
    protected void setParameter(EntityMessage message) {
        super.setParameter(message);
        switch (message.getParameter()) {
            case ParameterMonitor.CAN_SEND_FAILD:
                if (radioGroup.getCheckedRadioButtonId() != R.id.radio_button_tab_settings) {
                    radioGroup.check(R.id.radio_button_tab_settings);
                }
                break;
            default:

                break;
        }

    }

    //    private void onScreenOn() {
//        mLog.Debug(getClass(), "Screen on");
//
//        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
//                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
//                ParameterComm.PARAM_BROADCAST_OFFSET, new byte[]
//                {
//                        ParameterComm.BROADCAST_OFFSET_ALL
//                }));
//    }
//
    private boolean isHelpShowed = false;

    protected void onScreenOff() {
//        if (dialog != null && dialog.isShowing()) {
//            dialog.dismiss();
//            isHelpShowed = true;
//        }
        mLog.Debug(getClass(), "Screen off");
        if (!sWakeLock.isHeld()) {
            sWakeLock.acquire(500);
        }
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.PARAM_BROADCAST_OFFSET, new byte[]
                {
                        ParameterComm.BROADCAST_OFFSET_STATUS
                }));
        radioGroup.check(R.id.radio_button_tab_home);

        Intent lockIntent = new Intent(this, ActivityUnlockNew.class);
        startActivity(lockIntent);
        ActivityPathManager.getInstance().registerSourceActivity(getAddTime());
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        if (isHelpShowed) {
//            showHelpDialog();
//            isHelpShowed = false;
//        }
//    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showHelpDialog();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.help_title))
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });
        /**
         * 设置内容区域为自定义View
         */
        View loginDialog = getLayoutInflater().inflate(R.layout.dialog_help, null);
        builder.setView(loginDialog);
        dialog = builder.create();
//        dialog.getWindow().setGravity(Gravity.BOTTOM);
//        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundColor(Color.WHITE);

        WindowManager m = getWindowManager();
        Display d = m.getDefaultDisplay();  //为获取屏幕宽、高
        WindowManager.LayoutParams p = dialog.getWindow().getAttributes();  //获取对话框当前的参数值
        p.height = (int) (d.getHeight() * 0.8);   //高度设置为屏幕的
        p.width = (int) (d.getWidth() * 0.95);    //宽度设置为屏幕的
        dialog.getWindow().setAttributes(p);     //设置生效
    }
}
