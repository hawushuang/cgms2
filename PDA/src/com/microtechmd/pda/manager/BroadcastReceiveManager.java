package com.microtechmd.pda.manager;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;


public class BroadcastReceiveManager
{

	public static final String ACTION_HOME = ".action_home";
	public static final String ACTION_RESTART = ".action_restart";
	public static final String ACTION_BASAL_RESUME = ".action_basal_resume";
	public static final String ACTION_PUMP_DISCONNECTED = ".action_pump_disconnected";
	public static final String ACTION_PUMP_CONNECTED = ".action_pump_connected";
	public static final String ACTION_RESERVIOR_REWINDED = ".action_pump_rewinded";
	public static final String ACTION_PAUSE_MUSIC = ".action_pause_music";
	public static final String ACTION_RESUME_MUSIC = ".action_resume_music";
	// public static final String ACTION_TIMER = ".action_timer";

	public static final String EXTRA_DISCONNECT_RESULT = "disconnect_result";
	public static final String EXTRA_CONNECT_RESULT = "connect_result";
	public static final String EXTRA_REWIND_RESULT = "connect_result";

	private static final int MSG_HOME = 1;
	private static final int MSG_TIMER = 2;
	private static final int MSG_BATTERY = 3;
	private static final int MSG_BASAL_RESUME = 4;
	private static final int MSG_SCREEN_ON = 5;
	private static final int MSG_SCREEN_OFF = 6;
	private static final int MSG_PUMP_DISCONNECTED = 7;
	private static final int MSG_PUMP_CONNECTED = 8;
	private static final int MSG_RESERVIOR_REWINDED = 9;
	private static final int MSG_RESTART = 10;
	private static final int MSG_PAUSE_MUSIC = 11;
	private static final int MSG_RESUME_MUSIC = 12;

	private final Context mContext;

	private OnNotifyBroadcastListener mOnNotifyBroadcastListener;


	public OnNotifyBroadcastListener getOnNotifyBroadcastListener()
	{
		return mOnNotifyBroadcastListener;
	}


	public void setOnNotifyBroadcastListener(
		OnNotifyBroadcastListener onNotifyBroadcastListener)
	{
		this.mOnNotifyBroadcastListener = onNotifyBroadcastListener;
	}


	public interface OnNotifyBroadcastListener
	{
		void onNotifyHome();


		void onNotifyRestart();


		void onNotifyTime();


		void onNotifyBattery(int level, boolean isCharging);


		void onNotifyScreenOn();


		void onNotifyScreenOff();


		void onNotifyBasalResume();


		void onNotifyPumpDisconnected();


		void onNotifyPumpConnected(boolean success);


		void onNotifyReserviorRewinded();


		void onPauseMusic();


		void onResumeMusic();

	}


