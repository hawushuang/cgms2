package com.microtechmd.pda.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.liangmutian.mypicker.LoopView;
import com.microtechmd.pda.R;
import com.microtechmd.pda.ui.activity.fragment.FragmentInput;

import java.util.Arrays;
import java.util.List;

public class ConfirmDialog extends Dialog {

    public interface OnConfirmListener {
        void onConfirm();
    }


    private Params params;

    ConfirmDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    private void setParams(ConfirmDialog.Params params) {
        this.params = params;
    }


    private static final class Params {
        private String text_Title;
        private String text_Content;
        private OnConfirmListener callback;
    }

    public static class Builder {
        private final Context context;
        private final ConfirmDialog.Params params;

        public Builder(Context context) {
            this.context = context;
            params = new ConfirmDialog.Params();
        }

        public ConfirmDialog create() {
            final ConfirmDialog dialog = new ConfirmDialog(context,
                    com.example.liangmutian.mypicker.R.style.Theme_Light_NoTitle_Dialog);
            View view = LayoutInflater.from(context).inflate(R.layout.confirm_dialog, null);

            view.findViewById(R.id.button_negative).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.dismiss();
                }
            });
            view.findViewById(R.id.button_positive).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    params.callback.onConfirm();
                }
            });
            TextView tv_title = (TextView) view.findViewById(R.id.text_view_title);
            TextView tv_content = (TextView) view.findViewById(R.id.layout_content);
            tv_title.setText(params.text_Title);
            tv_content.setText(params.text_Content);
            Window win = dialog.getWindow();
            assert win != null;
            win.getDecorView().setPadding(0, 0, 0, 0);
            WindowManager.LayoutParams lp = win.getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            win.setAttributes(lp);
            win.setGravity(Gravity.BOTTOM);
            win.setWindowAnimations(com.example.liangmutian.mypicker.R.style.Animation_Bottom_Rising);

            dialog.setContentView(view);
            dialog.setParams(params);

            return dialog;
        }


        public Builder setOnTimeSelectedListener(OnConfirmListener onConfirm) {
            params.callback = onConfirm;
            return this;
        }

        public Builder setTextTile(String testTile) {
            params.text_Title = testTile;
            return this;
        }

        public Builder setTextContent(String testContent) {
            params.text_Content = testContent;
            return this;
        }
    }
}
