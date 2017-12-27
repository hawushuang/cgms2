package com.microtechmd.pda.control.task;


import android.os.Handler;

import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.control.platform.DeviceBLE;
import com.microtechmd.pda.library.entity.DataBundle;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Event;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.entity.monitor.Status;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.service.ServiceBase;
import com.microtechmd.pda.library.service.TaskBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;


public final class TaskComm extends TaskBase
{
	// Constant and variable definition

	private static final String SETTING_BROADCAST_SWITCH = "broadcast_switch";
	private static final int REMOTE_DEVICE_COUNT = 2;
	private static final int INTERVAL_LINK_CHECK = 100;
	private static final int INTERVAL_CONNECTION_TIMEOUT_SHORT = 200;
	private static final int INTERVAL_CONNECTION_TIMEOUT_LONG = 10000;

	private static TaskComm sInstance = null;
	private static DeviceBLE sDeviceBLE = null;

	private boolean mBroadcastSwitch = false;
	private byte mRFState = ParameterComm.COUNT_RF_STATE;
	private byte mRFSignal = 0;
	private int mConnectionTimeout = 0;
	private int mConnectionTimer = 0;
	private MessageList[] mMessageList = null;
	private EntityMessage[] mMessageRequest = null;
	private EntityMessage[] mMessageAcknowledge = null;
	private Handler[] mHandlerLink = null;
	private Runnable[] mRunnableLink = null;


	// Inner class definition

	private final class MessageList extends LinkedList<EntityMessage>
	{
		private static final long serialVersionUID = 1L;
	}

	private final class Broadcast extends DataBundle
	{
		private static final int BROADCAST_LENGTH = 20;
		private static final int DATA_LENGTH = 18;

		private static final String IDENTIFIER = "broadcast";
		private static final String KEY_DATA = IDENTIFIER + "_data";
		private static final String KEY_RF_SIGNAL = IDENTIFIER + "_rf_signal";


		private Broadcast()
		{
			super();
		}


		private Broadcast(byte[] byteArray)
		{
			super();
			setByteArray(byteArray);
		}


		private byte[] getData()
		{
			return getExtras(KEY_DATA);
		}


		private int getRFSignal()
		{
			return (int)getByte(KEY_RF_SIGNAL);
		}


		private void setData(byte[] data)
		{
			setExtras(KEY_DATA, data);
		}


		private void setRFSignal(int signal)
		{
			setByte(KEY_RF_SIGNAL, (byte)signal);
		}


