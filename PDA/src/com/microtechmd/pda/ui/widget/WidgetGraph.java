package com.microtechmd.pda.ui.widget;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;


public class WidgetGraph extends View
{
	public static final int POSITION_LEFT = 0;
	public static final int POSITION_TOP = 1;
	public static final int POSITION_RIGHT = 2;
	public static final int POSITION_BOTTOM = 3;
	public static final int COUNT_POSITION = 4;

	private int mWidth = 0;
	private int mHeight = 0;
	private int mMargin[] = null;
	private Bitmap mBitmap = null;
	private Canvas mCanvas = null;
	private Paint mPaintBorder = null;
	private Paint mPaintBackground = null;
	private ArrayList<ArrayList<Coordinate>> mCoordinates = null;
	private ArrayList<Profile> mProfiles = null;
	private ArrayList<Grid> mGrids = null;
	private OnNewCanvasListener mOnNewCanvasListener = null;


	public interface OnNewCanvasListener
	{
		void onNewCanvas(View view, Canvas canvas, int width, int height);
	}


	public class Profile
	{
		public static final int STYLE_LINE = 0;
		public static final int STYLE_STEP = 1;
		public static final int STYLE_BAR = 2;
		public static final int COUNT_STYLE = 3;

		private int mStyle = STYLE_LINE;
		private ArrayList<PointF> mData = null;
		private RectF mRange = null;
		private Paint mPaint = null;
		private ArrayList<Drawable> mDrawables = null;


		public Profile()
		{
		}


		public void draw(Canvas canvas, Rect bounds)
		{
			if ((mData == null) || (mRange == null))
			{
				return;
			}

			canvas.save();
			canvas.clipRect(bounds);
			ArrayList<PointF> pointList = new ArrayList<PointF>();

			if ((mPaint != null) && (mData.size() > 0))
			{
				RectF boundsF = new RectF(bounds);
				PointF point = new PointF();

				if (mStyle == STYLE_BAR)
				{
					Rect boundsBar = new Rect();

					for (int i = 0; i < mData.size(); i += 2)
					{
						if (i + 1 >= mData.size())
						{
							break;
						}

						scalePoint(mData.get(i), point, mRange, boundsF);
						boundsBar.left = Math.round(point.x);
						boundsBar.top = Math.round(point.y);
						scalePoint(mData.get(i + 1), point, mRange, boundsF);
						boundsBar.right = Math.round(point.x);
						boundsBar.bottom = Math.round(point.y);

						if ((mDrawables != null) && (mDrawables.size() > 0))
						{
							int index = i;

							if (index >= mDrawables.size())
							{
								index = mDrawables.size() - 1;
							}

							mDrawables.get(index).setBounds(boundsBar);
							mDrawables.get(index).draw(canvas);
						}
						else
						{
							canvas.drawRect(boundsBar, mPaint);
						}
					}
				}
				else
				{
					Path path = new Path();
					PointF pointStep = new PointF();
					scalePoint(mData.get(0), point, mRange, boundsF);
					pointList.add(new PointF(point.x, point.y));
					path.moveTo(point.x, point.y);

					for (int i = 1; i < mData.size(); i++)
					{
						switch (mStyle)
						{
							case STYLE_LINE:
								scalePoint(mData.get(i), point, mRange, boundsF);
								pointList.add(new PointF(point.x, point.y));
								path.lineTo(point.x, point.y);
								break;

							case STYLE_STEP:
								pointStep.x = mData.get(i).x;
								pointStep.y = mData.get(i - 1).y;
								scalePoint(pointStep, point, mRange, boundsF);
								path.lineTo(point.x, point.y);
								pointStep.y = mData.get(i).y;
								scalePoint(pointStep, point, mRange, boundsF);
								path.lineTo(point.x, point.y);
								break;

							default:
								break;
						}
					}

					canvas.drawPath(path, mPaint);
				}
			}

			if ((mDrawables != null) && (mDrawables.size() > 0) &&
				(pointList.size() > 0))
			{
				Rect drawableBounds = new Rect();
				PointF point = null;

				for (int i = 0; i < pointList.size(); i++)
				{
					int index = i;

					if (index >= mDrawables.size())
					{
						index = mDrawables.size() - 1;
					}

					Drawable drawable = mDrawables.get(index);
					int width = drawable.getIntrinsicWidth();
					int height = drawable.getIntrinsicHeight();
					point = pointList.get(i);
					drawableBounds.left = (int)(point.x - ((float)width / 2));
					drawableBounds.top = (int)(point.y - ((float)height / 2));
					drawableBounds.right = drawableBounds.left + width;
					drawableBounds.bottom = drawableBounds.top + height;
					drawable.setBounds(drawableBounds);
					drawable.draw(canvas);
				}
			}

			canvas.restore();
		}


