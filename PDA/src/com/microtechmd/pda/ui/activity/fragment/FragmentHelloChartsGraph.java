package com.microtechmd.pda.ui.activity.fragment;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.database.DataSetHistory;
import com.microtechmd.pda.database.DbHistory;
import com.microtechmd.pda.entity.CalibrationHistory;
import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterGlucose;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.SPUtils;
import com.microtechmd.pda.ui.activity.ActivityMain;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.util.CalibrationSaveUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.listener.ViewportChangeListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

import static com.microtechmd.pda.ui.activity.ActivityPDA.CALIBRATION_HISTORY;
import static com.microtechmd.pda.ui.activity.ActivityPDA.DATE_CHANGE;
import static com.microtechmd.pda.ui.activity.ActivityPDA.GLUCOSE;
import static com.microtechmd.pda.ui.activity.ActivityPDA.GLUCOSE_RECOMMEND_CAL;
import static com.microtechmd.pda.ui.activity.ActivityPDA.HYPER;
import static com.microtechmd.pda.ui.activity.ActivityPDA.HYPO;
import static com.microtechmd.pda.ui.activity.ActivityPDA.PDA_ERROR;
import static com.microtechmd.pda.ui.activity.ActivityPDA.SENSOR_ERROR;
import static com.microtechmd.pda.ui.activity.ActivityPDA.SENSOR_EXPIRATION;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingTips.HYPER_DEFAULT;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettingTips.HYPO_DEFAULT;

public class FragmentHelloChartsGraph extends FragmentBase implements EntityMessage.Listener {
    private static final long millisecond_1 = 60 * 60 * 1000;
    private static final long millisecond_2 = millisecond_1 * 2;
    private static final long millisecond_4 = millisecond_1 * 4;
    private View mRootView;


    private boolean dateChange;
    private PowerManager.WakeLock wakeLock = null;
    private LineChartView chart;
    private LineChartData data;
    private float minY = 0f;//Y轴坐标最小值
    private float maxY = 30f;//Y轴坐标最大值
    private List<AxisValue> mAxisYValues = new ArrayList<>();//Y轴坐标值
    private long step = millisecond_1;
    private long maxLimit;
    private long minLimit;
    private float displayLeft;
    private float displayRight;
    private float displayDragRight;
    private long zero;
    private static int pointSpace = 30 * 1000;

    private int high = HYPER_DEFAULT;
    private int low = HYPO_DEFAULT;

    private boolean mIsHistoryQuerying = false;
    private List<DbHistory> dbList;
    private List<PointValue> valuesAll;
    private List<DbHistory> dataListAll;
    private ArrayList<DbHistory> dataErrListAll;
    private List<DbHistory> dataListCash;
    private Handler mHandler;
    private Runnable mRunnable;
    private CountDownTimer refreshGraphCountdownTimer;

    private TextView tv_time_line_left;
    private TextView tv_time_line_right;

    private DataSetHistory mDataSetHistory;
    private ExecutorService fixedThreadPool;


    //    private List<List<PointValue>> lineList = new ArrayList<>();
    private int lastIndex = 0;

    public void setTimeData(int timeData) {
        switch (timeData) {
            case FragmentHome.TIME_DATA_6:
                step = millisecond_1;
                break;
            case FragmentHome.TIME_DATA_12:
                step = millisecond_2;
                break;
            case FragmentHome.TIME_DATA_24:
                step = millisecond_4;
                break;
            default:
                break;
        }
        if (valuesAll != null) {
            if (valuesAll.size() > 0) {
                setZoom();
            } else {
                generateData(new ArrayList<DbHistory>());
            }
        }
    }

    public void firstRefresh() {
        generateData(new ArrayList<DbHistory>());
    }

