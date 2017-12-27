package com.microtechmd.pda.manager;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.microtechmd.pda.R;

import java.text.DecimalFormat;


public class SharePreferenceManager
{

	public static final int VIBRATE_FIRST = 0;
	public static final int SOUND_FIRST = 1;
	public static final int VIBRATE_SOUND = 2;

	public static final int INSULIN_BASAL = 0x0;
	public static final int INSULIN_BOLUS = 0x2;
	public static final int INSULIN_TEMP_BASAL = 0x1;
	public static final int INSULIN_SUSPEND = 0x4;

	// public static String SETTING_STATUS_OFF;
	// public static String SETTING_STATUS_PERCENT;
	// public static String SETTING_STATUS_U;
	// public static String SETTING_STATUS_U_HR;

	public static String TIME_FORMAT_24 = "HH:mm";
	public static String TIME_FORMAT_12 = "hh:mm";

	private static final String SHARE_PREFERENCE_NAME = "pda.pre";
	private static final String LAST_WARNING_TIME = "last_warning_time";
	private static final String LAST_CLOCKING_TIME = "last_clocking_time";
	private static final String TIME_FORMAT = "time_format";
	private static final String DATE_FORMAT = "date_format";
	private static final String SOUND_SWITCH = "sound_switch";
	private static final String VIBRATION_SWITCH = "vibration_switch";
	private static final String SOUND_FILE = "sound_file";

	private static final String BOLUS_TOTAL = "bolus_total";
	private static final String BOLUS_NOW = "bolus_now";
	private static final String BOLUS_EXTEND_TIME = "bolus_extend_time";
	private static final String BOLUS_EXTEND = "bolus_extend";
	private static final String BOLUS_START_TIME = "bolus_start_time";
	private static final String INSULIN_TYPE = "insulin_type";
	private static final String TEMP_BASAL_START_TIME = "temp_basal_start_time";
	private static final String SUSPEND_START_TIME = "suspend_start_time";
	private static final String SUSPEND_TIME = "suspend_time";

	private static final String BASAL_NAME_PROGRAM1 = "basal_name_program1";
	private static final String BASAL_NAME_PROGRAM2 = "basal_name_program2";
	private static final String BASAL_NAME_PROGRAM3 = "basal_name_program3";
	private static final String BASAL_ACTIVATED_INDEX = "basal_activated_index";

	private static final String BG_HYPOGLYCEMIA_ALERT_LEVEL = "bg_hypoglycemia_alert_level";
	private static final String BG_HYPERGLYCEMIA_ALERT_LEVEL = "bg_hyperglycemia_alert_level";
	private static final String BG_REMINDER_SETTING = "bg_reminder_setting";
	private static final String BG_GOAL = "bg_goal";
	private static final String BG_GOAL_LOW = "bg_goal_low";
	private static final String BG_GOAL_HIGH = "bg_goal_high";

	private static final String INSULIN_DELIVERY_TARGET_BG = "insulin_delivery_target_bg";
	private static final String INSULIN_DELIVERY_SENSITIVITY = "insulin_delivery_sensitivity";
	private static final String INSULIN_DELIVERY_CARBOHYDRATE_TATIO = "insulin_delivery_carbohydrate_tatio";
	private static final String INSULIN_DELIVERY_ACTIVE_TIME = "insulin_delivery_active_time";
	private static final String INSULIN_DELIVERY_REVERSE_CORRECTION = "insulin_delivery_reverse_correction";
	private static final String INSULIN_DELIVERY_CANNULA_TYPE = "insulin_delivery_cannula_type";
	private static final String INSULIN_DELIVERY_BOLUS_CALCS = "insulin_delivery_bolus_calcs";
	private static final String INSULIN_DELIVERY_TEMP_BASAL = "insulin_delivery_temp_basal";
	private static final String INSULIN_DELIVERY_EXTENDED = "insulin_delivery_extended";
	private static final String INSULIN_DELIVERY_BOLUS_INCREMENT = "insulin_delivery_bolus_increment";
	private static final String INSULIN_DELIVERY_MAX_BOLUS = "insulin_delivery_max_bolus";
	private static final String INSULIN_DELIVERY_MAX_BASAL = "insulin_delivery_max_basal";
	private static final String INSULIN_DELIVERY_BASE_BASAL_RATE = "insulin_delivery_base_basal_rate";
	private static final String INSULIN_DELIVERY_QUICK_BOLUS_INCREMENT = "insulin_delivery_quick_bolus_increment";
	private static final String INSULIN_DELIVERY_QUICK_BOLUS = "insulin_delivery_quick_bolus";
	private static final String INSULIN_DELIVERY_LOW_RESERVOIR = "insulin_delivery_low_reservior";
	private static final String INSULIN_DELIVERY_AUTO_OFF = "insulin_delivery_auto_off";
	private static final String INSULIN_DELIVERY_EXPIRATION = "insulin_delivery_expiration";
	private static final String SOFTWARE_VERSION = "software_version";
	private static final String PDA_SERIAL_NUMBER = "pda_serial_number";
	private static final String ACTIVE_PUMP_SERIAL_NUMBER = "active_pump_serial_number";
	private static final String ACTIVE_PUMP_START_DATE = "active_pump_start_date";