		public int getStyle()
		{
			return mStyle;
		}


		public ArrayList<PointF> getData()
		{
			return mData;
		}


		public RectF getRange()
		{
			return mRange;
		}


		public Paint getPaint()
		{
			return mPaint;
		}


		public ArrayList<Drawable> getDrawables()
		{
			return mDrawables;
		}


		public void setStyle(int style)
		{
			this.mStyle = style;
		}


		public void setData(ArrayList<PointF> data)
		{
			this.mData = data;
		}


		public void setRange(RectF range)
		{
			this.mRange = range;
		}


		public void setPaint(Paint paint)
		{
			this.mPaint = paint;
		}


		public void setDrawables(ArrayList<Drawable> drawables)
		{
			this.mDrawables = drawables;
		}
	}


	public class Grid
	{
		public static final int STYLE_HORIZONTAL = 0;
		public static final int STYLE_VERTICAL = 1;
		public static final int COUNT_STYLE = 2;

		private int mStyle = STYLE_HORIZONTAL;
		private int mDivision = 0;
		private int mOffset = 0;
		private Paint mPaint = null;


		public Grid()
		{
		}


		public void draw(Canvas canvas, Rect bounds)
		{
			if ((mPaint == null) || (mDivision < 2))
			{
				return;
			}

			canvas.save();
			canvas.clipRect(bounds);

			int step = 0;
			int position = 0;

			if (mStyle == STYLE_VERTICAL)
			{
				step = Math.round((float)bounds.width() / (float)mDivision);

				if (mOffset > 0)
				{
					position = bounds.left + mOffset;
				}
				else
				{
					position = bounds.left + step;
				}

				while (position < bounds.right - (mDivision / 2))
				{
					canvas.drawLine(position, bounds.top, position,
						bounds.bottom, mPaint);
					position += step;
				}
			}
			else
			{
				step = Math.round((float)bounds.height() / (float)mDivision);

				if (mOffset > 0)
				{
					position = bounds.bottom - mOffset;
				}
				else
				{
					position = bounds.bottom - step;
				}

				while (position > bounds.top + (mDivision / 2))
				{
					canvas.drawLine(bounds.left, position, bounds.right,
						position, mPaint);
					position -= step;
				}
			}

			canvas.restore();
		}


		public int getStyle()
		{
			return mStyle;
		}


		public int getDivision()
		{
			return mDivision;
		}


		public int getOffset()
		{
			return mOffset;
		}


		public Paint getPaint()
		{
			return mPaint;
		}


		public void setStyle(int style)
		{
			this.mStyle = style;
		}


		public void setDivision(int division)
		{
			this.mDivision = division;
		}


		public void setOffset(int offset)
		{
			this.mOffset = offset;
		}


		public void setPaint(Paint paint)
		{
			this.mPaint = paint;
		}
	}


	public class Coordinate
	{
		private int mPosition = POSITION_LEFT;
		private int mPadding = 0;
		private int mOffset = 0;
		private ArrayList<String> mTexts = null;
		private Paint mPaint = null;
		private Drawable mDrawable = null;


		public Coordinate()
		{
			mPaint = new Paint();
		}


