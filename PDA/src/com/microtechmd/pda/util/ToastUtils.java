package com.microtechmd.pda.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Toast;

public class ToastUtils {

    @SuppressLint("StaticFieldLeak")
    private static Context context = null;
    private static Toast toast = null;

    public static void showToast(Context context, String text) {
        if (toast == null) {
            toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        } else {
            toast.setText(text);
            toast.setDuration(Toast.LENGTH_SHORT);
        }
        toast.show();
    }
    public static void showToast(Context context, int textId) {
        if (toast == null) {
            toast = Toast.makeText(context, textId, Toast.LENGTH_SHORT);
        } else {
            toast.setText(textId);
            toast.setDuration(Toast.LENGTH_SHORT);
        }
        toast.show();
    }
}