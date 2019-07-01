package com.microtechmd.pda.ui.activity.fragment;


import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.database.DataSetHistory;
import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.ParameterSystem;
import com.microtechmd.pda.library.entity.ValueInt;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.utility.ByteUtil;
import com.microtechmd.pda.library.utility.SPUtils;
import com.microtechmd.pda.ui.activity.ActivityMain;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.ui.widget.countdownview.CountdownView;
import com.microtechmd.pda.util.ToastUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.Context.ALARM_SERVICE;
import static com.microtechmd.pda.ui.activity.ActivityPDA.DATE_CHANGE;
import static com.microtechmd.pda.ui.activity.ActivityPDA.GLUCOSE;
import static com.microtechmd.pda.ui.activity.ActivityPDA.GLUCOSE_INVALID;
import static com.microtechmd.pda.ui.activity.ActivityPDA.GLUCOSE_RECOMMEND_CAL;
import static com.microtechmd.pda.ui.activity.ActivityPDA.HYPER;
import static com.microtechmd.pda.ui.activity.ActivityPDA.HYPO;
import static com.microtechmd.pda.ui.activity.ActivityPDA.IMPENDANCE;
import static com.microtechmd.pda.ui.activity.ActivityPDA.RFSIGNAL;
import static com.microtechmd.pda.ui.activity.ActivityPDA.SENSOR_ERROR;
import static com.microtechmd.pda.ui.activity.ActivityPDA.SENSOR_EXPIRATION;
import static com.microtechmd.pda.ui.activity.ActivityPDA.SENSOR_NEW;
import static com.microtechmd.pda.ui.activity.fragment.FragmentSettings.REALTIMEFLAG;


public class FragmentHome extends FragmentBase
        implements EntityMessage.Listener {
    private static final String STRING_UNKNOWN = "-.-";
    private static final String TAG_GRAPH = "graph";
    public static final int TIME_DATA_6 = 6;
    public static final int TIME_DATA_12 = 12;
    public static final int TIME_DATA_24 = 24;
    private static final int GLUCOSE_DISPLAY_TIMEOUT = 15 * 60 * 1000;

    private boolean expirtionFlag = false;
    private View mRootView = null;

    //    private FragmentNewGraph fragmentNewGraph = null;
//    private FragmentCombinedGraph fragmentNewGraph = null;
    private FragmentHelloChartsGraph fragmentNewGraph = null;

    private TextView textView_t;
    private TextView tv_minutes;
    private TextView textView_g;
    private TextView textView_g_recommend_cal;
    private ImageView textView_g_err;
    private TextView textView_unit;
    private TextView text_view_countdown;
    private CountdownView countdownView;
    private RadioGroup radio_group;
    private ImageView synchronize;
    private TextView tv_mode;

    private Handler handler;
    private CountDownTimer countDownTimer;
    private Runnable runnable;
//    private PendingIntent sender;
//    private AlarmManager manager;

    private History mHistory;

    private boolean firstLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_home, container, false);
        textView_g = (TextView) mRootView.findViewById(R.id.text_view_glucose);
        textView_g_recommend_cal = (TextView) mRootView.findViewById(R.id.text_view_glucose_recommend);
        textView_g_err = (ImageView) mRootView.findViewById(R.id.glucose_err);
        textView_t = (TextView) mRootView.findViewById(R.id.text_view_date_time);
        tv_minutes = (TextView) mRootView.findViewById(R.id.tv_minutes);
        textView_unit = (TextView) mRootView.findViewById(R.id.text_view_unit);
        text_view_countdown = (TextView) mRootView.findViewById(R.id.text_view_countdown);
        countdownView = (CountdownView) mRootView.findViewById(R.id.countdown_view);
        synchronize = (ImageView) mRootView.findViewById(R.id.synchronize);
        tv_mode = (TextView) mRootView.findViewById(R.id.tv_mode);
        if (fragmentNewGraph == null) {
            fragmentNewGraph = new FragmentHelloChartsGraph();
        }
        getChildFragmentManager().beginTransaction()
                .add(R.id.layout_graph, fragmentNewGraph, TAG_GRAPH).show(fragmentNewGraph).commit();