		@Override
		public byte[] getByteArray()
		{
			final DataOutputStreamLittleEndian dataOutputStream;
			final ByteArrayOutputStream byteArrayOutputStream;

			byteArrayOutputStream = new ByteArrayOutputStream();
			dataOutputStream =
				new DataOutputStreamLittleEndian(byteArrayOutputStream);

			try
			{
				byteArrayOutputStream.reset();

				final byte[] data = getData();

				if (data != null)
				{
					dataOutputStream.write(data);
				}

				dataOutputStream.writeByte((byte)getRFSignal());
				dataOutputStream.writeByte(0);
				dataOutputStream.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			return byteArrayOutputStream.toByteArray();
		}


		@Override
		public void setByteArray(byte[] byteArray)
		{
			if (byteArray == null)
			{
				return;
			}

			if (byteArray.length >= BROADCAST_LENGTH)
			{
				final DataInputStreamLittleEndian dataInputStream;
				final ByteArrayInputStream byteArrayInputStream;

				byteArrayInputStream = new ByteArrayInputStream(byteArray);
				dataInputStream =
					new DataInputStreamLittleEndian(byteArrayInputStream);

				try
				{
					clearBundle();
					final byte[] data = new byte[DATA_LENGTH];
					dataInputStream.read(data, 0, DATA_LENGTH);
					setData(data);
					setRFSignal((int)dataInputStream.readByte());
					dataInputStream.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}


	// Method definition

	private TaskComm(ServiceBase service)
	{
		super(service);

		mLog.Debug(getClass(), "Initialization");

		mConnectionTimeout = INTERVAL_CONNECTION_TIMEOUT_SHORT;
		mConnectionTimer = 0;

		mBroadcastSwitch = mService.getDataStorage(null)
			.getBoolean(SETTING_BROADCAST_SWITCH, false);

		if (mMessageList == null)
		{
			mMessageList = new MessageList[REMOTE_DEVICE_COUNT];
		}

		if (mMessageRequest == null)
		{
			mMessageRequest = new EntityMessage[REMOTE_DEVICE_COUNT];
		}

		if (mMessageAcknowledge == null)
		{
			mMessageAcknowledge = new EntityMessage[REMOTE_DEVICE_COUNT];
		}

		if (mHandlerLink == null)
		{
			mHandlerLink = new Handler[REMOTE_DEVICE_COUNT];
		}

		if (mRunnableLink == null)
		{
			mRunnableLink = new Runnable[REMOTE_DEVICE_COUNT];
		}

		for (int i = 0; i < REMOTE_DEVICE_COUNT; i++)
		{
			mMessageList[i] = new MessageList();
			mMessageRequest[i] = null;
			mMessageAcknowledge[i] = null;
			mHandlerLink[i] = new Handler();
			mRunnableLink[i] = null;
		}

		Handler handler = new Handler();
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				// Get broadcast data from RF slave device and switch it into
				// active broadcast mode
				EntityMessage message =
					new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
						ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
						ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
						ParameterComm.PARAM_BROADCAST_OFFSET, new byte[]
						{
							ParameterComm.BROADCAST_OFFSET_ALL
						});
				mService.onReceive(message);
				message.setOperation(EntityMessage.OPERATION_GET);
				message.setData(null);
/*				message.setParameter(ParameterComm.PARAM_BROADCAST_DATA);
				mService.onReceive(message);*/
				message.setParameter(ParameterComm.PARAM_RF_STATE);
				mService.onReceive(message);
			}
		});
	}


	public static synchronized TaskComm getInstance(final ServiceBase service)
	{
		sDeviceBLE = DeviceBLE.getInstance(service);

		if (sInstance == null)
		{
			sInstance = new TaskComm(service);
		}

		return sInstance;
	}


	@Override
	public void handleMessage(final EntityMessage message)
	{
		switch (message.getTargetAddress())
		{
			case ParameterGlobal.ADDRESS_REMOTE_MASTER:
			case ParameterGlobal.ADDRESS_REMOTE_SLAVE:
				handleMessageRemote(message);
				break;

			case ParameterGlobal.ADDRESS_LOCAL_CONTROL:

				if (message.getTargetPort() == ParameterGlobal.PORT_COMM)
				{
					handleMessageLocal(message);
				}
				else
				{
					mService.onReceive(message);
				}

				break;

			default:
				mService.onReceive(message);
				break;
		}
	}


	private void handleMessageRemote(final EntityMessage message)
	{
		// Check if the message is received from remote device or required
		// sending to remote device
		if (message.getSourceAddress() == message.getTargetAddress())
		{
			receiveRemoteMessage(message);
		}
		else
		{
			if ((message.getOperation() == EntityMessage.OPERATION_SET) ||
				(message.getOperation() == EntityMessage.OPERATION_GET) ||
				(message.getOperation() == EntityMessage.OPERATION_NOTIFY) ||
				(message.getOperation() == EntityMessage.OPERATION_ACKNOWLEDGE))
			{
				message.setMode(EntityMessage.MODE_ACKNOWLEDGE);
			}
			else if (message.getOperation() == EntityMessage.OPERATION_EVENT)
			{
				message.setMode(EntityMessage.MODE_NO_ACKNOWLEDGE);
			}
			else
			{
				return;
			}

			final MessageList messageList =
				mMessageList[message.getTargetAddress()];
			messageList.add(new EntityMessage(message.toByteArray()));

			mLog.Debug(getClass(), "Add message list: " + messageList.size() +
				", Address: " + message.getTargetAddress());

			if (messageList.size() <= 1)
			{
				sendRemoteMessage(message);
			}
		}
	}


