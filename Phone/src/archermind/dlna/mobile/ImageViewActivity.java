package archermind.dlna.mobile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import archermind.dlna.media.PhotoItem;

public class ImageViewActivity extends BaseActivity implements OnClickListener {
	
	private static final String TAG = "ImageViewActivity";
	
	public static final String IMAGE_INDEX = "image_index";
	public static ArrayList<PhotoItem> sImageItemList = new ArrayList<PhotoItem>();
	
	private static final float IMAGE_ROTATE_ANGLE_DEFAULT = 0.0f;
	private static final float IMAGE_ROTATE_LEFT_VALUE = -90.0f;
	private static final float IMAGE_ROTATE_RIGHT_VALUE = 90.0f;
	
	private static final float IMAGE_ZOOM_RATIO_DEFAULT = 1.0f;
	private static final float IMAGE_ZOOM_OUT_VALUE = 0.8f;
	private static final float IMAGE_ZOOM_IN_VALUE = 1.25f;
	
	private ArrayList<Bitmap> mBitmapList = new ArrayList<Bitmap>();
	private ArrayList<Float> mZoomRatioList = new ArrayList<Float>();
	private ArrayList<Float> mRotateList = new ArrayList<Float>();
	
	private RelativeLayout mTopLayout;
	private LinearLayout mBottomLayout;
	
	private ViewPager mViewPager;
	
	private LinearLayout mListView;
	private TextView mNameView;
	private ImageView mPrevView;
	private ImageView mRotateLeftView;
	private ImageView mRotateRightView;
	private ImageView mZoomInView;
	private ImageView mZoomOutView;
	private ImageView mNextView;
	
	private int mScreenWidth;
	private int mScreenHeight;
	private int mImageCurrentIndex;
	private int mImageListMaxSize;
	
	private ViewPagerAdapter mViewPagerAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("onCreate is call");
		
		mImageCurrentIndex = getIntent().getIntExtra(IMAGE_INDEX, 0);
		
		mImageListMaxSize = sImageItemList.size();
		
		setContentView(R.layout.local_media_image_view);
		getScreenWidthAndHeight();
		
		initImageView();
		
	}
	
	protected void onStop() {
		super.onStop();
		sImageItemList.removeAll(sImageItemList);
		mBitmapList.removeAll(mBitmapList);
		sImageItemList = null;
		mBitmapList = null;
	};
	
	private void getScreenWidthAndHeight() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		mScreenWidth = dm.widthPixels;
		mScreenHeight = dm.heightPixels;
	}
	
	private void initImageView() {
		
		mTopLayout = (RelativeLayout) findViewById(R.id.image_view_top_layout);
		mBottomLayout = (LinearLayout) findViewById(R.id.image_view_bottom_layout);
		mTopLayout.getBackground().setAlpha(150);
		mBottomLayout.getBackground().setAlpha(150);
		
		mListView = (LinearLayout) findViewById(R.id.image_view_list);
		mNameView = (TextView) findViewById(R.id.image_view_name);
		mPrevView = (ImageView) findViewById(R.id.image_view_prev);
		mRotateLeftView = (ImageView) findViewById(R.id.image_view_rotate_left);
		mRotateRightView = (ImageView) findViewById(R.id.image_view_rotate_right);
		mZoomInView = (ImageView) findViewById(R.id.image_view_zoom_in);
		mZoomOutView = (ImageView) findViewById(R.id.image_view_zoom_out);
		mNextView = (ImageView) findViewById(R.id.image_view_next);
		
		mListView.setOnClickListener(this);
		mPrevView.setOnClickListener(this);
		mRotateLeftView.setOnClickListener(this);
		mRotateRightView.setOnClickListener(this);
		mZoomInView.setOnClickListener(this);
		mZoomOutView.setOnClickListener(this);
		mNextView.setOnClickListener(this);
		
		initImageList();
		
		ArrayList<View> list = new ArrayList<View>();
		for(int i = 0;i < mBitmapList.size();i ++) {
			View view = getLayoutInflater().inflate(R.layout.local_media_image_view_item, null);
			list.add(view);
		}
		
		mViewPager = (ViewPager) findViewById(R.id.image_view_pager);
		mViewPagerAdapter = new ViewPagerAdapter(list);
		mViewPager.setAdapter(mViewPagerAdapter);
		mViewPager.setCurrentItem(mImageCurrentIndex);
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int arg0) {
				mImageCurrentIndex = arg0;
				setImageName(arg0);
				if(mImageCurrentIndex == 0) {
					Toast.makeText(ImageViewActivity.this, R.string.image_first_message, Toast.LENGTH_SHORT).show();
				}
				else if(mImageCurrentIndex == mImageListMaxSize - 1) {
					Toast.makeText(ImageViewActivity.this, R.string.image_last_message, Toast.LENGTH_SHORT).show();
				}
				postPlay(sImageItemList.get(mImageCurrentIndex).getItemUri(), IMAGE_TYPE);
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {
				
			}
		});
		
	}
	
	private void initImageList() {
		for(int i = 0;i < mImageListMaxSize;i ++) {
			mBitmapList.add(null);
			mZoomRatioList.add(IMAGE_ZOOM_RATIO_DEFAULT);
			mRotateList.add(IMAGE_ROTATE_ANGLE_DEFAULT);
		}
	}
	
	private float getMinScale(float scaleWidth, float scaleHeight) {
		return scaleWidth < scaleHeight ? scaleWidth : scaleHeight;
	}
	
	private void setImageName(int index) {
		mNameView.setText(sImageItemList.get(index).getTitle());
	}
	
	private Bitmap createImageBitmap(int index) {
		File file = new File(sImageItemList.get(index).getFilePath());
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(file), null, o);
			
			// The new size we want to scale to
			// final int REQUIRED_SIZE=getScreenWidthAndHeight();
			// Find the correct scale value. It should be the power of 2.
			int scale = 1;
			while (o.outWidth / scale / 2 >= mScreenWidth && o.outHeight / scale / 2 >= mScreenHeight)
				scale *= 2;
			o.inJustDecodeBounds = false;
			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(file), null, o2);
		}
		catch (FileNotFoundException e) {
		}
		return null;
