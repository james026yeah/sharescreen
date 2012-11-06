package archermind.dlna.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

class ImageThumbnailItem extends ImageView {
	
	private Drawable mFrame;
	private Rect mFrameBounds = new Rect();
	
	public ImageThumbnailItem(Context context) {
		this(context, null);
	}
	
	public ImageThumbnailItem(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public ImageThumbnailItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		mFrame = getResources().getDrawable(R.drawable.image_thumbnail_item_bg);
		mFrame.setCallback(this);
	}
	
	@Override
	protected boolean verifyDrawable(Drawable who) {
		return super.verifyDrawable(who) || (who == mFrame);
	}
	
	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		if (mFrame != null) {
			int[] drawableState = getDrawableState();
			mFrame.setState(drawableState);
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		final Rect frameBounds = mFrameBounds;
		if (frameBounds.isEmpty()) {
			final int w = getWidth();
			final int h = getHeight();
			
			frameBounds.set(0, 0, w, h);
			mFrame.setBounds(frameBounds);
		}
		mFrame.draw(canvas);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		mFrameBounds.setEmpty();
	}
	
}
