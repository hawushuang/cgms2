package com.microtechmd.pda.library.service;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;

import com.microtechmd.pda.library.entity.DataStorage;
import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.utility.LogPDA;


public class ServiceBase extends Service
	implements EntityMessage.Listener
{
	// Constant and variable definition

	private static PowerManager.WakeLock sWakeLock = null;
	private static EntityMessage sMessageBuffer = null;
	private static ExternalMessageHandler sMessageHandler = null;

	protected LogPDA mLog = null;
	private DataStorage mDataStorage = null;
	private Messenger mMessengerSelf = null;


	// Inner class definition

	protected interface ExternalMessageHandler
	{
		void handleMessage(EntityMessage message, Messenger messengerReply);
	}


	private static final class MessageHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg)
		{
			acquireWakeLock();

			sMessageBuffer.setAll(msg.getData());

			if (sMessageHandler != null)
			{
				sMessageHandler.handleMessage(sMessageBuffer, msg.replyTo);
			}

			releaseWakeLock();
		}
	}


	// Method definition

	private static synchronized void acquireWakeLock()
	{
		if (!sWakeLock.isHeld())
		{
//			sWakeLock.acquire();
		}
	}


	private static synchronized void releaseWakeLock()
	{
		if (sWakeLock.isHeld())
		{
			sWakeLock.release();
		}
	}


	@Override
	public void onCreate()
	{
		super.onCreate();

		if (mLog == null)
		{
			mLog = new LogPDA();
		}

		if (mMessengerSelf == null)
		{
			mMessengerSelf = new Messenger(new MessageHandler());
		}

		if (sMessageBuffer == null)
		{
			sMessageBuffer = new EntityMessage();
		}

		if (sWakeLock == null)
		{
			PowerManager powerManager =
				(PowerManager)getSystemService(Context.POWER_SERVICE);
			sWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				getClass().getSimpleName());
		}
	}


	@Override
	public void onDestroy()
	{
		mLog = null;
		mDataStorage = null;
		mMessengerSelf = null;
		sWakeLock = null;
		sMessageBuffer = null;
		sMessageHandler = null;
		super.onDestroy();
	}


	@Override
	public IBinder onBind(Intent intent)
	{
		mLog.Debug(getClass(), "Bind service");
		return mMessengerSelf.getBinder();
	}


	@Override
	public boolean onUnbind(Intent intent)
	{
		mLog.Debug(getClass(), "Unbind service");
		return super.onUnbind(intent);
	}


	@Override
	public void onRebind(Intent intent)
	{
		mLog.Debug(getClass(), "Rebind service");
		super.onRebind(intent);
	}


	@Override
	public void onReceive(EntityMessage message)
	{
		if (sMessageHandler != null)
		{
			sMessageHandler.handleMessage(message, null);
		}
	}


	public DataStorage getDataStorage(String name)
	{
		if (name == null)
		{
			name = getClass().getSimpleName();
		}

		if (mDataStorage == null)
		{
			mDataStorage = new DataStorage(this, name);
		}

		if (!mDataStorage.getName().equals(name))
		{
			mDataStorage = new DataStorage(this, name);
		}

		return mDataStorage;
	}


	protected void setExternalMessageHandler(
		ExternalMessageHandler messageHandler)
	{
		sMessageHandler = messageHandler;
	}


	protected void sendRemoteMessage(final Messenger messenger,
		final EntityMessage message)
	{
		if (messenger != null)
		{
			Message messageRemote = Message.obtain();
			messageRemote.setData(message.getAll());
			messageRemote.replyTo = mMessengerSelf;

			try
			{
				messenger.send(messageRemote);
			}
			catch (RemoteException e)
			{
				mLog.Debug(getClass(), "Send remote message fail");
				e.printStackTrace();
			}
		}
	}
}
