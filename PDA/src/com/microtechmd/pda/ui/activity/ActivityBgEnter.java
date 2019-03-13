package com.microtechmd.pda.ui.activity;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.microtechmd.pda.R;
import com.microtechmd.pda.entity.CalibrationHistory;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ValueShort;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.ui.activity.fragment.FragmentDialog;
import com.microtechmd.pda.ui.activity.fragment.FragmentInput;
import com.microtechmd.pda.ui.activity.fragment.FragmentMessage;
import com.microtechmd.pda.ui.widget.RuleView;
import com.microtechmd.pda.ui.widget.contrarywind.adapter.ArrayWheelAdapter;
import com.microtechmd.pda.ui.widget.contrarywind.listener.OnItemSelectedListener;
import com.microtechmd.pda.ui.widget.contrarywind.view.WheelView;
import com.microtechmd.pda.util.CalibrationSaveUtil;
import com.microtechmd.pda.util.MediaUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ActivityBgEnter extends ActivityPDA {
    public static final String EXTRA_BG_AMT = "extra_bg_amt";
    public static final String EXTRA_IS_MANUAL = "extra_is_manual";

    private static final int GLUCOSE_MAX = 333 * GLUCOSE_UNIT_MMOL_STEP;
    private static final int GLUCOSE_MIN = GLUCOSE_UNIT_MMOL_STEP;

    private int mGlucose = 0;
    private TextView calibrate_time_tv;

    private boolean mIsManual;
    private Calendar mCalendar;

    private RuleView ruleView;
    private TextView tv_glucose;
    private Button button_add;
    private Button button_sub;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_bg_enter);

        ruleView = (RuleView) findViewById(R.id.ruleView);
        tv_glucose = (TextView) findViewById(R.id.tv_glucose);
        button_add = (Button) findViewById(R.id.button_add);
        button_sub = (Button) findViewById(R.id.button_sub);
        ruleView.setValue(0, 30, 5, 0.1F, 10);
        ruleView.setOnValueChangedListener(new RuleView.OnValueChangedListener() {
            @Override
            public void onValueChanged(float value) {
                tv_glucose.setText(String.valueOf(value));
                mGlucose = (int) (value * 100);
            }
        });