		public void draw(Canvas canvas, Rect bounds)
		{
			if ((mTexts == null) || (bounds == null))
			{
				return;
			}

			if (mTexts.size() <= 0)
			{
				return;
			}

			if (mDrawable != null)
			{
				int height = mDrawable.getIntrinsicHeight();
				int width = mDrawable.getIntrinsicWidth();

				switch (mPosition)
				{
					case POSITION_LEFT:
						mDrawable.setBounds(bounds.right - width, bounds.top,
							bounds.right, bounds.bottom);
						break;

					case POSITION_TOP:
						mDrawable
							.setBounds(bounds.left, bounds.bottom - height,
								bounds.right, bounds.bottom);
						break;

					case POSITION_RIGHT:
						mDrawable.setBounds(bounds.left, bounds.top,
							bounds.left + width, bounds.bottom);
						break;

					case POSITION_BOTTOM:
						mDrawable.setBounds(bounds.left, bounds.top,
							bounds.right, bounds.top + height);
						break;

					default:
						break;
				}

				mDrawable.draw(canvas);
			}

			int originX = 0;
			int originY = 0;
			int step = 0;
			FontMetrics fontMetrics = mPaint.getFontMetrics();

			switch (mPosition)
			{
				case POSITION_LEFT:
				case POSITION_RIGHT:

					if (mTexts.size() > 1)
					{
						step =
							Math.round((float)bounds.height() /
								(float)(mTexts.size() - 1));
					}
					else
					{
						step = 0;
					}

					originY =
						(bounds.bottom - mOffset) +
							((int)(fontMetrics.bottom - fontMetrics.top) / 2) -
							(int)fontMetrics.bottom;

					if (mPosition == POSITION_LEFT)
					{
						originX = bounds.right - mPadding;
					}
					else
					{
						originX = bounds.left + mPadding;
					}

					break;

				case POSITION_TOP:
				case POSITION_BOTTOM:

					if (mTexts.size() > 1)
					{
						step =
							Math.round((float)bounds.width() /
								(float)(mTexts.size() - 1));
					}
					else
					{
						step = 0;
					}

					originX = bounds.left + mOffset;

					if (mPosition == POSITION_TOP)
					{
						originY =
							(bounds.bottom - mPadding) -
								(int)fontMetrics.bottom;
					}
					else
					{
						originY = bounds.top + mPadding - (int)fontMetrics.top;
					}

					break;

				default:
					break;
			}

			for (int i = 0; i < mTexts.size(); i++)
			{
				canvas.drawText(mTexts.get(i), originX, originY, mPaint);

				if ((mPosition == POSITION_LEFT) ||
					(mPosition == POSITION_RIGHT))
				{
					originY -= step;
				}
				else
				{
					originX += step;
				}
			}
		}


		public int getPosition()
		{
			return mPosition;
		}


		public int getPadding()
		{
			return mPadding;
		}


		public int getOffset()
		{
			return mOffset;
		}


		public ArrayList<String> getTexts()
		{
			return mTexts;
		}


		public Paint getPaint()
		{
			return mPaint;
		}


		public Drawable getDrawable()
		{
			return mDrawable;
		}


		public void setPosition(int position)
		{
			if (position < COUNT_POSITION)
			{
				this.mPosition = position;
			}
		}


		public void setPadding(int padding)
		{
			this.mPadding = padding;
		}


		public void setOffset(int offset)
		{
			this.mOffset = offset;
		}


		public void setTexts(ArrayList<String> texts)
		{
			this.mTexts = texts;
		}


		public void setPaint(Paint paint)
		{
			this.mPaint = paint;
		}


		public void setDrawable(Drawable drawable)
		{
			this.mDrawable = drawable;
		}
	}


	public WidgetGraph(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);

		if (mMargin == null)
		{
			mMargin = new int[COUNT_POSITION];
		}

		if (mCoordinates == null)
		{
			mCoordinates = new ArrayList<ArrayList<Coordinate>>();
		}

