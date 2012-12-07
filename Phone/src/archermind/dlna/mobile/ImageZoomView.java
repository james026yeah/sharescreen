package archermind.dlna.mobile;

import java.util.Observable;
import java.util.Observer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import archermind.dlna.mobile.ImageZoomState.ControlType;

/**
 * View capable of drawing an image at different zoom state levels
 */
public class ImageZoomView extends View implements Observer {

	/** Paint object used when drawing bitmap. */
	private final Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

	/** Rectangle used (and re-used) for cropping source image. */
	private final Rect mRectSrc = new Rect();

	/** Rectangle used (and re-used) for specifying drawing area on canvas. */
	private final Rect mRectDst = new Rect();

	/** The bitmap that we're zooming in, and drawing on the screen. */
	private Bitmap mBitmap;

	/** Pre-calculated aspect quotient. */
	private float mAspectQuotient;

	/** State of the zoom. */
	private ImageZoomState mState;
	
	/**
	 * Constructor
	 */
	public ImageZoomView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * Set image bitmap
	 * 
	 * @param bitmap
	 *            The bitmap to view and zoom into
	 */
	public void setImage(Bitmap bitmap) {
		mBitmap = bitmap;

		calculateAspectQuotient();

		invalidate();
	}

	/**
	 * Set object holding the zoom state that should be used
	 * 
	 * @param state
	 *            The zoom state
	 */
	public void setZoomState(ImageZoomState state) {
		if (mState != null) {
			mState.deleteObserver(this);
		}

		mState = state;
		mState.addObserver(this);

		invalidate();
	}

	private void calculateAspectQuotient() {
		if (mBitmap != null) {
			mAspectQuotient = (((float) mBitmap.getWidth()) / mBitmap.getHeight())
					/ (((float) getWidth()) / getHeight());
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mBitmap != null && mState != null) {
			final int vWidth = getWidth();
			final int vHeight = getHeight();
			final int bWidth = mBitmap.getWidth();
			final int bHeight = mBitmap.getHeight();
			
			final float panX = mState.getPanX();
			final float panY = mState.getPanY();
			final float zoomX = mState.getZoomX(mAspectQuotient) * vWidth / bWidth;
			final float zoomY = mState.getZoomY(mAspectQuotient) * vHeight / bHeight;

			// Setup source and destination rectangles
			mRectSrc.left = (int) (panX * bWidth - vWidth / (zoomX * 2));
			mRectSrc.top = (int) (panY * bHeight - vHeight / (zoomY * 2));
			mRectSrc.right = (int) (mRectSrc.left + vWidth / zoomX);
			mRectSrc.bottom = (int) (mRectSrc.top + vHeight / zoomY);
			mRectDst.left = getLeft();
			mRectDst.top = getTop();
			mRectDst.right = getRight();
			mRectDst.bottom = getBottom();

			if(mState.getControlType() == ControlType.ZOOM) {
				mState.setMaxPanX(1.0f);
				mState.setMinPanX(0.0f);
				mState.setMaxPanY(1.0f);
				mState.setMinPanY(0.0f);
			}
			
			// Adjust source rectangle so that it fits within the source image.
			if (mRectSrc.left < 0) {
				mRectDst.left += -mRectSrc.left * zoomX;
				mRectSrc.left = 0;
				if(mState.getControlType() == ControlType.PAN) {
					mState.setMinPanX(mState.getPanX());
				}
			}
			if (mRectSrc.right > bWidth) {
				mRectDst.right -= (mRectSrc.right - bWidth) * zoomX;
				mRectSrc.right = bWidth;
				if(mState.getControlType() == ControlType.PAN) {
					mState.setMaxPanX(mState.getPanX());
				}
			}
			if (mRectSrc.top < 0) {
				mRectDst.top += -mRectSrc.top * zoomY;
				mRectSrc.top = 0;
				if(mState.getControlType() == ControlType.PAN) {
					mState.setMinPanY(mState.getPanY());
				}
			}
			if (mRectSrc.bottom > bHeight) {
				mRectDst.bottom -= (mRectSrc.bottom - bHeight) * zoomY;
				mRectSrc.bottom = bHeight;
				if(mState.getControlType() == ControlType.PAN) {
					mState.setMaxPanY(mState.getPanY());
				}
			}

			canvas.drawBitmap(mBitmap, mRectSrc, mRectDst, mPaint);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		calculateAspectQuotient();
	}

	// implements Observer
	public void update(Observable observable, Object data) {
		invalidate();
	}

}
