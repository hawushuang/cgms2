package com.microtechmd.pda.library.utility;

import android.util.Log;

public class LogPDA 
{
	public static final int STATE_ENABLE = 0;
	public static final int STATE_DISABLE = 1;
	
	private int mState = STATE_ENABLE;
	
		
	public void setState(int state)
	{
		mState = state;
	}
	
	public int getState()
	{
		return mState;
	}
	
	public void Verbose(Class<?> classLog, String text)
	{
		if (mState == STATE_ENABLE)
		{
			Log.v(classLog.getSimpleName(), text);
		}
	}
	
	public void Debug(Class<?> classLog, String text)
	{
		if (mState == STATE_ENABLE)
		{
			Log.d(classLog.getSimpleName(), text);
		}
	}
	
	public void Debug(String tag, String text)
	{
		if (mState == STATE_ENABLE)
		{
			Log.d(tag, text);
		}
	}
	
	public void Info(Class<?> classLog, String text)
	{
		if (mState == STATE_ENABLE)
		{
			Log.i(classLog.getSimpleName(), text);
		}
	}
	
	public void Warning(Class<?> classLog, String text)
	{
		if (mState == STATE_ENABLE)
		{
			Log.w(classLog.getSimpleName(), text);
		}
	}
	
	public void Error(Class<?> classLog, String text)
	{
		if (mState == STATE_ENABLE)
		{
			Log.e(classLog.getSimpleName(), text);
		}
	}
}