	private void handleMessageLocal(final EntityMessage message)
	{
		switch (message.getOperation())
		{
			case EntityMessage.OPERATION_SET:
				setParameter(message);
				break;

			case EntityMessage.OPERATION_GET:
				getParameter(message);
				break;

			case EntityMessage.OPERATION_NOTIFY:
				handleNotification(message);
				break;

			case EntityMessage.OPERATION_ACKNOWLEDGE:
				handleAcknowledgement(message);
				break;

			case EntityMessage.OPERATION_EVENT:
				handleEvent(message);
				break;

			default:
				break;
		}
	}


	private void setParameter(final EntityMessage message)
	{
		int acknowledge = EntityMessage.FUNCTION_OK;


		if (message.getData() == null)
		{
			return;
		}

		switch (message.getParameter())
		{
			case ParameterComm.PARAM_RF_STATE:
				mLog.Debug(getClass(), "Set RF state: " + message.getData()[0]);

				if (message.getData()[0] == ParameterComm.RF_STATE_CONNECTED)
				{
					mConnectionTimeout = INTERVAL_CONNECTION_TIMEOUT_LONG;
				}
				else
				{
					mConnectionTimeout = INTERVAL_CONNECTION_TIMEOUT_SHORT;
				}

				break;

			case ParameterComm.PARAM_RF_BROADCAST_SWITCH:
				mLog.Debug(getClass(), "Set broadcast switch: " +
					message.getData()[0]);

				if (message.getData()[0] == 0)
				{
					mBroadcastSwitch = false;
				}
				else
				{
					mBroadcastSwitch = true;
				}

				mService.getDataStorage(null).setBoolean(SETTING_BROADCAST_SWITCH,
					mBroadcastSwitch);
				break;

			default:
				acknowledge = EntityMessage.FUNCTION_FAIL;
				break;
		}

		reverseMessagePath(message);
		message.setOperation(EntityMessage.OPERATION_ACKNOWLEDGE);
		message.setData(new byte[]
		{
			(byte)acknowledge
		});
		handleMessage(message);
	}


	private void getParameter(final EntityMessage message)
	{
		int acknowledge = EntityMessage.FUNCTION_OK;
		byte[] value = null;


		switch (message.getParameter())
		{
			case ParameterComm.PARAM_RF_STATE:
				mLog.Debug(getClass(), "Get RF state: " + mRFState);

				value = new byte[1];
				value[0] = mRFState;
				break;

			case ParameterComm.PARAM_RF_SIGNAL:
				mLog.Debug(getClass(), "Get RF signal: " + mRFSignal);

				value = new byte[1];
				value[0] = mRFSignal;
				break;

			default:
				acknowledge = EntityMessage.FUNCTION_FAIL;
		}

		reverseMessagePath(message);

		if (acknowledge == EntityMessage.FUNCTION_OK)
		{
			message.setOperation(EntityMessage.OPERATION_NOTIFY);
			message.setData(value);
		}
		else
		{
			message.setOperation(EntityMessage.OPERATION_ACKNOWLEDGE);
			message.setData(new byte[]
			{
				(byte)acknowledge
			});
		}

		handleMessage(message);
	}


