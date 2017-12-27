package com.microtechmd.pda.library.entity.monitor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.microtechmd.pda.library.entity.DataBundle;

public class History extends DataBundle 
{
	public static final int BYTE_ARRAY_LENGTH = 18;
	
	private static final String IDENTIFIER = "log";
	private static final String KEY_DATETIME = IDENTIFIER + "_datetime";
	private static final String KEY_STATUS = IDENTIFIER + "_status";
	private static final String KEY_EVENT = IDENTIFIER + "_event";
	
	
	public History()
	{
		super();
	}
	
	public History(byte[] byteArray) 
	{
		super(byteArray);
	}
	
	public History(final DateTime dateTime, final Status status, final Event event)
	{
		super();
		setDateTime(dateTime);
		setStatus(status);
		setEvent(event);
	}
	
	public DateTime getDateTime()
	{
		return new DateTime(getExtras(KEY_DATETIME));
	}
	
	public Status getStatus()
	{
		return new Status(getExtras(KEY_STATUS));
	}
	
	public Event getEvent()
	{
		return new Event(getExtras(KEY_EVENT));
	}
	
	public void setDateTime(final DateTime dateTime)
	{
		setExtras(KEY_DATETIME, dateTime.getByteArray());
	}
	
	public void setStatus(final Status status)
	{
		setExtras(KEY_STATUS, status.getByteArray());
	}
	
	public void setEvent(final Event event)
	{
		setExtras(KEY_EVENT, event.getByteArray());
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
			byte[] dateTime = getExtras(KEY_DATETIME);
			
			if (dateTime == null)
			{
				dateTime = new byte[DateTime.BYTE_ARRAY_LENGTH];
			}
			
			byte[] status = getExtras(KEY_STATUS);
			
			if (status == null)
			{
				status = new byte[Status.BYTE_ARRAY_LENGTH];
			}
			
			byte[] event = getExtras(KEY_EVENT);
			
			if (event == null)
			{
				event = new byte[Event.BYTE_ARRAY_LENGTH];
			}
			
			dataOutputStream.write(dateTime);
			dataOutputStream.write(status);
			dataOutputStream.write(event);
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
				final byte[] dateTime = new byte[DateTime.BYTE_ARRAY_LENGTH];
				dataInputStream.read(dateTime, 0, DateTime.BYTE_ARRAY_LENGTH);
				setExtras(KEY_DATETIME, dateTime);
				final byte[] status = new byte[Status.BYTE_ARRAY_LENGTH];
				dataInputStream.read(status, 0, Status.BYTE_ARRAY_LENGTH);
				setExtras(KEY_STATUS, status);
				final byte[] event = new byte[Event.BYTE_ARRAY_LENGTH];
				dataInputStream.read(event, 0, Event.BYTE_ARRAY_LENGTH);
				setExtras(KEY_EVENT, event);
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
