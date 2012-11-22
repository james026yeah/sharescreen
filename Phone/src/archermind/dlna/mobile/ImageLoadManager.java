package archermind.dlna.mobile;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.provider.MediaStore.Video.Thumbnails;
import android.util.Log;
import android.widget.ImageView;

import com.archermind.ashare.misc.CustomAsyncTask;

public class ImageLoadManager {
	
	private static final String TAG = "ImageLoadManager";
	
//	private static final int MAXIMUM_POOL_SIZE = 10;
	
	private static final Map<String, WeakReference<Bitmap>> mThumbnails = new HashMap<String, WeakReference<Bitmap>>();
	private static final Map<String, WeakReference<Bitmap>> mPreviewImages = new HashMap<String, WeakReference<Bitmap>>();
	private static final Map<String, WeakReference<Bitmap>> mVideoImages = new HashMap<String, WeakReference<Bitmap>>();
	
	private int mScreenWidth = 0;
	private int mScreenHeight = 0;
	
	private boolean mIsVideoBitmap = false;
	
	public BitmapDrawable loadImage(ImageView target) {
		if (mThumbnails.containsKey(target.getTag())) {
			WeakReference<Bitmap> temp = mThumbnails.get((String) target.getTag());
			Log.d("www", "loadImage: temp = " + temp.get());
			if (temp.get() != null) {
				target.setBackgroundDrawable(new BitmapDrawable(temp.get()));
				return null;
			}
			else {
				target.setBackgroundResource(R.color.app_main_bg);
			}
		}
		
//		ThreadPoolExecutor executor = (ThreadPoolExecutor) CustomAsyncTask.THREAD_POOL_EXECUTOR;
//		executor.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
		
		new ImageLoadTask().execute(target);
		
		return null;
	}
	
	public BitmapDrawable loadPreviewImage(ImageView target, int screenWidth, int screenHeight) {
		
		mScreenWidth = screenWidth;
		mScreenHeight = screenHeight;
		
		if (mPreviewImages.containsKey(target.getTag())) {
			WeakReference<Bitmap> temp = mPreviewImages.get((String) target.getTag());
			if (temp.get() != null) {
				target.setImageBitmap(temp.get());
				return null;
			}
			else {
				target.setBackgroundResource(R.color.app_main_bg);
			}
		}
		
//		ThreadPoolExecutor executor = (ThreadPoolExecutor) CustomAsyncTask.THREAD_POOL_EXECUTOR;
//		executor.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
		
		new ImageLoadTask().execute(target);
		
		return null;
	}
	
	public BitmapDrawable loadVideoImage(ImageView target) {
		
		mIsVideoBitmap = true;
		
		if (mVideoImages.containsKey(target.getTag())) {
			WeakReference<Bitmap> temp = mVideoImages.get((String) target.getTag());
			if (temp.get() != null) {
				target.setImageBitmap(temp.get());
				return null;
			}
			else {
				target.setBackgroundResource(R.color.app_main_bg);
			}
		}
		
//		ThreadPoolExecutor executor = (ThreadPoolExecutor) CustomAsyncTask.THREAD_POOL_EXECUTOR;
//		executor.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
		
		new ImageLoadTask().execute(target);
		
		return null;
	}
	
	class ImageLoadTask extends CustomAsyncTask<ImageView, Void, Bitmap> {
		
		private ImageView mTarget;
		private String mTag;
		
		@Override
		protected Bitmap doInBackground(ImageView... params) {
			mTarget = params[0];
			mTag = (String) mTarget.getTag();
			Log.d(TAG, "doInBackground: " + mTag);
			Log.d("www", "doInBackground: " + Thread.currentThread().getId());
			if(mScreenWidth != 0) {
				return createImageBitmap(mTag);
			}
			else {
				return createBitmap(mTag);
			}
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			Log.d(TAG, "onPostExecute: mTarget = " + mTarget.getTag());
			Log.d(TAG, "onPostExecute: mTag = " + mTag);
			if(mScreenWidth != 0) {
				mPreviewImages.put((String) mTarget.getTag(), new WeakReference<Bitmap>(result));
				if (mTag.equals(mTarget.getTag())) {
					mTarget.setImageBitmap(result);
				}
			}
			else {
				mThumbnails.put((String) mTarget.getTag(), new WeakReference<Bitmap>(result));
				if (mTag.equals(mTarget.getTag())) {
					mTarget.setBackgroundDrawable(new BitmapDrawable(result));
				}
			}
		}
	}
	
	private Bitmap createBitmap(String filePath) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 10;
		Bitmap bitmap = null;
		if(mIsVideoBitmap) {
			bitmap = ThumbnailUtils.createVideoThumbnail(filePath, Thumbnails.MINI_KIND);
		} else {
			bitmap = BitmapFactory.decodeFile(filePath, options);
		}
		return bitmap;
	}
	
	private Bitmap createImageBitmap(String path) {

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);
		int mWidth = options.outWidth;
		int mHeight = options.outHeight;
		int scale = 1;
		while ((mWidth / scale > mScreenWidth * 2) || (mHeight / scale > mScreenHeight * 2)) {
			scale *= 2;
		}

		options = new BitmapFactory.Options();
		options.inPreferredConfig = Config.ARGB_8888;
		options.inSampleSize = scale;
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
		return null;
	}
	
}
