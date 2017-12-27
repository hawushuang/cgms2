package com.microtechmd.pda.library.entity.system;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.microtechmd.pda.library.entity.DataBundle;

public class RelayCommand extends DataBundle
{
	//Constant definition
	
	private static final int MESSAGE_LENGTH = 4;
	private static final String MESSAGE_IDENTIFIER = "relay";
	private static final String KEY_SOURCE_PORT = MESSAGE_IDENTIFIER + "_source_port";
	private static final String KEY_TARGET_PORT = MESSAGE_IDENTIFIER + "_target_port";
	private static final String KEY_OPERATION = MESSAGE_IDENTIFIER + "_operation";
	private static final String KEY_PARAMETER = MESSAGE_IDENTIFIER + "_parameter";
	private static final String KEY_DATA = MESSAGE_IDENTIFIER + "_data";
	
		
	//Inner class definition
	
	public interface Listener 
	{
		void onReceive(RelayCommand message);
	}

	
	//Method definition
	
	public RelayCommand()
	{
		super();
	}
	
	public RelayCommand(byte[] byteArray) 
	{
		super(byteArray);
	}
	
	public RelayCommand(int sourcePort, int targetPort, int operation, 
		int parameter, byte[] data)
	{
		super();
		setSourcePort(sourcePort);
		setTargetPort(targetPort);
		setOperation(operation);
		setParameter(parameter);
		setData(data);
	}
	
	public int getSourcePort()
	{
		return getInt(KEY_SOURCE_PORT);
	}
	
	public int getTargetPort()
	{
		return getInt(KEY_TARGET_PORT);
	}
	
	public int getOperation()
	{
		return getInt(KEY_OPERATION);
	}
	
	public int getParameter()
	{
		return getInt(KEY_PARAMETER);
	}
	
	public byte[] getData()
	{
		return getExtras(KEY_DATA);
	}
	
	public void setSourcePort(int sourcePort)
	{
		setInt(KEY_SOURCE_PORT, sourcePort);
	}
	
	public void setTargetPort(int targetPort)
	{
		setInt(KEY_TARGET_PORT, targetPort);
	}
	
	public void setOperation(int operation)
	{
		setInt(KEY_OPERATION, operation);
	}
	
	public void setParameter(int parameter)
	{
		setInt(KEY_PARAMETER, parameter);
	}
	
	public void setData(byte[] data)
	{
		setExtras(KEY_DATA, data);
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
			dataOutputStream.write(getInt(KEY_SOURCE_PORT));
			dataOutputStream.write(getInt(KEY_TARGET_PORT));
			dataOutputStream.write(getInt(KEY_OPERATION));
			dataOutputStream.write(getInt(KEY_PARAMETER));
			
			final byte[] data = getExtras(KEY_DATA);
			
			if (data != null)
			{
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
		
		if (byteArray.length >= MESSAGE_LENGTH)
		{
			final DataInputStreamLittleEndian dataInputStream;
			final ByteArrayInputStream byteArrayInputStream;
			
			byteArrayInputStream = new ByteArrayInputStream(byteArray);
			dataInputStream = new DataInputStreamLittleEndian(byteArrayInputStream);
			
			try 
			{
				clearBundle();
				setInt(KEY_SOURCE_PORT, dataInputStream.read());
				setInt(KEY_TARGET_PORT, dataInputStream.read());
				setInt(KEY_OPERATION, dataInputStream.read());
				setInt(KEY_PARAMETER, dataInputStream.read());
				
				int dataLength = byteArray.length - MESSAGE_LENGTH;
				
				if (dataLength > 0)
				{
					final byte[] data = new byte[dataLength];
					dataInputStream.read(data, 0, dataLength);
					setExtras(KEY_DATA, data);
				}
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
