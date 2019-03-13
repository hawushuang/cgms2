package com.microtechmd.pda.ui.activity.fragment;


import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.microtechmd.pda.R;
import com.microtechmd.pda.ui.widget.contrarywind.adapter.ArrayWheelAdapter;
import com.microtechmd.pda.ui.widget.contrarywind.listener.OnItemSelectedListener;
import com.microtechmd.pda.ui.widget.contrarywind.view.WheelView;

import java.util.Arrays;


public class FragmentSingleChoice extends FragmentBase {

    private String mComment = null;

    private String[] items;
    private int checkIndex = 0;

    public FragmentSingleChoice() {
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        items = args.getStringArray("items");
        checkIndex = args.getInt("checkIndex");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_single_choice, container, false);
        WheelView wheelView = (WheelView) view.findViewById(R.id.wheelview);
        setComment(view, mComment);

        if (items != null) {
            wheelView.setAdapter(new ArrayWheelAdapter(Arrays.asList(items)));
            wheelView.setCyclic(false);
            wheelView.setTextColorOut(Color.GRAY);
            wheelView.setTextColorCenter(Color.WHITE);
            wheelView.setTextSize(25);
            wheelView.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(int index) {
                    checkIndex = index;
                }
            });
        }
        wheelView.setCurrentItem(checkIndex);
        return view;
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

    public void setComment(String comment) {
        mComment = comment;
        View view = getView();

        if (view != null) {
            setComment(view, comment);
        }
    }

    public int getCheckIndex() {
        return checkIndex;
    }
}
