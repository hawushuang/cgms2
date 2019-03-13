package com.microtechmd.pda.ui.activity.fragment;


import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.database.DbHistory;
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
import com.microtechmd.pda.ui.activity.ActivityFullGraph;
import com.microtechmd.pda.ui.activity.ActivityMain;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.util.CalibrationSaveUtil;
import com.microtechmd.pda.util.FormatterUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.microtechmd.pda.ui.activity.ActivityPDA.BLOOD_GLUCOSE;
import static com.microtechmd.pda.ui.activity.ActivityPDA.CALIBRATION;
import static com.microtechmd.pda.ui.activity.ActivityPDA.CALIBRATION_HISTORY;
import static com.microtechmd.pda.ui.activity.ActivityPDA.GLUCOSE;
import static com.microtechmd.pda.ui.activity.ActivityPDA.GLUCOSE_RECOMMEND_CAL;
import static com.microtechmd.pda.ui.activity.ActivityPDA.HYPER;
import static com.microtechmd.pda.ui.activity.ActivityPDA.HYPO;
import static com.microtechmd.pda.ui.activity.ActivityPDA.PDA_ERROR;
import static com.microtechmd.pda.ui.activity.ActivityPDA.RFSIGNAL;
import static com.microtechmd.pda.ui.activity.ActivityPDA.SENSOR_ERROR;
import static com.microtechmd.pda.ui.activity.ActivityPDA.SENSOR_EXPIRATION;
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
    private Handler mHandler = null;
    private Runnable mRunnable = null;

    private View mRootView = null;
    private CombinedChart mChart;
    private ImageView synchronize;
    private TextView textview_synchronizing;

    private XAxis xAxis;
    private String str;
    private long maxTime;

    public static int pointSpace = 30;
    public static int oneDay_lenth = 86400 / pointSpace;
    private List<History> dataListAll = null;
    private ArrayList<DbHistory> dataErrListAll = null;
    private List<History> dataListCash = new ArrayList<>();

    private boolean isTop;

    private boolean dateChange;


    private long todayMilisecond;

    private ApplicationPDA applicationPDA;
    private PowerManager powerManager = null;
    private PowerManager.WakeLock wakeLock = null;

    private CountDownTimer refreshGraphCountdownTimer;

    public void setTimeData(int timeData) {
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
        xAxis.setGranularity(visible_range_millisecond / 6 / 1000 / pointSpace);
        addTimeLine();
        moveCharView();
    }

    public void setDataChange() {
        if (dateChange) {
            mChart.getScatterData().clearValues();
            mChart.invalidate();
            drawScatterData(dataListAll);
            mChart.notifyDataSetChanged();
            dateChange = false;
        }
    }

    int index = 0;
    long bcd = getTodayDateTime().getCalendar().getTimeInMillis();

    @SuppressLint({"SetTextI18n", "InvalidWakeLockTag"})
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_combinedgraph, container, false);
        applicationPDA = (ApplicationPDA) getActivity().getApplication();
        powerManager = (PowerManager) getActivity().getSystemService(Service.POWER_SERVICE);
        wakeLock = this.powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Lock");
        wakeLock.setReferenceCounted(false);
        now_millisecond = MILLISECOND_24;
        visible_range_millisecond = MILLISECOND_6;
        dataErrListAll = applicationPDA.getDataErrListAll();
        if (dataListAll == null) {
            dataListAll = new ArrayList<>();
        }
        if (dataErrListAll == null) {
            dataErrListAll = new ArrayList<>();
        }
        TextView text_view_unit = (TextView) mRootView.findViewById(R.id.text_view_unit);
        text_view_unit.setText(getResources().getString(R.string.history_bg) +
                " (" + getResources().getString(R.string.unit_mmol_l) + ")");
        synchronize = (ImageView) mRootView.findViewById(R.id.synchronize);
        textview_synchronizing = (TextView) mRootView.findViewById(R.id.text_view_synchronizing);
        textview_synchronizing.setVisibility(View.GONE);

