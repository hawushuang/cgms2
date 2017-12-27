package com.microtechmd.pda.control.task;


import android.util.Log;

import java.util.Arrays;
import java.util.Calendar;

import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.ValueShort;
import com.microtechmd.pda.library.entity.comm.RFAddress;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.entity.monitor.StatusPump;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.service.ServiceBase;
import com.microtechmd.pda.library.service.TaskBase;


public final class TaskMonitor extends TaskBase {
    // Constant and variable definition

    private static final String SETTING_STATUS_LAST = "status_last";
    private static final String SETTING_BOLUS_LAST = "bolus_last";
    private static final String SETTING_HISTORY_SYNC = "history_sync";

    private static final int EVENT_INDEX_MAX = 10000;

    private static TaskMonitor sInstance = null;

    private StatusPump mStatusLast = null;
    private StatusPump mBolusLast = null;
    private boolean mIsNewStatusPump = false;
    private boolean mIsHistorySync = false;
    private int mEventIndexModel = -1;
    private int mEventIndexRemote = -1;


    // Method definition

    private TaskMonitor(ServiceBase service) {
        super(service);

        mStatusLast = new StatusPump(
                mService.getDataStorage(null).getExtras(SETTING_STATUS_LAST, null));
        mStatusLast.setBatteryCapacity(0);
        mStatusLast.setReservoirAmount(1);
        mStatusLast.setBasalRate(0);
        mBolusLast = new StatusPump(
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
            history.setStatus(new Status(-1, -1, -1, -1));
            history.setEvent(
                    new Event(0, ParameterGlobal.PORT_GLUCOSE, -1, -1, -1));
            datalist.pushData(history.getByteArray());
            history
                    .setEvent(new Event(0, ParameterGlobal.COUNT_PORT, -1, -1, -1));
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
            default:
                break;
        }
    }