//        button_add.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        button_add.setBackgroundResource(R.drawable.jia_2);
//                        updateAddOrSubtract(v.getId());    //手指按下时触发不停的发送消息
//                        break;
//                    case MotionEvent.ACTION_MOVE:
////                        if (!isInView(v, event)) {
////                            button_add.setBackgroundResource(R.drawable.jia);
////                            stopAddOrSubtract();    //停止发送
////                        }
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        button_add.setBackgroundResource(R.drawable.jia);
//                        stopAddOrSubtract();    //手指抬起时停止发送
//                        break;
//                }
//                return true;
//            }
//        });
//        button_sub.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                    button_sub.setBackgroundResource(R.drawable.jian_2);
//                    updateAddOrSubtract(v.getId());    //手指按下时触发不停的发送消息
//                } else if (event.getAction() == MotionEvent.ACTION_UP) {
//                    button_sub.setBackgroundResource(R.drawable.jian);
//                    stopAddOrSubtract();    //手指抬起时停止发送
//                }
//                return true;
//            }
//        });
        calibrate_time_tv = (TextView) findViewById(R.id.calibrate_time_tv);

        initialize(getIntent());

    }

    /**
     * 判断触摸的点是否在View范围内
     */
    private boolean isInView(View v, MotionEvent event) {
        Rect frame = new Rect();
        v.getHitRect(frame);
        float eventX = event.getX();
        float eventY = event.getY();
        return frame.contains((int) eventX, (int) eventY);
    }

    private ScheduledExecutorService scheduledExecutor;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case R.id.button_add:
                    float current = ruleView.getCurrentValue();
                    current = (current * 10 + 1) / 10;
                    if (current >= 30) {
                        current = 30;
                    }
                    ruleView.setCurrentValue(current);
                    break;
                case R.id.button_sub:
                    float current2 = ruleView.getCurrentValue();
                    current2 = (current2 * 10 - 1) / 10;
                    if (current2 <= 0) {
                        current2 = 0;
                    }
                    ruleView.setCurrentValue(current2);
                    break;
                default:

                    break;
            }
        }
    };

    private void updateAddOrSubtract(int viewId) {
        final int vid = viewId;
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = vid;
                handler.sendMessage(msg);
            }
        }, 0, 300, TimeUnit.MILLISECONDS);    //每间隔100ms发送Message
    }

    private void stopAddOrSubtract() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
            scheduledExecutor = null;
        }
    }

    @Override
    protected void onClickView(View v) {
        super.onClickView(v);

        switch (v.getId()) {
            case R.id.ibt_back:
                finish();
                break;
            case R.id.button_calibrate:
                showCalibrateConfirmDialog();
                break;
            case R.id.button_record:
                showRecordConfirmDialog();
                break;
            case R.id.button_add:
                float current = ruleView.getCurrentValue();
                current = (current * 10 + 1) / 10;
                if (current >= 30) {
                    current = 30;
                }
                ruleView.setCurrentValue(current);
                break;
            case R.id.button_sub:
                float current2 = ruleView.getCurrentValue();
                current2 = (current2 * 10 - 1) / 10;
                if (current2 <= 0) {
                    current2 = 0;
                }
                ruleView.setCurrentValue(current2);
                break;

            default:
                break;
        }
    }

    private void showRecordConfirmDialog() {
        FragmentInput fragmentInput = new FragmentInput();
        fragmentInput
                .setComment(getString(R.string.calibrate_confirm));
        fragmentInput.setInputText(FragmentInput.POSITION_LEFT, null);
        fragmentInput.setInputText(FragmentInput.POSITION_RIGHT, null);
        fragmentInput.setInputText(FragmentInput.POSITION_CENTER, null);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_LEFT, null);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_RIGHT, null);
        showDialogConfirm(getString(R.string.record), "", "",
                fragmentInput, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                sendRecord();
                                break;

                            default:
                                break;
                        }

                        return true;
                    }
                });
    }

    private void showCalibrateConfirmDialog() {
        FragmentInput fragmentInput = new FragmentInput();
        fragmentInput
                .setComment(getString(R.string.record_calibrate_confirm));
        fragmentInput.setInputText(FragmentInput.POSITION_LEFT, null);
        fragmentInput.setInputText(FragmentInput.POSITION_RIGHT, null);
        fragmentInput.setInputText(FragmentInput.POSITION_CENTER, null);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_LEFT, null);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_RIGHT, null);
        showDialogConfirm(getString(R.string.fragment_calibrate), "", "",
                fragmentInput, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                sendCalibrate();
//                                record();
                                break;

                            default:
                                break;
                        }

                        return true;
                    }
                });
    }

    private void showDialogConfirm(String title, String buttonTextPositive,
                                   String buttonTextNegative, Fragment content,
                                   FragmentDialog.ListenerDialog listener) {
        final FragmentDialog fragmentDialog = new FragmentDialog();
        fragmentDialog.setHomeCancelFlag(true);
        fragmentDialog.setTitle(title);
        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
                buttonTextPositive);
        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
                buttonTextNegative);
        fragmentDialog.setContent(content);
        fragmentDialog.setListener(listener);
        fragmentDialog.show(this.getSupportFragmentManager(), null);
    }

    private void record() {
        List<CalibrationHistory> list = (List<CalibrationHistory>) CalibrationSaveUtil.get(this, CALIBRATION_HISTORY);
        if (list == null) {
            list = new ArrayList<>();
        }
        long time = Calendar.getInstance().getTimeInMillis();
        float value = Float.parseFloat(tv_glucose.getText().toString());
        CalibrationHistory calibrationHistory = new CalibrationHistory(time, value);
        list.add(calibrationHistory);
        CalibrationSaveUtil.save(this, CALIBRATION_HISTORY, list);

        showDialogLoading();
        new Handler().postDelayed(new Runnable() {
            public void run() {
                handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
                        ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_SET,
                        ParameterComm.RESET_DATA,
                        new byte[]{(byte) 1}));

                dismissDialogLoading();
                finish();
            }
        }, 500);
    }

    @Override
    protected void onHomePressed() {
        finish();
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_GLUCOSE,
                ParameterGlobal.PORT_GLUCOSE, EntityMessage.OPERATION_EVENT,
                ParameterGlucose.HOME_PRESS, null));
    }

    @Override
    protected void handleAcknowledgement(final EntityMessage message) {
        super.handleAcknowledgement(message);

        if ((message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER) &&
                (message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE)) {
            if (message.getParameter() == ParameterGlucose.TASK_GLUCOSE_PARAM_CALIBRATON || message.getParameter() == ParameterGlucose.TASK_GLUCOSE_PARAM_CALIBRATON) {
                if (!(message.getData()[0] == EntityMessage.FUNCTION_OK)) {
                    Toast.makeText(this, getResources().getString(R.string.calibrate_failed),
                            Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
            }

            if (message.getParameter() == ParameterGlucose.TASK_GLUCOSE_PARAM_CALIBRATON) {
                Toast.makeText(this, getResources().getString(R.string.calibration_done), Toast.LENGTH_SHORT).show();
                record();
            }
            if (message.getParameter() == ParameterGlucose.TASK_GLUCOSE_PARAM_GLUCOSE) {
                Toast.makeText(this, getResources().getString(R.string.record_done), Toast.LENGTH_SHORT).show();
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private void initialize(Intent intent) {
        mIsManual = intent.getBooleanExtra(EXTRA_IS_MANUAL, true);

        if (mIsManual) {
            mGlucose = 5;
            mGlucose *= GLUCOSE_UNIT_MG_STEP;
            calibrate_time_tv.setVisibility(View.GONE);
        } else {
            button_add.setEnabled(false);
            button_add.setBackgroundResource(R.drawable.jia_2);
            button_sub.setEnabled(false);
            button_sub.setBackgroundResource(R.drawable.jian_2);
            ruleView.setCanScroll(false);

            mCalendar = Calendar.getInstance();
            mGlucose = intent.getIntExtra(EXTRA_BG_AMT, 0);
            if ((mGlucose > GLUCOSE_MAX) || (mGlucose < GLUCOSE_MIN)) {
                MediaUtil.playMp3ByType(this, "beep_ack.mp3", false);
                int errorStringID;

                if (mGlucose > GLUCOSE_MAX) {
                    errorStringID = R.string.glucose_error_overflow;
                    mGlucose = GLUCOSE_MAX;
                } else {
                    errorStringID = R.string.glucose_error_underflow;
                    mGlucose = GLUCOSE_MIN;
                }

                FragmentMessage fragmentMessage = new FragmentMessage();
                fragmentMessage.setComment(getString(errorStringID));
                showDialogConfirm(getString(R.string.glucose_error_title),
                        "", null, fragmentMessage, false,
                        new FragmentDialog.ListenerDialog() {
                            @Override
                            public boolean onButtonClick(int buttonID,
                                                         Fragment content) {
                                switch (buttonID) {
                                    case FragmentDialog.BUTTON_ID_POSITIVE:
                                        finish();
                                        break;

                                    default:
                                        break;
                                }

                                return true;
                            }
                        });
            }

            mGlucose *= GLUCOSE_UNIT_MG_STEP;
            mGlucose /= GLUCOSE_UNIT_MMOL_STEP;
            calibrate_time_tv.setText(getDateString(System.currentTimeMillis(), null)
                    + " "
                    + getTimeString(System.currentTimeMillis(), null));
            calibrate_time_tv.setVisibility(View.VISIBLE);
        }
        String glucose = getGlucoseValue(mGlucose * 10, false);
        try {
            ruleView.setCurrentValue(Float.parseFloat(glucose));
        } catch (Exception e) {
            Toast.makeText(mBaseActivity, R.string.input_err, Toast.LENGTH_SHORT).show();
        }
    }


    private void sendRecord() {
//        if (TextUtils.isEmpty(editTextGlucose.getText().toString())) {
//            Toast.makeText(mBaseActivity, R.string.input_empty, Toast.LENGTH_SHORT).show();
//            return;
//        }
        try {
            float value_s = Float.parseFloat(tv_glucose.getText().toString());
            mGlucose = (int) (value_s * 100.0f);
            ValueShort value = new ValueShort((short) (mGlucose / GLUCOSE_UNIT_MG_STEP));
            DateTime dateTime;
            if (mIsManual) {
                dateTime = new DateTime(Calendar.getInstance());
            } else {
                dateTime = new DateTime(mCalendar);
            }
            byte[] send = new byte[6];
            System.arraycopy(dateTime.getByteArray(), 0, send, 0, 4);
            System.arraycopy(value.getByteArray(), 0, send, 4, 2);
            record();
            handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                    ParameterGlobal.ADDRESS_REMOTE_MASTER, ParameterGlobal.PORT_MONITOR,
                    ParameterGlobal.PORT_GLUCOSE, EntityMessage.OPERATION_SET,
                    ParameterGlucose.TASK_GLUCOSE_PARAM_GLUCOSE, send));
        } catch (Exception e) {
            Toast.makeText(mBaseActivity, R.string.input_err, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendCalibrate() {
//        if (TextUtils.isEmpty(editTextGlucose.getText().toString())) {
//            Toast.makeText(mBaseActivity, R.string.input_empty, Toast.LENGTH_SHORT).show();
//            return;
//        }
        try {
            float value_s = Float.parseFloat(tv_glucose.getText().toString());
            mGlucose = (int) (value_s * 100.0f);
            ValueShort value = new ValueShort((short) (mGlucose / GLUCOSE_UNIT_MG_STEP));
            DateTime dateTime = new DateTime(Calendar.getInstance());
            byte[] send = new byte[6];
            System.arraycopy(dateTime.getByteArray(), 0, send, 0, 4);
            System.arraycopy(value.getByteArray(), 0, send, 4, 2);
            handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                    ParameterGlobal.ADDRESS_REMOTE_MASTER, ParameterGlobal.PORT_MONITOR,
                    ParameterGlobal.PORT_GLUCOSE, EntityMessage.OPERATION_SET,
                    ParameterGlucose.TASK_GLUCOSE_PARAM_CALIBRATON, send));
        } catch (Exception e) {
            Toast.makeText(mBaseActivity, R.string.input_err, Toast.LENGTH_SHORT).show();
        }
    }
}
