package archermind.dlna.mobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.archermind.ashare.dlna.localmedia.PhotoItem;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import archermind.dlna.mobile.ImageZoomState.ControlType;

@SuppressLint({ "HandlerLeak", "HandlerLeak", "UseSparseArrays" })
public class ImageViewActivity extends BaseActivity implements OnClickListener {

	private static final String TAG = "ImageViewActivity";

	public static final String IMAGE_INDEX = "image_index";
	public static ArrayList<PhotoItem> sImageItemList = new ArrayList<PhotoItem>();
	
	private static final int IMAGE_SETTINGS_REQUEST = 0;
	
	private static final float SCALE_IN_VALUE = 1.25f;
	private static final float SCALE_OUT_VALUE = 0.8f;
	
	private static final int START_SLIDE = 0;
	private static final int END_SLIDE = 1;
	private static final int POST_PREV_IMAGE = 2;
	private static final int POST_NEXT_IMAGE = 3;
	
	private HashMap<Integer, ImageTag> mImageTagMaps = new HashMap<Integer, ImageTag>();
	
	private RelativeLayout mTopLayout;
	private LinearLayout mBottomLayout;

	private LinearLayout mCoverView;
	private RelativeLayout mListView;
	private RelativeLayout mPushView;
	private TextView mNameView;
	private LinearLayout mPrevView;
	private LinearLayout mRotateLeftView;
	private LinearLayout mRotateRightView;
	private LinearLayout mZoomInView;
	private LinearLayout mZoomOutView;
	private LinearLayout mNextView;

	private int mScreenWidth;
	private int mScreenHeight;
	private int mOldIndex;
	private int mCurrentIndex;
	private int mImageListMaxSize;
	private int mPostIntervalTime;
	private int mSlideIntervalTime;
	
	private float mRotateAngle;
	
	private boolean mIsShowZoomView;
	private boolean mIsShowCoverView;
	private boolean mIsPushed;
	
	private ImageZoomView mImageZoomView;
	private ImageZoomState mImageZoomState;
	private ImageZoomListener mImageZoomListener;
	private Bitmap mZoomBitmap;
	
	private ViewPager mViewPager;
	private ViewPagerAdapter mViewPagerAdapter;
	private Bitmap mViewPagerBitmap;

	private Timer mTimer;
	private SlideTask mSlideTask;
	private ImageLoadManager mImageLoadManager;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCurrentIndex = getIntent().getIntExtra(IMAGE_INDEX, 0);
		mOldIndex = mCurrentIndex;
		mImageListMaxSize = sImageItemList.size();

		setContentView(R.layout.local_media_image_view);
		getScreenWidth();
		
		mImageLoadManager = new ImageLoadManager();
		mImageLoadManager.setScreenWidth(mScreenWidth);
		mImageLoadManager.setScreenHeight(mScreenHeight);
		
