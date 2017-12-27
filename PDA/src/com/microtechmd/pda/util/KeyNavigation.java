package com.microtechmd.pda.util;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;


public class KeyNavigation
	implements View.OnClickListener
{
	// Field definition

	private final static int FOCUS_DIRECTION_PREVIOUS = 0;
	private final static int FOCUS_DIRECTION_NEXT = 1;

	private Context mContext = null;
	private View mRootView = null;
	private View mFocusView = null;
	private View mEditView = null;
	private OnClickViewListener mOnClickViewListener = null;


	// Inner class definition

	public interface OnClickViewListener
	{
		void onClick(View v);
	}


	// Method definition

	public KeyNavigation(Context context, OnClickViewListener listener)
	{
		mContext = context;
		mOnClickViewListener = listener;
	}


	public boolean onKeyNext()
	{
		switchViewFocus(FOCUS_DIRECTION_NEXT);
		return true;
	}


	public boolean onKeyPrevious()
	{
		switchViewFocus(FOCUS_DIRECTION_PREVIOUS);
		return true;
	}


	public boolean onKeyConfirm()
	{
		if (mFocusView != null)
		{
			if (mOnClickViewListener != null)
			{
				if (mFocusView instanceof CompoundButton)
				{
					((CompoundButton)mFocusView).toggle();

				}

				mOnClickViewListener.onClick(mFocusView);
			}
		}

		return true;
	}


	public void clearFocus()
	{
		if (mFocusView != null)
		{
			mFocusView.clearFocus();
		}
	}


	public void resetNavigation(View rootView)
	{
		if (!(rootView instanceof ViewGroup))
		{
			return;
		}

		mRootView = rootView;
		View firstView = findFirstView((ViewGroup)rootView);
		View lastView = recurViewTree((ViewGroup)rootView, firstView);

		if (lastView != null)
		{
			lastView.setNextFocusLeftId(firstView.getId());
			lastView.setNextFocusDownId(getParentViewID(firstView));
			firstView.setNextFocusRightId(lastView.getId());
			firstView.setNextFocusUpId(getParentViewID(lastView));
			mFocusView = lastView;
		}

		if (mFocusView != null)
		{
			if (!mFocusView.isInTouchMode())
			{
				mFocusView.requestFocusFromTouch();
			}
		}
	}


	@Override
	public void onClick(View v)
	{
		if ((v.getVisibility() == View.VISIBLE) && (v.isFocusable()) &&
			(v.isEnabled()))
		{
			mFocusView.clearFocus();

			if (v instanceof EditText)
			{
				mEditView = v;
				v.requestFocusFromTouch();
				showInputMethod();
			}
			else
			{
				hideInputMethod();
			}

			mFocusView = v;

			if (mOnClickViewListener != null)
			{
				mOnClickViewListener.onClick(v);
			}
		}
	}


	private View findFirstView(ViewGroup viewGroup)
	{
		View currentView = null;


		for (int i = 0; i < viewGroup.getChildCount(); i++)
		{
			currentView = viewGroup.getChildAt(i);

			if ((currentView.getId() != View.NO_ID) &&
				(currentView.getVisibility() == View.VISIBLE) &&
				(currentView.isFocusable()) && (currentView.isEnabled()))
			{
				break;
			}

			if ((currentView instanceof ViewGroup) &&
				(currentView.getVisibility() == View.VISIBLE) &&
				(currentView.isEnabled()))
			{
				currentView = findFirstView((ViewGroup)currentView);

				if (currentView != null)
				{
					if ((currentView.getId() != View.NO_ID) &&
						(currentView.getVisibility() == View.VISIBLE) &&
						(currentView.isFocusable()) &&
						(currentView.isEnabled()))
					{
						break;
					}
				}
			}
		}

		return currentView;
	}


	private View recurViewTree(ViewGroup viewGroup, View previousView)
	{
		View currentView = null;


		for (int i = 0; i < viewGroup.getChildCount(); i++)
		{
			currentView = viewGroup.getChildAt(i);

			if ((currentView.getId() != View.NO_ID) &&
				(currentView.getVisibility() == View.VISIBLE) &&
				(currentView.isFocusable()) && (currentView.isEnabled()) &&
				(!(currentView instanceof AdapterView)))
			{
				currentView.setOnClickListener(this);

				if (currentView instanceof EditText)
				{
					mEditView = currentView;
				}

				if (previousView != null)
				{
					previousView.setNextFocusLeftId(currentView.getId());
					previousView
						.setNextFocusDownId(getParentViewID(currentView));
					currentView.setNextFocusRightId(previousView.getId());
					currentView.setNextFocusUpId(getParentViewID(previousView));
				}

				previousView = currentView;
			}

			if ((currentView instanceof ViewGroup) &&
				(currentView.getVisibility() == View.VISIBLE) &&
				(currentView.isEnabled()))
			{
				previousView =
					recurViewTree((ViewGroup)currentView, previousView);
			}
		}

		return previousView;
	}


	private int getParentViewID(View view)
	{
		View viewParent = view;

		if (viewParent.getParent() instanceof View)
		{
			viewParent = (View)viewParent.getParent();

			while (viewParent.getId() == View.NO_ID)
			{
				if (viewParent.getParent() instanceof View)
				{
					viewParent = (View)viewParent.getParent();
				}
				else
				{
					break;
				}
			}
		}

		return viewParent.getId();
	}


	private void switchViewFocus(int focusDirection)
	{
		if ((mRootView != null) && (mFocusView != null))
		{
			hideInputMethod();
			mFocusView.clearFocus();

			int nextViewID;
			int nextParentViewID;

			switch (focusDirection)
			{
				case FOCUS_DIRECTION_PREVIOUS:
					nextViewID = mFocusView.getNextFocusRightId();
					nextParentViewID = mFocusView.getNextFocusUpId();
					break;

				case FOCUS_DIRECTION_NEXT:
					nextViewID = mFocusView.getNextFocusLeftId();
					nextParentViewID = mFocusView.getNextFocusDownId();
					break;

				default:
					nextViewID = View.NO_ID;
					nextParentViewID = View.NO_ID;
					break;
			}

			View nextView = mRootView.findViewById(nextParentViewID);

			if (nextView != null)
			{
				nextView = nextView.findViewById(nextViewID);

				if (nextView != null)
				{
					mFocusView = nextView;
				}
			}

			mFocusView.requestFocusFromTouch();
		}
	}


	private void showInputMethod()
	{
		if ((mContext != null) && (mEditView != null))
		{
			InputMethodManager inputMethodManager =
				(InputMethodManager)mContext
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.showSoftInput(mEditView, 0);
		}
	}


	private void hideInputMethod()
	{
		if ((mContext != null) && (mEditView != null))
		{
			InputMethodManager inputMethodManager =
				(InputMethodManager)mContext
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(
				mEditView.getWindowToken(), 0);
		}
	}
}