    public void setDataChange() {
        dateChange = (boolean) SPUtils.get(app, DATE_CHANGE, false);
        if (dateChange) {
            dataListAll.clear();
            valuesAll.clear();
//            lineList.clear();
            lastIndex = 0;
            generateData(new ArrayList<DbHistory>());
            queryHistory();
            dateChange = false;
            SPUtils.put(app, DATE_CHANGE, false);
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
        switch (message.getParameter()) {
            case ParameterGlucose.PARAM_FILL_LIMIT:
            case ParameterGlucose.PARAM_BG_LIMIT:
//                generateData();
                generateData(new ArrayList<DbHistory>());
                break;
            case ParameterComm.RESET_DATA:
                switch (message.getData()[0]) {
                    case 0:
                        mDataSetHistory.cleanDatabases();
                        dataListAll.clear();
                        valuesAll.clear();
//                        lineList.clear();
                        lastIndex = 0;
                        generateData(new ArrayList<DbHistory>());
                        break;
                    case 1:
                        generateData(new ArrayList<DbHistory>());
                        break;
                    case 2:
                        dateChange = true;
                        dataListAll.clear();
                        valuesAll.clear();
                        lastIndex = 0;
                        generateData(new ArrayList<DbHistory>());
                        break;
                    case 3:
                        generateData(new ArrayList<DbHistory>());
                        break;
                    default:

                        break;
                }
                break;
            default:
                break;
        }
    }

    private void handleNotification(final EntityMessage message) {
        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
                (message.getParameter() == ParameterMonitor.PARAM_HISTORY)) {
            switch (message.getSourceAddress()) {
                case ParameterGlobal.ADDRESS_LOCAL_MODEL:
//                    handleMsgFromModel(message);
                    break;
                case ParameterGlobal.ADDRESS_LOCAL_CONTROL:
                case ParameterGlobal.ADDRESS_REMOTE_MASTER:
                    handleFromControl(message);
                    break;
                default:

                    break;
            }
        }
    }

    //数据库返回的数据
    private void handleMsgFromModel() {
        mIsHistoryQuerying = false;
        //取消屏幕常亮
        wakeLock.release();
        dismissDialogProgress();
        mLog.Error(FragmentHelloChartsGraph.this.getClass(), "UI数据库数量" + dbList.size());
        dataErrListAll.clear();
        for (DbHistory dbHistory : dbList) {
            int eventType = dbHistory.getEvent_type();
            if ((eventType == SENSOR_ERROR)
                    || (eventType == SENSOR_EXPIRATION)
                    || (eventType == HYPO)
                    || (eventType == HYPER)
                    || (eventType == PDA_ERROR)
//                            || (event.getEvent() == BLOOD_GLUCOSE)
//                            || (event.getEvent() == CALIBRATION)
            ) {
                dataErrListAll.add(dbHistory);
            }
        }
//        if (app != null) {
//            app.setDataErrListAll(dataErrListAll);
//        }
        valuesAll.clear();
//        lineList.clear();
        lastIndex = 0;
        long start = System.currentTimeMillis();
        generateData(dbList);
        long end = System.currentTimeMillis();
        mLog.Error(getClass(), "解析加画图时间：" + (end - start));
//        dismissDialogProgress();
//        DataList dataList = new DataList(message.getData());
//        for (int i = 0; i < dataList.getCount(); i++) {
//            History history = new History(dataList.getData(i));
//            Event event = history.getEvent();
//            switch (history.getStatus().getShortValue1()) {
//                case 0:
//                    history.getStatus().setShortValue1(20);
//                    break;
//                case 255:
//                    history.getStatus().setShortValue1(250);
//                    break;
//                default:
//                    break;
//            }
//            dataListAll.add(history);
//            if ((event.getEvent() == SENSOR_ERROR)
//                    || (event.getEvent() == SENSOR_EXPIRATION)
//                    || (event.getEvent() == HYPO)
//                    || (event.getEvent() == HYPER)
//                    || (event.getEvent() == PDA_ERROR)
////                            || (event.getEvent() == BLOOD_GLUCOSE)
////                            || (event.getEvent() == CALIBRATION)
//                    ) {
//                dataErrListAll.add(history);
//            }
//
//        }
//        if (app != null) {
//            app.setDataListAll(dataListAll);
//            app.setDataErrListAll(dataErrListAll);
//        }
//        valuesAll.clear();
//        generateData(dataListAll);
    }

