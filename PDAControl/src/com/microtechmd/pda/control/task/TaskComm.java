package com.microtechmd.pda.control.task;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import com.microtechmd.pda.control.platform.DeviceBLE;
import com.microtechmd.pda.control.util.SPUtils;
import com.microtechmd.pda.library.entity.DataBundle;
import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.comm.RFAddress;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.service.ServiceBase;
import com.microtechmd.pda.library.service.TaskBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Timer;

import static android.content.Context.ALARM_SERVICE;


public final class TaskComm extends TaskBase {
    // Constant and variable definition

    private static final int PDA_ERROR = 22;
    private static final int PDA_COMM_ERROR = 23;
    private static final String SETTING_BROADCAST_SWITCH = "broadcast_switch";
    public static final String RF_ADDRESS = "control_address";
    public static final String RF_MAC = "rf_mac";
    public static final String BONDID = "bondId";
    public static final String BONDKEY = "bondKey";
    private static final int REMOTE_DEVICE_COUNT = 2;
    private static final int INTERVAL_LINK_CHECK = 100;
    private static final int INTERVAL_CONNECTION_TIMEOUT_SHORT = 200;
    private static final int INTERVAL_CONNECTION_TIMEOUT_LONG = 10000;
    private static final int COMM_ERR_TIMEOUT = 15 * 60 * 1000;

    private Context context;
    private static TaskComm sInstance = null;
    private static DeviceBLE sDeviceBLE = null;

    private boolean mBroadcastSwitch;
    private byte mRFState = ParameterComm.COUNT_RF_STATE;
    private byte mRFSignal = 0;
    private int mConnectionTimeout = 0;
    private int mConnectionTimer = 0;
    private MessageList[] mMessageList = null;
    private EntityMessage[] mMessageRequest = null;
    private EntityMessage[] mMessageAcknowledge = null;
    private Handler[] mHandlerLink = null;
    private Runnable[] mRunnableLink = null;

    private Timer timer;

    private CountDownTimer countDownTimer;

    private boolean bondedFlag = false;
    private boolean bondingFlag = false;
    private EntityMessage mEntityMessage;
    private int bondCount = 0;
    private boolean sendPairRepeat = false;

    private byte[] rf_mac_address = null;
    // Inner class definition

    public void setmBroadcastSwitch(boolean flag) {
        this.mBroadcastSwitch = flag;
    }

    private AlarmManager manager;

    private final class MessageList extends LinkedList<EntityMessage> {
        private static final long serialVersionUID = 1L;
    }

    private final class Broadcast extends DataBundle {
        private static final int BROADCAST_LENGTH = 20;
        private static final int DATA_LENGTH = 18;

        private static final String IDENTIFIER = "broadcast";
        private static final String KEY_DATA = IDENTIFIER + "_data";
        private static final String KEY_RF_SIGNAL = IDENTIFIER + "_rf_signal";


        private Broadcast() {
            super();
        }


        private Broadcast(byte[] byteArray) {
            super();
            setByteArray(byteArray);
        }


        private byte[] getData() {
            return getExtras(KEY_DATA);
        }


        private int getRFSignal() {
            return (int) getByte(KEY_RF_SIGNAL);
        }


        private void setData(byte[] data) {
            setExtras(KEY_DATA, data);
        }


        private void setRFSignal(int signal) {
            setByte(KEY_RF_SIGNAL, (byte) signal);
        }


