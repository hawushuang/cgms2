package com.microtechmd.pda.library.entity;

public class ParameterComm 
{
	public static final int PARAM_RF_STATE = 0;
	public static final int PARAM_RF_SIGNAL = 1;
	public static final int PARAM_RF_LOCAL_ADDRESS = 2;
	public static final int PARAM_RF_REMOTE_ADDRESS = 3;
	public static final int PARAM_RF_BROADCAST_SWITCH = 4;
	public static final int PARAM_BROADCAST_DATA = 5;
	public static final int PARAM_BROADCAST_OFFSET = 6;
	public static final int COUNT_PARAM = 7;
	
	public static final byte RF_STATE_IDLE = 0;
	public static final byte RF_STATE_BROADCAST = 1;
	public static final byte RF_STATE_CONNECTED = 2;
	public static final byte COUNT_RF_STATE = 3;

	public static final byte BROADCAST_OFFSET_ALL = 0;
	public static final byte BROADCAST_OFFSET_STATUS = 8;
	public static final byte BROADCAST_OFFSET_EVENT = 12;
}
