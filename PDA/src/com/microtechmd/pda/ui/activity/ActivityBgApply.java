package com.microtechmd.pda.ui.activity;


import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.ui.activity.fragment.FragmentDialog;
import com.microtechmd.pda.ui.activity.fragment.FragmentMessage;
import com.microtechmd.pda.util.MediaUtil;

import java.util.Arrays;


public class ActivityBgApply extends ActivityPDA {
    private int mode;
    private int mode_set;

    private Button button_calibration_switch;
    private ImageView img;
    private TextView text_view_calibration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_bg_apply);
        ((ApplicationPDA) getApplication()).registerMessageListener(
                ParameterGlobal.PORT_GLUCOSE, mMessageListener);
        text_view_calibration = (TextView) findViewById(R.id.text_view_calibration);
        img = (ImageView) findViewById(R.id.image_view_calibration);
        button_calibration_switch = (Button) findViewById(R.id.button_calibration_switch);
        initBloodImgs();

        Handler mHandler = new Handler();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                getControl();
            }
        });
    }


    @Override
    protected void onDestroy() {
        ((ApplicationPDA) getApplication()).unregisterMessageListener(
                ParameterGlobal.PORT_GLUCOSE, mMessageListener);
        super.onDestroy();
    }

    @Override
    protected void onClickView(View v) {
        super.onClickView(v);

        switch (v.getId()) {
            case R.id.button_calibration_switch:
                switch (mode) {
                    case 0:
                        setControl(1);
                        mode_set = 1;
                        break;
                    case 1:
                        setControl(0);
                        mode_set = 0;
                        break;
                }
                break;

            default:
                break;
        }
    }

    @Override
    protected void handleAcknowledgement(EntityMessage message) {
        super.handleAcknowledgement(message);
        if ((message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) &&
                (message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE)) {
            switch (message.getParameter()) {
                case ParameterGlucose.PARAM_CONTROL:
                    mode = mode_set;
                    switch (mode) {
                        case 0:
                            initBloodImgs();
                            text_view_calibration.setText(R.string.actions_bg_apply_blood);
                            button_calibration_switch.setText(R.string.switchto_control_calibration);
                            break;
                        case 1:
                            initControlImgs();
                            text_view_calibration.setText(R.string.actions_bg_apply_control);
                            button_calibration_switch.setText(R.string.switchto_blood_calibration);
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void handleNotification(final EntityMessage message) {
        super.handleNotification(message);

        if ((message
                .getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) &&
                (message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE)) {
            switch (message.getParameter()) {
                case ParameterGlucose.PARAM_CONTROL:
                    mode = (int) message.getData()[0];
                    switch (mode) {
                        case 0:
                            initBloodImgs();
                            text_view_calibration.setText(R.string.actions_bg_apply_blood);
                            button_calibration_switch.setText(R.string.switchto_control_calibration);
                            break;
                        case 1:
                            initControlImgs();
                            text_view_calibration.setText(R.string.actions_bg_apply_control);
                            button_calibration_switch.setText(R.string.switchto_blood_calibration);
                            break;
                    }
                    break;
                case ParameterGlucose.PARAM_SIGNAL_PRESENT:
                    break;

                case ParameterGlucose.PARAM_COUNT_DOWN:

                    if (message.getData()[0] == 5) {
                        Intent intent = new Intent(mBaseActivity,
                                ActivityBgProcessing.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }

                    finish();
                    break;

                case ParameterGlucose.PARAM_ERROR:
                    MediaUtil.playMp3ByType(this, "beep_ack.mp3", false);
                    int errorStringID = R.string.glucose_error_unknown;

                    switch (message.getData()[0]) {
                        case ParameterGlucose.ERROR_CODE:
                            errorStringID = R.string.glucose_error_code;
                            break;

                        case ParameterGlucose.ERROR_CHANNEL:
                            errorStringID = R.string.glucose_error_channel;
                            break;

                        case ParameterGlucose.ERROR_NBB:
                            errorStringID = R.string.glucose_error_nbb;
                            break;

                        case ParameterGlucose.ERROR_TEMPERATURE:
                            errorStringID = R.string.glucose_error_temperature;
                            break;

                        case ParameterGlucose.ERROR_BLOOD_FILLING:
                            errorStringID =
                                    R.string.glucose_error_blood_filling;
                            break;

                        case ParameterGlucose.ERROR_BLOOD_NOT_ENOUGH:
                            errorStringID =
                                    R.string.glucose_error_blood_not_enough;
                            break;

                        case ParameterGlucose.ERROR_STRIP:
                            errorStringID = R.string.glucose_error_strip;
                            break;

                        default:
                            break;
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

                    break;

                default:
                    break;
            }
        }
    }

    //value为1是控制液模式，value为0是普通血液模式
    public void setControl(int value) {
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_GLUCOSE,
                ParameterGlobal.PORT_GLUCOSE, EntityMessage.OPERATION_SET,
                ParameterGlucose.PARAM_CONTROL, new byte[]
                {
                        (byte) value
                }));
    }

    public void getControl() {
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_GLUCOSE,
                ParameterGlobal.PORT_GLUCOSE, EntityMessage.OPERATION_GET,
                ParameterGlucose.PARAM_CONTROL, null));
    }

    private void initBloodImgs() {
        img.setBackgroundResource(R.drawable.animation_calibration);
        final AnimationDrawable frameAnimation =
                (AnimationDrawable) img.getBackground();
        frameAnimation.setOneShot(false);
        img.setBackgroundDrawable(frameAnimation);
        img.post(new Runnable() {
            @Override
            public void run() {
                frameAnimation.start();
            }
        });
    }

    private void initControlImgs() {
        img.setBackgroundResource(R.drawable.animation_glucose_control);
        final AnimationDrawable frameAnimation =
                (AnimationDrawable) img.getBackground();
        frameAnimation.setOneShot(false);
        img.setBackgroundDrawable(frameAnimation);
        img.post(new Runnable() {
            @Override
            public void run() {
                frameAnimation.start();
            }
        });
    }


    @Override
    protected void onHomePressed() {

    }

    @Override
    public void onBackPressed() {
    }

}