//        boolean realtimeFlag = (boolean) SPUtils.get(getActivity(), REALTIMEFLAG, true);
//        if (realtimeFlag) {
//            synchronize.setVisibility(View.GONE);
//        } else {
//            synchronize.setVisibility(View.VISIBLE);
//        }
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
//                List<History> list = new ArrayList<>();
//                long bcd = getTodayDateTime().getBCD();
//                Status status = new Status(1000 + index * 150);
//                Event event = new Event(0, 7, 0);
//                for (int i = 0; i < 300; i++) {
//                    History history = new History();
//                    DateTime dateTime = new DateTime();
//                    dateTime.setBCD(new DateTime(Calendar.getInstance()).getBCD() - 24 * 60 * 100 + 5 * 60 * 10 * i - index * 1000);
//                    history.setStatus(status);
//                    history.setDateTime(dateTime);
//                    history.setEvent(event);
//                    list.add(history);
//                }
//                index++;
//                drawScatterData(list);
//
//                List<History> list = new ArrayList<>();
//                Status status = new Status(1000);
//                Event event = new Event(0, 7, 0);
//                History history = new History();
//                DateTime dateTime = new DateTime();
//                Calendar c = dateTime.getCalendar();
//                c.setTimeInMillis(Calendar.getInstance().getTimeInMillis() - (8 * 24 * 60 * 60 - index * 1200) * 1000);
//                dateTime.setCalendar(c);
//                history.setStatus(status);
//                history.setDateTime(dateTime);
//                history.setEvent(event);
//                list.add(history);
//                dataListAll.add(history);
//                index++;
//                drawScatterData(list);
            }
        });

        initChart();

        if (applicationPDA != null) {
            dataListAll = applicationPDA.getDataListAll();
        }

        updateGraphProfiles();


        return mRootView;
    }