//        updateStatus(ActivityMain.getStatus());
        radio_group = (RadioGroup) mRootView.findViewById(R.id.radio_group);
        radio_group.check(R.id.btn_0);
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
        setCountDownVisible(false);
        countdownView.setOnCountdownEndListener(new CountdownView.OnCountdownEndListener() {
            @Override
            public void onEnd(CountdownView cv) {
                setCountDownVisible(false);
            }
        });

        textView_g.setTextColor(getResources().getColor(R.color.green));
        setDisplayUnKnow();
        setTimeVisible("");
        handler = new Handler();

        boolean realtimeFlag = (boolean) SPUtils.get(getActivity(), REALTIMEFLAG, true);
        if (realtimeFlag) {
            synchronize.setVisibility(View.INVISIBLE);
            tv_mode.setVisibility(View.INVISIBLE);
        } else {
            synchronize.setVisibility(View.VISIBLE);
            tv_mode.setVisibility(View.VISIBLE);
        }
        synchronize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int rfsignal = (int) SPUtils.get(getActivity(), RFSIGNAL, 0);
                if (rfsignal == 0) {
                    ToastUtils.showToast(getActivity(), R.string.connect_fail);
//                    Toast.makeText(getActivity(),
//                            getActivity().getResources().getString(R.string.connect_fail),
//                            Toast.LENGTH_SHORT)
//                            .show();
                } else {
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
        });
        firstLauncher = true;
        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_MONITOR, this);
        ((ApplicationPDA) getActivity().getApplication())
                .registerMessageListener(ParameterGlobal.PORT_COMM, this);
