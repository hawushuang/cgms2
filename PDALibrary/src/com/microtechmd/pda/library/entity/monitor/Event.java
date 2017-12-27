package com.microtechmd.pda.library.entity.monitor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.microtechmd.pda.library.entity.DataBundle;

public class Event extends DataBundle
{
	public static final int BYTE_ARRAY_LENGTH = 6;
	
	public static final int EVENT_INDEX_MAX = 2000;
	public static final int EVENT_INDEX_MIN = 1;
	public static final int OBSOLETE_EVENT_MASK = 0x8000;
	
	public static final int URGENCY_NOTIFICATION = 0;
	public static final int URGENCY_ALERT = 1;
	public static final int URGENCY_ALARM = 2;
	public static final int COUNT_URGENCY = 3;
	
	private static final String IDENTIFIER = "event";
	private static final String KEY_INDEX = IDENTIFIER + "_index";
	private static final String KEY_PORT = IDENTIFIER + "_port";
	private static final String KEY_EVENT = IDENTIFIER + "_event";
	private static final String KEY_URGENCY = IDENTIFIER + "_urgency";
	private static final String KEY_VALUE = IDENTIFIER + "_value";
	
	
	public Event()
	{
		super();
	}
	
	public Event(byte[] byteArray) 
	{
		super(byteArray);
	}
	
	public Event(int index, int port, int event, int urgency, int value)
	{
		super();
		setIndex(index);
		setPort(port);
		setEvent(event);
		setUrgency(urgency);
		setValue(value);
	}
	
	public int getIndex()
	{
		return (int)getShort(KEY_INDEX);
	}
	
	public int getPort()
	{
		return (int)getByte(KEY_PORT);
	}
	
	public int getEvent()
	{
		return (int)getByte(KEY_EVENT);
	}
	
	public int getUrgency()
	{
		return (int)getByte(KEY_URGENCY);
	}
	
	public int getValue()
	{
		return (int)getByte(KEY_VALUE);
	}
	
	public void setIndex(int index)
	{
		setShort(KEY_INDEX, (short)index);
	}
	
	public void setPort(int port)
	{
		setByte(KEY_PORT, (byte)port);
	}

	public void setEvent(int event)
	{
		setByte(KEY_EVENT, (byte)event);
	}

	public void setUrgency(int urgency)
	{
		setByte(KEY_URGENCY, (byte)urgency);
	}

	public void setValue(int value)
	{
		setByte(KEY_VALUE, (byte)value);
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
			dataOutputStream.writeShortLittleEndian((short)getIndex());
			dataOutputStream.writeByte((byte)getPort());
			dataOutputStream.writeByte((byte)getEvent());
			dataOutputStream.writeByte((byte)getUrgency());
			dataOutputStream.writeByte((byte)getValue());
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
		
		if (byteArray.length >= BYTE_ARRAY_LENGTH)
		{
			final DataInputStreamLittleEndian dataInputStream;
			final ByteArrayInputStream byteArrayInputStream;
			
			byteArrayInputStream = new ByteArrayInputStream(byteArray);
			dataInputStream = new DataInputStreamLittleEndian(byteArrayInputStream);
			
			try 
			{
				clearBundle();
				setIndex((int)dataInputStream.readShortLittleEndian());
				setPort((int)dataInputStream.readByte());
				setEvent((int)dataInputStream.readByte());
				setUrgency((int)dataInputStream.readByte());
				setValue((int)dataInputStream.readByte());
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
