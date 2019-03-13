package com.microtechmd.pda.ui.activity.fragment;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.microtechmd.pda.R;


public class FragmentUnPair extends FragmentBase {

    private String mComment = null;


    public FragmentUnPair() {
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_unpair, container, false);
        setComment(view, mComment);
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

    public boolean getIsRemove() {
        CheckBox cb_remove = (CheckBox) getView().findViewById(R.id.cb_remove);
        return cb_remove.isChecked();
    }
}
