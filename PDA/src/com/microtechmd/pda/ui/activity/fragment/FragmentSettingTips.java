package com.microtechmd.pda.ui.activity.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ValueByte;
import com.microtechmd.pda.library.entity.comm.RFAddress;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.SPUtils;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.ui.widget.WidgetSettingTipsItem;

import java.text.DecimalFormat;

import static com.microtechmd.pda.ui.activity.ActivityPDA.COMMMESSAGETIPS;
import static com.microtechmd.pda.ui.activity.ActivityPDA.HIMESSAGETIPS;
import static com.microtechmd.pda.ui.activity.ActivityPDA.LOMESSAGETIPS;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer.TYPE_SETTING;

public class FragmentSettingTips extends FragmentBase
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

    private boolean mIsProgressNotDismiss = false;

    private static final int QUERY_STATE_CYCLE = 1000;
    private static final int QUERY_STATE_TIMEOUT = 10000;

    private boolean mIsRFStateChecking = false;
    private int mQueryStateTimeout = 0;
    private int mHyper = HYPER_DEFAULT;
    private int mHypo = HYPO_DEFAULT;
    private String mRFAddress = "";
    private View mRootView = null;

    private byte[] rf_mac_address = null;
    private boolean pairFlag = true;

    private FragmentDialog highFragmentDialog;
    private FragmentDialog lowFragmentDialog;

    private CheckBox rb_checkbox_hi, rb_checkbox_lo, rb_checkbox_comm;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView =
                inflater.inflate(R.layout.fragment_setting_tips, container, false);

        rf_mac_address = ((ActivityPDA) getActivity())
                .getDataStorage(
                        ActivityPDA.class.getSimpleName())
                .getExtras(ActivityPDA.GET_RF_MAC_ADDRESS,
                        null);
        mRFAddress = getAddress(((ActivityPDA) getActivity())
                .getDataStorage(ActivityPDA.class.getSimpleName())
                .getExtras(ActivityPDA.SETTING_RF_ADDRESS, null));
        mHyper = ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettingTips.class.getSimpleName())
                .getInt(SETTING_HYPER, HYPER_DEFAULT);
        mHypo = ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettingTips.class.getSimpleName())
                .getInt(SETTING_HYPO, HYPO_DEFAULT);
        updateHyper(mHyper);
        updateHypo(mHypo);