	private void handleNotification(final EntityMessage message)
	{
		if ((message
			.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) &&
			(message.getSourcePort() == ParameterGlobal.PORT_COMM))
		{
			switch (message.getParameter())
			{
				case ParameterComm.PARAM_RF_STATE:
					mLog.Debug(getClass(),
						"Notify RF state: " + message.getData()[0]);

					if (mRFState != message.getData()[0])
					{
						mRFState = message.getData()[0];
						EntityMessage messageRequest;

						if (mRFState != ParameterComm.RF_STATE_IDLE)
						{
							messageRequest =
								mMessageList[ParameterGlobal.ADDRESS_REMOTE_MASTER]
									.peek();

							if ((messageRequest != null) && (sDeviceBLE.query(
								ParameterGlobal.ADDRESS_REMOTE_MASTER) == EntityMessage.FUNCTION_OK))
							{
								sendRemoteMessage(messageRequest);
							}
						}
						else
						{
							messageRequest =
								mMessageList[ParameterGlobal.ADDRESS_REMOTE_MASTER]
									.poll();

							while (messageRequest != null)
							{
								sendFailEvent(messageRequest);
								messageRequest =
									mMessageList[ParameterGlobal.ADDRESS_REMOTE_MASTER]
										.poll();
							}

							onRFSignalChanged((byte)0);
							checkLink(ParameterGlobal.ADDRESS_REMOTE_MASTER);
						}
					}

					break;

				case ParameterComm.PARAM_BROADCAST_DATA:
					mLog.Debug(getClass(), "Notify broadcast data");
					mLog.Debug("广播包数据**", Arrays.toString(message.getData()));

					if (mRFState == ParameterComm.RF_STATE_IDLE)
					{
						mRFState = ParameterComm.COUNT_RF_STATE;
					}

					Broadcast broadcast = new Broadcast();
					broadcast.setByteArray(message.getData());

					if (broadcast.getRFSignal() > 0)
					{
						onRFSignalChanged((byte)broadcast.getRFSignal());

						if (mBroadcastSwitch)
						{
							handleMessage(new EntityMessage(
								ParameterGlobal.ADDRESS_LOCAL_CONTROL,
								ParameterGlobal.ADDRESS_LOCAL_CONTROL,
								ParameterGlobal.PORT_MONITOR,
								ParameterGlobal.PORT_MONITOR,
								EntityMessage.OPERATION_NOTIFY,
								ParameterMonitor.PARAM_HISTORY,
								broadcast.getData()));
							mLog.Debug("广播包数据**", "广播包数据发送");
						}
					}

					break;

				default:
					break;
			}
		}
	}


	private void handleAcknowledgement(final EntityMessage message)
	{
		if ((message
			.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE) &&
			(message.getSourcePort() == ParameterGlobal.PORT_COMM))
		{
			if (message.getData()[0] == EntityMessage.FUNCTION_OK)
			{
				mLog.Debug(getClass(), "Acknowledge OK");
			}
			else
			{
				mLog.Debug(getClass(), "Acknowledge Fail");
			}
		}
	}


	private void handleEvent(final EntityMessage message)
	{
		switch (message.getEvent())
		{
			case EntityMessage.EVENT_SEND_DONE:
				break;

			case EntityMessage.EVENT_ACKNOWLEDGE:
				break;

			case EntityMessage.EVENT_TIMEOUT:
				mLog.Debug(getClass(), "Command timeout");
				break;

			default:
				break;
		}
	}


	private EntityMessage updateMessageList(int address)
	{
		EntityMessage message;
		EntityMessage messageRequest;


		mLog.Debug(getClass(), "Update message list: " +
			mMessageList[address].size() + ", Address: " + address);

		messageRequest = mMessageList[address].poll();
		message = mMessageList[address].peek();

		if (message != null)
		{
			sendRemoteMessage(message);
			return messageRequest;
		}

		checkLink(address);

		return messageRequest;
	}


	private void sendRemoteMessage(final EntityMessage message)
	{
		if (message.getTargetAddress() == ParameterGlobal.ADDRESS_REMOTE_MASTER)
		{
			if (mRFState != ParameterComm.RF_STATE_CONNECTED)
			{
				if (mRFState == ParameterComm.RF_STATE_IDLE)
				{
					updateMessageList(message.getTargetAddress());
					sendFailEvent(message);
				}
				else if (mRFState == ParameterComm.RF_STATE_BROADCAST)
				{
					changeRFState(ParameterComm.RF_STATE_CONNECTED);
				}

				return;
			}
		}

		if (sDeviceBLE.send(message) != EntityMessage.FUNCTION_OK)
		{
			mLog.Debug(getClass(), "Send remote fail");
		}
	}