	private static final String ACTION_PUMP_NUM = "action_pump_num";

	private static final String HISTORY_CHECK = "history_check";

	private static final String BLUETOOTH_STATUS = "bluetooth_status";
	private static final String BLUETOOTH_NAME = "bluetooth_name";
	private static final String BLUETOOTH_VISIABLE_TIMESTAMP = "bluetooth_visiable_timestamp";

	private static final String DISPLAY_TIME_OUT = "display_time_out";

	private static final String TIMEZONE_INDEX = "timezone_index";

	public static final int HISTORY_CHECK_BEFORE_EXERCISE = 1;
	public static final int HISTORY_CHECK_AFTER_EXERCISE = 2;
	public static final int HISTORY_CHECK_BEFORE_MEAL = 3;
	public static final int HISTORY_CHECK_AFTER_MEAL = 4;
	public static final int HISTORY_CHECK_ALL_READINGS = 5;

	private static boolean sIsBgUnit_mmol_l = true;


	private static SharedPreferences getSharedPreferences(Context context)
	{
		// String str = Locale.getDefault().getLanguage();
		// mIsCh = str.equals("zh");

		return context.getSharedPreferences(SHARE_PREFERENCE_NAME,
			Context.MODE_PRIVATE);
	}


	public static int getTimezoneIndex(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getInt(TIMEZONE_INDEX, -1);
	}


