package com.microtechmd.pda.ui.activity.fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.parameter.ParameterGlobal;

import java.util.Calendar;

import static com.microtechmd.pda.ui.activity.ActivityPDA.YEAR_MIN;


public class FragmentSettingContainer extends FragmentBase
        implements EntityMessage.Listener {
    public static final int TYPE_SETTING = 1;
    public static final int TYPE_UTILITIES = 2;
    public static final int TYPE_DATE_TIME = 3;
    public static final int TYPE_HISTORY_LOG = 4;
    public static final int TYPE_TIPS = 5;

    private static final String TAG_SETTING = "settings";
    private static final String TAG_SETTING_UTILITIES = "setting_utilities";
    private static final String TAG_SETTING_DATE_TIME = "setting_date_time";
    private static final String TAG_SETTING_HISTORY_LOG = "setting_history_log";
    private static final String TAG_SETTING_TIPS = "setting_tips";

    public static final String SETTING_TIME_CORRECT = "setting_time_correct";

    private FragmentSettings fragmentSetting;
    private FragmentSettingUtilities fragmentSettingUtilities;
    private FragmentSettingDateAndTime fragmentSettingDateAndTime;
    private FragmentSettingHistoryLog fragmentSettingHistoryLog;
    private FragmentSettingTips fragmentSettingTips;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View mRootView = inflater.inflate(R.layout.fragment_settings_container, container, false);
        fragmentSetting = new FragmentSettings();
        fragmentSettingUtilities = new FragmentSettingUtilities();
        fragmentSettingDateAndTime = new FragmentSettingDateAndTime();
        fragmentSettingHistoryLog = new FragmentSettingHistoryLog();
        fragmentSettingTips = new FragmentSettingTips();

        if (Calendar.getInstance()
                .get(Calendar.YEAR) < YEAR_MIN) {
            switchContent(fragmentSettingDateAndTime, TAG_SETTING_DATE_TIME);
        } else {
            switchContent(fragmentSetting, TAG_SETTING);
        }
        return mRootView;
    }

    public void switchContent(FragmentBase to, String tag) {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.setting_container, to, tag).commitAllowingStateLoss();

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_COMM, this);

    }

    @Override
    public void onDestroyView() {
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_COMM, this);
        super.onDestroyView();
    }


    @Override
    public void onReceive(EntityMessage message) {
        switch (message.getOperation()) {
            case EntityMessage.OPERATION_SET:
                handleSet(message);
                break;
            default:
                break;
        }
    }

    private void handleSet(EntityMessage message) {
        if (message.getParameter() == ParameterComm.SETTING_TYPE) {
            int type = message.getData()[0];
            switch (type) {
                case TYPE_SETTING:
                    switchContent(fragmentSetting, TAG_SETTING);
                    break;
                case TYPE_UTILITIES:
                    switchContent(fragmentSettingUtilities, TAG_SETTING_UTILITIES);
                    break;
                case TYPE_DATE_TIME:
                    switchContent(fragmentSettingDateAndTime, TAG_SETTING_DATE_TIME);
                    break;
                case TYPE_HISTORY_LOG:
                    switchContent(fragmentSettingHistoryLog, TAG_SETTING_HISTORY_LOG);
                    break;
                case TYPE_TIPS:
                    switchContent(fragmentSettingTips, TAG_SETTING_TIPS);
                    break;
                default:

                    break;
            }
        } else if (message.getParameter() == ParameterComm.SETTING_TYPE_BACK) {
            switchContent(fragmentSetting, TAG_SETTING);
        }
    }

}