//		int width = resizeBmp.getWidth();
//		int height = resizeBmp.getHeight();
//		
//		Matrix matrix = new Matrix();
//		float scaleWidth = ((float) mScreenWidth) / width;
//		float scaleHeight = ((float) mScreenHeight) / height;
//		float scale = getMinScale(scaleWidth, scaleHeight);
//		matrix.postScale(scale, scale);
//		Bitmap bm = Bitmap.createBitmap(resizeBmp, 0, 0, width, height, matrix, true);
//		return bm;
	}
	
	private void prevImage() {
		if (mImageCurrentIndex == 0) {
			Toast.makeText(this, R.string.image_first_message, Toast.LENGTH_SHORT).show();
		}
		else {
			mImageCurrentIndex--;
			setImageName(mImageCurrentIndex);
			postPrevious(sImageItemList.get(mImageCurrentIndex).getItemUri(), IMAGE_TYPE);
			mViewPager.setCurrentItem(mImageCurrentIndex);
			mViewPagerAdapter.notifyDataSetChanged();
		}
	}
	
	private void nextImage() {
		if (mImageCurrentIndex == mImageListMaxSize - 1) {
			Toast.makeText(this, R.string.image_last_message, Toast.LENGTH_SHORT).show();
		}
		else {
			mImageCurrentIndex++;
			setImageName(mImageCurrentIndex);
			postNext(sImageItemList.get(mImageCurrentIndex).getItemUri(), IMAGE_TYPE);
			mViewPager.setCurrentItem(mImageCurrentIndex);
			mViewPagerAdapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.image_view_list:
				finish();
				break;
			case R.id.image_view_prev:
				prevImage();
				break;
			case R.id.image_view_rotate_left:
				postFlipImage(mRotateList.get(mImageCurrentIndex) + IMAGE_ROTATE_LEFT_VALUE + "");
				break;
			case R.id.image_view_rotate_right:
				postFlipImage(mRotateList.get(mImageCurrentIndex) + IMAGE_ROTATE_RIGHT_VALUE + "");
				break;
			case R.id.image_view_zoom_in:
				postScalingImage(mZoomRatioList.get(mImageCurrentIndex) * IMAGE_ZOOM_IN_VALUE + "");
				break;
			case R.id.image_view_zoom_out:
				postScalingImage(mZoomRatioList.get(mImageCurrentIndex) * IMAGE_ZOOM_OUT_VALUE + "");
				break;
			case R.id.image_view_next:
				nextImage();
				break;
		}
	}
	
	private class ViewPagerAdapter extends PagerAdapter {
		
		private ArrayList<View> list = new ArrayList<View>();
		
		public ViewPagerAdapter(ArrayList<View> list) {
			this.list = list;
		}
		
		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}
		
		@Override
		public int getCount() {
			return list.size();
		}
		
		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}
		
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			((ViewPager) container).removeView(list.get(position));
		}
		
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			
			View view = list.get(position);
			ImageView imageView = (ImageView) view.findViewById(R.id.image_view_show);
			
			if(mBitmapList.get(position) != null) {
				imageView.setImageBitmap(mBitmapList.get(position));
			}
			else {
				imageView.setImageBitmap(createImageBitmap(position));
			}
			
			imageView.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(mTopLayout.getVisibility() == View.VISIBLE) {
						mTopLayout.setVisibility(View.GONE);
						mBottomLayout.setVisibility(View.GONE);
					}
					else {
						mTopLayout.setVisibility(View.VISIBLE);
						mBottomLayout.setVisibility(View.VISIBLE);
					}
				}
			});
			
			((ViewPager) container).addView(list.get(position), 0);
			return list.get(position);
		}
		
	}
	
	private void log(String str) {
		Log.e(TAG, str);
	}
	
}