	public static void setTimezoneIndex(Context context, int index)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putInt(TIMEZONE_INDEX, index);
		editor.commit();
	}


	public static boolean isBgUnit_mmol_l()
	{
		return sIsBgUnit_mmol_l;
	}


	public static long getLastWarningTime(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getLong(LAST_WARNING_TIME, 0);
	}


	public static void setLastWarningTime(Context context, long time)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putLong(LAST_WARNING_TIME, time);
		editor.commit();
	}


	public static long getLastClockingTime(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getLong(LAST_CLOCKING_TIME, 0);
	}


	public static void setLastClockingTime(Context context, long time)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putLong(LAST_CLOCKING_TIME, time);
		editor.commit();
	}


	public static String getTimeFormat(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(TIME_FORMAT, "HH:mm");
	}


	public static void setTimeFormat(Context context, String timeFormat)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(TIME_FORMAT, timeFormat);
		editor.commit();
	}


	public static String getDateFormat(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(DATE_FORMAT, "MM-dd-yyyy");
	}


	public static void setDateFormat(Context context, String dateFormat)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(DATE_FORMAT, dateFormat);
		editor.commit();
	}


	public static boolean isSoundOpen(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getBoolean(SOUND_SWITCH, false);
	}


	public static void setSoundSwitch(Context context, boolean open)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putBoolean(SOUND_SWITCH, open);
		editor.commit();
	}


	public static boolean isVibrationOpen(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getBoolean(VIBRATION_SWITCH, false);
	}


	public static void setVibrationSwitch(Context context, boolean open)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putBoolean(VIBRATION_SWITCH, open);
		editor.commit();
	}


	public static String getSoundFile(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(SOUND_FILE, "sound.mp3");
	}


	public static void setSoundFile(Context context, String soundFile)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(SOUND_FILE, soundFile);
		editor.commit();
	}


	public static float getBolusTotal(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getFloat(BOLUS_TOTAL, 3.0f);
	}


	public static void setBolusTotal(Context context, float bolusTotal)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putFloat(BOLUS_TOTAL, bolusTotal);
		editor.commit();
	}


	public static float getBolusNow(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getFloat(BOLUS_NOW, 0.4f);
	}


	public static void setBolusNow(Context context, float bolusNow)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putFloat(BOLUS_NOW, bolusNow);
		editor.commit();
	}


	public static float getBolusExtendTime(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getFloat(BOLUS_EXTEND_TIME, 0.01f);
	}


	public static void setBolusExtendTime(Context context, float extendTime)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putFloat(BOLUS_EXTEND_TIME, extendTime);
		editor.commit();
	}


	public static boolean getBolusExtend(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getBoolean(BOLUS_EXTEND, true);
	}


	public static void setBolusExtend(Context context, boolean extend)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putBoolean(BOLUS_EXTEND, extend);
		editor.commit();
	}


	public static int getInsulinType(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getInt(INSULIN_TYPE, INSULIN_BASAL);
	}


	public static void setInsulinType(Context context, int insulinType)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();

		editor.putInt(INSULIN_TYPE, insulinType);
		editor.commit();
	}


	public static long getBolusStartTime(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getLong(BOLUS_START_TIME, 0);
	}


	public static void setBolusStartTime(Context context, long time)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putLong(BOLUS_START_TIME, time);
		editor.commit();
	}


	public static long getTempBasalStartTime(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getLong(TEMP_BASAL_START_TIME, 0);
	}


	public static void setTempBasalStartTime(Context context, long time)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putLong(TEMP_BASAL_START_TIME, time);
		editor.commit();
	}


	public static long getSuspendStartTime(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getLong(SUSPEND_START_TIME, 0);
	}


	public static void setSuspendStartTime(Context context, long time)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putLong(SUSPEND_START_TIME, time);
		editor.commit();
	}


	public static float getSuspendTime(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getFloat(SUSPEND_TIME, 2.0f);
	}


	public static void setSuspendTime(Context context, float suspendTime)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putFloat(SUSPEND_TIME, suspendTime);
		editor.commit();
	}


	public static String getInsulinDeliveryTargetBg(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		String defValue = "70";
		return sp.getString(INSULIN_DELIVERY_TARGET_BG, defValue);
	}


	public static void setInsulinDeliveryTargetBg(Context context, String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(INSULIN_DELIVERY_TARGET_BG, value);
		editor.commit();
	}


	public static String getInsulinDeliverySensitivity(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(INSULIN_DELIVERY_SENSITIVITY, "0");
	}


	public static void setInsulinDeliverySensitivity(Context context,
		String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(INSULIN_DELIVERY_SENSITIVITY, value);
		editor.commit();
	}


	public static String getInsulinDeliveryCarbohydrateTatio(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(INSULIN_DELIVERY_CARBOHYDRATE_TATIO, "0");
	}


	public static void setInsulinDeliveryCarbohydrateTatio(Context context,
		String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(INSULIN_DELIVERY_CARBOHYDRATE_TATIO, value);
		editor.commit();
	}


	public static String getInsulinDeliveryActiveTime(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(INSULIN_DELIVERY_ACTIVE_TIME, "0");
	}


	public static void setInsulinDeliveryActiveTime(Context context,
		String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(INSULIN_DELIVERY_ACTIVE_TIME, value);
		editor.commit();
	}


	public static boolean getInsulinDeliveryReverseCorrection(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getBoolean(INSULIN_DELIVERY_REVERSE_CORRECTION, true);
	}


	public static void setInsulinDeliveryReverseCorrection(Context context,
		boolean value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putBoolean(INSULIN_DELIVERY_REVERSE_CORRECTION, value);
		editor.commit();
	}


	public static String getInsulinDeliveryCannulaType(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(INSULIN_DELIVERY_CANNULA_TYPE,
			context.getString(R.string.actions_reservoir_cannula_6));
	}


	public static void setInsulinDeliveryCannulaType(Context context,
		String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(INSULIN_DELIVERY_CANNULA_TYPE, value);
		editor.commit();
	}


	public static boolean getInsulinDeliveryBolusCalcs(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getBoolean(INSULIN_DELIVERY_BOLUS_CALCS, false);
	}


	public static void setInsulinDeliveryBolusCalcs(Context context,
		boolean value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putBoolean(INSULIN_DELIVERY_BOLUS_CALCS, value);
		editor.commit();
	}


	public static int getInsulinDeliveryTempBasal(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getInt(INSULIN_DELIVERY_TEMP_BASAL, 0);
	}


	public static void setInsulinDeliveryTempBasal(Context context, int value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putInt(INSULIN_DELIVERY_TEMP_BASAL, value);
		editor.commit();
	}


	public static int getInsulinDeliveryExtended(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getInt(INSULIN_DELIVERY_EXTENDED, 0);
	}


	public static void setInsulinDeliveryExtended(Context context, int value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putInt(INSULIN_DELIVERY_EXTENDED, value);
		editor.commit();
	}


	public static float getInsulinDeliveryBolusIncrement(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getFloat(INSULIN_DELIVERY_BOLUS_INCREMENT, 0.1f);
	}


	public static void setInsulinDeliveryBolusIncrement(Context context,
		float value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putFloat(INSULIN_DELIVERY_BOLUS_INCREMENT, value);
		editor.commit();
	}


	public static String getInsulinDeliveryMaxBolus(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(INSULIN_DELIVERY_MAX_BOLUS, "10");
	}


	public static void setInsulinDeliveryMaxBolus(Context context, String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(INSULIN_DELIVERY_MAX_BOLUS, value);
		editor.commit();
	}


	public static float getInsulinDeliveryMaxBasal(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getFloat(INSULIN_DELIVERY_MAX_BASAL, 3.0f);
	}


	public static void setInsulinDeliveryMaxBasal(Context context, float value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putFloat(INSULIN_DELIVERY_MAX_BASAL, value);
		editor.commit();
	}


	public static float getInsulinDeliveryBaseBasalRate(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getFloat(INSULIN_DELIVERY_BASE_BASAL_RATE, 0.5f);
	}


	public static void setInsulinDeliveryBaseBasalRate(Context context,
		float value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putFloat(INSULIN_DELIVERY_BASE_BASAL_RATE, value);
		editor.commit();
	}


	public static float getInsulinDeliveryQuickBolusIncrement(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getFloat(INSULIN_DELIVERY_QUICK_BOLUS_INCREMENT, 0.1f);
	}


	public static void setInsulinDeliveryQuickBolusIncrement(Context context,
		float value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putFloat(INSULIN_DELIVERY_QUICK_BOLUS_INCREMENT, value);
		editor.commit();
	}


	public static boolean getInsulinDeliveryQuickBolus(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getBoolean(INSULIN_DELIVERY_QUICK_BOLUS, false);
	}


	public static void setInsulinDeliveryQuickBolus(Context context,
		boolean value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putBoolean(INSULIN_DELIVERY_QUICK_BOLUS, value);
		editor.commit();
	}


	public static String getInsulinDeliveryLowReservoir(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(INSULIN_DELIVERY_LOW_RESERVOIR, "10");
	}


	public static void setInsulinDeliveryLowReservoir(Context context,
		String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(INSULIN_DELIVERY_LOW_RESERVOIR, value);
		editor.commit();
	}


	public static boolean getInsulinDeliveryAutoOff(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getBoolean(INSULIN_DELIVERY_AUTO_OFF, false);
	}


	public static void setInsulinDeliveryAutoOff(Context context, boolean value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putBoolean(INSULIN_DELIVERY_AUTO_OFF, value);
		editor.commit();
	}


	public static String getInsulinDeliveryExpiration(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(INSULIN_DELIVERY_EXPIRATION, "0");
	}


	public static void setInsulinDeliveryExpiration(Context context,
		String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(INSULIN_DELIVERY_EXPIRATION, value);
		editor.commit();
	}


	public static void setBasalProgram1Name(Context context, String name)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(BASAL_NAME_PROGRAM1, name);
		editor.commit();
	}


	public static String getBasalProgram1Name(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(BASAL_NAME_PROGRAM1,
			context.getString(R.string.basal_name_program1));
	}


	public static void setBasalProgram2Name(Context context, String name)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(BASAL_NAME_PROGRAM2, name);
		editor.commit();
	}


	public static String getBasalProgram2Name(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(BASAL_NAME_PROGRAM2,
			context.getString(R.string.basal_name_program2));
	}


	public static void setBasalProgram3Name(Context context, String name)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(BASAL_NAME_PROGRAM3, name);
		editor.commit();
	}


	public static String getBasalProgram3Name(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(BASAL_NAME_PROGRAM3,
			context.getString(R.string.basal_name_program3));
	}


	public static int getActivatedBasalIndex(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getInt(BASAL_ACTIVATED_INDEX, 0);
	}


	public static void setActivatedBasalIndex(Context context, int index)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putInt(BASAL_ACTIVATED_INDEX, index);
		editor.commit();
	}


	public static String getSoftwareVersion(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(SOFTWARE_VERSION, "");
	}


	public static String getPdaSerialNumber(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(PDA_SERIAL_NUMBER, "");
	}


	public static String getActivePumpSerialNumber(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(ACTIVE_PUMP_SERIAL_NUMBER, "");
	}


	public static void setActivePumpSerialNumber(Context context, String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(ACTIVE_PUMP_SERIAL_NUMBER, value);
		editor.commit();
	}


	public static String getActivePumpStartTime(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(ACTIVE_PUMP_START_DATE, "0");
	}


	public static String getBgHypoglycemiaAlertLevel(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		String defValue = "68";
		return sp.getString(BG_HYPOGLYCEMIA_ALERT_LEVEL, defValue);
	}


	public static String getBgHyperglycemiaAlertLevel(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		String defValue = "180";
		return sp.getString(BG_HYPERGLYCEMIA_ALERT_LEVEL, defValue);
	}


	public static String getBgReminderSetting(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(BG_REMINDER_SETTING, "0");
	}


	public static String getBgGoal(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		String defValue = "108";
		return sp.getString(BG_GOAL, defValue);
	}


	public static String getBgGoalLow(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		String defValue = "0";
		return sp.getString(BG_GOAL_LOW, defValue);
	}


	public static String getBgGoalHigh(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		String defValue = "0";
		return sp.getString(BG_GOAL_HIGH, defValue);
	}


	public static void setBgHypoglycemiaAlertLevel(Context context, String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(BG_HYPOGLYCEMIA_ALERT_LEVEL, preBgValue(value));
		editor.commit();
	}


	public static void setBgHyperglycemiaAlertLevel(Context context,
		String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(BG_HYPERGLYCEMIA_ALERT_LEVEL, preBgValue(value));
		editor.commit();
	}


	public static void setBgReminderSetting(Context context, String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(BG_REMINDER_SETTING, value);
		editor.commit();
	}


	public static void setBgGoal(Context context, String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(BG_GOAL, preBgValue(value));
		editor.commit();
	}


	public static void setBgGoalLow(Context context, String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(BG_GOAL_LOW, preBgValue(value));
		editor.commit();
	}


	public static void setBgGoalHigh(Context context, String value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putString(BG_GOAL_HIGH, preBgValue(value));
		editor.commit();
	}


	public static int getActionPumpNum(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getInt(ACTION_PUMP_NUM, 0);
	}


	public static void setActionPumpNum(Context context, int value)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putInt(ACTION_PUMP_NUM, value);
		editor.commit();
	}


	public static int getHistoryCheck(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getInt(HISTORY_CHECK, HISTORY_CHECK_ALL_READINGS);
	}


	public static void setHistoryCheck(Context context, int check)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putInt(HISTORY_CHECK, check);
		editor.commit();
	}


	public static boolean getBluetoothStatus(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getBoolean(BLUETOOTH_STATUS, false);
	}


	public static void setBluetoothStatus(Context context, boolean open)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putBoolean(BLUETOOTH_STATUS, open);
		editor.commit();
	}


	public static long getBluetoothVisiableTimestamp(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getLong(BLUETOOTH_VISIABLE_TIMESTAMP, 0);
	}


	public static void setBluetoothVisiableTimestamp(Context context,
		long timestamp)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putLong(BLUETOOTH_VISIABLE_TIMESTAMP, timestamp);
		editor.commit();
	}


	public static int getDisplayTimeout(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getInt(DISPLAY_TIME_OUT, 60000);
	}


	public static void setDisplayTimeout(Context context, int timeout)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.putInt(DISPLAY_TIME_OUT, timeout);
		editor.commit();
	}


	public static void clear(Context context)
	{
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		editor.clear();
		editor.commit();
	}


	private static String preBgValue(String value)
	{
		if (value.contains("."))
		{
			DecimalFormat df = new DecimalFormat("##0");
			value = df.format(Float.parseFloat(value) * 18f);
		}
		return value;
	}
}
