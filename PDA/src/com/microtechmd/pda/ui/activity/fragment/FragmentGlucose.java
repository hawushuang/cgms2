package com.microtechmd.pda.ui.activity.fragment;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.microtechmd.pda.R;
import com.microtechmd.pda.ui.widget.RuleView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class FragmentGlucose extends FragmentBase {

    private String glucose;

    private float[] items;
    private String mComment = null;

    private RuleView ruleView;
    private TextView tv_glucose;
    private Button button_add;
    private Button button_sub;

    public FragmentGlucose() {
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        items = args.getFloatArray("items");
        glucose = args.getString("glucose");
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_glucose_input, container, false);
        ruleView = (RuleView) view.findViewById(R.id.ruleView);
        tv_glucose = (TextView) view.findViewById(R.id.tv_glucose);
        button_add = (Button) view.findViewById(R.id.button_add);
        button_sub = (Button) view.findViewById(R.id.button_sub);

        try {
            if (items != null) {
                ruleView.setValue(items[0], items[1], Float.parseFloat(glucose), 0.1F, 10);
            }
            if (!TextUtils.isEmpty(glucose)) {
                ruleView.setCurrentValue(Float.parseFloat(glucose));
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.input_err, Toast.LENGTH_SHORT).show();
        }

        ruleView.setOnValueChangedListener(new RuleView.OnValueChangedListener() {
            @Override
            public void onValueChanged(float value) {
                tv_glucose.setText(String.valueOf(value));
            }
        });
        button_add.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    button_add.setBackgroundResource(R.drawable.jia_2);
                    updateAddOrSubtract(v.getId());    //手指按下时触发不停的发送消息
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    button_add.setBackgroundResource(R.drawable.jia);
                    stopAddOrSubtract();    //手指抬起时停止发送
                }
                return true;
            }
        });
        button_sub.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    button_sub.setBackgroundResource(R.drawable.jian_2);
                    updateAddOrSubtract(v.getId());    //手指按下时触发不停的发送消息
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    button_sub.setBackgroundResource(R.drawable.jian);
                    stopAddOrSubtract();    //手指抬起时停止发送
                }
                return true;
            }
        });

        setComment(view, mComment);
        return view;
    }

    private ScheduledExecutorService scheduledExecutor;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case R.id.button_add:
                    float current = ruleView.getCurrentValue();
                    current = (current * 10 + 1) / 10;
                    if (current >= items[1]) {
                        current = items[1];
                    }
                    ruleView.setCurrentValue(current);
                    break;
                case R.id.button_sub:
                    float current2 = ruleView.getCurrentValue();
                    current2 = (current2 * 10 - 1) / 10;
                    if (current2 <= items[0]) {
                        current2 = items[0];
                    }
                    ruleView.setCurrentValue(current2);
                    break;
                default:

                    break;
            }
        }
    };

    private void updateAddOrSubtract(int viewId) {
        final int vid = viewId;
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = vid;
                handler.sendMessage(msg);
            }
        }, 0, 300, TimeUnit.MILLISECONDS);    //每间隔100ms发送Message
    }

    private void stopAddOrSubtract() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdownNow();
            scheduledExecutor = null;
        }
    }

    public void setComment(String comment) {
        mComment = comment;
        View view = getView();

        if (view != null) {
            setComment(view, comment);
        }
    }


    private void setComment(View view, String comment) {
        TextView textViewComment =
                (TextView) view.findViewById(R.id.text_view_comment);

        if (comment != null) {
            textViewComment.setVisibility(View.VISIBLE);
            textViewComment.setText(comment);
        } else {
            textViewComment.setVisibility(View.GONE);
        }
    }

    public String getGlucose() {
        return tv_glucose.getText().toString();
    }

}
