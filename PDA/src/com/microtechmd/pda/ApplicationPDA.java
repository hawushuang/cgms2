package com.microtechmd.pda;


import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.microtechmd.pda.database.DbHistory;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.LogPDA;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class ApplicationPDA extends Application {
    public final static int KEY_CODE_BOLUS = 111;

    private static LogPDA sLog = null;
    private static Messenger sMessengerView = null;
    private static Messenger sMessengerControl = null;
    private static Messenger sMessengerModel = null;
    private static ControlConnection sControlConnection = null;
    private static ModelConnection sModelConnection = null;
    private static MessageListenerInternal[] sMessageListenerInternal = null;


    private List<History> dataListAll;
    private ArrayList<DbHistory> dataErrListAll;

    public List<History> getDataListAll() {
        if (dataListAll == null) {
            dataListAll = new ArrayList<>();
        }
        return dataListAll;
    }

    public void setDataListAll(List<History> dataListAll) {
        this.dataListAll = dataListAll;
    }

    public ArrayList<DbHistory> getDataErrListAll() {
        if (dataErrListAll == null) {
            dataErrListAll = new ArrayList<>();
        }
        return dataErrListAll;
    }

    public void setDataErrListAll(ArrayList<DbHistory> dataErrListAll) {
        this.dataErrListAll = dataErrListAll;
    }
    // Inner class definition

    private static class MessageHandler extends Handler {
        private final WeakReference<ApplicationPDA> mApplication;


        private MessageHandler(ApplicationPDA application) {
            super();
            mApplication = new WeakReference<ApplicationPDA>(application);
        }


        @Override
        public void handleMessage(Message msg) {
            EntityMessage message = new EntityMessage();
            message.setAll(msg.getData());
            ApplicationPDA application = mApplication.get();
            application.handleMessage(message);
        }
    }

    private class ControlConnection
            implements
            ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            sLog.Debug(getClass(), "Connect control service");
            sMessengerControl = new Messenger(service);

            handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                    ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                    ParameterGlobal.PORT_COMM, ParameterGlobal.PORT_COMM,
                    EntityMessage.OPERATION_GET, ParameterComm.PARAM_RF_SIGNAL,
                    null));
            /*
             * handleMessage(new
             * EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
             * ParameterGlobal.ADDRESS_LOCAL_CONTROL,
             * ParameterGlobal.PORT_MONITOR, ParameterGlobal.PORT_MONITOR,
             * EntityMessage.OPERATION_GET, ParameterMonitor.PARAM_STATUS,
             * null)); handleMessage(new
             * EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
             * ParameterGlobal.ADDRESS_LOCAL_CONTROL,
             * ParameterGlobal.PORT_MONITOR, ParameterGlobal.PORT_GLUCOSE,
             * EntityMessage.OPERATION_GET, ParameterGlucose.PARAM_STATUS,
             * null));
             */
        }


        @Override
        public void onServiceDisconnected(ComponentName name) {
            sLog.Debug(getClass(), "Disconnect control service");
            sMessengerControl = null;
        }
    }

    private class ModelConnection
            implements
            ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            sLog.Debug(getClass(), "Connect model service");
            sMessengerModel = new Messenger(service);
        }


        @Override
        public void onServiceDisconnected(ComponentName name) {
            sLog.Debug(getClass(), "Disconnect model service");
            sMessengerModel = null;
        }
    }

    private class MessageListenerInternal extends ArrayList<EntityMessage.Listener> {
        private static final long serialVersionUID = 1L;
    }


    // Method definition

    @Override
    public void onCreate() {
        super.onCreate();

        if (sLog == null) {
            sLog = new LogPDA();
        }

        if (sMessengerView == null) {
            sMessengerView =
                    new Messenger(new MessageHandler(ApplicationPDA.this));
        }

        if (sControlConnection == null) {
            sControlConnection = new ControlConnection();
            bindService(new Intent("com.microtechmd.pda.control"),
                    sControlConnection, Context.BIND_AUTO_CREATE);
        }

        if (sModelConnection == null) {
            sModelConnection = new ModelConnection();
            bindService(new Intent("com.microtechmd.pda.model"),
                    sModelConnection, Context.BIND_AUTO_CREATE);
        }

        if (sMessageListenerInternal == null) {
            sMessageListenerInternal =
                    new MessageListenerInternal[ParameterGlobal.COUNT_PORT];

            for (int i = 0; i < ParameterGlobal.COUNT_PORT; i++) {
                sMessageListenerInternal[i] = new MessageListenerInternal();
            }
        }
    }

    public void registerMessageListener(int port,
                                        final EntityMessage.Listener listener) {
        if (port < ParameterGlobal.COUNT_PORT) {
            sMessageListenerInternal[port].add(listener);
        }
    }


    public void unregisterMessageListener(int port,
                                          final EntityMessage.Listener listener) {
        if (port < ParameterGlobal.COUNT_PORT) {
            sMessageListenerInternal[port].remove(listener);
        }
    }


    public void clearMessageListener(int port) {
        if (port < ParameterGlobal.COUNT_PORT) {
            sMessageListenerInternal[port].clear();
        }
    }


    public void handleMessage(final EntityMessage message) {
        switch (message.getTargetAddress()) {
            case ParameterGlobal.ADDRESS_LOCAL_VIEW:
                handleMessageInternal(message);
                break;

            case ParameterGlobal.ADDRESS_LOCAL_MODEL:
                sendRemoteMessage(sMessengerModel, message);
                break;

            default:
                sendRemoteMessage(sMessengerControl, message);
                break;
        }
    }


    private void handleMessageInternal(final EntityMessage message) {
        int port;

        port = message.getTargetPort();

        if (port < ParameterGlobal.COUNT_PORT) {
            for (EntityMessage.Listener listener : sMessageListenerInternal[port]) {
                listener.onReceive(message);
            }
        }
    }


    private void sendRemoteMessage(final Messenger messenger,
                                   final EntityMessage message) {
        if (messenger != null) {
            Message messageRemote = Message.obtain();
            messageRemote.setData(message.getAll());
            messageRemote.replyTo = sMessengerView;

            try {
                messenger.send(messageRemote);
            } catch (RemoteException e) {
                sLog.Debug(getClass(), "Send remote message fail");
                e.printStackTrace();
            }
        }
    }
}