        @Override
        public byte[] getByteArray() {
            final DataOutputStreamLittleEndian dataOutputStream;
            final ByteArrayOutputStream byteArrayOutputStream;

            byteArrayOutputStream = new ByteArrayOutputStream();
            dataOutputStream =
                    new DataOutputStreamLittleEndian(byteArrayOutputStream);

            try {
                byteArrayOutputStream.reset();

                final byte[] data = getData();

                if (data != null) {
                    dataOutputStream.write(data);
                }

                dataOutputStream.writeByte((byte) getRFSignal());
                dataOutputStream.writeByte(0);
                dataOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return byteArrayOutputStream.toByteArray();
        }


        @Override
        public void setByteArray(byte[] byteArray) {
            if (byteArray == null) {
                return;
            }

            if (byteArray.length >= BROADCAST_LENGTH) {
                final DataInputStreamLittleEndian dataInputStream;
                final ByteArrayInputStream byteArrayInputStream;

                byteArrayInputStream = new ByteArrayInputStream(byteArray);
                dataInputStream =
                        new DataInputStreamLittleEndian(byteArrayInputStream);

                try {
                    clearBundle();
                    final byte[] data = new byte[DATA_LENGTH];
                    dataInputStream.read(data, 0, DATA_LENGTH);
                    setData(data);
                    setRFSignal((int) dataInputStream.readByte());
                    dataInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // Method definition

    private TaskComm(ServiceBase service) {
        super(service);
        context = service;
        manager = (AlarmManager) service.getSystemService(ALARM_SERVICE);
        mLog.Debug(getClass(), "Initialization");
        rf_mac_address = mService.getDataStorage(null).getExtras(RF_MAC,
                null);
        mConnectionTimeout = INTERVAL_CONNECTION_TIMEOUT_SHORT;
        mConnectionTimer = 0;

        mBroadcastSwitch = mService.getDataStorage(null)
                .getBoolean(SETTING_BROADCAST_SWITCH, false);

        if (mMessageList == null) {
            mMessageList = new MessageList[REMOTE_DEVICE_COUNT];
        }

        if (mMessageRequest == null) {
            mMessageRequest = new EntityMessage[REMOTE_DEVICE_COUNT];
        }

        if (mMessageAcknowledge == null) {
            mMessageAcknowledge = new EntityMessage[REMOTE_DEVICE_COUNT];
        }

        if (mHandlerLink == null) {
            mHandlerLink = new Handler[REMOTE_DEVICE_COUNT];
        }

        if (mRunnableLink == null) {
            mRunnableLink = new Runnable[REMOTE_DEVICE_COUNT];
        }

        for (int i = 0; i < REMOTE_DEVICE_COUNT; i++) {
            mMessageList[i] = new MessageList();
            mMessageRequest[i] = null;
            mMessageAcknowledge[i] = null;
            mHandlerLink[i] = new Handler();
            mRunnableLink[i] = null;
        }

        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                // Get broadcast data from RF slave device and switch it into
                // active broadcast mode
                EntityMessage message =
                        new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
                                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                                ParameterComm.PARAM_BROADCAST_OFFSET, new byte[]
                                {
                                        ParameterComm.BROADCAST_OFFSET_ALL
                                });
                mService.onReceive(message);
                message.setOperation(EntityMessage.OPERATION_GET);
                message.setData(null);
/*				message.setParameter(ParameterComm.PARAM_BROADCAST_DATA);
                mService.onReceive(message);*/
                message.setParameter(ParameterComm.PARAM_RF_STATE);
                mService.onReceive(message);
            }
        });
    }


    public static synchronized TaskComm getInstance(final ServiceBase service) {
        sDeviceBLE = DeviceBLE.getInstance(service);

        if (sInstance == null) {
            sInstance = new TaskComm(service);
        }

        return sInstance;
    }


    @Override
    public void handleMessage(final EntityMessage message) {
        switch (message.getTargetAddress()) {
            case ParameterGlobal.ADDRESS_REMOTE_MASTER:
                handleMessageRemote(message);
                break;
            case ParameterGlobal.ADDRESS_REMOTE_SLAVE:
                if (message.getParameter() == ParameterComm.PARAM_RF_LOCAL_ADDRESS) {
                    if (message.getData() != null) {
                        RFAddress address = new RFAddress(message.getData());
                        mService.getDataStorage(null).setExtras(RF_ADDRESS,
                                address.getByteArray());
                    }
                }
                handleMessageRemote(message);
                break;

            case ParameterGlobal.ADDRESS_LOCAL_CONTROL:

                if (message.getTargetPort() == ParameterGlobal.PORT_COMM) {
                    handleMessageLocal(message);
                } else {
                    mService.onReceive(message);
                }

                break;

            default:
                mService.onReceive(message);
                break;
        }
    }


