package com.microtechmd.pda.util;


import java.io.IOException;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Vibrator;

import com.microtechmd.pda.manager.BroadcastReceiveManager;
import com.microtechmd.pda.manager.SharePreferenceManager;


public class MediaUtil
{

	private static MediaPlayer mMediaPlayer = new MediaPlayer();
	private static Vibrator mVib;
	private static Context mContext;


	public static void playMp3ByType(Context context, String mp3File,
		boolean isUseSetting)
	{
		mContext = context;
		if (SharePreferenceManager.isSoundOpen(context) || !isUseSetting)
		{
			AndroidSystemInfoUtil.setMediaVolume(context, 100);
			Intent intent = new Intent(BroadcastReceiveManager.getFullAction(
				context, BroadcastReceiveManager.ACTION_PAUSE_MUSIC));
			context.sendBroadcast(intent);
			playMp3(context, mp3File);
		}

		if (SharePreferenceManager.isVibrationOpen(context) || !isUseSetting)
		{
			vibrate(context, 3000);
		}
	}


	private static void playMp3(final Context context, String mp3File)
	{
		AssetFileDescriptor fileDescriptor;
		try
		{
			fileDescriptor = context.getAssets().openFd(mp3File);
			mMediaPlayer.reset();
			mMediaPlayer.setOnCompletionListener(new OnCompletionListener()
			{

				@Override
				public void onCompletion(MediaPlayer mp)
				{
					Intent intent = new Intent(BroadcastReceiveManager
						.getFullAction(context,
							BroadcastReceiveManager.ACTION_RESUME_MUSIC));
					context.sendBroadcast(intent);
				}
			});
			mMediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(),
				fileDescriptor.getStartOffset(), fileDescriptor.getLength());
			mMediaPlayer.prepare();
			mMediaPlayer.start();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}


	public static void stop()
	{
		if (mMediaPlayer != null)
		{
			mMediaPlayer.stop();
			if (mContext == null)
				return;
			Intent intent = new Intent(BroadcastReceiveManager.getFullAction(
				mContext, BroadcastReceiveManager.ACTION_RESUME_MUSIC));
			mContext.sendBroadcast(intent);
		}
	}


	public static void vibrate(Context context, long milliseconds)
	{
		mVib = (Vibrator) context.getSystemService(Service.VIBRATOR_SERVICE);
		mVib.vibrate(milliseconds);
	}

}
