package com.microtechmd.pda.ui.activity.fragment;


import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.TypedArrayUtils;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.liangmutian.mypicker.DatePickerDialog;
import com.example.liangmutian.mypicker.TimePickerDialog;
import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.database.DataSetHistory;
import com.microtechmd.pda.database.DbHistory;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.ValueByte;
import com.microtechmd.pda.library.entity.ValueInt;
import com.microtechmd.pda.library.entity.comm.RFAddress;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.ByteUtil;
import com.microtechmd.pda.library.utility.SPUtils;
import com.microtechmd.pda.ui.activity.ActivityMain;
import com.microtechmd.pda.ui.activity.ActivityMessageTips;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.ui.widget.WidgetSettingItem;
import com.microtechmd.pda.util.DataCleanUtil;
import com.microtechmd.pda.util.TimeUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static com.microtechmd.pda.library.entity.ParameterComm.CLOSE_COMM;
import static com.microtechmd.pda.ui.activity.ActivityPDA.COMMMESSAGETIPS;
import static com.microtechmd.pda.ui.activity.ActivityPDA.COMM_CLOSE;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer.TYPE_DATE_TIME;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer.TYPE_HISTORY_LOG;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer.TYPE_TIPS;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer.TYPE_UTILITIES;

public class FragmentSettings extends FragmentBase
        implements
        EntityMessage.Listener {

    public static final String SETTING_HYPER = "hyper";
    public static final String SETTING_HYPO = "hypo";
    public static final String REALTIMEFLAG = "realtimeFlag";

    public static final int HYPER_DEFAULT = 120;
    private static final int HYPER_MAX = 250;
    private static final int HYPER_MIN = 80;
    public static final int HYPO_DEFAULT = 35;
    private static final int HYPO_MAX = 50;
    private static final int HYPO_MIN = 22;

    private boolean mIsProgressNotShow = false;
    private boolean mIsProgressNotDismiss = false;

    private boolean realtimeFlag = true;
    private static final int QUERY_STATE_CYCLE = 1000;
    private static final int QUERY_STATE_TIMEOUT = 10000;

    private BroadcastReceiver mBroadcastReceiver = null;
    private boolean mIsRFStateChecking = false;
    private int mQueryStateTimeout = 0;
    private int mHyper = HYPER_DEFAULT;
    private int mHypo = HYPO_DEFAULT;
    private String mRFAddress = "";
    private View mRootView = null;

    private Dialog dateDialog, timeDialog;
    private WidgetSettingItem modeSettingItem;
    //    private LukeSwitchButton switchBtn;
    private byte[] rf_mac_address = null;
    private boolean pairFlag = true;

    private FragmentDialog pairFragmentDialog;
    private FragmentDialog highFragmentDialog;
    private FragmentDialog lowFragmentDialog;


    private DataSetHistory mDataSetHistory;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView =
                inflater.inflate(R.layout.fragment_settings, container, false);

        if (mDataSetHistory == null) {
            mDataSetHistory = new DataSetHistory(getActivity());
        }
        rf_mac_address = ((ActivityPDA) getActivity()).getDataStorage(
                ActivityPDA.class.getSimpleName())
                .getExtras(ActivityPDA.GET_RF_MAC_ADDRESS,
                        null);
        updateDateTimeSetting(true);
        modeSettingItem = (WidgetSettingItem) mRootView.findViewById(R.id.item_mode);
        realtimeFlag = (boolean) SPUtils.get(getActivity(), REALTIMEFLAG, true);
        if (realtimeFlag) {
            modeSettingItem.setItemValue(getString(R.string.setting_general_mode_time));
        } else {
            modeSettingItem.setItemValue(getString(R.string.setting_general_mode_history));
        }
        rf_mac_address = ((ActivityPDA) getActivity())
                .getDataStorage(
                        ActivityPDA.class.getSimpleName())
                .getExtras(ActivityPDA.GET_RF_MAC_ADDRESS,
                        null);
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

//        rootViewGetFocus();
        return mRootView;
    }

    private void rootViewGetFocus() {
        mRootView.setFocusable(true);//这个和下面的这个命令必须要设置了，才能监听事件。
        mRootView.setFocusableInTouchMode(true);
        mRootView.requestFocus();
        mRootView.setOnKeyListener(volumeListener);
    }


    View.OnKeyListener volumeListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    mKeyNavigation.onKeyPrevious();
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    mKeyNavigation.onKeyNext();
                    return true;

                case ApplicationPDA.KEY_CODE_BOLUS:
                    return mKeyNavigation.onKeyConfirm();
            }
            return false;
        }
    };

    public void onVolumeUpPressed() {
        mKeyNavigation.onKeyPrevious();
        rootViewGetFocus();
    }

    public void onVolumeDownPressed() {
        mKeyNavigation.onKeyNext();
        rootViewGetFocus();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                assert action != null;
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
        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_MONITOR, this);
    }


    @Override
    public void onDestroyView() {
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_COMM, this);
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_GLUCOSE, this);
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_MONITOR, this);
        getActivity().unregisterReceiver(mBroadcastReceiver);
        super.onDestroyView();
    }


    @Override
    public void onClick(View v) {
        super.onClick(v);

        switch (v.getId()) {
            case R.id.item_date_time:
                changeSettingType(TYPE_DATE_TIME);
                break;
            case R.id.item_date:
                setDate();
                break;

            case R.id.item_time:
                setTime();
                break;

            case R.id.item_pairing:
                setTransmitterID();
                break;

            case R.id.item_mode:
                setMode();
                break;

            case R.id.item_message:
//                startActivity(new Intent(getActivity(), ActivityMessageTips.class));
                changeSettingType(TYPE_TIPS);
                break;

            case R.id.item_hi_bg:
                setHyper();
                break;

            case R.id.item_lo_bg:
                setHypo();
                break;
            case R.id.item_history:
//                startActivity(new Intent(getActivity(), ActivityHistoryLog.class));
                changeSettingType(TYPE_HISTORY_LOG);
                break;
            case R.id.item_recovery:
                recovery();
                break;
            case R.id.item_utilities:
                changeSettingType(TYPE_UTILITIES);
                break;

            default:
                break;
        }
    }

    private void changeSettingType(int type) {
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                        ParameterComm.SETTING_TYPE,
                        new byte[]{(byte) type}));
    }

    @Override
    public void onReceive(EntityMessage message) {
        switch (message.getOperation()) {
            case EntityMessage.OPERATION_SET:
                handleSet(message);
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

            case EntityMessage.OPERATION_PAIR:
                handlePair(message);
                break;

            case EntityMessage.OPERATION_UNPAIR:
                handleUnPair(message);
                break;

            default:
                break;
        }
    }

    private void handleSet(EntityMessage message) {
        if (message.getParameter() == ParameterComm.PAIRAGAIN) {
            mRFAddress = RFAddress.RF_ADDRESS_UNPAIR;
            pair(RFAddress.RF_ADDRESS_UNPAIR);
        }
    }

    private void handlePair(EntityMessage message) {
        if (message.getParameter() == ParameterComm.PARAM_MAC) {
            if (!(message.getData()[0] == EntityMessage.FUNCTION_OK)) {
                Toast.makeText(getActivity(), getResources().getString(R.string.bluetooth_setting_match_failed),
                        Toast.LENGTH_SHORT)
                        .show();
                mRFAddress = RFAddress.RF_ADDRESS_UNPAIR;
                pair(RFAddress.RF_ADDRESS_UNPAIR);
                mIsProgressNotDismiss = false;
                dismissDialogProgress();
            } else {
                ((WidgetSettingItem) mRootView
                        .findViewById(R.id.item_pairing))
                        .setItemValue(mRFAddress.toUpperCase());
                ((ActivityPDA) getActivity()).handleMessage(
                        new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                ParameterGlobal.PORT_COMM,
                                ParameterGlobal.PORT_COMM,
                                EntityMessage.OPERATION_SET,
                                CLOSE_COMM,
                                new byte[]{(byte) 1}));
                SPUtils.put(getActivity(), COMM_CLOSE, true);
                ((ActivityPDA) getActivity()).handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                        ParameterGlobal.PORT_COMM,
                        ParameterGlobal.PORT_COMM,
                        EntityMessage.OPERATION_SET,
                        ParameterComm.PARAM_RF_BROADCAST_SWITCH, new byte[]
                        {
                                (byte) 1
                        }));
