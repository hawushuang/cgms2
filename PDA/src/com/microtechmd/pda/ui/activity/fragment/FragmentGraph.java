package com.microtechmd.pda.ui.activity.fragment;


import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.ui.widget.WidgetGraph;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.SimpleTimeZone;

import static com.microtechmd.pda.ui.activity.ActivityPDA.GLUCOSE_UNIT_MMOL;


public class FragmentGraph extends FragmentBase
        implements
        EntityMessage.Listener {
    public static final int GRAPH_MARGIN_LEFT = 45;
    public static final int GRAPH_MARGIN_TOP = 25;
    public static final int GRAPH_MARGIN_RIGHT = 20;
    public static final int GRAPH_MARGIN_BOTTOM = 8;
    public static final int GRAPH_MARGIN_TIMELINE = 30;
    public static final int GRAPH_DIVISION_HORIZONTAL = 6;
    public static final int GRAPH_DIVISION_VERTICAL = 2;
    public static final int GRAPH_TEXT_SIZE = 14;
    public static final float GRAPH_RANGE_GAIN = 2.0f;

    private static final int GRAPH_TYPE_TOP = 0;
    private static final int GRAPH_TYPE_TIMELINE = 1;
    private static final int COUNT_GRAPH_TYPE = 2;
    private static final int VIEW_ID_GRAPH[] =
            {
                    R.id.widget_graph_top, R.id.widget_graph_timeline
            };
    private static final int VERTICAL_MIN_DISTANCE = 100;
    private static final int MIN_VELOCITY = 10;

    private boolean mIsHistoryQuerying = false;
    private int mGraphTimeBegin = 0;
    private int mGraphTimeEnd = 0;
    private WidgetGraph mGraphs[] = null;
    private WidgetGraph.Profile mGraphProfiles[] = null;
    private DateTime mDateTime = null;
    private HistoryModel mHistoryModel = null;
    private Handler mHandler = null;
    private Runnable mRunnable = null;
    private GestureDetector mGestureGraph = null;

    private View mRootView = null;


    private class HistoryView {
        public static final int TYPE_GLUCOSE = 0;
        public static final int COUNT_TYPE = 1;
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


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_graph, container, false);

        mRootView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {

                return mGestureGraph.onTouchEvent(event);
            }
        });

        return mRootView;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLog.Debug(getClass(), "Create graph");

        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_MONITOR, this);

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
            initializeGraph();

            mHandler = new Handler();
            mRunnable = new Runnable() {
                @Override
                public void run() {
//                    queryHistory(mDateTime);
                }
            };
            mHandler.postDelayed(mRunnable, 100);
        }

        updateGraphProfiles();
        updateGraphs();
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

                mGraphTimeBegin = 0;
                mGraphTimeEnd = DateTime.SECOND_PER_MINUTE *
                        DateTime.MINUTE_PER_HOUR * DateTime.HOUR_PER_DAY;
                updateGraphProfiles();
                updateGraphs();
            }
        }

        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
                (message.getParameter() == ParameterMonitor.PARAM_STATUS)) {
            if ((message
                    .getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_CONTROL) ||
                    (message
                            .getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER)) {
                mLog.Debug(getClass(), "new history");

                // if (mHistoryModel.getCount() <= 0)
                {
                    // initializeGraph();
                    mDateTime = new DateTime(Calendar.getInstance());
                    mDateTime.setHour(0);
                    mDateTime.setMinute(0);
                    mDateTime.setSecond(0);
//                    queryHistory(mDateTime);
                }
            }
        }
    }


    private void initializeGraph() {
        if (mGraphs == null) {
            mGraphs = new WidgetGraph[COUNT_GRAPH_TYPE];
            WidgetGraph graph;
            WidgetGraph.Coordinate coordinate;
            WidgetGraph.Grid grid;
            ArrayList<WidgetGraph.Coordinate> coordinates;
            ArrayList<WidgetGraph.Grid> grids;
            Paint paint;
            Paint paintText = new Paint();
            paintText.setColor(getResources().getColor(R.color.text_light));
            paintText.setTextSize(GRAPH_TEXT_SIZE);
            paintText.setAntiAlias(true);
            Paint paintBorder = new Paint();
            paintBorder
                    .setColor(getResources().getColor(R.color.background_light));
            paintBorder.setStyle(Paint.Style.STROKE);
            paintBorder.setStrokeWidth(0);

            graph = new WidgetGraph(getActivity());
            paint = new Paint(paintText);
            paint.setTextAlign(Paint.Align.RIGHT);
            coordinates = new ArrayList<>();
            coordinate = graph.new Coordinate();
            coordinate.setPadding(GRAPH_TEXT_SIZE / 2);
            coordinate.setPaint(paint);
            coordinates.add(coordinate);
            graph.setCoordinates(WidgetGraph.POSITION_LEFT, GRAPH_MARGIN_LEFT,
                    coordinates);
            paint = new Paint(paintText);
            paint.setTextAlign(Paint.Align.CENTER);
            coordinates = new ArrayList<>();
            coordinate = graph.new Coordinate();
            coordinate.setPadding(GRAPH_TEXT_SIZE / 2);
            coordinate.setPaint(paint);
            coordinates.add(coordinate);
            graph.setCoordinates(WidgetGraph.POSITION_TOP, GRAPH_MARGIN_TOP,
                    coordinates);
            paint = new Paint(paintText);
            paint.setTextAlign(Paint.Align.LEFT);
            coordinates = new ArrayList<>();
            coordinate = graph.new Coordinate();
            coordinate.setPadding(GRAPH_TEXT_SIZE / 2);
            coordinate.setPaint(paint);
            coordinates.add(coordinate);
            graph.setCoordinates(WidgetGraph.POSITION_RIGHT, GRAPH_MARGIN_RIGHT,
                    coordinates);
            graph.setCoordinates(WidgetGraph.POSITION_BOTTOM,
                    GRAPH_MARGIN_BOTTOM, null);
            graph.setPaintBorder(paintBorder);
            paint = new Paint(paintBorder);
            grids = new ArrayList<>();
            grid = graph.new Grid();
            grid.setStyle(WidgetGraph.Grid.STYLE_HORIZONTAL);
            grid.setDivision(GRAPH_DIVISION_VERTICAL);
            grid.setPaint(paint);
            grids.add(grid);
            grid = graph.new Grid();
            grid.setStyle(WidgetGraph.Grid.STYLE_VERTICAL);
            grid.setDivision(GRAPH_DIVISION_HORIZONTAL);
            grid.setPaint(paint);
            grids.add(grid);
            graph.setGrids(grids);
            mGraphs[GRAPH_TYPE_TOP] = graph;

            mGraphTimeBegin = 0;
            mGraphTimeEnd = DateTime.SECOND_PER_MINUTE *
                    DateTime.MINUTE_PER_HOUR * DateTime.HOUR_PER_DAY;
            int timeSegment =
                    (mGraphTimeEnd - mGraphTimeBegin) / GRAPH_DIVISION_HORIZONTAL;
            ArrayList<String> texts = new ArrayList<>();

            for (int j = mGraphTimeBegin; j <= mGraphTimeEnd; j += timeSegment) {
                texts.add(((ActivityPDA) getActivity()).getTimeString(
                        j * DateTime.MILLISECOND_PER_SECOND,
                        new SimpleTimeZone(0, "UTC")));
            }

            paint = new Paint(paintText);
            paint.setTextAlign(Paint.Align.CENTER);
            graph = new WidgetGraph(getActivity());
            coordinates = new ArrayList<>();
            coordinate = graph.new Coordinate();
            coordinate.setPaint(paint);
            coordinate.setTexts(texts);
            coordinates.add(coordinate);
            graph.setCoordinates(WidgetGraph.POSITION_LEFT, GRAPH_MARGIN_LEFT,
                    null);
            graph.setCoordinates(WidgetGraph.POSITION_TOP, 0, null);
            graph.setCoordinates(WidgetGraph.POSITION_RIGHT, GRAPH_MARGIN_RIGHT,
                    null);
            graph.setCoordinates(WidgetGraph.POSITION_BOTTOM,
                    GRAPH_MARGIN_TIMELINE, coordinates);
            mGraphs[GRAPH_TYPE_TIMELINE] = graph;
        }

        if (mGraphProfiles == null) {
            mGraphProfiles = new WidgetGraph.Profile[HistoryView.COUNT_TYPE];
            mGraphProfiles[HistoryView.TYPE_GLUCOSE] =
                    mGraphs[GRAPH_TYPE_TOP].new Profile();
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(getResources().getColor(R.color.blue));
            mGraphProfiles[HistoryView.TYPE_GLUCOSE].setPaint(paint);
        }

        if (mGestureGraph == null) {
            mGestureGraph = new GestureDetector(getActivity(),
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
                            } else if (e2.getX() -
                                    e1.getX() > VERTICAL_MIN_DISTANCE &&
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


    private void queryHistory(final DateTime dateTime) {
        if (mIsHistoryQuerying) {
            return;
        }

        mLog.Debug(getClass(), "Query history");

        mIsHistoryQuerying = true;
        History history = new History(new DateTime(dateTime.getByteArray()),
                new Status(-1), new Event(-1, -1, -1));
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


    private ArrayList<String> getRangeTexts(RectF range, int ratio,
                                            int division, DecimalFormat decimalFormat) {
        float step;
        float value;

        ArrayList<String> texts = new ArrayList<>();

        if (range != null) {
            step = (range.top - range.bottom) / division;
            value = range.bottom;

            for (int i = 0; i <= division; i++) {
                texts.add(decimalFormat.format(value / ratio));
                value += step;
            }
        }

        return texts;
    }


    private void calibrateRange(RectF range, float gain, int ratio) {
        float rangeTop;
        float rangeBottom;
        float rangeHeight;

        rangeHeight = (range.top - range.bottom) * gain;
        rangeTop = ((range.top + range.bottom) / 2.0f) + (rangeHeight / 2.0f);
        rangeBottom =
                ((range.top + range.bottom) / 2.0f) - (rangeHeight / 2.0f);

        if (rangeBottom < 0.0f) {
            rangeBottom = 0.0f;
        }

        range.top = (float) (((int) rangeTop / ratio) * ratio);
        range.bottom = (float) (((int) rangeBottom / ratio) * ratio);

        if (range.top <= GRAPH_DIVISION_VERTICAL * 2 * ratio) {
            range.top = GRAPH_DIVISION_VERTICAL * 2 * ratio;
        }
    }


    private void updateGraphProfiles() {
        RectF range[] = new RectF[HistoryView.COUNT_TYPE];
        ArrayList<ArrayList<PointF>> dataList = new ArrayList<>();

        for (int i = 0; i < HistoryView.COUNT_TYPE; i++) {
            range[i] = new RectF((float) mGraphTimeBegin, -1.0f,
                    (float) mGraphTimeEnd, -1.0f);
            dataList.add(new ArrayList<PointF>());
        }

        for (int i = mHistoryModel.getCount() - 1; i >= 0; i--) {
            History history = mHistoryModel.getHistory(i);
            Status status = history.getStatus();
            int value = status.getShortValue1() & 0xFFFF;
            value /= 10;

            DateTime dateTime = history.getDateTime();
        }

        for (int i = 0; i <= HistoryView.TYPE_GLUCOSE; i++) {
            if ((range[i].top - range[i].bottom) <= 0.0f) {
                range[i].top = range[i].top * (1.0f + (GRAPH_RANGE_GAIN / 10));
                range[i].bottom =
                        range[i].bottom * (1.0f - (GRAPH_RANGE_GAIN / 10));
            }

            if (range[i].top < 0.0f) {
                range[i].top = 0.0f;
            }

            if (range[i].bottom < 0.0f) {
                range[i].bottom = 0.0f;
            }

            calibrateRange(range[i], GRAPH_RANGE_GAIN, 1);
            mGraphProfiles[i].setData(dataList.get(i));
            mGraphProfiles[i].setRange(range[i]);
        }
    }


    private void updateGraphs() {
        mGraphs[GRAPH_TYPE_TOP].setProfiles(null);
        mGraphs[GRAPH_TYPE_TOP].getCoordinates(WidgetGraph.POSITION_LEFT).get(0)
                .setTexts(null);
        mGraphs[GRAPH_TYPE_TOP].getCoordinates(WidgetGraph.POSITION_TOP).get(0)
                .setTexts(null);
        mGraphs[GRAPH_TYPE_TOP].getCoordinates(WidgetGraph.POSITION_RIGHT)
                .get(0).setTexts(null);
        ArrayList<WidgetGraph.Profile> graphProfiles = new ArrayList<>();
        ArrayList<String> coordinateTexts = new ArrayList<>();
        DecimalFormat decimalFormat = new DecimalFormat();
        int scaleDivision;

        scaleDivision = GRAPH_DIVISION_VERTICAL * 2;

        if (((ActivityPDA) getActivity()).getGlucoseUnit() == GLUCOSE_UNIT_MMOL) {
            decimalFormat.applyPattern("0.0");
            mGraphs[GRAPH_TYPE_TOP].getCoordinates(WidgetGraph.POSITION_LEFT)
                    .get(0)
                    .setTexts(getRangeTexts(
                            mGraphProfiles[HistoryView.TYPE_GLUCOSE].getRange(), 10,
                            scaleDivision, decimalFormat));
        } else {
            decimalFormat.applyPattern("0");
            mGraphs[GRAPH_TYPE_TOP].getCoordinates(WidgetGraph.POSITION_LEFT)
                    .get(0)
                    .setTexts(getRangeTexts(
                            mGraphProfiles[HistoryView.TYPE_GLUCOSE].getRange(), 1,
                            scaleDivision, decimalFormat));
        }

        graphProfiles.add(mGraphProfiles[HistoryView.TYPE_GLUCOSE]);
        coordinateTexts.add(getResources().getString(R.string.history_bg) +
                " (" + getResources().getString(R.string.unit_mmol_l) + ")");
        coordinateTexts.add("");
        coordinateTexts.add("");
        coordinateTexts.add("");
        coordinateTexts.add("");
        coordinateTexts.add(((ActivityPDA) getActivity())
                .getDateString(mDateTime.getCalendar().getTimeInMillis(), null));
        coordinateTexts.add("");

        if (graphProfiles.size() > 0) {
            WidgetGraph.Coordinate coordinate = mGraphs[GRAPH_TYPE_TOP]
                    .getCoordinates(WidgetGraph.POSITION_TOP).get(0);
            coordinate.setTexts(coordinateTexts);
            coordinate.setOffset(20);
            mGraphs[GRAPH_TYPE_TOP].setProfiles(graphProfiles);
        }

        mGraphs[GRAPH_TYPE_TOP].getGrids()
                .get(WidgetGraph.Grid.STYLE_HORIZONTAL).setDivision(scaleDivision);
        int timeSegment =
                (mGraphTimeEnd - mGraphTimeBegin) / GRAPH_DIVISION_HORIZONTAL;
        int timeOffset = timeSegment - (mGraphTimeBegin % timeSegment);

        if (timeOffset >= timeSegment) {
            timeOffset = 0;
        }

        for (int i = 0; i < COUNT_GRAPH_TYPE; i++) {
            int width = mRootView.findViewById(VIEW_ID_GRAPH[i]).getWidth();
            int graphOffset = 0;

            if (width > 0) {
                graphOffset =
                        ((width - GRAPH_MARGIN_LEFT - GRAPH_MARGIN_RIGHT) *
                                timeOffset) / (mGraphTimeEnd - mGraphTimeBegin);
            }

            if (i == GRAPH_TYPE_TIMELINE) {
                int timelineBegin = mGraphTimeBegin + timeOffset;
                coordinateTexts = new ArrayList<>();

                for (int j = timelineBegin; j <= mGraphTimeEnd; j +=
                        timeSegment) {
                    coordinateTexts.add(((ActivityPDA) getActivity())
                            .getTimeString(j * DateTime.MILLISECOND_PER_SECOND,
                                    new SimpleTimeZone(0, "UTC")));
                }

                if (timeOffset > 0) {
                    coordinateTexts.add("");
                }

                WidgetGraph.Coordinate coordinate = mGraphs[i]
                        .getCoordinates(WidgetGraph.POSITION_BOTTOM).get(0);
                coordinate.setTexts(coordinateTexts);
                coordinate.setOffset(graphOffset);
            } else {
                WidgetGraph.Grid grid =
                        mGraphs[i].getGrids().get(WidgetGraph.Grid.STYLE_VERTICAL);
                grid.setOffset(graphOffset);
                grid.setDivision(GRAPH_DIVISION_HORIZONTAL);
            }

            WidgetGraph widgetGraph =
                    (WidgetGraph) mRootView.findViewById(VIEW_ID_GRAPH[i]);
            widgetGraph.setWidget(mGraphs[i]);
            widgetGraph.updateWidget();

            if (i != GRAPH_TYPE_TIMELINE) {
                if (widgetGraph.getProfiles() == null) {
                    widgetGraph.setVisibility(View.GONE);
                } else {
                    widgetGraph.setVisibility(View.VISIBLE);
                }
            }

            widgetGraph.invalidate();
        }
    }


    private void adjustDateTime(long offset) {
        long time = mDateTime.getCalendar().getTimeInMillis() + offset;

        if (time <= System.currentTimeMillis()) {
            Calendar calendar = mDateTime.getCalendar();
            calendar.setTimeInMillis(time);
            mDateTime.setCalendar(calendar);
//            queryHistory(mDateTime);
        }
    }
}