//        rootViewGetFocus();

        rb_checkbox_hi = (CheckBox) mRootView.findViewById(R.id.rb_checkbox_hi);
        rb_checkbox_lo = (CheckBox) mRootView.findViewById(R.id.rb_checkbox_lo);
        rb_checkbox_comm = (CheckBox) mRootView.findViewById(R.id.rb_checkbox_comm);
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_COMM, this);
        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_GLUCOSE, this);
        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_MONITOR, this);

        initCheckBox();
    }


    @Override
    public void onDestroyView() {
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_COMM, this);
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_GLUCOSE, this);
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_MONITOR, this);
        super.onDestroyView();
    }

    private void initCheckBox() {
        boolean hi_messageFlag = (boolean) SPUtils.get(getActivity(), HIMESSAGETIPS, true);
        boolean low_messageFlag = (boolean) SPUtils.get(getActivity(), LOMESSAGETIPS, true);
        boolean comm_messageFlag = (boolean) SPUtils.get(getActivity(), COMMMESSAGETIPS, true);

        rb_checkbox_hi.setChecked(hi_messageFlag);
        rb_checkbox_lo.setChecked(low_messageFlag);
        rb_checkbox_comm.setChecked(comm_messageFlag);

        setCheckChange(HIMESSAGETIPS, rb_checkbox_hi);
        setCheckChange(LOMESSAGETIPS, rb_checkbox_lo);
        setCheckChange(COMMMESSAGETIPS, rb_checkbox_comm);
    }

    private void setCheckChange(final String key, CheckBox checkBox) {
        //开关切换事件
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                SPUtils.put(getActivity(), key, b);
            }
        });
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        switch (v.getId()) {
            case R.id.ibt_back:
                ((ActivityPDA) getActivity())
                        .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                                ParameterComm.SETTING_TYPE,
                                new byte[]{(byte) TYPE_SETTING}));
                break;

            case R.id.item_hi_bg:
                setHyper();
                break;

            case R.id.item_lo_bg:
                setHypo();
                break;

            default:
                break;
        }
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

            default:
                break;
        }
    }

    private void handleSet(EntityMessage message) {
    }

    protected void handleEvent(EntityMessage message) {
        switch (message.getEvent()) {
            case EntityMessage.EVENT_SEND_DONE:
                break;

            case EntityMessage.EVENT_ACKNOWLEDGE:
                break;

            case EntityMessage.EVENT_TIMEOUT:
                break;

            default:
                break;
        }
    }


    protected void handleNotification(EntityMessage message) {

    }


    protected void handleAcknowledgement(final EntityMessage message) {
        if (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER) {
            if (message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE) {
                if (message.getParameter() == ParameterGlucose.PARAM_FILL_LIMIT) {
                    if (!(message.getData()[0] == EntityMessage.FUNCTION_OK)) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.setting_failed),
                                Toast.LENGTH_SHORT)
                                .show();
                        setHypo();
                        return;
                    }
                    mLog.Debug(getClass(), "Set hypo success!");
                    updateHypo(mHypo);
                    if (lowFragmentDialog != null) {
                        lowFragmentDialog.dismissAllowingStateLoss();
                    }
                    ((ActivityPDA) getActivity()).handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.PORT_MONITOR,
                                    ParameterGlobal.PORT_MONITOR,
                                    EntityMessage.OPERATION_SET,
                                    ParameterGlucose.PARAM_FILL_LIMIT,
                                    new ValueByte(mHypo).getByteArray()
                            ));
                }
                if (message.getParameter() == ParameterGlucose.PARAM_BG_LIMIT) {
                    if (!(message.getData()[0] == EntityMessage.FUNCTION_OK)) {
                        Toast.makeText(getActivity(), getResources().getString(R.string.setting_failed),
                                Toast.LENGTH_SHORT)
                                .show();
                        setHyper();
                        return;
                    }
                    mLog.Debug(getClass(), "Set hyper success!");
                    updateHyper(mHyper);
                    if (highFragmentDialog != null) {
                        highFragmentDialog.dismissAllowingStateLoss();
                    }
                    ((ActivityPDA) getActivity()).handleMessage(
                            new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                    ParameterGlobal.PORT_MONITOR,
                                    ParameterGlobal.PORT_MONITOR,
                                    EntityMessage.OPERATION_SET,
                                    ParameterGlucose.PARAM_BG_LIMIT,
                                    new ValueByte(mHyper).getByteArray()
                            ));
                }
            }
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

    private void setHyper() {
        mHyper = ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettingTips.class.getSimpleName())
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
                .getDataStorage(FragmentSettingTips.class.getSimpleName())
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


    private void updateHyper(int hyper) {
        WidgetSettingTipsItem settingItem =
                (WidgetSettingTipsItem) mRootView.findViewById(R.id.item_hi_bg);

        if (settingItem != null) {
            settingItem.setItemValue(
                    new DecimalFormat("0.0").format((double) hyper / 10.0));
        }

        ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettingTips.class.getSimpleName())
                .setInt(SETTING_HYPER, hyper);


    }


    private void updateHypo(int hypo) {
        WidgetSettingTipsItem settingItem =
                (WidgetSettingTipsItem) mRootView.findViewById(R.id.item_lo_bg);

        if (settingItem != null) {
            settingItem.setItemValue(
                    new DecimalFormat("0.0").format((double) hypo / 10.0));
        }

        ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettingTips.class.getSimpleName())
                .setInt(SETTING_HYPO, hypo);
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
