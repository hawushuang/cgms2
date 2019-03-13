package com.microtechmd.pda.database;


import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.os.CountDownTimer;
import android.os.Message;

import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.comm.RFAddress;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.utility.LogPDA;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class DataSetHistory {
    private DatabaseManager databaseManager;
    private String mRFAddress = RFAddress.RF_ADDRESS_UNPAIR;
    LogPDA mLog = null;
    private List<DbHistory> dbCash = new ArrayList<>();
    private CountDownTimer saveCountdownTimer;
    private Timer signalTimer;

    public DataSetHistory(Context context) {
        if (databaseManager == null) {
            databaseManager = DatabaseManager.getInstance(context);
            mLog = new LogPDA();
        }
    }

    public void close() {
        databaseManager.close();
    }

    public void setRFAddress(final String address) {
        mRFAddress = address;
    }

    public void cleanDatabases() {
        databaseManager.delete(DbHistory.class);
    }


    public void insertAllHistory(List<DbHistory> dbHistories) {
        try {
            if (dbHistories.size() > 0) {

                DbHistory aa = dbHistories.get(dbHistories.size() - 1);
                mLog.Error(getClass(), "保存数据库：" +
                        aa.getDate_time() +
                        "type:" + aa.getEvent_type() +
                        "index: " + aa.getEvent_index() +
                        "value" + aa.getValue());
                databaseManager.insertAll(dbHistories);
//                dbCash.addAll(dbHistories);
//
//                if (dbCash.size() < 3 || dbCash.size() > 500) {
//                    databaseManager.insertAll(dbCash);
//                    dbCash.clear();
//                }
//
//                if (signalTimer != null) {
//                    signalTimer.cancel();
//                    signalTimer = null;
//                }
//                signalTimer = new Timer();
//                signalTimer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        if (dbCash.size() > 0) {
//                            databaseManager.insertAll(dbCash);
//                            dbCash.clear();
//                        }
//                    }
//                }, 0, 3000);


            }

        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }
//    public void insertAllHistory(List<History> historyList) {
//        if (historyList == null || historyList.size() <= 0) {
//            return;
//        }
//        List<DbHistory> dbHistories = new ArrayList<>();
//        for (History history : historyList) {
//            Status status = history.getStatus();
//            Event event = history.getEvent();
//            DbHistory dbHistory = new DbHistory();
//            dbHistory.setRf_address(mRFAddress);
//            dbHistory.setDate_time(history.getDateTime().getCalendar().getTimeInMillis());
//            dbHistory.setEvent_index(event.getIndex());
//            dbHistory.setEvent_type(event.getEvent());
//            dbHistory.setValue(status.getShortValue1());
//            dbHistory.setSupplement_value(status.getShortValue1Supplement());
//            dbHistory.setSensorIndex(event.getSensorIndex());
//            dbHistories.add(dbHistory);
//        }
//        try {
//            DbHistory aa = dbHistories.get(dbHistories.size() - 1);
//            mLog.Error(getClass(), "保存数据库：" +
//                    aa.getDate_time() +
//                    "type:" + aa.getEvent_type() +
//                    "index: " + aa.getEvent_index() +
//                    "value" + aa.getValue());
//            databaseManager.insertAll(dbHistories);
//        } catch (SQLiteException e) {
//            e.printStackTrace();
//        }
//    }

//    public void insertTest() {
//        List<DbHistory> dbHistories = new ArrayList<>();
//        for (int i = 0; i < 200; i++) {
//            DbHistory dbHistory = new DbHistory();
//            dbHistory.setRf_address("AAAAA1");
//            dbHistory.setDate_time(System.currentTimeMillis());
//            dbHistory.setEvent_index(i + 1);
//            dbHistory.setEvent_type(7);
//            dbHistories.add(dbHistory);
//        }
//        try {
//            databaseManager.insertAll(dbHistories);
//        } catch (SQLiteException e) {
//            e.printStackTrace();
//        }
//    }

//    public void insertHistory(History history) {
//        if (history == null) {
//            return;
//        }
//        Status status = history.getStatus();
//        Event event = history.getEvent();
//        DbHistory dbHistory = new DbHistory();
//        dbHistory.setRf_address(mRFAddress);
//        dbHistory.setDate_time(history.getDateTime().getBCD());
//        dbHistory.setEvent_index(event.getIndex());
//        dbHistory.setEvent_type(event.getEvent());
//        dbHistory.setValue(status.getShortValue1());
//        dbHistory.setSupplement_value(status.getShortValue1Supplement());
//        dbHistory.setSensorIndex(event.getSensorIndex());
//        try {
//            databaseManager.insert(dbHistory);
//        } catch (SQLiteException e) {
//            e.printStackTrace();
//        }
//    }

    public List<DbHistory> querHistoryLast() {
        return databaseManager.getQueryLast(DbHistory.class);
    }

    public List<DbHistory> querAddressHistoryLast(String adress) {
        return databaseManager.getAddressQueryLast(DbHistory.class, adress);
    }

    public List<DbHistory> getDbList() {
        return databaseManager.getQueryAllByColumns(DbHistory.class);
    }
    public List<DbHistory> getErrDbList() {
        return databaseManager.getQueryErrByColumns(DbHistory.class);
    }

    public DataList queryHistory(final DataList filter) {
        DataList dataList = new DataList();

        if ((filter != null) && (filter.getCount() > 0)) {
            List<DbHistory> historyList;
            if (filter.getCount() == 1) {
                History history = new History(filter.getData(0));
                historyList = databaseManager.getQueryByWhere(DbHistory.class,
                        "date_time",
                        String.valueOf(history.getDateTime().getBCD()));
            } else {
                History history1 = new History(filter.getData(0));
                History history2 = new History(filter.getData(1));
                historyList = databaseManager.getQueryByDate(DbHistory.class,
                        history1.getDateTime().getBCD(),
                        history2.getDateTime().getBCD());
                mLog.Error(getClass(), "query1: " + history1.getDateTime().getBCD()
                        + "query2: " + history2.getDateTime().getBCD());
            }
            for (DbHistory dbh : historyList) {
                dataList.pushData(getHistory(dbh).getByteArray());
            }
            mLog.Error(getClass(), "query: " + historyList.size());
        } else {
            long t1 = System.currentTimeMillis();
            List<DbHistory> historyListtest = databaseManager.getQueryAllByColumns(DbHistory.class);
            long t2 = System.currentTimeMillis();
            mLog.Error(getClass(), "数量" + historyListtest.size() + "时间：" + (t2 - t1));
            long start = System.currentTimeMillis();
            for (DbHistory dbh : historyListtest) {
                dataList.pushData(getHistory(dbh).getByteArray());
            }
            long end = System.currentTimeMillis();
            mLog.Error(getClass(), "数据库转换时间：" + (end - start));
        }
//        String sb = "时间：" + history.getDateTime().getBCD() + "index : " + event.getIndex()
//                + "envent :" + event.getEvent() + "value :" + status.getShortValue1();

        return dataList;
    }

    private History getHistory(DbHistory dbhistory) {
        History history = new History();
        if (dbhistory == null) {
            history = new History();
        } else {
            DateTime dateTime = new DateTime();
            Status status = new Status();
            Event event = new Event();

            dateTime.setBCD(dbhistory.getDate_time());
            status.setShortValue1(dbhistory.getValue());
            status.setShortValue1Supplement(dbhistory.getSupplement_value());
            event.setIndex(dbhistory.getEvent_index());
            event.setEvent(dbhistory.getEvent_type());
            event.setSensorIndex(dbhistory.getSensorIndex());
            history.setDateTime(dateTime);
            history.setStatus(status);
            history.setEvent(event);
        }
        return history;
    }
}
