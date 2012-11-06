package archermind.dlna.verticalseekbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsSeekBar;

/**
 * A Verticalseekbar is an extension of ProgressBar that adds a draggable
 * thumb. The user can touch the thumb and drag left or right to set the current
 * progress level or use the arrow keys. Placing focusable widgets to the left
 * or right of a Verticalseekbar is discouraged.
 * <p>
 * Clients of the Verticalseekbar can attach a
 * {@link VerticalSeekBar.OnSeekBarChangeListener} to be notified of the
 * user's actions.
 * 
 * @attr ref android.R.styleable#SeekBar_thumb
 */
public class VerticalSeekBar extends AbsSeekBar {
	private Drawable mThumb;
	private int height;
	private int width;

	public VerticalSeekBar(Context context) {
		this(context, null);
	}

	public VerticalSeekBar(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.seekBarStyle);
	}

	public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public interface OnSeekBarChangeListener {
		void onProgressChanged(VerticalSeekBar Verticalseekbar,
				int progress, boolean fromUser);

		void onStartTrackingTouch(VerticalSeekBar Verticalseekbar);

		void onStopTrackingTouch(VerticalSeekBar Verticalseekbar);
	}

	private OnSeekBarChangeListener mOnSeekBarChangeListener;

	public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {

		mOnSeekBarChangeListener = l;

	}

	void onStartTrackingTouch() {
		if (mOnSeekBarChangeListener != null) {
			mOnSeekBarChangeListener.onStartTrackingTouch(this);
		}
	}

	void onStopTrackingTouch() {
		if (mOnSeekBarChangeListener != null) {
			mOnSeekBarChangeListener.onStopTrackingTouch(this);
		}
	}

	void onProgressRefresh(float scale, boolean fromUser) {
		Drawable thumb = mThumb;
		if (thumb != null) {
			setThumbPos(getHeight(), thumb, scale, Integer.MIN_VALUE);
			invalidate();
		}
		if (mOnSeekBarChangeListener != null) {
			mOnSeekBarChangeListener.onProgressChanged(this, getProgress(),
					fromUser);
		}
	}

	protected synchronized void onMeasure(int widthMeasureSpec,
			int heightMeasureSpec) {
		height = View.MeasureSpec.getSize(heightMeasureSpec);
		width = View.MeasureSpec.getSize(widthMeasureSpec);
		this.setMeasuredDimension(height, width);

	}


	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(h, w, oldw, oldh);
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			setPressed(true);
			onStartTrackingTouch();
			trackTouchEvent(event);
			break;

		case MotionEvent.ACTION_MOVE:
			trackTouchEvent(event);
			attemptClaimDrag();
			break;

		case MotionEvent.ACTION_UP:
			trackTouchEvent(event);
			onStopTrackingTouch();
			setPressed(false);
			break;

		case MotionEvent.ACTION_CANCEL:
			onStopTrackingTouch();
			setPressed(false);
			break;
		}
		return true;
	}

	private void attemptClaimDrag() {
		if (getParent() != null) {
			getParent().requestDisallowInterceptTouchEvent(true);
		}
	}

	@Override
	protected synchronized void onDraw(Canvas canvas) {

		canvas.rotate(-90);

		canvas.translate(-this.getHeight(), 0);
		super.onDraw(canvas);
	}

	private void trackTouchEvent(MotionEvent event) {
		final int height = getHeight();
		final int available = height - this.getPaddingBottom()
				- this.getPaddingTop();
		int y = (int) event.getY();
		float scale;
		float progress = 0;
		if (y > height - getPaddingBottom()) {
			scale = 0.0f;
		} else if (y < getPaddingTop()) {
			scale = 1.0f;
		} else {
			scale = (float) (height - getPaddingBottom() - y)
					/ (float) available;
		}
		final int max = getMax();
		progress += scale * max;

		this.setProgress((int) progress);
		// setThumbPos(getHeight(), mThumb, scale, 0) ;
	}

	private void setThumbPos(int w, Drawable thumb, float scale, int gap) {
		int available = w + getPaddingLeft() - getPaddingRight();
		int thumbWidth = thumb.getIntrinsicWidth();
		int thumbHeight = thumb.getIntrinsicHeight();
		available -= thumbWidth;
		// The extra space for the thumb to move on the track
		available += getThumbOffset() * 2;
		int thumbPos = (int) (scale * available);
		int topBound, bottomBound;
		if (gap == Integer.MIN_VALUE) {
			Rect oldBounds = thumb.getBounds();
			topBound = oldBounds.top;
			bottomBound = oldBounds.bottom;
		} else {
			topBound = gap;
			bottomBound = gap + thumbHeight;
		}
		thumb.setBounds(thumbPos, topBound, thumbPos + thumbWidth, bottomBound);
	}

	@Override
	public void setThumb(Drawable thumb) {
		mThumb = thumb;
		super.setThumb(thumb);
	}

	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			KeyEvent newEvent = null;
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_DPAD_UP:
				newEvent = new KeyEvent(KeyEvent.ACTION_DOWN,
						KeyEvent.KEYCODE_DPAD_RIGHT);
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
				newEvent = new KeyEvent(KeyEvent.ACTION_DOWN,
						KeyEvent.KEYCODE_DPAD_LEFT);
				break;
			case KeyEvent.KEYCODE_DPAD_LEFT:
				newEvent = new KeyEvent(KeyEvent.ACTION_DOWN,
						KeyEvent.KEYCODE_DPAD_DOWN);
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				newEvent = new KeyEvent(KeyEvent.ACTION_DOWN,
						KeyEvent.KEYCODE_DPAD_UP);
				break;
			default:
				newEvent = new KeyEvent(KeyEvent.ACTION_DOWN,
						event.getKeyCode());
				break;
			}
			return newEvent.dispatch(this);
		}
		return false;
	}
}
