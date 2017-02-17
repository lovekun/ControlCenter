package biye.controlcenter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * 
 * TestArcSeekBar
 * 
 * @author jay.jiang
 */
public class ArcSeekBar extends LinearLayout {

	private Context mContext;
	// 定义三个画笔，分辨表示弧，弧上圆点，已划过的弧
	private Paint drawArc;
	private Paint drawDot;
	private Paint drawArcShaper;
	// 定义弧的圆心
	private CenterPoint centerpoint;
	// 圆弧对应的rect的到边界的内边距
	// private int mPadding = 75;
	// 圆弧的半径
	private int arc_radius;
	private RectF rectF;
	// 滑动点在滑动条上的位置，按百分比来算0-100
	private float mDotPosition = 0;
	// 滑动点的半径
	private int mDotRadius = 50;
	// 滑动点的x,y坐标，以及滑动过的角度
	private float mThumbX;
	private float mThumbY;
	private double thumbangle;
	// 圆弧开始和经过的角度，三点钟的角度是0，按顺时针
	private final static float START_ARC = 180;
	private final static float END_ARC = 90;
	private int mDotColor;
	// 当前点到y轴负方向的角度
	private double angle;

	public ArcSeekBar(Context context) {
		super(context);
		this.mContext = context;
		initPaint();
	}

	public ArcSeekBar(Context context, AttributeSet set) {
		super(context, set);
		this.mContext = context;
		initPaint();
	}

	@Deprecated
	private ArcSeekBar(Context context, AttributeSet set, float viewsize) {
		super(context, set);
		// this.mContext = context;
		// view = (int) viewsize;
		// initPaint();
	}

	@SuppressLint("NewApi")
	public ArcSeekBar(Context context, AttributeSet set, int defStyle) {
		super(context, set, defStyle);
		this.mContext = context;
		initPaint();
	}

	// use Paint in this
	private void initPaint() {
		setBackgroundColor(Color.TRANSPARENT);
		setGravity(Gravity.CENTER);

		// all Paint will be init and must be setAntiAlias
		drawArc = new Paint();
		drawArc.setAntiAlias(true);
		drawArcShaper = new Paint();
		drawArcShaper.setAntiAlias(true);
		drawDot = new Paint();
		drawDot.setAntiAlias(true);
	}

	/**
	 * setDotCircleRadius
	 * 
	 * @param radius
	 */
	public void setDotCircleRadius(int radius) {
		this.mDotRadius = radius;
	}

	/**
	 * setArcStrokeWidth
	 * 
	 * @param px
	 * @param isNeed
	 */
	public void setArcStrokeWidth(int px, boolean isNeed) {
		if (isNeed) {
			drawArc.setStyle(Paint.Style.STROKE);
			drawArc.setStrokeWidth(px);
			drawArcShaper.setStyle(Paint.Style.STROKE);
			drawArcShaper.setStrokeWidth(px + 2);
		}
	}

	/**
	 * setDotColorBackground
	 * 
	 * @param color
	 */
	public void setDotColorBackground(int color) {
		this.mDotColor = color;
		drawDot.setColor(color);
	}

	public void setArc_radius(int arc_radius) {
		this.arc_radius = arc_radius;
	}

	/**
	 * setDotPosition
	 * 
	 * @param position
	 */
	public void setDotPosition(float position) {
		if (position > 100) {
			position = 100;
		}
		if (position < 0) {
			position = 0;
		}
		this.mDotPosition = position;
		initDataforThumb();
	}

	/**
	 * setArcColor
	 * 
	 * @param color
	 */
	public void setArcColorBackground(int color) {
		drawArc.setColor(color);
	}

	public void setArcShaperColorBackground(int color) {
		drawArcShaper.setColor(color);
	}

	/**
	 * setCenterPoint
	 * 
	 * @param point
	 */
	public void setCenterPoint(CenterPoint point) {
		this.centerpoint = point;
	}