	private void receiveRemoteMessage(final EntityMessage message)
	{
		message.setTargetAddress(ParameterGlobal.ADDRESS_LOCAL_CONTROL);

		if (((message.getOperation() == EntityMessage.OPERATION_NOTIFY) ||
			(message.getOperation() == EntityMessage.OPERATION_ACKNOWLEDGE)) &&
			(message.getMode() == EntityMessage.MODE_ACKNOWLEDGE))
		{
			mMessageAcknowledge[message.getSourceAddress()] =
				new EntityMessage(message.getTargetAddress(),
					message.getSourceAddress(), message.getTargetPort(),
					message.getSourcePort(), EntityMessage.OPERATION_EVENT,
					message.getParameter(), new byte[]
					{
						EntityMessage.EVENT_ACKNOWLEDGE
					});
			checkLink(message.getSourceAddress());
		}

		EntityMessage messageRequest;

		if ((message.getOperation() == EntityMessage.OPERATION_EVENT) &&
			(message.getEvent() < EntityMessage.COUNT_EVENT))
		{
//			获取消息列表中的第一个
			messageRequest = mMessageList[message.getSourceAddress()].peek();

			if (messageRequest != null)
			{
				message.setTargetAddress(messageRequest.getSourceAddress());
				message.setParameter(messageRequest.getParameter());

				if (messageRequest.getMode() == EntityMessage.MODE_ACKNOWLEDGE)
				{
					if (message.getEvent() == EntityMessage.EVENT_ACKNOWLEDGE)
					{
						mMessageRequest[message.getSourceAddress()] =
							updateMessageList(message.getSourceAddress());
					}
					else if (message.getEvent() == EntityMessage.EVENT_TIMEOUT)
					{
						mMessageRequest[message.getSourceAddress()] = null;
						updateMessageList(message.getSourceAddress());

						if (message
							.getSourceAddress() == ParameterGlobal.ADDRESS_REMOTE_SLAVE)
						{
							sendPDAErrorAlert();
						}
					}
				}
				else
				{
					if (message.getEvent() == EntityMessage.EVENT_SEND_DONE)
					{
						mMessageRequest[message.getSourceAddress()] = null;
						updateMessageList(message.getSourceAddress());
					}
				}
			}
		}
		else
		{
			messageRequest = mMessageRequest[message.getSourceAddress()];

			if (messageRequest != null)
			{
				if ((message.getSourcePort() == messageRequest
					.getTargetPort()) &&
					(message.getTargetPort() == messageRequest
						.getSourcePort()) &&
					(message.getParameter() == messageRequest.getParameter()) &&
					((message
						.getOperation() == EntityMessage.OPERATION_NOTIFY) ||
						(message
							.getOperation() == EntityMessage.OPERATION_ACKNOWLEDGE)))
				{
					message.setTargetAddress(messageRequest.getSourceAddress());
				}

				mMessageRequest[message.getSourceAddress()] = null;
			}
		}

		handleMessage(message);
	}


	private void reverseMessagePath(final EntityMessage message)
	{
		message.setTargetAddress(message.getSourceAddress());
		message.setSourceAddress(ParameterGlobal.ADDRESS_LOCAL_CONTROL);
		message.setTargetPort(message.getSourcePort());
		message.setSourcePort(ParameterGlobal.PORT_COMM);
	}


	private void sendFailEvent(final EntityMessage message)
	{
		mLog.Debug(getClass(), "Send fail event");

		final EntityMessage messageEvent =
			new EntityMessage(message.getTargetAddress(),
				message.getSourceAddress(), message.getTargetPort(),
				message.getSourcePort(), EntityMessage.EVENT_SEND_DONE);
		messageEvent.setParameter(message.getParameter());

		handleMessage(messageEvent);

		if ((message.getOperation() == EntityMessage.OPERATION_SET) ||
			(message.getOperation() == EntityMessage.OPERATION_GET))
		{
			messageEvent.setEvent(EntityMessage.EVENT_TIMEOUT);
		}

		handleMessage(messageEvent);
	}


