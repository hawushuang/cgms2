package com.microtechmd.pda.ui.activity.fragment;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.microtechmd.pda.R;


public class FragmentNewProgress extends Dialog {
    private String mComment = null;
    private View view;

    public FragmentNewProgress(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        view = LayoutInflater.from(context).inflate(R.layout.fragment_progress, null);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (mComment == null) {
            mComment = context.getResources().getString(R.string.connecting);
        }

        setComment(view, mComment);
        setContentView(view);
    }


    public void setComment(String comment) {
        if (comment == null) {
            return;
        }

        mComment = comment;
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
        }
    }
}
