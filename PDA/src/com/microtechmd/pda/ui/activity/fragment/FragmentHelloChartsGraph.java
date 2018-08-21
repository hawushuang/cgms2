package com.microtechmd.pda.ui.activity.fragment;


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.monitor.DateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

public class FragmentHelloChartsGraph extends FragmentBase {
    private static final long millisecond_1 = (long) (DateTime.MILLISECOND_PER_SECOND *
            DateTime.SECOND_PER_MINUTE * DateTime.MINUTE_PER_HOUR);
    private static final long millisecond_2 = millisecond_1 * 2;
    private static final long millisecond_4 = millisecond_1 * 4;
    private View mRootView;

    private LineChartView chart;
    private LineChartData data;
    private float minY = 0f;//Y轴坐标最小值
    private float maxY = 30f;//Y轴坐标最大值
    private List<AxisValue> mAxisYValues = new ArrayList<>();//Y轴坐标值
    private float max = 1000f;
    private float step = 1;

    public void setTimeData(int timeData) {
        switch (timeData) {
            case FragmentHome.TIME_DATA_6:
                step = 1;
                max += 100;
                generateData();
                break;
            case FragmentHome.TIME_DATA_12:
                step = 2;
                max += 100;
                generateData();
                break;
            case FragmentHome.TIME_DATA_24:
                step = 4;
                max += 100;
                generateData();
                break;
            default:
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_hellochartsgraph, container, false);
        chart = (LineChartView) mRootView.findViewById(R.id.chart);
        chart.setInteractive(true);
        chart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        chart.setZoomEnabled(false);
        chart.setMaxZoom(8000000f);

        for (float i = minY; i <= maxY; i += 5) {
            mAxisYValues.add(new AxisValue(i).setLabel((int) i + ""));
        }
        generateData();
        return mRootView;
    }

    private void generateData() {
        long millisecond_now = System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        calendar.set(Calendar.HOUR_OF_DAY, hour + 2);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String format = formatter.format(new Date(calendar.getTimeInMillis()));

        List<Line> lines = new ArrayList<>();
        List<PointValue> values = new ArrayList<>();
        for (float i = 0f; i < max; i += 0.1f) {
            values.add(new PointValue(i, 5 + (float) (Math.random() * 1f)));
        }
        Line glucoseLine = getGlucoseLine(values);
        Line lowLine = getLimitLine("low");
        Line highLine = getLimitLine("high");

        lines.add(glucoseLine);
        lines.add(lowLine);
        lines.add(highLine);
        data = new LineChartData(lines);


        Axis axisX = new Axis().setHasLines(true);
        axisX.setLineColor(Color.GRAY);
        List<AxisValue> xValues = new ArrayList<>();
        for (float value = 0; value <= max + 1; value += step) {
            AxisValue axisValue = new AxisValue(value).setLabel((int) value + "");
            xValues.add(axisValue);
        }
        axisX.setValues(xValues);
        axisX.setMaxLabelChars(0);

        Axis axisY = new Axis();
        axisY.setHasLines(false);
        axisY.setValues(mAxisYValues);

        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
//        data.setBaseValue(Float.NEGATIVE_INFINITY);

        chart.setLineChartData(data);


        Viewport v = new Viewport(chart.getMaximumViewport());
        v.bottom = minY;
        v.top = maxY;
        //固定Y轴的范围,如果没有这个,Y轴的范围会根据数据的最大值和最小值决定
        chart.setMaximumViewport(v);
//        //这2个属性的设置一定要在lineChart.setMaximumViewport(v);这个方法之后,不然显示的坐标数据是不能左右滑动查看更多数据的
        v.left = 0;
        v.right = step * 6;

        chart.setCurrentViewport(v);
    }


    @NonNull
    private Line getGlucoseLine(List<PointValue> values) {
        Line line = new Line(values);
        line.setPointColor(Color.parseColor("#FF00DEFF"));
        line.setPointRadius(2);
        line.setHasLines(false);
        return line;
    }

    private Line getLimitLine(String limit) {
        Line line = new Line();
        List<PointValue> valuesLimit = new ArrayList<>();
        line.setHasPoints(false);
        line.setHasLines(true);
        line.setFilled(true);
        line.setCubic(false);
        line.setStrokeWidth(0);
        switch (limit) {
            case "low":
                valuesLimit.add(new PointValue(-2f, 3.5f));
                valuesLimit.add(new PointValue(max + 1, 3.5f));
                line.setAreaTransparency(120);
                line.setColor(Color.BLACK);
                break;
            case "high":
                valuesLimit.add(new PointValue(-2f, 12f));
                valuesLimit.add(new PointValue(max + 1, 12f));
                line.setColor(Color.LTGRAY);
                break;
            default:

                break;
        }
        line.setValues(valuesLimit);
        return line;
    }
}