//
                ((ActivityPDA) getActivity()).handleMessage(
                        new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                ParameterGlobal.PORT_GLUCOSE,
                                ParameterGlobal.PORT_GLUCOSE,
                                EntityMessage.OPERATION_SET,
                                ParameterGlucose.PARAM_BG_LIMIT,
                                new ValueByte(mHyper).getByteArray()
                        ));
                ((ActivityPDA) getActivity()).handleMessage(
                        new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                ParameterGlobal.PORT_GLUCOSE,
                                ParameterGlobal.PORT_GLUCOSE,
                                EntityMessage.OPERATION_SET,
                                ParameterGlucose.PARAM_FILL_LIMIT,
                                new ValueByte(mHypo).getByteArray()
                        ));

                List<DbHistory> lastList = mDataSetHistory.querAddressHistoryLast(mRFAddress.toUpperCase());
                if (lastList != null) {
                    if (lastList.size() > 0) {
                        DbHistory dbLast = lastList.get(lastList.size() - 1);
                        byte[] sensorBytes = ByteUtil.intToBytes(dbLast.getSensorIndex());
                        byte[] eventBytes = ByteUtil.intToBytes(dbLast.getEvent_index());
                        byte[] lastByes = ByteUtil.concat(sensorBytes, eventBytes);
                        ((ActivityPDA) getActivity()).handleMessage(
                                new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                        ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                        ParameterGlobal.PORT_MONITOR,
                                        ParameterGlobal.PORT_MONITOR,
                                        EntityMessage.OPERATION_SET,
                                        ParameterComm.PAIR_SUCCESS,
                                        lastByes));
                    }
                }

                Toast.makeText(getActivity(), getResources().getString(R.string.bluetooth_setting_match_success),
                        Toast.LENGTH_SHORT)
                        .show();
                if (pairFragmentDialog != null) {
                    pairFragmentDialog.dismissAllowingStateLoss();
                }

                ((ActivityPDA) getActivity()).handleMessage(new EntityMessage(
                        ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_MODEL, ParameterGlobal.PORT_MONITOR,
                        ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_GET,
                        ParameterMonitor.PARAM_HISTORY_LAST, null));

                mIsProgressNotDismiss = false;
                dismissDialogProgress();
