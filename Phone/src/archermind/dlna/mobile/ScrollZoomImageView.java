package archermind.dlna.mobile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

@SuppressLint({ "FloatMath", "FloatMath", "HandlerLeak", "DrawAllocation" })
public class ScrollZoomImageView extends ImageView {
	
	public static final String TAG = "ScrollZoomImageView";

	private boolean first = true;
	
	private double drawableLeft;
	private double drawableTop;
	private double drawableRight;
	private double drawableBottom;
	
	private float scale;
	private float minScale;
	private float maxScale;
	
	private double mLastSpace;

	private boolean pointFlag;
	private boolean scrollable = true;
	private boolean zoomable = true;
	private boolean moveing;
	private boolean scaling;

	private double mLastX;
	private double mLastY;

	private int dWidth;
	private int dHeight;
	private int vWidth;
	private int vHeight;
	
	private Handler handler = new Handler() {
		
		private Matrix matrix = new Matrix();
		private int delayMillis = 30;
		private float s = 1;
		private float ruleScale;
		private float dx = 0;
		private float dy = 0;

		@Override
		public void handleMessage(Message msg) {
			matrix.set(getImageMatrix());
			Log.e("ImageViewActivity","msg.what = " + msg.what);
			switch (msg.what) {
			case 1:
				ruleScale = scale;
				if (scale < minScale)
					ruleScale = minScale;
				if (scale > maxScale)
					ruleScale = maxScale;

				s = ruleScale / scale;

				if((int) (s * 100) != 100) {
					scaling = true;
					s = (float) Math.sqrt(Math.sqrt(s));
					delayMillis = 30;
					Log.e("ImageViewActivity","11111 s = " + s);
					drawablePostScale(matrix, s);
					handler.sendEmptyMessageDelayed(2, delayMillis);
				} else {
					delayMillis = 50;
					handler.sendEmptyMessage(3);
				}
				break;
			case 2:
				s = ruleScale / scale;
				Log.e("ImageViewActivity","22222 s = " + s);
				drawablePostScale(matrix, s);
				handler.sendEmptyMessageDelayed(3, delayMillis);
				scaling = false;
				break;
			case 3:
				move();
				if ((int) dx == 0 && (int) dy == 0) {
					break;
				}
				moveing = true;
				drawableTranslate(matrix, dx / 4, dy / 4);
				handler.sendEmptyMessageDelayed(4, delayMillis);
				break;
			case 4:
				move();
				drawableTranslate(matrix, dx, dy);
				dx = 0;
				dy = 0;
				moveing = false;
				break;
			}
		}

		private void move() {
			if (drawableLeft > 0) {
				dx = (float) (0 - drawableLeft);
			}
			if (drawableTop > 0) {
				dy = (float) (0 - drawableTop);
			}
			if (drawableRight < vWidth) {
				dx = (float) (vWidth - drawableRight);
			}
			if (drawableBottom < vHeight) {
				dy = (float) (vHeight - drawableBottom);
			}
		}

	};

