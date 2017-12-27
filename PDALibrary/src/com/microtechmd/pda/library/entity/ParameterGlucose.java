package com.microtechmd.pda.library.entity;

public class ParameterGlucose 
{
	public static final int PARAM_STATUS = 0;
	public static final int PARAM_ERROR = 1;
	public static final int PARAM_CONTROL = 2;
	public static final int PARAM_GLUCOSE = 3;
	public static final int PARAM_SIGNAL_PRESENT = 4;
	public static final int PARAM_COUNT_DOWN = 5;
	public static final int PARAM_FILL_LIMIT = 6;
	public static final int PARAM_BG_LIMIT = 7;
	public static final int PARAM_NEW_SENSOR = 8;
//	public static final int PARAM_NBB_LIMIT = 8;
	public static final int PARAM_EARLY_FILL_LIMIT = 9;
	public static final int PARAM_CODE = 10;
	public static final int PARAM_REFERENCE = 11;
	public static final int PARAM_SWITCH = 12;
	public static final int COUNT_PARAM = 13;
	
	public static final int EVENT_GLUCOSE = 0;
	public static final int EVENT_CARBOHYDRATE = 1;
	public static final int COUNT_EVENT = 2;
	
	public static final int ERROR_NONE = 0;
	public static final int ERROR_CODE = 1;
	public static final int ERROR_CHANNEL = 2;
	public static final int ERROR_NBB = 3;
	public static final int ERROR_TEMPERATURE = 4;
	public static final int ERROR_BLOOD_FILLING = 5;
	public static final int ERROR_BLOOD_NOT_ENOUGH = 6;
	public static final int ERROR_STRIP = 7;
	public static final int COUNT_ERROR = 8;
	
	public static final int FLAG_MANUAL = 0;
	public static final int FLAG_INVALID = 1;
	public static final int FLAG_MEAL = 2;
	public static final int FLAG_AFTER_MEAL = 3;
	public static final int FLAG_EXERCISE = 4;
	public static final int FLAG_AFTER_EXERCISE = 5;
	public static final int COUNT_FLAG = 6;
}
