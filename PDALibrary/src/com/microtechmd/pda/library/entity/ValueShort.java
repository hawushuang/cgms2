package com.microtechmd.pda.library.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ValueShort extends DataBundle 
{
	private static final int VALUE_LENGTH = 2;
	private static final String IDENTIFIER = "short";
	private static final String KEY_VALUE = IDENTIFIER + "_value";
	
	
	public ValueShort()
	{
		super();
	}
	
	public ValueShort(byte[] byteArray) 
	{
		super(byteArray);
	}
	
	public ValueShort(short value)
	{
		super();
		setValue(value);
	}
	
	public short getValue()
	{
		return getShort(KEY_VALUE);
	}
	
	public void setValue(short value)
	{
		setShort(KEY_VALUE, value);
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
			dataOutputStream.writeShortLittleEndian(getValue());
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
		
		if (byteArray.length >= VALUE_LENGTH)
		{
			final DataInputStreamLittleEndian dataInputStream;
			final ByteArrayInputStream byteArrayInputStream;
			
			byteArrayInputStream = new ByteArrayInputStream(byteArray);
			dataInputStream = new DataInputStreamLittleEndian(byteArrayInputStream);
			
			try 
			{
				clearBundle();
				setValue(dataInputStream.readShortLittleEndian());
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
