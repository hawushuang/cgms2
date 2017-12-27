package com.microtechmd.pda.library.entity;

public class ParameterMonitor 
{
	public static final int PARAM_DATETIME = 0;
	public static final int PARAM_HISTORY = 1;
	public static final int PARAM_EVENT = 2;
	public static final int PARAM_STATUS = 3;
	public static final int PARAM_STATISTICS = 4;
	public static final int COUNT_PARAM = 5;
	
	public static final int EVENT_BATTERY = 0;
	public static final int EVENT_STARTUP = 1;
	public static final int EVENT_PDA_BATTERY = 2;
	public static final int EVENT_PDA_ERROR = 3;
	public static final int COUNT_EVENT = 4;
	
	public static final int STATUS_INDEX_BATTERY_CAPACITY = 0;
	public static final int STATUS_INDEX_RESERVOIR_AMOUNT = 1;
	public static final int STATUS_INDEX_BASAL_RATE = 0;
	public static final int STATUS_INDEX_BOLUS_RATE = 1;
}
