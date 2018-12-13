package com.microtechmd.pda.control.platform;


import android.util.Log;

import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.parameter.ParameterGlobal;

import java.util.Arrays;


public final class DeviceBLE {
    private static DeviceBLE sInstance = null;
    private static EntityMessage.Listener sMessageListener = null;


    private class JNICallback
            implements JNIInterface.Callback {
        public int onHandleEvent(int address, int sourcePort, int targetPort,
                                 int event) {
            if (sMessageListener == null) {
                return EntityMessage.FUNCTION_FAIL;
            }
            sMessageListener.onReceive(new EntityMessage(address, address,
                    sourcePort, targetPort, event));

            return EntityMessage.FUNCTION_OK;
        }


        public int onHandleCommand(int address, int sourcePort, int targetPort,
                                   int mode, int operation, int parameter, byte[] data) {
            if (sMessageListener == null) {
                return EntityMessage.FUNCTION_FAIL;
            }
            sMessageListener.onReceive(new EntityMessage(address, address,
                    sourcePort, targetPort, mode, operation, parameter, data));

            return EntityMessage.FUNCTION_OK;
        }
    }


    private DeviceBLE() {
    }


    public static synchronized DeviceBLE getInstance(
            final EntityMessage.Listener listener) {
        sMessageListener = listener;

        if (sInstance == null) {
            sInstance = new DeviceBLE();
            JNIInterface.setCallback(sInstance.new JNICallback());
        }

        return sInstance;
    }


    public int send(final EntityMessage message) {
        if ((message
                .getTargetAddress() != ParameterGlobal.ADDRESS_REMOTE_MASTER) &&
                (message
                        .getTargetAddress() != ParameterGlobal.ADDRESS_REMOTE_SLAVE)) {
            return EntityMessage.FUNCTION_FAIL;
        }

        Log.e("发送数据", "param:" + message.getParameter() +
                "operation:" + message.getOperation() + "port:" + message.getSourcePort() +
                "Sourceaddress:" + message.getSourceAddress() + "getTargetAddress" + message.getTargetAddress());
        return JNIInterface.getInstance().send(message.getTargetAddress(),
                message.getSourcePort(), message.getTargetPort(), message.getMode(),
                message.getOperation(), message.getParameter(), message.getData());
    }


    public int query(int address) {
        return JNIInterface.getInstance().query(address);
    }


    public int switchLink(int address, int value) {
        return JNIInterface.getInstance().switchLink(address, value);
    }

    public void turnOff() {
        JNIInterface.getInstance().turnOff();
    }

    public void ready(byte[] data) {
        JNIInterface.getInstance().ready(data);
    }
}
