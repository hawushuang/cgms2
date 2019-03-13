package com.microtechmd.pda.ui.activity.fragment;


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.util.CalibrationSaveUtil;
import com.microtechmd.pda.util.FormatterUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.microtechmd.pda.ui.activity.ActivityPDA.CALIBRATION_HISTORY;
import static com.microtechmd.pda.ui.activity.ActivityPDA.GLUCOSE;
import static com.microtechmd.pda.ui.activity.ActivityPDA.GLUCOSE_RECOMMEND_CAL;
import static com.microtechmd.pda.ui.activity.fragment.FragmentCombinedGraph.oneDay_lenth;
import static com.microtechmd.pda.ui.activity.fragment.FragmentCombinedGraph.pointSpace;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingTips.HYPER_DEFAULT;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingTips.HYPO_DEFAULT;

public class FragmentLockCombinedGraph extends FragmentBase
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
    private ImageView synchronize;
    private TextView textview_synchronizing;

    private XAxis xAxis;
    private String str;
    private long maxTime;

    private List<History> dataListAll = null;
    List<History> dataListCash = new ArrayList<>();

    private boolean isTop;


    private ApplicationPDA applicationPDA;
    private long todayMilisecond;
    private CountDownTimer refreshGraphCountdownTimer;

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_combinedgraph, container, false);
        applicationPDA = (ApplicationPDA) getActivity().getApplication();
        now_millisecond = MILLISECOND_24;
        visible_range_millisecond = MILLISECOND_6;
        if (mDateTime == null) {
            mDateTime = new DateTime(Calendar.getInstance());
        }
        TextView text_view_unit = (TextView) mRootView.findViewById(R.id.text_view_unit);
        text_view_unit.setText(getResources().getString(R.string.history_bg) +
                " (" + getResources().getString(R.string.unit_mmol_l) + ")");
        synchronize = (ImageView) mRootView.findViewById(R.id.synchronize);
        textview_synchronizing = (TextView) mRootView.findViewById(R.id.text_view_synchronizing);
        textview_synchronizing.setVisibility(View.GONE);
        synchronize.setVisibility(View.GONE);
        initChart();

        if (applicationPDA != null) {
            dataListAll = applicationPDA.getDataListAll();
        }
        if (dataListAll == null) {
            dataListAll = new ArrayList<>();
        }
        if (dataListAll.size() > 240) {
            int indexFirst = 0;
            int indexLast = 0;
            for (int i = dataListAll.size() - 1; i >= 0; i--) {
                History history = dataListAll.get(i);
                if (history.getEvent().getEvent() == GLUCOSE ||
                        history.getEvent().getEvent() == GLUCOSE_RECOMMEND_CAL) {
                    indexLast = i;
                    break;
                }
            }
            if (indexLast > 240) {
                indexFirst = indexLast - 240;
            }
            drawScatterData(dataListAll.subList(indexFirst, indexLast));
            mChart.notifyDataSetChanged();
        } else {
            drawScatterData(dataListAll);
            mChart.notifyDataSetChanged();
        }