//    public FragmentDialog showDialogConfirm(String title,
//                                            String buttonTextPositive, String buttonTextNegative, Fragment content,
//                                            boolean isCancelable, FragmentDialog.ListenerDialog listener) {
//        FragmentDialog fragmentDialog = new FragmentDialog();
//        fragmentDialog.setTitle(title);
//        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
//                buttonTextPositive);
//        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
//                buttonTextNegative);
//        fragmentDialog.setContent(content);
//        fragmentDialog.setCancelable(isCancelable);
//        fragmentDialog.setListener(listener);
//        fragmentDialog.show(getActivity().getSupportFragmentManager(), null);
//
//        return fragmentDialog;
//    }

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
//        mChart.setDragDecelerationEnabled(false);
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


        mChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
                float indexLowest = mChart.getLowestVisibleX();
                if (lastPerformedGesture == ChartTouchListener.ChartGesture.DRAG) {
                    addLine(indexLowest);
                }
            }

            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {
//                startActivity(new Intent(getContext(), ActivityFullGraph.class));
            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {
            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {
                float indexLowest = mChart.getLowestVisibleX();
                float indexHighest = mChart.getHighestVisibleX();
                float chartMax = mChart.getXChartMax();
                if (indexHighest < chartMax) {
                    addLine(indexLowest);
                }
            }
        });
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
            if (set.getEntryForIndex(0).getY() == -1) {
                set.removeEntry(0);
            }
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
        mLog.Error(getClass(), "更新血糖曲线");
        mLog.Error(getClass(), "总数量：：" + set.getEntryCount());
    }

    private ScatterData generateScatterData() {
        ScatterData d = new ScatterData();
        DateTime todayDateTime = getTodayDateTime();
        long maxIndex = new DateTime(Calendar.getInstance()).getCalendar().getTimeInMillis()
                - todayMilisecond;
        List<Entry> entries = new ArrayList<>();
        if (dataListAll == null) {
            dataListAll = new ArrayList<>();
        }
        if (dataListAll.size() > 0) {
            for (History history : dataListAll) {
                if (history.getEvent().getEvent() == GLUCOSE ||
                        history.getEvent().getEvent() == GLUCOSE_RECOMMEND_CAL) {
                    if (history.getDateTime().getCalendar().getTimeInMillis() < System.currentTimeMillis() + 60 * 1000) {
                        Status status = history.getStatus();
                        int value = status.getShortValue1() & 0xFFFF;
                        float value_display = Float.parseFloat(((ActivityPDA) getActivity())
                                .getGlucoseValue((value) *
                                        ActivityPDA.GLUCOSE_UNIT_MG_STEP, false).trim());
                        long time = (history.getDateTime().getCalendar().getTimeInMillis() - todayMilisecond) / 1000;
                        entries.add(new Entry(time / pointSpace, value_display));
                    }
                }
            }
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
                - todayDateTime.getCalendar().getTimeInMillis() + now_millisecond / 5;

        long minIndex = new DateTime(Calendar.getInstance()).getCalendar().getTimeInMillis()
                - todayDateTime.getCalendar().getTimeInMillis()
                - now_millisecond;

        if (dataListAll.size() > 0) {
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
    public void onResume() {
        super.onResume();
        isTop = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        isTop = false;
        if (applicationPDA != null) {
            applicationPDA.setDataListAll(dataListAll);
        }
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
//        if (message.getParameter() == ParameterComm.BROADCAST_SAVA) {
//            if (message.getData()[0] == 0) {
//                synchronize.setVisibility(View.VISIBLE);
//            } else {
//                synchronize.setVisibility(View.GONE);
//            }
//        }
        if (message.getParameter() == ParameterGlucose.PARAM_FILL_LIMIT
                || message.getParameter() == ParameterGlucose.PARAM_BG_LIMIT) {
            mChart.getData().setData(generateLineData());
            mChart.notifyDataSetChanged();
        }
        if (message.getParameter() == ParameterComm.RESET_DATA) {
            switch (message.getData()[0]) {
                case 0:
                    dataListAll.clear();
                    updateGraphProfiles();
                    break;
                case 1:
//                    drawScatterData(dataListAll);
                    drawOthers();
                    mChart.notifyDataSetChanged();
                    break;
                case 2:
                    dateChange = true;
                    break;
                default:

                    break;
            }
//            if (message.getData()[0] == 0) {
//                dataListAll.clear();
//                textview_synchronizing.setVisibility(View.GONE);
//            }
//            updateGraphProfiles();
        }
    }


    private void handleNotification(final EntityMessage message) {
        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
                (message.getParameter() == ParameterMonitor.PARAM_HISTORY)) {
            if (message
                    .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_MODEL) {
                mLog.Debug(getClass(), "Receive history");
                mIsHistoryQuerying = false;
                //取消屏幕常亮
                wakeLock.release();
                dismissDialogProgress();
//                mHistoryModel.setList(new DataList(message.getData()));
//                mHistoryModel.update();
                DataList dataList = new DataList(message.getData());
                for (int i = 0; i < dataList.getCount(); i++) {
                    History history = new History(dataList.getData(i));
                    Event event = history.getEvent();
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
                    if ((event.getEvent() == SENSOR_ERROR)
                            || (event.getEvent() == SENSOR_EXPIRATION)
                            || (event.getEvent() == HYPO)
                            || (event.getEvent() == HYPER)
                            || (event.getEvent() == PDA_ERROR)
//                            || (event.getEvent() == BLOOD_GLUCOSE)
//                            || (event.getEvent() == CALIBRATION)
                            ) {
//                        dataErrListAll.add(history);
                    }

                }
                if (applicationPDA != null) {
                    applicationPDA.setDataListAll(dataListAll);
                    applicationPDA.setDataErrListAll(dataErrListAll);
                }

//                updateGraphProfiles();
                drawScatterData(dataListAll);
                mChart.notifyDataSetChanged();
            }
        }

        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
                (message.getParameter() == ParameterMonitor.PARAM_HISTORY)) {
            if ((message.getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_CONTROL) ||
                    (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER)) {
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
                    Event event = history.getEvent();
                    if ((event.getEvent() == SENSOR_ERROR)
                            || (event.getEvent() == SENSOR_EXPIRATION)
                            || (event.getEvent() == HYPO)
                            || (event.getEvent() == HYPER)
                            || (event.getEvent() == PDA_ERROR)
//                            || (event.getEvent() == BLOOD_GLUCOSE)
//                            || (event.getEvent() == CALIBRATION)
                            ) {
                        if (applicationPDA != null) {
                            dataErrListAll = applicationPDA.getDataErrListAll();
//                            dataErrListAll.add(history);
                            applicationPDA.setDataErrListAll(dataErrListAll);
                        }
                    }
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
//        if (message.getSourcePort() == ParameterGlobal.PORT_MONITOR) {
//            switch (message.getParameter()) {
//                case ParameterMonitor.PARAM_STATUS:
//                    if (dataListCash.size() > 0) {
//                        drawScatterData(dataListCash);
//                        mChart.notifyDataSetChanged();
//                        dataListCash.clear();
//                    }
//                    break;
//                default:
//
//                    break;
//            }
//        }
//        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
//                (message.getParameter() == ParameterMonitor.SYNCHRONIZEDONE)) {
//            int synchronizeDone = message.getData()[0];
//            switch (synchronizeDone) {
//                case 0:
//                    textview_synchronizing.setVisibility(View.VISIBLE);
//                    break;
//                case 1:
//                    textview_synchronizing.setVisibility(View.GONE);
//                    break;
//                default:
//
//                    break;
//            }
//        }

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
        //请求屏幕常亮
//        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
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
        todayMilisecond = getTodayDateTime().getCalendar().getTimeInMillis();
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
        if ((maxIndex - (visible_range_millisecond / 1000 / pointSpace) * 0.8) > mChart.getLineData().getXMin()) {
            mChart.moveViewToX((float) (maxIndex - (visible_range_millisecond / 1000 / pointSpace) * 0.8));
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
        List<CalibrationHistory> list = (List<CalibrationHistory>) CalibrationSaveUtil.get(getActivity(), CALIBRATION_HISTORY);
        long todayMills = getTodayDateTime().getCalendar().getTimeInMillis();
        ArrayList<Entry> yVals = new ArrayList<>();

        if (list != null) {
            for (CalibrationHistory calibrationHistory : list) {
                yVals.add(new Entry((calibrationHistory.getTime() - todayMills) / 1000 / pointSpace, calibrationHistory.getValue()));
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
        if ((maxIndex - (visible_range_millisecond / 1000 / pointSpace) * 0.8) > mChart.getLineData().getXMin()) {
            t = (float) (maxIndex - (visible_range_millisecond / 1000 / pointSpace) * 0.8);
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

    private boolean rangeInDefined(long current, long min, long max) {
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

//    private void addEntry(float time, float value) {
//
//        ScatterData data = mChart.getData().getScatterData();
//
//        if (data != null) {
//
//            IScatterDataSet set = data.getDataSetByIndex(0);
//            // set.addEntry(...); // can be called as well
//
//            if (set == null) {
//                set = createSet();
//                data.addDataSet(set);
//            }
//
////            data.addEntry(new Entry(time, value / 10), 0);
//            set.addEntryOrdered(new Entry(time, value / 10));
//            data.notifyDataChanged();
//
//
//            // let the chart know it's data has changed
////            mChart.notifyDataSetChanged();
//
////            mLineChart.setVisibleXRangeMaximum(now_millisecond / 100000);
//
//            // limit the number of visible entries
////            mLineChart.setVisibleXRangeMaximum(8);
////            mLineChart.setVisibleYRange(30, AxisDependency.LEFT);
//            // move to the latest entry
////            mLineChart.moveViewToX(value);
//            mChart.fitScreen();
//            // this automatically refreshes the chart (calls invalidate())
//            // mChart.moveViewTo(data.getXValCount()-7, 55f,
//            // AxisDependency.LEFT);
//
//        }
//    }

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
