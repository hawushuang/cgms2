package com.microtechmd.pda.ui.widget;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;


public class LockScreenView extends ImageView
{

	public static final int E_DOWN = 1;
	public static final int E_MOVE = 2;
	public static final int E_UP = 3;
	public static final int E_CANCEL = 4;

	private boolean mDown;
	private float mDownX;
	private float mMoveX;

	private UnlockListener mUnlockListener;


	public interface UnlockListener
	{
		void unlock();
	}


	/**
	 * �������������»����λ��
	 * 
	 * @author qian.zhou
	 * @version 1.1.1, 2012-12-25
	 * @since 1.1.1, 2012-12-25
	 * @param completePercent
	 *            ���黬���İٷֱȣ�ֻ�е�E_Action == E_Moveʱ�ò�����Ч����Чʱ���鴫��-1
	 * @param E_Action
	 *            ���ڱ�ʾ��Ϊ����MotionEvent��ACTION_XXX��Ӧ
	 */
	public void setUnlockPercent(float completePercent, int E_Action)
	{
		int eventAction = -1;
		if (completePercent > 1.2)
		{
			return;
		}
		float x = 400 * completePercent + 4;
		switch (E_Action)
		{
			case E_DOWN:
				eventAction = MotionEvent.ACTION_DOWN;
				onTouchDeal(4f, -1f, eventAction);
				break;
			case E_MOVE:
				eventAction = MotionEvent.ACTION_MOVE;
				onTouchDeal(x, 40f, eventAction);
				break;
			case E_UP:
				eventAction = MotionEvent.ACTION_UP;
				onTouchDeal(-1f, -1f, eventAction);
				break;
			case E_CANCEL:
				eventAction = MotionEvent.ACTION_CANCEL;
				onTouchDeal(-1f, -1f, eventAction);
				break;
		}
	}


	public LockScreenView(Context context)
	{
		this(context, null);
	}


	public LockScreenView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}


	public LockScreenView(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}


	/**
	 * onTouchEvent�¼��ľ��崦�����
	 * 
	 * @author qian.zhou
	 * @version 1.1.1, 2012-12-25
	 * @since 1.1.1, 2012-12-25
	 * @param x
	 *            Touch�¼��е�x���ֵ
	 * @param y
	 *            Touch�¼��е�y���ֵ
	 * @param eventAction
	 *            Touch�¼��е�Touch����
	 */
	private void onTouchDeal(float x, float y, int eventAction)
	{
		Drawable drawable = getDrawable();
		int drawWidth = drawable.getIntrinsicWidth();
		switch (eventAction)
		{
			case MotionEvent.ACTION_DOWN:
			{
				if (x < drawWidth + getPaddingLeft())
				{
					mDown = true;
					mDownX = x;
				}
				else
				{
					mDown = false;
				}
				break;
			}
			case MotionEvent.ACTION_MOVE:
			{
				if (!mDown)
					break;
				if (mMoveX >= getWidth() - getPaddingLeft() - getPaddingRight()
					- drawWidth - 5)
					break;
				mMoveX = x - mDownX;
				mMoveX = mMoveX < 0 ? 0 : mMoveX;
				invalidate();
				break;
			}
			case MotionEvent.ACTION_UP:
			{
				mDown = false;
				if (mMoveX >= (getWidth() - getPaddingLeft() - getPaddingRight()
					- drawWidth - 8) / 2)
				{
					if (mUnlockListener != null)
					{
						mUnlockListener.unlock();
					}
				}
				else
				{
					mMoveX = 0;
					invalidate();
				}
				break;
			}
			case MotionEvent.ACTION_CANCEL:
			{
				mDown = false;
				mMoveX = 0;
				invalidate();
				break;
			}
		}
	}


	/**
	 * Touch�¼��Ļص�������崦����onTouchDeal������
	 * 
	 * @author qian.zhou
	 * @version 1.1.1, 2012-12-25
	 * @since origin
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		float x = event.getX();
		float y = event.getY();
		onTouchDeal(x, y, event.getAction());
		return true;
	}


	@Override
	protected void onDraw(Canvas canvas)
	{
		int width = getWidth();
		int height = getHeight();

		Drawable bgDrawable = getBackground();
		if (bgDrawable != null)
		{
			bgDrawable.setBounds(0, 0, width, height);
			bgDrawable.draw(canvas);
		}

		int padingLeft = getPaddingLeft();
		width -= padingLeft;
		width -= getPaddingRight();
		height -= getPaddingTop();
		height -= getPaddingBottom();

		Drawable drawable = getDrawable();
		if (drawable != null)
		{
			if (mMoveX > getWidth() - getPaddingLeft() - getPaddingRight()
				- drawable.getIntrinsicWidth())
			{
				mMoveX = getWidth() - getPaddingLeft() - getPaddingRight()
					- drawable.getIntrinsicWidth();
			}
			drawable.setBounds(padingLeft + (int) mMoveX, getPaddingTop(),
				padingLeft + drawable.getIntrinsicWidth() + (int) mMoveX,
				getPaddingTop() + drawable.getIntrinsicHeight());
			canvas.save();
			canvas.translate(padingLeft, getPaddingTop());
			drawable.draw(canvas);
			canvas.restore();
		}
	}


	public UnlockListener getUnlockListener()
	{
		return mUnlockListener;
	}


	public void setUnlockListener(UnlockListener unlockListener)
	{
		this.mUnlockListener = unlockListener;
	}

}
