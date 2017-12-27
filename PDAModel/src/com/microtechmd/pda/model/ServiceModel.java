package com.microtechmd.pda.model;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Messenger;

import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.service.ServiceBase;
import com.microtechmd.pda.model.task.TaskComm;
import com.microtechmd.pda.model.task.TaskMonitor;


public final class ServiceModel extends ServiceBase
{
	// Constant and variable definition

	private Messenger mMessengerView = null;
	private Messenger mMessengerControl = null;
	private ControlConnection mControlConnection = null;


	// Inner class definition

	private final class ControlConnection
		implements ServiceConnection
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			mLog.Debug(getClass(), "Connect control service");
			mMessengerControl = new Messenger(service);
		}


		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			mLog.Debug(getClass(), "Disconnect control service");
			mMessengerControl = null;
		}
	}


	// Method definition

	@Override
	public void onCreate()
	{
		super.onCreate();

		setExternalMessageHandler(new ExternalMessageHandler()
		{
			@Override
			public void handleMessage(final EntityMessage message,
				Messenger messengerReply)
			{
				if (messengerReply != null)
				{
					if (message
						.getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_VIEW)
					{
						mMessengerView = messengerReply;
					}

					if (message
						.getSourceAddress() == ParameterGlobal.ADDRESS_LOCAL_CONTROL)
					{
						mMessengerControl = messengerReply;
					}
				}

				handleMessageExternal(message);
			}
		});

		if (mControlConnection == null)
		{
			mControlConnection = new ControlConnection();
			bindService(new Intent("com.microtechmd.pda.control"),
				mControlConnection, Context.BIND_AUTO_CREATE);
		}
	}


	@Override
	public void onDestroy()
	{
		mMessengerView = null;
		mMessengerControl = null;
		mControlConnection = null;
		super.onDestroy();
	}


	private synchronized void handleMessageExternal(final EntityMessage message)
	{
		mLog.Debug(getClass(), "Handle message: " + "Source Address:" +
			message.getSourceAddress() + " Target Address:" +
			message.getTargetAddress() + " Source Port:" +
			message.getSourcePort() + " Target Port:" +
			message.getTargetPort() + " Operation:" + message.getOperation() +
			" Parameter:" + message.getParameter());

		switch (message.getTargetAddress())
		{
			case ParameterGlobal.ADDRESS_REMOTE_MASTER:
			case ParameterGlobal.ADDRESS_REMOTE_SLAVE:
			case ParameterGlobal.ADDRESS_LOCAL_CONTROL:
				sendRemoteMessage(mMessengerControl, message);
				break;

			case ParameterGlobal.ADDRESS_LOCAL_VIEW:
				sendRemoteMessage(mMessengerView, message);
				break;

			default:
				handleMessageInternal(message);
				break;
		}

		mLog.Debug(getClass(), "Handle message end");
	}


	private void handleMessageInternal(final EntityMessage message)
	{
		switch (message.getTargetPort())
		{
			case ParameterGlobal.PORT_COMM:
				TaskComm taskComm = TaskComm.getInstance(ServiceModel.this);
				taskComm.handleMessage(message);
				break;

			case ParameterGlobal.PORT_MONITOR:
				TaskMonitor taskMonitor =
					TaskMonitor.getInstance(ServiceModel.this);
				taskMonitor.handleMessage(message);
				break;

			default:
				break;
		}
	}
}