package com.microtechmd.pda.manager;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/2/26.
 */

public class ActivityPathManager {
    /**
     * 这里用数组记录源Activity而不是单独用一个String，
     * 是因为：在某一个跳转路径上可能有多个注册源Activity行为
     * * 比如A->B->C-D-A，在A中进行注册源Activity，
     * * 同时另外一条链路M->N->B->C->D->M（当然这两条链路是不可能同时发生的），需要在B中注册源Activity。
     * 但是这两条链路有重合部分， *
     * 如果仅仅用String来表示addTime，会存在覆盖的情况，因此用数组来保存addTime，但是只有第一条数据有效。
     **/
    private static List<String> sAddTimeList = new ArrayList<>();
    private static List<Class<Activity>> sActivityClassList = new ArrayList<>();

    private static class ActivityPathManagerHolder {
        private static ActivityPathManager sInstance = new ActivityPathManager();
    }

    public static ActivityPathManager getInstance() {
        return ActivityPathManagerHolder.sInstance;
    }

    /**
     * 注册源Activity * * @param addTime Activity的创建时间，可以唯一表示某一Activity
     */
    public void registerSourceActivity(String addTime) {
        sAddTimeList.add(addTime);
    }

    /**
     * 注册源Activity * * @param indexClass Activity的类名
     */
    public void registerSourceActivity(Class<Activity> indexClass) {
        sActivityClassList.add(indexClass);
    }

    /**
     * 当从源Activity通过任意跳转路径到达目标Activity时，调用此方法后可以返回到源Activity
     * * @return 如果是true，直接跳转到源Activity;如果是false，走原有逻辑
     */
    public boolean back2SourceActivity() {
        if (!sAddTimeList.isEmpty()) {
            ActivityStackManager.getInstance().back2TargetActivity(sAddTimeList.get(0));
            clearAddTime();
            return true;
        }
        return false;
    }

    public boolean back2SourceActivityNew() {
        if (!sAddTimeList.isEmpty()) {
            ActivityStackManager.getInstance().back2TargetActivity(sAddTimeList.get(sAddTimeList.size() - 1));
            clearAddTime();
            return true;
        }
        return false;
    }

    /**
     * 当从源Activity通过任意跳转路径到达目标Activity时，调用此方法后可以返回到源Activity
     * * * @return 如果是true，直接跳转到源Activity;如果是false，走原有逻辑
     */
    public boolean back2SourceActivity2() {
        if (!sActivityClassList.isEmpty()) {
            ActivityStackManager.getInstance().back2TargetActivity(sActivityClassList.get(0));
            clearClass();
            return true;
        }
        return false;
    }

    /**
     * 当从源Activity通过任意跳转路径到达目标Activity时，调用此方法后可以返回到源Activity，此方法不需要注册Activity
     * * * @param addTime 源Activity的添加时间
     * * @return 如果是true，直接跳转到源Activity;如果是false，走原有逻辑
     */
    public boolean back2SourceActivity(String addTime) {
        if (addTime != null) {
            ActivityStackManager.getInstance().back2TargetActivity(addTime);
            return true;
        }
        return false;
    }

    /**
     * 当从源Activity通过任意跳转路径到达目标Activity时，调用此方法后可以返回到源Activity，此方法不需要注册Activity * * @param indexClass 源Activity的类 * @return 如果是true，直接跳转到源Activity;如果是false，走原有逻辑
     */
    public boolean back2SourceActivity(Class<Activity> indexClass) {
        if (indexClass != null) {
            ActivityStackManager.getInstance().back2TargetActivity(indexClass);
            return true;
        }
        return false;
    }

    private void clearAddTime() {
        sAddTimeList.clear();
    }

    private void clearClass() {
        sActivityClassList.clear();
    }

    /**
     * 清除所有已经注册Activity的addTime * notice：在你的源Activity的onCreate和onRestart方法调用该方法！
     */
    public void unregisterSourceActivity(String addTime) {
        sAddTimeList.remove(addTime);
    }

    /**
     * 清除所有已经注册Activity的Class * notice：在你的源Activity的onCreate和onRestart方法调用该方法！
     */
    public void unregisterSourceActivity(Class<Activity> indexClass) {
        sActivityClassList.remove(indexClass);
    }


}
