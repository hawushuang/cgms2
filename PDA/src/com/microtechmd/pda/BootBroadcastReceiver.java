package com.microtechmd.pda;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.microtechmd.pda.ui.activity.ActivityMain;

/**
 * Created by Administrator on 2018/1/11.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {

    public static final String TAG = "BootBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        assert action != null;
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            // u can start your service here
            Intent startIntent = new Intent(context, ActivityMain.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startIntent);
        }
    }
}
