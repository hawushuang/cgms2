package com.microtechmd.pda.control.task;


import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import com.microtechmd.pda.control.util.DbHistory;
import com.microtechmd.pda.control.util.SPUtils;
import com.microtechmd.pda.library.entity.DataBundle;
import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.EntityMessage;
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
import com.microtechmd.pda.library.service.ServiceBase;
import com.microtechmd.pda.library.service.TaskBase;
import com.microtechmd.pda.library.utility.ByteUtil;


public final class TaskMonitor extends TaskBase {
    // Constant and variable definition
    private static final int SENSOR_NEW = 4;
    public static final int SENSOR_EXPIRATION = 6;
    private static final String SETTING_STATUS_LAST = "status_last";
    private static final String SETTING_BOLUS_LAST = "bolus_last";
    private static final String SETTING_HISTORY_SYNC = "history_sync";

    private static final String SETTING_BROADCAST_SAVE = "broadcast_save";
    private static final int GLUCOSE = 7;
    private static final int GLUCOSE_RECOMMEND_CAL = 8;
    private static final int HYPO = 10;
    private static final int HYPER = 11;
    private static final int BLOOD_GLUCOSE = 13;
    private static final int CALIBRATION = 14;
    private String MEVENT_INDEX_MODEL = "index_model";
    private String SENSORINDEX = "sensorIndex";

    private static final int EVENT_INDEX_MAX = 10000;

    private static TaskMonitor sInstance = null;

    private DataList mStatusLast = null;
    private History mBolusLast = null;
    private boolean mIsNewStatusPump = false;
    private boolean mIsHistorySync = false;
    private int mEventIndexRemote = -1;//广播包数据index最大值
    private int mEventIndexModel = -1;
    private boolean mBroadcastSave = true;
    private boolean synchronizeDone;

    private History history_broadcast;
    private int sensorIndex;

    private boolean forceSynchronizeFlag = false;
    private boolean sendFlag = true;
    private boolean timeSet = false;

