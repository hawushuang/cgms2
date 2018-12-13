package com.microtechmd.pda.library.entity;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class EntityMessage extends DataBundle
{
	// Constant definition

	public static final int FUNCTION_FAIL = 0;
	public static final int FUNCTION_OK = 1;

	public static final int ADDRESS_REMOTE_MASTER = 0;
	public static final int ADDRESS_REMOTE_SLAVE = 1;
	public static final int ADDRESS_LOCAL_VIEW = 2;
	public static final int ADDRESS_LOCAL_CONTROL = 3;
	public static final int ADDRESS_LOCAL_MODEL = 4;
	public static final int COUNT_ADDRESS = 5;

	public static final int PORT_SYSTEM = 0;
	public static final int PORT_COMM = 1;
	public static final int PORT_SHELL = 2;
	public static final int PORT_GLUCOSE = 3;
	public static final int PORT_DELIVERY = 4;
	public static final int PORT_MONITOR = 5;
	public static final int COUNT_PORT = 6;

	public static final int MODE_ACKNOWLEDGE = 0;
	public static final int MODE_NO_ACKNOWLEDGE = 1;
	public static final int COUNT_MODE = 2;

	public static final int OPERATION_EVENT = 0;
	public static final int OPERATION_SET = 1;
	public static final int OPERATION_GET = 2;
	public static final int OPERATION_WRITE = 3;
	public static final int OPERATION_READ = 4;
	public static final int OPERATION_NOTIFY = 5;
	public static final int OPERATION_ACKNOWLEDGE = 6;
	public static final int OPERATION_PAIR = 7;
	public static final int OPERATION_UNPAIR = 8;
	public static final int OPERATION_BOND = 9;
	public static final int COUNT_OPERATION = 7;

	public static final int EVENT_SEND_DONE = 0;
	public static final int EVENT_ACKNOWLEDGE = 1;
	public static final int EVENT_TIMEOUT = 2;
	public static final int EVENT_RECEIVE_DONE = 3;
	public static final int COUNT_EVENT = 4;

	public static final int PARAMETER_INVALID = -1;

	private static final int MESSAGE_LENGTH = 32;
	private static final String MESSAGE_IDENTIFIER = "message";
	private static final String KEY_SOURCE_ADDRESS =
		MESSAGE_IDENTIFIER + "_source_address";
	private static final String KEY_TARGET_ADDRESS =
		MESSAGE_IDENTIFIER + "_target_address";
	private static final String KEY_SOURCE_PORT =
		MESSAGE_IDENTIFIER + "_source_port";
	private static final String KEY_TARGET_PORT =
		MESSAGE_IDENTIFIER + "_target_port";
	private static final String KEY_MODE = MESSAGE_IDENTIFIER + "_mode";
	private static final String KEY_OPERATION =
		MESSAGE_IDENTIFIER + "_operation";
	private static final String KEY_EVENT = MESSAGE_IDENTIFIER + "_event";
	private static final String KEY_PARAMETER =
		MESSAGE_IDENTIFIER + "_parameter";
	private static final String KEY_DATA = MESSAGE_IDENTIFIER + "_data";


	// Inner class definition

	public interface Listener
	{
		void onReceive(EntityMessage message);
	}


	// Method definition

	public EntityMessage()
	{
		super();
	}


	public EntityMessage(byte[] byteArray)
	{
		super(byteArray);
	}


	public EntityMessage(int sourceAddress, int targetAddress, int sourcePort,
		int targetPort, int event)
	{
		this(sourceAddress, targetAddress, sourcePort, targetPort,
			MODE_NO_ACKNOWLEDGE, OPERATION_EVENT, PARAMETER_INVALID, null);
		setEvent(event);
	}


	public EntityMessage(int sourceAddress, int targetAddress, int sourcePort,
		int targetPort, int operation, int parameter, byte[] data)
	{
		this(sourceAddress, targetAddress, sourcePort, targetPort,
			MODE_ACKNOWLEDGE, operation, parameter, data);
	}


	public EntityMessage(int sourceAddress, int targetAddress, int sourcePort,
		int targetPort, int mode, int operation, int parameter, byte[] data)
	{
		super();
		setSourceAddress(sourceAddress);
		setTargetAddress(targetAddress);
		setSourcePort(sourcePort);
		setTargetPort(targetPort);
		setMode(mode);
		setOperation(operation);
		setParameter(parameter);
		setData(data);
		setEvent(COUNT_EVENT);
	}


	public int getSourceAddress()
	{
		return getInt(KEY_SOURCE_ADDRESS);
	}


	public int getTargetAddress()
	{
		return getInt(KEY_TARGET_ADDRESS);
	}


	public int getSourcePort()
	{
		return getInt(KEY_SOURCE_PORT);
	}


	public int getTargetPort()
	{
		return getInt(KEY_TARGET_PORT);
	}


	public int getMode()
	{
		return getInt(KEY_MODE);
	}


	public int getOperation()
	{
		return getInt(KEY_OPERATION);
	}


	public int getEvent()
	{
		return getInt(KEY_EVENT);
	}


	public int getParameter()
	{
		return getInt(KEY_PARAMETER);
	}


	public byte[] getData()
	{
		return getExtras(KEY_DATA);
	}


	public void setSourceAddress(int address)
	{
		setInt(KEY_SOURCE_ADDRESS, address);
	}


	public void setTargetAddress(int address)
	{
		setInt(KEY_TARGET_ADDRESS, address);
	}


	public void setSourcePort(int sourcePort)
	{
		setInt(KEY_SOURCE_PORT, sourcePort);
	}


	public void setTargetPort(int targetPort)
	{
		setInt(KEY_TARGET_PORT, targetPort);
	}


	public void setMode(int mode)
	{
		setInt(KEY_MODE, mode);
	}


	public void setOperation(int operation)
	{
		setInt(KEY_OPERATION, operation);
	}


	public void setEvent(int event)
	{
		setInt(KEY_EVENT, event);
	}


	public void setParameter(int parameter)
	{
		setInt(KEY_PARAMETER, parameter);
	}


	public void setData(byte[] data)
	{
		setExtras(KEY_DATA, data);
	}


	public byte[] toByteArray()
	{
		return getByteArray();
	}


	public void fromByteArray(byte[] byteArray)
	{
		setByteArray(byteArray);
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
			dataOutputStream.writeIntLittleEndian(getInt(KEY_SOURCE_ADDRESS));
			dataOutputStream.writeIntLittleEndian(getInt(KEY_TARGET_ADDRESS));
			dataOutputStream.writeIntLittleEndian(getInt(KEY_SOURCE_PORT));
			dataOutputStream.writeIntLittleEndian(getInt(KEY_TARGET_PORT));
			dataOutputStream.writeIntLittleEndian(getInt(KEY_MODE));
			dataOutputStream.writeIntLittleEndian(getInt(KEY_OPERATION));
			dataOutputStream.writeIntLittleEndian(getInt(KEY_EVENT));
			dataOutputStream.writeIntLittleEndian(getInt(KEY_PARAMETER));

			final byte[] data = getExtras(KEY_DATA);

			if (data != null)
			{
				dataOutputStream.write(data);
			}
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

		if (byteArray.length >= MESSAGE_LENGTH)
		{
			final DataInputStreamLittleEndian dataInputStream;
			final ByteArrayInputStream byteArrayInputStream;

			byteArrayInputStream = new ByteArrayInputStream(byteArray);
			dataInputStream =
				new DataInputStreamLittleEndian(byteArrayInputStream);

			try
			{
				clearBundle();
				setInt(KEY_SOURCE_ADDRESS,
					dataInputStream.readIntLittleEndian());
				setInt(KEY_TARGET_ADDRESS,
					dataInputStream.readIntLittleEndian());
				setInt(KEY_SOURCE_PORT, dataInputStream.readIntLittleEndian());
				setInt(KEY_TARGET_PORT, dataInputStream.readIntLittleEndian());
				setInt(KEY_MODE, dataInputStream.readIntLittleEndian());
				setInt(KEY_OPERATION, dataInputStream.readIntLittleEndian());
				setInt(KEY_EVENT, dataInputStream.readIntLittleEndian());
				setInt(KEY_PARAMETER, dataInputStream.readIntLittleEndian());

				int dataLength = byteArray.length - MESSAGE_LENGTH;

				if (dataLength > 0)
				{
					final byte[] data = new byte[dataLength];
					dataInputStream.read(data, 0, dataLength);
					setExtras(KEY_DATA, data);
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
