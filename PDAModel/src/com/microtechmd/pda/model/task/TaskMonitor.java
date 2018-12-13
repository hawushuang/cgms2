package com.microtechmd.pda.model.task;


import android.util.Log;

import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.comm.RFAddress;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.service.ServiceBase;
import com.microtechmd.pda.library.service.TaskBase;
import com.microtechmd.pda.library.utility.ByteUtil;
import com.microtechmd.pda.model.database.DataSetHistory;
import com.microtechmd.pda.model.database.DbHistory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public final class TaskMonitor extends TaskBase {
    // Constant and variable definition

    private static TaskMonitor sInstance = null;

    private DataSetHistory mDataSetHistory = null;


    // Method definition

    private TaskMonitor(ServiceBase service) {
        super(service);

        if (mDataSetHistory == null) {
            mDataSetHistory = new DataSetHistory(service);

            byte[] addressSetting =
                    mService.getDataStorage(null)
                            .getExtras(TaskComm.SETTING_RF_ADDRESS, null);

            if (addressSetting != null) {
                mDataSetHistory.setRFAddress(getAddress(addressSetting));
            }
        }
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
                .getTargetAddress() == ParameterGlobal.ADDRESS_LOCAL_MODEL) &&
                (message.getTargetPort() == ParameterGlobal.PORT_MONITOR)) {
            switch (message.getOperation()) {
                case EntityMessage.OPERATION_SET:
                    setParameter(message);
                    break;

                case EntityMessage.OPERATION_GET:
                    getParameter(message);
                    break;

                case EntityMessage.OPERATION_EVENT:
                    break;

                case EntityMessage.OPERATION_NOTIFY:
                    handleNotification(message);
                    break;

                case EntityMessage.OPERATION_ACKNOWLEDGE:
                    break;

                default:
                    break;
            }
        } else {
            mService.onReceive(message);
        }
    }


    private void setParameter(EntityMessage message) {
        int acknowledge = EntityMessage.FUNCTION_OK;


        if (message.getData() == null) {
            return;
        }

        switch (message.getParameter()) {
            case ParameterMonitor.PARAM_HISTORY:
                DataList historyList = new DataList(message.getData());
//                mDataSetHistory.exportHistory(historyList);
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


    private void getParameter(EntityMessage message) {
        int acknowledge = EntityMessage.FUNCTION_OK;
        byte[] value = null;

        switch (message.getParameter()) {
            case ParameterMonitor.PARAM_HISTORY:
                DataList historyList = new DataList(message.getData());
                historyList = mDataSetHistory.queryHistory(historyList);
                value = historyList.getByteArray();
                break;
            case ParameterMonitor.PARAM_HISTORY_LAST:
                List<DbHistory> lastList = mDataSetHistory.querHistoryLast();
                if (lastList.size() > 0) {
                    value = lastList.get(0).getRf_address().getBytes();
                }
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


    private void handleNotification(EntityMessage message) {
        mLog.Debug(getClass(), "保存数据到库");
//        Log.e("数据广播包：", Arrays.toString(message.getData()));
        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
                (message.getParameter() == ParameterMonitor.PARAM_HISTORY)) {
            if (message.getData() != null) {
                final List<History> historyList = new ArrayList<>();
                DataList dataList = new DataList(message.getData());
                for (int i = 0; i < dataList.getCount(); i++) {
                    historyList.add(new History(dataList.getData(i)));
                }

//                mDataSetHistory.insertHistory(new History(message.getData()));
                History history = historyList.get(historyList.size() - 1);
                Runnable command = new Runnable() {
                    @Override
                    public void run() {
                        mDataSetHistory.insertAllHistory(historyList);
                    }
                };
                //定长线程池，可控制线程最大并发数，超出的线程会在队列中等待
                ExecutorService fixedThreadPool = Executors.newFixedThreadPool(1);
                fixedThreadPool.execute(command);
            }
        }

        if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
                (message.getParameter() == ParameterComm.PARAM_RF_REMOTE_ADDRESS)) {
            if (message.getData() != null) {
                mDataSetHistory.setRFAddress(getAddress(message.getData()));
            }
        }
        if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
                (message.getParameter() == ParameterComm.CLEAN_DATABASES)) {
            mDataSetHistory.cleanDatabases();
        }
    }


    private void reverseMessagePath(EntityMessage message) {
        message.setTargetAddress(message.getSourceAddress());
        message.setSourceAddress(ParameterGlobal.ADDRESS_LOCAL_MODEL);
        message.setTargetPort(message.getSourcePort());
        message.setSourcePort(ParameterGlobal.PORT_MONITOR);
    }

    private String getAddress(byte[] addressByte) {
        if (addressByte != null) {
            for (int i = 0; i < addressByte.length; i++) {
                if (addressByte[i] < 10) {
                    addressByte[i] += '0';
                } else {
                    addressByte[i] -= 10;
                    addressByte[i] += 'A';
                }
            }

            return new String(addressByte);
        } else {
            return "";
        }
    }
}
