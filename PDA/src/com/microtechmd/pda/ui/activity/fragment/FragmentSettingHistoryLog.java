package com.microtechmd.pda.ui.activity.fragment;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.microtechmd.pda.R;
import com.microtechmd.pda.database.DataSetHistory;
import com.microtechmd.pda.database.DbHistory;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.util.AndroidSystemInfoUtil;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lecho.lib.hellocharts.model.PointValue;

import static com.microtechmd.pda.ui.activity.ActivityPDA.BLOOD_GLUCOSE;
import static com.microtechmd.pda.ui.activity.ActivityPDA.CALIBRATION;
import static com.microtechmd.pda.ui.activity.ActivityPDA.HYPER;
import static com.microtechmd.pda.ui.activity.ActivityPDA.HYPO;
import static com.microtechmd.pda.ui.activity.ActivityPDA.PDA_ERROR;
import static com.microtechmd.pda.ui.activity.ActivityPDA.SENSOR_ERROR;
import static com.microtechmd.pda.ui.activity.ActivityPDA.SENSOR_EXPIRATION;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingContainer.TYPE_SETTING;

public class FragmentSettingHistoryLog extends FragmentBase
        implements
        EntityMessage.Listener {
    public static final long MILLISECOND_DAY = 1000 * 60 * 60 * 24;
    private View mRootView;
    private ImageView btn_left, btn_right;
    private boolean clickFlagLeft, clickFlagRight;

    private Calendar mCalendar;
    private List<DbHistory> dataErrListAll;
    private HistoryModel mHistoryModel;
    private HistoryAdapter mHistoryAdapter;
    private long firstTime;

    private DataSetHistory mDataSetHistory;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_history, container, false);
        btn_left = (ImageView) mRootView.findViewById(R.id.btn_left);
        btn_right = (ImageView) mRootView.findViewById(R.id.btn_right);

        setLeftButton(true);
        setRightButton(false);

        if (mDataSetHistory == null) {
            mDataSetHistory = new DataSetHistory(getActivity());
        }
        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        dataErrListAll = new ArrayList<>();
//        if (app != null) {
//            ArrayList<DbHistory> dataErr = app.getDataErrListAll();
//            dataErrListAll.addAll(dataErr);
//        }
        mHistoryModel = new HistoryModel();
        mHistoryAdapter = new HistoryAdapter();
        ((ListView) mRootView.findViewById(R.id.lv_log)).setAdapter(mHistoryAdapter);
        mCalendar = Calendar.getInstance();
        mCalendar.set(Calendar.HOUR_OF_DAY, 0);
        mCalendar.set(Calendar.MINUTE, 0);
        mCalendar.set(Calendar.SECOND, 0);
        updateDateTime(mCalendar);
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<DbHistory> dataBaseErrListAll = mDataSetHistory.getErrDbList();
                for (DbHistory dbHistory : dataBaseErrListAll) {
                    if (dbHistory.getEvent_type() == 5 ||
                            dbHistory.getEvent_type() == 6 ||
                            dbHistory.getEvent_type() == 10 ||
                            dbHistory.getEvent_type() == 11
                    ) {
                        dataErrListAll.add(dbHistory);
                    }
                }
//                dataErrListAll = mDataSetHistory.getErrDbList();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        queryHistory(mCalendar);
                        if (dataErrListAll.size() > 0) {
                            firstTime = Collections.min(dataErrListAll, new MyComparator()).getDate_time();
                        }
                    }
                });
            }
        }).start();
