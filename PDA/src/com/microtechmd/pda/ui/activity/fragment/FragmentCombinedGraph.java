package com.microtechmd.pda.ui.activity.fragment;


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.microtechmd.pda.ui.activity.ActivityPDA.CALIBRATION_HISTORY;
import static com.microtechmd.pda.ui.activity.ActivityPDA.GLUCOSE;
import static com.microtechmd.pda.ui.activity.ActivityPDA.GLUCOSE_RECOMMEND_CAL;
import static com.microtechmd.pda.ui.activity.ActivityPDA.HYPER;
import static com.microtechmd.pda.ui.activity.ActivityPDA.HYPO;
import static com.microtechmd.pda.ui.activity.ActivityPDA.IMPENDANCE;
import static com.microtechmd.pda.ui.activity.ActivityPDA.RFSIGNAL;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettings.HYPER_DEFAULT;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettings.HYPO_DEFAULT;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettings.REALTIMEFLAG;

public class FragmentCombinedGraph extends FragmentBase
        implements
        EntityMessage.Listener {
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
    private Handler mHandler = null;
    private Runnable mRunnable = null;

    private View mRootView = null;
    private CombinedChart mChart;
    private TextView synchronize;

    private XAxis xAxis;
    private String str;
    private long maxTime;

    private ArrayList<History> dataListAll = null;

    public void setTimeData(int timeData) {
        mDateTime = new DateTime(Calendar.getInstance());
        switch (timeData) {
            case FragmentHome.TIME_DATA_6:
                visible_range_millisecond = MILLISECOND_6;
                break;
            case FragmentHome.TIME_DATA_12:
                visible_range_millisecond = MILLISECOND_12;
                break;
            case FragmentHome.TIME_DATA_24:
                visible_range_millisecond = MILLISECOND_24;
                break;
            default:
                break;
        }
        mChart.setVisibleXRange(visible_range_millisecond / 100000, visible_range_millisecond / 100000);
        if ((mChart.getScatterData().getXMax() - visible_range_millisecond / 100000 * 0.8) > mChart.getLineData().getXMin()) {
            mChart.moveViewToX((float) (mChart.getScatterData().getXMax() - visible_range_millisecond / 100000 * 0.8));
        } else {
            mChart.moveViewToX(mChart.getLineData().getXMin());
        }
        mChart.invalidate();
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_combinedgraph, container, false);
        now_millisecond = MILLISECOND_24;
        visible_range_millisecond = MILLISECOND_6;
        if (dataListAll == null) {
            dataListAll = new ArrayList<>();
        }
        if (mDateTime == null) {
            mDateTime = new DateTime(Calendar.getInstance());
        }
        TextView text_view_unit = (TextView) mRootView.findViewById(R.id.text_view_unit);
        text_view_unit.setText(getResources().getString(R.string.history_bg) +
                " (" + getResources().getString(R.string.unit_mmol_l) + ")");
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

        initChart();
        return mRootView;
    }

    private void initChart() {
        mChart = (CombinedChart) mRootView.findViewById(R.id.chart);
//        mLineChart.setGridBackgroundColor(Color.parseColor("#E5575A68"));
//        mLineChart.setDrawGridBackground(true);
        //显示边界
        mChart.setDrawBorders(true);
        mChart.setBorderColor(Color.GRAY);
        mChart.getDescription().setEnabled(false);
        mChart.getLegend().setEnabled(false);
        mChart.setScaleXEnabled(false);
        mChart.setScaleYEnabled(false);
        mChart.setDoubleTapToZoomEnabled(false);
        mChart.setDragDecelerationEnabled(false);
// draw LINE behind SCATTER
        mChart.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.LINE, CombinedChart.DrawOrder.SCATTER
        });

        xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setAvoidFirstLastClipping(true);
        FormatterUtil formatterUtil = new FormatterUtil();
        xAxis.setValueFormatter(formatterUtil);
        xAxis.setLabelCount(7);
        xAxis.setGranularity(36);
        xAxis.removeAllLimitLines();

        YAxis leftYAxis = mChart.getAxisLeft();
        YAxis rightYAxis = mChart.getAxisRight();
        rightYAxis.setEnabled(false);
        leftYAxis.setTextColor(Color.WHITE);
        leftYAxis.setDrawGridLines(false);
        leftYAxis.setAxisMinimum(0);
        leftYAxis.setAxisMaximum(30);
        leftYAxis.removeAllLimitLines();

        CombinedData data = new CombinedData();
        data.setData(new ScatterData());
        data.setData(new LineData());

        mChart.setData(data);
        mChart.invalidate();
    }

    private ScatterData generateScatterData() {
        DateTime todayDateTime = getTodayDateTime();
        long todayMills = todayDateTime.getCalendar().getTimeInMillis();
        long maxIndex = new DateTime(Calendar.getInstance()).getCalendar().getTimeInMillis()
                - todayMills;
        long minIndex = maxIndex - now_millisecond;
        ScatterData d = new ScatterData();
        List<Entry> entries = new ArrayList<>();
        if (dataListAll.size() > 0) {
            for (History history : dataListAll) {
                if (history.getEvent().getEvent() == GLUCOSE ||
                        history.getEvent().getEvent() == GLUCOSE_RECOMMEND_CAL ||
                        history.getEvent().getEvent() == HYPO ||
                        history.getEvent().getEvent() == HYPER ||
                        history.getEvent().getEvent() == IMPENDANCE) {
                    Status status = history.getStatus();
                    int value = status.getShortValue1() & 0xFFFF;
                    value /= 100;
                    long time = (history.getDateTime().getCalendar().getTimeInMillis() - todayMills) / 1000;
                    entries.add(new Entry(time / 100, value));
                }
            }
        } else {
            entries.add(new Entry((minIndex) / 100000, 0));
            entries.add(new Entry((maxIndex) / 100000, 0));
        }
        ScatterDataSet set = new ScatterDataSet(entries, null);
        set.setColor(Color.parseColor("#00DEFF"));
        set.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set.setScatterShapeSize(2.5f);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawIcons(true);
        d.addDataSet(set);
        return d;
    }

    private LineData generateLineData() {
        int mHyper = ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettings.class.getSimpleName())
                .getInt(FragmentSettings.SETTING_HYPER, HYPER_DEFAULT);
        int mHypo = ((ActivityPDA) getActivity())
                .getDataStorage(FragmentSettings.class.getSimpleName())
                .getInt(FragmentSettings.SETTING_HYPO, HYPO_DEFAULT);
        DateTime todayDateTime = getTodayDateTime();

        long maxIndex = new DateTime(Calendar.getInstance()).getCalendar().getTimeInMillis()
                - todayDateTime.getCalendar().getTimeInMillis() + visible_range_millisecond / 7;
        long minIndex = mDateTime.getCalendar().getTimeInMillis()
                - todayDateTime.getCalendar().getTimeInMillis()
                - now_millisecond;

        if (dataListAll.size() > 0) {
//            maxIndex = (long) (mChart.getData().getScatterData().getXMax() * 100000 + visible_range_millisecond / 7);
            if ((maxIndex / 100000 - mChart.getData().getScatterData().getXMin()) > MILLISECOND_24 / 100000) {
                minIndex = (long) (mChart.getData().getScatterData().getXMin() * 100000);
            }
        }

        LineData lineData = new LineData();

        List<Entry> yVals1 = new ArrayList<>();
        yVals1.add(new Entry(minIndex / 100000, mHypo / 100));
        yVals1.add(new Entry(maxIndex / 100000, mHypo / 100));

        List<Entry> yVals2 = new ArrayList<>();
        yVals2.add(new Entry(minIndex / 100000, mHyper / 100));
        yVals2.add(new Entry(maxIndex / 100000, mHyper / 100));

        LineDataSet set1, set2;
        set1 = new LineDataSet(yVals1, null);
        setLineDataSet(set1, Color.BLACK);

        set2 = new LineDataSet(yVals2, null);
        setLineDataSet(set2, Color.LTGRAY);

        lineData.addDataSet(set2);
        lineData.addDataSet(set1);

        return lineData;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_MONITOR, this);
        now_millisecond = MILLISECOND_24;
        visible_range_millisecond = MILLISECOND_6;

        if (dataListAll == null) {
            dataListAll = new ArrayList<>();
        }

        if (mDateTime == null) {
            mDateTime = new DateTime(Calendar.getInstance());
        }

        if (dataListAll.size() <= 0) {
            mHandler = new Handler();
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    queryHistory();
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
                dataListAll.clear();
                updateGraphProfiles();
            } else {
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
//                mHistoryModel.setList(new DataList(message.getData()));
//                mHistoryModel.update();
                DataList dataList = new DataList(message.getData());
                for (int i = 0; i < dataList.getCount(); i++) {
                    dataListAll.add(new History(dataList.getData(i)));

                }
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
//                mDateTime = new DateTime(Calendar.getInstance());
//                queryHistory(mDateTime);
                DataList dataList = new DataList(message.getData());
                for (int i = 0; i < dataList.getCount(); i++) {
                    dataListAll.add(new History(dataList.getData(i)));

                }
                updateGraphProfiles();
            }
        }

    }

    private void showDialogProgress() {
        ((ActivityPDA) getActivity()).showDialogLoading();
    }


    private void dismissDialogProgress() {
        ((ActivityPDA) getActivity()).dismissDialogLoading();
    }

    private void queryHistory() {
        if (mIsHistoryQuerying) {
            return;
        }
        mLog.Debug(getClass(), "Query history");
        showDialogProgress();
        mIsHistoryQuerying = true;
        DataList dataList = new DataList();
//        History history = new History(new DateTime(),
//                new Status(-1), new Event(-1, -1, -1));
//        Calendar calendar = dateTime.getCalendar();
//        calendar.setTimeInMillis(dateTime.getCalendar().getTimeInMillis() - now_millisecond);
//        history.setDateTime(new DateTime(calendar));
//        dataList.pushData(history.getByteArray());
//        history.setDateTime(new DateTime(dateTime.getByteArray()));
//        dataList.pushData(history.getByteArray());
        ((ActivityPDA) getActivity()).handleMessage(new EntityMessage(
                ParameterGlobal.ADDRESS_LOCAL_VIEW,
                ParameterGlobal.ADDRESS_LOCAL_MODEL, ParameterGlobal.PORT_MONITOR,
                ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_GET,
                ParameterMonitor.PARAM_HISTORY, dataList.getByteArray()));
    }

    private void updateGraphProfiles() {

        mChart.getData().setData(generateScatterData());
        mChart.getData().setData(generateLineData());
        mChart.notifyDataSetChanged();
        mChart.setVisibleXRange(visible_range_millisecond / 100000, visible_range_millisecond / 100000);
        if ((mChart.getScatterData().getXMax() - visible_range_millisecond / 100000 * 0.8) > mChart.getLineData().getXMin()) {
            mChart.moveViewToX((float) (mChart.getScatterData().getXMax() - visible_range_millisecond / 100000 * 0.8));
        } else {
            mChart.moveViewToX(mChart.getLineData().getXMin());
        }
        mChart.invalidate();

        float maxIndex = mChart.getScatterData().getXMax();
        float maxValue = mChart.getScatterData().getDataSetByIndex(0).getEntryForXValue(maxIndex, 0).getY();
        addValueTextEntry(maxIndex, maxValue);
        addCalibrationEntry();
        xAxis.removeAllLimitLines();
        addTimeLine();

        dismissDialogProgress();
    }

    @NonNull
    private DateTime getTodayDateTime() {
        DateTime todayDateTime = new DateTime(Calendar.getInstance());
        todayDateTime.setHour(0);
        todayDateTime.setMinute(0);
        todayDateTime.setSecond(0);
        return todayDateTime;
    }

    private void addValueTextEntry(float t, float value) {
        ArrayList<Entry> yVals = new ArrayList<>();
        yVals.add(new Entry(t, value));
        ScatterDataSet set;
        set = new ScatterDataSet(yVals, null);
        setValueTextLineDataSet(set);
        ScatterData data = mChart.getData().getScatterData();
        data.addDataSet(set);
        // set data
        mChart.getData().setData(data);
    }

    private void addCalibrationEntry() {
        List<CalibrationHistory> list = (List<CalibrationHistory>) CalibrationSaveUtil.get(getActivity(), CALIBRATION_HISTORY);
        long todayMills = getTodayDateTime().getCalendar().getTimeInMillis();
        ArrayList<Entry> yVals = new ArrayList<>();

        if (list != null) {
            for (CalibrationHistory calibrationHistory : list) {
                yVals.add(new Entry((calibrationHistory.getTime() - todayMills) / 100000, calibrationHistory.getValue()));
            }
        }
        if (yVals.size() > 0) {
            ScatterDataSet set;
            set = new ScatterDataSet(yVals, null);
            setCalibrationLineDataSet(set);
            ScatterData data = mChart.getData().getScatterData();
            data.addDataSet(set);
            // set data
            mChart.getData().setData(data);
        }
    }

    private void addTimeLine() {
        for (int i = 0; -MILLISECOND_24 * i / 100000 > mChart.getLineData().getXMin(); i++) {
            long time = getTodayDateTime().getCalendar().getTimeInMillis() - MILLISECOND_24 * i;
            String nowStr = ((ActivityPDA) getActivity()).getDateString(time, null);
            addLimitLine(nowStr, -MILLISECOND_24 * i / 100000);
        }
    }

    private boolean rangeInDefined(long current, long min, long max) {
        return Math.max(min, current) == Math.min(current, max);
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


    private void setValueTextLineDataSet(ScatterDataSet set) {
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.GREEN);
        set.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set.setScatterShapeSize(8f);
        set.setHighlightEnabled(false);
        set.setDrawValues(true);
        set.setValueTextColor(Color.WHITE);
        set.setDrawIcons(true);
        set.setHighlightEnabled(false);
    }

    private void setCalibrationLineDataSet(ScatterDataSet set) {
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.RED);
        set.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set.setScatterShapeSize(10f);
        set.setHighlightEnabled(false);
        set.setDrawValues(true);
        set.setValueTextColor(Color.WHITE);
        set.setDrawIcons(true);
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