//                ((ActivityPDA) getActivity())
//                        .handleMessage(new EntityMessage(
//                                ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                                ParameterGlobal.ADDRESS_REMOTE_MASTER,
//                                ParameterGlobal.PORT_COMM,
//                                ParameterGlobal.PORT_COMM,
//                                EntityMessage.OPERATION_BOND,
//                                ParameterComm.PARAM_MAC,
//                                bondId));
//
//                ((ActivityPDA) getActivity())
//                        .handleMessage(new EntityMessage(
//                                ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                                ParameterGlobal.ADDRESS_LOCAL_CONTROL,
//                                ParameterGlobal.PORT_COMM,
//                                ParameterGlobal.PORT_COMM,
//                                EntityMessage.OPERATION_SET,
//                                ParameterComm.READY,
//                                bondKey));

            }
        }
    }

    private void handleUnPair(EntityMessage message) {
        byte[] data = message.getData();
        mIsProgressNotDismiss = false;
        if (data[0] == EntityMessage.FUNCTION_OK) {
            dismissDialogProgress();
            mRFAddress = RFAddress.RF_ADDRESS_UNPAIR;
            pair(RFAddress.RF_ADDRESS_UNPAIR);
            ((ActivityPDA) getActivity())
                    .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                            ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
                            ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_SET,
                            ParameterComm.RESET_DATA,
                            new byte[]{(byte) 3}));
            ((ActivityPDA) getActivity())
                    .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                            ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                            ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                            ParameterComm.UNPAIRNOSIGNAL,
                            null));
        }
    }

    protected void handleEvent(EntityMessage message) {
        switch (message.getEvent()) {
            case EntityMessage.EVENT_SEND_DONE:
                break;

            case EntityMessage.EVENT_ACKNOWLEDGE:
                break;

            case EntityMessage.EVENT_TIMEOUT:

                if (mIsProgressNotDismiss && message.getSourcePort() == ParameterGlobal.PORT_COMM &&
                        message.getParameter() == ParameterComm.PARAM_MAC) {
                    if (pairFlag) {
                        pair(RFAddress.RF_ADDRESS_UNPAIR);
                    } else {
                        ensureUnpair();
                    }
                }

                mIsProgressNotDismiss = false;
                dismissDialogProgress();
                break;

            default:
                break;
        }
    }

    private void ensureUnpair() {
        FragmentInput fragmentInput = new FragmentInput();
        fragmentInput
                .setComment(getString(R.string.force_unpair_info));
        fragmentInput.setInputText(FragmentInput.POSITION_LEFT, null);
        fragmentInput.setInputText(FragmentInput.POSITION_RIGHT, null);
        fragmentInput.setInputText(FragmentInput.POSITION_CENTER, null);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_LEFT, null);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_RIGHT, null);
        showDialogConfirm(getString(R.string.fragment_settings_force_unpair), "", "",
                fragmentInput, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                pair(RFAddress.RF_ADDRESS_UNPAIR);
                                break;
                            default:
                                break;
                        }

                        return true;
                    }
                });
    }


    protected void handleNotification(EntityMessage message) {
        if (message.getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_CONTROL) {
            if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
                    (message.getParameter() == ParameterComm.PARAM_RF_STATE)) {
                if (message.getData()[0] != ParameterComm.RF_STATE_IDLE) {
                    if (mIsRFStateChecking) {
                        mIsRFStateChecking = false;
                        mLog.Error(getClass(), "Set address" + mRFAddress);
                        // Set address
                        ((ActivityPDA) getActivity())
                                .handleMessage(new EntityMessage(
                                        ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                        ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                        ParameterGlobal.PORT_COMM,
                                        ParameterGlobal.PORT_COMM,
                                        EntityMessage.OPERATION_PAIR,
                                        ParameterComm.PARAM_MAC,
                                        rf_mac_address));
                        pairFlag = true;
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
                            Toast.makeText(getActivity(),
                                    getActivity().getResources().getString(R.string.connect_fail),
                                    Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }
                }
            }
        }

        if (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) {
            if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
                    (message.getParameter() == ParameterComm.PARAM_RF_LOCAL_ADDRESS)) {
                if (message.getData() != null) {
                    rf_mac_address = message.getData();
                    checkRFState();
                    ((ActivityPDA) getActivity())
                            .getDataStorage(
                                    ActivityPDA.class.getSimpleName())
                            .setExtras(ActivityPDA.GET_RF_MAC_ADDRESS,
                                    rf_mac_address);
                }
            }
        }
        if ((message.getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_MODEL) &&
                (message.getParameter() == ParameterMonitor.PARAM_HISTORY_LAST)) {
            if (message.getData() != null) {
                String addr = new String(message.getData());
                RFAddress address = new RFAddress(mRFAddress);

                if (!addr.equals(address.getAddress())) {
                    ((ActivityPDA) getActivity()).handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                    ParameterGlobal.PORT_MONITOR,
                                    ParameterGlobal.PORT_MONITOR,
                                    EntityMessage.OPERATION_SET,
                                    ParameterComm.ADDCHANGE,
                                    null));
                }
            }
        }
    }


    protected void handleAcknowledgement(final EntityMessage message) {
//        if (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER) {
//            if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
//                    (message.getParameter() == ParameterComm.PARAM_RF_REMOTE_ADDRESS)) {
//                mLog.Error(getClass(), "Set remote address success!");
//
//                if (!(message.getData()[0] == EntityMessage.FUNCTION_OK)) {
//                    Toast.makeText(getActivity(), getResources().getString(R.string.bluetooth_setting_match_failed),
//                            Toast.LENGTH_SHORT)
//                            .show();
//                    mRFAddress = RFAddress.RF_ADDRESS_UNPAIR;
//                }
//                if (mRFAddress.equals(RFAddress.RF_ADDRESS_UNPAIR)) {
//                    pair(RFAddress.RF_ADDRESS_UNPAIR);
//                } else {
//                    ((WidgetSettingItem) mRootView
//                            .findViewById(R.id.item_pairing))
//                            .setItemValue(mRFAddress.toUpperCase());
//                    ((ActivityPDA) getActivity()).handleMessage(
//                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                                    ParameterGlobal.PORT_COMM,
//                                    ParameterGlobal.PORT_COMM,
//                                    EntityMessage.OPERATION_SET,
//                                    CLOSE_COMM,
//                                    new byte[]{(byte) 1}));
//                    SPUtils.put(getActivity(), COMM_CLOSE, true);
//                    ((ActivityPDA) getActivity()).handleMessage(
//                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                                    ParameterGlobal.ADDRESS_REMOTE_MASTER,
//                                    ParameterGlobal.PORT_GLUCOSE,
//                                    ParameterGlobal.PORT_GLUCOSE,
//                                    EntityMessage.OPERATION_SET,
//                                    ParameterGlucose.PARAM_BG_LIMIT,
//                                    new ValueByte(mHyper).getByteArray()
//                            ));
//                    ((ActivityPDA) getActivity()).handleMessage(
//                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                                    ParameterGlobal.ADDRESS_REMOTE_MASTER,
//                                    ParameterGlobal.PORT_GLUCOSE,
//                                    ParameterGlobal.PORT_GLUCOSE,
//                                    EntityMessage.OPERATION_SET,
//                                    ParameterGlucose.PARAM_FILL_LIMIT,
//                                    new ValueByte(mHypo).getByteArray()
//                            ));
////                    ((ActivityPDA) getActivity())
////                            .getDataStorage(ActivityMain.class.getSimpleName())
////                            .setLong(ActivityMain.SETTING_STARTUP_TIME,
////                                    System.currentTimeMillis());
//                    ((ActivityPDA) getActivity()).handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                            ParameterGlobal.ADDRESS_LOCAL_CONTROL,
//                            ParameterGlobal.PORT_COMM,
//                            ParameterGlobal.PORT_COMM,
//                            EntityMessage.OPERATION_SET,
//                            ParameterComm.PARAM_RF_BROADCAST_SWITCH, new byte[]
//                            {
//                                    (byte) 1
//                            }));
//                    Toast.makeText(getActivity(), getResources().getString(R.string.bluetooth_setting_match_success),
//                            Toast.LENGTH_SHORT)
//                            .show();
//                    mIsProgressNotDismiss = false;
//                    dismissDialogProgress();
//                }
//            }
//
//            if (message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE) {
//                if (message.getParameter() == ParameterGlucose.PARAM_FILL_LIMIT) {
//                    if (!(message.getData()[0] == EntityMessage.FUNCTION_OK)) {
//                        Toast.makeText(getActivity(), getResources().getString(R.string.setting_failed),
//                                Toast.LENGTH_SHORT)
//                                .show();
//                        setHypo();
//                        return;
//                    }
//                    mLog.Debug(getClass(), "Set hypo success!");
//                    updateHypo(mHypo);
//                    if (lowFragmentDialog != null) {
//                        lowFragmentDialog.dismissAllowingStateLoss();
//                    }
//                    ((ActivityPDA) getActivity()).handleMessage(
//                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                                    ParameterGlobal.PORT_MONITOR,
//                                    ParameterGlobal.PORT_MONITOR,
//                                    EntityMessage.OPERATION_SET,
//                                    ParameterGlucose.PARAM_FILL_LIMIT,
//                                    new ValueByte(mHypo).getByteArray()
//                            ));
//                }
//                if (message.getParameter() == ParameterGlucose.PARAM_BG_LIMIT) {
//                    if (!(message.getData()[0] == EntityMessage.FUNCTION_OK)) {
//                        Toast.makeText(getActivity(), getResources().getString(R.string.setting_failed),
//                                Toast.LENGTH_SHORT)
//                                .show();
//                        setHyper();
//                        return;
//                    }
//                    mLog.Debug(getClass(), "Set hyper success!");
//                    updateHyper(mHyper);
//                    if (highFragmentDialog != null) {
//                        highFragmentDialog.dismissAllowingStateLoss();
//                    }
//                    ((ActivityPDA) getActivity()).handleMessage(
//                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                                    ParameterGlobal.PORT_MONITOR,
//                                    ParameterGlobal.PORT_MONITOR,
//                                    EntityMessage.OPERATION_SET,
//                                    ParameterGlucose.PARAM_BG_LIMIT,
//                                    new ValueByte(mHyper).getByteArray()
//                            ));
//                }
//            }
//        }

        if (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) {
            if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
                    (message
                            .getParameter() == ParameterComm.PARAM_RF_LOCAL_ADDRESS)) {
                mLog.Error(getClass(), "Set local address success!");

                if (!mRFAddress.equals(RFAddress.RF_ADDRESS_UNPAIR)) {
                    mIsRFStateChecking = true;
                    mQueryStateTimeout = 0;
                    ((ActivityPDA) getActivity())
                            .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
                                    ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_GET,
                                    ParameterComm.PARAM_RF_LOCAL_ADDRESS,
                                    null));
//                    checkRFState();
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
                                    ParameterComm.PARAM_RF_BROADCAST_SWITCH,
                                    new byte[]{(byte) 0}));
                    ((ActivityPDA) getActivity()).handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                    ParameterGlobal.PORT_MONITOR,
                                    ParameterGlobal.PORT_MONITOR,
                                    EntityMessage.OPERATION_NOTIFY,
                                    ParameterMonitor.PARAM_HISTORY,
                                    new History(new DateTime(Calendar.getInstance()),
                                            new Status(0),
                                            new Event(0,
                                                    0, 0))
                                            .getByteArray()));

                    ((ActivityPDA) getActivity()).handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.PORT_COMM,
                                    ParameterGlobal.PORT_COMM,
                                    EntityMessage.OPERATION_SET,
                                    CLOSE_COMM,
                                    new byte[]
                                            {
                                                    (byte) 0
                                            }));
                    SPUtils.put(getActivity(), COMM_CLOSE, false);
                    ((ActivityPDA) getActivity()).handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.PORT_MONITOR,
                                    ParameterGlobal.PORT_MONITOR,
                                    EntityMessage.OPERATION_SET,
                                    ParameterMonitor.COUNTDOWNVIEW_VISIBLE,
                                    new ValueInt(0).getByteArray()));
                    ActivityMain.setStatus(null);

                    ((ActivityPDA) getActivity()).handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                    ParameterGlobal.PORT_MONITOR,
                                    ParameterGlobal.PORT_MONITOR,
                                    EntityMessage.OPERATION_SET,
                                    ParameterComm.SYNCHRONIZEDONE, new byte[]
                                    {
                                            (byte) 0
                                    }));
                    ((ActivityPDA) getActivity()).handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.PORT_MONITOR,
                                    ParameterGlobal.PORT_MONITOR,
                                    EntityMessage.OPERATION_SET,
                                    ParameterComm.UNPAIR, new byte[]
                                    {
                                            (byte) 0
                                    }));
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
                    .setExtras(ActivityPDA.SETTING_RF_ADDRESS,
                            null);
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


    private void showRetryDialog(FragmentDialog fragmentDialog, String title, String buttonTextPositive,
                                 String buttonTextNegative, Fragment content,
                                 FragmentDialog.ListenerDialog listener) {
        fragmentDialog.setHomeCancelFlag(true);
        fragmentDialog.setTitle(title);
        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
                buttonTextPositive);
        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
                buttonTextNegative);
        fragmentDialog.setContent(content);
        fragmentDialog.setListener(listener);
        fragmentDialog.show(getChildFragmentManager(), null);
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
        fragmentDialog.show(getChildFragmentManager(), null);
    }

    private void showDateDialog(List<Integer> date) {
        DatePickerDialog.Builder builder = new DatePickerDialog.Builder(getActivity())
                .setSelectYear(date.get(0) - 1)
                .setSelectMonth(date.get(1))
                .setSelectDay(date.get(2) - 1);
        builder.setOnDateSelectedListener(new DatePickerDialog.OnDateSelectedListener() {
            @Override
            public void onDateSelected(int[] dates) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, dates[0]);
                calendar.set(Calendar.MONTH, dates[1] - 1);
                calendar.set(Calendar.DAY_OF_MONTH, dates[2]);
                SystemClock.setCurrentTimeMillis(
                        calendar.getTimeInMillis());
                dateOrTimeChanged();
            }

            @Override
            public void onCancel() {
            }
        });

        dateDialog = builder.create();
        dateDialog.show();
        dateDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_HOME:
                        dateDialog.dismiss();
                        return true;
                }
                return false;
            }
        });
        dateDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);

    }

    private void setDate() {
        Calendar calendar = Calendar.getInstance();
        List<Integer> date = new ArrayList<>();
        date.add(calendar.get(Calendar.YEAR));
        date.add(calendar.get(Calendar.MONTH));
        date.add(calendar.get(Calendar.DAY_OF_MONTH));
        showDateDialog(date);
//        final FragmentInput fragmentInput = new FragmentInput();
//        fragmentInput.setInputText(FragmentInput.POSITION_LEFT,
//                calendar.get(Calendar.YEAR) + "");
//        fragmentInput.setInputText(FragmentInput.POSITION_CENTER,
//                calendar.get(Calendar.MONTH) + 1 + "");
//        fragmentInput.setInputText(FragmentInput.POSITION_RIGHT,
//                calendar.get(Calendar.DAY_OF_MONTH) + "");
//
//        for (int i = 0; i < FragmentInput.COUNT_POSITION; i++) {
//            fragmentInput.setInputType(i, InputType.TYPE_CLASS_NUMBER);
//        }
//
//        fragmentInput.setSeparatorText(FragmentInput.POSITION_LEFT, "-");
//        fragmentInput.setSeparatorText(FragmentInput.POSITION_RIGHT, "-");
//        showDialogConfirm(getString(R.string.setting_general_set_date), "", "",
//                fragmentInput, new FragmentDialog.ListenerDialog() {
//                    @Override
//                    public boolean onButtonClick(int buttonID, Fragment content) {
//                        switch (buttonID) {
//                            case FragmentDialog.BUTTON_ID_POSITIVE:
//                                Calendar calendar = Calendar.getInstance();
//                                calendar.set(Calendar.YEAR,
//                                        Integer.parseInt(fragmentInput.getInputText(
//                                                FragmentInput.POSITION_LEFT)));
//                                calendar
//                                        .set(Calendar.MONTH,
//                                                Integer
//                                                        .parseInt(fragmentInput.getInputText(
//                                                                FragmentInput.POSITION_CENTER)) -
//                                                        1);
//                                calendar.set(Calendar.DAY_OF_MONTH,
//                                        Integer.parseInt(fragmentInput.getInputText(
//                                                FragmentInput.POSITION_RIGHT)));
//                                SystemClock.setCurrentTimeMillis(
//                                        calendar.getTimeInMillis());
//                                updateDateTimeSetting(true);
//                                break;
//
//                            default:
//                                break;
//                        }
//
//                        return true;
//                    }
//                });
    }

    private void showTimePick(List<Integer> time) {
        TimePickerDialog.Builder builder = new TimePickerDialog.Builder(getActivity())
                .setSelectHour(time.get(0))
                .setSelectMinute(time.get(1));
        timeDialog = builder.setOnTimeSelectedListener(new TimePickerDialog.OnTimeSelectedListener() {
            @Override
            public void onTimeSelected(int[] times) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, times[0]);
                calendar.set(Calendar.MINUTE, times[1]);
                calendar.set(Calendar.SECOND, 0);
                SystemClock.setCurrentTimeMillis(
                        calendar.getTimeInMillis());
                dateOrTimeChanged();
            }
        }).create();

        timeDialog.show();
        timeDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_HOME:
                        timeDialog.dismiss();
                        return true;
                }
                return false;
            }
        });
        timeDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
    }

    private void dateOrTimeChanged() {
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
                        ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_SET,
                        ParameterComm.RESET_DATA,
                        new byte[]{(byte) 2}));

        ((ActivityPDA) getActivity()).handleMessage(
                new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                        ParameterGlobal.PORT_MONITOR,
                        ParameterGlobal.PORT_MONITOR,
                        EntityMessage.OPERATION_SET,
                        ParameterComm.FORCESYNCHRONIZEFLAG, new byte[]{}));
        updateDateTimeSetting(true);
    }

    private void setTime() {
        Calendar calendar = Calendar.getInstance();
        List<Integer> time = new ArrayList<>();
        time.add(calendar.get(Calendar.HOUR_OF_DAY));
        time.add(calendar.get(Calendar.MINUTE));
        showTimePick(time);
//        final FragmentInput fragmentInput = new FragmentInput();
//        fragmentInput.setInputText(FragmentInput.POSITION_LEFT,
//                calendar.get(Calendar.HOUR_OF_DAY) + "");
//        fragmentInput.setInputText(FragmentInput.POSITION_CENTER,
//                calendar.get(Calendar.MINUTE) + "");
//        fragmentInput.setInputType(FragmentInput.POSITION_LEFT,
//                InputType.TYPE_CLASS_NUMBER);
//        fragmentInput.setInputType(FragmentInput.POSITION_CENTER,
//                InputType.TYPE_CLASS_NUMBER);
//        fragmentInput.setSeparatorText(FragmentInput.POSITION_LEFT, ":");
//        showDialogConfirm(getString(R.string.setting_general_set_time), "", "",
//                fragmentInput, new FragmentDialog.ListenerDialog() {
//                    @Override
//                    public boolean onButtonClick(int buttonID, Fragment content) {
//                        switch (buttonID) {
//                            case FragmentDialog.BUTTON_ID_POSITIVE:
//                                int hour = Integer.parseInt(fragmentInput
//                                        .getInputText(FragmentInput.POSITION_LEFT));
//                                int minute = Integer.parseInt(fragmentInput
//                                        .getInputText(FragmentInput.POSITION_CENTER));
//                                Calendar calendar = Calendar.getInstance();
//                                calendar.set(Calendar.MINUTE, minute);
//                                calendar.set(Calendar.HOUR_OF_DAY, hour);
//                                SystemClock.setCurrentTimeMillis(
//                                        calendar.getTimeInMillis());
//                                updateDateTimeSetting(true);
//                                break;
//
//                            default:
//                                break;
//                        }
//                        return true;
//                    }
//                });
    }

