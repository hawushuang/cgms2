package com.microtechmd.pda.control.task;


import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.glucose.StatusGlucose;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.service.ServiceBase;
import com.microtechmd.pda.library.service.TaskBase;


public class TaskGlucose extends TaskBase {
    private static TaskGlucose sInstance = null;
    private static StatusGlucose sStatusGlucose = null;


    private TaskGlucose(ServiceBase service) {
        super(service);

        EntityMessage message = new EntityMessage(
                ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                ParameterGlobal.ADDRESS_LOCAL_MODEL, ParameterGlobal.PORT_GLUCOSE,
                ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_GET,
                ParameterMonitor.PARAM_HISTORY, null);
        DataList dataList = new DataList();
        History history = new History();
        history.setDateTime(new DateTime(-1, -1, -1, -1, -1, -1));
        history.setStatus(new Status(-1));
        history.setEvent(new Event(0, -1, -1));
        dataList.pushData(history.getByteArray());
        message.setData(dataList.getByteArray());
        mService.onReceive(message);
    }


    public static synchronized TaskGlucose getInstance(
            final ServiceBase service) {
        if (sInstance == null) {
            sInstance = new TaskGlucose(service);
        }

        return sInstance;
    }


    @Override
    public void handleMessage(EntityMessage message) {
        if ((message
                .getTargetAddress() == ParameterGlobal.ADDRESS_LOCAL_CONTROL) &&
                (message.getTargetPort() == ParameterGlobal.PORT_GLUCOSE)) {
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
        if ((message
                .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_VIEW) &&
                (message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE)) {
            mLog.Debug(getClass(), "Set parameter: " + message.getParameter());

            message.setTargetAddress(ParameterGlobal.ADDRESS_REMOTE_SLAVE);
            handleMessage(message);
        }
    }


    private void getParameter(final EntityMessage message) {
        int acknowledge = EntityMessage.FUNCTION_OK;
        byte[] value = null;


        switch (message.getParameter()) {
            case ParameterGlucose.PARAM_STATUS:
                mLog.Debug(getClass(), "Get status");

                if (sStatusGlucose == null) {
                    sStatusGlucose = new StatusGlucose();
                }

                value = sStatusGlucose.getByteArray();
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
        if ((message
                .getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) &&
                (message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE)) {
            mLog.Debug(getClass(),
                    "Event parameter: " + message.getParameter());

            message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_VIEW);
            handleMessage(message);
        }
    }


    private void handleNotification(final EntityMessage message) {
        mLog.Debug(getClass(), "Notify parameter: " + message.getParameter());

        if ((message
                .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_MODEL) &&
                (message.getSourcePort() == ParameterGlobal.PORT_MONITOR)) {
            if ((message.getParameter() == ParameterMonitor.PARAM_HISTORY) &&
                    (message.getData() != null)) {
                DataList dataList = new DataList(message.getData());

                if (dataList.getCount() > 0) {
                    History history = new History(dataList.getData(0));
                    Event event = history.getEvent();

                    if ((event.getIndex() == 0) &&
                            (event.getEvent() == ParameterGlucose.EVENT_GLUCOSE)) {
                        if (sStatusGlucose == null) {
                            sStatusGlucose = new StatusGlucose();
                        }

                        sStatusGlucose.setDateTime(history.getDateTime());
                        sStatusGlucose.setStatus(history.getStatus());
                        message.setSourceAddress(
                                ParameterGlobal.ADDRESS_LOCAL_CONTROL);
                        message.setSourcePort(ParameterGlobal.PORT_GLUCOSE);
                        updateStatusGlucose(message, sStatusGlucose);
                    }
                }
            }
        }

        if ((message
                .getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) &&
                (message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE)) {
            message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_VIEW);
            handleMessage(message);
        }
    }


    private void handleAcknowledgement(final EntityMessage message) {
        if ((message
                .getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) &&
                (message.getSourcePort() == ParameterGlobal.PORT_GLUCOSE)) {
            mLog.Debug(getClass(),
                    "Acknowledge parameter: " + message.getParameter());

            message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_VIEW);
            handleMessage(message);
        }
    }


    private void reverseMessagePath(EntityMessage message) {
        message.setTargetAddress(message.getSourceAddress());
        message.setSourceAddress(ParameterGlobal.ADDRESS_LOCAL_CONTROL);
        message.setTargetPort(message.getSourcePort());
        message.setSourcePort(ParameterGlobal.PORT_GLUCOSE);
    }


    private void updateStatusGlucose(final EntityMessage message,
                                     final StatusGlucose status) {
        mLog.Debug(getClass(), "Update status glucose");

        message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_VIEW);
        message.setTargetPort(ParameterGlobal.PORT_MONITOR);
        message.setParameter(ParameterGlucose.PARAM_STATUS);
        message.setData(status.getByteArray());
        handleMessage(message);
    }
}