    private void handleMessageRemote(final EntityMessage message) {
        // Check if the message is received from remote device or required
        // sending to remote device
        if (message.getSourceAddress() == message.getTargetAddress()) {
            if (message.getOperation() == EntityMessage.OPERATION_PAIR &&
                    message.getParameter() == ParameterComm.PARAM_MAC &&
                    message.getSourcePort() == ParameterGlobal.PORT_COMM) {
                byte[] data = message.getData();
                mLog.Error(getClass(), "配对返回：" + Arrays.toString(data));
                if (data.length >= 23) {
                    mLog.Error(getClass(), "配对成功");
                    byte[] bondId = new byte[6];
                    byte[] bondKey = new byte[16];
                    System.arraycopy(data, 1, bondId, 0, 6);
                    System.arraycopy(data, 7, bondKey, 0, bondKey.length);
                    mService.getDataStorage(null).setExtras(BONDID,
                            bondId);
                    mService.getDataStorage(null).setExtras(BONDKEY,
                            bondKey);
                }
                if (sendPairRepeat) {
                    sendRemoteMessage(mEntityMessage);
                    return;
                }
                receiveRemoteMessage(message);
                return;
            }
            if (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) {
                if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
                        (message.getParameter() == ParameterComm.PARAM_RF_LOCAL_ADDRESS)) {
                    if (message.getData() != null) {
                        if (message.getData().length >= 6) {
                            rf_mac_address = message.getData();
                            mLog.Error(getClass(), "MAC:" + Arrays.toString(rf_mac_address));
                            mService.getDataStorage(null).setExtras(RF_MAC,
                                    rf_mac_address);
                        }
                    }
                }
            }
            if ((message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER) && bondingFlag &&
                    (message.getOperation() != EntityMessage.OPERATION_EVENT)) {

                bondingFlag = false;
                if (message.getOperation() == EntityMessage.OPERATION_BOND &&
                        message.getParameter() == ParameterComm.PARAM_MAC &&
                        message.getSourcePort() == ParameterGlobal.PORT_COMM) {
                    if (message.getData()[0] == EntityMessage.FUNCTION_OK) {
                        mLog.Error(getClass(), "绑定成功");
                        bondedFlag = true;
                        handleMessage(mEntityMessage);
                        bondCount = 0;
                    } else {
                        mLog.Error(getClass(), "绑定返回，参数位失败");
                        mService.getDataStorage(null).setExtras(BONDID,
                                null);
                        mService.getDataStorage(null).setExtras(BONDKEY,
                                null);
                        changeRFState(ParameterComm.RF_STATE_BROADCAST);
                        bondedFlag = false;
                    }
                } else {
                    mLog.Error(getClass(), "绑定失败" + "param:" + message.getParameter() +
                            "operation:" + message.getOperation() + "port:" + message.getSourcePort() +
                            "address:" + message.getSourceAddress());
                    mService.getDataStorage(null).setExtras(BONDID,
                            null);
                    mService.getDataStorage(null).setExtras(BONDKEY,
                            null);
                    changeRFState(ParameterComm.RF_STATE_BROADCAST);
                    bondedFlag = false;
                }
            }
            receiveRemoteMessage(message);
        } else {
            if ((message.getOperation() == EntityMessage.OPERATION_SET) ||
                    (message.getOperation() == EntityMessage.OPERATION_GET) ||
                    (message.getOperation() == EntityMessage.OPERATION_NOTIFY) ||
                    (message.getOperation() == EntityMessage.OPERATION_ACKNOWLEDGE) ||
                    (message.getOperation() == EntityMessage.OPERATION_PAIR) ||
                    (message.getOperation() == EntityMessage.OPERATION_UNPAIR) ||
                    (message.getOperation() == EntityMessage.OPERATION_BOND)) {
                message.setMode(EntityMessage.MODE_ACKNOWLEDGE);
            } else if (message.getOperation() == EntityMessage.OPERATION_EVENT) {
                message.setMode(EntityMessage.MODE_NO_ACKNOWLEDGE);
            } else {
                return;
            }

            final MessageList messageList =
                    mMessageList[message.getTargetAddress()];
            messageList.add(new EntityMessage(message.toByteArray()));

            mLog.Debug(getClass(), "Add message list: " + messageList.size() +
                    ", Address: " + message.getTargetAddress());

            if (messageList.size() <= 1) {
                sendRemoteMessage(message);
            }
        }
    }


