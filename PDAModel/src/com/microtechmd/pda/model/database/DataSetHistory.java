package com.microtechmd.pda.model.database;


import android.content.Context;
import android.database.sqlite.SQLiteException;

import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.comm.RFAddress;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.utility.LogPDA;

import java.util.ArrayList;
import java.util.List;


public class DataSetHistory {
    private DatabaseManager databaseManager;
    private String mRFAddress = RFAddress.RF_ADDRESS_UNPAIR;
    LogPDA mLog = null;


    public DataSetHistory(Context context) {
        if (databaseManager == null) {
            databaseManager = DatabaseManager.getInstance(context);
            mLog = new LogPDA();
        }
    }


    public void setRFAddress(final String address) {
        mRFAddress = address;
    }

    public void cleanDatabases() {
        databaseManager.delete(DbHistory.class);
    }


    public void insertAllHistory(List<History> historyList) {
        if (historyList == null || historyList.size() <= 0) {
            return;
        }
        List<DbHistory> dbHistories = new ArrayList<>();
        for (History history : historyList) {
            Status status = history.getStatus();
            Event event = history.getEvent();
            DbHistory dbHistory = new DbHistory();
            dbHistory.setRf_address(mRFAddress);
            dbHistory.setDate_time(history.getDateTime().getBCD());
            dbHistory.setEvent_index(event.getIndex());
            dbHistory.setEvent_type(event.getEvent());
            dbHistory.setValue(status.getShortValue1());
            dbHistory.setSupplement_value(status.getShortValue1Supplement());
            dbHistory.setSensorIndex(event.getSensorIndex());
            dbHistories.add(dbHistory);
//            String sb = "时间：" + history.getDateTime().getBCD() + "index : " + event.getIndex()
//                    + "envent :" + event.getEvent() + "value :" + status.getShortValue1();
//            Log.e("aa", "insertHistory: " + sb);
        }
//        Log.e("aa", "insertHistory: " + "-------------------------------------");
        try {
            DbHistory aa = dbHistories.get(dbHistories.size() - 1);
//            mLog.Error(getClass(), "保存数据库：" +
//                    aa.getDate_time() +
//                    "type:" + aa.getEvent_type() +
//                    "index: " + aa.getEvent_index() +
//                    "value" + aa.getValue());
            databaseManager.insertAll(dbHistories);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    public void insertHistory(History history) {
        if (history == null) {
            return;
        }
        Status status = history.getStatus();
        Event event = history.getEvent();
        DbHistory dbHistory = new DbHistory();
        dbHistory.setRf_address(mRFAddress);
        dbHistory.setDate_time(history.getDateTime().getBCD());
        dbHistory.setEvent_index(event.getIndex());
        dbHistory.setEvent_type(event.getEvent());
        dbHistory.setValue(status.getShortValue1());
        dbHistory.setSupplement_value(status.getShortValue1Supplement());
        dbHistory.setSensorIndex(event.getSensorIndex());
        try {
            databaseManager.insert(dbHistory);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    public List<DbHistory> querHistoryLast() {
        return databaseManager.getQueryLast(DbHistory.class);
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

//    public void exportHistory(final DataList filter) {
//        final String FILE_PATH = Environment.getExternalStorageDirectory() +
//                "/" + History.class.getSimpleName() + ".csv";
//        final String SPLIT_TAG = ",";
//
//
//        if (Environment.getExternalStorageState()
//                .equals(Environment.MEDIA_MOUNTED)) {
//            try {
//                BufferedWriter bufferdWriter =
//                        new BufferedWriter(new FileWriter(FILE_PATH));
//                bufferdWriter
//                        .write(FIELD_RF_ADDRESS + SPLIT_TAG + FIELD_DATE_TIME +
//                                SPLIT_TAG + FIELD_STATUS_SHORT0 +
//                                SPLIT_TAG + FIELD_EVENT_INDEX +
//                                SPLIT_TAG + FIELD_EVENT_VALUE);
//                bufferdWriter.newLine();
//
//                try {
//                    mIsOneLimit = false;
//                    String queryString = buildQuery(buildFilter(filter));
//
//                    if (mIsOneLimit) {
//                        queryString += " LIMIT 1";
//                    }
//
//                    SQLiteDatabase database =
//                            mDatabaseHelper.getReadableDatabase();
//                    Cursor cursor = database.rawQuery(queryString, null);
//
//                    if (cursor.moveToLast()) {
//                        do {
//                            bufferdWriter.write(cursor.getString(
//                                    cursor.getColumnIndex(FIELD_RF_ADDRESS)) +
//                                    SPLIT_TAG +
//                                    cursor.getLong(cursor.getColumnIndex(FIELD_DATE_TIME)) +
//                                    SPLIT_TAG +
//                                    cursor.getInt(cursor.getColumnIndex(FIELD_STATUS_SHORT0)) +
//                                    SPLIT_TAG +
//                                    cursor.getInt(cursor.getColumnIndex(FIELD_EVENT_INDEX)) +
//                                    SPLIT_TAG +
//                                    cursor.getInt(cursor.getColumnIndex(FIELD_EVENT_VALUE)));
//                            bufferdWriter.newLine();
//                            bufferdWriter.flush();
//                        }
//                        while (cursor.moveToPrevious());
//                    }
//
//                    cursor.close();
//                    database.close();
//                } catch (SQLiteException e) {
//                    e.printStackTrace();
//                }
//
//                bufferdWriter.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//
//    private String buildQuery(String filterString) {
//        String queryString = "SELECT * FROM " + TABLE_NAME;
//        queryString += filterString;
//        queryString +=
//                " ORDER BY " + FIELD_DATE_TIME + " DESC, " + FIELD_ID + " DESC";
//
//        return queryString;
//    }
//
//
//    private String buildFilter(final DataList filter) {
//        String filterString = "";
//
//        if ((filter != null) && (filter.getCount() > 0)) {
//            if (filter.getCount() == 1) {
//                History history = new History(filter.getData(0));
//                filterString += buildFilterDateTime(history.getDateTime(), "=");
//                filterString += buildFilterStatus(history.getStatus(), "=");
//                filterString += buildFilterEvent(history.getEvent(), "=");
//            } else {
//                History history = new History(filter.getData(0));
//                filterString +=
//                        buildFilterDateTime(history.getDateTime(), ">=");
//                filterString += buildFilterStatus(history.getStatus(), ">=");
//                filterString += buildFilterEvent(history.getEvent(), ">=");
//                history.setByteArray(filter.getData(1));
//                filterString += buildFilterDateTime(history.getDateTime(), "<");
//                filterString += buildFilterStatus(history.getStatus(), "<");
//                filterString += buildFilterEvent(history.getEvent(), "<");
//            }
//
//            if (!filterString.equals("")) {
//                filterString = filterString.replaceFirst("AND", "WHERE");
//            }
//        }
//
//        return filterString;
//    }
//
//
//    private String buildFilterDateTime(final DateTime dateTime,
//                                       final String operator) {
//        String filterString = "";
//        DecimalFormat decimalFormat = new DecimalFormat("#");
//
//        if (operator.equals("=")) {
//            decimalFormat.setMinimumIntegerDigits(2);
//
//            if (dateTime.getYear() != -1) {
//                filterString += decimalFormat
//                        .format(DateTime.YEAR_BASE + dateTime.getYear());
//            } else {
//                filterString += "____";
//            }
//
//            if (dateTime.getMonth() != -1) {
//                filterString += decimalFormat.format(dateTime.getMonth());
//            } else {
//                filterString += "__";
//            }
//
//            if (dateTime.getDay() != -1) {
//                filterString += decimalFormat.format(dateTime.getDay());
//            } else {
//                filterString += "__";
//            }
//
//            if (dateTime.getHour() != -1) {
//                filterString += decimalFormat.format(dateTime.getHour());
//            } else {
//                filterString += "__";
//            }
//
//            if (dateTime.getMinute() != -1) {
//                filterString += decimalFormat.format(dateTime.getMinute());
//            } else {
//                filterString += "__";
//            }
//
//            if (dateTime.getSecond() != -1) {
//                filterString += decimalFormat.format(dateTime.getSecond());
//            } else {
//                filterString += "__";
//            }
//
//            if (filterString.equals("______________")) {
//                filterString = "";
//            } else {
//                filterString =
//                        " AND " + FIELD_DATE_TIME + " LIKE '" + filterString + "'";
//            }
//        } else if ((operator.equals(">=")) || (operator.equals("<"))) {
//            decimalFormat.setMinimumIntegerDigits(14);
//            long filterValue = 0;
//
//            if (dateTime.getYear() > 0) {
//                filterValue += (long) (DateTime.YEAR_BASE + dateTime.getYear()) *
//                        10000000000l;
//            }
//
//            if (dateTime.getMonth() > 0) {
//                filterValue += (long) dateTime.getMonth() * 100000000l;
//            }
//
//            if (dateTime.getDay() > 0) {
//                filterValue += (long) dateTime.getDay() * 1000000l;
//            }
//
//            if (dateTime.getHour() > 0) {
//                filterValue += (long) dateTime.getHour() * 10000l;
//            }
//
//            if (dateTime.getMinute() > 0) {
//                filterValue += (long) dateTime.getMinute() * 100l;
//            }
//
//            if (dateTime.getSecond() > 0) {
//                filterValue += (long) dateTime.getSecond();
//            }
//
//            if (filterValue > 0) {
//                filterString = " AND " + FIELD_DATE_TIME + operator + "'" +
//                        decimalFormat.format(filterValue) + "'";
//            }
//        }
//
//        return filterString;
//    }
//
//
//    private String buildFilterStatus(final Status status, final String operator) {
//        String filterString = "";
//
//        if (status.getShortValue1() != -1) {
//            filterString += " AND " + FIELD_STATUS_SHORT0 + operator +
//                    status.getShortValue1();
//        }
//        return filterString;
//    }
//
//
//    private String buildFilterEvent(final Event event, final String operator) {
//        String filterString = "";
//
//        if (event.getIndex() != -1) {
//            if (event.getIndex() < Event.EVENT_INDEX_MIN) {
//                mIsOneLimit = true;
//            } else {
//                filterString +=
//                        " AND " + FIELD_EVENT_INDEX + operator + event.getIndex();
//            }
//        }
//
//
//        if (event.getEvent() != -1) {
//            filterString +=
//                    " AND " + operator + event.getEvent();
//        }
//
//        if (event.getBatteryValue() != -1) {
//            filterString +=
//                    " AND " + FIELD_EVENT_VALUE + operator + event.getBatteryValue();
//        }
//
//        return filterString;
//    }
}
