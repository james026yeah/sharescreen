package archermind.dlna.mobile;

import java.util.ArrayList;

import com.archermind.ashare.dlna.localmedia.PhotoItem;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import archermind.dlna.mobile.SimpleZoomListener.ControlType;

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
	private ArrayList<String> mImagePaths = new ArrayList<String>();

	private RelativeLayout mTopLayout;
	private LinearLayout mBottomLayout;

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

	private ImageAdapter mImageAdapter;
	private Gallery mGallery;
	private ImageZoomView mImageZoomView;
	private ZoomState mZoomState;
	private SimpleZoomListener mZoomListener;
	private Bitmap mZoomBitmap;

	private boolean mIsMoved;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("onCreate is call");

		mImageCurrentIndex = getIntent().getIntExtra(IMAGE_INDEX, 0);
		mImageListMaxSize = sImageItemList.size();

		setContentView(R.layout.local_media_image_view);

		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		mScreenWidth = dm.widthPixels;
		mScreenHeight = dm.heightPixels;
		
		initImageView();

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

		mGallery = (Gallery) findViewById(R.id.image_view_gallery);
		mImageAdapter = new ImageAdapter(this);
		mGallery.setAdapter(mImageAdapter);
		mGallery.setSelection(mImageCurrentIndex);
		mGallery.setCallbackDuringFling(false);
		mGallery.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mImageCurrentIndex = position;
				setImageName(position);
				postPlay(sImageItemList.get(mImageCurrentIndex).getItemUri(), IMAGE_TYPE);
				mImageAdapter.notifyDataSetChanged();
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {

			}
		});

		mImageZoomView = (ImageZoomView) findViewById(R.id.zoomview);
		mZoomState = new ZoomState();
		mZoomListener = new SimpleZoomListener();
		mZoomListener.setmGestureDetector(new GestureDetector(this, new MyGestureListener()));

		mZoomListener.setZoomState(mZoomState);
		mZoomListener.setControlType(ControlType.ZOOM);
		mImageZoomView.setZoomState(mZoomState);
		mImageZoomView.setOnTouchListener(mZoomListener);

	}

	private void initImageList() {
		for (int i = 0; i < mImageListMaxSize; i++) {
			mBitmapList.add(null);
			mZoomRatioList.add(IMAGE_ZOOM_RATIO_DEFAULT);
			mRotateList.add(IMAGE_ROTATE_ANGLE_DEFAULT);
			mImagePaths.add(sImageItemList.get(i).getFilePath());
		}
	}

	private void setImageName(int index) {
		mNameView.setText(sImageItemList.get(index).getTitle());
	}

	private void prevImage() {
		if (mImageCurrentIndex > 0) {
			mImageCurrentIndex--;
			setImageName(mImageCurrentIndex);
			postPrevious(sImageItemList.get(mImageCurrentIndex).getItemUri(), IMAGE_TYPE);
			mGallery.setSelection(mImageCurrentIndex);
			mImageAdapter.notifyDataSetChanged();
		}
	}

	private void nextImage() {
		if (mImageCurrentIndex < mImageListMaxSize - 1) {
			mImageCurrentIndex++;
			setImageName(mImageCurrentIndex);
			postNext(sImageItemList.get(mImageCurrentIndex).getItemUri(), IMAGE_TYPE);
			mGallery.setSelection(mImageCurrentIndex);
			mImageAdapter.notifyDataSetChanged();
		}
	}

	// private void rotateImage(float rotateValue) {
	// String path = sImageItemList.get(mImageCurrentIndex).getFilePath();
	// if (path != null) {
	//
	// }
	// }

	// private void zoomImage(float zoomValue) {
	//
	// }

	private Bitmap getDrawable(int index) {
		if (index >= 0 && index < sImageItemList.size()) {
			String path = sImageItemList.get(index).getFilePath();

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, options);
			int mWidth = options.outWidth;
			int mHeight = options.outHeight;
			int s = 1;
			while ((mWidth / s > mScreenWidth * 2) || (mHeight / s > mScreenHeight * 2)) {
				s *= 2;
			}

			options = new BitmapFactory.Options();
			options.inSampleSize = s;
			options.inPreferredConfig = Config.ARGB_8888;
			Bitmap bm = BitmapFactory.decodeFile(path, options);

			if (bm != null) {
				int h = bm.getHeight();
				int w = bm.getWidth();

				float ft = (float) ((float) w / (float) h);
				float fs = (float) ((float) mScreenWidth / (float) mScreenHeight);

				int neww = ft >= fs ? mScreenWidth : (int) (mScreenHeight * ft);
				int newh = ft >= fs ? (int) (mScreenWidth / ft) : mScreenHeight;

				float scaleWidth = ((float) neww) / w;
				float scaleHeight = ((float) newh) / h;

				Matrix matrix = new Matrix();
				matrix.postScale(scaleWidth, scaleHeight);
				bm = Bitmap.createBitmap(bm, 0, 0, w, h, matrix, true);

				return bm;
			}
		}
		return null;
	}

	private void resetZoomState() {
		if (mZoomBitmap != null) {
			mZoomBitmap.recycle();
		}
		mZoomBitmap = getDrawable(mImageCurrentIndex);
		mImageZoomView.setImage(mZoomBitmap);

		mZoomListener.setControlType(ControlType.ZOOM);
		mZoomState.setPanX(0.5f);
		mZoomState.setPanY(0.5f);
		mZoomState.setZoom(1f);
		mZoomState.notifyObservers();
	}

	public void showZoomView() {
		resetZoomState();
		mGallery.setVisibility(View.GONE);
		mIsMoved = false;
		mImageZoomView.setVisibility(View.VISIBLE);
	}

	public void showGalleryView() {
		mGallery.setVisibility(View.VISIBLE);
		mImageZoomView.setVisibility(View.GONE);
	}

	public void movedClick(View v) {
		mIsMoved = !mIsMoved;
		if (mIsMoved) {
			mZoomListener.setControlType(ControlType.PAN);
		} else {
			mZoomListener.setControlType(ControlType.ZOOM);
		}
	}

	private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			showGalleryView();
			return true;
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
			showZoomView();
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

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (event.getAction()) {
		case KeyEvent.KEYCODE_BACK:
			finish();
			break;
		}
		return super.onKeyDown(keyCode, event);
	};

	@Override
	public void onDestroy() {
		if (mZoomBitmap != null) {
			mZoomBitmap.recycle();
		}
		super.onDestroy();
	}

	private class GalleryViewItem extends LinearLayout {

		public GalleryViewItem(Context context, int position) {
			super(context);
			
			//create the imageView
			ImageView image = new ImageView(context);
			String path = sImageItemList.get(position).getFilePath();
			image.setTag(path);
			new ImageLoadManager().loadPreviewImage(image, mScreenWidth, mScreenHeight);

			//create the imageView background
			LinearLayout layout = new LinearLayout(context);
			layout.setBackgroundResource(R.drawable.photo_big);
			layout.addView(image, new Gallery.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));

			//create the gallery item view
			setOrientation(LinearLayout.VERTICAL);
			setLayoutParams(new Gallery.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT));
			setGravity(Gravity.CENTER);
			addView(layout, new Gallery.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));

		}
	}

	private class ImageAdapter extends BaseAdapter {

		private Context mContext;

		public ImageAdapter(Context context) {
			mContext = context;
		}

		@Override
		public int getCount() {
			return sImageItemList.size();
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
			return new GalleryViewItem(mContext, position);
		}

	}

	private void log(String str) {
		Log.e(TAG, str);
	}

}