    private void handleMessageLocal(final EntityMessage message) {
        switch (message.getOperation()) {
            case EntityMessage.OPERATION_SET:
                setParameter(message);
                break;

            case EntityMessage.OPERATION_GET:
                getParameter(message);
                break;

            case EntityMessage.OPERATION_NOTIFY:
                handleNotification(message);
                break;

            case EntityMessage.OPERATION_ACKNOWLEDGE:
                handleAcknowledgement(message);
                break;

            case EntityMessage.OPERATION_EVENT:
                handleEvent(message);
                break;

            default:
                break;
        }
    }


    private void setParameter(final EntityMessage message) {
        int acknowledge = EntityMessage.FUNCTION_OK;


        if (message.getData() == null) {
            return;
        }

        switch (message.getParameter()) {
            case ParameterComm.PARAM_RF_STATE:
                mLog.Debug(getClass(), "Set RF state: " + message.getData()[0]);

                if (message.getData()[0] == ParameterComm.RF_STATE_CONNECTED) {
                    mConnectionTimeout = INTERVAL_CONNECTION_TIMEOUT_LONG;
                } else {
                    mConnectionTimeout = INTERVAL_CONNECTION_TIMEOUT_SHORT;
                }

                break;

            case ParameterComm.PARAM_RF_BROADCAST_SWITCH:
                mLog.Debug(getClass(), "Set broadcast switch: " +
                        message.getData()[0]);

                if (message.getData()[0] == 0) {
                    mBroadcastSwitch = false;
                } else {
                    mBroadcastSwitch = true;
                }

                mService.getDataStorage(null).setBoolean(SETTING_BROADCAST_SWITCH,
                        mBroadcastSwitch);
                break;
            case ParameterComm.CLEAN_DATABASES:
                SPUtils.clear(context);
                break;

            default:
                acknowledge = EntityMessage.FUNCTION_FAIL;
                break;
        }

        reverseMessagePath(message);
        message.setOperation(EntityMessage.OPERATION_ACKNOWLEDGE);
        message.setData(new byte[]
                {
                        (byte) acknowledge
                });
        handleMessage(message);
    }


    private void getParameter(final EntityMessage message) {
        int acknowledge = EntityMessage.FUNCTION_OK;
        byte[] value = null;


        switch (message.getParameter()) {
            case ParameterComm.PARAM_RF_STATE:
                mLog.Debug(getClass(), "Get RF state: " + mRFState);

                value = new byte[1];
                value[0] = mRFState;
                break;

            case ParameterComm.PARAM_RF_SIGNAL:
                mLog.Debug(getClass(), "Get RF signal: " + mRFSignal);

                value = new byte[1];
                value[0] = mRFSignal;
                break;

            default:
                acknowledge = EntityMessage.FUNCTION_FAIL;
        }

        reverseMessagePath(message);

        if (acknowledge == EntityMessage.FUNCTION_OK) {
            message.setOperation(EntityMessage.OPERATION_NOTIFY);
            message.setData(value);
        } else {
            message.setOperation(EntityMessage.OPERATION_ACKNOWLEDGE);
            message.setData(new byte[]
                    {
                            (byte) acknowledge
                    });
        }

        handleMessage(message);
    }


