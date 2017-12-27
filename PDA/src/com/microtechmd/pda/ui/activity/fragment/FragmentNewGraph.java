package com.microtechmd.pda.ui.activity.fragment;


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.LogPDA;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.ui.widget.WidgetGraph;
import com.microtechmd.pda.util.FormatterUtil;
import com.microtechmd.pda.util.TimeUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.SimpleTimeZone;

import static com.microtechmd.pda.ui.activity.ActivityPDA.GLUCOSE_UNIT_MMOL;


public class FragmentNewGraph extends FragmentBase
        implements
        EntityMessage.Listener {
    public static final int GRAPH_DIVISION_VERTICAL = 2;
    private static final int VERTICAL_MIN_DISTANCE = 100;
    private static final int MIN_VELOCITY = 10;

    private boolean mIsHistoryQuerying = false;
    private DateTime mDateTime = null;
    private HistoryModel mHistoryModel = null;
    private Handler mHandler = null;
    private Runnable mRunnable = null;
    private GestureDetector mGestureGraph = null;

    private View mRootView = null;
    private LineChart mLineChart;
    private TextView text_view_time;

    private int mHyper;
    private int mHypo;

    private boolean newHistoryFlag;
    private DateTime todayDateTime;
    private String nowStr;

    private class HistoryView {
        public static final int TYPE_GLUCOSE = 0;
        public static final int TYPE_IMPEDANCE = 1;
        public static final int COUNT_TYPE = 2;
    }


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

            History history =
                    new History(historyList.getData(historyList.getCount() - 1));
            DateTime dateTime = history.getDateTime();
            Status status;
            Event event;

            if ((dateTime.getHour() != 0) || (dateTime.getMinute() != 0) ||
                    (dateTime.getSecond() != 0)) {
                dateTime.setHour(0);
                dateTime.setMinute(0);
                dateTime.setSecond(0);
                status = new Status();
                event = new Event();
                event.setPort(ParameterGlobal.PORT_MONITOR);
                history.setDateTime(dateTime);
                history.setStatus(status);
                history.setEvent(event);
                historyList.pushData(history.getByteArray());
            }

            int glucoseAmount = 0;
            for (int i = historyList.getCount() - 1; i >= 0; i--) {
                history.setByteArray(historyList.getData(i));
                event = history.getEvent();
                status = history.getStatus();
                status.setByteValue1(HistoryView.COUNT_TYPE);

                if ((event.getPort() == ParameterGlobal.PORT_MONITOR) ||
                        (event.getPort() == ParameterGlobal.PORT_GLUCOSE)) {
                    if ((byte) status.getByteValue2() == 0xFF) {
                        status.setByteValue1(HistoryView.TYPE_IMPEDANCE);
                    } else {
                        status.setByteValue1(HistoryView.TYPE_GLUCOSE);
                    }
                    history.setStatus(status);
                    mModelList.add(new History(history.getByteArray()));
                }
            }
        }


        public void update() {
            mViewList.clear();

            if (mModelList.size() == 0) {
                return;
            }

            for (int i = mModelList.size() - 1; i >= 0; i--) {
                History history = mModelList.get(i);
                mViewList.add(history);
            }
        }
    }

    public void setTimeData(int timeData) {
        switch (timeData) {
            case FragmentHome.TIME_DATA_6:
                adjustDateTime(-(DateTime.MILLISECOND_PER_SECOND *
                        DateTime.SECOND_PER_MINUTE *
                        DateTime.MINUTE_PER_HOUR *
                        DateTime.HOUR_PER_DAY));
                break;
            case FragmentHome.TIME_DATA_12:
                break;
            case FragmentHome.TIME_DATA_24:
                break;
            default:
                break;
        }
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
                    if ((indexLowest - lowest) < 20) {
                        adjustDateTime(-(DateTime.MILLISECOND_PER_SECOND *
                                DateTime.SECOND_PER_MINUTE *
                                DateTime.MINUTE_PER_HOUR *
                                DateTime.HOUR_PER_DAY));
                    }
//                    else if (indexHighest == DateTime.SECOND_PER_HOUR) {
//                        adjustDateTime((DateTime.MILLISECOND_PER_SECOND *
//                                DateTime.SECOND_PER_MINUTE *
//                                DateTime.MINUTE_PER_HOUR *
//                                DateTime.HOUR_PER_DAY));
//                    }
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
                    adjustDateTime((DateTime.MILLISECOND_PER_SECOND *
                            DateTime.SECOND_PER_MINUTE *
                            DateTime.MINUTE_PER_HOUR *
                            DateTime.HOUR_PER_DAY));
                } else if (me2.getX() - me1.getX() > VERTICAL_MIN_DISTANCE &&
                        Math.abs(velocityX) > MIN_VELOCITY) {
                    adjustDateTime(-(DateTime.MILLISECOND_PER_SECOND *
                            DateTime.SECOND_PER_MINUTE *
                            DateTime.MINUTE_PER_HOUR *
                            DateTime.HOUR_PER_DAY));
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

        //显示边界
        mLineChart.setDrawBorders(true);
        mLineChart.getDescription().setEnabled(false);
        mLineChart.getLegend().setEnabled(false);
        mLineChart.setScaleYEnabled(false);

        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mLineChart.setData(data);


        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setAxisMaximum(864);
//        xAxis.setSpaceMax(864f / 4f);
//        xAxis.setSpaceMin(864f / 6f);
        FormatterUtil formatterUtil = new FormatterUtil();
        xAxis.setValueFormatter(formatterUtil);

        YAxis leftYAxis = mLineChart.getAxisLeft();
        YAxis rightYAxis = mLineChart.getAxisRight();
        rightYAxis.setEnabled(false);

        leftYAxis.setTextColor(Color.WHITE);
        leftYAxis.setDrawGridLines(false);
        leftYAxis.setStartAtZero(true);
        //        leftYAxis.setAxisMaxValue(30);
        leftYAxis.setAxisMaximum(30);

        todayDateTime = new DateTime(Calendar.getInstance());
        //可以设置一条警戒线，如下：
        // .. and more styling options
        //重置所有限制线,以避免重叠线
        leftYAxis.removeAllLimitLines();
        xAxis.removeAllLimitLines();
//        leftYAxis.addLimitLine(ll);

    }

    private void addLimitLine(XAxis xAxis, String str, float x) {
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

        mLog.Debug(getClass(), "Create graph");

        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_MONITOR, this);
        newHistoryFlag = false;
        todayDateTime = new DateTime(Calendar.getInstance());
        todayDateTime.setHour(0);
        todayDateTime.setMinute(0);
        todayDateTime.setSecond(0);
        if (mHistoryModel == null) {
            mHistoryModel = new HistoryModel();
        }

        if (mDateTime == null) {
            mDateTime = new DateTime(Calendar.getInstance());
            mDateTime.setHour(0);
            mDateTime.setMinute(0);
            mDateTime.setSecond(0);
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

//        if (mHandler == null) {
//            mHandler = new Handler();
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    adjustDateTime(-(DateTime.MILLISECOND_PER_SECOND *
//                            DateTime.SECOND_PER_MINUTE *
//                            DateTime.MINUTE_PER_HOUR *
//                            DateTime.HOUR_PER_DAY));
//                }
//            }, 400);
//        }
        updateGraphProfiles();
//        updateGraphs();
    }


    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRunnable);
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_MONITOR, this);

        super.onDestroyView();
    }


    @Override
    public void onReceive(EntityMessage message) {
        switch (message.getOperation()) {
            case EntityMessage.OPERATION_SET:
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


    private void handleNotification(final EntityMessage message) {
        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
                (message.getParameter() == ParameterMonitor.PARAM_HISTORY)) {
            if (message
                    .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_MODEL) {
                mLog.Debug(getClass(), "Receive history");

                mIsHistoryQuerying = false;
                mHistoryModel.setList(new DataList(message.getData()));
                mHistoryModel.update();

                if (mHistoryModel.getCount() > 0) {
                    mDateTime = new DateTime(mHistoryModel.getHistory(0)
                            .getDateTime().getByteArray());
                    mDateTime.setHour(0);
                    mDateTime.setMinute(0);
                    mDateTime.setSecond(0);
                }
                updateGraphProfiles();
//                updateGraphs();
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
                // if (mHistoryModel.getCount() <= 0)
                {
                    // initializeGraph();
                    mDateTime = new DateTime(Calendar.getInstance());
                    mDateTime.setHour(0);
                    mDateTime.setMinute(0);
                    mDateTime.setSecond(0);
                    queryHistory(mDateTime);
                }
            }
        }
    }


    private void queryHistory(final DateTime dateTime) {
        if (mIsHistoryQuerying) {
            return;
        }

        mLog.Debug(getClass(), "Query history");

        mIsHistoryQuerying = true;
        History history = new History(new DateTime(dateTime.getByteArray()),
                new Status(-1, -1, -1, -1), new Event(-1, -1, -1, -1, -1));
        DataList dataList = new DataList();
        dataList.pushData(history.getByteArray());
        Calendar calendar = dateTime.getCalendar();
        calendar.setTimeInMillis(dateTime.getCalendar().getTimeInMillis() +
                (long) (DateTime.MILLISECOND_PER_SECOND *
                        DateTime.SECOND_PER_MINUTE * DateTime.MINUTE_PER_HOUR *
                        DateTime.HOUR_PER_DAY));
        history.setDateTime(new DateTime(calendar));
        dataList.pushData(history.getByteArray());
        ((ActivityPDA) getActivity()).handleMessage(new EntityMessage(
                ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_LOCAL_MODEL, ParameterGlobal.PORT_MONITOR,
                ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_GET,
                ParameterMonitor.PARAM_HISTORY, dataList.getByteArray()));
    }

    private void updateGraphProfiles() {
//        mLineChart.clearValues();
        todayDateTime = new DateTime(Calendar.getInstance());
        todayDateTime.setHour(0);
        todayDateTime.setMinute(0);
        todayDateTime.setSecond(0);
        int dataDay = mDateTime.getDay();
        int today = todayDateTime.getDay();
        nowStr = ((ActivityPDA) getActivity()).getDateString(todayDateTime.getCalendar().getTimeInMillis(), null);
        //可以设置一条警戒线，如下：
        long time0 = (FormatterUtil.timeStrToSecond(FormatterUtil.dateToString(mDateTime)) - FormatterUtil.timeStrToSecond(FormatterUtil.dateToString(todayDateTime))) / 1000;
        if (dataDay == today) {
            mLineChart.clearValues();
            mLineChart.getXAxis().removeAllLimitLines();
            addLimitLine(mLineChart.getXAxis(), nowStr, 0);
            if (mHistoryModel.getCount() > 0) {
                for (int i = mHistoryModel.getCount() - 1; i >= 0; i--) {
                    History history = mHistoryModel.getHistory(i);
                    Status status = history.getStatus();
                    DateTime dateTime = history.getDateTime();
                    int type = status.getByteValue1();
                    int value;
                    switch (type) {
                        case HistoryView.TYPE_GLUCOSE:
                            value = status.getShortValue1() & 0xFFFF;
                            value /= 10;
                            long time = (FormatterUtil.timeStrToSecond(FormatterUtil.dateToString(dateTime)) - FormatterUtil.timeStrToSecond(FormatterUtil.dateToString(todayDateTime))) / 1000;
                            addEntry(time / 100, value);
                            break;
                    }
                }
            } else {
                addEntry(time0 / 100, 0f);
            }
        } else {
            String string = ((ActivityPDA) getActivity()).getDateString(mDateTime.getCalendar().getTimeInMillis(), null);
            addLimitLine(mLineChart.getXAxis(), string, time0 / 100);
            if (mHistoryModel.getCount() > 0) {
                for (int i = 0; i < mHistoryModel.getCount(); i++) {
                    History history = mHistoryModel.getHistory(i);
                    Status status = history.getStatus();
                    DateTime dateTime = history.getDateTime();
                    int type = status.getByteValue1();
                    int value;
                    switch (type) {
                        case HistoryView.TYPE_GLUCOSE:
                            value = status.getShortValue1() & 0xFFFF;
                            value /= 10;
                            long time = (FormatterUtil.timeStrToSecond(FormatterUtil.dateToString(dateTime)) - FormatterUtil.timeStrToSecond(FormatterUtil.dateToString(todayDateTime))) / 1000;
//                                float time = dateTime.getBCD() - todayDateTime.getBCD();
                            addEntry(time / 100, value);
                            break;
                    }
                }
            } else {
                long time24 = time0 + 23 * 3600;
//                    float time12 = mDateTime.getBCD() - todayDateTime.getBCD() + 120000;
//                    float time6 = mDateTime.getBCD() - todayDateTime.getBCD() + 60000;
//                    float time0 = mDateTime.getBCD() - todayDateTime.getBCD();
                addEntry(time24 / 100, 0);
//                    addEntry(time6 / 100, 0);
                addEntry(time0 / 100, 0);
            }
        }
        addRangLine(time0 / 100);
        text_view_time.setText(((ActivityPDA) getActivity())
                .getDateString(mDateTime.getCalendar().getTimeInMillis(), null));
    }

    private DateTime getZeroDateTime(DateTime d) {
        DateTime dt = new DateTime();
        dt.setYear(d.getYear());
        dt.setMonth(d.getMonth());
        dt.setDay(d.getDay());
        dt.setHour(0);
        dt.setMinute(0);
        dt.setMinute(0);
        return dt;
    }


    private void addRangLine(float minIndex) {
        mHyper = ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettings.class.getSimpleName())
                .getInt(FragmentSettings.SETTING_HYPER, 0);
        mHypo = ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettings.class.getSimpleName())
                .getInt(FragmentSettings.SETTING_HYPO, 0);

        ArrayList<Entry> yVals1 = new ArrayList<Entry>();
        yVals1.add(new Entry(minIndex, mHypo / 100));
        yVals1.add(new Entry(864f, mHypo / 100));
        ArrayList<Entry> yVals2 = new ArrayList<Entry>();
        yVals2.add(new Entry(minIndex, mHyper / 100));
        yVals2.add(new Entry(864f, mHyper / 100));
        LineDataSet set1, set2;
        set1 = new LineDataSet(yVals1, null);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        set1.setLineWidth(0.5f);
        set1.setDrawCircles(false);
        set1.setColor(Color.RED);
        set1.setDrawFilled(true);
//        set1.setFillAlpha(210);
        set1.setDrawValues(false);
        set1.setFillColor(Color.BLACK);

        // create a dataset and give it a type
        set2 = new LineDataSet(yVals2, null);
        set2.setAxisDependency(YAxis.AxisDependency.LEFT);
        set2.setLineWidth(0.5f);
        set2.setDrawCircles(false);
        set2.setColor(Color.RED);
        set2.setDrawFilled(true);
        set2.setDrawValues(false);
        set2.setFillColor(Color.LTGRAY);

        set1.setHighlightEnabled(false);
        set2.setHighlightEnabled(false);
        LineData data = mLineChart.getData();
        int count = data.getDataSetCount();
        if (count > 2) {
            data.removeDataSet(2);
            data.removeDataSet(1);
        }
        data.addDataSet(set2);
        data.addDataSet(set1);
        // set data
        mLineChart.setData(data);
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

            // limit the number of visible entries
            mLineChart.setVisibleXRangeMaximum(1000);
//            mLineChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            mLineChart.moveViewToX(mLineChart.getXChartMax());

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
//        set.setValueTextColor(Color.WHITE);
        return set;
    }
}