    //广播包及同步的数据
    private void handleFromControl(EntityMessage message) {
        final DataList dataList = new DataList(message.getData());
//        if (dataList.getCount() > 0) {
//
//        }
        long start = System.currentTimeMillis();
        final List<DbHistory> dbHistories = new ArrayList<>();
        String addressSetting = getAddress(((ActivityPDA) getActivity())
                .getDataStorage(ActivityPDA.class.getSimpleName())
                .getExtras(ActivityPDA.SETTING_RF_ADDRESS, null));
        for (int i = 0; i < dataList.getCount(); i++) {
            DbHistory history = new DbHistory(dataList.getData(i));
            history.setRf_address(addressSetting);
            if (history.getEvent_type() != PDA_ERROR) {
                dbHistories.add(history);
            }
            switch (history.getValue()) {
                case 0:
                    history.setValue(20);
                    break;
                case 255:
                    history.setValue(250);
                    break;
                default:
                    break;
            }
            switch (history.getEvent_type()) {
                case GLUCOSE:
                case GLUCOSE_RECOMMEND_CAL:
                    dataListAll.add(history);
                    dataListCash.add(history);
                    break;
                case SENSOR_ERROR:
                case SENSOR_EXPIRATION:
                case HYPO:
                case HYPER:
                case PDA_ERROR:
//                case BLOOD_GLUCOSE:
//                case CALIBRATION:
//                    if (app != null) {
//                        dataErrListAll = app.getDataErrListAll();
//                        dataErrListAll.add(history);
//                        app.setDataErrListAll(dataErrListAll);
//                    }
                    break;
                default:

                    break;
            }
        }

        //保存到数据库
        Runnable command = new Runnable() {
            @Override
            public void run() {
                mDataSetHistory.insertAllHistory(dbHistories);
            }
        };
        fixedThreadPool.execute(command);
        long end = System.currentTimeMillis();
        mLog.Error(getClass(), "转换时间：" + (end - start));
        if (dataListCash.size() < 3 || dataListCash.size() > 500) {
            generateData(dataListCash);
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
                    generateData(dataListCash);
                    dataListCash.clear();
                }

            }
        }.start();
    }

    @SuppressLint({"SetTextI18n", "InvalidWakeLockTag"})
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_hellochartsgraph, container, false);
        if (mDataSetHistory == null) {
            mDataSetHistory = new DataSetHistory(getActivity());
        }
        //定长线程池，可控制线程最大并发数，超出的线程会在队列中等待
        fixedThreadPool = Executors.newFixedThreadPool(1);
        PowerManager powerManager = (PowerManager) getActivity().getSystemService(Service.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Lock");
            wakeLock.setReferenceCounted(false);
        }
        chart = (LineChartView) mRootView.findViewById(R.id.chart);
        tv_time_line_left = (TextView) mRootView.findViewById(R.id.tv_time_line_left);
        tv_time_line_right = (TextView) mRootView.findViewById(R.id.tv_time_line_right);
        chart.setInteractive(true);
        chart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        chart.setZoomEnabled(false);
        chart.setMaxZoom(Float.MAX_VALUE);
        chart.setViewportChangeListener(new ViewportChangeListener() {
            @Override
            public void onViewportChanged(Viewport viewport) {
                displayDragRight = viewport.right;
                long timeLineLeft = ((long) viewport.left) * pointSpace + zero;
                long timeLineRight = ((long) viewport.right) * pointSpace + zero;
                SimpleDateFormat format = new SimpleDateFormat("MM-dd", Locale.getDefault());
                String left = format.format(new Date(timeLineLeft));
                String right = format.format(new Date(timeLineRight));
                tv_time_line_left.setText(left);
                tv_time_line_right.setText(right);
                if (TextUtils.equals(left, right)) {
                    tv_time_line_right.setVisibility(View.GONE);
                } else {
                    tv_time_line_right.setVisibility(View.VISIBLE);
                }
            }
        });

        for (float i = minY; i <= maxY; i += 5) {
            mAxisYValues.add(new AxisValue(i).setLabel((int) i + ""));
        }
        dataListAll = new ArrayList<>();
        dataErrListAll = new ArrayList<>();
        dataListCash = new ArrayList<>();
        valuesAll = new ArrayList<>();
        dbList = new ArrayList<>();

        generateData(dataListAll);
        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_MONITOR, this);

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
        if (dataListAll.size() == 0) {
            mHandler.postDelayed(mRunnable, 100);
        }
    }

    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRunnable);
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_MONITOR, this);
        super.onDestroyView();
    }

    protected Activity mActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    private void queryHistory() {
        if (mIsHistoryQuerying) {
            return;
        }
        //请求屏幕常亮
//        wakeLock.acquire();
        showDialogProgress();
        mIsHistoryQuerying = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                dbList = mDataSetHistory.getDbList();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleMsgFromModel();
                    }
                });
            }
        }).start();
    }

    private void showDialogProgress() {
        ((ActivityPDA) getActivity()).showDialogLoading();
    }

    private void dismissDialogProgress() {
        ((ActivityPDA) getActivity()).dismissDialogLoading();
    }

    private void generateData(List<DbHistory> dataList) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        long now_zero = calendar.getTimeInMillis();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        //以当天0点位基准 如果数据相隔一天则所有点重新换基准
        long offset = zero - calendar.getTimeInMillis();
        if ((offset != 0) && (zero != 0)) {
            for (int i = 0; i < valuesAll.size(); i++) {
                PointValue value = valuesAll.get(i);
                valuesAll.set(i, new PointValue(value.getX() + offset / pointSpace, value.getY()));
//                lineList.clear();
                lastIndex = 0;
            }
        }
        zero = calendar.getTimeInMillis();

