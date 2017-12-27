package com.microtechmd.pda.library.entity;

import android.os.Bundle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class DataBundle 
{
	//Field definition
	
	private final Bundle mBundle;
	
	
	//Inner class definition
	
	public class DataInputStreamLittleEndian extends DataInputStream
	{
		public DataInputStreamLittleEndian(InputStream out)
		{
			super(out);
		}
		
		public final short readShortLittleEndian() throws IOException
		{
			short value;
			int result = 0;
			
			value = readShort(); 
			result = ((int)value << 8) & 0x0000FF00;
			result |= ((int)value >> 8) & 0x000000FF;
			
			return (short)result;
		}
		
		public final int readIntLittleEndian() throws IOException
		{
			int value;
			int result = 0;
			
			value = readInt(); 
			result = ((int)value << 24) & 0xFF000000;
			result |= ((int)value << 8) & 0x00FF0000;
			result |= ((int)value >> 8) & 0x0000FF00;
			result |= ((int)value >> 24) & 0x000000FF;
			
			return result;
		}
	}
	
	public class DataOutputStreamLittleEndian extends DataOutputStream
	{
		public DataOutputStreamLittleEndian(OutputStream out)
		{
			super(out);
		}
		
		public final void writeShortLittleEndian(short val) throws IOException 
		{
			writeByte((val & 0x00FF));
			writeByte((val >> 8) & 0x00FF);
		}
		
		public final void writeIntLittleEndian(int val) throws IOException 
		{
			writeByte((val & 0x000000FF));
			writeByte((val >> 8) & 0x000000FF);
			writeByte((val >> 16) & 0x000000FF);
			writeByte((val >> 24) & 0x000000FF);
		}
	}
	
	
	//Method definition
	
	protected DataBundle()
	{
		mBundle = new Bundle();
	}
	
	protected DataBundle(byte[] byteArray)
	{
		mBundle = new Bundle();
		setByteArray(byteArray);
	}
	
	protected byte getByte(String key)
	{
		return mBundle.getByte(key);
	}
	
	protected short getShort(String key)
	{
		return mBundle.getShort(key);
	}
	
	protected int getInt(String key)
	{
		return mBundle.getInt(key);
	}
	
	protected float getFloat(String key)
	{
		return mBundle.getFloat(key);
	}
	
	protected double getDouble(String key)
	{
		return mBundle.getDouble(key);
	}
	
	protected String getString(String key)
	{
		return mBundle.getString(key);
	}
	
	protected byte[] getExtras(String key)
	{
		return mBundle.getByteArray(key);
	}
	
	protected void setByte(String key, byte value)
	{
		mBundle.putByte(key, value);
	}
	
	protected void setShort(String key, short value)
	{
		mBundle.putShort(key, value);
	}
	
	protected void setInt(String key, int value)
	{
		mBundle.putInt(key, value);
	}
	
	protected void setFloat(String key, float value)
	{
		mBundle.putFloat(key, value);
	}
	
	protected void setDouble(String key, double value)
	{
		mBundle.putDouble(key, value);
	}
	
	protected void setString(String key, String value)
	{
		mBundle.putString(key, value);
	}
	
	protected void setExtras(String key, byte[] value)
	{
		mBundle.putByteArray(key, value);
	}
	
	public Bundle getAll()
	{
		return mBundle;
	}
	
	public void setAll(Bundle bundle)
	{
		mBundle.clear();
		mBundle.putAll(bundle);
	}
	
	public void clearBundle()
	{
		mBundle.clear();
	}
	
	public abstract byte[] getByteArray();
	public abstract void setByteArray(byte[] byteArray);
}
