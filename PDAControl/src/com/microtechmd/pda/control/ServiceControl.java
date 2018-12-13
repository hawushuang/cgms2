package com.microtechmd.pda.control;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Messenger;

import com.microtechmd.pda.control.task.TaskComm;
import com.microtechmd.pda.control.task.TaskGlucose;
import com.microtechmd.pda.control.task.TaskMonitor;
import com.microtechmd.pda.control.task.TaskSystem;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.service.ServiceBase;


public final class ServiceControl extends ServiceBase {
    // Constant and variable definition

    private Messenger mMessengerView = null;
    private Messenger mMessengerModel = null;
    private ModelConnection mModelConnection = null;


    // Inner class definition

    private final class ModelConnection
            implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mLog.Debug(getClass(), "Connect model service");
            mMessengerModel = new Messenger(service);
        }


        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLog.Debug(getClass(), "Disconnect model service");
            mMessengerModel = null;
        }
    }


    // Method definition

    @Override
    public void onCreate() {
        super.onCreate();

        setExternalMessageHandler(new ExternalMessageHandler() {
            @Override
            public void handleMessage(final EntityMessage message,
                                      Messenger messengerReply) {
                if (messengerReply != null) {
                    if (message
                            .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_VIEW) {
                        mMessengerView = messengerReply;
                    }

                    if (message
                            .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_MODEL) {
                        mMessengerModel = messengerReply;
                    }
                }

                handleMessageExternal(message);
            }
        });

        if (mModelConnection == null) {
            mModelConnection = new ModelConnection();
            bindService(new Intent("com.microtechmd.pda.model"),
                    mModelConnection, Context.BIND_AUTO_CREATE);
        }
    }


    @Override
    public void onDestroy() {
        mMessengerView = null;
        mMessengerModel = null;
        mModelConnection = null;
        super.onDestroy();
    }


    private synchronized void handleMessageExternal(final EntityMessage message) {
        mLog.Debug(getClass(), "Handle message: " + "Source Address:" +
                message.getSourceAddress() + " Target Address:" +
                message.getTargetAddress() + " Source Port:" +
                message.getSourcePort() + " Target Port:" +
                message.getTargetPort() + " Operation:" + message.getOperation() +
                " Parameter:" + message.getParameter());

        switch (message.getTargetAddress()) {
            case ParameterGlobal.ADDRESS_REMOTE_MASTER:
            case ParameterGlobal.ADDRESS_REMOTE_SLAVE:
                TaskComm taskComm = TaskComm.getInstance(ServiceControl.this);
                taskComm.handleMessage(message);
                break;

//				将消息返回界面
            case ParameterGlobal.ADDRESS_LOCAL_VIEW:
                sendRemoteMessage(mMessengerView, message);
                break;

            case ParameterGlobal.ADDRESS_LOCAL_MODEL:
                sendRemoteMessage(mMessengerModel, message);
                break;

            default:
                handleMessageInternal(message);
                break;
        }

        mLog.Debug(getClass(), "Handle message end");
    }


    private void handleMessageInternal(final EntityMessage message) {
        switch (message.getTargetPort()) {
            case ParameterGlobal.PORT_SYSTEM:
                TaskSystem taskSystem =
                        TaskSystem.getInstance(ServiceControl.this);
                taskSystem.handleMessage(message);
                break;

            case ParameterGlobal.PORT_COMM:
                TaskComm taskComm = TaskComm.getInstance(ServiceControl.this);
                taskComm.handleMessage(message);
                break;

            case ParameterGlobal.PORT_GLUCOSE:
                TaskGlucose taskGlucose =
                        TaskGlucose.getInstance(ServiceControl.this);
                taskGlucose.handleMessage(message);
                break;

            case ParameterGlobal.PORT_MONITOR:
                TaskMonitor taskMonitor =
                        TaskMonitor.getInstance(ServiceControl.this);
                taskMonitor.handleMessage(message);
                break;

            default:
                break;
        }
    }
}