//        if (offset > 0) {
//            maxLimit = now_zero - zero + 5 * 60 * 60 * 1000 + offset;
//        } else {
        maxLimit = now_zero - zero + 5 * 60 * 60 * 1000;
//        }

        minLimit = now_zero - zero - ((24 + hour % 4) * 60 * 60 * 1000);

        List<Line> lines = new ArrayList<>();

        if (dataList.size() > 0) {
            valuesAll.addAll(dbToPoint(dataList));
        }
        if (valuesAll.size() > 0) {
            while (valuesAll.size() > 12 * 24 * 40) {
                valuesAll.remove(0);
            }
            setMinPoint();
            List<List<PointValue>> lineList = new ArrayList<>();
            List<Integer> errList = new ArrayList<>();
            for (int i = 0; i < valuesAll.size(); i++) {
                if (i > 0) {
                    if (valuesAll.get(i).getX() < valuesAll.get(i - 1).getX()
                            || (valuesAll.get(i).getX() - valuesAll.get(i - 1).getX()) > 15 * 60 * 1000 / pointSpace) {
                        errList.add(i);
                    }
                }
            }
            lastIndex = valuesAll.size() - 1;
            if (errList.size() > 0) {
                for (int i = 0; i < errList.size(); i++) {
                    if (i == 0) {
                        lineList.add(valuesAll.subList(0, errList.get(i)));
                    } else {
                        lineList.add(valuesAll.subList(errList.get(i - 1), errList.get(i)));
                    }
                }
                if (errList.get(errList.size() - 1) < valuesAll.size()) {
                    lineList.add(valuesAll.subList(errList.get(errList.size() - 1), valuesAll.size()));
                }

            } else {
                lineList.add(valuesAll);
            }

            mLog.Error(getClass(), "线段条数" + lineList.size());

            for (List<PointValue> pointValues : lineList) {
                if (pointValues.size() > 1) {
                    Line glucoseLine = getGlucoseLine(pointValues);
                    lines.add(glucoseLine);
                } else {
                    Line glucoseLine = getSingleGlucoseLine(pointValues);
                    lines.add(glucoseLine);
                }
            }


            //添加血糖时间最大值的点
            PointValue maxPoint = null;
            History historyMax = ActivityMain.getStatus();
            if (historyMax != null) {
                int value = historyMax.getStatus().getShortValue1();
                mLog.Error(getClass(), "绿点值：" + value);
                if (value == 0) {
                    value = 20;
                } else if (value == 255) {
                    value = 250;
                }
                if (value >= 20 && value <= 255) {
                    float value_display = value / 10f;
                    long time = historyMax.getDateTime().getCalendar().getTimeInMillis() - zero;
                    if (rangeInDefined(time, minLimit, maxLimit)) {
                        maxPoint = new PointValue(time / pointSpace, value_display);
                    }
                }
            }

            if (maxPoint != null) {
                Line maxPointLine = new Line();
                List<PointValue> maxPointValue = new ArrayList<>();
                maxPointValue.add(maxPoint);
                maxPointLine.setValues(maxPointValue);
                maxPointLine.setPointColor(Color.GREEN);
                maxPointLine.setPointRadius(4);
                maxPointLine.setHasLines(false);
                lines.add(maxPointLine);
            }
        } else {
            lines.add(new Line());
        }

        //添加血糖校准线
        List<CalibrationHistory> list = (List<CalibrationHistory>) CalibrationSaveUtil.get(getActivity(), CALIBRATION_HISTORY);
        if (list != null) {
            if (list.size() > 0) {
                Line calibrationLine = getCalibrationLine(list);
                lines.add(calibrationLine);
            }
        }

        //添加阈值线
        Line lowLine = getLimitLine("low");
        Line highLine = getLimitLine("high");

        lines.add(lowLine);
        lines.add(highLine);

        data = new LineChartData(lines);
        data.setValueLabelBackgroundEnabled(false);
        setZoom();
    }

    private boolean rangeInDefined(long current, long min, long max) {
        return Math.max(min, current) == Math.min(current, max);
    }

    private void setMinPoint() {
        PointValue minPoint = Collections.min(valuesAll, new MyComparator());
        if (minPoint.getX() * pointSpace < minLimit) {
            Calendar calendarMin = Calendar.getInstance();
            calendarMin.setTimeInMillis(((long) minPoint.getX()) * pointSpace + zero);
            calendarMin.set(Calendar.MINUTE, 0);
            calendarMin.set(Calendar.SECOND, 0);
            calendarMin.set(Calendar.MILLISECOND, 0);
            minLimit = calendarMin.getTimeInMillis() -
                    (calendarMin.get(Calendar.HOUR_OF_DAY) % 4) * millisecond_1 - zero;
        }
    }

    private Line getCalibrationLine(List<CalibrationHistory> list) {
        Line line = new Line();
        line.setPointColor(Color.RED);
        line.setPointRadius(5);
        line.setHasLabels(true);
        line.setHasLines(false);

        List<PointValue> values = new ArrayList<>();
        for (CalibrationHistory calibrationHistory : list) {
            long time = calibrationHistory.getTime() - zero;
            if (rangeInDefined(time, minLimit, maxLimit)) {
                float value = calibrationHistory.getValue();
                PointValue pointValue = new PointValue(time / pointSpace, value);
                pointValue.setLabel(value + "");
                values.add(pointValue);
            }
        }
        line.setValues(values);
        return line;
    }