    private void handleNotification(final EntityMessage message) {
        if ((message
                .getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) &&
                (message.getSourcePort() == ParameterGlobal.PORT_COMM)) {
            switch (message.getParameter()) {
                case ParameterComm.PARAM_RF_STATE:
                    mLog.Debug(getClass(),
                            "Notify RF state: " + message.getData()[0]);


                    if (mRFState != message.getData()[0]) {
                        mRFState = message.getData()[0];
                        EntityMessage messageRequest;

                        if (mRFState != ParameterComm.RF_STATE_IDLE) {
                            messageRequest =
                                    mMessageList[ParameterGlobal.ADDRESS_REMOTE_MASTER]
                                            .peek();

                            if ((messageRequest != null) && (sDeviceBLE.query(
                                    ParameterGlobal.ADDRESS_REMOTE_MASTER) == EntityMessage.FUNCTION_OK)) {
                                sendRemoteMessage(messageRequest);
                            }
//                            if (timer != null) {
//                                timer.cancel();
//                                Event event = new Event(0, PDA_COMM_ERROR, 0);
//                                History history = new History(
//                                        new DateTime(Calendar.getInstance()), new Status(), event);
//                                handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
//                                        ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
//                                        ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_EVENT,
//                                        ParameterMonitor.PARAM_EVENT_COMM, history.getByteArray()));
//                            }

//                            if (countDownTimer != null) {
//                                countDownTimer.cancel();
//                                countDownTimer = null;
//                                Event event = new Event(0, PDA_COMM_ERROR, 0);
//                                History history = new History(
//                                        new DateTime(Calendar.getInstance()), new Status(), event);
//                                handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
//                                        ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
//                                        ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_EVENT,
//                                        ParameterMonitor.PARAM_EVENT_COMM, history.getByteArray()));
//                            }
                        } else {
                            messageRequest =
                                    mMessageList[ParameterGlobal.ADDRESS_REMOTE_MASTER]
                                            .poll();

                            while (messageRequest != null) {
                                sendFailEvent(messageRequest);
                                messageRequest =
                                        mMessageList[ParameterGlobal.ADDRESS_REMOTE_MASTER]
                                                .poll();
                            }

                            onRFSignalChanged((byte) 0);
                            checkLink(ParameterGlobal.ADDRESS_REMOTE_MASTER);
//
//                            timer = new Timer();
//                            TimerTask timerTask = new TimerTask() {
//                                @Override
//                                public void run() {
//                                    Event event = new Event(0, PDA_COMM_ERROR, 0);
//                                    History history = new History(
//                                            new DateTime(Calendar.getInstance()), new Status(), event);
//                                    handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
//                                            ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
//                                            ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_EVENT,
//                                            ParameterMonitor.PARAM_EVENT, history.getByteArray()));
//                                    timer.cancel();
//                                }
//                            };
//                            timer.schedule(timerTask, COMM_ERR_TIMEOUT);

                        }
                    }

                    break;

                case ParameterComm.PARAM_BROADCAST_DATA:
                    mLog.Debug(getClass(), "Notify broadcast data");
                    mLog.Debug("广播包数据**", Arrays.toString(message.getData()));

                    handleMessage(new EntityMessage(
                            ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                            ParameterGlobal.ADDRESS_LOCAL_VIEW,
                            ParameterGlobal.PORT_COMM,
                            ParameterGlobal.PORT_COMM,
                            EntityMessage.OPERATION_NOTIFY,
                            ParameterComm.DATA_TEST,
                            message.getData()));

                    if (mRFState == ParameterComm.RF_STATE_IDLE) {
                        mRFState = ParameterComm.COUNT_RF_STATE;
                    }

                    Broadcast broadcast = new Broadcast();
                    broadcast.setByteArray(message.getData());

                    onRFSignalChanged((byte) broadcast.getRFSignal());
                    mLog.Debug("广播包数据**", "flag :" + mBroadcastSwitch);
                    if (mBroadcastSwitch) {
                        handleMessage(new EntityMessage(
                                ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                ParameterGlobal.PORT_MONITOR,
                                ParameterGlobal.PORT_MONITOR,
                                EntityMessage.OPERATION_NOTIFY,
                                ParameterMonitor.PARAM_HISTORY,
                                broadcast.getData()));
                        mLog.Debug("广播包数据**", "广播包数据发送");
                    }

                    break;

                default:
                    break;
            }
        }
    }


    private void handleAcknowledgement(final EntityMessage message) {
        if ((message
                .getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) &&
                (message.getSourcePort() == ParameterGlobal.PORT_COMM)) {
            if (message.getData()[0] == EntityMessage.FUNCTION_OK) {
                mLog.Debug(getClass(), "Acknowledge OK");
            } else {
                mLog.Debug(getClass(), "Acknowledge Fail");
            }
        }
    }


    private void handleEvent(final EntityMessage message) {
        switch (message.getEvent()) {
            case EntityMessage.EVENT_SEND_DONE:
                break;

            case EntityMessage.EVENT_ACKNOWLEDGE:
                break;

            case EntityMessage.EVENT_TIMEOUT:
                mLog.Debug(getClass(), "Command timeout");
                break;

            default:
                break;
        }
    }


    private EntityMessage updateMessageList(int address) {
        EntityMessage message;
        EntityMessage messageRequest;


        mLog.Debug(getClass(), "Update message list: " +
                mMessageList[address].size() + ", Address: " + address);

        messageRequest = mMessageList[address].poll();
        message = mMessageList[address].peek();

        if (message != null) {
            sendRemoteMessage(message);
            return messageRequest;
        }

        checkLink(address);

        return messageRequest;
    }