		getSharedPreferenceValue();
		initImageView();
	}

	
	private void getScreenWidth() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		mScreenWidth = dm.widthPixels;
		mScreenHeight = dm.heightPixels;
	}
	
	
	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
	}
	
	
	@Override
	protected void onGetFriendlyName(String friendlyName) {
		if(friendlyName == null || friendlyName.equals("")) {
			Toast.makeText(this, R.string.video_not_connection_toast_message, Toast.LENGTH_SHORT).show();
		} else {
			pushImage();
		}
	}

	
	private void pushImage() {
		if (!mIsPushed) {
			log("push out to TV");
			log("current uri = " + sImageItemList.get(mCurrentIndex).getItemUri());
			mIsPushed = true;
			postPlay(sImageItemList.get(mCurrentIndex).getItemUri(), sImageItemList.get(mCurrentIndex).metaData);
		} else {
			log("push back to phone");
			mIsPushed = false;
			poststop();
		}
	}
	
	
	/**
	 * Initialization the image view layout
	 */
	private void initImageView() {

		mTopLayout = (RelativeLayout) findViewById(R.id.image_view_top_layout);
		mBottomLayout = (LinearLayout) findViewById(R.id.image_view_bottom_layout);
		mTopLayout.getBackground().setAlpha(180);
		mBottomLayout.getBackground().setAlpha(180);

		mListView = (RelativeLayout) findViewById(R.id.image_view_list);
		mPushView = (RelativeLayout) findViewById(R.id.image_view_push);
		mNameView = (TextView) findViewById(R.id.image_view_name);
		mPrevView = (LinearLayout) findViewById(R.id.image_view_prev);
		mRotateLeftView = (LinearLayout) findViewById(R.id.image_view_rotate_left);
		mRotateRightView = (LinearLayout) findViewById(R.id.image_view_rotate_right);
		mZoomInView = (LinearLayout) findViewById(R.id.image_view_zoom_in);
		mZoomOutView = (LinearLayout) findViewById(R.id.image_view_zoom_out);
		mNextView = (LinearLayout) findViewById(R.id.image_view_next);

		mListView.setOnClickListener(this);
		mPushView.setOnClickListener(this);
		mPrevView.setOnClickListener(this);
		mRotateLeftView.setOnClickListener(this);
		mRotateRightView.setOnClickListener(this);
		mZoomInView.setOnClickListener(this);
		mZoomOutView.setOnClickListener(this);
		mNextView.setOnClickListener(this);

		setImageName(mCurrentIndex);
		mViewPagerBitmap = createBitmap(mCurrentIndex);

		mViewPager = (ViewPager) findViewById(R.id.image_view_pager);
		mViewPagerAdapter = new ViewPagerAdapter(this);
		mViewPager.setAdapter(mViewPagerAdapter);
		mViewPager.setCurrentItem(mCurrentIndex);
		// the default loading page on each side of the current page,
		// so the all loading page is 3 or 4 or 5
		mViewPager.setOffscreenPageLimit(2);
		mViewPager.setOnPageChangeListener(pageChangeListener);
		
		mImageZoomView = (ImageZoomView) findViewById(R.id.zoomview);
		mImageZoomState = new ImageZoomState();
		mImageZoomListener = new ImageZoomListener();
		mImageZoomListener.setmGestureDetector(new GestureDetector(this, new MyGestureListener()));

		mImageZoomListener.setZoomState(mImageZoomState);
		mImageZoomView.setZoomState(mImageZoomState);
		mImageZoomView.setOnTouchListener(mImageZoomListener);

		mCoverView = (LinearLayout) findViewById(R.id.cover_view);
		mCoverView.setOnTouchListener(mImageZoomListener);
	}

	OnPageChangeListener pageChangeListener = new OnPageChangeListener() {
		
		@Override
		public void onPageSelected(int position) {
			mHandler.removeMessages(POST_PREV_IMAGE);
			mHandler.removeMessages(POST_NEXT_IMAGE);
			if(mOldIndex > position) {
				mOldIndex = mCurrentIndex;
				mCurrentIndex = position;
				mHandler.sendEmptyMessageDelayed(POST_PREV_IMAGE, mPostIntervalTime);
			} else {
				mOldIndex = mCurrentIndex;
				mCurrentIndex = position;
				mHandler.sendEmptyMessageDelayed(POST_NEXT_IMAGE, mPostIntervalTime);
			}
			setImageName(position);
			mViewPagerBitmap = null;
//			if(position == 0) {
//				Toast.makeText(ImageViewActivity.this, R.string.image_first_number_toast_message,
//						Toast.LENGTH_SHORT).show();
//			} else if(position == mImageListMaxSize - 1) {
//				Toast.makeText(ImageViewActivity.this, R.string.image_last_number_toast_message,
//						Toast.LENGTH_SHORT).show();
//			}
		}
		
		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}
		
		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
	};
	
	
	/**
	 * @param index
	 * 			the position of the current image
	 * 			set the current image name
	 */
	private void setImageName(int index) {
		mNameView.setText(sImageItemList.get(index).getTitle());
	}

	
	/**
	 * show the previous image if is exist
	 */
	public void prevImage() {
		if(mCurrentIndex == 0) {
			Toast.makeText(this, R.string.image_first_number_toast_message, Toast.LENGTH_SHORT).show();
		} else {
			if(mIsShowZoomView) {
				showViewPagerView();
			}
			mCurrentIndex--;
			setImageName(mCurrentIndex);
			mViewPager.setCurrentItem(mCurrentIndex);
		}
	}

	
	/**
	 * show the next image if is exist
	 */
	public void nextImage() {
		if(mCurrentIndex == mImageListMaxSize - 1) {
			Toast.makeText(this, R.string.image_last_number_toast_message, Toast.LENGTH_SHORT).show();
		} else {
			if(mIsShowZoomView) {
				showViewPagerView();
			}
			mOldIndex = mCurrentIndex;
			mCurrentIndex++;
			setImageName(mCurrentIndex);
			mViewPager.setCurrentItem(mCurrentIndex);
		}
	}

	
	/**
	 * show the image when rotate left
	 */
	private void rotateLeftImage() {
		ImageTag tag = mImageTagMaps.get(mCurrentIndex);
		if (tag == null || tag.getScale() == 0) {
			createBitmap(mCurrentIndex);
		}
		tag = mImageTagMaps.get(mCurrentIndex);
		if (mIsShowZoomView) {
			rotateZoomViewImage(tag, -mRotateAngle);
		} else {
			rotateViewPagerImage(tag, -mRotateAngle);
		}
		if (mIsPushed) {
			postFlipImage(tag.getRotateValue() + "");
		}

	}
	
	
	/**
	 * show the image when rotate right
	 */
	private void rotateRightImage() {
		ImageTag tag = mImageTagMaps.get(mCurrentIndex);
		if(tag == null || tag.getScale() == 0) {
			createBitmap(mCurrentIndex);
		}
		tag = mImageTagMaps.get(mCurrentIndex);
		if(mIsShowZoomView) {
			rotateZoomViewImage(tag, mRotateAngle);
		} else {
			rotateViewPagerImage(tag, mRotateAngle);
		}
		if(mIsPushed) {
			postFlipImage(tag.getRotateValue() + "");
		}
	}
	
	
	/**
	 * show the image when zoom in
	 */
	private void zoomInImage() {
		ImageTag tag = mImageTagMaps.get(mCurrentIndex);
		if (tag == null || tag.getScale() == 0) {
			createBitmap(mCurrentIndex);
		}
		tag = mImageTagMaps.get(mCurrentIndex);
		if (tag.getIsBigImage()) {
			zoomInBigImage(tag);
		} else {
			zoomInSmallImage(tag);
		}
		if (mIsPushed) {
			postScalingImage(tag.getScale() + "");
		}
	}
	
	
	/**
	 * show the image when zoom in
	 */
	private void zoomOutImage() {
		ImageTag tag = mImageTagMaps.get(mCurrentIndex);
		if (tag == null || tag.getScale() == 0) {
			createBitmap(mCurrentIndex);
		}
		tag = mImageTagMaps.get(mCurrentIndex);
		if (tag.getIsBigImage()) {
			zoomOutBigImage(tag);
		} else {
			zoomOutSmallImage(tag);
		}
		if (mIsPushed) {
			postScalingImage(tag.getScale() + "");
		}
	}
	
	
	/**
	 * zoom in the image which large than screen
	 */
	private void zoomInBigImage(ImageTag tag) {
		float scale = tag.getScale() * SCALE_IN_VALUE;
		if(scale > tag.getMaxScale()) {
			scale = tag.getMaxScale();
		}
		tag.setScale(scale);
		if(!mIsShowZoomView) {
			showZoomView();
		}
		if(mZoomBitmap == null) {
			mZoomBitmap = createBitmap(mCurrentIndex);
		}
		mImageZoomView.setImage(mZoomBitmap);
		mImageZoomState.setControlType(ControlType.ZOOM);
		mImageZoomState.setMaxScale(tag.getMaxScale());
		mImageZoomState.setMinScale(tag.getMinScale());
		mImageZoomState.setScale(scale);
		mImageZoomState.setPanX(0.5f);
		mImageZoomState.setPanY(0.5f);
		mImageZoomState.notifyObservers();
	}
	
	
	/**
	 * zoom out the image which large than screen
	 */
	private void zoomOutBigImage(ImageTag tag) {
		float scale = tag.getScale() * SCALE_OUT_VALUE;
		if(scale < tag.getMinScale()) {
			scale = tag.getMinScale();
		}
		tag.setScale(scale);
		if(!mIsShowZoomView) {
			showZoomView();
		}
		if(mZoomBitmap == null) {
			mZoomBitmap = createBitmap(mCurrentIndex);
		}
		mImageZoomView.setImage(mZoomBitmap);
		mImageZoomState.setControlType(ControlType.ZOOM);
		mImageZoomState.setScale(scale);
		mImageZoomState.setPanX(0.5f);
		mImageZoomState.setPanY(0.5f);
		mImageZoomState.notifyObservers();
	}
	
	
	/**
	 * zoom in the image which small than screen
	 */
	private void zoomInSmallImage(ImageTag tag) {
		float scale = tag.getScale() * SCALE_IN_VALUE;
		if(scale > tag.getMaxScale()) {
			scale = tag.getMaxScale();
		}
		tag.setScale(scale);
		mViewPagerBitmap = createBitmap(mCurrentIndex);
		mViewPagerAdapter.notifyDataSetChanged();
	}
	
	
	/**
	 * zoom out the image which small than screen
	 */
	private void zoomOutSmallImage(ImageTag tag) {
		float scale = tag.getScale() * SCALE_OUT_VALUE;
		if (scale < tag.getMinScale()) {
			scale = tag.getMinScale();
		}
		tag.setScale(scale);
		mViewPagerBitmap = createBitmap(mCurrentIndex);
		mViewPagerAdapter.notifyDataSetChanged();
	}
	
	
	/**
	 * @param rotateValue
	 * 		the rotate value
	 * 
	 * 		reset the new bitmap when rotate at zoom view
	 */
	private void rotateZoomViewImage(ImageTag tag, float rotateValue) {
		if(!mIsShowZoomView) {
			showZoomView();
		}
		tag.setRotateValue(tag.getRotateValue() + rotateValue);
		if(mZoomBitmap == null) {
			mZoomBitmap = createBitmap(mCurrentIndex);
		}
		Matrix matrix = new Matrix();
		matrix.setRotate(rotateValue);
		mZoomBitmap = Bitmap.createBitmap(mZoomBitmap, 0, 0, mZoomBitmap.getWidth(), 
				mZoomBitmap.getHeight(), matrix, true);
		mImageZoomView.setImage(mZoomBitmap);
		mImageZoomState.setControlType(ControlType.ZOOM);
		mImageZoomState.setScale(tag.getScale());
		mImageZoomState.setPanX(0.5f);
		mImageZoomState.setPanY(0.5f);
		mImageZoomState.notifyObservers();
	}
	
	
	/**
	 * @param rotateValue
	 * 		the rotate value
	 * 
	 * 		reset the new bitmap when rotate at viewPager view
	 */
	private void rotateViewPagerImage(ImageTag tag, float rotateValue) {
		tag.setRotateValue(tag.getRotateValue() + rotateValue);
		mViewPagerBitmap = createBitmap(mCurrentIndex);
		mViewPagerAdapter.notifyDataSetChanged();
	}

	
	/**
	 *  create the timer and slideTask of show slide
	 */
	private void createTimer() {
		mTimer = new Timer();
		mSlideTask = new SlideTask();
		mTimer.schedule(mSlideTask, mSlideIntervalTime * 1000, mSlideIntervalTime * 1000);
	}
	
	
	/**
	 * @author jian.he
	 * 
	 * 		send the message of start show slide
	 *
	 */
	private class SlideTask extends TimerTask {

		@Override
		public void run() {
			try {
				mHandler.sendEmptyMessage(START_SLIDE);
			} catch (Exception e) {
				mHandler.sendEmptyMessage(END_SLIDE);
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 *  cancel the timer and slideTask
	 */
	private void cancelTask() {
		if(mTimer != null) {
			mTimer.cancel();
		}
		if(mSlideTask != null) {
			mSlideTask.cancel();
		}
	}
	
	
	/**
	 * Receive the timer task messages, then refresh UI
	 */
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case START_SLIDE:
					if(mCurrentIndex == mImageListMaxSize - 1) {
						hideCoverView();
						return;
					}
					if(mCurrentIndex < sImageItemList.size() - 1) {
						mCurrentIndex ++;
						mViewPager.setCurrentItem(mCurrentIndex);
					}
					if(mIsPushed) {
						postNext(sImageItemList.get(mCurrentIndex).getItemUri(), sImageItemList.get(mCurrentIndex).metaData);
					}
					break;
				case END_SLIDE:
					hideCoverView();
					break;
				case POST_PREV_IMAGE:
					if(mIsPushed) {
						postPrevious(sImageItemList.get(mCurrentIndex).getItemUri(), sImageItemList.get(mCurrentIndex).metaData);
					}
					break;
				case POST_NEXT_IMAGE:
					if(mIsPushed) {
						postNext(sImageItemList.get(mCurrentIndex).getItemUri(), sImageItemList.get(mCurrentIndex).metaData);
					}
					break;
				}
		};
	};
	
	
	/**
	 * @param index
	 * 			the current image
	 * @return
	 */
	private Bitmap createBitmap(int index) {

		if (index >= 0 && index < sImageItemList.size()) {
			String filePath = sImageItemList.get(index).getFilePath();
			String thumbnailPath = sImageItemList.get(index).getThumbFilePath();
			
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(filePath, options);

			int scale = 1;
			while ((options.outWidth / scale > mScreenWidth * 2)
					|| (options.outHeight / scale > mScreenHeight * 2)) {
				scale *= 2;
			}
			options.inJustDecodeBounds = false;
			options.inSampleSize = scale;
			Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

			if (bitmap != null) {
				
				int width = bitmap.getWidth();
				int height = bitmap.getHeight();
				Matrix matrix = new Matrix();
				ImageTag tag = mImageTagMaps.get(mCurrentIndex);
				
				if(tag == null || tag.getScale() == 0) {
					/**
					 * Into the application for the first time or Currently displayed thumbnail_image
					 */
					tag = new ImageTag();
					tag.setFilePath(filePath);
					tag.setThumbnailPath(thumbnailPath);
					tag.setType(ImageTag.IMAGE_FULL);
					if (width > mScreenWidth || height > mScreenHeight) {
						tag.setIsBigImage(true);
						tag.setMaxScale(3.0f);
						tag.setMinScale(1.0f);
						tag.setScale(1.0f);
					} else {
						float scaleWidth = (float) mScreenWidth / (float) width;
						float scaleHeight = (float) mScreenHeight / (float) height;
						tag.setIsBigImage(false);
						tag.setMaxScale(Math.min(scaleWidth, scaleHeight));
						tag.setMinScale(1.0f);
						tag.setScale(1.0f);
					}
					mImageTagMaps.put(mCurrentIndex, tag);
				}
				matrix.setScale(tag.getScale(), tag.getScale());
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

				width = bitmap.getWidth();
				height = bitmap.getHeight();

				matrix = new Matrix();
				matrix.setRotate(tag.getRotateValue());
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

				return bitmap;
			}
		}
		return null;
	}
	
	
	/**
	 * hide the viewPager, show the zoom view
	 */
	private void showZoomView() {
		mIsShowZoomView = true;
		mViewPager.setVisibility(View.GONE);
		mImageZoomView.setVisibility(View.VISIBLE);
		mImageZoomListener.setActivity(this);
	}

	
	/**
	 * show the slide view, hide viewPager view
	 */
	private void showCoverView() {
		mIsShowCoverView = true;
		mCoverView.setVisibility(View.VISIBLE);
		createTimer();
		hideControlLayout();
		String value = getResources().getString(R.string.image_start_show_slide_before) + 
				" " + mSlideIntervalTime + " " + getResources().getString(R.string.image_start_show_slide_after);
		Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
		if(mIsPushed) {
			postNext(sImageItemList.get(mCurrentIndex).getItemUri(), sImageItemList.get(mCurrentIndex).metaData);
		}
	}


	/**
	 * end play slide , hide slide view , show viewPager view
	 */
	private void hideCoverView() {
		mIsShowCoverView = false;
		mCoverView.setVisibility(View.GONE);
		cancelTask();
		showControlLayout();
		Toast.makeText(this, R.string.image_end_show_slide, Toast.LENGTH_SHORT).show();
	}


	/**
	 * show the viewPager, hide the zoom view
	 */
	private void showViewPagerView() {
		mIsShowZoomView = false;
		mTopLayout.setVisibility(View.VISIBLE);
		mBottomLayout.setVisibility(View.VISIBLE);
		mViewPager.setVisibility(View.VISIBLE);
		mImageZoomView.setVisibility(View.GONE);
		ImageTag tag = mImageTagMaps.get(mCurrentIndex);
		tag.setScale(tag.getMinScale());
		
		if(mZoomBitmap != null) {
			mZoomBitmap.recycle();
			mZoomBitmap = null;
		}
	}

	
	/**
	 * show the layout (include the image name, all buttons)
	 */
	private void showControlLayout() {
		mTopLayout.setVisibility(View.VISIBLE);
		mBottomLayout.setVisibility(View.VISIBLE);
	}


	/**
	 * hide the layout (include the image name, all buttons)
	 */
	private void hideControlLayout() {
		mTopLayout.setVisibility(View.GONE);
		mBottomLayout.setVisibility(View.GONE);
	}
	
	
	/**
	 * @author jian.he
	 * 
	 * 		the Gesture event of the zoom view and cover view and viewPager
	 */
	private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
		
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if(!mIsShowCoverView && !mIsShowZoomView) {
				showCoverView();
				return true;
			}
			if(mIsShowCoverView) {
				hideCoverView();
				return true;
			}
			if(mIsShowZoomView) {
				showViewPagerView();
				mViewPagerBitmap = createBitmap(mCurrentIndex);
				mViewPagerAdapter.notifyDataSetChanged();
			}
			return true;
		}
		
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			if(mIsShowZoomView) {
				if(mTopLayout.getVisibility() == View.VISIBLE) {
					hideControlLayout();
				} else {
					showControlLayout();
				}
			}
			return true;
		}
	}
	
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.image_view_list:
			mHandler.removeMessages(START_SLIDE);
			mHandler.removeMessages(END_SLIDE);
			cancelTask();
			poststop();
			finish();
			overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
			break;
		case R.id.image_view_push:
			getFriendlyNameOfRenderer();
			break;
		case R.id.image_view_prev:
			prevImage();
			break;
		case R.id.image_view_rotate_left:
			rotateLeftImage();
			break;
		case R.id.image_view_rotate_right:
			rotateRightImage();
			break;
		case R.id.image_view_zoom_in:
			zoomInImage();
			break;
		case R.id.image_view_zoom_out:
			zoomOutImage();
			break;
		case R.id.image_view_next:
			nextImage();
			break;
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			mHandler.removeMessages(START_SLIDE);
			mHandler.removeMessages(END_SLIDE);
			cancelTask();
			poststop();
			finish();
			overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
			break;
		}
		return super.onKeyDown(keyCode, event);
	};

	
	@Override
	protected void onRestart() {
		super.onRestart();
		if(mIsShowCoverView) {
			showCoverView();
		}
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();
		if(mIsShowCoverView) {
			mHandler.removeMessages(START_SLIDE);
			mHandler.removeMessages(END_SLIDE);
			cancelTask();
		}
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mZoomBitmap != null) {
			mZoomBitmap.recycle();
			mZoomBitmap = null;
		}
		if(mViewPagerBitmap != null) {
			mViewPagerBitmap.recycle();
			mViewPagerBitmap = null;
		}
	}

	
	/**
	 * @author jian.he
	 * 		
	 *		return the item of viewPager
	 */
	private class ViewPagerItem extends LinearLayout {

		/**
		 * @param context
		 * @param position
		 * 		the current image position
		 * 
		 * 		Asynchronous loading picture
		 */
		private ViewPagerItem(Context context, int position) {
			super(context);
			
//			ScrollZoomImageView image = new ScrollZoomImageView(context);
			String filePath = sImageItemList.get(position).getFilePath();
			String thumbnailPath = sImageItemList.get(position).getThumbFilePath();
			
			ImageTag tag = mImageTagMaps.get(position);
			if(tag == null) {
				tag = new ImageTag();
				tag.setFilePath(filePath);
				tag.setThumbnailPath(thumbnailPath);
				tag.setType(ImageTag.IMAGE_FULL);
				mImageTagMaps.put(position, tag);
			}
			
			ImageView image = new ImageView(context);
			image.setTag(tag);
			mImageLoadManager.loadImage(image);
//			LinearLayout layout = new LinearLayout(context);
//			layout.setBackgroundResource(R.drawable.photo_big);
//			layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
//					LinearLayout.LayoutParams.WRAP_CONTENT));
//			layout.addView(image);

			setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
					LinearLayout.LayoutParams.FILL_PARENT));
			setGravity(Gravity.CENTER);
			addView(image);
		}
		
		/**
		 * @param context
		 * @param bm
		 * 		the bitmap to show
		 * 
		 * 		change the bitmap when rotate the image at viewPager view
		 */
		private ViewPagerItem(Context context, Bitmap bm) {
			super(context);
			
//			ScrollZoomImageView image = new ScrollZoomImageView(context);
			ImageView image = new ImageView(context);
			image.setImageBitmap(bm);

//			LinearLayout layout = new LinearLayout(context);
//			layout.setBackgroundResource(R.drawable.photo_big);
//			layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
//					LinearLayout.LayoutParams.WRAP_CONTENT));
//			layout.addView(image);

			setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
					LinearLayout.LayoutParams.FILL_PARENT));
			setGravity(Gravity.CENTER);
			addView(image);
		}
		
	}

	private class ViewPagerAdapter extends PagerAdapter {

		private Context mContext;
		private ArrayList<View> list = new ArrayList<View>();

		public ViewPagerAdapter(Context context) {
			initViewList();
			mContext = context;
		}

		private void initViewList() {
			for(int i = 0;i < getCount();i ++) {
				list.add(null);
			}
		}
		
		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}

		@Override
		public int getCount() {
			return mImageListMaxSize;
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			((ViewPager) container).removeView(list.get(position));
			list.set(position, null);
		}

		@Override
		public Object instantiateItem(ViewGroup container, int position) {

			View view = null;

			if (list.get(position) == null) {
				if (position == mCurrentIndex && mViewPagerBitmap != null) {
					view = new ViewPagerItem(mContext, mViewPagerBitmap);
				} else {
					view = new ViewPagerItem(mContext, position);
				}
				list.set(position, view);
			} else {
				view = list.get(position);
			}

			view.setOnTouchListener(mImageZoomListener);

			((ViewPager) container).addView(view, 0);
			return view;
		}

	}
	
	
	/**
	 *  get the settings value
	 */
	private void getSharedPreferenceValue() {
		String value = "";
		SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		//get post interval time
		value = mPreferences.getString(ImageSettingsActivity.POST_TIME_KEY, 
				getResources().getString(R.string.image_post_time_default_value));
		mPostIntervalTime = Integer.parseInt(value);
		
		//get slide interval time
		value = mPreferences.getString(ImageSettingsActivity.SLIDE_TIME_KEY, 
				getResources().getString(R.string.image_slide_time_default_value));
		mSlideIntervalTime = Integer.parseInt(value);
		
		//get rotate angle value
		value = mPreferences.getString(ImageSettingsActivity.ROTATE_ANGLE_KEY, 
				getResources().getString(R.string.image_rotate_angle_default_value));
		mRotateAngle = Integer.parseInt(value);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
//		MenuInflater inflater = getMenuInflater();
//		inflater.inflate(R.menu.image_setting_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.image_settings_menu:
				Intent intent = new Intent(this, ImageSettingsActivity.class);
				startActivityForResult(intent, IMAGE_SETTINGS_REQUEST);
				if(mIsShowCoverView) {
					mHandler.removeMessages(START_SLIDE);
					mHandler.removeMessages(END_SLIDE);
					cancelTask();
				}
				break;
		}
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == IMAGE_SETTINGS_REQUEST) {
			if(resultCode == RESULT_OK) {
				getSharedPreferenceValue();
				if(mIsShowCoverView) {
					showCoverView();
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	private void log(String str) {
		Log.e(TAG, str);
	}

}