//    private List<PointValue> historyToPoint(List<History> dataList) {
//        List<PointValue> values = new ArrayList<>();
//        long start = System.currentTimeMillis();
//        for (History history : dataList) {
//            int type = history.getEvent().getEvent();
//            if (type == GLUCOSE ||
//                    type == GLUCOSE_RECOMMEND_CAL) {
//                long t = history.getDateTime().getCalendar().getTimeInMillis();
//                if (t < System.currentTimeMillis() + 60 * 60 * 1000) {
//                    Status status = history.getStatus();
//                    float value_display = status.getShortValue1() / 10f;
//                    long time = t - zero;
//                    values.add(new PointValue(time / pointSpace, value_display));
//
//                }
//            }
//        }
//        long end = System.currentTimeMillis();
//        mLog.Error(getClass(), "解析时间：" + (end - start));
//        return values;
//    }

    private List<PointValue> dbToPoint(List<DbHistory> dataList) {
        List<PointValue> values = new ArrayList<>();
        long current = System.currentTimeMillis();
        for (DbHistory dbHistory : dataList) {
            if (dbHistory.getEvent_type() == GLUCOSE ||
                    dbHistory.getEvent_type() == GLUCOSE_RECOMMEND_CAL) {
                long t = dbHistory.getDate_time();
                if (t < current + 4 * 60 * 60 * 1000) {
                    float value_display = dbHistory.getValue() / 10f;
                    long time = t - zero;
                    values.add(new PointValue(time / pointSpace, value_display));
                }
            }
        }
        return values;
    }

    private void setZoom() {
        Axis axisX = new Axis().setHasLines(true);
        axisX.setLineColor(Color.GRAY);
        List<AxisValue> xValues = new ArrayList<>();
        for (long value = minLimit; value < maxLimit; value += step) {
            SimpleDateFormat format = new SimpleDateFormat("HH", Locale.getDefault());
            AxisValue axisValue = new AxisValue(value / pointSpace);
            axisValue.setLabel("" + format.format(new Date(value + zero)));
            xValues.add(axisValue);
        }
        axisX.setValues(xValues);
        axisX.setMaxLabelChars(0);

        Axis axisY = new Axis();
        axisY.setHasLines(false);
        axisY.setInside(true);
        axisY.setValues(mAxisYValues);


        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        chart.setLineChartData(data);

        setDisplayArea();
    }

    private void setDisplayArea() {
        Viewport v = new Viewport(chart.getMaximumViewport());
        v.bottom = minY;
        v.top = maxY + 0.6f;
        //固定Y轴的范围,如果没有这个,Y轴的范围会根据数据的最大值和最小值决定
        chart.setMaximumViewport(v);
        //这2个属性的设置一定要在lineChart.setMaximumViewport(v);这个方法之后,不然显示的坐标数据是不能左右滑动查看更多数据的

        long now = System.currentTimeMillis();
        displayLeft = (now - zero + 60 * 60 * 1000 - 6 * step) / pointSpace;
        displayRight = (now - zero + 60 * 60 * 1000) / pointSpace;
        if (valuesAll.size() > 0) {
            PointValue maxPoint = Collections.max(valuesAll, new MyComparator());
            float l = maxPoint.getX() - (step * 5) / pointSpace;
            float r = maxPoint.getX() + step / pointSpace;
            if (l < minLimit / pointSpace) {
                displayLeft = minLimit / pointSpace;
                displayRight = (minLimit + 6 * step) / pointSpace;
            } else {
                displayLeft = l;
                displayRight = r;
            }
        }
        v.left = displayLeft;
        v.right = displayRight;

        chart.setCurrentViewport(v);
    }
