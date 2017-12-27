package com.microtechmd.pda.library.entity;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.util.ArrayList;


public class DataStorage
{
	// Field definition
	private static final String KEY_SUFFIX_COUNT = "_count_";
	private static final String KEY_SUFFIX_ARRAY = "_array_";

	private SharedPreferences mSharedPreferences = null;
	private String mName = null;


	// Inner class definition


	// Method definition

	public DataStorage(final Context context, String name)
	{
		if ((context != null) && (name != null))
		{
			mSharedPreferences =
				context.getSharedPreferences(name, Context.MODE_PRIVATE);
			mName = name;
		}
	}


	public String getName()
	{
		return mName;
	}


	public boolean getBoolean(String key, boolean defaultValue)
	{
		return mSharedPreferences.getBoolean(key, defaultValue);
	}


	public byte getByte(String key, byte defaultValue)
	{
		return (byte)mSharedPreferences.getInt(key, (int)defaultValue);
	}


	public short getShort(String key, short defaultValue)
	{
		return (short)mSharedPreferences.getInt(key, (int)defaultValue);
	}


	public int getInt(String key, int defaultValue)
	{
		return mSharedPreferences.getInt(key, defaultValue);
	}


	public long getLong(String key, long defaultValue)
	{
		return mSharedPreferences.getLong(key, defaultValue);
	}


	public float getFloat(String key, float defaultValue)
	{
		return mSharedPreferences.getFloat(key, defaultValue);
	}


	public String getString(String key, String defaultValue)
	{
		return mSharedPreferences.getString(key, defaultValue);
	}


	public ArrayList<String> getStringArray(String key,
		ArrayList<String> defaultValue)
	{
		int count =
			mSharedPreferences.getInt(
				key + KEY_SUFFIX_ARRAY + KEY_SUFFIX_COUNT, 0);

		if (count == 0)
		{
			return defaultValue;
		}

		ArrayList<String> stringArray = new ArrayList<String>();

		for (int i = 0; i < count; i++)
		{
			String string =
				mSharedPreferences.getString(key + KEY_SUFFIX_ARRAY + i, null);

			if (string == null)
			{
				return defaultValue;
			}

			stringArray.add(string);
		}

		return stringArray;
	}


	public byte[] getExtras(String key, byte[] defaultValue)
	{
		final String defaultString;

		if (defaultValue != null)
		{
			defaultString = Base64.encodeToString(defaultValue, Base64.DEFAULT);
		}
		else
		{
			defaultString = "";
		}

		final String resultString =
			mSharedPreferences.getString(key, defaultString);

		if (resultString.equals(""))
		{
			return null;
		}
		else
		{
			return Base64.decode(resultString, Base64.DEFAULT);
		}
	}


	public void setBoolean(String key, boolean value)
	{
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}


	public void setByte(String key, byte value)
	{
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putInt(key, (int)value);
		editor.commit();
	}


	public void setShort(String key, short value)
	{
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putInt(key, (int)value);
		editor.commit();
	}


	public void setInt(String key, int value)
	{
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putInt(key, value);
		editor.commit();
	}


	public void setLong(String key, long value)
	{
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putLong(key, value);
		editor.commit();
	}


	public void setFloat(String key, float value)
	{
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putFloat(key, value);
		editor.commit();
	}


	public void setString(String key, String value)
	{
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}


	public void setStringArray(String key, ArrayList<String> value)
	{
		if (value.size() > 0)
		{
			SharedPreferences.Editor editor = mSharedPreferences.edit();
			editor.putInt(key + KEY_SUFFIX_ARRAY + KEY_SUFFIX_COUNT,
				value.size());

			for (int i = 0; i < value.size(); i++)
			{
				editor.putString(key + KEY_SUFFIX_ARRAY + i, value.get(i));
			}

			editor.commit();
		}
	}


	public void setExtras(String key, byte[] value)
	{
		final String string;


		SharedPreferences.Editor editor = mSharedPreferences.edit();

		if (value != null)
		{
			string = Base64.encodeToString(value, Base64.DEFAULT);
		}
		else
		{
			string = "";
		}

		editor.putString(key, string);
		editor.commit();
	}


	public void clear()
	{
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.clear();
		editor.commit();
	}
}
