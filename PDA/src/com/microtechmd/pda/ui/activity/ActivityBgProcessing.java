package com.microtechmd.pda.ui.activity;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.TextView;

import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ValueShort;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.ui.activity.fragment.FragmentDialog;
import com.microtechmd.pda.ui.activity.fragment.FragmentMessage;
import com.microtechmd.pda.util.MediaUtil;

import static com.microtechmd.pda.ui.activity.ActivityBgEnter.EXTRA_BG_MODE;


public class ActivityBgProcessing extends ActivityPDA {
    private int mode;

    @Override
    public void onBackPressed() {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_bg_processing);
        mode = getIntent().getIntExtra(EXTRA_BG_MODE, 0);
        ((ApplicationPDA) getApplication()).registerMessageListener(
                ParameterGlobal.PORT_GLUCOSE, mMessageListener);
        if (ActivityUnlockNew.instance != null) {
            ActivityUnlockNew.instance.finish();
        }
    }


    @Override
    protected void onDestroy() {
        ((ApplicationPDA) getApplication()).unregisterMessageListener(
                ParameterGlobal.PORT_GLUCOSE, mMessageListener);
        super.onDestroy();
    }


    @Override
    protected void onHomePressed() {
    }


    @Override
    protected void handleNotification(final EntityMessage message) {
        super.handleNotification(message);

        if ((message
                .getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) &&
                (message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE)) {
            switch (message.getParameter()) {
                case ParameterGlucose.PARAM_COUNT_DOWN:
                    TextView tv = (TextView) findViewById(R.id.tv_progress);
                    tv.setText(message.getData()[0] + "");
                    break;

                case ParameterGlucose.PARAM_GLUCOSE:
                    Intent intent =
                            new Intent(mBaseActivity, ActivityBgEnter.class);
                    intent.putExtra(ActivityBgEnter.EXTRA_IS_MANUAL,
                            false);
                    intent.putExtra(ActivityBgEnter.EXTRA_BG_AMT,
                            (int) (new ValueShort(message.getData()).getValue() &
                                    0xFFFF));
                    intent.putExtra(EXTRA_BG_MODE, mode);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
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
}
