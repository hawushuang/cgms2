package com.microtechmd.pda.ui.activity;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.DataStorage;
import com.microtechmd.pda.library.utility.LogPDA;
import com.microtechmd.pda.manager.ActivityStackManager;
import com.microtechmd.pda.ui.activity.fragment.FragmentNewProgress;
import com.microtechmd.pda.ui.widget.WidgetStatusBar;
import com.microtechmd.pda.util.KeyNavigation;

import java.util.Stack;


public class ActivityLockBase extends AppCompatActivity
        implements
        KeyNavigation.OnClickViewListener {
    protected static final String SETTING_STATUS_BAR = "status_bar";

    protected ActivityLockBase mBaseActivity;
    protected LayoutInflater mLayoutInflater;
    protected boolean mLandscape;

    private static PowerManager.WakeLock sWakeLock = null;
    private static WidgetStatusBar sStatusBar = null;
    private static boolean sIsPowerdown = false;
    protected LogPDA mLog = null;

    private DataStorage mDataStorage = null;
    private KeyNavigation mKeyNavigation = null;
    private Handler mHandlerBrightness = null;
    private Runnable mRunnableBrightness = null;
    private Stack<Window> mScreenWindowStack = null;

    private FragmentNewProgress mFragmentProgress = null;

    private int mDialogLoadingCount = 0;


    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(R.layout.activity_lock_base);

        ViewStub viewStub = (ViewStub) findViewById(R.id.stub_activity);

        if (viewStub != null) {
            viewStub.setLayoutResource(layoutResID);
            viewStub.inflate();
            resetKeyNavigation();
        }
    }


    @Override
    public void onAttachedToWindow() {
        this.getWindow()
                .setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        super.onAttachedToWindow();
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            updateScreenBrightness();
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                return true;

            case KeyEvent.KEYCODE_POWER:
                return showDialogPower();

            default:
                return super.onKeyDown(keyCode, event);
        }
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {

            case KeyEvent.KEYCODE_HOME:
                onHomePressed();
                return true;
            case ApplicationPDA.KEY_CODE_BOLUS:
                if (mLandscape)
                    return true;
                return mKeyNavigation.onKeyConfirm();

            case KeyEvent.KEYCODE_POWER:
                return true;

            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private boolean showDialogPower() {
        if (sIsPowerdown) {
            return false;
        }

        sIsPowerdown = true;
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setTitle(getString(R.string.shutdown_title))
                .setMessage(getString(R.string.shutdown_content))
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            Intent shutdown = new Intent(
                                    "android.intent.action.ACTION_REQUEST_SHUTDOWN");
                            startActivity(shutdown);
                        } catch (Exception e) {
                            Toast.makeText(mBaseActivity, "Shut Down failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sIsPowerdown = false;
                    }
                });
        AlertDialog dialog = builder.create();
//        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
//        dialog.getWindow().setContentView(R.layout.dialog_power);
//        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
//        View v = inflater.inflate(R.layout.dialog_power, null);
//        dialog.getWindow().setContentView(v);
        return true;
    }

    @Override
    public void onClick(View v) {
        onClickView(v);
    }

    public void updateScreenBrightness() {
        final int SCREEN_BRIGHTNESS_MAX = 255;
        final int SCREEN_BRIGHTNESS_MIN = 15;

        int reduceBrightnessCycle = 0;


        if (reduceBrightnessCycle > 0) {
            if (mHandlerBrightness == null) {
                mHandlerBrightness = new Handler();
            }

            if (mRunnableBrightness != null) {
                mHandlerBrightness.removeCallbacks(mRunnableBrightness);
            }

            mRunnableBrightness = new Runnable() {

                @Override
                public void run() {
                    if ((mScreenWindowStack != null) &&
                            (!mScreenWindowStack.isEmpty())) {
                        // Set screen brightness to minimum
                        WindowManager.LayoutParams layoutParams =
                                mScreenWindowStack.peek().getAttributes();
                        layoutParams.screenBrightness =
                                (float) SCREEN_BRIGHTNESS_MIN /
                                        (float) SCREEN_BRIGHTNESS_MAX;
                        mScreenWindowStack.peek().setAttributes(layoutParams);
                    }
                }
            };

            mHandlerBrightness.postDelayed(mRunnableBrightness,
                    reduceBrightnessCycle);
        }

        if ((mScreenWindowStack != null) && (!mScreenWindowStack.isEmpty())) {
            // Restore screen brightness to system setting
            WindowManager.LayoutParams layoutParams =
                    mScreenWindowStack.peek().getAttributes();
            layoutParams.screenBrightness =
                    (float) Settings.System.getInt(getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS, SCREEN_BRIGHTNESS_MAX) /
                            (float) SCREEN_BRIGHTNESS_MAX;
            mScreenWindowStack.peek().setAttributes(layoutParams);
        }
    }


    public void pushScreenWindow(final Window window) {
        if (mScreenWindowStack == null) {
            mScreenWindowStack = new Stack<Window>();
        }

        mScreenWindowStack.push(window);
    }


    public void popScreenWindow() {
        if (mScreenWindowStack == null) {
            mScreenWindowStack = new Stack<Window>();
        }

        if (!mScreenWindowStack.isEmpty()) {
            mScreenWindowStack.pop();
        }
    }


    public DataStorage getDataStorage(String name) {
        if (name == null) {
            name = getClass().getSimpleName();
        }

        if (mDataStorage == null) {
            mDataStorage = new DataStorage(this, name);
        }

        if (!mDataStorage.getName().equals(name)) {
            mDataStorage = new DataStorage(this, name);
        }

        return mDataStorage;
    }


    public WidgetStatusBar getStatusBar() {
        return sStatusBar;
    }


    public void resetKeyNavigation() {
        mKeyNavigation.resetNavigation(getWindow().getDecorView());
    }

    @SuppressLint("ShortAlarm")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStackManager.getInstance().addActivity(this);
        mLog = new LogPDA();
        mKeyNavigation = new KeyNavigation(this, this);

        if (sWakeLock == null) {
            PowerManager powerManager =
                    (PowerManager) getSystemService(Context.POWER_SERVICE);
            assert powerManager != null;
            sWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getSimpleName());
        }

        if (sStatusBar == null) {
            sStatusBar = new WidgetStatusBar();
            sStatusBar
                    .setByteArray(getDataStorage(ActivityLockBase.class.getSimpleName())
                            .getExtras(SETTING_STATUS_BAR, null));

        }
        pushScreenWindow(getWindow());
        mBaseActivity = this;
        mLayoutInflater =
                (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        getStatusBar().setView(getWindow().getDecorView());
    }


    @Override
    protected void onPause() {
        super.onPause();
        getStatusBar().setPDACharger(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityStackManager.getInstance().removeActivity(this);
        mDialogLoadingCount = 0;
        dismissDialogProgress();
        popScreenWindow();
    }


    protected void onHomePressed() {
    }


    protected void onClickView(View v) {
    }

    public void dismissDialogProgress() {
        mLog.Debug(getClass(), "Dismiss progress dialog");

        if (mDialogLoadingCount > 0) {
            mDialogLoadingCount--;
        }

        if (mDialogLoadingCount <= 0) {
            mDialogLoadingCount = 0;

            if (mFragmentProgress != null) {
                mFragmentProgress.setComment(getString(R.string.connecting));
                mFragmentProgress.dismiss();
                mFragmentProgress = null;
            }
        }
    }

}