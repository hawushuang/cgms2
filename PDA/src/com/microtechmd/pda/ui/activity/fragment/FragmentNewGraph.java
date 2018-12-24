package com.microtechmd.pda.ui.activity.fragment;


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.entity.CalibrationHistory;
import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.SPUtils;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.util.CalibrationSaveUtil;
import com.microtechmd.pda.util.FormatterUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import static com.microtechmd.pda.ui.activity.ActivityPDA.CALIBRATION_HISTORY;
import static com.microtechmd.pda.ui.activity.ActivityPDA.RFSIGNAL;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettings.REALTIMEFLAG;


public class FragmentNewGraph extends FragmentBase
        implements
        EntityMessage.Listener {
    public static final int GRAPH_DIVISION_VERTICAL = 2;
    private static final int VERTICAL_MIN_DISTANCE = 100;
    private static final int MIN_VELOCITY = 10;

    private static final long MILLISECOND_24 = (long) (DateTime.MILLISECOND_PER_SECOND *
            DateTime.SECOND_PER_MINUTE * DateTime.MINUTE_PER_HOUR *
            DateTime.HOUR_PER_DAY);

    private static final long MILLISECOND_12 = (long) (DateTime.MILLISECOND_PER_SECOND *
            DateTime.SECOND_PER_MINUTE * DateTime.MINUTE_PER_HOUR *
            12);

    private static final long MILLISECOND_6 = (long) (DateTime.MILLISECOND_PER_SECOND *
            DateTime.SECOND_PER_MINUTE * DateTime.MINUTE_PER_HOUR *
            6);

    private static final long millisecond_1 = (long) (DateTime.MILLISECOND_PER_SECOND *
            DateTime.SECOND_PER_MINUTE * DateTime.MINUTE_PER_HOUR);
    private long now_millisecond;
    private long visible_range_millisecond;
    private long max;

    private boolean mIsHistoryQuerying = false;
    private DateTime mDateTime = null;
    private HistoryModel mHistoryModel = null;
    private Handler mHandler = null;
    private Runnable mRunnable = null;
    private GestureDetector mGestureGraph = null;

    private View mRootView = null;
    private LineChart mLineChart;
    private TextView text_view_time;
    private TextView synchronize;

    private boolean newHistoryFlag;
    //    private DateTime todayDateTime;
    private DateTime nowDateTime;
    private XAxis xAxis;
    private String str;
    private int maxValue;
    private long maxTime;

    private class HistoryModel {
        private ArrayList<History> mModelList = null;
        private ArrayList<History> mViewList = null;


        public HistoryModel() {
            mModelList = new ArrayList<>();
            mViewList = new ArrayList<>();
        }


        public History getHistory(int index) {
            if (index >= mViewList.size()) {
                return null;
            } else {
                return mViewList.get(index);
            }
        }


        public int getCount() {
            return mViewList.size();
        }


        public void setList(DataList historyList) {
            if (historyList == null) {
                return;
            }

            mModelList.clear();

            if (historyList.getCount() == 0) {
                return;
            }

            History history = new History();
            for (int i = 0; i < historyList.getCount(); i++) {
                history.setByteArray(historyList.getData(i));
                if (history.getEvent().getEvent() == 7) {
                    mModelList.add(new History(history.getByteArray()));
                }
            }
        }

        public void update() {
            mViewList.clear();

            if (mModelList.size() == 0) {
                return;
            }

            mViewList.addAll(mModelList);
        }
    }

    public void setTimeData(int timeData) {
        mDateTime = new DateTime(Calendar.getInstance());
        mLineChart.clearValues();
        switch (timeData) {
            case FragmentHome.TIME_DATA_6:
//                now_millisecond = MILLISECOND_6;
                visible_range_millisecond = MILLISECOND_6;
                updateGraphProfiles();
                break;
            case FragmentHome.TIME_DATA_12:
//                now_millisecond = MILLISECOND_12;
                visible_range_millisecond = MILLISECOND_12;
                updateGraphProfiles();
                break;
            case FragmentHome.TIME_DATA_24:
//                now_millisecond = MILLISECOND_24;
                newHistoryFlag = true;
                visible_range_millisecond = MILLISECOND_24;
                if (xAxis.getLimitLines().size() > 6) {
                    xAxis.removeAllLimitLines();
                }
                queryHistory(mDateTime);
                break;
            default:
                break;
        }
//        queryHistory(mDateTime);
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_newgraph, container, false);
        TextView text_view_unit = (TextView) mRootView.findViewById(R.id.text_view_unit);
        text_view_unit.setText(getResources().getString(R.string.history_bg) +
                " (" + getResources().getString(R.string.unit_mmol_l) + ")");
        text_view_time = (TextView) mRootView.findViewById(R.id.text_view_time);
        synchronize = (TextView) mRootView.findViewById(R.id.synchronize);
        boolean realtimeFlag = (boolean) SPUtils.get(getActivity(), REALTIMEFLAG, true);
        if (realtimeFlag) {
            synchronize.setVisibility(View.GONE);
        } else {
            synchronize.setVisibility(View.VISIBLE);
        }
        synchronize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int rfsignal = (int) SPUtils.get(getActivity(), RFSIGNAL, 0);
                if (rfsignal == 0) {
                    Toast.makeText(getActivity(),
                            getActivity().getResources().getString(R.string.connect_fail),
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    synchronize_data();
                }
            }
        });
