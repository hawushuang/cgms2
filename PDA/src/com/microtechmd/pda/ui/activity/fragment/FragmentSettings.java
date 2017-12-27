package com.microtechmd.pda.ui.activity.fragment;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.ValueShort;
import com.microtechmd.pda.library.entity.comm.RFAddress;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.ui.activity.ActivityHistoryLog;
import com.microtechmd.pda.ui.activity.ActivityMain;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.ui.widget.WidgetSettingItem;
import com.microtechmd.pda.util.DataCleanUtil;
import com.microtechmd.pda.util.TimeUtil;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Locale;


public class FragmentSettings extends FragmentBase
        implements
        EntityMessage.Listener {
    public static final String SETTING_HYPER = "hyper";
    public static final String SETTING_HYPO = "hypo";

    private static final int HYPER_DEFAULT = 1200;
    private static final int HYPER_MAX = 2500;
    private static final int HYPER_MIN = 800;
    private static final int HYPO_DEFAULT = 350;
    private static final int HYPO_MAX = 500;
    private static final int HYPO_MIN = 200;

    private boolean mIsProgressNotShow = false;
    private boolean mIsProgressNotDismiss = false;

    private static final int QUERY_STATE_CYCLE = 1000;
    private static final int QUERY_STATE_TIMEOUT = 10000;

    private BroadcastReceiver mBroadcastReceiver = null;
    private boolean mIsRFStateChecking = false;
    private int mQueryStateTimeout = 0;
    private int mHyper = HYPER_DEFAULT;
    private int mHypo = HYPO_DEFAULT;
    private String mRFAddress = "";
    private View mRootView = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView =
                inflater.inflate(R.layout.fragment_settings, container, false);

        updateDateTimeSetting(true);
        mRFAddress = getAddress(((ActivityPDA) getActivity())
                .getDataStorage(ActivityPDA.class.getSimpleName())
                .getExtras(ActivityPDA.SETTING_RF_ADDRESS, null));

        ((WidgetSettingItem) mRootView.findViewById(R.id.item_pairing))
                .setItemValue(mRFAddress);
        mHyper = ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettings.class.getSimpleName())
                .getInt(SETTING_HYPER, HYPER_DEFAULT);
        mHypo = ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettings.class.getSimpleName())
                .getInt(SETTING_HYPO, HYPO_DEFAULT);
        updateHyper(mHyper);
        updateHypo(mHypo);

        return mRootView;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(Intent.ACTION_TIME_TICK)) {
                    if (getActivity() != null) {
                        updateDateTimeSetting(true);
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        getActivity().registerReceiver(mBroadcastReceiver, intentFilter);
        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_COMM, this);
        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_GLUCOSE, this);

        if (Calendar.getInstance()
                .get(Calendar.YEAR) < ActivityPDA.YEAR_MIN) {
            setTime();
            setDate();
        }
    }


    @Override
    public void onDestroyView() {
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_COMM, this);
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_GLUCOSE, this);
        getActivity().unregisterReceiver(mBroadcastReceiver);
        super.onDestroyView();
    }


    @Override
    public void onClick(View v) {
        super.onClick(v);

        switch (v.getId()) {
            case R.id.item_date:
                setDate();
                break;

            case R.id.item_time:
                setTime();
                break;

            case R.id.item_pairing:
                setTransmitterID();
                break;

            case R.id.item_hi_bg:
                setHyper();
                break;

            case R.id.item_lo_bg:
                setHypo();
                break;

            case R.id.item_language:
                setLanguage();
                break;

            case R.id.item_history:
                startActivity(new Intent(getActivity(), ActivityHistoryLog.class));
                break;

            case R.id.item_recovery:
                recovery();
                break;

            default:
                break;
        }
    }


    @Override
    public void onReceive(EntityMessage message) {
        switch (message.getOperation()) {
            case EntityMessage.OPERATION_SET:
                break;

            case EntityMessage.OPERATION_GET:
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


    protected void handleEvent(EntityMessage message) {
        switch (message.getEvent()) {
            case EntityMessage.EVENT_SEND_DONE:
                break;

            case EntityMessage.EVENT_ACKNOWLEDGE:
                break;

            case EntityMessage.EVENT_TIMEOUT:

                if (mIsProgressNotDismiss) {
                    if (!mRFAddress.equals(RFAddress.RF_ADDRESS_UNPAIR)) {
                        pair(RFAddress.RF_ADDRESS_UNPAIR);
                    } else {
                        mRFAddress = getAddress(((ActivityPDA) getActivity())
                                .getDataStorage(ActivityPDA.class
                                        .getSimpleName())
                                .getExtras(
                                        ActivityPDA.SETTING_RF_ADDRESS,
                                        null));
                    }
                }

                mIsProgressNotDismiss = false;
                dismissDialogProgress();
                break;

            default:
                break;
        }
    }


    protected void handleNotification(EntityMessage message) {
        if (message.getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_CONTROL) {
            if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
                    (message.getParameter() == ParameterComm.PARAM_RF_STATE)) {
                if (message.getData()[0] != ParameterComm.RF_STATE_IDLE) {
                    if (mIsRFStateChecking) {
                        mIsRFStateChecking = false;

                        // Set address
                        ((ActivityPDA) getActivity())
                                .handleMessage(new EntityMessage(
                                        ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                        ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                        ParameterGlobal.PORT_COMM,
                                        ParameterGlobal.PORT_COMM,
                                        EntityMessage.OPERATION_SET,
                                        ParameterComm.PARAM_RF_REMOTE_ADDRESS,
                                        new RFAddress(mRFAddress).getByteArray()));
                    }
                } else {
                    if (mQueryStateTimeout < QUERY_STATE_TIMEOUT) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mIsRFStateChecking) {
                                    mQueryStateTimeout += QUERY_STATE_CYCLE;
                                    checkRFState();
                                }
                            }
                        }, QUERY_STATE_CYCLE);
                    } else {
                        if (mIsRFStateChecking) {
                            mLog.Debug(ActivityPDA.class,
                                    "No RF signal");

                            mIsRFStateChecking = false;
                            pair(RFAddress.RF_ADDRESS_UNPAIR);
                            mIsProgressNotDismiss = false;
                            dismissDialogProgress();
                        }
                    }
                }
            }
        }
    }


    protected void handleAcknowledgement(final EntityMessage message) {
        if (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER) {
            if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
                    (message
                            .getParameter() == ParameterComm.PARAM_RF_REMOTE_ADDRESS)) {
                mLog.Debug(getClass(), "Set remote address success!");

                if (mRFAddress.equals(RFAddress.RF_ADDRESS_UNPAIR)) {
                    // Unpair PDA
                    /*
                     * ((ActivityPDA)getActivity()).handleMessage( new
					 * EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
					 * ParameterGlobal.ADDRESS_REMOTE_SLAVE,
					 * ParameterGlobal.PORT_COMM, ParameterGlobal.PORT_COMM,
					 * EntityMessage.OPERATION_SET,
					 * ParameterComm.PARAM_RF_LOCAL_ADDRESS, new
					 * RFAddress(RFAddress.RF_ADDRESS_UNPAIR) .getByteArray()));
					 */
                    pair(RFAddress.RF_ADDRESS_UNPAIR);
                } else {
                    ((WidgetSettingItem) mRootView
                            .findViewById(R.id.item_pairing))
                            .setItemValue(mRFAddress.toUpperCase());
                    ((ActivityPDA) getActivity()).handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                    ParameterGlobal.PORT_GLUCOSE,
                                    ParameterGlobal.PORT_GLUCOSE,
                                    EntityMessage.OPERATION_SET,
                                    ParameterGlucose.PARAM_BG_LIMIT,
                                    new ValueShort((short) mHyper).getByteArray()));
                    ((ActivityPDA) getActivity()).handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                    ParameterGlobal.PORT_GLUCOSE,
                                    ParameterGlobal.PORT_GLUCOSE,
                                    EntityMessage.OPERATION_SET,
                                    ParameterGlucose.PARAM_FILL_LIMIT,
                                    new ValueShort((short) mHypo).getByteArray()));
                    ((ActivityPDA) getActivity())
                            .getDataStorage(ActivityMain.class.getSimpleName())
                            .setLong(ActivityMain.SETTING_STARTUP_TIME,
                                    System.currentTimeMillis());
                    mIsProgressNotDismiss = false;
                    dismissDialogProgress();
                }
            }

            if (message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE) {
                if (message.getParameter() == ParameterGlucose.PARAM_FILL_LIMIT) {
                    mLog.Debug(getClass(), "Set hypo success!");
                    updateHypo(mHypo);
                }

                if (message.getParameter() == ParameterGlucose.PARAM_BG_LIMIT) {
                    mLog.Debug(getClass(), "Set hyper success!");
                    updateHyper(mHyper);
                }
            }
        }

        if (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) {
            if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
                    (message
                            .getParameter() == ParameterComm.PARAM_RF_LOCAL_ADDRESS)) {
                mLog.Debug(getClass(), "Set local address success!");

                if (!mRFAddress.equals(RFAddress.RF_ADDRESS_UNPAIR)) {
                    mIsRFStateChecking = true;
                    mQueryStateTimeout = 0;
                    checkRFState();
                } else {
                    mIsProgressNotDismiss = false;
                    dismissDialogProgress();
                    mRFAddress = "";
                    ((WidgetSettingItem) mRootView
                            .findViewById(R.id.item_pairing))
                            .setItemValue(mRFAddress);
                    ((ActivityPDA) getActivity())
                            .getDataStorage(
                                    ActivityPDA.class.getSimpleName())
                            .setExtras(
                                    ActivityPDA.SETTING_RF_ADDRESS,
                                    null);
                    ((ActivityPDA) getActivity()).handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                    ParameterGlobal.PORT_COMM,
                                    ParameterGlobal.PORT_COMM,
                                    EntityMessage.OPERATION_SET,
                                    ParameterComm.PARAM_RF_BROADCAST_SWITCH, new byte[]
                                    {
                                            (byte) 0
                                    }));
                    ((ActivityPDA) getActivity()).handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                    ParameterGlobal.PORT_MONITOR,
                                    ParameterGlobal.PORT_MONITOR,
                                    EntityMessage.OPERATION_NOTIFY,
                                    ParameterMonitor.PARAM_HISTORY,
                                    new History(new DateTime(Calendar.getInstance()),
                                            new Status(0, 0, 0, 0),
                                            new Event(0, ParameterGlobal.PORT_MONITOR,
                                                    ParameterMonitor.EVENT_PDA_BATTERY,
                                                    Event.URGENCY_NOTIFICATION, 0))
                                            .getByteArray()));
                    ActivityMain.setStatus(null);
                }
            }
        }
    }


    private void pair(String addressString) {
        mRFAddress = addressString;

        if (mRFAddress.equals(RFAddress.RF_ADDRESS_UNPAIR)) {
            ((ActivityPDA) getActivity())
                    .getDataStorage(
                            ActivityPDA.class.getSimpleName())
                    .setExtras(ActivityPDA.SETTING_RF_ADDRESS, null);
        } else {
            ((ActivityPDA) getActivity())
                    .getDataStorage(
                            ActivityPDA.class.getSimpleName())
                    .setExtras(ActivityPDA.SETTING_RF_ADDRESS,
                            new RFAddress(mRFAddress).getByteArray());
        }

        // Set remote address
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                        ParameterComm.PARAM_RF_LOCAL_ADDRESS,
                        new RFAddress(addressString).getByteArray()));

        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_MODEL, ParameterGlobal.PORT_COMM,
                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                        ParameterComm.PARAM_RF_REMOTE_ADDRESS,
                        new RFAddress(addressString).getByteArray()));
    }


    private void checkRFState() {
        mLog.Debug(getClass(), "Check RF state");

        if (getActivity() != null) {
            ((ActivityPDA) getActivity()).handleMessage(
                    new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                            ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                            ParameterGlobal.PORT_COMM, ParameterGlobal.PORT_COMM,
                            EntityMessage.OPERATION_GET, ParameterComm.PARAM_RF_STATE,
                            null));
        }
    }


    private void showDialogProgress() {
        if (!mIsProgressNotShow) {
            ((ActivityPDA) getActivity()).showDialogProgress();
        }
    }


    private void dismissDialogProgress() {
        if (!mIsProgressNotDismiss) {
            ((ActivityPDA) getActivity()).dismissDialogProgress();
        }
    }


    private void showDialogConfirm(String title, String buttonTextPositive,
                                   String buttonTextNegative, Fragment content,
                                   FragmentDialog.ListenerDialog listener) {
        FragmentDialog fragmentDialog = new FragmentDialog();
        fragmentDialog.setTitle(title);
        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
                buttonTextPositive);
        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
                buttonTextNegative);
        fragmentDialog.setContent(content);
        fragmentDialog.setListener(listener);
        fragmentDialog.show(getChildFragmentManager(), null);
    }


    private void setDate() {
        final FragmentInput fragmentInput = new FragmentInput();
        Calendar calendar = Calendar.getInstance();
        fragmentInput.setInputText(FragmentInput.POSITION_LEFT,
                calendar.get(Calendar.YEAR) + "");
        fragmentInput.setInputText(FragmentInput.POSITION_CENTER,
                calendar.get(Calendar.MONTH) + 1 + "");
        fragmentInput.setInputText(FragmentInput.POSITION_RIGHT,
                calendar.get(Calendar.DAY_OF_MONTH) + "");

        for (int i = 0; i < FragmentInput.COUNT_POSITION; i++) {
            fragmentInput.setInputType(i, InputType.TYPE_CLASS_NUMBER);
        }

        fragmentInput.setSeparatorText(FragmentInput.POSITION_LEFT, "-");
        fragmentInput.setSeparatorText(FragmentInput.POSITION_RIGHT, "-");
        showDialogConfirm(getString(R.string.setting_general_set_date), "", "",
                fragmentInput, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                Calendar calendar = Calendar.getInstance();
                                calendar.set(Calendar.YEAR,
                                        Integer.parseInt(fragmentInput.getInputText(
                                                FragmentInput.POSITION_LEFT)));
                                calendar
                                        .set(Calendar.MONTH,
                                                Integer
                                                        .parseInt(fragmentInput.getInputText(
                                                                FragmentInput.POSITION_CENTER)) -
                                                        1);
                                calendar.set(Calendar.DAY_OF_MONTH,
                                        Integer.parseInt(fragmentInput.getInputText(
                                                FragmentInput.POSITION_RIGHT)));
                                SystemClock.setCurrentTimeMillis(
                                        calendar.getTimeInMillis());
                                updateDateTimeSetting(true);
                                break;

                            default:
                                break;
                        }

                        return true;
                    }
                });
    }


    private void setTime() {
        final FragmentInput fragmentInput = new FragmentInput();
        Calendar calendar = Calendar.getInstance();
        fragmentInput.setInputText(FragmentInput.POSITION_LEFT,
                calendar.get(Calendar.HOUR_OF_DAY) + "");
        fragmentInput.setInputText(FragmentInput.POSITION_CENTER,
                calendar.get(Calendar.MINUTE) + "");
        fragmentInput.setInputType(FragmentInput.POSITION_LEFT,
                InputType.TYPE_CLASS_NUMBER);
        fragmentInput.setInputType(FragmentInput.POSITION_CENTER,
                InputType.TYPE_CLASS_NUMBER);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_LEFT, ":");
        showDialogConfirm(getString(R.string.setting_general_set_time), "", "",
                fragmentInput, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                int hour = Integer.parseInt(fragmentInput
                                        .getInputText(FragmentInput.POSITION_LEFT));
                                int minute = Integer.parseInt(fragmentInput
                                        .getInputText(FragmentInput.POSITION_CENTER));
                                Calendar calendar = Calendar.getInstance();
                                calendar.set(Calendar.MINUTE, minute);
                                calendar.set(Calendar.HOUR_OF_DAY, hour);
                                SystemClock.setCurrentTimeMillis(
                                        calendar.getTimeInMillis());
                                updateDateTimeSetting(true);
                                break;

                            default:
                                break;
                        }
                        return true;
                    }
                });
    }


    private void setTransmitterID() {
        final FragmentInput fragmentInput = new FragmentInput();

        if ((mRFAddress.equals("")) ||
                (mRFAddress.equals(RFAddress.RF_ADDRESS_UNPAIR))) {
            fragmentInput.setInputText(FragmentInput.POSITION_CENTER,
                    mRFAddress);
            fragmentInput.setInputType(FragmentInput.POSITION_LEFT,
                    InputType.TYPE_CLASS_TEXT |
                            InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            fragmentInput.setInputWidth(FragmentInput.POSITION_CENTER, 150);
            showDialogConfirm(getString(R.string.fragment_settings_pairing), "",
                    "", fragmentInput, new FragmentDialog.ListenerDialog() {
                        @Override
                        public boolean onButtonClick(int buttonID, Fragment content) {
                            switch (buttonID) {
                                case FragmentDialog.BUTTON_ID_POSITIVE:
                                    String address = fragmentInput.getInputText(
                                            FragmentInput.POSITION_CENTER);

                                    if ((address.trim().length() != 6) ||
                                            (address.trim()
                                                    .equals(RFAddress.RF_ADDRESS_UNPAIR))) {
                                        Toast.makeText(getActivity(),
                                                R.string.actions_pump_id_blank,
                                                Toast.LENGTH_SHORT).show();
                                        return false;
                                    } else {
                                        mIsProgressNotDismiss = true;
                                        showDialogProgress();
                                        pair(address.trim());
                                    }

                                    break;

                                default:
                                    break;
                            }

                            return true;
                        }
                    });
        } else {
            fragmentInput
                    .setComment(getString(R.string.fragment_settings_unpair));
            showDialogConfirm(getString(R.string.fragment_settings_pairing), "",
                    "", fragmentInput, new FragmentDialog.ListenerDialog() {
                        @Override
                        public boolean onButtonClick(int buttonID, Fragment content) {
                            switch (buttonID) {
                                case FragmentDialog.BUTTON_ID_POSITIVE:
                                    mRFAddress = RFAddress.RF_ADDRESS_UNPAIR;
                                    mIsProgressNotDismiss = true;
                                    showDialogProgress();
                                    ((ActivityPDA) getActivity())
                                            .handleMessage(new EntityMessage(
                                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                                    ParameterGlobal.PORT_COMM,
                                                    ParameterGlobal.PORT_COMM,
                                                    EntityMessage.OPERATION_SET,
                                                    ParameterComm.PARAM_RF_REMOTE_ADDRESS,
                                                    new RFAddress(mRFAddress)
                                                            .getByteArray()));
                                    break;

                                default:
                                    break;
                            }
                            return true;
                        }
                    });
        }
    }


    private void setHyper() {
        mHyper = ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettings.class.getSimpleName())
                .getInt(SETTING_HYPER, HYPER_DEFAULT);
        final FragmentInput fragmentInput = new FragmentInput();
        fragmentInput.setInputText(FragmentInput.POSITION_CENTER,
                new DecimalFormat("0.0").format((double) mHyper / 100));
        fragmentInput.setInputType(FragmentInput.POSITION_CENTER,
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_RIGHT,
                getString(R.string.unit_mmol_l));
        showDialogConfirm(getString(R.string.fragment_settings_hi_bg_threshold),
                "", "", fragmentInput, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                mHyper = (int) (Float.parseFloat(fragmentInput
                                        .getInputText(FragmentInput.POSITION_CENTER)
                                        .toString()) * 100.0f);

                                if ((mHyper > HYPER_MAX) || (mHyper < HYPER_MIN)) {
                                    Toast.makeText(getActivity(),
                                            R.string.fragment_settings_hyper_error,
                                            Toast.LENGTH_SHORT).show();
                                    return false;
                                } else {
                                    if ((!mRFAddress.equals("")) && (!mRFAddress
                                            .equals(RFAddress.RF_ADDRESS_UNPAIR))) {
                                        ((ActivityPDA) getActivity())
                                                .handleMessage(new EntityMessage(
                                                        ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                        ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                                        ParameterGlobal.PORT_GLUCOSE,
                                                        ParameterGlobal.PORT_GLUCOSE,
                                                        EntityMessage.OPERATION_SET,
                                                        ParameterGlucose.PARAM_BG_LIMIT,
                                                        new ValueShort((short) mHyper)
                                                                .getByteArray()));
                                    } else {
                                        updateHyper(mHyper);
                                    }
                                }

                                break;

                            default:
                                break;
                        }

                        return true;
                    }
                });
    }


    private void setHypo() {
        mHypo = ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettings.class.getSimpleName())
                .getInt(SETTING_HYPO, HYPO_DEFAULT);
        final FragmentInput fragmentInput = new FragmentInput();
        fragmentInput.setInputText(FragmentInput.POSITION_CENTER,
                new DecimalFormat("0.0").format((double) mHypo / 100));
        fragmentInput.setInputType(FragmentInput.POSITION_CENTER,
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_RIGHT,
                getString(R.string.unit_mmol_l));
        showDialogConfirm(getString(R.string.fragment_settings_lo_bg_threshold),
                "", "", fragmentInput, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                mHypo = (int) (Float.parseFloat(fragmentInput
                                        .getInputText(FragmentInput.POSITION_CENTER)
                                        .toString()) * 100.0f);

                                if ((mHypo > HYPO_MAX) || (mHypo < HYPO_MIN)) {
                                    Toast.makeText(getActivity(),
                                            R.string.fragment_settings_hypo_error,
                                            Toast.LENGTH_SHORT).show();
                                    return false;
                                } else {
                                    if ((!mRFAddress.equals("")) && (!mRFAddress
                                            .equals(RFAddress.RF_ADDRESS_UNPAIR))) {
                                        ((ActivityPDA) getActivity())
                                                .handleMessage(new EntityMessage(
                                                        ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                        ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                                        ParameterGlobal.PORT_GLUCOSE,
                                                        ParameterGlobal.PORT_GLUCOSE,
                                                        EntityMessage.OPERATION_SET,
                                                        ParameterGlucose.PARAM_FILL_LIMIT,
                                                        new ValueShort((short) mHypo)
                                                                .getByteArray()));
                                    } else {
                                        updateHypo(mHypo);
                                    }
                                }

                                break;

                            default:
                                break;
                        }
                        return true;
                    }
                });
    }


    private void setLanguage() {
        FragmentInput fragmentInput = new FragmentInput();
        fragmentInput
                .setComment(getString(R.string.setting_general_language_switch));
        fragmentInput.setInputText(FragmentInput.POSITION_LEFT, null);
        fragmentInput.setInputText(FragmentInput.POSITION_RIGHT, null);
        fragmentInput.setInputText(FragmentInput.POSITION_CENTER, null);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_LEFT, null);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_RIGHT, null);
        showDialogConfirm(getString(R.string.setting_general_language), "", "",
                fragmentInput, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:

                                if (Locale.getDefault().getLanguage().equals("zh")) {
                                    updateLanguage(Locale.ENGLISH);
                                } else {
                                    updateLanguage(Locale.SIMPLIFIED_CHINESE);
                                }

                                break;

                            default:
                                break;
                        }

                        return true;
                    }
                });
    }

    public void updateDateTimeSetting(boolean timeFormat) {
        WidgetSettingItem settingItem =
                (WidgetSettingItem) mRootView.findViewById(R.id.item_date);

        if (settingItem != null) {
            settingItem.setItemValue(((ActivityPDA) getActivity())
                    .getDateString(System.currentTimeMillis(), null));
        }

        settingItem = (WidgetSettingItem) mRootView.findViewById(R.id.item_time);

        if (settingItem != null) {
            settingItem.setItemValue(((ActivityPDA) getActivity())
                    .getTimeString(System.currentTimeMillis(), null));
        }

        TimeUtil.set24HourFormat(timeFormat, getActivity());
        ((ActivityPDA) getActivity())
                .getDataStorage(ActivityPDA.class.getSimpleName())
                .setBoolean(ActivityPDA.SETTING_TIME_FORMAT,
                        timeFormat);
        ((ActivityPDA) getActivity()).getStatusBar()
                .setDateTime(System.currentTimeMillis(), timeFormat);
    }


    private void updateHyper(int hyper) {
        WidgetSettingItem settingItem =
                (WidgetSettingItem) mRootView.findViewById(R.id.item_hi_bg);

        if (settingItem != null) {
            settingItem.setItemValue(
                    new DecimalFormat("0.0").format((double) hyper / 100.0));
        }

        ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettings.class.getSimpleName())
                .setInt(SETTING_HYPER, hyper);


    }


    private void updateHypo(int hypo) {
        WidgetSettingItem settingItem =
                (WidgetSettingItem) mRootView.findViewById(R.id.item_lo_bg);

        if (settingItem != null) {
            settingItem.setItemValue(
                    new DecimalFormat("0.0").format((double) hypo / 100.0));
        }

        ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettings.class.getSimpleName())
                .setInt(SETTING_HYPO, hypo);
    }


    private void updateLanguage(Locale paramLocale) {
        try {
            Class localClass1 =
                    Class.forName("android.app.ActivityManagerNative");
            Object localObject1 =
                    localClass1.getMethod("getDefault", new Class[0])
                            .invoke(localClass1, new Object[0]);
            Object localObject2 = localObject1.getClass()
                    .getMethod("getConfiguration", new Class[0])
                    .invoke(localObject1, new Object[0]);
            localObject2.getClass().getDeclaredField("locale").set(localObject2,
                    paramLocale);
            localObject2.getClass().getDeclaredField("userSetLocale")
                    .setBoolean(localObject2, true);
            Class localClass2 = localObject1.getClass();
            Class[] arrayOfClass = new Class[1];
            arrayOfClass[0] = Configuration.class;
            Method localMethod =
                    localClass2.getMethod("updateConfiguration", arrayOfClass);
            Object[] arrayOfObject = new Object[1];
            arrayOfObject[0] = localObject2;
            localMethod.invoke(localObject1, arrayOfObject);
            Intent refresh =
                    new Intent(getActivity(), getActivity().getClass());
            getActivity().startActivity(refresh);
            getActivity().finish();
        } catch (Exception localException) {
            localException.printStackTrace();
        }
    }

    private void recovery() {
        restoreHg();
        unTransmitterId();
        cleanCache();
    }

    private void restoreHg() {
        mHyper = (int) (10.0f * 100.0f);
        mHypo = (int) (3.5f * 100.0f);
        if ((!mRFAddress.equals("")) && (!mRFAddress
                .equals(RFAddress.RF_ADDRESS_UNPAIR))) {
            ((ActivityPDA) getActivity())
                    .handleMessage(new EntityMessage(
                            ParameterGlobal.ADDRESS_LOCAL_VIEW,
                            ParameterGlobal.ADDRESS_REMOTE_MASTER,
                            ParameterGlobal.PORT_GLUCOSE,
                            ParameterGlobal.PORT_GLUCOSE,
                            EntityMessage.OPERATION_SET,
                            ParameterGlucose.PARAM_BG_LIMIT,
                            new ValueShort((short) mHyper)
                                    .getByteArray()));
        } else {
            updateHyper(mHyper);
        }
        if ((!mRFAddress.equals("")) && (!mRFAddress
                .equals(RFAddress.RF_ADDRESS_UNPAIR))) {
            ((ActivityPDA) getActivity())
                    .handleMessage(new EntityMessage(
                            ParameterGlobal.ADDRESS_LOCAL_VIEW,
                            ParameterGlobal.ADDRESS_REMOTE_MASTER,
                            ParameterGlobal.PORT_GLUCOSE,
                            ParameterGlobal.PORT_GLUCOSE,
                            EntityMessage.OPERATION_SET,
                            ParameterGlucose.PARAM_FILL_LIMIT,
                            new ValueShort((short) mHypo)
                                    .getByteArray()));
        } else {
            updateHypo(mHypo);
        }
    }

    private void unTransmitterId() {
        FragmentInput fragmentInput = new FragmentInput();
        if ((mRFAddress.equals("")) ||
                (mRFAddress.equals(RFAddress.RF_ADDRESS_UNPAIR))) {

        } else {
            showDialogConfirm(getString(R.string.recovery_pair), "",
                    "", fragmentInput, new FragmentDialog.ListenerDialog() {
                        @Override
                        public boolean onButtonClick(int buttonID, Fragment content) {
                            switch (buttonID) {
                                case FragmentDialog.BUTTON_ID_POSITIVE:
                                    mRFAddress = RFAddress.RF_ADDRESS_UNPAIR;
                                    mIsProgressNotDismiss = true;
                                    showDialogProgress();
                                    ((ActivityPDA) getActivity())
                                            .handleMessage(new EntityMessage(
                                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                                    ParameterGlobal.PORT_COMM,
                                                    ParameterGlobal.PORT_COMM,
                                                    EntityMessage.OPERATION_SET,
                                                    ParameterComm.PARAM_RF_REMOTE_ADDRESS,
                                                    new RFAddress(mRFAddress)
                                                            .getByteArray()));
                                    break;

                                default:
                                    break;
                            }
                            return true;
                        }
                    });
        }
    }

    private void cleanCache() {
        DataCleanUtil.cleanSharedPreference(getActivity());
    }

    public String getAddress(byte[] addressByte) {
        if (addressByte != null) {
            for (int i = 0; i < addressByte.length; i++) {
                if (addressByte[i] < 10) {
                    addressByte[i] += '0';
                } else {
                    addressByte[i] -= 10;
                    addressByte[i] += 'A';
                }
            }

            return new String(addressByte);
        } else {
            return "";
        }
    }
}
