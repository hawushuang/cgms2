package com.microtechmd.pda.ui.activity;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.database.DbHistory;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.SPUtils;
import com.microtechmd.pda.util.AndroidSystemInfoUtil;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ActivityHistoryLog extends ActivityPDA {
    private static final int VERTICAL_MIN_DISTANCE = 200;
    private static final int MIN_VELOCITY = 20;
    private DateTime mDateTime = null;
    private HistoryModel mHistoryModel = null;
    private HistoryAdapter mHistoryAdapter = null;
    private GestureDetector mGestureGraph = null;
    private boolean isTop;
    private List<DbHistory> dataErrListAll = null;
    private ApplicationPDA applicationPDA;

    private class HistoryView extends LinearLayout {
        public static final int TYPE_ALARM = 0;
        public static final int COUNT_TYPE = 1;

        private DecimalFormat mDecimalFormat = null;


        public HistoryView(Context context) {
            super(context);
            initializeLayout(context);
        }


        public void setView(long time, boolean timeFormat, final String comment) {
            String template;

            if (timeFormat) {
                template = "HH:mm:ss";
            } else {
                template = "hh:mm:ss a";
            }

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(template, Locale.getDefault());
            findViewById(R.id.iv_icon).setVisibility(INVISIBLE);
            TextView textView = (TextView) findViewById(R.id.tv_item_name);
            textView.setText(simpleDateFormat.format(new Date(time)));
            textView = (TextView) findViewById(R.id.tv_item_value);

            if (comment == null) {
                textView.setText("");
            } else {
                textView.setText(comment);
            }
        }


        private void initializeLayout(Context context) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.widget_setting_item, this, true);

            if (mDecimalFormat == null) {
                mDecimalFormat = new DecimalFormat();
            }
        }
    }


    private class HistoryModel {
        private ArrayList<DbHistory> mModelList;
        private ArrayList<DbHistory> mViewList;


        public HistoryModel() {
            mModelList = new ArrayList<>();
            mViewList = new ArrayList<>();
        }


        public DbHistory getHistory(int index) {
            if (index >= mViewList.size()) {
                return null;
            } else {
                return mViewList.get(index);
            }
        }


        public int getCount() {
            return mViewList.size();
        }


        public void setList(List<DbHistory> historyList) {
            if (historyList == null) {
                return;
            }

            mModelList.clear();

            if (historyList.size() == 0) {
                return;
            }
            for (int i = historyList.size() - 1; i >= 0; i--) {
                DbHistory history = historyList.get(i);
                history.setValue(0);
                mModelList.add(history);
            }
        }


        public void update() {
            mViewList.clear();

            if (mModelList.size() == 0) {
                return;
            }

            for (int i = mModelList.size() - 1; i >= 0; i--) {
                DbHistory history = mModelList.get(i);
                mViewList.add(history);
            }

        }
    }


    private class HistoryAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mHistoryModel.getCount();
        }


        @Override
        public Object getItem(int position) {
            return null;
        }


        @Override
        public long getItemId(int position) {
            return 0;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HistoryView historyListView;

            if (convertView != null) {
                historyListView = (HistoryView) convertView;
            } else {
                historyListView = new HistoryView(ActivityHistoryLog.this);
            }

            DbHistory history = mHistoryModel.getHistory(position);
            Event event = new Event();
            event.setEvent(history.getEvent_type());
            String comment = getEventContent(event);

            historyListView.setView(history.getDate_time(),
                    true, comment);
            return historyListView;
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_history);
        if (dataErrListAll == null) {
            dataErrListAll = new ArrayList<>();
        }
        dataErrListAll.clear();
        applicationPDA = (ApplicationPDA) getApplication();
        if (applicationPDA != null) {
            ArrayList<DbHistory> dataErr = applicationPDA.getDataErrListAll();
            dataErrListAll.addAll(dataErr);
        }

        if (mHistoryModel == null) {
            mHistoryModel = new HistoryModel();
        }

        if (mHistoryAdapter == null) {
            mHistoryAdapter = new HistoryAdapter();
            ((ListView) findViewById(R.id.lv_log)).setAdapter(mHistoryAdapter);
        }

        if (mDateTime == null) {
            mDateTime = new DateTime(Calendar.getInstance());
            mDateTime.setHour(0);
            mDateTime.setMinute(0);
            mDateTime.setSecond(0);
            updateDateTime(mDateTime);
            queryHistory(mDateTime);
        }
        initGesture();
