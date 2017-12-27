package com.microtechmd.pda.library.entity.glucose;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.microtechmd.pda.library.entity.DataBundle;
import com.microtechmd.pda.library.entity.monitor.DateTime;
import com.microtechmd.pda.library.entity.monitor.Status;


public class StatusGlucose extends DataBundle
{
	public static final int BYTE_ARRAY_LENGTH = 12;

	private DateTime mDateTime = null;
	private Status mStatus = null;


	public StatusGlucose()
	{
		super();
		mDateTime = new DateTime();
		mStatus = new Status();
	}


	public StatusGlucose(byte[] byteArray)
	{
		super();
		mDateTime = new DateTime();
		mStatus = new Status();
		this.setByteArray(byteArray);
	}


	public StatusGlucose(int glucose)
	{
		super();
		mDateTime = new DateTime();
		mStatus = new Status();
		mStatus.setShortValue1(glucose);
	}


	public StatusGlucose(DateTime dateTime, Status status)
	{
		super();
		mDateTime = new DateTime(dateTime.getByteArray());
		mStatus = new Status(status.getByteArray());
	}


	public DateTime getDateTime()
	{
		return mDateTime;
	}


	public int getFlag()
	{
		return (int)mStatus.getByteValue1();
	}


	public int getGlucose()
	{
		return (int)mStatus.getShortValue1();
	}


	public Status getStatus()
	{
		return mStatus;
	}


	public void setDateTime(final DateTime dateTime)
	{
		mDateTime.setByteArray(dateTime.getByteArray());
	}


	public void setFlag(int flag)
	{
		mStatus.setByteValue1(flag);
	}


	public void setGlucose(int glucose)
	{
		mStatus.setShortValue1(glucose);
	}


	public void setStatus(final Status status)
	{
		mStatus.setByteArray(status.getByteArray());
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
			dataOutputStream.write(mDateTime.getByteArray());
			dataOutputStream.write(mStatus.getByteArray());
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

		if (byteArray.length >= BYTE_ARRAY_LENGTH)
		{
			final DataInputStreamLittleEndian dataInputStream;
			final ByteArrayInputStream byteArrayInputStream;

			byteArrayInputStream = new ByteArrayInputStream(byteArray);
			dataInputStream =
				new DataInputStreamLittleEndian(byteArrayInputStream);

			try
			{
				clearBundle();
				final byte[] dateTime = new byte[DateTime.BYTE_ARRAY_LENGTH];
				dataInputStream.read(dateTime, 0, DateTime.BYTE_ARRAY_LENGTH);
				mDateTime.setByteArray(dateTime);
				final byte[] status = new byte[Status.BYTE_ARRAY_LENGTH];
				dataInputStream.read(status, 0, Status.BYTE_ARRAY_LENGTH);
				mStatus.setByteArray(status);
				dataInputStream.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
