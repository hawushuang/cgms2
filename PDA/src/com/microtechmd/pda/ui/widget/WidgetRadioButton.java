package com.microtechmd.pda.ui.widget;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatRadioButton;
import android.util.AttributeSet;


public class WidgetRadioButton extends AppCompatRadioButton
{

	public WidgetRadioButton(Context context)
	{
		super(context);
	}


	public WidgetRadioButton(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}


	public WidgetRadioButton(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}


	@Override
	protected void onDraw(Canvas canvas)
	{
		Drawable[] drawables = getCompoundDrawables();

		if (drawables != null)
		{
			Drawable drawable = drawables[0];
			float textWidth = getPaint().measureText(getText().toString());
			int drawablePadding = getCompoundDrawablePadding();
			int drawableWidth = 0;

			if (drawable != null)
			{
				drawableWidth = drawable.getIntrinsicWidth();
				float bodyWidth = textWidth + drawableWidth + drawablePadding;
				canvas.translate((getWidth() - bodyWidth) / 2, 0);
			}

			drawable = drawables[2];

			if (drawable != null)
			{
				drawableWidth = drawable.getIntrinsicWidth();
				float bodyWidth = textWidth + drawableWidth + drawablePadding;
				setPadding(0, 0, (int)(getWidth() - bodyWidth), 0);
				canvas.translate((getWidth() - bodyWidth) / 2, 0);
			}
		}

		super.onDraw(canvas);
	}
}