	/**
	 * do OnDraw(Canvas canvas)
	 */
	public void doDraw() {
		rectF = new RectF();
		rectF.top = centerpoint.y - arc_radius;
		rectF.left = centerpoint.x - arc_radius;
		rectF.right = centerpoint.x + arc_radius;
		rectF.bottom = centerpoint.y + arc_radius;
		invalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			if (isTouchThumb(event.getX(), event.getY())) {
				drawDot.setColor(Color.BLUE);
				return true;
			}
			return false;
		case MotionEvent.ACTION_MOVE:
			move2radius(event.getX(), event.getY());
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			drawDot.setColor(mDotColor);
			invalidate();
			break;
		}
		return super.onTouchEvent(event);
	}

	// initDataforThumb move Dot to x y
	private void initDataforThumb() {
		double proportion = (double) (END_ARC / 100f);
		thumbangle = proportion * mDotPosition;
		angle = thumbangle;
		getPoint(thumbangle);
	}

	// get Quadrant return is DotCircle circle dot
	private float[] thumbsxy(double thumbangle) {
		float a[] = new float[2];
		float arcradius = arc_radius;
		// a[0]代表y轴距离值，a[1]代表x轴距离值
		a[0] = arcradius * (float) Math.sin(Math.toRadians(thumbangle));
		a[1] = arcradius * (float) Math.cos(Math.toRadians(thumbangle));
		return a;
	}

	// move point to radius
	private void move2radius(float x, float y) {
		angle = (double) buildingradius(x, y);
		postionAngle(angle);
		if (angle > END_ARC) {
			angle = END_ARC;
		}
		if (angle < 0) {
			angle = 0;
		}

		postionAngle(angle);
		getPoint(angle);
	}

	// get Point and set Thumb to ARC
	private void getPoint(double thumbangle) {
		float xy[] = thumbsxy(thumbangle);
		mThumbX = (float) centerpoint.x - Math.abs(xy[1]);
		mThumbY = (float) centerpoint.y - Math.abs(xy[0]);

		invalidate();
	}

	// get postion
	private void postionAngle(double angle) {
		double mangle = angle;
		double proportion = (double) (END_ARC / 100f);
		mDotPosition = (float) (mangle / proportion);
		if (listener != null) {
			listener.onMove(mDotPosition);
		}
	}

	// Get the angle between two points
	private double buildingradius(double x, double y) {
		double temx = Math.abs(x - centerpoint.x);
		double temy = Math.abs(y - centerpoint.y);
		double angle = Math.atan2(temy, temx) * 180 / Math.PI;
		return angle;
	}

	// This method can add something
	private boolean isTouchThumb(float x, float y) {
		if (inside(x, y)) {
			return true;
		}
		return false;
	}

	// if you touch the Thumb can return true else return false
	private boolean inside(float x, float y) {
		Rect r = new Rect((int) (mThumbX - mDotRadius),
				(int) (mThumbY - mDotRadius), (int) (mThumbX + mDotRadius),
				(int) (mThumbY + mDotRadius));
		if (r.contains((int) x, (int) y)) {
			return true;
		}
		return false;
	}

	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.drawArc(rectF, START_ARC, END_ARC, false, drawArc);
		canvas.save();
		drawArcShaper.setColor(Color.RED);
		canvas.drawArc(rectF, START_ARC, (float) angle, false, drawArcShaper);
		canvas.drawCircle(mThumbX, mThumbY, mDotRadius, drawDot);
		canvas.restore();
		super.onDraw(canvas);
	}

	/**
	 * 
	 * CenterPoint
	 * 
	 * @author jay
	 */
	public static class CenterPoint {
		int x;
		int y;
	}

	public interface OnSeekMoveListener {
		public void onMove(float f);
	}

	private OnSeekMoveListener listener;

	/**
	 * setOnSeekMoveListener
	 * 
	 * @param listener
	 */
	public void setOnSeekMoveListener(OnSeekMoveListener listener) {
		this.listener = listener;
	}

}