//        getChildFragmentManager().beginTransaction()
//                .replace(R.id.layout_graph, fragmentNewGraph, TAG_GRAPH).commit();
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
        ((ApplicationPDA) getActivity().getApplication())
                .unregisterMessageListener(ParameterGlobal.PORT_COMM, this);
        super.onDestroyView();
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    public void setDateChange() {
        if (fragmentNewGraph != null) {
            fragmentNewGraph.setDataChange();
            firstLauncher = true;
            boolean dateChange = (boolean) SPUtils.get(app, DATE_CHANGE, false);
            if (dateChange) {
                setTimeVisible("");
            }
        }
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
                tv_mode.setVisibility(View.VISIBLE);
                if (text_view_countdown.getVisibility() == View.GONE) {
                    setTimeVisible("");
                    setGlucoseVisible(true);
                    setDisplayUnKnow();
                    textView_g_recommend_cal.setVisibility(View.GONE);
                }
                firstLauncher = true;
                fragmentNewGraph.firstRefresh();
            } else {
                synchronize.setVisibility(View.INVISIBLE);
                tv_mode.setVisibility(View.INVISIBLE);
            }
        }
        if (message.getParameter() == ParameterMonitor.COUNTDOWNVIEW_VISIBLE) {
            if (message.getData() != null) {
                mHistory = null;
                int value = ByteUtil.bytesToInt(message.getData());
                setCountdownViewVisible(value);
//                setGlucoseVisible(true);
            }
        }
        if (message.getParameter() == ParameterMonitor.GLUCOSE_DISPLAY) {
            boolean realtimeFlag = (boolean) SPUtils.get(getActivity(), REALTIMEFLAG, true);
            if (!realtimeFlag) {
                return;
            }
            if (mHistory != null) {
                long space = System.currentTimeMillis() - mHistory.getDateTime().getCalendar().getTimeInMillis();
                int minute = (int) (space / 60000);
                mLog.Error(getClass(), "系统时间： " + minute);
                if (glucoseState == 1) {
                    return;
                }
                if (minute < -1) {
                    setTimeVisible("");
                    setDisplayUnKnow();
                } else if (minute <= 0) {
                    setTimeVisible(String.valueOf(0));
                } else if (minute < 15) {
                    setTimeVisible(String.valueOf(minute));
                } else {
                    if (textView_g_err.getVisibility() == View.VISIBLE) {
                        setTimeVisible(String.valueOf(minute));
                        return;
                    }
                    setTimeVisible("");
                    setDisplayUnKnow();
                    textView_g_recommend_cal.setVisibility(View.GONE);
                    fragmentNewGraph.firstRefresh();
                }
            }
        }
        if (message.getParameter() == ParameterComm.RESET_DATA) {
            switch (message.getData()[0]) {
                case 0:
                case 3:
                    setDisplayUnKnow();
                    setTimeVisible("");
                    textView_g_recommend_cal.setVisibility(View.GONE);
                    mHistory = null;
                    glucoseState = 0;
                    break;
                case 1:
                    radio_group.check(R.id.btn_1);
                    radio_group.check(R.id.btn_0);
                    break;
                default:

                    break;
            }
        }
    }

    private void setDisplayUnKnow() {
        textView_g.setText(STRING_UNKNOWN);
        textView_unit.setText(R.string.unit_mmol_l);
        ActivityMain.setStatus(null);
    }

    public void setCountdownViewVisible(int value) {
        if (value == 0) {
            setCountDownVisible(false);
        } else {
            if (value < 0) {
                return;
            }
            setCountDownVisible(true);
            if (((value * 60 - countdownView.getRemainTime() / 1000) < -30) ||
                    ((value * 60 - countdownView.getRemainTime() / 1000) > 90)) {
                if (value == 60) {
                    value++;
                }
                countdownView.start(value * 60 * 1000);
                countdownView.restart();
            }
            History history = new History();
            Event event = new Event();
            event.setEvent(SENSOR_NEW);
            Status status = new Status();
            status.setShortValue1((int) (countdownView.getRemainTime() / 1000 / 60));
            history.setEvent(event);
            history.setStatus(status);
            ActivityMain.setStatus(history);
        }
    }

    private void handleNotification(final EntityMessage message) {
        //通信异常清除显示
        if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
                (message.getParameter() == ParameterComm.COMM_ERR)) {
            if (glucoseState == 2) {
                mHistory = null;
                setTimeVisible("");
                setGlucoseVisible(true);
                setDisplayUnKnow();
            }
        }

        if (message.getSourcePort() == ParameterGlobal.PORT_MONITOR) {
            switch (message.getParameter()) {
                case ParameterMonitor.PARAM_STATUS:
                    if (message.getData() != null) {
                        DataList dataList = new DataList(message.getData());
                        if (dataList.getCount() > 0) {
                            History history = new History(dataList.getData(dataList.getCount() - 1));
                            updateStatus(history);
                            boolean a = (history.getEvent().getEventFlag() & 0x40) == 0;
                            mLog.Error(getClass(), "广播包血糖：" + (history.getStatus().getShortValue1()) +
                                    "valueflag：" + a + "enventype:" + history.getEvent().getEvent());
                            mLog.Error(getClass(), "广播包血糖：" + Arrays.toString(history.getByteArray()));
                        }
                    }
                    break;
                case ParameterMonitor.PARAM_HISTORY:
//                    if ((message.getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_CONTROL) ||
//                            (message.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER)) {
//                        History historyLast = ActivityMain.getStatus();
//                        if (historyLast == null) {
//                            return;
//                        }
//                        if (message.getData() != null) {
//                            final DataList dataList = new DataList(message.getData());
//                            if (dataList.getCount() > 0) {
//                                //同步比广播包时间新则取同步数据显示
//                                History history = new History(dataList.getData(dataList.getCount() - 1));
//                                if (history.getDateTime().getBCD() > historyLast.getDateTime().getBCD()) {
//                                    updateStatus(history);
//                                    boolean a = (history.getEvent().getEventFlag() & 0x40) == 0;
//                                    mLog.Error(getClass(), "同步血糖：" + (history.getStatus().getShortValue1()) +
//                                            "valueflag：" + a + "enventype:" + history.getEvent().getEvent());
//                                    mLog.Error(getClass(), "同步血糖：" + Arrays.toString(history.getByteArray()));
//                                }
//                            }
//                        }
//                    }
                    break;
                default:

                    break;
            }

        }