    private void sendRemoteMessage(final EntityMessage message) {
        if (message.getTargetAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER) {
            mLog.Error(getClass(), "发送" + "operation:" + message.getOperation() +
                    "params:" + message.getParameter());
            if (mRFState != ParameterComm.RF_STATE_CONNECTED) {
                if (mRFState == ParameterComm.RF_STATE_IDLE) {
                    updateMessageList(message.getTargetAddress());
                    sendFailEvent(message);
                } else if (mRFState == ParameterComm.RF_STATE_BROADCAST) {
                    changeRFState(ParameterComm.RF_STATE_CONNECTED);
                    mLog.Error(getClass(), "发送链接");
                }
                bondedFlag = false;
                sDeviceBLE.turnOff();
                return;
            }
            if ((message.getOperation() != EntityMessage.OPERATION_PAIR) &&
                    (message.getOperation() != EntityMessage.OPERATION_EVENT)) {
                mEntityMessage = message;
                byte[] bondId = mService.getDataStorage(null).getExtras(BONDID, null);
                byte[] bondKey = mService.getDataStorage(null).getExtras(BONDKEY, null);
                sendPairRepeat = false;
                if (bondId == null) {
                    //重新配对
                    bondCount++;
                    mLog.Error(getClass(), "绑定次数：" + bondCount +
                            "sendParameter：" + message.getParameter());
                    if (bondCount > 3) {
                        //界面提示重新配对
                        bondCount = 0;
                        sendPairRepeat = false;
                        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                ParameterGlobal.PORT_COMM,
                                ParameterGlobal.PORT_COMM,
                                EntityMessage.OPERATION_SET,
                                ParameterComm.PAIRAGAIN,
                                null));

                    } else {
                        sendPairRepeat = true;
                        sDeviceBLE.send(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                                ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                ParameterGlobal.PORT_COMM,
                                ParameterGlobal.PORT_COMM,
                                EntityMessage.OPERATION_PAIR,
                                ParameterComm.PARAM_MAC,
                                rf_mac_address));
                    }
                    return;
                }