    private void getParameter(final EntityMessage message) {
        int acknowledge = EntityMessage.FUNCTION_OK;
        byte[] value = null;

        switch (message.getParameter()) {
            case ParameterMonitor.PARAM_STATUS:
                mLog.Debug(getClass(), "Get pump status");

                StatusPump statusPump =
                        new StatusPump(mStatusLast.getByteArray());
                statusPump.setBolusRate(mBolusLast.getBolusRate());
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
            if (message.getParameter() == ParameterMonitor.PARAM_HISTORY) {
                onNotifyHistory(message);
            }
        }

        if (mIsNewStatusPump) {
            mLog.Debug(getClass(), "Update status pump");

            mIsNewStatusPump = false;
            StatusPump statusPump = new StatusPump(mStatusLast.getByteArray());
            message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_VIEW);
            message.setOperation(EntityMessage.OPERATION_NOTIFY);
            message.setParameter(ParameterMonitor.PARAM_STATUS);
            message.setData(statusPump.getByteArray());
            handleMessage(message);
        }
    }


    private void handleAcknowledgement(final EntityMessage message) {
        mLog.Debug(getClass(),
                "Acknowledge port comm: " + message.getData()[0]);
    }


    private void reverseMessagePath(EntityMessage message) {
        message.setTargetAddress(message.getSourceAddress());
        message.setSourceAddress(ParameterGlobal.ADDRESS_LOCAL_CONTROL);
        message.setTargetPort(message.getSourcePort());
        message.setSourcePort(ParameterGlobal.PORT_MONITOR);
    }


    private void onNotifyHistory(final EntityMessage message) {
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
    private int flag;

    private void onNotifyHistoryRemote(final EntityMessage message) {
        mLog.Debug(getClass(), "Notify history remote begin");
        History history = new History(message.getData());
        if ((mEventIndexModel >= 0) && (mEventIndexRemote >= 0)) {
            int eventIndexModel = mEventIndexModel + 1;

            if (eventIndexModel > EVENT_INDEX_MAX) {
                eventIndexModel = Event.EVENT_INDEX_MIN;
            }

            if (eventIndexModel == history.getEvent().getIndex()) {
                updateHistory(message, history);
                mEventIndexModel = eventIndexModel;
                flag = 0;
            } else {
                flag++;
            }

            if (flag > 3){
                flag = 0;
                mEventIndexModel = eventIndexModel;
            }
            if (mEventIndexModel != mEventIndexRemote) {
                eventIndexModel = mEventIndexModel + 1;

                if (eventIndexModel > EVENT_INDEX_MAX) {
                    eventIndexModel = Event.EVENT_INDEX_MIN;
                }

                synchronizeHistory(eventIndexModel);
                mLog.Debug(getClass(),
                        "Notify history *****Model: " + eventIndexModel);
                mLog.Debug(getClass(),
                        "Notify history *****Remote: " + mEventIndexRemote);
            }
        }

        mLog.Debug(getClass(),
                "Notify history remote: " + history.getEvent().getIndex());
    }


    private void onNotifyHistoryModel(final EntityMessage message) {
        mLog.Debug(getClass(), "Notify history model begin");

        if (message.getData() != null) {
            DataList dataList = new DataList(message.getData());

            if (dataList.getCount() > 0) {
                History history = new History(dataList.getData(0));

                if (history.getEvent().getIndex() != 0) {
                    mStatusLast.setDateTime(history.getDateTime());
                    mStatusLast.setStatus(history.getStatus());
                    mIsNewStatusPump = true;
                    mEventIndexModel = history.getEvent().getIndex();

                    mLog.Debug(getClass(),
                            "Notify history model pump: " + mEventIndexModel);
                }
            } else {
                mEventIndexModel = 0;

                mLog.Debug(getClass(),
                        "Notify history model pump: " + mEventIndexModel);
            }
        }
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

        mLog.Debug(getClass(), "Notify history view: " + mEventIndexModel);
    }


    private void onNotifyHistoryControl(final EntityMessage message) {
        mLog.Debug(getClass(), "Notify history control begin");


        History history = new History(message.getData());

        int eventIndexRemote = history.getEvent().getIndex();

        Log.e("历史广播包：", Arrays.toString(message.getData())+"index: "+eventIndexRemote);
        if (eventIndexRemote < 0) {
            eventIndexRemote += Event.OBSOLETE_EVENT_MASK;
        }

        if (eventIndexRemote == 0) {
            mLog.Debug(getClass(), "Notify history control invalid");
            return;
        }

        mEventIndexRemote = eventIndexRemote;

        synchronizeDateTime(history);

        if (!mIsHistorySync) {
            mIsHistorySync = true;
            mEventIndexModel = eventIndexRemote;
        } else {
            if (mEventIndexModel >= 0) {
                if (eventIndexRemote != mEventIndexModel) {
                    int eventIndexModel = mEventIndexModel + 1;

                    if (eventIndexModel > EVENT_INDEX_MAX) {
                        eventIndexModel = Event.EVENT_INDEX_MIN;
                    }

//                    if ((eventIndexRemote >= eventIndexModel) || eventIndexRemote == 1 &&
//                            (history.getEvent().getIndex() > 0)) {
//                        synchronizeHistory(eventIndexModel);
//                    }


                    if ((eventIndexRemote == eventIndexModel) &&
                            (history.getEvent().getIndex() > 0)) {
                        updateHistory(message, history);
                        mEventIndexModel = eventIndexModel;
                    } else {
                        synchronizeHistory(eventIndexModel);
                    }
                }
            }
        }
        mLog.Debug(getClass(), "Notify history control: " + eventIndexRemote);
    }


    private void updateHistory(final EntityMessage message,
                               final History history) {
        mLog.Debug(getClass(),
                "Update history: " + history.getEvent().getIndex());

        message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_MODEL);
        message.setOperation(EntityMessage.OPERATION_NOTIFY);
        message.setParameter(ParameterMonitor.PARAM_HISTORY);
        message.setData(history.getByteArray());
        handleMessage(message);
        message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_VIEW);
        handleMessage(message);
        mService.getDataStorage(null).setBoolean(SETTING_HISTORY_SYNC,
                mIsHistorySync);

        mStatusLast.setDateTime(history.getDateTime());
        mStatusLast.setStatus(history.getStatus());
        mIsNewStatusPump = true;
    }


    private void synchronizeHistory(int index) {
        mLog.Debug(getClass(), "Get history remote: " + index);

        ValueShort value = new ValueShort((short) index);

        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                ParameterGlobal.ADDRESS_REMOTE_MASTER, ParameterGlobal.PORT_MONITOR,
                ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_GET,
                ParameterMonitor.PARAM_HISTORY, value.getByteArray()));
    }


    private void synchronizeDateTime(History history) {
        final long DATE_TIME_ERROR_MAX = 20000;
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
                history.getDateTime().getCalendar().getTimeInMillis();
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

            final DateTime dateTime = new DateTime(calendar);
            handleMessage(
                    new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                            ParameterGlobal.ADDRESS_REMOTE_MASTER,
                            ParameterGlobal.PORT_MONITOR, ParameterGlobal.PORT_MONITOR,
                            EntityMessage.OPERATION_SET,
                            ParameterMonitor.PARAM_DATETIME, dateTime.getByteArray()));
            history.setDateTime(dateTime);
        }
    }
}