//        mRootView.setOnTouchListener(new View.OnTouchListener() {
//            public boolean onTouch(View v, MotionEvent event) {
//
//                return mGestureGraph.onTouchEvent(event);
//            }
//        });

        initChart();
        mLineChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
//                int rightXIndex = mLineChart.getHighestVisibleXIndex();    //获取可视区域中，显示在x轴最右边的index
                float indexHighest = mLineChart.getHighestVisibleX();
                float indexLowest = mLineChart.getLowestVisibleX();
                float lowest = mLineChart.getXChartMin();
//                Log.e("**********", "最大： " + indexHighest + "最小  " + indexLowest + "offset:  " + lowest);
                if (lastPerformedGesture == ChartTouchListener.ChartGesture.DRAG) {
                    if ((indexLowest - lowest) < 10) {
                        newHistoryFlag = false;
                        adjustDateTime(-now_millisecond);
                    } else if (indexHighest == DateTime.SECOND_PER_HOUR) {
//                        adjustDateTime((DateTime.MILLISECOND_PER_SECOND *
//                                DateTime.SECOND_PER_MINUTE *
//                                DateTime.MINUTE_PER_HOUR *
//                                DateTime.HOUR_PER_DAY));
                    }
                }
            }

            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
                if (me1.getX() - me2.getX() > VERTICAL_MIN_DISTANCE &&
                        Math.abs(velocityX) > MIN_VELOCITY) {
//                    adjustDateTime((DateTime.MILLISECOND_PER_SECOND *
//                            DateTime.SECOND_PER_MINUTE *
//                            DateTime.MINUTE_PER_HOUR *
//                            DateTime.HOUR_PER_DAY));
                } else if (me2.getX() - me1.getX() > VERTICAL_MIN_DISTANCE &&
                        Math.abs(velocityX) > MIN_VELOCITY) {
                    newHistoryFlag = false;
                    adjustDateTime(-now_millisecond);
                }
            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
            }
        });
        return mRootView;
    }

    private void initChart() {
        mLineChart = (LineChart) mRootView.findViewById(R.id.chart);
//        mLineChart.setGridBackgroundColor(Color.parseColor("#E5575A68"));
//        mLineChart.setDrawGridBackground(true);
        //显示边界
        mLineChart.setDrawBorders(true);
        mLineChart.setBorderColor(Color.GRAY);
        mLineChart.getDescription().setEnabled(false);
        mLineChart.getLegend().setEnabled(false);
        mLineChart.setScaleXEnabled(false);
        mLineChart.setScaleYEnabled(false);
//        mLineChart.setScaleXEnabled(false);
        mLineChart.setDoubleTapToZoomEnabled(false);
        mLineChart.setDragDecelerationEnabled(false);
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mLineChart.setData(data);


        xAxis = mLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setAvoidFirstLastClipping(true);
        FormatterUtil formatterUtil = new FormatterUtil();
        xAxis.setValueFormatter(formatterUtil);

        xAxis.setLabelCount(7);
        xAxis.setGranularity(36);
//        xAxis.setAxisMaximum(864);
//        xAxis.setSpaceMax(864f / 4f);
//        xAxis.setSpaceMin(864f / 6f);


        YAxis leftYAxis = mLineChart.getAxisLeft();
        YAxis rightYAxis = mLineChart.getAxisRight();
        rightYAxis.setEnabled(false);

        leftYAxis.setTextColor(Color.WHITE);
        leftYAxis.setDrawGridLines(false);
        leftYAxis.setAxisMinimum(0);
        leftYAxis.setAxisMaximum(30);

//        todayDateTime = new DateTime(Calendar.getInstance());
        //重置所有限制线,以避免重叠线
        leftYAxis.removeAllLimitLines();
        xAxis.removeAllLimitLines();

    }

    private void addLimitLine(String str, float x) {
        //可以设置一条警戒线，如下：
        LimitLine ll = new LimitLine(x, str);
        ll.setLineColor(Color.RED);
        ll.setLineWidth(1f);
        ll.setTextColor(Color.WHITE);
        ll.setTextSize(12f);
        xAxis.addLimitLine(ll);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_MONITOR, this);
        now_millisecond = MILLISECOND_24;
        visible_range_millisecond = MILLISECOND_6;
        newHistoryFlag = true;
        nowDateTime = new DateTime(Calendar.getInstance());

        if (mHistoryModel == null) {
            mHistoryModel = new HistoryModel();
        }

        if (mDateTime == null) {
            mDateTime = new DateTime(Calendar.getInstance());
//            mDateTime.setMonth(1);
//            mDateTime.setDay(4);
//            mDateTime.setHour(12);
//            mDateTime.setMinute(0);
//            mDateTime.setSecond(0);
        }

        if (mHistoryModel.getCount() <= 0) {
            mHandler = new Handler();
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    queryHistory(mDateTime);
                }
            };
            mHandler.postDelayed(mRunnable, 100);
        }

        updateGraphProfiles();
    }


    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRunnable);
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_MONITOR, this);
//        frameLayout.removeAllViews();
        super.onDestroyView();
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
                break;

            case EntityMessage.OPERATION_NOTIFY:
                handleNotification(message);
                break;

            case EntityMessage.OPERATION_ACKNOWLEDGE:
                break;

            default:
                break;
        }
    }

    private void handleSet(EntityMessage message) {
        if (message.getParameter() == ParameterComm.BROADCAST_SAVA) {
            if (message.getData()[0] == 0) {
                synchronize.setVisibility(View.VISIBLE);
            } else {
                synchronize.setVisibility(View.GONE);
            }
        }
        if (message.getParameter() == ParameterGlucose.PARAM_FILL_LIMIT
                || message.getParameter() == ParameterGlucose.PARAM_BG_LIMIT) {
            updateGraphProfiles();
        }
        if (message.getParameter() == ParameterComm.RESET_DATA) {
            if (message.getData()[0] == 0) {
                mLineChart.clearValues();
                mDateTime = new DateTime(Calendar.getInstance());
                queryHistory(mDateTime);
            } else {
                mLineChart.clearValues();
                updateGraphProfiles();
            }
        }
    }


    private void handleNotification(final EntityMessage message) {
        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
                (message.getParameter() == ParameterMonitor.PARAM_HISTORY)) {
            if (message
                    .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_MODEL) {
                mLog.Debug(getClass(), "Receive history");

                mIsHistoryQuerying = false;
                mHistoryModel.setList(new DataList(message.getData()));
                mHistoryModel.update();
                updateGraphProfiles();
            }
        }

        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
                (message.getParameter() == ParameterMonitor.PARAM_STATUS)) {
            if ((message
                    .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_CONTROL) ||
                    (message
                            .getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER)) {
                mLog.Debug(getClass(), "new history");
                newHistoryFlag = true;
                mDateTime = new DateTime(Calendar.getInstance());
                queryHistory(mDateTime);
            }
        }

    }

    private void showDialogProgress() {
        ((ActivityPDA) getActivity()).showDialogLoading();
    }


    private void dismissDialogProgress() {
        ((ActivityPDA) getActivity()).dismissDialogLoading();
    }

    private void queryHistory(final DateTime dateTime) {
        if (mIsHistoryQuerying) {
            return;
        }
        mLog.Debug(getClass(), "Query history");
        showDialogProgress();
        mIsHistoryQuerying = true;
        History history = new History(new DateTime(),
                new Status(-1), new Event(-1, -1, -1));
        DataList dataList = new DataList();
        Calendar calendar = dateTime.getCalendar();
        calendar.setTimeInMillis(dateTime.getCalendar().getTimeInMillis() - now_millisecond);
        history.setDateTime(new DateTime(calendar));
        dataList.pushData(history.getByteArray());
        history.setDateTime(new DateTime(dateTime.getByteArray()));
        dataList.pushData(history.getByteArray());
        ((ActivityPDA) getActivity()).handleMessage(new EntityMessage(
                ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_LOCAL_MODEL, ParameterGlobal.PORT_MONITOR,
                ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_GET,
                ParameterMonitor.PARAM_HISTORY, dataList.getByteArray()));
    }

    private void updateGraphProfiles() {
//        mLineChart.clearValues();
//        mLineChart.setVisibleXRangeMinimum(0);
//        mLineChart.getXAxis().removeAllLimitLines();
        DateTime todayDateTime = new DateTime(Calendar.getInstance());
        todayDateTime.setHour(0);
        todayDateTime.setMinute(0);
        todayDateTime.setSecond(0);
        long todayMills = todayDateTime.getCalendar().getTimeInMillis();
        long min;

        if (newHistoryFlag) {
            mLineChart.clearValues();
            mLineChart.resetZoom();
            max = mDateTime.getCalendar().getTimeInMillis() - todayMills;
            min = max - now_millisecond;
            mLineChart.setVisibleXRangeMaximum(now_millisecond / 100000);
            mLineChart.notifyDataSetChanged();
            if (mHistoryModel.getCount() > 0) {
                if ((mHistoryModel.getHistory(0).getDateTime().getCalendar().getTimeInMillis() - todayMills) >
                        mDateTime.getCalendar().getTimeInMillis() - todayMills - now_millisecond) {
                    addEntry((mDateTime.getCalendar().getTimeInMillis() - todayMills - now_millisecond) / 100000, 0);
                }
                for (int i = 0; i < mHistoryModel.getCount(); i++) {
                    History history = mHistoryModel.getHistory(i);
                    if (history.getEvent().getEvent() == 7) {
                        Status status = history.getStatus();
                        int value = status.getShortValue1() & 0xFFFF;
                        value /= 10;
                        long time = (history.getDateTime().getCalendar().getTimeInMillis() - todayMills) / 1000;

                        addEntry(time / 100, value);
                    }
//                addEntry(i, value);
                }
                History historyMax = mHistoryModel.getHistory(mHistoryModel.getCount() - 1);
                maxValue = historyMax.getStatus().getShortValue1() & 0xFFFF;
                maxTime = historyMax.getDateTime().getCalendar().getTimeInMillis() - todayMills;
            } else {
                long timeMax = mDateTime.getCalendar().getTimeInMillis() - todayMills;
                long timeMin = timeMax - now_millisecond;
                addEntry(timeMin / 100000, 0);
//                for (int i = 0; i < 800; i+=80) {
//                    addEntry(timeMin / 100000 + i + 50, (float) (Math.random()*200+50));
//                }
//                addEntry(timeMax / 100000-300, (float) (Math.random()*200+50));
//                addEntry(timeMax / 100000-200, (float) (Math.random()*200+50));
//                addEntry(timeMax / 100000-100, (float) (Math.random()*200+50));
                addEntry(timeMax / 100000, 0);
                maxValue = 0;
                maxTime = max;
            }
            mLineChart.zoom(1.0f, 1.0f, 0f, 0f);
//            mLineChart.setVisibleXRangeMaximum((max-min) / 100000 + max / 100000 / 7);
        } else {
            min = mDateTime.getCalendar().getTimeInMillis() - todayMills - now_millisecond;
            if (mHistoryModel.getCount() > 0) {
                for (int i = mHistoryModel.getCount() - 1; i >= 0; i--) {
                    History history = mHistoryModel.getHistory(i);
                    if (history.getEvent().getEvent() == 7) {
                        Status status = history.getStatus();
                        int value;
                        value = status.getShortValue1() & 0xFFFF;
                        value /= 10;
                        long time = (history.getDateTime().getCalendar().getTimeInMillis() - todayMills) / 1000;

                        addEntry(time / 100, value);
                    }
//                addEntry(i, value);
                }
            } else {
                long time1 = mDateTime.getCalendar().getTimeInMillis() - todayMills;
                long time2 = time1 - now_millisecond;
                addEntry(time1 / 100000 - 1, 0);
//                for (int i = 0; i < 40; i++) {
//                    addEntry(time1 / 100000 - i - 10, i * 2 + 50);
//                }
                addEntry(time2 / 100000, 0);
            }
        }
        int count = mLineChart.getData().getDataSetCount();
        if (count > 4) {
            mLineChart.getData().removeDataSet(4);
            mLineChart.getData().removeDataSet(3);
            mLineChart.getData().removeDataSet(2);
            mLineChart.getData().removeDataSet(1);
        } else if (count > 3) {
            mLineChart.getData().removeDataSet(3);
            mLineChart.getData().removeDataSet(2);
            mLineChart.getData().removeDataSet(1);
        }
//        mLineChart.setVisibleXRange(0, visibleLength / 100000 + now_millisecond / 100000 / 7);
        mLineChart.setVisibleXRangeMaximum(visible_range_millisecond / 100000);
        if (visible_range_millisecond == MILLISECOND_24) {
            mLineChart.moveViewToX(min / 100000);
        } else {
            mLineChart.moveViewToX(max / 100000);
        }
//        mLineChart.notifyDataSetChanged();
//        mLineChart.zoom(1.0f, 1.0f, 0f, 0f);
//        mLineChart.setVisibleXRange(10, 500);
//        int cou = mLineChart.getData().getDataSetByIndex(0).getEntryCount();
//        mLog.Error(getClass(),"点数量："+cou);
//        mLineChart.setVisibleXRangeMaximum(max / 100000 + max / 100000 / 7);
//        mLineChart.setVisibleXRangeMinimum((min/10000));
//        mLineChart.setVisibleXRangeMaximum(500);
//        mLineChart.setVisibleXRange(0, (max - min) / 100000 + max / 100000 / 7);

//        text_view_time.setText(((ActivityPDA) getActivity())
//                .getDateString(mDateTime.getCalendar().getTimeInMillis(), null));
        addValueTextEntry(maxTime, maxValue);
        addCalibrationEntry(todayMills, min, max);
        addRangLine(min / 100000, max / 100000 + visible_range_millisecond / 100000 / 7);

        DateTime limitLine = new DateTime(mDateTime.getByteArray());
        limitLine.setHour(0);
        limitLine.setMinute(0);
        limitLine.setSecond(0);
        Calendar c = mDateTime.getCalendar();
        c.setTimeInMillis(mDateTime.getCalendar().getTimeInMillis() - now_millisecond);
        DateTime limitDateTimte = new DateTime(c);
        String nowStr = ((ActivityPDA) getActivity()).getDateString(mDateTime.getCalendar().getTimeInMillis(), null);
        if ((limitDateTimte.getCalendar().getTimeInMillis() - todayMills) <= 0) {
            if (!nowStr.equals(str)) {
                addLimitLine(nowStr, (limitLine.getCalendar().getTimeInMillis() - todayMills) / 100000);
            }
            str = nowStr;
        }
        dismissDialogProgress();
    }

    private void addValueTextEntry(long t, int value) {
        ArrayList<Entry> yVals = new ArrayList<>();
        yVals.add(new Entry(t / 100000, value / 100));
        if (yVals.size() > 0) {
            LineDataSet set;
            set = new LineDataSet(yVals, null);
            setValueTextLineDataSet(set);
            LineData data = mLineChart.getData();
            data.addDataSet(set);
            // set data
            mLineChart.setData(data);
        }
    }

    private void addCalibrationEntry(long todayMills, long min, long max) {
        List<CalibrationHistory> list = (List<CalibrationHistory>) CalibrationSaveUtil.get(getActivity(), CALIBRATION_HISTORY);
        ArrayList<Entry> yVals = new ArrayList<>();

        if (list != null) {
            for (CalibrationHistory calibrationHistory : list) {
                if (rangeInDefined(calibrationHistory.getTime() - todayMills, min, max)) {
                    yVals.add(new Entry((calibrationHistory.getTime() - todayMills) / 100000, calibrationHistory.getValue()));
                }
            }
        }
        if (yVals.size() > 0) {
//            mLog.Error(getClass(), "刷新");
            LineDataSet set;
            set = new LineDataSet(yVals, null);
            setCalibrationLineDataSet(set);
            LineData data = mLineChart.getData();
            data.addDataSet(set);
            // set data
            mLineChart.setData(data);
        }
    }

    private boolean rangeInDefined(long current, long min, long max) {
        return Math.max(min, current) == Math.min(current, max);
    }

    private void addRangLine(float minIndex, float maxIndex) {
        int mHyper = ((ActivityPDA) getActivity())
                .getDataStorage(ActivityPDA.class.getSimpleName())
                .getInt(FragmentSettings.SETTING_HYPER, 0);
        int mHypo = ((ActivityPDA) getActivity())
                .getDataStorage(ActivityPDA.class.getSimpleName())
                .getInt(FragmentSettings.SETTING_HYPO, 0);

        ArrayList<Entry> yVals1 = new ArrayList<>();
        yVals1.add(new Entry(minIndex, mHypo / 100));
        yVals1.add(new Entry(maxIndex, mHypo / 100));
        ArrayList<Entry> yVals2 = new ArrayList<>();
        yVals2.add(new Entry(minIndex, mHyper / 100));
        yVals2.add(new Entry(maxIndex, mHyper / 100));
        LineDataSet set1, set2;
        set1 = new LineDataSet(yVals1, null);
        setLineDataSet(set1, Color.BLACK);
//        setLineDataSet(set1,Color.parseColor("#ff333645"));
//        set1.setFillFormatter(new IFillFormatter() {
//            @Override
//            public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
//                return mLineChart.getAxisLeft().getAxisMinimum();
//            }
//        });

        set2 = new LineDataSet(yVals2, null);
        setLineDataSet(set2, Color.LTGRAY);
//        setLineDataSet(set2,Color.parseColor("#ff333645"));
//        set2.setFillFormatter(new IFillFormatter() {
//            @Override
//            public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
//                return mLineChart.getAxisLeft().getAxisMaximum();
//            }
//        });

        LineData data = mLineChart.getData();
        data.addDataSet(set2);
        data.addDataSet(set1);
        // set data
        mLineChart.setData(data);
    }

    private void setValueTextLineDataSet(LineDataSet set) {
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(0.5f);
        set.setDrawCircles(true);
        set.setDrawCircleHole(true);
        set.setCircleHoleRadius(2f);
        set.setCircleColor(Color.GREEN);
        set.setCircleRadius(4);
        set.setDrawValues(true);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(12f);
        set.setColor(Color.TRANSPARENT);
        set.setHighlightEnabled(false);
    }

    private void setCalibrationLineDataSet(LineDataSet set) {
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(0.5f);
        set.setDrawCircles(true);
        set.setDrawCircleHole(false);
        set.setCircleColor(Color.RED);
        set.setCircleRadius(4);
        set.setDrawValues(true);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(12f);
        set.setColor(Color.TRANSPARENT);
        set.setHighlightEnabled(false);
    }

    private void setLineDataSet(LineDataSet set, int color) {
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(0.5f);
        set.setDrawCircles(false);
        set.setColor(Color.GREEN);
        set.setDrawFilled(true);
        set.setDrawValues(false);
        set.setHighlightEnabled(false);
        set.enableDashedLine(10f, 5f, 0f);
        set.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
        set.setFillColor(color);
    }


    private void adjustDateTime(long offset) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        date.setTime(mDateTime.getCalendar().getTimeInMillis());
        long time = mDateTime.getCalendar().getTimeInMillis() + offset;
        if (time <= System.currentTimeMillis()) {
            Calendar calendar = mDateTime.getCalendar();
            calendar.setTimeInMillis(time);
            mDateTime.setCalendar(calendar);
            queryHistory(mDateTime);
        }
    }

    private void addEntry(float time, float value) {

        LineData data = mLineChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

//            data.addEntry(new Entry(time, value / 10), 0);
            set.addEntryOrdered(new Entry(time, value / 10));
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mLineChart.notifyDataSetChanged();

//            mLineChart.setVisibleXRangeMaximum(now_millisecond / 100000);

            // limit the number of visible entries
//            mLineChart.setVisibleXRangeMaximum(8);
//            mLineChart.setVisibleYRange(30, AxisDependency.LEFT);
            // move to the latest entry
//            mLineChart.moveViewToX(value);
            mLineChart.fitScreen();
            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);

        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, null);
//        set.setCircleRadius(3f);
        set.setDrawCircles(false);
//        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawIcons(true);
//        set.setValueTextColor(Color.WHITE);
        return set;
    }

    private void synchronize_data() {
        if (getActivity() != null) {
            ((ActivityPDA) getActivity()).handleMessage(
                    new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_VIEW,
                            ParameterGlobal.ADDRESS_LOCAL_CONTROL,
                            ParameterGlobal.PORT_MONITOR,
                            ParameterGlobal.PORT_MONITOR,
                            EntityMessage.OPERATION_SET,
                            ParameterComm.SYNCHRONIZE_DATA,
                            null));
        }
    }
}
