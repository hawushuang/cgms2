package com.microtechmd.pda.model.task;


import com.microtechmd.pda.library.entity.EntityMessage;
import com.microtechmd.pda.library.entity.ParameterComm;
import com.microtechmd.pda.library.entity.comm.RFAddress;
import com.microtechmd.pda.library.parameter.ParameterGlobal;
import com.microtechmd.pda.library.service.ServiceBase;
import com.microtechmd.pda.library.service.TaskBase;


public class TaskComm extends TaskBase
{
	// Constant and variable definition

	public static final String SETTING_RF_ADDRESS = "rf_address";

	private static TaskComm sInstance = null;


	private TaskComm(ServiceBase service)
	{
		super(service);
	}


	public static synchronized TaskComm getInstance(final ServiceBase service)
	{
		if (sInstance == null)
		{
			sInstance = new TaskComm(service);
		}

		return sInstance;
	}


	@Override
	public void handleMessage(EntityMessage message)
	{
		if ((message
			.getTargetAddress() == ParameterGlobal.ADDRESS_LOCAL_MODEL) &&
			(message.getTargetPort() == ParameterGlobal.PORT_COMM))
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
			case ParameterComm.PARAM_RF_REMOTE_ADDRESS:

				if (message.getData() != null)
				{
					RFAddress address = new RFAddress(message.getData());
					mService.getDataStorage(null).setExtras(SETTING_RF_ADDRESS,
						address.getByteArray());
					handleMessage(new EntityMessage(
						ParameterGlobal.ADDRESS_LOCAL_MODEL,
						ParameterGlobal.ADDRESS_LOCAL_MODEL,
						ParameterGlobal.PORT_COMM, ParameterGlobal.PORT_MONITOR,
						EntityMessage.OPERATION_NOTIFY,
						ParameterComm.PARAM_RF_REMOTE_ADDRESS,
						message.getData()));
				}

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
			case ParameterComm.PARAM_RF_REMOTE_ADDRESS:
				value = mService.getDataStorage(null)
					.getExtras(SETTING_RF_ADDRESS, null);
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


	private void reverseMessagePath(EntityMessage message)
	{
		message.setTargetAddress(message.getSourceAddress());
		message.setSourceAddress(ParameterGlobal.ADDRESS_LOCAL_MODEL);
		message.setTargetPort(message.getSourcePort());
		message.setSourcePort(ParameterGlobal.PORT_COMM);
	}
}
