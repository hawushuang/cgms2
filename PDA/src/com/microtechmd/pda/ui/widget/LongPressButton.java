package com.microtechmd.pda.ui.widget;


import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Button;

import com.microtechmd.pda.ApplicationPDA;


public class LongPressButton extends Button
{
	private MontionListener mListener;
	private boolean mDown;


	public LongPressButton(Context context)
	{
		this(context, null);
	}


	public LongPressButton(Context context, AttributeSet attrs)
	{
		this(context, attrs, android.R.attr.buttonStyle);
	}


	public LongPressButton(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		setFocusable(true);
	}


	public void setMontionListener(MontionListener l)
	{
		mListener = l;
	}


	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if (event.getAction() == MotionEvent.ACTION_UP ||
			event.getAction() == MotionEvent.ACTION_CANCEL)
		{
			if (mListener != null)
			{
				mListener.onActionUp();
			}
		}

		if (event.getAction() == MotionEvent.ACTION_DOWN)
		{
			if (mListener != null)
			{
				mListener.onActionDown();
			}
		}

		return super.onTouchEvent(event);
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
			case ApplicationPDA.KEY_CODE_BOLUS:

				if (mListener != null && !mDown)
				{
					mDown = true;
					mListener.onActionDown();
				}

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
			case ApplicationPDA.KEY_CODE_BOLUS:
				mDown = false;

				if (mListener != null)
				{
					mListener.onActionUp();
				}

				break;

			default:
				break;
		}

		return super.onKeyUp(keyCode, event);
	}


	public interface MontionListener
	{
		void onActionUp();


		void onActionDown();
	}
}