                if (!bondedFlag) {
                    mLog.Error(getClass(), "绑定：" + "param:" + message.getParameter() +
                            "operation:" + message.getOperation() + "port:" + message.getSourcePort() +
                            "address:" + message.getSourceAddress() + "bondKey:" + Arrays.toString(bondKey));
                    sDeviceBLE.send(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                            ParameterGlobal.ADDRESS_REMOTE_MASTER,
                            ParameterGlobal.PORT_COMM,
                            ParameterGlobal.PORT_COMM,
                            EntityMessage.OPERATION_BOND,
                            ParameterComm.PARAM_MAC,
                            bondId));
                    bondingFlag = true;
                    sDeviceBLE.ready(bondKey);
                    return;
                }
            }
        }
        if (sDeviceBLE.send(message) != EntityMessage.FUNCTION_OK) {
            mLog.Debug(getClass(), "Send remote fail");
        }
    }


    private void receiveRemoteMessage(final EntityMessage message) {
//        mLog.Error(getClass(), "PDA back message:"
//                + "getSourceAddress" + message.getSourceAddress() +
//                "Event():" + message.getEvent());
        message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_CONTROL);

        if (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) {
            if (message.getEvent() == EntityMessage.EVENT_ACKNOWLEDGE) {
                sendPDAErrorRecoveryAlert();
            }
        }
        if (((message.getOperation() == EntityMessage.OPERATION_NOTIFY) ||
                (message.getOperation() == EntityMessage.OPERATION_ACKNOWLEDGE) ||
                (message.getOperation() == EntityMessage.OPERATION_PAIR) ||
                (message.getOperation() == EntityMessage.OPERATION_UNPAIR) ||
                (message.getOperation() == EntityMessage.OPERATION_BOND)) &&
                (message.getMode() == EntityMessage.MODE_ACKNOWLEDGE)) {
            mMessageAcknowledge[message.getSourceAddress()] =
                    new EntityMessage(message.getTargetAddress(),
                            message.getSourceAddress(), message.getTargetPort(),
                            message.getSourcePort(), EntityMessage.OPERATION_EVENT,
                            message.getParameter(), new byte[]
                            {
                                    EntityMessage.EVENT_ACKNOWLEDGE
                            });
            checkLink(message.getSourceAddress());
        }

        EntityMessage messageRequest;

        if ((message.getOperation() == EntityMessage.OPERATION_EVENT) &&
                (message.getEvent() < EntityMessage.COUNT_EVENT)) {
//			获取消息列表中的第一个
            messageRequest = mMessageList[message.getSourceAddress()].peek();

            if (messageRequest != null) {
                message.setTargetAddress(messageRequest.getSourceAddress());
                message.setParameter(messageRequest.getParameter());

                if (messageRequest.getMode() == EntityMessage.MODE_ACKNOWLEDGE) {
                    if (message.getEvent() == EntityMessage.EVENT_ACKNOWLEDGE) {
                        mMessageRequest[message.getSourceAddress()] =
                                updateMessageList(message.getSourceAddress());
                    } else if (message.getEvent() == EntityMessage.EVENT_TIMEOUT) {
                        mMessageRequest[message.getSourceAddress()] = null;
                        updateMessageList(message.getSourceAddress());

                        if (message
                                .getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) {
                            sendPDAErrorAlert();
                        }
                    }
                } else {
                    if (message.getEvent() == EntityMessage.EVENT_SEND_DONE) {
                        mMessageRequest[message.getSourceAddress()] = null;
                        updateMessageList(message.getSourceAddress());
                    }
                }
            }
        } else {
            messageRequest = mMessageRequest[message.getSourceAddress()];

            if (messageRequest != null) {
                if ((message.getSourcePort() == messageRequest.getTargetPort()) &&
                        (message.getTargetPort() == messageRequest.getSourcePort()) &&
                        (message.getParameter() == messageRequest.getParameter()) &&
                        ((message.getOperation() == EntityMessage.OPERATION_NOTIFY) ||
                                (message.getOperation() == EntityMessage.OPERATION_ACKNOWLEDGE) ||
                                (message.getOperation() == EntityMessage.OPERATION_PAIR) ||
                                (message.getOperation() == EntityMessage.OPERATION_UNPAIR) ||
                                (message.getOperation() == EntityMessage.OPERATION_BOND))) {
                    message.setTargetAddress(messageRequest.getSourceAddress());
                }

                mMessageRequest[message.getSourceAddress()] = null;
            }
        }

        handleMessage(message);
    }


    private void reverseMessagePath(final EntityMessage message) {
        message.setTargetAddress(message.getSourceAddress());
        message.setSourceAddress(ParameterGlobal.ADDRESS_LOCAL_CONTROL);
        message.setTargetPort(message.getSourcePort());
        message.setSourcePort(ParameterGlobal.PORT_COMM);
    }


    private void sendFailEvent(final EntityMessage message) {
        mLog.Debug(getClass(), "Send fail event");

        final EntityMessage messageEvent =
                new EntityMessage(message.getTargetAddress(),
                        message.getSourceAddress(), message.getTargetPort(),
                        message.getSourcePort(), EntityMessage.EVENT_SEND_DONE);
        messageEvent.setParameter(message.getParameter());

        handleMessage(messageEvent);

        if ((message.getOperation() == EntityMessage.OPERATION_SET) ||
                (message.getOperation() == EntityMessage.OPERATION_GET) ||
                (message.getOperation() == EntityMessage.OPERATION_UNPAIR) ||
                (message.getOperation() == EntityMessage.OPERATION_PAIR)) {
            messageEvent.setEvent(EntityMessage.EVENT_TIMEOUT);
        }

        handleMessage(messageEvent);
    }


    private void changeRFState(byte state) {
        mLog.Debug(getClass(), "Change RF state: " + state);

        mRFState = ParameterComm.COUNT_RF_STATE;
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.PARAM_RF_STATE, new byte[]
                {
                        state
                }));
    }

    private Handler comm_handler;
    private Runnable mRunnable;
    private PendingIntent sender;
    int i = 0;

    private void onRFSignalChanged(byte signal) {
        mRFSignal = signal;
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_NOTIFY,
                ParameterComm.PARAM_RF_SIGNAL, new byte[]
                {
                        signal
                }));

        if (((int) signal) <= 0) {
            Intent intent = new Intent();
            // 设置Intent action属性
            intent.setAction("comm_err");
            // 实例化PendingIntent
            if (sender == null) {
                sender = PendingIntent.getBroadcast(mService, 123,
                        intent, 0);
            }

            assert manager != null;
            manager.set(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + COMM_ERR_TIMEOUT, sender);


//            if (comm_handler == null) {
//                comm_handler = new Handler(Looper.getMainLooper());
//                mRunnable = new Runnable() {
//                    @Override
//                    public void run() {
//                        countDownTimer = new CountDownTimer(COMM_ERR_TIMEOUT, 5 * 1000) {
//                            @Override
//                            public void onTick(long l) {
//                                i++;
//                                mLog.Error(TaskComm.this.getClass(), ("到时+" + i));
//                            }
//
//                            @Override
//                            public void onFinish() {
//                                mLog.Error(TaskComm.this.getClass(), "执行");
//                                handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
//                                        ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
//                                        ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_NOTIFY,
//                                        ParameterComm.COMM_ERR, null));
//                                i = 0;
//                            }
//                        };
//                        countDownTimer.start();
//
//                    }
//                };
//            }
//            comm_handler.post(mRunnable);
        } else {
            if (sender != null) {
                manager.cancel(sender);
                sender = null;
            }
            handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                    ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                    ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_NOTIFY,
                    ParameterComm.COMM_ERR_RECOVERY, null));
            i = 0;
            if (comm_handler != null) {
                comm_handler.removeCallbacks(mRunnable);
                comm_handler = null;
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                    countDownTimer = null;
                }
            }
        }

    }


    private void sendPDAErrorAlert() {
        Event event = new Event(0, PDA_ERROR, 0);
        History history = new History(
                new DateTime(Calendar.getInstance()), new Status(), event);

        DataList dataList = new DataList();
        dataList.pushData(history.getByteArray());
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
                ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_NOTIFY,
                ParameterMonitor.PARAM_HISTORY, dataList.getByteArray()));
    }

    private void sendPDAErrorRecoveryAlert() {
        Event event = new Event(0, PDA_ERROR, 0);
        History history = new History(
                new DateTime(Calendar.getInstance()), new Status(), event);
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
                ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_EVENT,
                ParameterMonitor.PDA_ERROR_RECOVER, history.getByteArray()));
    }


    private void checkLink(final int address) {
        mLog.Debug(getClass(), "Check link: " + address);

        if (mRunnableLink[address] == null) {
            mRunnableLink[address] = new Runnable() {
                @Override
                public void run() {
                    if ((mMessageList[address].size() == 0) && (sDeviceBLE
                            .query(address) == EntityMessage.FUNCTION_OK)) {
                        if (mMessageAcknowledge[address] == null) {
                            mLog.Debug(TaskComm.class, "Link idle: " + address);

                            if (address == ParameterGlobal.ADDRESS_REMOTE_MASTER) {
                                mConnectionTimer += INTERVAL_LINK_CHECK;

                                if (mConnectionTimer >= mConnectionTimeout) {
                                    mConnectionTimer = 0;

                                    if (mRFState == ParameterComm.RF_STATE_CONNECTED) {
                                        changeRFState(
                                                ParameterComm.RF_STATE_BROADCAST);
                                    }

                                    sDeviceBLE.switchLink(
                                            ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                            0);
                                    sDeviceBLE.switchLink(
                                            ParameterGlobal.ADDRESS_REMOTE_MASTER,
                                            1);
                                } else {
                                    mHandlerLink[address].postDelayed(this,
                                            INTERVAL_LINK_CHECK);
                                }
                            }

                            return;
                        } else {
                            handleMessage(mMessageAcknowledge[address]);
                            mMessageAcknowledge[address] = null;
                        }
                    }

                    mLog.Debug(TaskComm.class, "Link busy: " + address);

                    if (address == ParameterGlobal.ADDRESS_REMOTE_MASTER) {
                        mConnectionTimer = 0;
                    }

                    mHandlerLink[address].postDelayed(this,
                            INTERVAL_LINK_CHECK);
                }
            };
        }

        if (address == ParameterGlobal.ADDRESS_REMOTE_MASTER) {
            mConnectionTimer = 0;
        }

        mHandlerLink[address].removeCallbacks(mRunnableLink[address]);
        mHandlerLink[address].postDelayed(mRunnableLink[address],
                INTERVAL_LINK_CHECK);
    }
}