//        queryHistory(mCalendar);
//        if (dataErrListAll.size() > 0) {
//            firstTime = Collections.min(dataErrListAll, new MyComparator()).getDate_time();
//        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.ibt_back:
                ((ActivityPDA) getActivity())
                        .handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                                ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
                                ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
                                ParameterComm.SETTING_TYPE,
                                new byte[]{(byte) TYPE_SETTING}));
                break;
            case R.id.btn_left:
                if (clickFlagLeft) {
                    adjustDateTime(-MILLISECOND_DAY);
                }
                break;

            case R.id.btn_right:
                if (clickFlagRight) {
                    adjustDateTime(MILLISECOND_DAY);
                }
                break;
            default:
                break;
        }
    }

    private void setLeftButton(boolean isClick) {
        clickFlagLeft = isClick;
        if (isClick) {
            btn_left.setImageDrawable(getResources().getDrawable(R.drawable.btn_animation_left));
        } else {
            btn_left.setImageDrawable(getResources().getDrawable(R.drawable.btn_animation_left_grey));
        }
    }

    private void setRightButton(boolean isClick) {
        clickFlagRight = isClick;
        if (isClick) {
            btn_right.setImageDrawable(getResources().getDrawable(R.drawable.btn_animation_right));
        } else {
            btn_right.setImageDrawable(getResources().getDrawable(R.drawable.btn_animation_right_grey));
        }
    }

    @Override
    public void onReceive(EntityMessage message) {
        switch (message.getOperation()) {
            case EntityMessage.OPERATION_SET:
                handleSet(message);
                break;

            case EntityMessage.OPERATION_GET:
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
    }

    private void handleSet(EntityMessage message) {
    }


    protected void handleEvent(EntityMessage message) {
    }

    protected void handleNotification(EntityMessage message) {
    }


    protected void handleAcknowledgement(final EntityMessage message) {
    }

    private void queryHistory(Calendar calendar) {
        List<DbHistory> dataLisSelected = new ArrayList<>();
        long t_today = calendar.getTimeInMillis();
        long t_next = calendar.getTimeInMillis() + MILLISECOND_DAY;

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
        updateDateTime(mCalendar);

    }

    private boolean rangeInDefined(long current, long min, long max) {
        return Math.max(min, current) == Math.min(current, max);
    }

    private void adjustDateTime(long offset) {
        long time = mCalendar.getTimeInMillis() + offset;

        if (time <= System.currentTimeMillis()) {
            mCalendar.setTimeInMillis(time);
            queryHistory(mCalendar);
        }
    }

    private void updateDateTime(Calendar calendar) {
        String template;


        if ("en".equals(AndroidSystemInfoUtil.getLanguage().getLanguage())) {
            template = "E, MMMMM dd, yyyy";
        } else {
            template = "yyyy" + getString(R.string.year) + "MM" + getString(R.string.month) + "dd" +
                    getString(R.string.day);
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(template, Locale.getDefault());
        TextView textView = (TextView) mRootView.findViewById(R.id.tv_date);
        textView.setText(simpleDateFormat.format(calendar.getTimeInMillis()));
        if (dataErrListAll.size() == 0) {
            setLeftButton(false);
            setRightButton(false);
        } else {
            if (mCalendar.getTimeInMillis() + MILLISECOND_DAY > System.currentTimeMillis()) {
                setRightButton(false);
            } else {
                setRightButton(true);
            }


            if (mCalendar.getTimeInMillis() <= firstTime) {
                setLeftButton(false);
            } else {
                setLeftButton(true);
            }

        }
    }

    public class MyComparator implements Comparator<DbHistory> {
        @Override
        public int compare(DbHistory t1, DbHistory t2) {
            Long v1 = t1.getDate_time();
            Long v2 = t2.getDate_time();
            return v1.compareTo(v2);
        }
    }

    private class HistoryView extends LinearLayout {
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
                historyListView = new HistoryView(getActivity());
            }

            DbHistory history = mHistoryModel.getHistory(position);
            String comment = getEventContent(history.getEvent_type());

            historyListView.setView(history.getDate_time(),
                    true, comment);
            return historyListView;
        }

    }

    public String getAddress(byte[] addressByte) {
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

    private String getEventContent(int eventType) {
        String content = "";
        if (isAdded()) {
            switch (eventType) {
                case SENSOR_ERROR:
                    content = getString(R.string.alarm_sensor_error);
                    break;

                case SENSOR_EXPIRATION:
                    content = getString(R.string.alarm_expiration2);
                    break;

                case HYPO:
                    content = getString(R.string.alarm_hypo);
                    break;

                case HYPER:
                    content = getString(R.string.alarm_hyper);
                    break;
                case PDA_ERROR:
                    content = getString(R.string.alarm_pda_error);
                    break;
                case BLOOD_GLUCOSE:
                    content = getString(R.string.blood_glucose);
                    break;

                case CALIBRATION:
                    content = getString(R.string.calibrate_blood);
                    break;
                default:
                    break;
            }
        }
        return content;
    }
}
