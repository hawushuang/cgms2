package com.microtechmd.pda.control.platform;

public final class DeviceLED 
{
	public final static int COLOR_RED = 1;
	public final static int COLOR_YELLOW = 0;
	
	private static DeviceLED sInstance = null;

	private DeviceLED() 
	{
	}
	
	public static synchronized DeviceLED getInstance()
	{
		if (sInstance == null)
		{
			sInstance = new DeviceLED();
		}
		
		return sInstance;
	}
	
	public void set(int color, int brightness)
	{
		JNIInterface jniInterface = JNIInterface.getInstance();
		jniInterface.setLED(color, brightness);
	}
	
	public int get(int color)
	{
		JNIInterface jniInterface = JNIInterface.getInstance();
		return jniInterface.getLED(color);
	}
}
