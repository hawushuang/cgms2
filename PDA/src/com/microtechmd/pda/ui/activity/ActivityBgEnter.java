package com.microtechmd.pda.ui.activity;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
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
import com.microtechmd.pda.ui.widget.ConfirmDialog;
import com.microtechmd.pda.util.CalibrationSaveUtil;
import com.microtechmd.pda.util.MediaUtil;
import com.triggertrap.seekarc.SeekArc;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class ActivityBgEnter extends ActivityPDA {
    public static final String EXTRA_BG_AMT = "extra_bg_amt";
    public static final String EXTRA_IS_MANUAL = "extra_is_manual";

    private static final int GLUCOSE_MAX = 333 * GLUCOSE_UNIT_MMOL_STEP;
    private static final int GLUCOSE_MIN = GLUCOSE_UNIT_MMOL_STEP;

    private int mGlucose = 0;
    private EditText editTextGlucose;
    private SeekArc mSeekArc;
    private TextView calibrate_time_tv;

    private ConfirmDialog confirmDialog;
    private boolean mIsManual;
    private Calendar mCalendar;
//
//    private VirtualKeyboardView virtualKeyboardView;
//
//    private GridView gridView;
//
//    private ArrayList<Map<String, String>> valueList;
//
//    private Animation enterAnim;
//
//    private Animation exitAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_bg_enter);
//        setContentView(R.layout.seekbar);
        initAnim();
        mSeekArc = (SeekArc) findViewById(R.id.seekArc);
        calibrate_time_tv = (TextView) findViewById(R.id.calibrate_time_tv);
        editTextGlucose = (EditText) findViewById(R.id.edit_text_glucose);
        initialize(getIntent());
//        valueList = virtualKeyboardView.getValueList();
        mSeekArc.setTouchInSide(false);
        mSeekArc.setArcWidth(8);
        mSeekArc.setProgressWidth(8);
        mSeekArc.setArcColor(Color.GRAY);
        mSeekArc.setEnabled(false);
        mSeekArc.setMax(300);
        mSeekArc.setProgress(mGlucose);
//        mSeekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
//
//            @Override
//            public void onStopTrackingTouch(SeekArc seekArc) {
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekArc seekArc) {
//            }
//
//            @Override
//            public void onProgressChanged(SeekArc seekArc, int progress,
//                                          boolean fromUser) {
//                DecimalFormat df = new DecimalFormat("0.0");
//                editTextGlucose.setText(df.format((float) progress / (float) 10));
//            }
//        });
        editTextGlucose.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String progress = editable.toString();
                if (!TextUtils.isEmpty(progress)) {
                    try {
                        int glucose = (int) (Float.valueOf(progress) * GLUCOSE_UNIT_MG_STEP);
                        if (glucose <= 300) {
                            mSeekArc.setProgress(glucose);
                        } else {
                            editTextGlucose.setText(getGlucoseValue(300 * 10, false));
                            Toast.makeText(mBaseActivity, R.string.input_calibration_err, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(mBaseActivity, R.string.input_err, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(mBaseActivity, R.string.input_empty, Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    /**
     * 数字键盘显示动画
     */
    private void initAnim() {

//        enterAnim = AnimationUtils.loadAnimation(this, R.anim.bottomview_anim_enter);
//        exitAnim = AnimationUtils.loadAnimation(this, R.anim.bottomview_anim_exit);
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
        float value = Float.parseFloat(editTextGlucose.getText().toString());
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
        editTextGlucose.setLongClickable(false);
        // 设置不调用系统键盘
//        if (android.os.Build.VERSION.SDK_INT <= 10) {
//            editTextGlucose.setInputType(InputType.TYPE_NULL);
//        } else {
//            this.getWindow().setSoftInputMode(
//                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//            try {
//                Class<EditText> cls = EditText.class;
//                Method setShowSoftInputOnFocus;
//                setShowSoftInputOnFocus = cls.getMethod("setShowSoftInputOnFocus",
//                        boolean.class);
//                setShowSoftInputOnFocus.setAccessible(true);
//                setShowSoftInputOnFocus.invoke(editTextGlucose, false);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        virtualKeyboardView = (VirtualKeyboardView) findViewById(R.id.virtualKeyboardView);
//        virtualKeyboardView.setVisibility(View.GONE);
//        virtualKeyboardView.getLayoutBack().setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                virtualKeyboardView.startAnimation(exitAnim);
//                virtualKeyboardView.setVisibility(View.GONE);
//            }
//        });
//
//        gridView = virtualKeyboardView.getGridView();
//        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
//                if (position < 11 && position != 9) {    //点击0~9按钮
//
//                    String amount = editTextGlucose.getText().toString().trim();
//                    amount = amount + valueList.get(position).get("name");
//
//                    editTextGlucose.setText(amount);
//
//                    Editable ea = editTextGlucose.getText();
//                    editTextGlucose.setSelection(ea.length());
//                } else {
//
//                    if (position == 9) {      //点击退格键
//                        String amount = editTextGlucose.getText().toString().trim();
//                        if (!amount.contains(".")) {
//                            amount = amount + valueList.get(position).get("name");
//                            editTextGlucose.setText(amount);
//
//                            Editable ea = editTextGlucose.getText();
//                            editTextGlucose.setSelection(ea.length());
//                        }
//                    }
//
//                    if (position == 11) {      //点击退格键
//                        String amount = editTextGlucose.getText().toString().trim();
//                        if (amount.length() > 0) {
//                            amount = amount.substring(0, amount.length() - 1);
//                            editTextGlucose.setText(amount);
//
//                            Editable ea = editTextGlucose.getText();
//                            editTextGlucose.setSelection(ea.length());
//                        }
//                    }
//                }
//            }
//        });
//
//        editTextGlucose.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                virtualKeyboardView.setFocusable(true);
//                virtualKeyboardView.setFocusableInTouchMode(true);
//
//                virtualKeyboardView.startAnimation(enterAnim);
//                virtualKeyboardView.setVisibility(View.VISIBLE);
//            }
//        });
        mIsManual = intent.getBooleanExtra(EXTRA_IS_MANUAL, true);

        if (mIsManual) {
            editTextGlucose.setEnabled(true);
            mSeekArc.setEnabled(false);
            mGlucose = 5;
            mGlucose *= GLUCOSE_UNIT_MG_STEP;
            calibrate_time_tv.setVisibility(View.GONE);
        } else {
            mCalendar = Calendar.getInstance();
            editTextGlucose.setEnabled(false);
            mSeekArc.setEnabled(false);
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
            calibrate_time_tv.setText(getDateString(System.currentTimeMillis(),
                    null) + " " + getTimeString(System.currentTimeMillis(),
                    null));
            calibrate_time_tv.setVisibility(View.VISIBLE);
        }

        editTextGlucose.setText(getGlucoseValue(mGlucose * 10, false));
    }


    private void sendRecord() {
        if (TextUtils.isEmpty(editTextGlucose.getText().toString())) {
            Toast.makeText(mBaseActivity, R.string.input_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            mGlucose = (int) (Float.parseFloat(editTextGlucose.getText().toString()) *
                    100.0f);
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
        if (TextUtils.isEmpty(editTextGlucose.getText().toString())) {
            Toast.makeText(mBaseActivity, R.string.input_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            mGlucose = (int) (Float.parseFloat(editTextGlucose.getText().toString()) *
                    100.0f);
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
