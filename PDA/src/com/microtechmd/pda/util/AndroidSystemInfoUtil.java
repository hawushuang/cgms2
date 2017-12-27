package com.microtechmd.pda.util;


import java.io.File;
import java.util.Calendar;
import java.util.Locale;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.WindowManager;


public class AndroidSystemInfoUtil
{

	// ��������
	public static void setMediaVolume(Context context, int volume)
	{
		AudioManager audioManager = (AudioManager) context
			.getSystemService(Context.AUDIO_SERVICE);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume,
			AudioManager.FLAG_SHOW_UI);
	}


	public static int getMediaVolume(Context context)
	{
		AudioManager audioManager = (AudioManager) context
			.getSystemService(Context.AUDIO_SERVICE);
		return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	}


	// ��������
	public static void setSystemTime(int year, int monthOfYear, int dayOfMonth)
	{
		Calendar calendar = Calendar.getInstance();
		int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		setSystemTime(year, monthOfYear, dayOfMonth, hourOfDay, minute);
	}


	public static boolean isSDCardMounted()
	{
		boolean isSDCard = false;
		if (android.os.Environment.getExternalStorageState().equals(
			android.os.Environment.MEDIA_MOUNTED))
		{
			isSDCard = true;
		}
		return isSDCard;
	}


	// ����ʱ��
	public static void setSystemTime(int hourOfDay, int minute)
	{
		Calendar calendar = Calendar.getInstance();
		int year = calendar.get(Calendar.YEAR);
		int monthOfYear = calendar.get(Calendar.MONTH);
		int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
		setSystemTime(year, monthOfYear, dayOfMonth, hourOfDay, minute);
	}


	public static void setSystemTime(int year, int monthOfYear, int dayOfMonth,
		int hourOfDay, int minute)
	{
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, monthOfYear);
		calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
		calendar.set(Calendar.MINUTE, minute);

		long when = calendar.getTimeInMillis();
		if (when / 1000 < Integer.MAX_VALUE)
		{
			SystemClock.setCurrentTimeMillis(when);
		}
	}


	// ��ȡ����
	public static Locale getLanguage()
	{
		return Locale.getDefault();
	}


	// ��������
	public static void setLanguage(Activity activity, String language)
	{
		Locale locale = new Locale(language);
		Locale.setDefault(locale);
		Configuration config = new Configuration();
		config.locale = locale;
		activity.getBaseContext().getResources()
			.updateConfiguration(config, null);
	}


	// ������Ļ����
	public static void setBrightness(Activity activity, int bright)
	{
		if (isAutoBrightness(activity.getContentResolver()))
		{
			stopAutoBrightness(activity);
		}

		Settings.System.putInt(activity.getContentResolver(),
			Settings.System.SCREEN_BRIGHTNESS, bright);
		bright = Settings.System.getInt(activity.getContentResolver(),
			Settings.System.SCREEN_BRIGHTNESS, -1);
		WindowManager.LayoutParams wl = activity.getWindow().getAttributes();

		float tmpFloat = (float) bright / 255;
		if (tmpFloat > 0 && tmpFloat <= 1)
		{
			wl.screenBrightness = tmpFloat;
		}
		activity.getWindow().setAttributes(wl);
	}


	// �Ƿ��Զ�������Ļ����
	public static boolean isAutoBrightness(ContentResolver aContentResolver)
	{
		boolean automicBrightness = false;
		try
		{
			automicBrightness = Settings.System.getInt(aContentResolver,
				Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
		}
		catch (SettingNotFoundException e)
		{
			e.printStackTrace();
		}
		return automicBrightness;
	}


	// �Ƿ�ֹͣ������Ļ����
	public static void stopAutoBrightness(Activity activity)
	{
		Settings.System.putInt(activity.getContentResolver(),
			Settings.System.SCREEN_BRIGHTNESS_MODE,
			Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
	}


	// ������Ļ���ĳ�ʱʱ��
	public static void setScreenOffTimeout(Context context, int time)
	{
		Settings.System.putInt(context.getContentResolver(),
			Settings.System.SCREEN_OFF_TIMEOUT, time);
	}


	// ��ȡSD���Ŀ��ÿռ�
	public static long getSDAvailaleSize()
	{
		long size = 0;
		if (Environment.getExternalStorageState().equals(
			Environment.MEDIA_MOUNTED))
		{
			File path = Environment.getExternalStorageDirectory(); // ȡ��sdcard�ļ�·��
			StatFs stat = new StatFs(path.getPath());
			long blockSize = stat.getBlockSize();
			long availableBlocks = stat.getAvailableBlocks();
			size = availableBlocks * blockSize;
		}
		return size;
	}


	// ��ȡSD�����ܿռ�
	public static long getSDAllSize()
	{
		long size = 0;
		if (Environment.getExternalStorageState().equals(
			Environment.MEDIA_MOUNTED))
		{
			File path = Environment.getExternalStorageDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize = stat.getBlockSize();
			long availableBlocks = stat.getBlockCount();
			size = availableBlocks * blockSize;
		}
		return size;
	}

}
