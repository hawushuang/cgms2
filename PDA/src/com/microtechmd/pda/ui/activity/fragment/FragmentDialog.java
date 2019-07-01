package com.microtechmd.pda.ui.activity.fragment;


import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.utility.LogPDA;
import com.microtechmd.pda.util.KeyNavigation;


public class FragmentDialog extends DialogFragment
        implements KeyNavigation.OnClickViewListener {
    public static final int BUTTON_ID_POSITIVE = 0;
    public static final int BUTTON_ID_NEGATIVE = 1;
    public static final int COUNT_BUTTON_ID = 2;

    private static final String TAG_CONTENT = "content";

    protected LogPDA mLog = null;

    private int mGravity = 0;
    private int mAnimation = 0;
    private boolean mBottom = true;
    private String mTitle = null;
    private String mButtonText[] = null;
    private Fragment mContent = null;
    private ListenerDialog mListener = null;
    private KeyNavigation mKeyNavigation = null;

    private boolean homeCancelFlag;

    public void setHomeCancelFlag(boolean homeCancelFlag) {
        this.homeCancelFlag = homeCancelFlag;
    }


    public interface ListenerDialog {
        boolean onButtonClick(int buttonID, Fragment content);
    }


    public FragmentDialog() {
        mLog = new LogPDA();
        mButtonText = new String[COUNT_BUTTON_ID];

        for (int buttonID = 0; buttonID < COUNT_BUTTON_ID; buttonID++) {
            mButtonText[buttonID] = "";
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =
                inflater.inflate(R.layout.fragment_dialog, container, false);
        Dialog dialog = getDialog();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        WindowManager.LayoutParams layoutParams =
                dialog.getWindow().getAttributes();
        mGravity = layoutParams.gravity;
        mAnimation = layoutParams.windowAnimations;

        if (mButtonText[BUTTON_ID_POSITIVE] != null) {
            if (mButtonText[BUTTON_ID_POSITIVE].equals("")) {
                mButtonText[BUTTON_ID_POSITIVE] =
                        getResources().getString(R.string.ok);
            }
        }

        if (mButtonText[BUTTON_ID_NEGATIVE] != null) {
            if (mButtonText[BUTTON_ID_NEGATIVE].equals("")) {
                mButtonText[BUTTON_ID_NEGATIVE] =
                        getResources().getString(R.string.cancel);
            }
        }

        setBottom(view, dialog, mBottom);
        setButtonText(view, BUTTON_ID_POSITIVE,
                mButtonText[BUTTON_ID_POSITIVE]);
        setButtonText(view, BUTTON_ID_NEGATIVE,
                mButtonText[BUTTON_ID_NEGATIVE]);
        setTitle(view, mTitle);
        setContent(view, mContent);
//
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_HOME) {
                    if (homeCancelFlag) {
                        dialog.dismiss();
//                        dismissAllowingStateLoss();
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (homeCancelFlag) {
                        dialog.dismiss();
//                        dismissAllowingStateLoss();
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });
//        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
//        rootViewGetFocus(view);
        return view;
    }

    private void rootViewGetFocus(View mRootView) {
        mRootView.setFocusable(true);//这个和下面的这个命令必须要设置了，才能监听事件。
        mRootView.setFocusableInTouchMode(true);
        mRootView.requestFocus();
        mRootView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_VOLUME_UP:
                        Toast.makeText(getActivity(), "++", Toast.LENGTH_SHORT).show();
                        return true;
                    case KeyEvent.KEYCODE_VOLUME_DOWN:
                        Toast.makeText(getActivity(), "--", Toast.LENGTH_SHORT).show();
                        return true;
                    case KeyEvent.KEYCODE_BACK:
                        Toast.makeText(getActivity(), "返回", Toast.LENGTH_SHORT).show();
                        return true;
                    case KeyEvent.KEYCODE_HOME:
                        Toast.makeText(getActivity(), "回主页", Toast.LENGTH_SHORT).show();
                        return true;
                    case ApplicationPDA.KEY_CODE_BOLUS:
                        Toast.makeText(getActivity(), "确定", Toast.LENGTH_SHORT).show();
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mKeyNavigation = new KeyNavigation(getActivity(), this);
        resetKeyNavigation();
    }

    @Override
    public void onDestroyView() {
        mKeyNavigation.clearFocus();
        mKeyNavigation = null;
        super.onDestroyView();
    }


    @Override
    public void onStart() {
        super.onStart();

        Window window = getDialog().getWindow();

        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(layoutParams);
            View view = getView();

            if (view != null) {
                if ((view.findViewById(R.id.button_positive)
                        .getVisibility() == View.GONE) &&
                        (view.findViewById(R.id.button_negative)
                                .getVisibility() == View.GONE) &&
                        (view.findViewById(R.id.text_view_title)
                                .getVisibility() == View.GONE)) {
                    window.setBackgroundDrawable(new ColorDrawable(0));
                } else {
                    window.setBackgroundDrawable(new ColorDrawable(ContextCompat
                            .getColor(getActivity(), R.color.text_light)));
                }
            }
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_positive:

                if ((mListener != null) && (getFragmentManager() != null)) {
                    if (mListener.onButtonClick(BUTTON_ID_POSITIVE,
                            getFragmentManager().findFragmentByTag(TAG_CONTENT))) {
                        dismiss();
                    }
                } else {
                    dismiss();
                }

                break;

            case R.id.button_negative:

                if ((mListener != null) && (getFragmentManager() != null)) {
                    if (mListener.onButtonClick(BUTTON_ID_NEGATIVE,
                            getFragmentManager().findFragmentByTag(TAG_CONTENT))) {
                        dismiss();
                    }
                } else {
                    dismiss();
                }

                break;

            default:
                break;
        }
    }


    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            super.show(manager, tag);
            mLog.Error(getClass(), "弹窗");
        } catch (IllegalStateException ignore) {
            FragmentTransaction ft = manager.beginTransaction();
            ft.add(this, tag);
            ft.commitAllowingStateLoss();
            mLog.Error(getClass(), "弹窗报错");
        }
    }


    public void setBottom(boolean bottom) {
        mBottom = bottom;
        Dialog dialog = getDialog();
        View view = getView();


        if ((dialog != null) && (view != null)) {
            setBottom(view, dialog, bottom);
        }
    }


    public void setTitle(String title) {
        mTitle = title;
        View view = getView();

        if (view != null) {
            setTitle(view, title);
        }
    }


    public void setButtonText(int buttonID, String text) {
        if (buttonID < COUNT_BUTTON_ID) {
            mButtonText[buttonID] = text;
            View view = getView();

            if (view != null) {
                setButtonText(view, buttonID, text);
            }
        }
    }


    public void setContent(Fragment content) {
        mContent = content;
        View view = getView();

        if (view != null) {
            setContent(view, content);
        }
    }


    public void setListener(ListenerDialog listener) {
        if (listener != null) {
            mListener = listener;
        }
    }


    protected void resetKeyNavigation() {
        if (getView() != null) {
            mKeyNavigation.resetNavigation(getView());
        }
    }


    private void setBottom(View view, Dialog dialog, boolean bottom) {
        Window window = dialog.getWindow();
        WindowManager.LayoutParams layoutParams = window.getAttributes();

        if (bottom) {
            layoutParams.gravity = Gravity.BOTTOM;
            layoutParams.windowAnimations = R.style.bottom_to_top_anim;
            view.setBackgroundColor(
                    getResources().getColor(R.color.background_dark));
        } else {
            layoutParams.gravity = mGravity;
            layoutParams.windowAnimations = mAnimation;
            view.setBackgroundColor(
                    getResources().getColor(R.color.transparent));
        }

        window.setAttributes(layoutParams);
    }


    private void setTitle(View view, String title) {
        TextView textViewTitle =
                (TextView) view.findViewById(R.id.text_view_title);

        if (title == null) {
            textViewTitle.setVisibility(View.GONE);
        } else {
            textViewTitle.setVisibility(View.VISIBLE);
            textViewTitle.setText(title);
        }
    }


    private void setButtonText(View view, int buttonID, String text) {
        Button button = null;

        switch (buttonID) {
            case BUTTON_ID_POSITIVE:
                button = (Button) view.findViewById(R.id.button_positive);
                break;

            case BUTTON_ID_NEGATIVE:
                button = (Button) view.findViewById(R.id.button_negative);
                break;

            default:
                break;
        }

        if (button == null) {
            return;
        }

        if (text == null) {
            button.setVisibility(View.GONE);
        } else {
            button.setVisibility(View.VISIBLE);
            button.setText(text);
        }
    }


    private void setContent(View view, Fragment content) {
        if (content == null) {
            view.findViewById(R.id.layout_content).setVisibility(View.GONE);
        } else {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.layout_content, content, TAG_CONTENT).commit();
        }
    }
}
