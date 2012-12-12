package archermind.dlna.mobile;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.provider.MediaStore.Video.Thumbnails;
import android.widget.ImageView;

import com.archermind.ashare.misc.CustomAsyncTask;

public class ImageLoadManager {
	
	private static final int IMAGE_THUMBNAIL = ImageTag.IMAGE_THUMBNAIL;
	private static final int IMAGE_FULL = ImageTag.IMAGE_FULL;
	private static final int VIDEO_THUMBNAIL = ImageTag.VIDEO_THUMBNAIL;
	
	private static Map<String, WeakReference<Bitmap>> mBitmaps = new HashMap<String, WeakReference<Bitmap>>();
	
	private int mScreenWidth;
	private int mScreenHeight;

	public ImageLoadManager() {
		clearBitmaps();
	}
	
	public void clearBitmaps() {
		if(!mBitmaps.isEmpty()) {
			Set<String> set= mBitmaps.keySet();
			for(String path : set) {
				mBitmaps.put(path, null);
			}
			mBitmaps.clear();
		}
	}
	
	public void setScreenWidth(int width) {
		mScreenWidth = width;
	}
	
	public void setScreenHeight(int height) {
		mScreenHeight = height;
	}
	
	public BitmapDrawable loadImage(ImageView target) {

		ImageTag tag = (ImageTag) target.getTag();
		String path = tag.getFilePath();
		int type = tag.getType();
		
		if (IMAGE_THUMBNAIL == type || VIDEO_THUMBNAIL == type) {
			if (mBitmaps.containsKey(path)) {
				WeakReference<Bitmap> temp = mBitmaps.get(path);
				if (temp != null) {
					if (temp.get() != null) {
						target.setBackgroundDrawable(new BitmapDrawable(temp.get()));
						return null;
					}
				}
			}
			new ImageLoadTask().execute(target);
		}
		if (IMAGE_FULL == type) {
			new ImageLoadTask().execute(target);
		}
		return null;
	}
	
	
	class ImageLoadTask extends CustomAsyncTask<ImageView, Void, Bitmap> {
		
		private ImageView mImageView;
		private ImageTag mTag;
		
		@Override
		protected Bitmap doInBackground(ImageView... params) {
			mImageView = params[0];
			mTag = (ImageTag) mImageView.getTag();
			return createBitmap(mTag);
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			mBitmaps.put(mTag.getFilePath(), new WeakReference<Bitmap>(result));
			if(mTag.equals(mImageView.getTag())) {
				if (IMAGE_THUMBNAIL == mTag.getType() || VIDEO_THUMBNAIL == mTag.getType()) {
					mImageView.setBackgroundDrawable(new BitmapDrawable(result));
				}
				if(IMAGE_FULL == mTag.getType()) {
					mImageView.setImageBitmap(result);
				}
			}
		}
	}
	
	
	private Bitmap createBitmap(ImageTag tag) {
		Bitmap bitmap = null;
		String thumbnailPath = tag.getThumbnailPath();
		int type = tag.getType();
		switch(type) {
		case IMAGE_THUMBNAIL:
			if (thumbnailPath != null) {
				bitmap = compressPictures(thumbnailPath, type);
				if(bitmap != null) {
					return bitmap;
				}
			}
			bitmap = compressPictures(tag.getFilePath(), type);
			break;
		case VIDEO_THUMBNAIL:
			if (thumbnailPath != null) {
				bitmap = compressPictures(thumbnailPath, type);
				if(bitmap != null) {
					return bitmap;
				}
			}
			bitmap = ThumbnailUtils.createVideoThumbnail(tag.getFilePath(), Thumbnails.MICRO_KIND);
			break;
		case IMAGE_FULL:
			bitmap = createImageFull(tag);
			break;
		}
		return bitmap;
	}

	
	private Bitmap compressPictures(String path, int type) {
		Bitmap bitmap = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		int width = 0;
		int height = 0;
		int scale = 1;

		if (IMAGE_THUMBNAIL == type || VIDEO_THUMBNAIL == type) {
			width = mScreenWidth / 3;
			height = mScreenHeight / 3;
		}
		if (IMAGE_FULL == type) {
			width = mScreenWidth;
			height = mScreenHeight;
		}
		while ((options.outWidth / scale > width * 2) || (options.outHeight / scale > height * 2)) {
			scale *= 2;
		}
		options.inJustDecodeBounds = false;
		options.inSampleSize = scale;
		bitmap = BitmapFactory.decodeFile(path, options);
		return bitmap;
	}
	
	
	private Bitmap createImageFull(ImageTag tag) {
		Bitmap bitmap = null;
		bitmap = compressPictures(tag.getFilePath(), IMAGE_FULL);
		
		if (bitmap != null) {

			int width = bitmap.getWidth();
			int height = bitmap.getHeight();

			Matrix matrix = new Matrix();

			if (tag.getScale() == 0) {
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
			}
			matrix.setScale(tag.getScale(), tag.getScale());
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

			width = bitmap.getWidth();
			height = bitmap.getHeight();

			matrix = new Matrix();
			matrix.setRotate(tag.getRotateValue());
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
		}

		return bitmap;
	}
	
}