//        (findViewById(R.id.lv_log)).setOnTouchListener(new View.OnTouchListener() {
//            public boolean onTouch(View v, MotionEvent event) {
//
//                return mGestureGraph.onTouchEvent(event);
//            }
//        });
    }

    private void initGesture() {
        if (mGestureGraph == null) {
            mGestureGraph = new GestureDetector(this,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDown(MotionEvent e) {
                            return true;
                        }

                        @Override
                        public boolean onFling(MotionEvent e1, MotionEvent e2,
                                               float velocityX, float velocityY) {
                            if (e1.getX() - e2.getX() > VERTICAL_MIN_DISTANCE &&
                                    Math.abs(velocityX) > MIN_VELOCITY) {
                                adjustDateTime((DateTime.MILLISECOND_PER_SECOND *
                                        DateTime.SECOND_PER_MINUTE *
                                        DateTime.MINUTE_PER_HOUR *
                                        DateTime.HOUR_PER_DAY));
                            } else if (e2.getX() - e1.getX() > VERTICAL_MIN_DISTANCE &&
                                    Math.abs(velocityX) > MIN_VELOCITY) {
                                adjustDateTime(-(DateTime.MILLISECOND_PER_SECOND *
                                        DateTime.SECOND_PER_MINUTE *
                                        DateTime.MINUTE_PER_HOUR *
                                        DateTime.HOUR_PER_DAY));
                            }

                            return false;
                        }
                    });
        }
    }


    @Override
    protected void onClickView(View v) {
        super.onClickView(v);

        switch (v.getId()) {
            case R.id.btn_left:
                adjustDateTime(-(DateTime.MILLISECOND_PER_SECOND *
                        DateTime.SECOND_PER_MINUTE * DateTime.MINUTE_PER_HOUR *
                        DateTime.HOUR_PER_DAY));
                break;

            case R.id.btn_right:
                adjustDateTime((DateTime.MILLISECOND_PER_SECOND *
                        DateTime.SECOND_PER_MINUTE * DateTime.MINUTE_PER_HOUR *
                        DateTime.HOUR_PER_DAY));
                break;
            case R.id.ibt_back:
                finish();
                break;

            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isTop = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isTop = false;
    }

    @Override
    protected void handleNotification(final EntityMessage message) {
        super.handleNotification(message);

        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
                (message.getParameter() == ParameterMonitor.PARAM_HISTORY)) {
            if (message
                    .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_MODEL) {
//                if (isTop) {
//                    mLog.Error(getClass(), "刷新日志");
//                    mHistoryModel.setList(new DataList(message.getData()));
//                    mHistoryModel.update();
//                    updateDateTime(mDateTime);
//                    ((ListView) findViewById(R.id.lv_log)).setAdapter(mHistoryAdapter);
//                }
            }
        }

    }


    private void queryHistory(final DateTime dateTime) {
        mLog.Debug(getClass(), "Query history");
        List<DbHistory> dataLisSelected = new ArrayList<>();
        long t_today = dateTime.getCalendar().getTimeInMillis();
        long t_next = dateTime.getCalendar().getTimeInMillis() +
                (long) (DateTime.MILLISECOND_PER_SECOND *
                        DateTime.SECOND_PER_MINUTE * DateTime.MINUTE_PER_HOUR *
                        DateTime.HOUR_PER_DAY);

        for (int i = 0; i < dataErrListAll.size(); i++) {
            long t = dataErrListAll.get(i).getDate_time();
            if (rangeInDefined(t, t_today, t_next)) {
                dataLisSelected.add(dataErrListAll.get(i));
            }
        }
        mLog.Error(getClass(), "err总数：" + dataErrListAll.size());
        mHistoryModel.setList(dataLisSelected);
        mHistoryModel.update();
        mHistoryAdapter.notifyDataSetChanged();
        updateDateTime(mDateTime);
//        for (History history : dataErrListAll) {
//            mLog.Error(getClass(), "总数时间：" + history.getDateTime().getBCD());
//        }
//        mLog.Error(getClass(), "总数：" + dataErrListAll.size());
//        ((ListView) findViewById(R.id.lv_log)).setAdapter(mHistoryAdapter);
//        History history = new History(new DateTime(dateTime.getByteArray()),
//                new Status(-1), new Event(-1, -1, -1));
//        DataList dataList = new DataList();
//        dataList.pushData(history.getByteArray());
//        Calendar calendar = dateTime.getCalendar();
//        calendar.setTimeInMillis(dateTime.getCalendar().getTimeInMillis() +
//                (long) (DateTime.MILLISECOND_PER_SECOND *
//                        DateTime.SECOND_PER_MINUTE * DateTime.MINUTE_PER_HOUR *
//                        DateTime.HOUR_PER_DAY));
//        history.setDateTime(new DateTime(calendar));
//        dataList.pushData(history.getByteArray());
//        handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
//                ParameterGlobal.ADDRESS_LOCAL_MODEL, ParameterGlobal.PORT_MONITOR,
//                ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_GET,
//                ParameterMonitor.PARAM_HISTORY, dataList.getByteArray()));

    }

    private boolean rangeInDefined(long current, long min, long max) {
        return Math.max(min, current) == Math.min(current, max);
    }

    private void adjustDateTime(long offset) {
        long time = mDateTime.getCalendar().getTimeInMillis() + offset;

        if (time <= System.currentTimeMillis()) {
            Calendar calendar = mDateTime.getCalendar();
            calendar.setTimeInMillis(time);
            mDateTime.setCalendar(calendar);
            queryHistory(mDateTime);
        }
    }


    private void updateDateTime(final DateTime dateTime) {
        String template;


        if ("en".equals(AndroidSystemInfoUtil.getLanguage().getLanguage())) {
            template = "E, MMMMM dd, yyyy";
        } else {
            template = "yyyy" + getString(R.string.year) + "MM" + getString(R.string.month) + "dd" +
                    getString(R.string.day);
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(template, Locale.getDefault());
        TextView textView = (TextView) findViewById(R.id.tv_date);
        textView.setText(simpleDateFormat
                .format(new Date(dateTime.getCalendar().getTimeInMillis())));
    }
}
