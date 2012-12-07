package archermind.dlna.mobile;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import archermind.dlna.mobile.ImageZoomState.ControlType;

/**
 * Simple on touch listener for zoom view
 */
@SuppressLint({ "HandlerLeak", "HandlerLeak" })
public class ImageZoomListener implements OnTouchListener {

	/** X-coordinate of previously handled touch event */
	private float mX;

	/** Y-coordinate of previously handled touch event */
	private float mY;
	
	/** State being controlled by touch events */
	private ImageZoomState mState;

	private GestureDetector mGestureDetector;
	
	private static final int PREV_IMAGE = 0;
	private static final int NEXT_IMAGE = 1;
	private static final int CHANGE_IMAGE_TIME = 50;
	
	private ImageViewActivity mImageViewActivity;

	public void setmGestureDetector(GestureDetector gestureDetector) {
		mGestureDetector = gestureDetector;
	}

	/**
	 * Sets the zoom state that should be controlled
	 * 
	 * @param state
	 *            Zoom state
	 */
	public void setZoomState(ImageZoomState state) {
		mState = state;
	}

	public void setActivity(ImageViewActivity activity) {
		mImageViewActivity = activity;
	}
	
	// implements View.OnTouchListener
	public boolean onTouch(View v, MotionEvent event) {
		if (mGestureDetector != null && mGestureDetector.onTouchEvent(event)) {
			return true;
		}

		mState.setControlType(ControlType.PAN);
		
		final float x = event.getX();
		final float y = event.getY();

		final int action = event.getAction();
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				mX = x;
				mY = y;
				break;
	
			case MotionEvent.ACTION_MOVE:
	
				mHandler.removeMessages(PREV_IMAGE);
				mHandler.removeMessages(NEXT_IMAGE);
				
				final float dx = (x - mX) / v.getWidth();
				final float dy = (y - mY) / v.getHeight();
	
				// if (mControlType == ControlType.ZOOM) {
				// mState.setZoom(mState.getZoom() * (float) Math.pow(20, -dy));
				// mState.notifyObservers();
				// } else {
				// }
	
				float panX = mState.getPanX() - dx;
				float panY = mState.getPanY() - dy;
				
				if (panX < mState.getMinPanX()) {
					panX = mState.getMinPanX();
//					mHandler.sendEmptyMessageDelayed(PREV_IMAGE, CHANGE_IMAGE_TIME);
				}
				if (panX > mState.getMaxPanX()) {
					panX = mState.getMaxPanX();
//					mHandler.sendEmptyMessageDelayed(NEXT_IMAGE, CHANGE_IMAGE_TIME);
				}
				if (panY < mState.getMinPanY()) {
					panY = mState.getMinPanY();
				}
				if (panY > mState.getMaxPanY()) {
					panY = mState.getMaxPanY();
				}
				
				mState.setPanX(panX);
				mState.setPanY(panY);
				mState.notifyObservers();
				
				mX = x;
				mY = y;
				break;
		}
		return true;
	}

	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case PREV_IMAGE:
					if(mImageViewActivity != null) {
						mImageViewActivity.prevImage();
						mImageViewActivity = null;
					}
					break;
				case NEXT_IMAGE:
					if(mImageViewActivity != null) {
						mImageViewActivity.nextImage();
						mImageViewActivity = null;
					}
					break;
			}
		};
	};
	
}
