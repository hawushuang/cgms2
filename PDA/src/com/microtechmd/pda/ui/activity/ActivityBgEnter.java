package com.microtechmd.pda.ui.activity;


import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ValueShort;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.ui.activity.fragment.FragmentDialog;
import com.microtechmd.pda.ui.activity.fragment.FragmentMessage;
import com.microtechmd.pda.util.MediaUtil;
import com.triggertrap.seekarc.SeekArc;

import java.text.DecimalFormat;


public class ActivityBgEnter extends ActivityPDA {
    public static final String EXTRA_BG_AMT = "extra_bg_amt";
    public static final String EXTRA_IS_MANUAL = "extra_is_manual";

    private static final int GLUCOSE_MAX = 333 * GLUCOSE_UNIT_MMOL_STEP;
    private static final int GLUCOSE_MIN = GLUCOSE_UNIT_MMOL_STEP;

    private int mGlucose = 0;
    private EditText editTextGlucose;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_bg_enter);
//        setContentView(R.layout.seekbar);
        SeekArc mSeekArc = (SeekArc) findViewById(R.id.seekArc);
        initialize(getIntent());

        mSeekArc.setArcWidth(30);
        mSeekArc.setProgressWidth(28);
        mSeekArc.setArcColor(Color.GRAY);
        mSeekArc.setMax(3000);
        mSeekArc.setProgress(mGlucose * 10);
        mSeekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekArc seekArc) {
            }

            @Override
            public void onStartTrackingTouch(SeekArc seekArc) {
            }

            @Override
            public void onProgressChanged(SeekArc seekArc, int progress,
                                          boolean fromUser) {
                DecimalFormat df = new DecimalFormat("0.00");
                editTextGlucose.setText(df.format((float) progress / (float) 100));
            }
        });
    }


    @Override
    protected void onClickView(View v) {
        super.onClickView(v);

        switch (v.getId()) {
            case R.id.button_calibrate:
                calibrate();
                //finish();
                break;

            case R.id.button_cancel:
                finish();
                break;

            default:
                break;
        }
    }


    @Override
    protected void handleAcknowledgement(final EntityMessage message) {
        super.handleAcknowledgement(message);

        if ((message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER) &&
                (message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE)) {
            if (message.getParameter() == ParameterGlucose.PARAM_GLUCOSE) {
                finish();
            }
        }
    }


    private void initialize(Intent intent) {
        editTextGlucose = (EditText) findViewById(R.id.edit_text_glucose);
        boolean mIsManual = intent.getBooleanExtra(EXTRA_IS_MANUAL, true);

        if (mIsManual) {
            editTextGlucose.setEnabled(true);
            mGlucose = 5;
            mGlucose *= GLUCOSE_UNIT_MG_STEP;
        } else {
            editTextGlucose.setEnabled(false);
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
        }

        editTextGlucose.setText(getGlucoseValue(mGlucose * 100, false));
    }


    private void calibrate() {
        EditText editTextGlucose = (EditText) findViewById(R.id.edit_text_glucose);
        mGlucose = (int) (Float.parseFloat(editTextGlucose.getText().toString()) *
                1000.0f);
        ValueShort value = new ValueShort((short) (mGlucose / GLUCOSE_UNIT_MG_STEP));
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_REMOTE_MASTER, ParameterGlobal.PORT_MONITOR,
                ParameterGlobal.PORT_GLUCOSE, EntityMessage.OPERATION_SET,
                ParameterGlucose.PARAM_GLUCOSE, value.getByteArray()));
    }
}
