package com.microtechmd.pda.library.entity.monitor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.microtechmd.pda.library.entity.DataBundle;


public class StatusPump extends DataBundle 
{
	public static final int BYTE_ARRAY_LENGTH = 12;
	
	private DateTime mDateTime = null;
	private Status mStatus = null;
	
	
	public StatusPump()
	{
		super();
		mDateTime = new DateTime();
		mStatus = new Status();
	}
	
	public StatusPump(byte[] byteArray) 
	{
		super();
		mDateTime = new DateTime();
		mStatus = new Status();
		this.setByteArray(byteArray);
	}
	
	public StatusPump(int batteryCapacity, int reservoirAmount, int basalRate,
		int bolusRate)
	{
		super();
		mDateTime = new DateTime();
		mStatus = new Status(batteryCapacity, reservoirAmount, basalRate,
			bolusRate);
	}
	
	public StatusPump(DateTime dateTime, Status status)
	{
		super();
		mDateTime = new DateTime(dateTime.getByteArray());
		mStatus = new Status(status.getByteArray());
	}
	
	public DateTime getDateTime()
	{
		return mDateTime;
	}
	
	public Status getStatus()
	{
		return mStatus;
	}
	
	public int getBatteryCapacity()
	{
		return (int)mStatus.getByteValue1() & 0xFF;
	}
	
	public int getReservoirAmount()
	{
		return (int)mStatus.getByteValue2() & 0xFF;
	}
	
	public int getBasalRate()
	{
		return (int)mStatus.getShortValue1();
	}
	
	public int getBolusRate()
	{
		return (int)mStatus.getShortValue2();
	}
	
	public void setDateTime(final DateTime dateTime)
	{
		mDateTime.setByteArray(dateTime.getByteArray());
	}
	
	public void setStatus(final Status status)
	{
		mStatus.setByteArray(status.getByteArray());
	}
	
	public void setBatteryCapacity(int batteryCapacity)
	{
		mStatus.setByteValue1(batteryCapacity);
	}
	
	public void setReservoirAmount(int reservoirAmount)
	{
		mStatus.setByteValue2(reservoirAmount);
	}
	
	public void setBasalRate(int basalRate)
	{
		mStatus.setShortValue1(basalRate);
	}
	
	public void setBolusRate(int bolusRate)
	{
		mStatus.setShortValue2(bolusRate);
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
			dataOutputStream.write(mDateTime.getByteArray());
			dataOutputStream.write(mStatus.getByteArray());
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
				mDateTime.setByteArray(dateTime);
				final byte[] status = new byte[Status.BYTE_ARRAY_LENGTH];
				dataInputStream.read(status, 0, Status.BYTE_ARRAY_LENGTH);
				mStatus.setByteArray(status);
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
