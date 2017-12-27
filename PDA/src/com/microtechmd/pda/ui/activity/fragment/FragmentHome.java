package com.microtechmd.pda.ui.activity.fragment;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.StatusPump;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.ui.activity.ActivityMain;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.ui.widget.countdownview.CountdownView;

import java.util.Calendar;


public class FragmentHome extends FragmentBase
        implements EntityMessage.Listener {
    private static final String STRING_UNKNOWN = "-.-";
    private static final String TAG_GRAPH = "graph";
    public static final int TIME_DATA_6 = 6;
    public static final int TIME_DATA_12 = 12;
    public static final int TIME_DATA_24 = 24;

    private View mRootView = null;
    private FragmentGraph mFragmentGraph = null;

    private FragmentNewGraph fragmentNewGraph = null;

    private CountdownView countdownView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_home, container, false);
        updateStatus(ActivityMain.getStatus());
        RadioGroup radio_group = (RadioGroup) mRootView.findViewById(R.id.radio_group);
        countdownView = (CountdownView) mRootView.findViewById(R.id.countdown_view);
        radio_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                switch (i) {
                    case R.id.btn_0:
                        fragmentNewGraph.setTimeData(TIME_DATA_6);
                        break;
                    case R.id.btn_1:
                        fragmentNewGraph.setTimeData(TIME_DATA_12);
                        break;
                    case R.id.btn_2:
                        fragmentNewGraph.setTimeData(TIME_DATA_24);
                        break;
                }
            }
        });

        countdownView.start(60 * 60 * 1000);
        countdownView.setOnCountdownEndListener(new CountdownView.OnCountdownEndListener() {
            @Override
            public void onEnd(CountdownView cv) {
                cv.setVisibility(View.GONE);
            }
        });
        return mRootView;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (fragmentNewGraph == null) {
            fragmentNewGraph = new FragmentNewGraph();
        }

        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_MONITOR, this);
        getChildFragmentManager().beginTransaction()
                .add(R.id.layout_graph, fragmentNewGraph, TAG_GRAPH).show(fragmentNewGraph).commit();
//        getChildFragmentManager().beginTransaction()
//                .replace(R.id.layout_graph, mFragmentChart, TAG_GRAPH).commit();
//        if (mFragmentGraph == null) {
//            mFragmentGraph = new FragmentGraph();
//        }
//        getChildFragmentManager().beginTransaction()
//                .replace(R.id.layout_graph, mFragmentGraph, TAG_GRAPH).commit();

//        ((ApplicationPDA) getActivity().getApplication())
//                .registerMessageListener(ParameterGlobal.PORT_MONITOR, this);

    }


    @Override
    public void onDestroyView() {
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_MONITOR, this);
        getChildFragmentManager().beginTransaction().remove(fragmentNewGraph);
        super.onDestroyView();
    }


    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onClick(View v) {
        super.onClick(v);

        switch (v.getId()) {
            default:
                break;
        }
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
        if (message.getSourcePort() == ParameterGlobal.PORT_MONITOR) {
            if (message.getParameter() == ParameterMonitor.PARAM_STATUS) {
                StatusPump status = new StatusPump(message.getData());
                updateStatus(status);
                ActivityMain.setStatus(status);
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private void updateStatus(StatusPump status) {
        if (status == null) {
            ((TextView) mRootView.findViewById(R.id.text_view_glucose))
                    .setText(STRING_UNKNOWN);
            ((TextView) mRootView.findViewById(R.id.text_view_date_time))
                    .setText("");
        } else {
            mLog.Debug(getClass(), "Update status: " + status.getBasalRate());

            TextView textView_g =
                    (TextView) mRootView.findViewById(R.id.text_view_glucose);
            DateTime nowTime = new DateTime(Calendar.getInstance());
            DateTime statusTime = status.getDateTime();
            if ((nowTime.getMinute() * 60 + nowTime.getSecond()) - (statusTime.getMinute() * 60 + statusTime.getSecond()) > 15 * 60) {
                textView_g.setText(STRING_UNKNOWN);
            } else {
                textView_g.setText(((ActivityPDA) getActivity())
                        .getGlucoseValue((status.getBasalRate() & 0xFFFF) *
                                ActivityPDA.GLUCOSE_UNIT_MG_STEP, false));
            }
            TextView textView_t = (TextView) mRootView.findViewById(R.id.text_view_date_time);
            textView_t.setText(((ActivityPDA) getActivity()).getDateString(
                    status.getDateTime().getCalendar().getTimeInMillis(),
                    null) + " " + ((ActivityPDA) getActivity()).getTimeString(
                    status.getDateTime().getCalendar().getTimeInMillis(),
                    null));
        }
    }
}