	private void changeRFState(byte state)
	{
		mLog.Debug(getClass(), "Change RF state: " + state);

		mRFState = ParameterComm.COUNT_RF_STATE;
		handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
			ParameterGlobal.ADDRESS_REMOTE_SLAVE, ParameterGlobal.PORT_COMM,
			ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_SET,
			ParameterComm.PARAM_RF_STATE, new byte[]
			{
				state
			}));
	}


	private void onRFSignalChanged(byte signal)
	{
		mRFSignal = signal;
		handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
			ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_COMM,
			ParameterGlobal.PORT_COMM, EntityMessage.OPERATION_NOTIFY,
			ParameterComm.PARAM_RF_SIGNAL, new byte[]
			{
				signal
			}));
	}


	private void sendPDAErrorAlert()
	{
		final Event event = new Event(0, ParameterGlobal.PORT_MONITOR,
			ParameterMonitor.EVENT_PDA_ERROR, Event.URGENCY_ALERT, 0);
		final History history = new History(
			new DateTime(Calendar.getInstance()), new Status(), event);

		handleMessage(new EntityMessage(ParameterGlobal.ADDRESS_LOCAL_CONTROL,
			ParameterGlobal.ADDRESS_LOCAL_VIEW, ParameterGlobal.PORT_MONITOR,
			ParameterGlobal.PORT_MONITOR, EntityMessage.OPERATION_NOTIFY,
			ParameterMonitor.PARAM_HISTORY, history.getByteArray()));
	}


	private void checkLink(final int address)
	{
		mLog.Debug(getClass(), "Check link: " + address);

		if (mRunnableLink[address] == null)
		{
			mRunnableLink[address] = new Runnable()
			{
				@Override
				public void run()
				{
					if ((mMessageList[address].size() == 0) && (sDeviceBLE
						.query(address) == EntityMessage.FUNCTION_OK))
					{
						if (mMessageAcknowledge[address] == null)
						{
							mLog.Debug(TaskComm.class, "Link idle: " + address);

							if (address == ParameterGlobal.ADDRESS_REMOTE_MASTER)
							{
								mConnectionTimer += INTERVAL_LINK_CHECK;

								if (mConnectionTimer >= mConnectionTimeout)
								{
									mConnectionTimer = 0;

									if (mRFState == ParameterComm.RF_STATE_CONNECTED)
									{
										changeRFState(
											ParameterComm.RF_STATE_BROADCAST);
									}

									sDeviceBLE.switchLink(
										ParameterGlobal.ADDRESS_REMOTE_MASTER,
										0);
									sDeviceBLE.switchLink(
										ParameterGlobal.ADDRESS_REMOTE_MASTER,
										1);
								}
								else
								{
									mHandlerLink[address].postDelayed(this,
										INTERVAL_LINK_CHECK);
								}
							}

							return;
						}
						else
						{
							handleMessage(mMessageAcknowledge[address]);
							mMessageAcknowledge[address] = null;
						}
					}

					mLog.Debug(TaskComm.class, "Link busy: " + address);

					if (address == ParameterGlobal.ADDRESS_REMOTE_MASTER)
					{
						mConnectionTimer = 0;
					}

					mHandlerLink[address].postDelayed(this,
						INTERVAL_LINK_CHECK);
				}
			};
		}

		if (address == ParameterGlobal.ADDRESS_REMOTE_MASTER)
		{
			mConnectionTimer = 0;
		}

		mHandlerLink[address].removeCallbacks(mRunnableLink[address]);
		mHandlerLink[address].postDelayed(mRunnableLink[address],
			INTERVAL_LINK_CHECK);
	}
}
