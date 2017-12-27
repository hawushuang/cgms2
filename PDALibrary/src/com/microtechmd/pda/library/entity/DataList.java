package com.microtechmd.pda.library.entity;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class DataList extends DataBundle
{
	private static final int COUNT_LENGTH = 4;
	private static final String IDENTIFIER = "list";
	private static final String KEY_DATA = IDENTIFIER + "_data";

	private int mCount = 0;


	public DataList()
	{
		super();
	}


	public DataList(byte[] byteArray)
	{
		super();
		setByteArray(byteArray);
	}


	public int getCount()
	{
		return mCount;
	}


	public byte[] getData(int index)
	{
		if (index < mCount)
		{
			return getExtras(KEY_DATA + index);
		}
		else
		{
			return null;
		}
	}


	public void setData(int index, byte[] data)
	{
		if ((index <= mCount) && (data != null))
		{
			setExtras(KEY_DATA + index, data);

			if (index == mCount)
			{
				mCount++;
			}
		}
	}


	public byte[] popData()
	{
		if (mCount > 0)
		{
			mCount--;
			return getExtras(KEY_DATA + mCount);
		}
		else
		{
			return null;
		}
	}


	public void pushData(byte[] data)
	{
		if (data != null)
		{
			setExtras(KEY_DATA + mCount, data);
			mCount++;
		}
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
			dataOutputStream.writeIntLittleEndian(mCount);

			for (int i = 0; i < mCount; i++)
			{
				byte[] data = getData(i);
				dataOutputStream.writeIntLittleEndian(data.length);
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

		if (byteArray.length >= COUNT_LENGTH)
		{
			final DataInputStreamLittleEndian dataInputStream;
			final ByteArrayInputStream byteArrayInputStream;

			byteArrayInputStream = new ByteArrayInputStream(byteArray);
			dataInputStream =
				new DataInputStreamLittleEndian(byteArrayInputStream);

			try
			{
				clearBundle();
				int lengthList = byteArray.length;
				int lenghtData = 0;
				mCount = dataInputStream.readIntLittleEndian();
				lengthList -= COUNT_LENGTH;

				for (int i = 0; i < mCount; i++)
				{
					if (lengthList >= COUNT_LENGTH)
					{
						lenghtData = dataInputStream.readIntLittleEndian();
						lengthList -= COUNT_LENGTH;
					}
					else
					{
						clearBundle();
						mCount = 0;
						break;
					}

					if (lengthList >= lenghtData)
					{
						byte[] data = new byte[lenghtData];
						dataInputStream.read(data, 0, lenghtData);
						setExtras(KEY_DATA + i, data);
						lengthList -= lenghtData;
					}
					else
					{
						clearBundle();
						mCount = 0;
						break;
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