//    private void adjustDateTime(long offset) {
//        Date date = new Date();
//        date.setTime(mDateTime.getCalendar().getTimeInMillis());
//        long time = mDateTime.getCalendar().getTimeInMillis() + offset;
//        if (time <= System.currentTimeMillis()) {
//            Calendar calendar = mDateTime.getCalendar();
//            calendar.setTimeInMillis(time);
//            mDateTime.setCalendar(calendar);
//            queryHistory(mDateTime);
//        }
//    }

    private void addEntry(float time, float value) {

        ScatterData data = mChart.getData().getScatterData();

        if (data != null) {

            IScatterDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

//            data.addEntry(new Entry(time, value / 10), 0);
            set.addEntryOrdered(new Entry(time, value / 10));
            data.notifyDataChanged();


            // let the chart know it's data has changed
//            mChart.notifyDataSetChanged();

//            mLineChart.setVisibleXRangeMaximum(now_millisecond / 100000);

            // limit the number of visible entries
//            mLineChart.setVisibleXRangeMaximum(8);
//            mLineChart.setVisibleYRange(30, AxisDependency.LEFT);
            // move to the latest entry
//            mLineChart.moveViewToX(value);
            mChart.fitScreen();
            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);

        }
    }

    private ScatterDataSet createSet() {

        ScatterDataSet set = new ScatterDataSet(null, null);
        set.setColor(Color.parseColor("#00DEFF"));
        set.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set.setScatterShapeSize(5f);
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
