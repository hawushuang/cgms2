package com.microtechmd.pda.library.entity.comm;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.microtechmd.pda.library.entity.DataBundle;


public class RFAddress extends DataBundle
{
	public static final int BYTE_ARRAY_LENGTH = 6;
	public static final String RF_ADDRESS_UNPAIR = "000000";

	private static final String IDENTIFIER = "rf";
	private static final String KEY_ADDRESS = IDENTIFIER + "_address";


	public RFAddress()
	{
		super();
	}


	public RFAddress(byte[] byteArray)
	{
		super(byteArray);
	}


	public RFAddress(final String address)
	{
		super();
		setAddress(address);
	}


	public String getAddress()
	{
		byte[] addressByte = getExtras(KEY_ADDRESS);
		String addressString = "";

		if (addressByte != null)
		{
			for (int i = 0; i < addressByte.length; i++)
			{
				String hex = Integer.toHexString(addressByte[i] & 0xFF);
				addressString += hex.toUpperCase();
			}
		}

		return addressString;
	}


	public void setAddress(final String address)
	{
		if (address.length() > BYTE_ARRAY_LENGTH)
		{
			return;
		}

		byte[] addressString = address.getBytes();
		
		if (addressString == null)
		{
			return;
		}
		
		byte[] addressByte = new byte[BYTE_ARRAY_LENGTH];

		for (int i = 0; i < addressString.length; i++)
		{
			if ((addressString[i] >= '0') && (addressString[i] <= '9'))
			{
				addressString[i] -= '0';
			}
			else if ((addressString[i] >= 'A') && (addressString[i] <= 'Z'))
			{
				addressString[i] -= 'A';
				addressString[i] += 10;
			}
			else if ((addressString[i] >= 'a') && (addressString[i] <= 'z'))
			{
				addressString[i] -= 'a';
				addressString[i] += 10;
			}
			else
			{
				return;
			}
		}

		for (int i = 0; i < BYTE_ARRAY_LENGTH; i++)
		{
			if (i < addressString.length)
			{
				addressByte[i] = addressString[i];
			}
			else
			{
				addressByte[i] = 0;
			}
		}

		setExtras(KEY_ADDRESS, addressByte);
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
			final byte[] address = getExtras(KEY_ADDRESS);

			if (address != null)
			{
				dataOutputStream.write(address);
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
				final byte[] address = new byte[BYTE_ARRAY_LENGTH];
				dataInputStream.read(address, 0, BYTE_ARRAY_LENGTH);
				setExtras(KEY_ADDRESS, address);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
