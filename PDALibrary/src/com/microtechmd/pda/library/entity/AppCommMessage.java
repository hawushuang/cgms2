package com.microtechmd.pda.library.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AppCommMessage extends DataBundle
{
	private static final int MESSAGE_LENGTH = 6;
	private static final String MESSAGE_IDENTIFIER = "appcomm";
	
	public static final byte TYPE_INVALID = 0;
	public static final byte TYPE_ERROR = 1;
	public static final byte TYPE_ONGOING = 2;
	public static final byte TYPE_BREAK = 3;
	public static final byte TYPE_REQUEST = 4;
	public static final byte TYPE_FINISH = 5;
	public static final byte TYPE_LOOPBACK = 6;
	public static final byte COUNT_TYPE = 7;
	
	public static final byte TOKENCOUNT_INVALID = 0;
	
	public static final byte COMMAND_INVALID = 0;
	public static final byte COMMAND_CONFIG = 1;
	public static final byte COMMAND_TEST = 2;
	public static final byte COMMAND_DEBUG = 3;
	public static final byte COMMAND_HELP = 4;
	public static final byte COMMAND_RTEST = 5;
	public static final byte COMMAND_NOTIFY = 6;
	public static final byte COUNT_COMMAND = 7;
	
	public static final byte TARGET_INVALID = 0;
	public static final byte TARGET_SYSTEM = 1;
	public static final byte TARGET_RF = 2;
	public static final byte TARGET_MOTOR = 3;
	public static final byte TARGET_ALARM = 4;
	public static final byte TARGET_LED = 5;
	public static final byte TARGET_MEMORY = 6;
	public static final byte TARGET_POWER = 7;
	public static final byte TARGET_WATCHDOG = 8;
	public static final byte TARGET_SENSOR = 9;
	public static final byte TARGET_BGMETER = 10;
	public static final byte TARGET_FS = 11;
	public static final byte TARGET_DELIVERY = 12;
	public static final byte COUNT_TARGET = 13;
	
	public static final byte OPERATION_INVALID = 0;
	public static final byte OPERATION_SEND = 1;
	public static final byte OPERATION_RECEIVE = 2;
	public static final byte OPERATION_WRITE = 3;
	public static final byte OPERATION_READ = 4;
	public static final byte OPERATION_OPEN = 5;
	public static final byte OPERATION_CLOSE = 6;
	public static final byte OPERATION_REMOVE = 7;
	public static final byte OPERATION_ERASE = 8;
	public static final byte OPERATION_SET = 9;
	public static final byte OPERATION_GET = 10;
	public static final byte OPERATION_START = 11;
	public static final byte OPERATION_STOP = 12;
	public static final byte OPERATION_MONITOR = 13;
	public static final byte OPERATION_LOOPBACK = 14;
	public static final byte OPERATION_REDIRECT = 15;
	public static final byte OPERATION_TICK = 16;
	public static final byte COUNT_OPERATION = 17;
	
	public static final byte PARAMETER_INVALID = 0;
	public static final byte PARAM_CYCLE = 1;
	public static final byte PARAM_COUNTER = 2;
	public static final byte PARAM_SPEED = 3;
	public static final byte PARAM_STEP = 4;
	public static final byte PARAM_DIRECT = 5;
	public static final byte PARAM_VOLTAGE = 6;
	public static final byte PARAM_CURRENT = 7;
	public static final byte PARAM_CAPACITY = 8;
	public static final byte PARAM_REFERENCE = 9;
	public static final byte PARAM_BUTTON = 10;
	public static final byte PARAM_SWITCH = 11;
	public static final byte PARAM_OCCLUSION = 12;
	public static final byte PARAM_TONE = 13;
	public static final byte PARAM_VOLUME = 14;
	public static final byte PARAM_STATE = 15;
	public static final byte PARAM_MODE = 16;
	public static final byte PARAM_REMOTE = 17;
	public static final byte PARAM_LOCAL = 18;
	public static final byte PARAM_HCTLIMIT = 19;
	public static final byte PARAM_BGLIMIT = 20;
	public static final byte PARAM_SIZE = 21;
	public static final byte PARAM_OFFSET = 22;
	public static final byte PARAM_AMOUNT = 23;
	public static final byte PARAM_UNIT = 24;
	public static final byte PARAM_BASAL = 25;
	public static final byte PARAM_BOLUS = 26;
	public static final byte PARAM_TEMP = 27;
	public static final byte PARAM_DATE = 28;
	public static final byte PARAM_TIME = 29;
	public static final byte COUNT_PARAM = 30;
	
	public static final byte LENGTH_INVALID = 0;
	
	public static final String[] STRING_COMMAND = new String[] 
	{
		"", 
		"config",
		"test",
		"debug",
		"help",
		"rtest",
		"notify"
	};
	
	public static final String[] STRING_TARGET = new String[] 
	{
		"", 
		"system",
		"rf",
		"motor",
		"alarm",
		"led",
		"memory",
		"power",
		"watchdog",
		"sensor",
		"bgmeter",
		"fs",
		"delivery"
	};
	
	public static final String[] STRING_OPERATION = new String[] 
	{
		"",
		"send",
		"receive",
		"write",
		"read",
		"open",
		"close",
		"remove",
		"erase",
		"set",
		"get",
		"start",
		"stop",
		"monitor",
		"loopback",
		"redirect",
		"tick"
	};
	
	public static final String[] STRING_PARAM = new String[] 
	{
		"",
		"cycle",
		"counter",
		"speed",
		"step",
		"direct",
		"voltage",
		"current",
		"capacity",
		"reference",
		"button",
		"switch",
		"occlusion",
		"tone",
		"volume",
		"state",
		"mode",
		"remote",
		"local",
		"hctlimit",
		"bglimit",
		"size",
		"offset",
		"amount",
		"unit",
		"basal",
		"bolus",
		"temp",
		"date",
		"time"
	};
	
	private static final String APPCOMM_KEY_TYPE = MESSAGE_IDENTIFIER + "_type";
	private static final String APPCOMM_KEY_COMMAND = MESSAGE_IDENTIFIER + "_command";
	private static final String APPCOMM_KEY_TARGET = MESSAGE_IDENTIFIER + "_target";
	private static final String APPCOMM_KEY_OPERATION = MESSAGE_IDENTIFIER + "_operation";
	private static final String APPCOMM_KEY_PARAMETER = MESSAGE_IDENTIFIER + "_parameter";
	private static final String APPCOMM_KEY_DATA = MESSAGE_IDENTIFIER + "_data";
	
	private static int mDataCount;
		
	public interface MessageListener 
	{
		void onReceiveAppCommMessage(AppCommMessage message);
	}

	public class AppCommMessageData extends DataBundle
	{
		private static final String IDENTIFIER = "appcomm_data";
		private static final int BYTE_ARRAY_LENGTH = 4;
		private static final String DATA_KEY_VALUE = IDENTIFIER + "_value";
		
		
		public int getValue()
		{
			return getInt(DATA_KEY_VALUE);
		}
		
		public void setValue(int value)
		{
			setInt(DATA_KEY_VALUE, value);
		}

		@Override
		public byte[] getByteArray() 
		{
			final DataOutputStreamLittleEndian dataOutputStream;
			final ByteArrayOutputStream byteArrayOutputStream;
			
			byteArrayOutputStream = new ByteArrayOutputStream();
			dataOutputStream = new DataOutputStreamLittleEndian(byteArrayOutputStream);
			
			try 
			{
				byteArrayOutputStream.reset();	
				dataOutputStream.writeIntLittleEndian(getInt(DATA_KEY_VALUE));
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
			if (byteArray.length >= BYTE_ARRAY_LENGTH)
			{
				final DataInputStreamLittleEndian dataInputStream;
				final ByteArrayInputStream byteArrayInputStream;
				
				byteArrayInputStream = new ByteArrayInputStream(byteArray);
				dataInputStream = new DataInputStreamLittleEndian(byteArrayInputStream);
				
				try 
				{
					clearBundle();
					setInt(DATA_KEY_VALUE, dataInputStream.readIntLittleEndian());
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public AppCommMessage()
	{
		super();
		mDataCount = 0;
	}
	
	public AppCommMessage(byte[] byteArray) 
	{
		super(byteArray);
	}

	public AppCommMessage(byte type, byte command, byte target, byte operation,
		byte parameter, byte[] data)
	{
		super();
		mDataCount = 0;
		
		setType(type);
		setCommand(command);
		setTarget(target);
		setOperation(operation);
		setParameter(parameter);
		setData(data);
	}
	
	public byte getType()
	{
		return getByte(APPCOMM_KEY_TYPE);
	}
	
	public byte getCommand()
	{
		return getByte(APPCOMM_KEY_COMMAND);
	}
	
	public byte getTarget()
	{
		return getByte(APPCOMM_KEY_TARGET);
	}
	
	public byte getOperation()
	{
		return getByte(APPCOMM_KEY_OPERATION);
	}
	
	public byte getParameter()
	{
		return getByte(APPCOMM_KEY_PARAMETER);
	}
	
	public byte[] getData(int index)
	{
		return getExtras(APPCOMM_KEY_DATA + index);
	}
	
	public int getDataCount()
	{
		return mDataCount;
	}
	
	public void setType(byte type)
	{
		setByte(APPCOMM_KEY_TYPE, type);
	}
	
	public void setCommand(byte command)
	{
		setByte(APPCOMM_KEY_COMMAND, command);
	}
	
	public void setTarget(byte target)
	{
		setByte(APPCOMM_KEY_TARGET, target);
	}
	
	public void setOperation(byte operation)
	{
		setByte(APPCOMM_KEY_OPERATION, operation);
	}
	
	public void setParameter(byte parameter)
	{
		setByte(APPCOMM_KEY_PARAMETER, parameter);
	}
	
	public void setData(byte[] data)
	{
		setExtras(APPCOMM_KEY_DATA + mDataCount, data);
		mDataCount++;
	}
	
	@Override
	public byte[] getByteArray()
	{
		final DataOutputStreamLittleEndian dataOutputStream;
		final ByteArrayOutputStream byteArrayOutputStream;
		
		byteArrayOutputStream = new ByteArrayOutputStream();
		dataOutputStream = new DataOutputStreamLittleEndian(byteArrayOutputStream);
		
		try 
		{
			byteArrayOutputStream.reset();	
			dataOutputStream.writeByte(getByte(APPCOMM_KEY_TYPE));
			dataOutputStream.writeByte(TOKENCOUNT_INVALID);
			dataOutputStream.writeByte(getByte(APPCOMM_KEY_COMMAND));
			dataOutputStream.writeByte(getByte(APPCOMM_KEY_TARGET));
			dataOutputStream.writeByte(getByte(APPCOMM_KEY_OPERATION));
			dataOutputStream.writeByte(getByte(APPCOMM_KEY_PARAMETER));
			
			for (int index = 0; index < mDataCount; index++)
			{
				byte[] data = getExtras(APPCOMM_KEY_DATA + index);
				
				if (data != null)
				{
					dataOutputStream.writeShortLittleEndian((short)data.length);
					dataOutputStream.write(data);
				}
			}
			
			if (mDataCount == 0)
			{
				dataOutputStream.writeShortLittleEndian((short)0);
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
		if (byteArray.length >= MESSAGE_LENGTH)
		{
			final DataInputStreamLittleEndian dataInputStream;
			final ByteArrayInputStream byteArrayInputStream;
			
			byteArrayInputStream = new ByteArrayInputStream(byteArray);
			dataInputStream = new DataInputStreamLittleEndian(byteArrayInputStream);
			
			try 
			{
				clearBundle();
				setByte(APPCOMM_KEY_TYPE, dataInputStream.readByte());
				dataInputStream.readByte();
				setByte(APPCOMM_KEY_COMMAND, dataInputStream.readByte());
				setByte(APPCOMM_KEY_TARGET, dataInputStream.readByte());
				setByte(APPCOMM_KEY_OPERATION, dataInputStream.readByte());
				setByte(APPCOMM_KEY_PARAMETER, dataInputStream.readByte());
				
				int totalLength = byteArray.length - MESSAGE_LENGTH; 
				int index = 0;
				
				while (totalLength > 0)
				{
					short dataLength = 0;
					
					if (totalLength > 2)
					{
						dataLength = dataInputStream.readShortLittleEndian();
						totalLength -= 2;
					}
					
					if ((dataLength > 0) && (totalLength >= (int)dataLength))
					{
						final byte[] data = new byte[dataLength];
						dataInputStream.read(data, 0, dataLength);
						setExtras(APPCOMM_KEY_DATA + index, data);
						index++;
						totalLength -= (int)dataLength;
					}
					else
					{
						break;
					}
				}
				
				mDataCount = index;
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