//        if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
//                (message.getParameter() == ParameterMonitor.PARAM_HISTORY)) {
//            if (message
//                    .getSourceAddress() != ParameterGlobal.ADDRESS_LOCAL_MODEL) {
//                if (message.getData() != null) {
//                    DataList dataList = new DataList(message.getData());
//                    for (int i = 0; i < dataList.getCount(); i++) {
//                        History history = new History(dataList.getData(i));
//                        Event event = history.getEvent();
//                        long timeChange = System.currentTimeMillis() - history.getDateTime().getCalendar().getTimeInMillis();
//                        if (Math.abs(timeChange) > 15 * 60 * 1000) {
//                            continue;
//                        }
//                        switch (event.getEvent()) {
//                            case SENSOR_ERROR:
//                                textView_g_err.setBackgroundResource(R.drawable.glucose_err);
//                                setGlucoseVisible(false);
//                                expirtionFlag = false;
//                                break;
//                            case SENSOR_EXPIRATION:
//                                textView_g_err.setBackgroundResource(R.drawable.expirtion_err);
//                                setGlucoseVisible(false);
//                                expirtionFlag = true;
//                                break;
//                            case GLUCOSE:
//                            case GLUCOSE_RECOMMEND_CAL:
//                                if (expirtionFlag) {
//                                    setGlucoseVisible(true);
//                                } else if ((history.getEvent().getEventFlag() & 0x40) != 0) {
//                                    setGlucoseVisible(true);
//                                }
//                                break;
//                            case GLUCOSE_INVALID:
//                            case HYPO:
//                            case HYPER:
//                            case IMPENDANCE:
//                                if (expirtionFlag) {
//                                    setGlucoseVisible(true);
//                                }
//                                break;
//                            default:
//
//                                break;
//                        }
//                    }
//                }
//            }
//        }
    }

    private int glucoseState = 0;

    @SuppressLint("SetTextI18n")
    private void updateStatus(History history) {
        mLog.Error(getClass(), "显示广播包血糖值");
        if (history != null) {
            DateTime statusTime = history.getDateTime();
//            DateTime nowTime = new DateTime(Calendar.getInstance());
//            long time_space = nowTime.getCalendar().getTimeInMillis() - statusTime.getCalendar().getTimeInMillis();
            long time_space = System.currentTimeMillis() - statusTime.getCalendar().getTimeInMillis();
            boolean a = (history.getEvent().getEventFlag() & 0x40) == 0;
//            mLog.Error(getClass(), "血糖：" + (history.getStatus().getShortValue1()) + "valueflag：" + a + "enventype:" + history.getEvent().getEvent());
            if (time_space / 1000 >= -60) {
                if (history.getEvent().getEvent() == SENSOR_EXPIRATION) {
//                    ActivityMain.setStatus(history);
                    textView_g_err.setBackgroundResource(R.drawable.expirtion_err);
                    //判断血糖时间
//                    long space = System.currentTimeMillis() - history.getDateTime().getCalendar().getTimeInMillis();
//                    int minute = (int) (space / 60000);
                    setTimeVisible("");
                    textView_unit.setText(R.string.alarm_expiration);
                    glucoseState = 1;
                    setGlucoseVisible(false);
//                    mHistory = history;
                    return;
                }
                textView_unit.setText(R.string.unit_mmol_l);
                if (Math.abs(time_space) < 15 * 60 * 1000) {
                    if ((history.getEvent().getEventFlag() & 0x20) != 0) {
                        if (glucoseState != 2) {
                            mHistory = history;
                            setDisplayUnKnow();
                            //判断血糖时间
                            long space = System.currentTimeMillis() - history.getDateTime().getCalendar().getTimeInMillis();
                            int minute = (int) (space / 60000);
                            setTimeVisible(String.valueOf(minute));
                            mLog.Error(getClass(), "故障广播包时间： " + minute);
                        }
                        textView_g_err.setBackgroundResource(R.drawable.glucose_err);
                        textView_unit.setText(R.string.alarm_sensor_error);
                        textView_g_recommend_cal.setVisibility(View.GONE);
                        setGlucoseVisible(false);
                        glucoseState = 2;
                        return;
                    } else {
                        setGlucoseVisible(true);
                        textView_g.setTextColor(getResources().getColor(R.color.green));
                    }

                    mHistory = history;
                    glucoseState = 0;
                    switch (history.getEvent().getEvent()) {
                        case SENSOR_ERROR:
                            setDisplayUnKnow();
                            textView_g_recommend_cal.setVisibility(View.GONE);
                            break;
                        case GLUCOSE:
                        case GLUCOSE_RECOMMEND_CAL:
                        case GLUCOSE_INVALID:
                        case HYPO:
                        case HYPER:
                        case IMPENDANCE:
                            if (a) {
                                switch (history.getStatus().getShortValue1()) {
                                    case 0:
                                        textView_g.setText(R.string.low);
                                        break;
                                    case 255:
                                        textView_g.setText(R.string.high);
                                        break;
                                    default:
                                        textView_g.setTextColor(getResources().getColor(R.color.green));
                                        textView_g.setText(((ActivityPDA) getActivity())
                                                .getGlucoseValue((history.getStatus().getShortValue1() & 0xFFFF) *
                                                        ActivityPDA.GLUCOSE_UNIT_MG_STEP, false).trim());
                                        break;
                                }
                                setGlucoseVisible(true);
                                ActivityMain.setStatus(history);
                                //判断血糖时间
                                long space = System.currentTimeMillis() - history.getDateTime().getCalendar().getTimeInMillis();
                                int minute = (int) (space / 60000);
                                setTimeVisible(String.valueOf(minute));

                                if ((history.getEvent().getEventFlag() & 0x80) != 0) {
                                    textView_g_recommend_cal.setVisibility(View.VISIBLE);
                                } else {
                                    textView_g_recommend_cal.setVisibility(View.GONE);
                                }
                            } else {
                                setDisplayUnKnow();
                                textView_g_recommend_cal.setVisibility(View.GONE);
//                                textView_t.setText("");
                            }
                            break;
                        default:
                            textView_g_recommend_cal.setVisibility(View.GONE);
                            break;
                    }
//                    if ((history.getEvent().getEventFlag() & 0x20) != 0) {
//                        if (textView_g_err.getVisibility() == View.GONE) {
//                            setDisplayUnKnow();
//                            //判断血糖时间
//                            long space = System.currentTimeMillis() - history.getDateTime().getCalendar().getTimeInMillis();
//                            int minute = (int) (space / 60000);
//                            setTimeVisible(String.valueOf(minute));
//                            mLog.Error(getClass(), "故障广播包时间： " + minute);
//                        }
//                        textView_g_err.setBackgroundResource(R.drawable.glucose_err);
//                        textView_unit.setText(R.string.alarm_sensor_error);
//                        textView_g_recommend_cal.setVisibility(View.GONE);
//                        setGlucoseVisible(false);
//                    } else {
//                        setGlucoseVisible(true);
//                        textView_g.setTextColor(getResources().getColor(R.color.green));
//                    }
                    if (firstLauncher) {
                        firstLauncher = false;
                        fragmentNewGraph.firstRefresh();
                    }
                }
            }
        }
    }

    private void setCountDownVisible(boolean countDownVisible) {
        if (countDownVisible) {
            textView_g.setVisibility(View.GONE);
            textView_t.setVisibility(View.GONE);
            tv_minutes.setVisibility(View.GONE);
            textView_g_err.setVisibility(View.GONE);
            textView_unit.setVisibility(View.GONE);
            textView_g_recommend_cal.setVisibility(View.GONE);

            text_view_countdown.setVisibility(View.VISIBLE);
            countdownView.setVisibility(View.VISIBLE);
        } else {
            textView_g.setVisibility(View.VISIBLE);
            setTimeVisible("");
            textView_g_err.setVisibility(View.GONE);
            textView_unit.setVisibility(View.VISIBLE);

            text_view_countdown.setVisibility(View.GONE);
            countdownView.setVisibility(View.GONE);
        }
    }

    public void setGlucoseVisible(boolean glucoseVisible) {
        if (glucoseVisible) {
            textView_g.setVisibility(View.VISIBLE);
            textView_g_err.setVisibility(View.GONE);
        } else {
            textView_g.setVisibility(View.GONE);
            textView_g_recommend_cal.setVisibility(View.GONE);
            textView_g_err.setVisibility(View.VISIBLE);
        }
    }

    private void setTimeVisible(String time) {
        textView_t.setVisibility(View.VISIBLE);
        textView_t.setText(time);
        if (TextUtils.isEmpty(time)) {
            tv_minutes.setVisibility(View.GONE);
        } else {
            if (Integer.parseInt(time) > 60) {
                tv_minutes.setVisibility(View.GONE);
                textView_t.setText(R.string.more_hour);
                return;
            }
            tv_minutes.setVisibility(View.VISIBLE);
        }
    }
//
//    private FragmentDialog showDialogConfirm(String title,
//                                             String buttonTextPositive, String buttonTextNegative, Fragment content,
//                                             boolean isCancelable, FragmentDialog.ListenerDialog listener) {
//        FragmentDialog fragmentDialog = new FragmentDialog();
//        fragmentDialog.setTitle(title);
//        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_POSITIVE,
//                buttonTextPositive);
//        fragmentDialog.setButtonText(FragmentDialog.BUTTON_ID_NEGATIVE,
//                buttonTextNegative);
//        fragmentDialog.setContent(content);
//        fragmentDialog.setCancelable(isCancelable);
//        fragmentDialog.setListener(listener);
//        fragmentDialog.show(getChildFragmentManager(), null);
//        return fragmentDialog;
//    }
}
