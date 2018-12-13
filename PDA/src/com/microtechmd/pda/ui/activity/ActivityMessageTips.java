package com.microtechmd.pda.ui.activity;


import android.os.Bundle;
import android.view.View;

import com.microtechmd.pda.R;
import com.microtechmd.pda.library.utility.SPUtils;
import com.microtechmd.pda.ui.widget.myswitchbutton.LukeSwitchButton;


public class ActivityMessageTips extends ActivityPDA {
    private LukeSwitchButton hi_switchBtn, low_switchBtn, comm_switchBtn, glucose_switchBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_messagetips);
        hi_switchBtn = (LukeSwitchButton) findViewById(R.id.hi_message_switch);
        low_switchBtn = (LukeSwitchButton) findViewById(R.id.low_message_switch);
        comm_switchBtn = (LukeSwitchButton) findViewById(R.id.comm_message_switch);
        glucose_switchBtn = (LukeSwitchButton) findViewById(R.id.glucose_message_switch);

        initSwitchBtn();
    }

    private void initSwitchBtn() {
        boolean hi_messageFlag = (boolean) SPUtils.get(this, HIMESSAGETIPS, true);
        boolean low_messageFlag = (boolean) SPUtils.get(this, LOMESSAGETIPS, true);
        boolean comm_messageFlag = (boolean) SPUtils.get(this, COMMMESSAGETIPS, true);
        boolean glucose_messageFlag = (boolean) SPUtils.get(this, GLUCOSEMESSAGETIPS, true);

        setSwitchState(hi_messageFlag, hi_switchBtn);
        setSwitchState(low_messageFlag, low_switchBtn);
        setSwitchState(comm_messageFlag, comm_switchBtn);
        setSwitchState(glucose_messageFlag, glucose_switchBtn);

        setSwitchToggleChange(HIMESSAGETIPS, hi_switchBtn);
        setSwitchToggleChange(LOMESSAGETIPS, low_switchBtn);
        setSwitchToggleChange(COMMMESSAGETIPS, comm_switchBtn);
        setSwitchToggleChange(GLUCOSEMESSAGETIPS, glucose_switchBtn);
    }


    private void setSwitchState(boolean state, LukeSwitchButton switchButton) {
        if (state) {
            switchButton.toggleOn();
        } else {
            switchButton.toggleOff();
        }
    }

    private void setSwitchToggleChange(final String key, LukeSwitchButton switchButton) {
        //开关切换事件
        switchButton.setOnToggleChanged(new LukeSwitchButton.OnToggleChanged() {
            @Override
            public void onToggle(boolean on) {
                if (on) {
                    SPUtils.put(ActivityMessageTips.this, key, true);
//                    Toast.makeText(ActivityMessageTips.this, "打开提示信息", Toast.LENGTH_SHORT).show();
                } else {
                    SPUtils.put(ActivityMessageTips.this, key, false);
//                    Toast.makeText(ActivityMessageTips.this, "关闭提示信息", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onClickView(View v) {
        super.onClickView(v);

        switch (v.getId()) {
            case R.id.ibt_back:
                finish();
                break;
            default:
                break;
        }
    }
}