//
//    @NonNull
//    private List<PointValue> getGlucoseValues() {
//        List<PointValue> values = new ArrayList<>();
//        List<Long> xIndexs = new ArrayList<>();
//        for (History history : dataListAll) {
//            if (history.getEvent().getEvent() == GLUCOSE ||
//                    history.getEvent().getEvent() == GLUCOSE_RECOMMEND_CAL) {
//                if (history.getDateTime().getCalendar().getTimeInMillis() < System.currentTimeMillis() + 60 * 60 * 1000) {
//                    Status status = history.getStatus();
//                    int value = status.getShortValue1() & 0xFFFF;
//                    float value_display = Float.parseFloat(((ActivityPDA) getActivity())
//                            .getGlucoseValue((value) * ActivityPDA.GLUCOSE_UNIT_MG_STEP, false).trim());
//                    long time = history.getDateTime().getCalendar().getTimeInMillis() - zero;
//                    values.add(new PointValue(time / pointSpace, value_display));
//                    xIndexs.add(time + zero);
//                }
//            }
//        }
////        long now = System.currentTimeMillis();
////        for (long i = now; i > now - 20 * 24 * 60 * 60 * 1000; i -= 5 * 60 * 1000) {
////            values.add(new PointValue((i - zero) / pointSpace, 6));
////            xIndexs.add(i);
////        }
//        Long minIndex = Collections.min(xIndexs);
//        if ((minIndex - zero) < minLimit) {
//            Calendar calendarMin = Calendar.getInstance();
//            calendarMin.setTimeInMillis(minIndex);
//            calendarMin.set(Calendar.MINUTE, 0);
//            calendarMin.set(Calendar.SECOND, 0);
//            calendarMin.set(Calendar.MILLISECOND, 0);
//            minLimit = calendarMin.getTimeInMillis() - zero;
//        }
//        return values;
//    }


    @NonNull
    private Line getGlucoseLine(List<PointValue> values) {
        Line line = new Line(values);
//        line.setPointColor(Color.parseColor("#FF00DEFF"));
//        line.setPointRadius(2);
        line.setHasLines(true);
        line.setColor(Color.parseColor("#FF00DEFF"));
        line.setHasPoints(false);
        return line;
    }

    @NonNull
    private Line getSingleGlucoseLine(List<PointValue> values) {
        Line line = new Line(values);
        line.setPointColor(Color.parseColor("#FF00DEFF"));
        line.setPointRadius(2);
        line.setHasLines(false);
//        line.setColor(Color.parseColor("#FF00DEFF"));
//        line.setHasPoints(false);
        return line;
    }

    private Line getErrGlucoseLine(List<PointValue> values) {
        Line line = new Line(values);
        line.setPointColor(Color.YELLOW);
        line.setPointRadius(2);
        line.setHasLines(false);
        return line;
    }

    private Line getLimitLine(String limit) {
        high = ((ActivityPDA) getActivity())
                .getDataStorage(ActivityPDA.class.getSimpleName())
                .getInt(FragmentSettings.SETTING_HYPER, HYPER_DEFAULT);
        low = ((ActivityPDA) getActivity())
                .getDataStorage(ActivityPDA.class.getSimpleName())
                .getInt(FragmentSettings.SETTING_HYPO, HYPO_DEFAULT);

        Line line = new Line();
        List<PointValue> valuesLimit = new ArrayList<>();
        line.setHasPoints(false);
        line.setHasLines(true);
        line.setColor(Color.GRAY);
        line.setAreaTransparency(90);
        line.setStrokeWidth(0);
        switch (limit) {
            case "low":
                valuesLimit.add(new PointValue(minLimit / pointSpace, low / 10f));
                valuesLimit.add(new PointValue(maxLimit / pointSpace, low / 10f));
                line.setFilled(false);
                break;
            case "high":
                line.setBaseArea(low / 10f);
                line.setFilled(true);
                valuesLimit.add(new PointValue(minLimit / pointSpace, high / 10f));
                valuesLimit.add(new PointValue(maxLimit / pointSpace, high / 10f));
                break;
            default:

                break;
        }
        line.setValues(valuesLimit);
        return line;
    }


    public class MyComparator implements Comparator<PointValue> {
        @Override
        public int compare(PointValue pointValue, PointValue t1) {
            Float f1 = pointValue.getX();
            Float f2 = t1.getX();
            return f1.compareTo(f2);
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

}
