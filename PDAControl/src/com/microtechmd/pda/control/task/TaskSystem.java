package com.microtechmd.pda.control.task;


import android.os.Handler;

import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.control.platform.DeviceLED;
import com.microtechmd.pda.library.entity.ParameterSystem;
import com.microtechmd.pda.library.entity.ValueInt;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.service.ServiceBase;
import com.microtechmd.pda.library.service.TaskBase;


public class TaskSystem extends TaskBase
{
	private static TaskSystem sInstance = null;
	private static DeviceLED sDeviceLED = null;

	private int mReaction = ParameterSystem.REACTION_NORMAL;


	private TaskSystem(ServiceBase service)
	{
		super(service);
	}


	public static synchronized TaskSystem getInstance(final ServiceBase service)
	{
		if (sInstance == null)
		{
			sInstance = new TaskSystem(service);

			if (sDeviceLED == null)
			{
				sDeviceLED = DeviceLED.getInstance();
			}
		}

		return sInstance;
	}


	@Override
	public void handleMessage(EntityMessage message)
	{
		switch (message.getOperation())
		{
			case EntityMessage.OPERATION_SET:
				setParameter(message);
				break;

			case EntityMessage.OPERATION_GET:
				getParameter(message);
				break;

			case EntityMessage.OPERATION_EVENT:
				handleEvent(message);
				break;

			case EntityMessage.OPERATION_NOTIFY:
				handleNotification(message);
				break;

			case EntityMessage.OPERATION_ACKNOWLEDGE:
				handleAcknowledgement(message);
				break;

			default:
				break;
		}
	}


	private void setParameter(final EntityMessage message)
	{
		mLog.Debug(getClass(), "Set parameter: " + message.getParameter());

		switch (message.getParameter())
		{
			case ParameterSystem.PARAM_REACTION:
				setReaction(new ValueInt(message.getData()).getValue());
				break;

			default:
				message.setTargetAddress(ParameterGlobal.ADDRESS_REMOTE_MASTER);
				mService.onReceive(message);
				break;
		}
	}


	private void getParameter(final EntityMessage message)
	{
	}


	private void handleEvent(final EntityMessage message)
	{
		mLog.Debug(getClass(), "Handle event: " + message.getEvent());

		message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_VIEW);
		mService.onReceive(message);
	}


	private void handleNotification(final EntityMessage message)
	{
	}


	private void handleAcknowledgement(final EntityMessage message)
	{
		if (message.getSourcePort() == ParameterGlobal.PORT_SYSTEM)
		{
			mLog.Debug(getClass(),
				"Acknowledge port comm: " + message.getData()[0]);

			message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_VIEW);
			mService.onReceive(message);
		}
	}


	private void setReaction(int value)
	{
		final int REACTION_CYCLE_ALARM = 1000;

		if (value == mReaction)
		{
			return;
		}

		mReaction = value;

		if (value == ParameterSystem.REACTION_ALARM)
		{
			final Handler handlerReaction = new Handler();

			handlerReaction.postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					if (mReaction == ParameterSystem.REACTION_ALARM)
					{
						handlerReaction.postDelayed(this, REACTION_CYCLE_ALARM);

						if (sDeviceLED.get(DeviceLED.COLOR_YELLOW) > 0)
						{
							sDeviceLED.set(DeviceLED.COLOR_YELLOW, 0);
						}
						else
						{
							sDeviceLED.set(DeviceLED.COLOR_YELLOW, 100);
						}
					}
					else
					{
						handlerReaction.removeCallbacks(this);

						if (mReaction == ParameterSystem.REACTION_ALERT)
						{
							sDeviceLED.set(DeviceLED.COLOR_YELLOW, 100);
						}
						else
						{
							sDeviceLED.set(DeviceLED.COLOR_YELLOW, 0);
						}
					}
				}
			}, REACTION_CYCLE_ALARM);
		}
		else if (value == ParameterSystem.REACTION_ALERT)
		{
			sDeviceLED.set(DeviceLED.COLOR_YELLOW, 100);
		}
		else
		{
			sDeviceLED.set(DeviceLED.COLOR_YELLOW, 0);
		}
	}
}
