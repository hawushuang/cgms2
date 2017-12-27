package com.microtechmd.pda.ui.activity;


import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;

import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.monitor.StatusPump;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.manager.SharePreferenceManager;
import com.microtechmd.pda.ui.activity.fragment.FragmentGraph;
import com.microtechmd.pda.ui.widget.LockScreenView;
import com.microtechmd.pda.util.AndroidSystemInfoUtil;
import com.microtechmd.pda.util.TimeUtil;


public class ActivityUnlock extends ActivityPDA
{
	private static final String STRING_UNKNOWN = " -.- ";
	private static final long UNLOCK_SCREEN_TIME = 3000;
	private static final String TAG_GRAPH = "graph";
	private long mBolusKeyDownTime = 0;
	private long mVolumeDownKeyDownTime = 0;
	private FragmentGraph mFragmentGraph = null;


	@Override
	public void onBackPressed()
	{
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		long KeyNowTime = System.currentTimeMillis();
		LockScreenView lsv =
			(LockScreenView)findViewById(R.id.image_view_unlock);
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (event.getRepeatCount() == 0)
				{
					mVolumeDownKeyDownTime = KeyNowTime;
					if (mBolusKeyDownTime > 0)
					{
						lsv.setUnlockPercent(-1f, LockScreenView.E_DOWN);
					}
				}
				else if (event.getRepeatCount() == 1)
				{
					mVolumeDownKeyDownTime = KeyNowTime;
				}
				else
				{
					onKeyDownDeal_Unlocking(KeyNowTime);
				}
				return true;
			case KeyEvent.KEYCODE_VOLUME_UP:

				break;

			case ApplicationPDA.KEY_CODE_BOLUS:
				if (event.getRepeatCount() == 0)
				{
					mBolusKeyDownTime = KeyNowTime;
					if (mVolumeDownKeyDownTime > 0)
					{
						lsv.setUnlockPercent(-1f, LockScreenView.E_DOWN);
					}
				}
				else if (event.getRepeatCount() == 1)
				{
					mBolusKeyDownTime = KeyNowTime;
				}
				else
				{
					onKeyDownDeal_Unlocking(KeyNowTime);
				}
				return true;

		}
		return super.onKeyDown(keyCode, event);
	}


	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		LockScreenView lsv =
			(LockScreenView)findViewById(R.id.image_view_unlock);
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				mVolumeDownKeyDownTime = 0;
				if (mBolusKeyDownTime == 0)
				{
					lsv.setUnlockPercent(-1f, LockScreenView.E_UP);
				}
				return true;
			case KeyEvent.KEYCODE_VOLUME_UP:

				break;

			case ApplicationPDA.KEY_CODE_BOLUS:
				mBolusKeyDownTime = 0;
				if (mVolumeDownKeyDownTime == 0)
				{
					lsv.setUnlockPercent(-1f, LockScreenView.E_UP);
				}
				return true;

		}
		return super.onKeyUp(keyCode, event);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
		setContentView(R.layout.fragment_unlock);
		setStatusButtonVisibility(false);
		initViews();

		if (mFragmentGraph == null)
		{
			mFragmentGraph = new FragmentGraph();
		}

		getSupportFragmentManager().beginTransaction()
			.replace(R.id.layout_graph, mFragmentGraph, TAG_GRAPH).commit();
		updateStatus(ActivityMain.getStatus());
	}


	@Override
	protected void onResume()
	{
		super.onResume();

		getStatusBar().setGlucose(false);
		updateScreenBrightness();
		updateCalendar();
	}


	@Override
	protected void onDestroy()
	{
		int time = SharePreferenceManager.getDisplayTimeout(mBaseActivity);
		setTimeout(time);
		super.onDestroy();
	}


	@Override
	protected void onHomePressed()
	{
	}


	@Override
	protected void onTimeTick()
	{
		super.onTimeTick();

		updateCalendar();
	}


	@Override
	protected void handleNotification(final EntityMessage message)
	{
		super.handleNotification(message);

		if (message.getSourcePort() == ParameterGlobal.PORT_MONITOR)
		{
			if (message.getParameter() == ParameterMonitor.PARAM_STATUS)
			{
				StatusPump status = new StatusPump(message.getData());
				updateStatus(status);
				ActivityMain.setStatus(status);
			}
		}
	}


	private void initViews()
	{
		LockScreenView lsv =
			(LockScreenView)findViewById(R.id.image_view_unlock);
		lsv.setUnlockListener(new LockScreenView.UnlockListener()
		{
			@Override
			public void unlock()
			{
				getStatusBar().setGlucose(true);
				finish();
			}
		});
	}


	private boolean onKeyDownDeal_Unlocking(long KeyNowTime)
	{
		LockScreenView lsv =
			(LockScreenView)findViewById(R.id.image_view_unlock);
		if (mBolusKeyDownTime > 0 && mVolumeDownKeyDownTime > 0)
		{
			long min = 0;

			min = mBolusKeyDownTime > mVolumeDownKeyDownTime ? mBolusKeyDownTime
				: mVolumeDownKeyDownTime;
			float x = (float)(KeyNowTime - min) / (float)UNLOCK_SCREEN_TIME;
			lsv.setUnlockPercent(x, LockScreenView.E_MOVE);
			return true;
		}
		else
		{
			return false;
		}
	}


	private void setTimeout(int time)
	{
		Settings.System.putInt(getContentResolver(),
			Settings.System.SCREEN_OFF_TIMEOUT, time);
	}


	private void updateStatus(StatusPump status)
	{
		if (status == null)
		{
			((TextView)findViewById(R.id.text_view_glucose))
				.setText(STRING_UNKNOWN);
		}
		else
		{
			mLog.Debug(getClass(), "Update status: " + status.getBasalRate());

			TextView textView = (TextView)findViewById(R.id.text_view_glucose);
			textView.setText(getGlucoseValue(
				(status.getBasalRate() & 0xFFFF) * GLUCOSE_UNIT_MG_STEP,
				false));
		}
	}


	private void updateCalendar()
	{
		long currentTime = System.currentTimeMillis();
		boolean format = getDataStorage(ActivityPDA.class.getSimpleName())
			.getBoolean(SETTING_TIME_FORMAT, true);
		String time1 = TimeUtil.getStatusTimeByTimeMillis(currentTime, "HH:mm");
		if (format)
		{
			TextView tv = (TextView)findViewById(R.id.text_view_time);
			tv.setText(time1);
		}
		else
		{
			String time2 = TimeUtil.getStatusTimeByTimeMillis(currentTime, "a");
			TextView tv = (TextView)findViewById(R.id.text_view_time);
			tv.setText(time1);
			tv = (TextView)findViewById(R.id.text_view_ampm);
			tv.setText(time2);
		}
		String time3 =
			TimeUtil.getStatusTimeByTimeMillis(currentTime, "EE, MMMM d");
		if (AndroidSystemInfoUtil.getLanguage().getLanguage().equals("zh"))
		{
			time3 += getString(R.string.day);
		}
		time3 += ", " + TimeUtil.getStatusTimeByTimeMillis(currentTime, "yyyy");
		if (AndroidSystemInfoUtil.getLanguage().getLanguage().equals("zh"))
		{
			time3 += getString(R.string.year);
		}
		TextView tv = (TextView)findViewById(R.id.text_view_date);
		tv.setText(time3);
	}
}