		for (int i = 0; i < COUNT_POSITION; i++)
		{
			mCoordinates.add(null);
			mMargin[i] = 0;
		}
	}


	public WidgetGraph(Context context, AttributeSet attrs)
	{
		this(context, attrs, 0);
	}


	public WidgetGraph(Context context)
	{
		this(context, null);
	}


	public static void scalePoint(PointF pointSource, PointF pointTarget,
		RectF rectSource, RectF rectTarget)
	{
		if (pointSource.x < rectSource.left)
		{
			pointTarget.x =
				rectTarget.left -
					((rectSource.left - pointSource.x) * rectTarget.width() / rectSource
						.width());
		}
		else if (pointSource.x > rectSource.right)
		{
			pointTarget.x =
				rectTarget.right +
					((pointSource.x - rectSource.right) * rectTarget.width() / rectSource
						.width());
		}
		else
		{
			pointTarget.x =
				rectTarget.left +
					((pointSource.x - rectSource.left) * rectTarget.width() / rectSource
						.width());
		}

		if (pointSource.y < rectSource.top)
		{
			pointTarget.y =
				rectTarget.top -
					((rectSource.top - pointSource.y) * rectTarget.height() / rectSource
						.height());
		}
		else if (pointSource.y > rectSource.bottom)
		{
			pointTarget.y =
				rectTarget.bottom +
					((pointSource.y - rectSource.bottom) * rectTarget.height() / rectSource
						.height());
		}
		else
		{
			pointTarget.y =
				rectTarget.top +
					((pointSource.y - rectSource.top) * rectTarget.height() / rectSource
						.height());
		}
	}


	public OnNewCanvasListener getOnNewCanvasListener()
	{
		return mOnNewCanvasListener;
	}


	public int getMargin(int position)
	{
		return mMargin[position];
	}


	public ArrayList<Coordinate> getCoordinates(int position)
	{
		if (position >= COUNT_POSITION)
		{
			return null;
		}

		return mCoordinates.get(position);
	}


	public ArrayList<Profile> getProfiles()
	{
		return mProfiles;
	}


	public ArrayList<Grid> getGrids()
	{
		return mGrids;
	}


	public Paint getPaintBorder()
	{
		return mPaintBorder;
	}


	public Paint getPaintBackground()
	{
		return mPaintBackground;
	}


	public Canvas getCanvas()
	{
		return mCanvas;
	}


	public void setOnNewCanvasListener(OnNewCanvasListener onNewCanvasListener)
	{
		this.mOnNewCanvasListener = onNewCanvasListener;
	}


	public void setMargin(int position, int margin)
	{
		if (position >= COUNT_POSITION)
		{
			return;
		}

		mMargin[position] = margin;
	}


	public void setCoordinates(int position, int margin,
		ArrayList<Coordinate> coordinates)
	{
		if (position >= COUNT_POSITION)
		{
			return;
		}

		mMargin[position] = margin;

		if (coordinates != null)
		{
			for (int i = 0; i < coordinates.size(); i++)
			{
				if (coordinates.get(i) != null)
				{
					coordinates.get(i).setPosition(position);
				}
			}
		}

		mCoordinates.set(position, coordinates);
	}


	public void setProfiles(ArrayList<Profile> profiles)
	{
		mProfiles = profiles;
	}


	public void setGrids(ArrayList<Grid> grids)
	{
		mGrids = grids;
	}


	public void setPaintBorder(Paint paint)
	{
		this.mPaintBorder = paint;
	}


	public void setPaintBackground(Paint paint)
	{
		this.mPaintBackground = paint;
	}


	public void setWidget(WidgetGraph widgetGraph)
	{
		for (int i = 0; i < COUNT_POSITION; i++)
		{
			mCoordinates.set(i, widgetGraph.getCoordinates(i));
			mMargin[i] = widgetGraph.getMargin(i);
		}

		mProfiles = widgetGraph.getProfiles();
		mGrids = widgetGraph.getGrids();
		mPaintBorder = widgetGraph.getPaintBorder();
		mPaintBackground = widgetGraph.getPaintBackground();
		mOnNewCanvasListener = widgetGraph.getOnNewCanvasListener();
	}


	public void updateGraph()
	{
		Rect bounds = new Rect();
		bounds.left = mMargin[POSITION_LEFT];
		bounds.top = mMargin[POSITION_TOP];
		bounds.right = mWidth - mMargin[POSITION_RIGHT];
		bounds.bottom = mHeight - mMargin[POSITION_BOTTOM];

		if (mCanvas != null)
		{
			Paint paint = new Paint();

			if (mPaintBackground != null)
			{
				paint = mPaintBackground;
			}
			else
			{
				paint = new Paint();
				paint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
			}

			mCanvas.drawRect(bounds, paint);

			if (mGrids != null)
			{
				for (int i = 0; i < mGrids.size(); i++)
				{
					mGrids.get(i).draw(mCanvas, bounds);
				}
			}

			if (mProfiles != null)
			{
				for (int i = 0; i < mProfiles.size(); i++)
				{
					mProfiles.get(i).draw(mCanvas, bounds);
				}
			}

			if (mPaintBorder != null)
			{
				mCanvas.drawRect(bounds, mPaintBorder);
			}
		}
	}


	public void updateWidget()
	{
		Rect boundsCoordinate;
		Rect boundsComment;

		if (mCanvas != null)
		{
			Paint paint = new Paint();
			paint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
			mCanvas.drawPaint(paint);
		}

		for (int i = 0; i < COUNT_POSITION; i++)
		{
			boundsCoordinate = new Rect();
			boundsComment = new Rect();

			switch (i)
			{
				case POSITION_LEFT:
					boundsCoordinate.left = 0;
					boundsCoordinate.top = mMargin[POSITION_TOP];
					boundsCoordinate.right =
						mMargin[POSITION_LEFT];
					boundsCoordinate.bottom =
						mHeight - mMargin[POSITION_BOTTOM];
					boundsComment.left = 0;
					boundsComment.top = boundsCoordinate.top;
					boundsComment.right = 0;
					boundsComment.bottom = boundsCoordinate.bottom;
					break;

				case POSITION_TOP:
					boundsCoordinate.left = mMargin[POSITION_LEFT];
					boundsCoordinate.top = 0;
					boundsCoordinate.right =
						mWidth - mMargin[POSITION_RIGHT];
					boundsCoordinate.bottom =
						mMargin[POSITION_TOP];
					boundsComment.left = boundsCoordinate.left;
					boundsComment.top = 0;
					boundsComment.right = boundsCoordinate.right;
					boundsComment.bottom = 0;
					break;

				case POSITION_RIGHT:
					boundsCoordinate.left =
						mWidth - mMargin[POSITION_RIGHT];
					boundsCoordinate.top = mMargin[POSITION_TOP];
					boundsCoordinate.right = mWidth;
					boundsCoordinate.bottom =
						mHeight - mMargin[POSITION_BOTTOM];
					boundsComment.left = mWidth;
					boundsComment.top = boundsCoordinate.top;
					boundsComment.right = mWidth;
					boundsComment.bottom = boundsCoordinate.bottom;
					break;

				case POSITION_BOTTOM:
					boundsCoordinate.left = mMargin[POSITION_LEFT];
					boundsCoordinate.top =
						mHeight - mMargin[POSITION_BOTTOM];
					boundsCoordinate.right =
						mWidth - mMargin[POSITION_RIGHT];
					boundsCoordinate.bottom = mHeight;
					boundsComment.left = boundsCoordinate.left;
					boundsComment.top = mHeight;
					boundsComment.right = boundsCoordinate.right;
					boundsComment.bottom = mHeight;
					break;

				default:
					break;
			}

			if ((mCoordinates.get(i) != null) && (mCanvas != null))
			{
				ArrayList<Coordinate> coordinate = mCoordinates.get(i);

				for (int j = 0; j < coordinate.size(); j++)
				{
					if (coordinate.get(j) != null)
					{
						coordinate.get(j).draw(mCanvas, boundsCoordinate);
					}
				}
			}
		}

		updateGraph();
	}


	@Override
	protected void onDraw(Canvas canvas)
	{
		super.onDraw(canvas);

		if (mBitmap != null)
		{
			canvas.drawBitmap(mBitmap, 0, 0, null);
		}
	}


	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);

		if ((mWidth != w) || (mHeight != h))
		{
			mWidth = w;
			mHeight = h;

			if ((w > 0) && (h > 0))
			{
				if (mBitmap != null)
				{
					if (!mBitmap.isRecycled())
					{
						mBitmap.recycle();
					}

					mBitmap = null;
				}

				mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

				if (mCanvas == null)
				{
					mCanvas = new Canvas();
				}

				mCanvas.setBitmap(mBitmap);
				updateWidget();

				if (mOnNewCanvasListener != null)
				{
					mOnNewCanvasListener.onNewCanvas(this, mCanvas, w, h);
				}
			}
		}
	}
}