	public BroadcastReceiveManager(Context context)
	{
		mContext = context;
		mHandler.sendEmptyMessage(MSG_TIMER);
		IntentFilter filter = new IntentFilter();
		filter.addAction(getFullAction(context, ACTION_HOME));
		filter.addAction(getFullAction(context, ACTION_RESTART));
		filter.addAction(getFullAction(context, ACTION_BASAL_RESUME));
		filter.addAction(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(Intent.ACTION_BATTERY_CHANGED);
		filter.addAction(getFullAction(context, ACTION_PUMP_DISCONNECTED));
		filter.addAction(getFullAction(context, ACTION_PUMP_CONNECTED));
		filter.addAction(getFullAction(context, ACTION_RESERVIOR_REWINDED));
		filter.addAction(getFullAction(context, ACTION_PAUSE_MUSIC));
		filter.addAction(getFullAction(context, ACTION_RESUME_MUSIC));
		// filter.addAction(getFullAction(context, ACTION_TIMER));
		context.registerReceiver(mBroadcastReceiver, filter);
	}


	public static String getFullAction(Context context, String actionName)
	{
		return context.getPackageName() + actionName;
	}


	public void onDestory()
	{
		mContext.unregisterReceiver(mBroadcastReceiver);
		mHandler.removeMessages(MSG_HOME);
		mHandler.removeMessages(MSG_TIMER);
		mHandler.removeMessages(MSG_BATTERY);
		mHandler.removeMessages(MSG_PUMP_DISCONNECTED);
		mHandler.removeMessages(MSG_PUMP_CONNECTED);
		mHandler.removeMessages(MSG_RESERVIOR_REWINDED);
	}


	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver()
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			if (action.equals(getFullAction(context, ACTION_HOME)))
			{
				mHandler.sendEmptyMessage(MSG_HOME);
			}
			else if (action.equals(Intent.ACTION_BATTERY_CHANGED))
			{
				int battery = intent.getIntExtra("level", 0);
				int status = intent.getIntExtra("status", -1);
				Message msg = new Message();
				msg.what = MSG_BATTERY;
				msg.arg1 = battery;

				if ((status == BatteryManager.BATTERY_STATUS_CHARGING)
					|| (status == BatteryManager.BATTERY_STATUS_FULL))
				{
					msg.arg2 = 1;
				}
				else
				{
					msg.arg2 = 0;
				}

				mHandler.sendMessage(msg);
			}
			else if (action.equals(Intent.ACTION_SCREEN_ON))
			{
				mHandler.sendEmptyMessage(MSG_SCREEN_ON);
			}
			else if (action.equals(Intent.ACTION_SCREEN_OFF))
			{
				mHandler.sendEmptyMessage(MSG_SCREEN_OFF);
			}
			else if (action.equals(getFullAction(context, ACTION_BASAL_RESUME)))
			{
				mHandler.sendEmptyMessage(MSG_BASAL_RESUME);
			}
			else if (action.equals(getFullAction(context,
				ACTION_PUMP_DISCONNECTED)))
			{
				mHandler.sendEmptyMessageDelayed(MSG_PUMP_DISCONNECTED, 2000);
			}
			else if (action
				.equals(getFullAction(context, ACTION_PUMP_CONNECTED)))
			{
				Boolean result = intent.getBooleanExtra(EXTRA_CONNECT_RESULT,
					true);
				Message msg = new Message();
				msg.what = MSG_PUMP_CONNECTED;
				msg.obj = result;
				mHandler.sendMessageDelayed(msg, 2000);
			}
			else if (action.equals(getFullAction(context,
				ACTION_RESERVIOR_REWINDED)))
			{
				mHandler.sendEmptyMessageDelayed(MSG_RESERVIOR_REWINDED, 2000);
			}
			else if (action.equals(getFullAction(context, ACTION_RESTART)))
			{
				mHandler.sendEmptyMessage(MSG_RESTART);
			}
			else if (action.equals(getFullAction(context, ACTION_PAUSE_MUSIC)))
			{
				mHandler.sendEmptyMessage(MSG_PAUSE_MUSIC);
			}
			else if (action.equals(getFullAction(context, ACTION_RESUME_MUSIC)))
			{
				mHandler.sendEmptyMessage(MSG_RESUME_MUSIC);
			} /*
			 * else if (action.equals(getFullAction(context, ACTION_TIMER))) {
			 * if (mOnNotifyBroadcastListener != null)
			 * mOnNotifyBroadcastListener.onNotifyTime(); }
			 */
		}

	};

	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler()
	{

		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case MSG_HOME:
				{
					if (mOnNotifyBroadcastListener != null)
						mOnNotifyBroadcastListener.onNotifyHome();
					break;
				}
				case MSG_TIMER:
				{
					if (mOnNotifyBroadcastListener != null)
						mOnNotifyBroadcastListener.onNotifyTime();
					mHandler.sendEmptyMessageDelayed(MSG_TIMER, 1000);
					break;
				}
				case MSG_BATTERY:
				{
					if (mOnNotifyBroadcastListener != null)

						if (msg.arg2 == 0)
						{
							mOnNotifyBroadcastListener.onNotifyBattery(
								msg.arg1, false);
						}
						else
						{
							mOnNotifyBroadcastListener.onNotifyBattery(
								msg.arg1, true);
						}

					break;
				}
				case MSG_SCREEN_ON:
				{
					if (mOnNotifyBroadcastListener != null)
						mOnNotifyBroadcastListener.onNotifyScreenOn();
					break;
				}
				case MSG_SCREEN_OFF:
				{
					if (mOnNotifyBroadcastListener != null)
						mOnNotifyBroadcastListener.onNotifyScreenOff();
					break;
				}
				case MSG_BASAL_RESUME:
				{
					if (mOnNotifyBroadcastListener != null)
						mOnNotifyBroadcastListener.onNotifyBasalResume();
					break;
				}
				case MSG_PUMP_DISCONNECTED:
				{
					if (mOnNotifyBroadcastListener != null)
					{
						mOnNotifyBroadcastListener.onNotifyPumpDisconnected();
					}
					break;
				}
				case MSG_PUMP_CONNECTED:
				{
					if (mOnNotifyBroadcastListener != null)
					{
						Boolean result = (msg.obj == null) ? true
							: (Boolean) msg.obj;
						mOnNotifyBroadcastListener
							.onNotifyPumpConnected(result);
					}
					break;
				}
				case MSG_RESERVIOR_REWINDED:
				{
					if (mOnNotifyBroadcastListener != null)
					{
						mOnNotifyBroadcastListener.onNotifyReserviorRewinded();
					}
					break;
				}
				case MSG_RESTART:
				{
					if (mOnNotifyBroadcastListener != null)
					{
						mOnNotifyBroadcastListener.onNotifyRestart();
					}
					break;
				}
				case MSG_PAUSE_MUSIC:
				{
					if (mOnNotifyBroadcastListener != null)
					{
						mOnNotifyBroadcastListener.onPauseMusic();
					}
					break;
				}
				case MSG_RESUME_MUSIC:
				{
					if (mOnNotifyBroadcastListener != null)
					{
						mOnNotifyBroadcastListener.onResumeMusic();
					}
					break;
				}
			}
		}

	};

}