	public ScrollZoomImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setScaleType(ScaleType.MATRIX);
	}

	public ScrollZoomImageView(Context context) {
		super(context);
		setScaleType(ScaleType.MATRIX);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		
		Matrix matrix = new Matrix();
		matrix.set(getImageMatrix());

		// single point touch
		float x = event.getX();
		float y = event.getY();

		// Multiple point touch
		float point1X = 0;
		float point1Y = 0;
		float point2X = 0;
		float point2Y = 0;
		double currentSpace = mLastSpace;
		if (event.getPointerCount() > 1) {
			point1X = event.getX(0);
			point1Y = event.getY(0);
			point2X = event.getX(1);
			point2Y = event.getY(1);
			currentSpace = Math.hypot(point2X - point1X, point2Y - point1Y);
		}
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			mLastX = x;
			mLastY = y;
			break;
		case MotionEvent.ACTION_MOVE:
			//Disruption of viewPager slip events
			getParent().requestDisallowInterceptTouchEvent(true);
			
			if (pointFlag) {
				if (zoomable && !scaling && !moveing) {
					float scale = (float) (currentSpace / mLastSpace);
					
					// To prevent scaling ratio is too large
					if (scale > 1.2) {
						scale = 1.2f;
					}
					if (scale < 0.8) {
						scale = 0.8f;
					}
					
					// Scale range
					if (this.scale * scale < minScale * 0.7 && scale < 1) {
						scale = 1;
					}
					if (this.scale * scale > maxScale * 1.3 && scale > 1) {
						scale = 1;
					}
					mLastSpace = currentSpace;
					Log.e("ImageViewActivity", "11111 scale = " + scale);
					drawablePostScale(matrix, scale);
				}
			} else {
				if (scrollable && !scaling && !moveing) {
					float deltaX = (float) (x - mLastX);
					float deltaY = (float) (y - mLastY);
					mLastX = x;
					mLastY = y;
					double space = Math.hypot(deltaX, deltaY);
					/**
					 * Prevents excess movement
					 */
					if (space > 50) {
						double arg = 50 / space;
						deltaX *= arg;
						deltaY *= arg;
					}
					if (drawableLeft > 0 && deltaX > 0) {
						getParent().requestDisallowInterceptTouchEvent(false);
						deltaX = 0;
					}
					if (drawableTop > 0 && deltaY > 0) {
						deltaY = 0;
					}
					if (vWidth - drawableRight > 0 && deltaX < 0) {
						getParent().requestDisallowInterceptTouchEvent(false);
						deltaX = 0;
					}
					if (vHeight - drawableBottom > 0 && deltaY < 0) {
						deltaY = 0;
					}
					drawableTranslate(matrix, deltaX, deltaY);
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			pointFlag = false;
			handler.sendEmptyMessageDelayed(1, 30);
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			pointFlag = true;
			mLastSpace = currentSpace;
			break;
		case MotionEvent.ACTION_POINTER_UP:
			break;
		}

		return true;
	}

	/**
	 * matrix.postScale(scale, scale,vWidth*0.5f,vHeight*0.5f); 以View 中心为中点进行缩放
	 * 
	 * @param matrix
	 * @param scale
	 */
	private synchronized void drawablePostScale(Matrix matrix, float scale) {

		int scaleX = (int) (vWidth * 0.5f);
		int scaleY = (int) (vHeight * 0.5f);
		matrix.postScale(scale, scale, scaleX, scaleY);
		this.scale *= scale;
		Log.e("ImageViewActivity", "22222  scale = " + scale);
		drawableLeft = (drawableLeft - scaleX) * scale + scaleX;
		drawableTop = (drawableTop - scaleY) * scale + scaleY;
		drawableRight = drawableLeft + this.scale * dWidth;
		drawableBottom = drawableTop + this.scale * dHeight;
		
		setImageMatrix(matrix);
	}

	/**
	 * matrix.postTranslate(deltaX, deltaY);
	 * 
	 * @param matrix
	 * @param deltaX
	 * @param deltaY
	 */
	private synchronized void drawableTranslate(Matrix matrix, float deltaX,
			float deltaY) {

		matrix.postTranslate(deltaX, deltaY);
		drawableLeft += deltaX ;
		drawableTop += deltaY ;
		drawableRight = drawableLeft + scale * dWidth;
		drawableBottom = drawableTop + scale * dHeight;

		setImageMatrix(matrix);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (first) {
			first = false;
			centerCrop();
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		first = true;
		super.onSizeChanged(w, h, oldw, oldh);
	}

	private void centerCrop() {
		Bitmap bm = getImageBitmap();
		if (bm != null) {
			Matrix matrix = new Matrix();
			matrix.set(getImageMatrix());

			vWidth = getWidth() - getPaddingLeft() - getPaddingRight();
			vHeight = getHeight() - getPaddingTop() - getPaddingBottom();
			dWidth = bm.getWidth();
			dHeight = bm.getHeight();

			Log.e("ImageViewActivity", "vWidth = " + vWidth + "  vHeight = " + vHeight);
			Log.e("ImageViewActivity", "dWidth = " + dWidth + "  dHeight = " + dHeight);
			
			float dx = 0, dy = 0;

			if (dWidth * vHeight > vWidth * dHeight) {
				scale = (float) vHeight / (float) dHeight;
				dx = (vWidth - dWidth * scale) * 0.5f;
			} else {
				scale = (float) vWidth / (float) dWidth;
				dy = (vHeight - dHeight * scale) * 0.5f;
			}
			setMinScale(scale);
			matrix.setScale(scale, scale);
			matrix.postTranslate(dx, dy);
			drawableLeft = dx;
			drawableTop = dy;
			drawableRight = drawableLeft + scale * dWidth;
			drawableBottom = drawableTop + scale * dHeight;

			setImageMatrix(matrix);
		} else {
			first = true;
		}

	}
	
	public void setMinScale(float scale) {
		minScale = scale;
		maxScale = scale * 4;
	}
	
	public void setMaxScale(float scale) {
		maxScale = scale;
	}

	private Bitmap getImageBitmap() {
		Drawable drawable = getDrawable();
		BitmapDrawable bd = (BitmapDrawable) drawable;
		if (bd != null)
			return bd.getBitmap();
		return null;
	}

	public void setScrollable(boolean scrollable) {
		this.scrollable = scrollable;
	}

	public void setZoomable(boolean zoomable) {
		this.zoomable = zoomable;
	}

}
