package com.microtechmd.pda.model.task;


import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.DataList;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.ParameterMonitor;
import com.microtechmd.pda.library.entity.comm.RFAddress;
import com.microtechmd.pda.library.entity.monitor.History;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.service.ServiceBase;
import com.microtechmd.pda.library.service.TaskBase;
import com.microtechmd.pda.model.database.DataSetHistory;


public final class TaskMonitor extends TaskBase
{
	// Constant and variable definition

	private static TaskMonitor sInstance = null;

	private DataSetHistory mDataSetHistory = null;


	// Method definition

	private TaskMonitor(ServiceBase service)
	{
		super(service);

		if (mDataSetHistory == null)
		{
			mDataSetHistory = new DataSetHistory(service);

			byte[] addressSetting =
				mService.getDataStorage(null)
					.getExtras(TaskComm.SETTING_RF_ADDRESS, null);

			if (addressSetting != null)
			{
				RFAddress address = new RFAddress(addressSetting);
				mDataSetHistory.setRFAddress(address.getAddress());
			}
		}
	}


	public static synchronized TaskMonitor getInstance(
		final ServiceBase service)
	{
		if (sInstance == null)
		{
			sInstance = new TaskMonitor(service);
		}

		return sInstance;
	}


	@Override
	public void handleMessage(EntityMessage message)
	{
		if ((message
			.getTargetAddress() == ParameterGlobal.ADDRESS_LOCAL_MODEL) &&
			(message.getTargetPort() == ParameterGlobal.PORT_MONITOR))
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
					break;

				case EntityMessage.OPERATION_NOTIFY:
					handleNotification(message);
					break;

				case EntityMessage.OPERATION_ACKNOWLEDGE:
					break;

				default:
					break;
			}
		}
		else
		{
			mService.onReceive(message);
		}
	}


	private void setParameter(EntityMessage message)
	{
		int acknowledge = EntityMessage.FUNCTION_OK;


		if (message.getData() == null)
		{
			return;
		}

		switch (message.getParameter())
		{
			case ParameterMonitor.PARAM_HISTORY:
				DataList historyList = new DataList(message.getData());
				mDataSetHistory.exportHistory(historyList);
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


	private void getParameter(EntityMessage message)
	{
		int acknowledge = EntityMessage.FUNCTION_OK;
		byte[] value = null;

		switch (message.getParameter())
		{
			case ParameterMonitor.PARAM_HISTORY:
				DataList historyList = new DataList(message.getData());
				historyList = mDataSetHistory.queryHistory(historyList);
				value = historyList.getByteArray();
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


	private void handleNotification(EntityMessage message)
	{
		if ((message.getSourcePort() == ParameterGlobal.PORT_MONITOR) &&
			(message.getParameter() == ParameterMonitor.PARAM_HISTORY))
		{
			if (message.getData() != null)
			{
				mDataSetHistory.insertHistory(new History(message.getData()));
			}
		}

		if ((message.getSourcePort() == ParameterGlobal.PORT_COMM) &&
			(message.getParameter() == ParameterComm.PARAM_RF_REMOTE_ADDRESS))
		{
			if (message.getData() != null)
			{
				RFAddress address = new RFAddress(message.getData());
				mDataSetHistory.setRFAddress(address.getAddress());
			}
		}
	}


	private void reverseMessagePath(EntityMessage message)
	{
		message.setTargetAddress(message.getSourceAddress());
		message.setSourceAddress(ParameterGlobal.ADDRESS_LOCAL_MODEL);
		message.setTargetPort(message.getSourcePort());
		message.setSourcePort(ParameterGlobal.PORT_MONITOR);
	}
}
