package com.microtechmd.pda.library.entity.monitor;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.microtechmd.pda.library.entity.DataBundle;


public class HistoryStatistics extends DataBundle
{
	public static final int BYTE_ARRAY_LENGTH = 20 + DateTime.BYTE_ARRAY_LENGTH;

	private static final String IDENTIFIER = "history_statistics";
	private static final String KEY_DATETIME = IDENTIFIER + "_datetime";
	private static final String KEY_BASAL_AMOUNT = IDENTIFIER + "_basal_amount";
	private static final String KEY_BOLUS_AMOUNT = IDENTIFIER + "_bolus_amount";
	private static final String KEY_BOLUS_EXTEND_AMOUNT = IDENTIFIER +
		"_bolus_extend_amount";
	private static final String KEY_GLUCOSE_AVERAGE = IDENTIFIER +
		"_glucose_average";
	private static final String KEY_CARBOHYDRATE_AMOUNT = IDENTIFIER +
		"_carbohydrate_amount";


	public HistoryStatistics()
	{
		super();
	}


	public HistoryStatistics(byte[] byteArray)
	{
		super(byteArray);
	}


	public HistoryStatistics(final DateTime dateTime, int basalAmount,
		int bolusAmount, int bolusExtendAmount, int glucoseAverage,
		int carbohydrateAmount)
	{
		super();
		setDateTime(dateTime);
		setBasalAmount(basalAmount);
		setBolusAmount(bolusAmount);
		setBolusExtendAmount(bolusExtendAmount);
		setGlucoseAverage(glucoseAverage);
		setCarbohydrateAmount(carbohydrateAmount);
	}


	public DateTime getDateTime()
	{
		return new DateTime(getExtras(KEY_DATETIME));
	}


	public int getBasalAmount()
	{
		return getInt(KEY_BASAL_AMOUNT);
	}


	public int getBolusAmount()
	{
		return getInt(KEY_BOLUS_AMOUNT);
	}


	public int getBolusExtendAmount()
	{
		return getInt(KEY_BOLUS_EXTEND_AMOUNT);
	}


	public int getGlucoseAverage()
	{
		return getInt(KEY_GLUCOSE_AVERAGE);
	}


	public int getCarbohydrateAmount()
	{
		return getInt(KEY_CARBOHYDRATE_AMOUNT);
	}


	public void setDateTime(final DateTime dateTime)
	{
		setExtras(KEY_DATETIME, dateTime.getByteArray());
	}


	public void setBasalAmount(int value)
	{
		setInt(KEY_BASAL_AMOUNT, value);
	}


	public void setBolusAmount(int value)
	{
		setInt(KEY_BOLUS_AMOUNT, value);
	}


	public void setBolusExtendAmount(int value)
	{
		setInt(KEY_BOLUS_EXTEND_AMOUNT, value);
	}


	public void setGlucoseAverage(int value)
	{
		setInt(KEY_GLUCOSE_AVERAGE, value);
	}


	public void setCarbohydrateAmount(int value)
	{
		setInt(KEY_CARBOHYDRATE_AMOUNT, value);
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
			byte[] dateTime = getExtras(KEY_DATETIME);
			
			if (dateTime == null)
			{
				dateTime = new byte[DateTime.BYTE_ARRAY_LENGTH];
			}

			dataOutputStream.write(dateTime);
			dataOutputStream.writeInt(getBasalAmount());
			dataOutputStream.writeInt(getBolusAmount());
			dataOutputStream.writeInt(getBolusExtendAmount());
			dataOutputStream.writeInt(getGlucoseAverage());
			dataOutputStream.writeInt(getCarbohydrateAmount());
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
				setExtras(KEY_DATETIME, dateTime);
				setBasalAmount(dataInputStream.readInt());
				setBolusAmount(dataInputStream.readInt());
				setBolusExtendAmount(dataInputStream.readInt());
				setGlucoseAverage(dataInputStream.readInt());
				setCarbohydrateAmount(dataInputStream.readInt());
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
