package com.microtechmd.pda.library.entity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ValueInt extends DataBundle 
{
	private static final int VALUE_LENGTH = 4;
	private static final String IDENTIFIER = "int";
	private static final String KEY_VALUE = IDENTIFIER + "_value";
	
	
	public ValueInt()
	{
		super();
	}
	
	public ValueInt(byte[] byteArray) 
	{
		super(byteArray);
	}
	
	public ValueInt(int value)
	{
		super();
		setValue(value);
	}
	
	public int getValue()
	{
		return getInt(KEY_VALUE);
	}
	
	public void setValue(int value)
	{
		setInt(KEY_VALUE, value);
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
			dataOutputStream.writeIntLittleEndian(getValue());
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
				setValue(dataInputStream.readIntLittleEndian());
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
