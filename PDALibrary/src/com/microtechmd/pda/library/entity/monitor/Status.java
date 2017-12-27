package com.microtechmd.pda.library.entity.monitor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.microtechmd.pda.library.entity.DataBundle;

public class Status extends DataBundle 
{
	public static final int BYTE_ARRAY_LENGTH = 6;
	
	private static final String IDENTIFIER = "status";
	private static final String KEY_BYTE_VALUE1 = IDENTIFIER + "_byte_value1";
	private static final String KEY_BYTE_VALUE2 = IDENTIFIER + "_byte_value2";
	private static final String KEY_SHORT_VALUE1 = IDENTIFIER + "_short_value1";
	private static final String KEY_SHORT_VALUE2 = IDENTIFIER + "_short_value2";
	
	
	public Status()
	{
		super();
	}
	
	public Status(byte[] byteArray) 
	{
		super(byteArray);
	}
	
	public Status(int byteValue1, int byteValue2, int shortValue1, int shortValue2)
	{
		super();
		setByteValue1(byteValue1);
		setByteValue2(byteValue2);
		setShortValue1(shortValue1);
		setShortValue2(shortValue2);
	}
	
	public int getByteValue1()
	{
		return (int)getByte(KEY_BYTE_VALUE1);
	}
	
	public int getByteValue2()
	{
		return (int)getByte(KEY_BYTE_VALUE2);
	}
	
	public int getShortValue1()
	{
		return (int)getShort(KEY_SHORT_VALUE1);
	}
	
	public int getShortValue2()
	{
		return (int)getShort(KEY_SHORT_VALUE2);
	}
	
	public void setByteValue1(int value)
	{
		setByte(KEY_BYTE_VALUE1, (byte)value);
	}
	
	public void setByteValue2(int value)
	{
		setByte(KEY_BYTE_VALUE2, (byte)value);
	}
	
	public void setShortValue1(int value)
	{
		setShort(KEY_SHORT_VALUE1, (short)value);
	}
	
	public void setShortValue2(int value)
	{
		setShort(KEY_SHORT_VALUE2, (short)value);
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
			dataOutputStream.writeByte((byte)getByteValue1());
			dataOutputStream.writeByte((byte)getByteValue2());
			dataOutputStream.writeShortLittleEndian((short)getShortValue1());
			dataOutputStream.writeShortLittleEndian((short)getShortValue2());
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
				setByteValue1((int)dataInputStream.readByte());
				setByteValue2((int)dataInputStream.readByte());
				setShortValue1((int)dataInputStream.readShortLittleEndian());
				setShortValue2((int)dataInputStream.readShortLittleEndian());
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
