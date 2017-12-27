package com.microtechmd.pda.ui.widget;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.microtechmd.pda.ApplicationPDA;
import com.microtechmd.pda.R;
import com.microtechmd.pda.library.utility.LogPDA;
import com.microtechmd.pda.ui.activity.ActivityPDA;
import com.microtechmd.pda.ui.widget.WidgetButtonRepeating.RepeatListener;
import com.microtechmd.pda.util.KeyNavigation;


public abstract class BaseDialog extends Dialog
	implements KeyNavigation.OnClickViewListener
{
	private View mView = null;
	private View mDialogTrigger = null;
	protected LogPDA mLog = null;

	private Context mContext = null;
	private KeyNavigation mKeyNavigation = null;


	public BaseDialog(Context context)
	{
		this(context, R.style.ThemeDialog);
	}


	public BaseDialog(Context context, int theme)
	{
		super(context, theme);

		mContext = context;
		mLog = new LogPDA();
		mKeyNavigation = new KeyNavigation(context, this);

		setOnShowListener(new DialogInterface.OnShowListener()
		{
			@Override
			public void onShow(DialogInterface dialog)
			{
				((ActivityPDA)mContext).pushScreenWindow(getWindow());

				if (mDialogTrigger != null)
				{
					mDialogTrigger.setEnabled(false);
				}
			}
		});

		setOnDismissListener(new DialogInterface.OnDismissListener()
		{
			@Override
			public void onDismiss(DialogInterface dialog)
			{
				((ActivityPDA)mContext).popScreenWindow();

				if (mDialogTrigger != null)
				{
					mDialogTrigger.setEnabled(true);
				}
			}
		});

		getWindow().setGravity(Gravity.CENTER);
	}


	@Override
	public boolean dispatchTouchEvent(MotionEvent ev)
	{
		if (ev.getAction() == MotionEvent.ACTION_DOWN)
		{
			((ActivityPDA)mContext).updateScreenBrightness();
		}

		return super.dispatchTouchEvent(ev);
	}


	@Override
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		if (event.getAction() == KeyEvent.ACTION_UP)
		{
			((ActivityPDA)mContext).updateScreenBrightness();
		}

		return super.dispatchKeyEvent(event);
	}


	@Override
	public void onAttachedToWindow()
	{
		this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
		super.onAttachedToWindow();
	}


	@Override
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);

		if (hasFocus)
		{
			((ActivityPDA)mContext).updateScreenBrightness();
		}
	}


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_HOME:
			case KeyEvent.KEYCODE_VOLUME_DOWN:
			case KeyEvent.KEYCODE_VOLUME_UP:
				return true;

			default:
				return super.onKeyDown(keyCode, event);
		}
	}


	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				return mKeyNavigation.onKeyNext();

			case KeyEvent.KEYCODE_HOME:
				return true;

			case KeyEvent.KEYCODE_VOLUME_UP:
				return mKeyNavigation.onKeyPrevious();

			case ApplicationPDA.KEY_CODE_BOLUS:
				return mKeyNavigation.onKeyConfirm();

			default:
				return super.onKeyUp(keyCode, event);
		}
	}


	@Override
	public void onClick(View v)
	{
		onClickView(v);
	}


	public ActivityPDA getActivity()
	{
		return (ActivityPDA)mContext;
	}


	public View getView()
	{
		return mView;
	}


	public void setView(int resId, int[] focusViewsId)
	{
		LayoutInflater inflater =
			(LayoutInflater)mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mView = inflater.inflate(resId, null);
		setContentView(mView);

		if (focusViewsId != null)
		{
			for (int i = 0; i < focusViewsId.length; i++)
			{
				View v = mView.findViewById(focusViewsId[i]);

				if (v instanceof WidgetButtonRepeating)
				{
					((WidgetButtonRepeating)v).setRepeatListener(
						mRepeatListener, 50);
				}
			}
		}

		mKeyNavigation.resetNavigation(getWindow().getDecorView());
	}


	public void setView(View view, Integer[] focusViewsId)
	{
		mView = view;
		setContentView(mView);

		if (focusViewsId != null)
		{
			for (int i = 0; i < focusViewsId.length; i++)
			{
				View v = mView.findViewById(focusViewsId[i]);

				if (v instanceof WidgetButtonRepeating)
				{
					((WidgetButtonRepeating)v).setRepeatListener(
						mRepeatListener, 50);
				}
			}
		}

		mKeyNavigation.resetNavigation(getWindow().getDecorView());
	}


	public void setDialogTrigger(final View view)
	{
		if (view != null)
		{
			mDialogTrigger = view;
		}
	}


	public void resetKeyNavigation()
	{
		mKeyNavigation.resetNavigation(getWindow().getDecorView());
	}


	protected void onClickView(View v)
	{
	}


	private RepeatListener mRepeatListener = new RepeatListener()
	{
		@Override
		public void onTouch(MotionEvent event)
		{
		}


		@Override
		public void onRepeat(View v, long duration, int repeatcount)
		{
			onClickView(v);
		}
	};
}