    // Method definition
    private TaskMonitor(ServiceBase service) {
        super(service);
        mBroadcastSave = (boolean) SPUtils.get(mService, SETTING_BROADCAST_SAVE, true);
        synchronizeDone = true;
        sensorIndex = (int) SPUtils.get(mService, SENSORINDEX, 0);
        mEventIndexModel = (int) SPUtils.get(mService, MEVENT_INDEX_MODEL, 0) + 1;
        History mStatusLastHistory = new History(
                mService.getDataStorage(null).getExtras(SETTING_STATUS_LAST, null));
        mStatusLast = new DataList();
        mStatusLast.pushData(mStatusLastHistory.getByteArray());
        mBolusLast = new History(
                mService.getDataStorage(null).getExtras(SETTING_BOLUS_LAST, null));

        mIsNewStatusPump = false;
        mIsHistorySync = mService.getDataStorage(null)
                .getBoolean(SETTING_HISTORY_SYNC, false);

        if (mIsHistorySync) {
            EntityMessage message =
                    new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                            ParameterGlobal.ADDRESS_LOCAL_MODEL,
                            ParameterGlobal.PORT_MONITOR, ParameterGlobal.PORT_MONITOR,
                            EntityMessage.OPERATION_GET, ParameterMonitor.PARAM_HISTORY,
                            null);
            DataList datalist = new DataList();
            History history = new History();
            history.setDateTime(new DateTime(-1, -1, -1, -1, -1, -1));
            history.setStatus(new Status(-1));
            history.setEvent(
                    new Event(0, -1, -1));
            datalist.pushData(history.getByteArray());
            history
                    .setEvent(new Event(0, -1, -1));
            datalist.pushData(history.getByteArray());
            message.setData(datalist.getByteArray());
            mService.onReceive(message);
        }
        mLog.Debug(getClass(), "Initialization");
    }


    public static synchronized TaskMonitor getInstance(
            final ServiceBase service) {
        if (sInstance == null) {
            sInstance = new TaskMonitor(service);
        }

        return sInstance;
    }


    @Override
    public void handleMessage(EntityMessage message) {
        if ((message
                .getTargetAddress() == ParameterGlobal.ADDRESS_LOCAL_CONTROL) &&
                (message.getTargetPort() == ParameterGlobal.PORT_MONITOR)) {
            switch (message.getOperation()) {
                case EntityMessage.OPERATION_SET:
                    setParameter(message);
                    break;

                case EntityMessage.OPERATION_GET:
                    getParameter(message);
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
        } else {
            mService.onReceive(message);
        }
    }

    private void setParameter(final EntityMessage message) {
        mLog.Debug(getClass(), "Set parameter: " + message.getParameter());

        switch (message.getParameter()) {
            case ParameterComm.BROADCAST_SAVA:
                mBroadcastSave = message.getData()[0] != 0;

                SPUtils.put(mService, SETTING_BROADCAST_SAVE, mBroadcastSave);
                break;
            case ParameterComm.SYNCHRONIZEDONE:
                synchronizeDone = true;
                break;
            case ParameterComm.FORCESYNCHRONIZEFLAG:
                forceSynchronizeFlag = true;
                break;
            case ParameterComm.SYNCHRONIZE_DATA:
                synchronizeHistoryManual();
                break;
            case ParameterComm.ADDCHANGE:
                mEventIndexModel = 1;
                break;
            case ParameterComm.CLEAN_DATABASES:
                mEventIndexModel = 1;
                break;
            case ParameterComm.PAIR_SUCCESS:
                if (message.getData() != null) {
                    mLog.Error(getClass(), "数组" + Arrays.toString(message.getData()));
                    byte[] sensorBytes = Arrays.copyOfRange(message.getData(), 0, 4);
                    byte[] eventBytes = Arrays.copyOfRange(message.getData(), 4, 8);
                    sensorIndex = ByteUtil.bytesToInt(sensorBytes);
                    mEventIndexModel = ByteUtil.bytesToInt(eventBytes) + 1;
                }
                break;
        }
    }


    private void getParameter(final EntityMessage message) {
        int acknowledge = EntityMessage.FUNCTION_OK;
        byte[] value = null;

        switch (message.getParameter()) {
            case ParameterMonitor.PARAM_STATUS:
                mLog.Debug(getClass(), "Get pump status");

                History statusPump =
                        new History(mStatusLast.getByteArray());
                statusPump.setDateTime(mBolusLast.getDateTime());
                value = statusPump.getByteArray();
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


    private void handleEvent(final EntityMessage message) {
        mLog.Debug(getClass(), "Handle event: " + message.getEvent());

        switch (message.getEvent()) {
            case EntityMessage.EVENT_SEND_DONE:
                break;

            case EntityMessage.EVENT_ACKNOWLEDGE:
                break;

            case EntityMessage.EVENT_TIMEOUT:
                mLog.Debug(getClass(), "Command Time Out!");
                break;
        }
    }


    private void handleNotification(final EntityMessage message) {
        mLog.Debug(getClass(), "Notify parameter: " + message.getParameter());

        if (message.getSourcePort() == ParameterGlobal.PORT_MONITOR) {
            if (message.getParameter() == ParameterMonitor.PARAM_HISTORY
                    || message.getParameter() == ParameterMonitor.PARAM_HISTORIES) {
                onNotifyHistory(message);
            }
        }

//        if (mIsNewStatusPump) {
//            mLog.Debug(getClass(), "Update status pump");
//
//            mIsNewStatusPump = false;
////            Log.e("界面发送广播包：", Arrays.toString(mStatusLast.getByteArray()));
//            message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_VIEW);
//            message.setOperation(EntityMessage.OPERATION_NOTIFY);
//            message.setParameter(ParameterMonitor.PARAM_STATUS);
//            message.setData(mStatusLast.getByteArray());
//            handleMessage(message);
//        }
    }


    private void handleAcknowledgement(final EntityMessage message) {
        mLog.Debug(getClass(),
                "Acknowledge port comm: " + message.getData()[0]);
        if (message.getParameter() == ParameterGlucose.TASK_GLUCOSE_PARAM_NEW_SENSOR) {
            if (message.getData()[0] == EntityMessage.FUNCTION_OK) {
                if (history_broadcast != null) {
                    synchronizeDateTime(history_broadcast);
                }
            }
        }

        if (message.getParameter() == ParameterMonitor.PARAM_DATETIME) {
            timeSet = true;
        }
    }


    private void reverseMessagePath(EntityMessage message) {
        message.setTargetAddress(message.getSourceAddress());
        message.setSourceAddress(ParameterGlobal.ADDRESS_LOCAL_CONTROL);
        message.setTargetPort(message.getSourcePort());
        message.setSourcePort(ParameterGlobal.PORT_MONITOR);
    }


    private void onNotifyHistory(final EntityMessage message) {
        mLog.Error(getClass(), "SourceAddress()" + message.getSourceAddress()
                + "Parameter()" + message.getParameter());

//		连接后返回的数据
        if (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER) {
            onNotifyHistoryRemote(message);
        }
//		本地数据库返回的数据
        else if (message
                .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_MODEL) {
            onNotifyHistoryModel(message);
        }
//		解配后数据清除
        else if (message
                .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_VIEW) {
            onNotifyHistoryView(message);
        }
//		接收广播包数据
        else if (message
                .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_CONTROL) {
            onNotifyHistoryControl(message);
        }
    }


    private List<byte[]> getListIntArray(byte[] dd, int b) {
        List<byte[]> aa = new ArrayList<>();
        // tyy 取整代表可以拆分的数组个数
        int f = dd.length / b;
        for (int i = 0; i < f; i++) {
            byte[] bbb = new byte[b];
            System.arraycopy(dd, i * b, bbb, 0, b);
            aa.add(bbb);
        }
        return aa;
    }

    private int flag;


    private void onNotifyHistoryRemote(final EntityMessage message) {
        mLog.Debug(getClass(), "Notify history remote begin");
        long t1 = System.currentTimeMillis();
//        EntityMessage messageDone = new EntityMessage();
//        messageDone.setSourceAddress(ParameterGlobal.ADDRESS_LOCAL_CONTROL);
//        messageDone.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_VIEW);
//        messageDone.setSourcePort(ParameterGlobal.PORT_MONITOR);
//        messageDone.setTargetPort(ParameterGlobal.PORT_MONITOR);
//        messageDone.setOperation(EntityMessage.OPERATION_NOTIFY);
//        messageDone.setParameter(ParameterMonitor.SYNCHRONIZEDONE);
//        messageDone.setData(new byte[]{0});
//        handleMessage(messageDone);

        DataList dataList = new DataList();

        byte[] data = message.getData();
        int pointer = 0;
        boolean error = false;

        byte[] history_byte = new byte[14];
        byte[] short_byte = new byte[2];
        byte[] int_byte = new byte[4];
        int index = 0;
        int dateTime = 0;
        int value = 0;

        for (int i = 0; pointer < data.length; i++) {
            if (i == 0) {
                if (data.length < 7) {
                    error = true;
                    break;
                }

                history_byte[0] = 0;
                history_byte[1] = 0;
                System.arraycopy(data, 2, history_byte, 2, 4);
                System.arraycopy(data, 0, history_byte, 6, 2);
                history_byte[8] = ByteUtil.intToByte(sensorIndex);
                history_byte[9] = data[6];
                history_byte[12] = 0;
                history_byte[13] = 0;
                pointer = 7;

                System.arraycopy(history_byte, 2, int_byte, 0, 4);
                dateTime = ByteUtil.bytesToInt(int_byte);
                System.arraycopy(history_byte, 6, short_byte, 0, 2);
                index = ByteUtil.bytesToInt(short_byte);

                if (index != mEventIndexModel) {
                    error = true;
                    break;
                }
            } else {
                byte firstByte = data[pointer];
                pointer += 1;

                int addTime = firstByte & 0x7F;
                if (addTime <= 120) {
                    dateTime += addTime * 10;
                } else {
                    if (pointer + 4 > data.length) {
                        error = true;
                        break;
                    }
                    System.arraycopy(data, pointer, int_byte, 0, 4);
                    dateTime = ByteUtil.bytesToInt(int_byte);
                    pointer += 4;
                }
                System.arraycopy(ByteUtil.intToBytes(dateTime), 0, history_byte, 2, 4);

                index++;
                System.arraycopy(ByteUtil.intToBytes(index), 0, history_byte, 6, 2);

                if ((firstByte & 0x80) != 0) {
                    if (pointer + 1 > data.length) {
                        error = true;
                        break;
                    }
                    history_byte[9] = data[pointer];
                    pointer += 1;
                }

            }
            byte type = history_byte[9];
            if (type == GLUCOSE || type == GLUCOSE_RECOMMEND_CAL || type == HYPO || type == HYPER) {
                if (pointer + 1 > data.length) {
                    error = true;
                    break;
                }
                history_byte[10] = data[pointer];
                pointer += 1;
            } else if (type == BLOOD_GLUCOSE || type == CALIBRATION) {
                if (pointer + 2 > data.length) {
                    error = true;
                    break;
                }
                history_byte[10] = data[pointer];
                history_byte[11] = data[pointer + 1];
                pointer += 2;
            } else {
                history_byte[10] = 0;
            }


//            if (type == 7 || type == 8 || type == 10 || type == 11 || type == 13 || type == 14) {
//                if (pointer + 1 > data.length) {
//                    error = true;
//                    break;
//                }
//                history_byte[10] = data[pointer];
//                pointer += 1;
//            } else {
//                history_byte[10] = 0;
//            }
            byte[] history_byte_add = new byte[14];
            System.arraycopy(history_byte, 0, history_byte_add, 0, 14);
            dataList.pushData(history_byte_add);
        }
//        int pointer = 0;
//        List<History> historyList = new ArrayList<>();
//        History preHistory = new History();
//        for (int i = 0; pointer < message.getData().length; i++) {
//            if (i == 0) {
//                History history = new History();
//                byte[] history_byte = new byte[14];
//                history_byte[0] = 0;
//                history_byte[1] = 0;
//                System.arraycopy(message.getData(), 2, history_byte, 2, 4);
//                System.arraycopy(message.getData(), 0, history_byte, 6, 2);
//                history_byte[8] = ByteUtil.intToByte(sensorIndex);
//                history_byte[9] = message.getData()[6];
//                System.arraycopy(message.getData(), 7, history_byte, 10, 2);
//                history_byte[12] = 0;
//                history_byte[13] = 0;
//                history.setByteArray(history_byte);
//                historyList.add(history);
//                preHistory = history;
//
//                mLog.Error(getClass(), history.getDateTime().getBCD() +
//                        "index:" +
//                        history.getEvent().getIndex() +
//                        "value:" +
//                        history.getStatus().getShortValue1());
//                pointer = 9;
//            } else {
//                int addTime = ByteUtil.byteToInt(message.getData()[pointer]);
//                int addTime1 = addTime & 0x3F;
//                DateTime dateTime = new DateTime();
//                pointer += 1;
//                if (addTime1 <= 60) {
//                    long time_last = preHistory.getDateTime().getCalendar().getTimeInMillis();
//                    long time = time_last + addTime1 * 10 * 1000 - DateTime.BASE_TIME;
//                    dateTime.setByteArray(ByteUtil.intToBytes((int) (time / 1000)));
//                } else {
//                    byte[] dateTimeByte = new byte[4];
//                    System.arraycopy(message.getData(), pointer, dateTimeByte, 0, 4);
//                    dateTime.setByteArray(dateTimeByte);
//                    pointer += 4;
//                }
//                Event event = new Event();
//                event.setIndex(preHistory.getEvent().getIndex() + 1);
//                if ((addTime & 0x80) != 0) {
//                    event.setEvent(ByteUtil.byteToInt(message.getData()[pointer]));
//                    pointer += 1;
//                } else {
//                    event.setEvent(preHistory.getEvent().getEvent());
//                }
//                event.setSensorIndex(sensorIndex);
//
//                int type = event.getEvent();
//                Status status = new Status();
//                if (type == 7 || type == 8 || type == 10 || type == 11 || type == 13 || type == 14) {
//                    if ((addTime & 0x40) != 0) {
//                        byte[] statusByte = new byte[4];
//                        byte[] valuebyte = new byte[2];
//                        System.arraycopy(message.getData(), pointer, valuebyte, 0, 2);
//                        if (type == 10 || type == 11) {
//                            System.arraycopy(valuebyte, 0, statusByte, 2, 2);
//                        } else {
//                            System.arraycopy(valuebyte, 0, statusByte, 0, 2);
//                        }
//                        status.setByteArray(statusByte);
//                        pointer += 2;
//                    } else {
//                        if (type == 10 || type == 11) {
//                            status.setShortValue1(0);
//                            status.setShortValue1Supplement(preHistory.getStatus().getShortValue1() + message.getData()[pointer]);
//                        } else {
//                            status.setShortValue1(preHistory.getStatus().getShortValue1() + message.getData()[pointer]);
//                            status.setShortValue1Supplement(0);
//                        }
//                        pointer += 1;
//                    }
//                } else {
//                    status.setShortValue1(0);
//                    status.setShortValue1Supplement(0);
//                }
//                History h = new History(dateTime, status, event);
//                historyList.add(h);
//                preHistory = h;
//            }
//        }
//        History history = new History();
//        byte[] history_byte = new byte[10];
//        if (message.getData().length >= 9) {
//            System.arraycopy(message.getData(), 2, history_byte, 0, 4);
//            System.arraycopy(message.getData(), 0, history_byte, 4, 2);
//            history_byte[6] = message.getData()[6];
//            history_byte[7] = 0;
//            System.arraycopy(message.getData(), 7, history_byte, 8, 2);
//            history.setByteArray(history_byte);
//            historyList.add(history);
//
//            byte[] historynext_byte = new byte[message.getData().length - 9];
//            System.arraycopy(message.getData(), 9, historynext_byte, 0, message.getData().length - 9);
//            List<byte[]> his = getListIntArray(historynext_byte, 4);
//            for (int i = 0; i < his.size(); i++) {
//                long time_last = historyList.get(i).getDateTime().getCalendar().getTimeInMillis();
//                long time = time_last + ByteUtil.byteToInt(his.get(i)[0]) * 10 * 1000 - DateTime.BASE_TIME;
//                History history1 = new History();
//                history1.setDateTime(new DateTime(ByteUtil.intToBytes((int) (time / 1000))));
//                history1.setEvent(new Event(historyList.get(i).getEvent().getIndex() + 1,
//                        ByteUtil.byteToInt(his.get(i)[1]),
//                        0));
//                byte[] valuebyte = new byte[2];
//                System.arraycopy(his.get(i), 2, valuebyte, 0, 2);
//                history1.setStatus(new Status(valuebyte));
//                historyList.add(history1);
//            }
//        }
//        StringBuilder sb = new StringBuilder();
//        for (History hi : historyList) {
//            sb.append(hi.getDateTime().getBCD())
//                    .append("index:")
//                    .append(hi.getEvent().getIndex())
//                    .append("value:")
//                    .append(hi.getStatus().getShortValue1())
//                    .append("\n");
//        }
//        mLog.Error(getClass(), sb.toString());
//        if (historyList.get(0).getEvent().getIndex() == 1) {
//            time1 = System.currentTimeMillis();
//        }
//        if (historyList.get(historyList.size() - 1).getEvent().getIndex() >= 8200) {
//            stop = false;
//            mLog.Error(getClass(), "同步数据包时间：" + (System.currentTimeMillis() - time1) / 1000);
//        }
//        int modelIndex = (int) SPUtils.get(mService, MEVENT_INDEX_MODEL, 0);
//        mLog.Error(getClass(), "modelIndex：" + modelIndex);
        if ((mEventIndexModel >= 0) && (mEventIndexRemote >= 0)) {

            if (!error) {
                mEventIndexModel = index;
                flag = 0;
            } else {
                if (flag < 3) {
                    flag++;
                    synchronizeHistory(mEventIndexModel);
                    return;
                } else {
                    flag = 0;
                    DataList dataListInvalid = new DataList();
                    History historyInvalid = new History();
                    DateTime dateTimeInvalid = new DateTime(20000000000000L);
                    Event eventInvalid = new Event(mEventIndexModel, 15, 0);
                    historyInvalid.setDateTime(dateTimeInvalid);
                    historyInvalid.setEvent(eventInvalid);
                    dataListInvalid.pushData(historyInvalid.getByteArray());
                    updateHistory(message, dataListInvalid);
                }
            }

            long t2 = System.currentTimeMillis();
//            mLog.Error(getClass(), "解析时间：" + (t2 - t1));

            index = mEventIndexModel;
            mEventIndexModel += 1;
            if (index < mEventIndexRemote) {
                mLog.Error(getClass(), "连续同步" + mEventIndexModel);
                synchronizeHistory(mEventIndexModel);
            } else {
                synchronizeDone = true;
//                messageDone.setData(new byte[]{1});
//                handleMessage(messageDone);
            }
            SPUtils.put(mService, MEVENT_INDEX_MODEL, index);
            updateHistory(message, dataList);
        }
    }
//
//        mLog.Debug(getClass(),
//                "Notify history remote: " + history.getEvent().getIndex());


    private void onNotifyHistoryModel(final EntityMessage message) {
        mLog.Debug(getClass(), "Notify history model begin");
//
//        if (message.getData() != null) {
//            DataList dataList = new DataList(message.getData());
//
//            if (dataList.getCount() > 0) {
//                History history = new History(dataList.getData(dataList.getCount() - 1));
//
//                if (history.getEvent().getIndex() != 0) {
////                    mStatusLast.setDateTime(history.getDateTime());
////                    mStatusLast.setStatus(history.getStatus());
//                    mStatusLast.setByteArray(dataList.getByteArray());
//                    mIsNewStatusPump = true;
//                    SPUtils.put(mService, MEVENT_INDEX_MODEL, history.getEvent().getIndex());
////                    mEventIndexModel = history.getEvent().getIndex();
//
//                }
//            } else {
////                mEventIndexModel = 0;
//
//                SPUtils.put(mService, MEVENT_INDEX_MODEL, 0);
////                mLog.Debug(getClass(),
////                        "Notify history model pump: " + mEventIndexModel);
//            }
//        }
    }


    private void onNotifyHistoryView(final EntityMessage message) {
        mLog.Debug(getClass(), "Notify history view begin");

        mIsHistorySync = false;
        mService.getDataStorage(null).setBoolean(SETTING_HISTORY_SYNC,
                mIsHistorySync);
        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                ParameterGlobal.ADDRESS_LOCAL_MODEL, ParameterGlobal.PORT_COMM,
                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                ParameterComm.PARAM_RF_REMOTE_ADDRESS,
                new RFAddress(RFAddress.RF_ADDRESS_UNPAIR).getByteArray()));

//        mLog.Debug(getClass(), "Notify history view: " + mEventIndexModel);
    }


    private void onNotifyHistoryControl(final EntityMessage message) {
        mLog.Debug(getClass(), "Notify history control begin");


        byte[] broadcastBytes = new byte[12];
        if (message.getData().length >= 12) {
            System.arraycopy(message.getData(), 0, broadcastBytes, 0, 11);
        }
        History history = new History(broadcastBytes);
        mLog.Error(getClass(), "广播包：" + history.getDateTime().getBCD() +
                "type:" + history.getEvent().getEvent() +
                "index: " + history.getEvent().getIndex() +
                "value" + history.getStatus().getShortValue1());
        mLog.Error(getClass(), "广播包：" + Arrays.toString(broadcastBytes));

        if ((history.getDateTime().getYear() == 2000)
                && (history.getEvent().getEvent() == 0)
                && (history.getEvent().getIndex() == 0)
                && (history.getStatus().getShortValue1() == 0)) {
            return;
        }
        history_broadcast = new History(history.getByteArray());

        mEventIndexRemote = history.getEvent().getIndex();

        //接收发射器重启设置为可校准时间
        if (history.getEvent().getEvent() == 0) {
            timeSet = false;
        }
        // 新传感器
        int value = history.getStatus().getShortValue1();
        if (history.getEvent().getEvent() == SENSOR_NEW) {
            if (value == (0xFF - 1)) {
                return;
            }
            if (value == 0xFF) {
                handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                        ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
                        ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_EVENT,
                        ParameterMonitor.PARAM_NEW, history.getByteArray()));
                sendFlag = true;
                return;
            }
        } else {
            if (sendFlag) {
                History newSensorRecovery = new History(new DateTime(Calendar.getInstance()),
                        new Status(0), new Event(0, SENSOR_NEW, 0));
                handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                        ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
                        ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_EVENT,
                        ParameterMonitor.PARAM_NEW, newSensorRecovery.getByteArray()));
                sendFlag = false;
            }
        }
        ////////////////////////////////////////////

        //校准时间
        long systemDateTime = System.currentTimeMillis();
        long historyDateTime = history.getDateTime().getCalendar().getTimeInMillis()
                + history.getBattery().getElapsedtime() * 10 * 1000;
        long dateTimeError = Math.abs(systemDateTime - historyDateTime);

        if (dateTimeError < 60 * 1000) {
            forceSynchronizeFlag = false;
        } else if ((forceSynchronizeFlag ||
                (history.getEvent().getEvent() != SENSOR_EXPIRATION && history.getBattery().getElapsedtime() <= 120))
//                && !timeSet
                ) {
            synchronizeDateTime(history);
            return;
        }
        ////////////////////////////////////////

        //修正倒计时
        if (history.getEvent().getEvent() == SENSOR_NEW) {
            handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                    ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
                    ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_EVENT,
                    ParameterMonitor.PARAM_NEW, history.getByteArray()));
            return;
        }
        /////////////////////////////////////////////

        if (mEventIndexRemote <= 0) {
            return;
        }

        if (!mIsHistorySync) {
            mIsHistorySync = true;
        } else {
            mLog.Error(getClass(), "mEventIndexRemote: " + mEventIndexRemote);
            mLog.Error(getClass(), "BROADCAST_SAVE: " + mBroadcastSave);
            mLog.Error(getClass(), "synchronizeDone: " + synchronizeDone);
            DataList dataList = new DataList();
            dataList.pushData(history.getByteArray());
            if (mBroadcastSave) {
                message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_VIEW);
                message.setOperation(EntityMessage.OPERATION_NOTIFY);
                message.setParameter(ParameterMonitor.PARAM_STATUS);
                message.setData(dataList.getByteArray());
                handleMessage(message);
            }
            if (mEventIndexRemote != (mEventIndexModel - 1)) {
                if (mBroadcastSave || !synchronizeDone) {
                    int sensorIndex_last = history.getEvent().getSensorIndex();
                    if (sensorIndex_last != sensorIndex) {
                        SPUtils.put(mService, SENSORINDEX, sensorIndex_last);
                        sensorIndex = sensorIndex_last;
                        SPUtils.put(mService, MEVENT_INDEX_MODEL, 0);
                        mEventIndexModel = 1;
                        synchronizeHistory(mEventIndexModel);
                        return;
                    }
                    if (history.getEvent().getIndex() > 0) {
                        if (mEventIndexRemote == mEventIndexModel) {
                            if (history.getEvent().getEvent() == 0x1E) {
                                mLog.Error(getClass(), "同步: " + mEventIndexModel);
                                synchronizeHistory(mEventIndexModel);
                            } else {
                                updateHistory(message, dataList);
                                SPUtils.put(mService, MEVENT_INDEX_MODEL, mEventIndexModel);
                                mEventIndexModel++;
                                synchronizeDone = true;
                            }

                        } else if (mEventIndexRemote < mEventIndexModel - 1) {
                            updateHistory(message, dataList);
                            SPUtils.put(mService, MEVENT_INDEX_MODEL, mEventIndexModel);
                            mEventIndexModel = mEventIndexRemote + 1;
                            synchronizeDone = true;
                        } else if (mEventIndexRemote > mEventIndexModel) {
                            mLog.Error(getClass(), "同步: " + mEventIndexModel);
                            synchronizeHistory(mEventIndexModel);
                        }
                    }
                }
            }
        }
        mLog.Debug(getClass(), "Notify history control: " + mEventIndexRemote);
    }


    private void updateHistory(final EntityMessage message,
                               final DataList dataList) {
        History history = new History(dataList.getData(dataList.getCount() - 1));

//        mLog.Error(getClass(), "保存数据库：" +
//                history.getDateTime().getBCD() +
//                "type:" + history.getEvent().getEvent() +
//                "index: " + history.getEvent().getIndex() +
//                "value" + history.getStatus().getShortValue1());

        message.setOperation(EntityMessage.OPERATION_NOTIFY);
        message.setParameter(ParameterMonitor.PARAM_HISTORY);
        message.setData(dataList.getByteArray());
//        message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_MODEL);
//        handleMessage(message);
        message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_VIEW);
        handleMessage(message);
        mService.getDataStorage(null).setBoolean(SETTING_HISTORY_SYNC,
                mIsHistorySync);
        mStatusLast = new DataList(dataList.getByteArray());
        mIsNewStatusPump = true;
    }

    private void synchronizeHistoryManual() {
        mLog.Error(getClass(), "同步完成：" + synchronizeDone);
        int modelIndex = (int) SPUtils.get(mService, MEVENT_INDEX_MODEL, 0);
        if (modelIndex < mEventIndexRemote) {
            if (synchronizeDone) {
                synchronizeDone = false;
                synchronizeHistory(modelIndex + 1);
            }
        }
    }

    private void synchronizeHistory(int index) {
        mLog.Debug(getClass(), "Get history remote: " + index);

        ValueShort value = new ValueShort((short) index);

        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                ParameterGlobal.ADDRESS_REMOTE_MASTER, ParameterGlobal.PORT_MONITOR,
                ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_GET,
                ParameterMonitor.PARAM_HISTORIES, value.getByteArray()));
    }


    private void synchronizeDateTime(History history) {
        final long DATE_TIME_ERROR_MAX = 60 * 1000;
        final int YEAR_MIN = 2017;

        long systemDateTime = System.currentTimeMillis();
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(systemDateTime);

        if (calendar.get(Calendar.YEAR) < YEAR_MIN) {
            return;
        } else {
            if ((calendar.get(Calendar.YEAR) == YEAR_MIN) &&
                    (calendar.get(Calendar.MONTH) <= Calendar.JANUARY) &&
                    (calendar.get(Calendar.DAY_OF_MONTH) <= 1)) {
                return;
            }
        }

        long historyDateTime =
                history.getDateTime().getCalendar().getTimeInMillis()
                        + history.getBattery().getElapsedtime() * 10 * 1000;
        long dateTimeError;

        if ((history.getDateTime().getMonth() == 0) ||
                (history.getDateTime().getDay() == 0)) {
            dateTimeError = DATE_TIME_ERROR_MAX;
        } else {
            if (systemDateTime > historyDateTime) {
                dateTimeError = systemDateTime - historyDateTime;
            } else {

                dateTimeError = historyDateTime - systemDateTime;
            }
        }

        if (dateTimeError >= DATE_TIME_ERROR_MAX) {
            mLog.Debug(getClass(), "Set datetime remote: " + dateTimeError);
            mLog.Error(getClass(), "校准时间");
            final DateTime dateTime = new DateTime(calendar);
            long nowTime = systemDateTime - DateTime.BASE_TIME;
            handleMessage(
                    new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                            ParameterGlobal.ADDRESS_REMOTE_MASTER,
                            ParameterGlobal.PORT_MONITOR, ParameterGlobal.PORT_MONITOR,
                            EntityMessage.OPERATION_SET,
                            ParameterMonitor.PARAM_DATETIME, ByteUtil.intToBytes((int) (nowTime / 1000))));
            history.setDateTime(dateTime);
        } else {
            forceSynchronizeFlag = false;
        }
    }
}
