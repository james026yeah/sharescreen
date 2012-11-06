package archermind.dlna.mobile;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import archermind.dlna.media.PhotoItem;

@SuppressLint("HandlerLeak")
public class ImageViewActivity extends BaseActivity implements OnClickListener {
	
	private static final String TAG = "ImageViewActivity";
	
	public static final String IMAGE_INDEX = "image_index";
	public static ArrayList<PhotoItem> sImageItemList = new ArrayList<PhotoItem>();
	
	private static final int IMAGE_ROTATE_LEFT_VALUE = -90;
	private static final int IMAGE_ROTATE_RIGHT_VALUE = 90;
	private static final int IMAGE_ROTATE_ANGLE_DEFAULT = 0;
	
	private static final int IMAGE_BITMAP_CREATED = 0;
	
	private ArrayList<Bitmap> mBitmapList = new ArrayList<Bitmap>();
	private ArrayList<Integer> mBackgroudList = new ArrayList<Integer>();
	private ArrayList<Integer> mRotateAngleList = new ArrayList<Integer>();
	
	private RelativeLayout mTopLayout;
	private LinearLayout mBottomLayout;
	
	private LinearLayout mListView;
	private TextView mNameView;
	private Gallery mGallery;
	private ImageView mPrevView;
	private ImageView mRotateLeftView;
	private ImageView mRotateRightView;
	private ImageView mZoomInView;
	private ImageView mZoomOutView;
	private ImageView mNextView;
	
	private int mImageCurrentIndex;
	private int mImageListMaxSize;
	private int mImageRotateAngle;
	
	private int mScreenWidth;
	private int mScreenHeight;
	
	private ImageAdapter mImageAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("onCreate is call");
		
		setContentView(R.layout.local_media_image_view);
		
		mImageCurrentIndex = getIntent().getIntExtra(IMAGE_INDEX, 0);
		mImageListMaxSize = sImageItemList.size();
		
		getScreenWidthAndHeight();
		
		initImageView();
		
	}
	
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
		
		mGallery = (Gallery) findViewById(R.id.image_view_gallery);
		
		new Thread() {
			public void run() {
				for(int i = 0;i < mImageListMaxSize;i ++) {
					mBitmapList.add(createImageBitmap(i));
					mBackgroudList.add(R.drawable.photo_big);
					mRotateAngleList.add(IMAGE_ROTATE_ANGLE_DEFAULT);
				}
				mHandler.sendEmptyMessage(IMAGE_BITMAP_CREATED);
			};
		}.start();
		
	}
	
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case IMAGE_BITMAP_CREATED:
					mImageAdapter = new ImageAdapter(ImageViewActivity.this, mBitmapList, mBackgroudList);
					mGallery.setAdapter(mImageAdapter);
					mGallery.setSelection(mImageCurrentIndex);
					mGallery.setOnItemSelectedListener(new OnItemSelectedListener() {

						@Override
						public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
							mImageCurrentIndex = position;
							mImageRotateAngle = 0;
							setImageName(position);
						}

						@Override
						public void onNothingSelected(AdapterView<?> arg0) {
							// nothing to do
						}
						
					});
					mGallery.setOnItemClickListener(new OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
					break;
			}
		};
	};
	
	private float getMinScale(float scaleWidth, float scaleHeight) {
		return scaleWidth < scaleHeight ? scaleWidth : scaleHeight;
	}
	
	private void setImageName(int index) {
		mNameView.setText(sImageItemList.get(index).getTitle());
	}
	
	private void rotateImageBitmap(int rotateValue) {
		
//		Bitmap bitmap = mBitmapList.get(mImageCurrentIndex);
		Bitmap bitmap = BitmapFactory.decodeFile(sImageItemList.get(mImageCurrentIndex).getFilePath());
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		
		Matrix matrix = new Matrix();
		mImageRotateAngle = mRotateAngleList.get(mImageCurrentIndex) + rotateValue;
		matrix.setRotate(mImageRotateAngle);
		Bitmap bm = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
		mBitmapList.set(mImageCurrentIndex, bm);
		mBackgroudList.set(mImageCurrentIndex, R.drawable.photo_big);
		mImageAdapter.notifyDataSetChanged();
		
		mRotateAngleList.set(mImageCurrentIndex, mImageRotateAngle);
	}
	
	private Bitmap createImageBitmap(int index) {
		
		Bitmap bitmap = BitmapFactory.decodeFile(sImageItemList.get(index).getFilePath());
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		
		Matrix matrix = new Matrix();
		
		float scaleWidth;
		float scaleHeight;
		
		scaleWidth = ((float) mScreenWidth) / width;
		scaleHeight = ((float) mScreenHeight) / height;
		
		float scale = getMinScale(scaleWidth, scaleHeight);
		matrix.setScale(scale, scale);
		
		Bitmap bm = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
		return bm;
	}
	
	private void prevImage() {
		if (mImageCurrentIndex == 0) {
			Toast.makeText(this, R.string.image_gallery_first_message, Toast.LENGTH_SHORT).show();
		}
		else {
			mImageCurrentIndex--;
			postPrevious(sImageItemList.get(mImageCurrentIndex).getItemUri(), "images");
			mGallery.setSelection(mImageCurrentIndex);
			mImageAdapter.notifyDataSetChanged();
			setImageName(mImageCurrentIndex);
		}
	}
	
	private void nextImage() {
		if (mImageCurrentIndex == mImageListMaxSize - 1) {
			Toast.makeText(this, R.string.image_gallery_last_message, Toast.LENGTH_SHORT).show();
		}
		else {
			mImageCurrentIndex++;
			postNext(sImageItemList.get(mImageCurrentIndex).getItemUri(), "images");
			mGallery.setSelection(mImageCurrentIndex);
			mImageAdapter.notifyDataSetChanged();
			setImageName(mImageCurrentIndex);
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
				rotateImageBitmap(IMAGE_ROTATE_LEFT_VALUE);
				break;
			case R.id.image_view_rotate_right:
				rotateImageBitmap(IMAGE_ROTATE_RIGHT_VALUE);
				break;
			case R.id.image_view_zoom_in:
				break;
			case R.id.image_view_zoom_out:
				break;
			case R.id.image_view_next:
				nextImage();
				break;
		}
	}
	
	private class ImageAdapter extends BaseAdapter {

		private Context mContext;
		private ArrayList<Bitmap> mBitmaps = new ArrayList<Bitmap>();
		private ArrayList<Integer> mBackgrounds = new ArrayList<Integer>();
		
		public ImageAdapter(Context context, ArrayList<Bitmap> bitmaps,
					ArrayList<Integer> backgrounds) {
			mContext = context;
			mBitmaps = bitmaps;
			mBackgrounds = backgrounds;
		}
		
		@Override
		public int getCount() {
			return mBitmaps.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			
//			if(convertView == null) {
				view = getLayoutInflater().inflate(R.layout.local_media_image_view_item, null);
//			}
//			else {
//				view = convertView;
//			}
			
			LinearLayout imageViewBg = (LinearLayout) view.findViewById(R.id.image_view_show_bg);
			imageViewBg.setBackgroundResource(mBackgrounds.get(position));
			
			ImageView imageView = (ImageView) view.findViewById(R.id.image_view_show);
			imageView.setImageBitmap(mBitmaps.get(position));
			
			return view;
		}
		
	}
	
	private void log(String str) {
		Log.e(TAG, str);
	}
	
}
