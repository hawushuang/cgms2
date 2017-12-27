package com.microtechmd.pda.ui.widget;


import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;


public class WidgetButtonRepeating extends Button
{
	private final static int REPEAT_INTERVAL_DEFAULT = 50;
	private final static int KEY_CODE_BOLUS = 111;

	private int mRepeatCount = 0;
	private long mStartTime = 0;
	private long mInterval = REPEAT_INTERVAL_DEFAULT;
	private RepeatListener mListener = null;


	private final Runnable mRepeater = new Runnable()
	{
		@Override
		public void run()
		{
			doRepeat(false);

			if (isPressed())
			{
				postDelayed(this, mInterval);
			}
		}
	};


	public interface RepeatListener
	{
		void onRepeat(View v, long duration, int repeatCount);


		void onTouch(MotionEvent event);
	}


	public WidgetButtonRepeating(Context context)
	{
		super(context);
		setFocusable(true);
		setLongClickable(true);
	}


	public WidgetButtonRepeating(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setFocusable(true);
		setLongClickable(true);
	}


	public WidgetButtonRepeating(Context context,
		AttributeSet attrs,
		int defStyle)
	{
		super(context, attrs, defStyle);
		setFocusable(true);
		setLongClickable(true);
	}


	public void setRepeatListener(RepeatListener l)
	{
		mListener = l;
		mInterval = REPEAT_INTERVAL_DEFAULT;
	}


	public void setRepeatListener(RepeatListener l, long interval)
	{
		mListener = l;
		mInterval = interval;
	}

	
	@Override
	public boolean performClick()
	{
		doRepeat(false);
		
		return true;
	}
	

	@Override
	public boolean performLongClick()
	{
		mStartTime = SystemClock.elapsedRealtime();
		mRepeatCount = 0;
		post(mRepeater);

		return true;
	}


	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if (mListener != null)
		{
			mListener.onTouch(event);
		}

		if (event.getAction() == MotionEvent.ACTION_UP)
		{
			removeCallbacks(mRepeater);

			if (mStartTime != 0)
			{
				mStartTime = 0;
			}
		}

		return super.onTouchEvent(event);
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_ENTER:
				super.onKeyDown(keyCode, event);
				return true;

			case KEY_CODE_BOLUS:
				mStartTime = SystemClock.elapsedRealtime();
				mRepeatCount = 0;
				post(mRepeater);
				break;

			default:
				break;
		}

		return super.onKeyDown(keyCode, event);
	}


	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_ENTER:
			case KEY_CODE_BOLUS:
				removeCallbacks(mRepeater);

				if (mStartTime != 0)
				{
					mStartTime = 0;
				}

				break;

			default:
				break;
		}

		return super.onKeyUp(keyCode, event);
	}


	private void doRepeat(boolean last)
	{
		long now = SystemClock.elapsedRealtime();

		if (mListener != null)
		{
			mListener.onRepeat(this, now - mStartTime, last ? -1
				: mRepeatCount++);
		}
	}
}
