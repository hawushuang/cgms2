package com.microtechmd.pda.ui.activity.fragment;


import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.microtechmd.pda.R;

import java.util.ArrayList;
import java.util.List;


public class FragmentChart extends FragmentBase {

    private View mRootView;

    private LineChart mLineChart;

    protected String[] values;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_chart, container, false);
        mLineChart = (LineChart) mRootView.findViewById(R.id.chart);

        //显示边界
        mLineChart.setDrawBorders(true);
        mLineChart.setDescription(null);
        mLineChart.setScaleYEnabled(false);

        LimitLine ll1_h = new LimitLine(16f, "高血糖");
        ll1_h.setTextColor(Color.RED);
        ll1_h.enableDashedLine(4, 2, 1);
//        ll1_h.setLineWidth(2f);
//        ll1_h.setLineColor(Color.rgb(0,0,0));
//        ll1_h.enableDashedLine(10f, 10f, 0f);
        ll1_h.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll1_h.setTextSize(15f);

        LimitLine ll1_l = new LimitLine(5f, "低血糖");
        ll1_l.setTextColor(Color.RED);
        ll1_l.enableDashedLine(4, 2, 1);
//        ll1_l.setLineWidth(2f);
//        ll1_l.setLineColor(Color.rgb(0,0,0));
//        ll1_l.enableDashedLine(10f, 10f, 0f);
        ll1_l.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll1_l.setTextSize(15f);


        //设置数据
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            entries.add(new Entry(i, (float) (Math.random()) * 25));
        }
        //一个LineDataSet就是一条线
        LineDataSet lineDataSet = new LineDataSet(entries, "");
//        lineDataSet.setCircleSize(4);
        lineDataSet.setCircleRadius(3f);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setHighlightEnabled(false);
        lineDataSet.setValueTextColor(Color.WHITE);
//        LineData data = new LineData(lineDataSet);
//        mLineChart.setData(data);
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mLineChart.setData(data);
        mLineChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addEntry();
            }
        });
        mLineChart.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mLineChart.clearValues();
                return false;
            }
        });
        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setAvoidFirstLastClipping(true);
//        FormatterUtil formatterUtil = new FormatterUtil(values);
//        xAxis.setLabelCount(12);
//        xAxis.setValueFormatter(formatterUtil);

        YAxis leftYAxis = mLineChart.getAxisLeft();
        YAxis rightYAxis = mLineChart.getAxisRight();
        rightYAxis.setEnabled(false);
//        rightYAxis.setDrawAxisLine(true);
//        rightYAxis.setDrawGridLines(false);
//        rightYAxis.setTextColor(Color.TRANSPARENT);
//        rightYAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);

        leftYAxis.setTextColor(Color.WHITE);
        leftYAxis.setStartAtZero(true);
        //重置所有限制线,以避免重叠线
        leftYAxis.removeAllLimitLines();
        leftYAxis.addLimitLine(ll1_h);
        leftYAxis.addLimitLine(ll1_l);
//        leftYAxis.setAxisMaxValue(30);
        leftYAxis.setAxisMaximum(30);

        return mRootView;
    }

    private void addEntry() {

        LineData data = mLineChart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), (float) (Math.random() * 25)), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            mLineChart.notifyDataSetChanged();

            // limit the number of visible entries
//            mLineChart.setVisibleXRangeMaximum(120);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
//            mLineChart.moveViewToX(data.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Dynamic Data");
//        set.setAxisDependency(YAxis.AxisDependency.LEFT);
//        set.setColor(ColorTemplate.getHoloBlue());
//        set.setCircleColor(Color.WHITE);
//        set.setLineWidth(2f);
//        set.setCircleRadius(4f);
//        set.setFillAlpha(65);
//        set.setFillColor(ColorTemplate.getHoloBlue());
//        set.setHighLightColor(Color.rgb(244, 117, 117));
//        set.setValueTextColor(Color.WHITE);
//        set.setValueTextSize(9f);
//        set.setDrawValues(false);
        set.setCircleRadius(3f);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setHighlightEnabled(false);
        set.setValueTextColor(Color.WHITE);
        return set;
    }
}
