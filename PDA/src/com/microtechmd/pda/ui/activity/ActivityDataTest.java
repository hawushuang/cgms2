package com.microtechmd.pda.ui.activity;


import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ValueShort;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.SPUtils;

import java.util.Arrays;


public class ActivityDataTest extends ActivityPDA {
    private EditText input_testdata;
    private TextView respond;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_datatest);
        input_testdata = (EditText) findViewById(R.id.input_testdata);
        respond = (TextView) findViewById(R.id.respond);

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (isShouldHideInput(v, ev)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
            return super.dispatchTouchEvent(ev);
        }
        // 必不可少，否则所有的组件都不会有TouchEvent了
        if (getWindow().superDispatchTouchEvent(ev)) {
            return true;
        }
        return onTouchEvent(ev);
    }

    public boolean isShouldHideInput(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] leftTop = {0, 0};
            //获取输入框当前的location位置
            v.getLocationInWindow(leftTop);
            int left = leftTop[0];
            int top = leftTop[1];
            int bottom = top + v.getHeight();
            int right = left + v.getWidth();
            if (event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom) {
                // 点击的是输入框区域，保留点击EditText的事件
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onClickView(View v) {
        super.onClickView(v);

        switch (v.getId()) {
            case R.id.ibt_back:
                finish();
                break;
            case R.id.send:
                String request = input_testdata.getText().toString().trim();
                if (TextUtils.isEmpty(request)) {
                    Toast.makeText(this, R.string.actions_pump_data_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (request.length() > 18) {
                    Toast.makeText(this, R.string.actions_pump_data_long, Toast.LENGTH_SHORT).show();
                } else {
                    byte[] sendData = new byte[18];
                    System.arraycopy(request.getBytes(), 0, sendData, 0, request.getBytes().length);
                    handleMessage(new EntityMessage(
                            ParameterGlobal.ADDRESS_LOCAL_VIEW,
                            ParameterGlobal.ADDRESS_REMOTE_MASTER,
                            ParameterGlobal.PORT_COMM,
                            ParameterGlobal.PORT_COMM,
                            EntityMessage.OPERATION_SET,
                            ParameterComm.PARAM_BROADCAST_DATA,
                            sendData));
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void handleNotification(EntityMessage message) {
        super.handleNotification(message);
        if ((message.getParameter() == ParameterComm.DATA_TEST)) {
            mLog.Error(getClass(), "测试数据接收：" + Arrays.toString(message.getData()));
            byte[] recieveData = new byte[18];
            if (message.getData().length > 0) {
                System.arraycopy(message.getData(), 0, recieveData, 0, 18);
            }
            int zeroCount = 0; // 统计0的个数
            for (int i = 0; i < 18; i++) {
                if (recieveData[i] == 0) {
                    zeroCount++;
                }
            }
            if (zeroCount == 18) {
                return;
            }
            byte[] printData = new byte[18 - zeroCount];
            int index = 0;
            for (int i = 0; i < 18; i++) {
                if (recieveData[i] != 0) {
                    printData[index] = recieveData[i];
                    index++;
                }
            }
            if (printData.length > 0) {
                respond.setText(new String(printData));
            }
        }
    }
}
