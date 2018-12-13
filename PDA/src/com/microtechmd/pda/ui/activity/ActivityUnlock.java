package com.microtechmd.pda.ui.activity;


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.ByteUtil;
import com.microtechmd.pda.manager.SharePreferenceManager;
import com.microtechmd.pda.ui.activity.fragment.FragmentLockCombinedGraph;
import com.microtechmd.pda.ui.widget.LockScreenView;
import com.microtechmd.pda.util.AndroidSystemInfoUtil;
import com.microtechmd.pda.util.TimeUtil;

import java.util.Calendar;


public class ActivityUnlock extends ActivityPDA {
    private static final String STRING_UNKNOWN = " -.- ";
    private static final long UNLOCK_SCREEN_TIME = 3000;
    private static final String TAG_GRAPH = "graph";
    private long mBolusKeyDownTime = 0;
    private long mVolumeDownKeyDownTime = 0;
    //    private FragmentGraph mFragmentGraph = null;
    private FragmentLockCombinedGraph mFragmentGraph = null;

    private TextView textView_g;
    private TextView textView_g_recommend_cal;
    private TextView textView_g_unit;
    private ImageView textView_g_err;
    private CountDownTimer countDownTimer;

    @Override
    public void onBackPressed() {
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        long KeyNowTime = System.currentTimeMillis();
        LockScreenView lsv =
                (LockScreenView) findViewById(R.id.image_view_unlock);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (event.getRepeatCount() == 0) {
                    mVolumeDownKeyDownTime = KeyNowTime;
                    if (mBolusKeyDownTime > 0) {
                        lsv.setUnlockPercent(-1f, LockScreenView.E_DOWN);
                    }
                } else if (event.getRepeatCount() == 1) {
                    mVolumeDownKeyDownTime = KeyNowTime;
                } else {
                    onKeyDownDeal_Unlocking(KeyNowTime);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:

                break;

            case ApplicationPDA.KEY_CODE_BOLUS:
                if (event.getRepeatCount() == 0) {
                    mBolusKeyDownTime = KeyNowTime;
                    if (mVolumeDownKeyDownTime > 0) {
                        lsv.setUnlockPercent(-1f, LockScreenView.E_DOWN);
                    }
                } else if (event.getRepeatCount() == 1) {
                    mBolusKeyDownTime = KeyNowTime;
                } else {
                    onKeyDownDeal_Unlocking(KeyNowTime);
                }
                return true;

        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        LockScreenView lsv =
                (LockScreenView) findViewById(R.id.image_view_unlock);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                mVolumeDownKeyDownTime = 0;
                if (mBolusKeyDownTime == 0) {
                    lsv.setUnlockPercent(-1f, LockScreenView.E_UP);
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:

                break;

            case ApplicationPDA.KEY_CODE_BOLUS:
                mBolusKeyDownTime = 0;
                if (mVolumeDownKeyDownTime == 0) {
                    lsv.setUnlockPercent(-1f, LockScreenView.E_UP);
                }
                return true;

        }
        return super.onKeyUp(keyCode, event);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(R.layout.fragment_unlock);
        setStatusButtonVisibility(false);
        initViews();
        textView_g = (TextView) findViewById(R.id.text_view_glucose);
        textView_g_unit = (TextView) findViewById(R.id.text_view_unit);
        textView_g_recommend_cal = (TextView) findViewById(R.id.text_view_glucose_recommend);
        textView_g_err = (ImageView) findViewById(R.id.glucose_err);
        if (mFragmentGraph == null) {
            mFragmentGraph = new FragmentLockCombinedGraph();
        }

        ActivityUnlock.this.getSupportFragmentManager().beginTransaction()
                .add(R.id.layout_graph, mFragmentGraph, TAG_GRAPH).commit();
//        ActivityUnlock.this.getSupportFragmentManager().beginTransaction()
//                .show(mFragmentGraph).commit();
        textView_g.setText(STRING_UNKNOWN);
        updateStatus(ActivityMain.getStatus());
    }


    @Override
    protected void onResume() {
        super.onResume();

        getStatusBar().setGlucose(false);
        updateScreenBrightness();
        updateCalendar();
    }


    @Override
    protected void onDestroy() {
        int time = SharePreferenceManager.getDisplayTimeout(mBaseActivity);
        setTimeout(time);
        super.onDestroy();
    }


    @Override
    protected void onHomePressed() {
    }


    @Override
    protected void onTimeTick() {
        super.onTimeTick();

        updateCalendar();
    }

    @Override
    protected void setParameter(EntityMessage message) {
        super.setParameter(message);
        if (message.getParameter() == ParameterMonitor.COUNTDOWNVIEW_VISIBLE) {
            if (message.getData() != null) {
                int value = ByteUtil.bytesToInt(message.getData());
                setCountdownViewVisible(value);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    public void setCountdownViewVisible(int value) {
        if (value == 0) {
            textView_g.setText(STRING_UNKNOWN);
            textView_g_unit.setText(R.string.unit_mmol_l);
        } else {
            if (value < 0) {
                return;
            }
            textView_g.setText(R.string.initializing);
            textView_g_unit.setText(String.valueOf(value) + "  "
                    + getResources().getString(R.string.minutes));
        }
    }

    @Override
    protected void handleNotification(final EntityMessage message) {
        super.handleNotification(message);

        if (message.getSourcePort() == ParameterGlobal.PORT_MONITOR) {
            if (message.getParameter() == ParameterMonitor.PARAM_STATUS) {
                if (message.getData() != null) {
                    DataList dataList = new DataList(message.getData());
                    if (dataList.getCount() > 0) {
                        History history = new History(dataList.getData(dataList.getCount() - 1));
                        updateStatus(history);
                    }
                }
            }
        }
    }


    private void initViews() {
        LockScreenView lsv =
                (LockScreenView) findViewById(R.id.image_view_unlock);
        lsv.setUnlockListener(new LockScreenView.UnlockListener() {
            @Override
            public void unlock() {
                getStatusBar().setGlucose(true);
                finish();
            }
        });
    }

    private void setGlucoseVisible(boolean glucoseVisible) {
        if (glucoseVisible) {
            textView_g.setVisibility(View.VISIBLE);
            textView_g_err.setVisibility(View.GONE);
        } else {
            textView_g.setVisibility(View.GONE);
            textView_g_recommend_cal.setVisibility(View.GONE);
            textView_g_err.setVisibility(View.VISIBLE);
        }
    }

    private boolean onKeyDownDeal_Unlocking(long KeyNowTime) {
        LockScreenView lsv =
                (LockScreenView) findViewById(R.id.image_view_unlock);
        if (mBolusKeyDownTime > 0 && mVolumeDownKeyDownTime > 0) {
            long min = 0;

            min = mBolusKeyDownTime > mVolumeDownKeyDownTime ? mBolusKeyDownTime
                    : mVolumeDownKeyDownTime;
            float x = (float) (KeyNowTime - min) / (float) UNLOCK_SCREEN_TIME;
            lsv.setUnlockPercent(x, LockScreenView.E_MOVE);
            return true;
        } else {
            return false;
        }
    }


    private void setTimeout(int time) {
        Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, time);
    }


    private void updateStatus(History history) {
        if (history != null) {
            if (history.getEvent().getEvent() == SENSOR_NEW) {
                int value = history.getStatus().getShortValue1();
                setCountdownViewVisible(value);
                return;
            }
            if (history.getEvent().getEvent() != SENSOR_ERROR) {
                textView_g_unit.setText(R.string.unit_mmol_l);
            }
            DateTime nowTime = new DateTime(Calendar.getInstance());
            DateTime statusTime = history.getDateTime();
            long time_space = nowTime.getCalendar().getTimeInMillis() - statusTime.getCalendar().getTimeInMillis();
            boolean a = (history.getEvent().getEventFlag() & 0x40) == 0;
            mLog.Error(getClass(), "血糖：" + (history.getStatus().getShortValue1() & 0xFFFF) + "valueflag：" + a);
            if (time_space / 1000 >= -60) {
                if (history.getEvent().getEvent() == SENSOR_EXPIRATION) {
                    ActivityMain.setStatus(history);
                    textView_g_err.setBackgroundResource(R.drawable.expirtion_err);
                    setGlucoseVisible(false);
                    return;
                }
                if (Math.abs(time_space) < 15 * 60 * 1000) {
                    ActivityMain.setStatus(history);
                    switch (history.getEvent().getEvent()) {
                        case SENSOR_ERROR:
                            textView_g_recommend_cal.setVisibility(View.GONE);
                            break;
                        case GLUCOSE:
                        case GLUCOSE_RECOMMEND_CAL:
                        case GLUCOSE_INVALID:
                        case HYPO:
                        case HYPER:
                        case IMPENDANCE:
                            if ((history.getEvent().getEventFlag() & 0x40) == 0) {
                                setGlucoseVisible(true);
                                switch (history.getStatus().getShortValue1()) {
                                    case 0:
                                        textView_g.setTextColor(Color.RED);
                                        textView_g.setText(R.string.low);
                                        break;
                                    case 255:
                                        textView_g.setTextColor(Color.RED);
                                        textView_g.setText(R.string.high);
                                        break;
                                    default:
                                        textView_g.setTextColor(getResources().getColor(R.color.green));
                                        textView_g.setText(getGlucoseValue((history.getStatus().getShortValue1() & 0xFFFF) *
                                                ActivityPDA.GLUCOSE_UNIT_MG_STEP, false).trim());
                                        break;
                                }

                                if (countDownTimer != null) {
                                    countDownTimer.cancel();
                                    countDownTimer = null;
                                }
                                countDownTimer = new CountDownTimer(15 * 60 * 1000 - time_space,
                                        15 * 60 * 1000 - time_space) {
                                    @Override
                                    public void onTick(long l) {

                                    }

                                    @Override
                                    public void onFinish() {
                                        if (!isFinishing()) {
                                            textView_g.setTextColor(getResources().getColor(R.color.green));
                                            textView_g_recommend_cal.setVisibility(View.GONE);
                                            textView_g.setText(STRING_UNKNOWN);
//                                        textView_t.setText("");
//                                        setGlucoseVisible(true);
                                            ActivityMain.setStatus(null);
                                        }
                                    }
                                }.start();

                                if ((history.getEvent().getEventFlag() & 0x80) != 0) {
                                    textView_g_recommend_cal.setVisibility(View.VISIBLE);
                                } else {
                                    textView_g_recommend_cal.setVisibility(View.GONE);
                                }
                            } else {
                                textView_g_recommend_cal.setVisibility(View.GONE);
//                            textView_g.setText(STRING_UNKNOWN);
//                            textView_t.setText("");
                            }
                            break;
                        default:
                            textView_g_recommend_cal.setVisibility(View.GONE);
                            break;
                    }
                    if ((history.getEvent().getEventFlag() & 0x20) != 0) {
                        textView_g_err.setBackgroundResource(R.drawable.glucose_err);
                        setGlucoseVisible(false);
                    } else {
                        textView_g.setTextColor(getResources().getColor(R.color.green));
                        setGlucoseVisible(true);
                    }
                }
            }
        }
    }


    private void updateCalendar() {
        long currentTime = System.currentTimeMillis();
        boolean format = getDataStorage(ActivityPDA.class.getSimpleName())
                .getBoolean(SETTING_TIME_FORMAT, true);
        String time1 = TimeUtil.getStatusTimeByTimeMillis(currentTime, "HH:mm");
        if (format) {
            TextView tv = (TextView) findViewById(R.id.text_view_time);
            tv.setText(time1);
        } else {
            String time2 = TimeUtil.getStatusTimeByTimeMillis(currentTime, "a");
            TextView tv = (TextView) findViewById(R.id.text_view_time);
            tv.setText(time1);
            tv = (TextView) findViewById(R.id.text_view_ampm);
            tv.setText(time2);
        }
        String time3 =
                TimeUtil.getStatusTimeByTimeMillis(currentTime, "EE, MMMM d");
        if (AndroidSystemInfoUtil.getLanguage().getLanguage().equals("zh")) {
            time3 += getString(R.string.day);
        }
        time3 += ", " + TimeUtil.getStatusTimeByTimeMillis(currentTime, "yyyy");
        if (AndroidSystemInfoUtil.getLanguage().getLanguage().equals("zh")) {
            time3 += getString(R.string.year);
        }
        TextView tv = (TextView) findViewById(R.id.text_view_date);
        tv.setText(time3);
    }
}