//    private void dateOrTimeChanged() {
//
//    }

    private void setMode() {
        realtimeFlag = (boolean) SPUtils.get(getActivity(), REALTIMEFLAG, true);
        FragmentInput fragmentInput = new FragmentInput();
        if (realtimeFlag) {
            fragmentInput
                    .setComment(getString(R.string.setting_general_timemode_switch));
        } else {
            fragmentInput
                    .setComment(getString(R.string.setting_general_historymode_switch));
        }
        fragmentInput.setInputText(FragmentInput.POSITION_LEFT, null);
        fragmentInput.setInputText(FragmentInput.POSITION_RIGHT, null);
        fragmentInput.setInputText(FragmentInput.POSITION_CENTER, null);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_LEFT, null);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_RIGHT, null);
        showDialogConfirm(getString(R.string.setting_general_mode), "", "",
                fragmentInput, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                if (realtimeFlag) {
                                    ((ActivityPDA) getActivity()).handleMessage(
                                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                                    ParameterGlobal.PORT_MONITOR,
                                                    ParameterGlobal.PORT_MONITOR,
                                                    EntityMessage.OPERATION_SET,
                                                    ParameterComm.BROADCAST_SAVA, new byte[]
                                                    {
                                                            (byte) 0
                                                    }));
                                    ((ActivityPDA) getActivity()).handleMessage(
                                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.PORT_MONITOR,
                                                    ParameterGlobal.PORT_MONITOR,
                                                    EntityMessage.OPERATION_SET,
                                                    ParameterComm.BROADCAST_SAVA, new byte[]
                                                    {
                                                            (byte) 0
                                                    }));
                                    realtimeFlag = false;
                                    modeSettingItem.setItemValue(getString(R.string.setting_general_mode_history));
                                    SPUtils.put(getActivity(), COMMMESSAGETIPS, false);
                                } else {
                                    ((ActivityPDA) getActivity()).handleMessage(
                                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                                    ParameterGlobal.PORT_MONITOR,
                                                    ParameterGlobal.PORT_MONITOR,
                                                    EntityMessage.OPERATION_SET,
                                                    ParameterComm.BROADCAST_SAVA, new byte[]
                                                    {
                                                            (byte) 1
                                                    }));
                                    ((ActivityPDA) getActivity()).handleMessage(
                                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.PORT_MONITOR,
                                                    ParameterGlobal.PORT_MONITOR,
                                                    EntityMessage.OPERATION_SET,
                                                    ParameterComm.BROADCAST_SAVA, new byte[]
                                                    {
                                                            (byte) 1
                                                    }));
                                    realtimeFlag = true;
                                    modeSettingItem.setItemValue(getString(R.string.setting_general_mode_time));
                                    SPUtils.put(getActivity(), COMMMESSAGETIPS, true);
                                }
                                SPUtils.put(getActivity(), REALTIMEFLAG, realtimeFlag);
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

        if ((mRFAddress.equals("")) || (mRFAddress.equals(RFAddress.RF_ADDRESS_UNPAIR))) {
            fragmentInput.setInputText(FragmentInput.POSITION_CENTER,
                    mRFAddress);
            fragmentInput.setInputType(FragmentInput.POSITION_CENTER,
                    InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            );
            fragmentInput.setInputWidth(FragmentInput.POSITION_CENTER, 150);
            pairFragmentDialog = new FragmentDialog();
            showRetryDialog(pairFragmentDialog, getString(R.string.fragment_settings_pairing), "",
                    "", fragmentInput, new FragmentDialog.ListenerDialog() {
                        @Override
                        public boolean onButtonClick(int buttonID, Fragment content) {
                            switch (buttonID) {
                                case FragmentDialog.BUTTON_ID_POSITIVE:
                                    String address = fragmentInput.getInputText(
                                            FragmentInput.POSITION_CENTER);

                                    if (((address.trim().length() == 6) || (address.trim().length() == 7)) && !(address.trim()
                                            .equals(RFAddress.RF_ADDRESS_UNPAIR))) {
                                        mIsProgressNotDismiss = true;
                                        showDialogProgress();
                                        if (address.trim().length() == 7) {
                                            address = address.substring(1);
                                        }
                                        pair(address.trim());
                                        if (pairFragmentDialog != null) {
                                            pairFragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
                                                    getResources().getString(R.string.retry));
                                        }
                                        return false;
                                    } else {
                                        Toast.makeText(getActivity(),
                                                R.string.actions_pump_id_blank,
                                                Toast.LENGTH_SHORT).show();
                                        return false;
                                    }

//                                    if ((address.trim().length() != 6) ||
//                                            (address.trim()
//                                                    .equals(RFAddress.RF_ADDRESS_UNPAIR))) {
//
//                                        Toast.makeText(getActivity(),
//                                                R.string.actions_pump_id_blank,
//                                                Toast.LENGTH_SHORT).show();
////                                        toast.setGravity(Gravity.CENTER, 0, 0);
////                                        toast.show();
//
//                                        return false;
//                                    } else {
//                                        mIsProgressNotDismiss = true;
//                                        showDialogProgress();
//                                        if (address.trim().length() == 7) {
//                                            address = address.substring(1);
//                                        }
//                                            pair(address.trim());
//                                        if (pairFragmentDialog != null) {
//                                            pairFragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
//                                                    getResources().getString(R.string.retry));
//                                        }
//                                        return false;
//                                    }

//                                    break;

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
                                    mIsProgressNotDismiss = true;
                                    showDialogProgress();
                                    ((ActivityPDA) getActivity())
                                            .handleMessage(new EntityMessage(
                                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                    ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                                    ParameterGlobal.PORT_COMM,
                                                    ParameterGlobal.PORT_COMM,
                                                    EntityMessage.OPERATION_UNPAIR,
                                                    ParameterComm.PARAM_MAC,
                                                    new byte[]{}));
                                    pairFlag = false;
//                                    pair(mRFAddress);
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
                new DecimalFormat("0.0").format((double) mHyper / 10));
        fragmentInput.setInputType(FragmentInput.POSITION_CENTER,
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_RIGHT,
                getString(R.string.unit_mmol_l));
        highFragmentDialog = new FragmentDialog();
        showRetryDialog(highFragmentDialog, getString(R.string.fragment_settings_hi_bg_threshold),
                "", "", fragmentInput, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                mHyper = (int) (Float.parseFloat(fragmentInput
                                        .getInputText(FragmentInput.POSITION_CENTER)) * 10.0f);

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
                                                        new ValueByte(mHyper).getByteArray()
                                                ));
                                        if (highFragmentDialog != null) {
                                            highFragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
                                                    getResources().getString(R.string.retry));
                                        }
                                        return false;
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
                new DecimalFormat("0.0").format((double) mHypo / 10));
        fragmentInput.setInputType(FragmentInput.POSITION_CENTER,
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        fragmentInput.setSeparatorText(FragmentInput.POSITION_RIGHT,
                getString(R.string.unit_mmol_l));
        lowFragmentDialog = new FragmentDialog();
        showRetryDialog(lowFragmentDialog, getString(R.string.fragment_settings_lo_bg_threshold),
                "", "", fragmentInput, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                mHypo = (int) (Float.parseFloat(fragmentInput
                                        .getInputText(FragmentInput.POSITION_CENTER)) * 10.0f);

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
                                                        new ValueByte(mHypo).getByteArray()
                                                ));
                                        if (lowFragmentDialog != null) {
                                            lowFragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
                                                    getResources().getString(R.string.retry));
                                        }
                                        return false;
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
                    new DecimalFormat("0.0").format((double) hyper / 10.0));
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
                    new DecimalFormat("0.0").format((double) hypo / 10.0));
        }

        ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettings.class.getSimpleName())
                .setInt(SETTING_HYPO, hypo);
    }

    private void recovery() {
        unTransmitterId();
    }

    private void restoreHg() {
        mHyper = HYPER_DEFAULT;
        mHypo = HYPO_DEFAULT;
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
                            new ValueByte(mHyper).getByteArray()
                    ));
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
                            new ValueByte(mHypo).getByteArray()
                    ));
        } else {
            updateHypo(mHypo);
        }
    }

    private void unTransmitterId() {
        FragmentInput fragmentInput = new FragmentInput();
        showDialogConfirm(getString(R.string.recovery_pair), "",
                "", fragmentInput, new FragmentDialog.ListenerDialog() {
                    @Override
                    public boolean onButtonClick(int buttonID, Fragment content) {
                        switch (buttonID) {
                            case FragmentDialog.BUTTON_ID_POSITIVE:
                                ((ActivityPDA) getActivity()).handleMessage(
                                        new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                ParameterGlobal.PORT_MONITOR,
                                                ParameterGlobal.PORT_MONITOR,
                                                EntityMessage.OPERATION_SET,
                                                ParameterComm.BROADCAST_SAVA, new byte[]
                                                {
                                                        (byte) 1
                                                }));
                                ((ActivityPDA) getActivity()).handleMessage(
                                        new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                                ParameterGlobal.PORT_MONITOR,
                                                ParameterGlobal.PORT_MONITOR,
                                                EntityMessage.OPERATION_SET,
                                                ParameterComm.BROADCAST_SAVA, new byte[]
                                                {
                                                        (byte) 1
                                                }));
                                modeSettingItem.setItemValue(getString(R.string.setting_general_mode_time));
                                restoreHg();
                                cleanCache();
                                ((ActivityPDA) getActivity()).handleMessage(
                                        new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                                ParameterGlobal.PORT_MONITOR,
                                                ParameterGlobal.PORT_MONITOR,
                                                EntityMessage.OPERATION_SET,
                                                ParameterMonitor.COUNTDOWNVIEW_VISIBLE,
                                                new ValueInt(0).getByteArray()));
                                mRFAddress = RFAddress.RF_ADDRESS_UNPAIR;
                                mIsProgressNotDismiss = true;
//                                showDialogProgress();
                                pair(RFAddress.RF_ADDRESS_UNPAIR);
                                ((WidgetSettingItem) mRootView
                                        .findViewById(R.id.item_pairing))
                                        .setItemValue("");
//                                    ((ActivityPDA) getActivity())
//                                            .handleMessage(new EntityMessage(
//                                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                                                    ParameterGlobal.ADDRESS_REMOTE_MASTER,
//                                                    ParameterGlobal.PORT_COMM,
//                                                    ParameterGlobal.PORT_COMM,
//                                                    EntityMessage.OPERATION_SET,
//                                                    ParameterComm.PARAM_RF_REMOTE_ADDRESS,
//                                                    new RFAddress(mRFAddress)
//                                                            .getByteArray()));
                                break;

                            default:
                                break;
                        }
                        return true;
                    }
                });
    }

    private void cleanCache() {
        ArrayList<DbHistory> errList = ((ApplicationPDA) getActivity().getApplication()).getDataErrListAll();
        errList.clear();
        ((ApplicationPDA) getActivity().getApplication()).setDataErrListAll(errList);
        List<History> dataList = ((ApplicationPDA) getActivity().getApplication()).getDataListAll();
        dataList.clear();
        ((ApplicationPDA) getActivity().getApplication()).setDataListAll(dataList);
        DataCleanUtil.cleanSharedPreference(getActivity());
        SPUtils.clear(getActivity());
        ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettings.class.getSimpleName())
                .clear();
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_CONTROL, ParameterGlobal.PORT_COMM,
                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                        ParameterComm.CLEAN_DATABASES,
                        new byte[]{}));
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_CONTROL, ParameterGlobal.PORT_MONITOR,
                        ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_SET,
                        ParameterComm.CLEAN_DATABASES,
                        new byte[]{}));
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_MODEL, ParameterGlobal.PORT_COMM,
                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                        ParameterComm.CLEAN_DATABASES,
                        new byte[]{}));
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
                        ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_SET,
                        ParameterComm.RESET_DATA,
                        new byte[]{(byte) 0}));
        ((ActivityPDA) getActivity())
                .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                        ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                        ParameterComm.UNPAIRNOSIGNAL,
                        null));
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
