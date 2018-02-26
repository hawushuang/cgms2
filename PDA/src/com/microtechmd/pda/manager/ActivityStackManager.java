package com.microtechmd.pda.manager;

import android.app.Activity;

import com.microtechmd.pda.ui.activity.ActivityPDA;

import java.util.ArrayList;

/**
 * Created by Administrator on 2018/2/26.
 */

public class ActivityStackManager {
    private static ArrayList<Activity> sActivityList = new ArrayList<>();

    private static class ActivityStackManagerHolder {
        private static ActivityStackManager sInstance = new ActivityStackManager();
    }

    public static ActivityStackManager getInstance() {
        return ActivityStackManagerHolder.sInstance;
    }

    public void addActivity(Activity activity) {
        sActivityList.add(activity);
    }

    /**
     * back to target activity * * @param addTime the add time of target activity * @return true if back to target activity successfully
     */
    public boolean back2TargetActivity(String addTime) {
        if (isTargetActivityExist(addTime)) {
            for (int i = sActivityList.size() - 1; i >= 0; i--) {
                String var = ((ActivityPDA) sActivityList.get(i)).getAddTime();
                if (!var.equals(addTime)) {
                    popActivityFromStack(sActivityList.get(i));
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * back to target activity * * @param indexActivityClass the class of target activity * @return true if back to target activity successfully
     */
    public boolean back2TargetActivity(Class<Activity> indexActivityClass) {
        if (isTargetActivityExist(indexActivityClass)) {
            for (int i = sActivityList.size() - 1; i >= 0; i--) {
                if (sActivityList.get(i).getClass() != indexActivityClass) {
                    popActivityFromStack(sActivityList.get(i));
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTargetActivityExist(String addTime) {
        for (Activity activity : sActivityList) {
            if (activity == null) {
                continue;
            }
            if (((ActivityPDA) activity).getAddTime().equals(addTime)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTargetActivityExist(Class<Activity> targetActivityClass) {
        for (Activity activity : sActivityList) {
            if (activity == null) {
                continue;
            }
            if (activity.getClass() == targetActivityClass) {
                return true;
            }
        }
        return false;
    }

    private void popActivityFromStack(Activity activity) {
        if (activity != null && !activity.isFinishing()) {
            activity.finish();
            sActivityList.remove(activity);
        }
    }

    public void removeActivity(Activity activity) {
        sActivityList.remove(activity);
    }

}