//        updateGraphProfiles();
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
        mChart.setDragXEnabled(false);
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
        xAxis.setGranularity(visible_range_millisecond / 6 / 1000 / pointSpace);
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

    private void drawScatterData(List<History> dataListCash) {
        DateTime todayDateTime = getTodayDateTime();
        long todayMills = todayDateTime.getCalendar().getTimeInMillis();
        if (todayMills != todayMilisecond) {
            updateGraphProfiles();
            return;
        }
        ScatterData d = mChart.getScatterData();
        IScatterDataSet set = d.getDataSetByIndex(0);
        if (set == null) {
            set = createSet();
            d.addDataSet(set);
        }
        if (set.getEntryCount() == 1) {
            set.removeEntry(0);
        }
        if (dataListCash.size() > 0) {
            for (History history : dataListCash) {
                if (history.getEvent().getEvent() == GLUCOSE ||
                        history.getEvent().getEvent() == GLUCOSE_RECOMMEND_CAL) {
                    if (history.getDateTime().getCalendar().getTimeInMillis() < (System.currentTimeMillis() + 60 * 1000)) {
                        Status status = history.getStatus();
                        int value = status.getShortValue1() & 0xFFFF;
                        float value_display = Float.parseFloat(((ActivityPDA) getActivity())
                                .getGlucoseValue((value) *
                                        ActivityPDA.GLUCOSE_UNIT_MG_STEP, false).trim());
                        long time = (history.getDateTime().getCalendar().getTimeInMillis() - todayMilisecond) / 1000;
                        set.addEntryOrdered(new Entry(time / pointSpace, value_display));
                    }
                }
            }
        }
        if (set.getEntryCount() == 0) {
            set.addEntryOrdered(new Entry(0, -1));
        }
        d.notifyDataChanged();
        drawOthers();
    }

    private ScatterData generateScatterData() {
        ScatterData d = new ScatterData();
        DateTime todayDateTime = getTodayDateTime();
        long todayMills = todayDateTime.getCalendar().getTimeInMillis();
        long maxIndex = new DateTime(Calendar.getInstance()).getCalendar().getTimeInMillis()
                - todayMills;
        List<Entry> entries = new ArrayList<>();
        if (dataListAll == null) {
            dataListAll = new ArrayList<>();
        }
        if (dataListAll.size() > 0) {
            int indexFirst = 0;
            int indexLast = 0;
            for (int i = dataListAll.size() - 1; i >= 0; i--) {
                History history = dataListAll.get(i);
                if (history.getEvent().getEvent() == GLUCOSE ||
                        history.getEvent().getEvent() == GLUCOSE_RECOMMEND_CAL) {
                    indexLast = i;
                    break;
                }
            }
            if (indexLast > 240) {
                indexFirst = indexLast - 240;
            }
            for (int i = indexFirst; i <= indexLast; i++) {
                History history = dataListAll.get(i);
                if (history.getEvent().getEvent() == GLUCOSE ||
                        history.getEvent().getEvent() == GLUCOSE_RECOMMEND_CAL) {
                    if (history.getDateTime().getCalendar().getTimeInMillis() < System.currentTimeMillis()) {
                        Status status = history.getStatus();
                        int value = status.getShortValue1() & 0xFFFF;
                        float value_display = Float.parseFloat(((ActivityPDA) getActivity())
                                .getGlucoseValue((value) *
                                        ActivityPDA.GLUCOSE_UNIT_MG_STEP, false).trim());
                        long time = (history.getDateTime().getCalendar().getTimeInMillis() - todayMills) / 1000;
                        entries.add(new Entry(time / pointSpace, value_display));
                    }
                }
            }
//            for (History history : dataListAll) {
//                if (history.getEvent().getEvent() == GLUCOSE ||
//                        history.getEvent().getEvent() == GLUCOSE_RECOMMEND_CAL) {
//                    if (history.getDateTime().getCalendar().getTimeInMillis() < System.currentTimeMillis()) {
//                        Status status = history.getStatus();
//                        int value = status.getShortValue1() & 0xFFFF;
//                        float value_display = Float.parseFloat(((ActivityPDA) getActivity())
//                                .getGlucoseValue((value) *
//                                        ActivityPDA.GLUCOSE_UNIT_MG_STEP, false).trim());
//                        long time = (history.getDateTime().getCalendar().getTimeInMillis() - todayMills) / 1000;
//                        entries.add(new Entry(time / 100, value_display));
//                    }
//                }
//            }
        } else {
            entries.add(new Entry(0, -1));
        }
        if (entries.size() == 0) {
            entries.add(new Entry(0, -1));
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
                .getDataStorage(ActivityPDA.class.getSimpleName())
                .getInt(FragmentSettings.SETTING_HYPER, HYPER_DEFAULT);
        int mHypo = ((ActivityPDA) getActivity())
                .getDataStorage(ActivityPDA.class.getSimpleName())
                .getInt(FragmentSettings.SETTING_HYPO, HYPO_DEFAULT);
        DateTime todayDateTime = getTodayDateTime();


//        DateTime dateNow = new DateTime(Calendar.getInstance());
//        Calendar calendar = dateNow.getCalendar();
//        calendar.setTimeInMillis(dateNow.getCalendar().getTimeInMillis() +
//                60 * 60 * 1000);
//        DateTime dateNextHour = new DateTime(calendar);
//        dateNextHour.setMinute(0);
//        dateNextHour.setSecond(0);
//        long maxIndex = dateNextHour.getCalendar().getTimeInMillis()
//                - todayDateTime.getCalendar().getTimeInMillis();
//
//        long minIndex = maxIndex - now_millisecond;
        long maxIndex = new DateTime(Calendar.getInstance()).getCalendar().getTimeInMillis()
                - todayDateTime.getCalendar().getTimeInMillis() + visible_range_millisecond / 7;

        long minIndex = new DateTime(Calendar.getInstance()).getCalendar().getTimeInMillis()
                - todayDateTime.getCalendar().getTimeInMillis()
                - now_millisecond;

        if (dataListAll.size() > 0) {
//            maxIndex = (long) (mChart.getData().getScatterData().getXMax() * 100000 + visible_range_millisecond / 7);
//            for (History history : dataListAll) {
//                if (history.getDateTime().getCalendar().getTimeInMillis() > maxIndex) {
//                    maxIndex = history.getDateTime().getCalendar().getTimeInMillis();
//                    break;
//                }
//            }
            if ((maxIndex / 1000 / pointSpace - mChart.getData().getScatterData().getXMin()) > MILLISECOND_24 / 1000 / pointSpace) {
                minIndex = (long) (mChart.getData().getScatterData().getXMin() * 1000 * pointSpace);
            }
        }

        LineData lineData = new LineData();

        List<Entry> yVals1 = new ArrayList<>();
        yVals1.add(new Entry(minIndex / 1000 / pointSpace, mHypo / 10));
        yVals1.add(new Entry(maxIndex / 1000 / pointSpace, mHypo / 10));

        List<Entry> yVals2 = new ArrayList<>();
        yVals2.add(new Entry(minIndex / 1000 / pointSpace, mHyper / 10));
        yVals2.add(new Entry(maxIndex / 1000 / pointSpace, mHyper / 10));

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

        if (mDateTime == null) {
            mDateTime = new DateTime(Calendar.getInstance());
        }
        if (dataListAll == null) {
            dataListAll = new ArrayList<>();
        }
        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                queryHistory();
            }
        };
        if (dataListAll.size() <= 0) {
            mHandler.postDelayed(mRunnable, 100);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isTop = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        isTop = false;
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
        if (message.getParameter() == ParameterGlucose.PARAM_FILL_LIMIT
                || message.getParameter() == ParameterGlucose.PARAM_BG_LIMIT) {
            mChart.getData().setData(generateLineData());
            mChart.notifyDataSetChanged();
        }
        if (message.getParameter() == ParameterComm.RESET_DATA) {
            if (message.getData()[0] == 0) {
                dataListAll.clear();
            }
            updateGraphProfiles();
        }
    }


    private void handleNotification(final EntityMessage message) {
        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
                (message.getParameter() == ParameterMonitor.PARAM_HISTORY)) {
            if (message
                    .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_MODEL) {
                mLog.Debug(getClass(), "Receive history");
                mIsHistoryQuerying = false;
                dismissDialogProgress();
//                mHistoryModel.setList(new DataList(message.getData()));
//                mHistoryModel.update();
                if (isTop) {
                    DataList dataList = new DataList(message.getData());
                    for (int i = 0; i < dataList.getCount(); i++) {
                        dataListAll.add(new History(dataList.getData(i)));

                    }
                    if (applicationPDA != null) {
                        applicationPDA.setDataListAll(dataListAll);
                    }

                    drawScatterData(dataListAll);
                    mChart.notifyDataSetChanged();
                }
            }
        }

        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
                (message.getParameter() == ParameterMonitor.PARAM_HISTORY)) {
            if ((message
                    .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_CONTROL) ||
                    (message
                            .getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER)) {
//                mDateTime = new DateTime(Calendar.getInstance());
//                queryHistory(mDateTime);
                DataList dataList = new DataList(message.getData());
                for (int i = 0; i < dataList.getCount(); i++) {
                    History history = new History(dataList.getData(i));
                    switch (history.getStatus().getShortValue1()) {
                        case 0:
                            history.getStatus().setShortValue1(20);
                            break;
                        case 255:
                            history.getStatus().setShortValue1(250);
                            break;
                        default:
                            break;
                    }
                    dataListAll.add(history);
                    dataListCash.add(history);
                }
                if (dataListCash.size() < 3 || dataListCash.size() > 200) {
                    drawScatterData(dataListCash);
                    mChart.notifyDataSetChanged();
                    dataListCash.clear();
                }

                if (refreshGraphCountdownTimer != null) {
                    refreshGraphCountdownTimer.cancel();
                }
                refreshGraphCountdownTimer = new CountDownTimer(3000, 3000) {
                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        if (dataListCash.size() > 0) {
                            drawScatterData(dataListCash);
                            mChart.notifyDataSetChanged();
                            dataListCash.clear();
                        }
                    }
                }.start();
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
        drawOthers();
    }

    private void drawOthers() {
        mChart.getData().setData(generateLineData());
        mChart.notifyDataSetChanged();

        if (mChart.getScatterData().getDataSetCount() > 2) {
            mChart.getScatterData().removeDataSet(2);
            mChart.getScatterData().removeDataSet(1);
        } else if (mChart.getScatterData().getDataSetCount() > 1) {
            mChart.getScatterData().removeDataSet(1);
        }

        float maxIndex = mChart.getScatterData().getDataSetByIndex(0).getXMax();
        int count = mChart.getScatterData().getDataSetByIndex(0).getEntryCount();
        if (count > 0) {
            if (maxIndex == 0) {
                maxIndex = mChart.getScatterData().getDataSetByIndex(0).getEntryForIndex(count - 1).getX();
            }
        }
        if (mChart.getScatterData().getDataSetByIndex(0).getEntryForXValue(maxIndex, 0) != null) {
            float maxValue = mChart.getScatterData().getDataSetByIndex(0).getEntryForXValue(maxIndex, 0).getY();
            addValueTextEntry(maxIndex, maxValue);
        }
        addCalibrationEntry();
        todayMilisecond = getTodayDateTime().getCalendar().getTimeInMillis();
        addTimeLine();
        moveCharView();
    }

    private void moveCharView() {
        float maxIndex = mChart.getScatterData().getDataSetByIndex(0).getXMax();
        int count = mChart.getScatterData().getDataSetByIndex(0).getEntryCount();
        if (count > 0) {
            if (maxIndex == 0) {
                maxIndex = mChart.getScatterData().getDataSetByIndex(0).getEntryForIndex(count - 1).getX();
            }
        }
        mChart.setVisibleXRange(visible_range_millisecond / 1000 / pointSpace, visible_range_millisecond / 1000 / pointSpace);
        if ((maxIndex - visible_range_millisecond / 1000 / pointSpace * 0.8) > mChart.getLineData().getXMin()) {
            mChart.moveViewToX((float) (maxIndex - visible_range_millisecond / 1000 / pointSpace * 0.8));
        } else {
            mChart.moveViewToX(mChart.getLineData().getXMin());
        }
        mChart.invalidate();

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
        float maxIndex = mChart.getScatterData().getDataSetByIndex(0).getXMax();
        float minIndex;
        if ((maxIndex - visible_range_millisecond / 1000 / pointSpace * 0.8) > mChart.getLineData().getXMin()) {
            minIndex = (float) (maxIndex - visible_range_millisecond / 1000 / pointSpace * 0.8);
        } else {
            minIndex = mChart.getLineData().getXMin();
        }
        List<CalibrationHistory> list = (List<CalibrationHistory>) CalibrationSaveUtil.get(getActivity(), CALIBRATION_HISTORY);
        long todayMills = getTodayDateTime().getCalendar().getTimeInMillis();
        ArrayList<Entry> yVals = new ArrayList<>();

        if (list != null) {
            for (CalibrationHistory calibrationHistory : list) {
                if (rangeInDefined((calibrationHistory.getTime() - todayMills) / 1000 / pointSpace, minIndex, maxIndex)) {
                    yVals.add(new Entry((calibrationHistory.getTime() - todayMills) / 1000 / pointSpace, calibrationHistory.getValue()));
                }
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
        float t;
        float maxIndex = mChart.getScatterData().getDataSetByIndex(0).getXMax();
        int count = mChart.getScatterData().getDataSetByIndex(0).getEntryCount();
        if (count > 0) {
            if (maxIndex == 0) {
                maxIndex = mChart.getScatterData().getDataSetByIndex(0).getEntryForIndex(count - 1).getX();
            }
        }
        if ((maxIndex - visible_range_millisecond / 1000 / pointSpace * 0.8) > mChart.getLineData().getXMin()) {
            t = (float) (maxIndex - visible_range_millisecond / 1000 / pointSpace * 0.8);
        } else {
            t = mChart.getLineData().getXMin();
        }
        addLine(t);
//        for (int i = 0; -MILLISECOND_24 * i / 100000 > mChart.getLineData().getXMin(); i++) {
//            long time = getTodayDateTime().getCalendar().getTimeInMillis() - MILLISECOND_24 * i;
//            String nowStr = ((ActivityPDA) getActivity()).getDateString(time, null);
//            addLimitLine(nowStr, -MILLISECOND_24 * i / 100000);
//        }
    }

    private void addLine(float indexLowest) {
        xAxis.removeAllLimitLines();
        long todayMills = todayMilisecond;
        String s = ((ActivityPDA) getActivity()).getDateString((long) (indexLowest * pointSpace * 1000 + todayMills), null);
        if (indexLowest % oneDay_lenth == 0) {
            addLimitLine(s, indexLowest);
        } else {
            addCashLimitLine(s, indexLowest);
        }

        int index0 = (int) (indexLowest / oneDay_lenth);
        int index1 = (int) ((indexLowest + visible_range_millisecond / 1000 / pointSpace) / oneDay_lenth);
        if (index0 == 0) {
            if ((indexLowest + visible_range_millisecond / 1000 / pointSpace) > 0) {
                if (indexLowest < 0) {
                    xAxis.removeAllLimitLines();
                    String s1 = ((ActivityPDA) getActivity()).getDateString((long) ((indexLowest + visible_range_millisecond / 1000 / pointSpace) * pointSpace * 1000 + todayMills), null);
                    addLimitLine(s1, index0 * oneDay_lenth);
                    if (visible_range_millisecond == MILLISECOND_24) {
                        addLimitLine(s, index0 * oneDay_lenth - visible_range_millisecond / 1000 / pointSpace);
                    } else {
                        addCashLimitLine(s, index0 * oneDay_lenth - visible_range_millisecond / 1000 / pointSpace);
                    }
                }
            }
        }
        if (index0 != index1) {
//            if (Math.abs(index0 * 864 - indexLowest) < visible_range_millisecond / 100000 / 5) {
//                xAxis.removeAllLimitLines();
//            }
            xAxis.removeAllLimitLines();
            String s1 = ((ActivityPDA) getActivity()).getDateString((long) ((indexLowest + visible_range_millisecond / 1000 / pointSpace) * pointSpace * 1000 + todayMills), null);
            addLimitLine(s1, index0 * oneDay_lenth);
            if (visible_range_millisecond == MILLISECOND_24) {
                addLimitLine(s, index0 * oneDay_lenth - visible_range_millisecond / 1000 / pointSpace);
            } else {
                addCashLimitLine(s, index0 * oneDay_lenth - visible_range_millisecond / 1000 / pointSpace);
            }
        }
    }

    private boolean rangeInDefined(float current, float min, float max) {
        return Math.max(min, current) == Math.min(current, max);
    }

    private void addCashLimitLine(String str, float x) {
        LimitLine ll = new LimitLine(x, str);
        ll.setLineColor(Color.TRANSPARENT);
        ll.setLineWidth(1f);
        ll.setTextColor(Color.WHITE);
        ll.setTextSize(12f);
        xAxis.addLimitLine(ll);
    }

    private void addLimitLine(String str, float x) {
        //可以设置一条警戒线，如下：
        LimitLine ll = new LimitLine(x, str);
        ll.setLineColor(Color.LTGRAY);
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
        set.setDrawValues(false);
//        set.setValueTextColor(Color.WHITE);
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

    private ScatterDataSet createSet() {
        ScatterDataSet set = new ScatterDataSet(null, null);
        set.setColor(Color.parseColor("#00DEFF"));
        set.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set.setScatterShapeSize(2.5f);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawIcons(true);
        return set;
    }
}
