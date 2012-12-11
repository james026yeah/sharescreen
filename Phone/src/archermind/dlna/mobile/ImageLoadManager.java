package archermind.dlna.mobile;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

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
	
	private static Map<String, SoftReference<Bitmap>> mBitmaps;
	
	private int mScreenWidth;
	private int mScreenHeight;

	public ImageLoadManager() {
		if(mBitmaps != null) {
			mBitmaps = null;
			mScreenWidth = 0;
			mScreenHeight = 0;
		}
		mBitmaps = new HashMap<String, SoftReference<Bitmap>>();
	}
	
	public BitmapDrawable loadImage(ImageView target) {

		ImageTag tag = (ImageTag) target.getTag();
		String path = tag.getFilePath();
		int type = tag.getType();
		
		if(mScreenWidth == 0 || mScreenHeight == 0) {
			mScreenWidth = tag.getScreenWidth();
			mScreenHeight = tag.getScreenHeight();
		}

		if (IMAGE_THUMBNAIL == type || VIDEO_THUMBNAIL == type) {
			if (mBitmaps.containsKey(path)) {
				SoftReference<Bitmap> temp = mBitmaps.get(path);
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
			mBitmaps.put(mTag.getFilePath(), new SoftReference<Bitmap>(result));
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
		if (thumbnailPath != null) {
			bitmap = createThumbnail(tag);
			if(bitmap != null) {
				return bitmap;
			}
		}
		int type = tag.getType();
		switch(type) {
		case IMAGE_THUMBNAIL:
			bitmap = compressPictures(tag.getFilePath(), IMAGE_THUMBNAIL);
			break;
		case IMAGE_FULL:
			bitmap = createImageFull(tag);
			break;
		case VIDEO_THUMBNAIL:
			bitmap = ThumbnailUtils.createVideoThumbnail(tag.getFilePath(), Thumbnails.MICRO_KIND);
			break;
		}
		return bitmap;
	}

	
	private Bitmap createThumbnail(ImageTag tag) {
		Bitmap bitmap = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		
		if(VIDEO_THUMBNAIL == tag.getType()) {
			bitmap = compressPictures(tag.getThumbnailPath(), VIDEO_THUMBNAIL);
		} else {
			bitmap = BitmapFactory.decodeFile(tag.getThumbnailPath(), options);
		}
		
		if(IMAGE_FULL == tag.getType()) {
			if(bitmap != null) {
				int width = bitmap.getWidth();
				int height = bitmap.getHeight();
				
				Matrix matrix = new Matrix();
				if(tag.getScale() == 0) {
					matrix.setScale(1.0f, 1.0f);
				} else {
					matrix.setScale(tag.getScale(), tag.getScale());
				}
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
				
				width = bitmap.getWidth();
				height = bitmap.getHeight();
				
				matrix = new Matrix();
				matrix.setRotate(tag.getRotateValue());
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
			}
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
				if (width > tag.getScreenWidth() || height > tag.getScreenHeight()) {
					tag.setIsBigImage(true);
					tag.setMaxScale(3.0f);
					tag.setMinScale(1.0f);
					tag.setScale(1.0f);
				} else {
					float scaleWidth = (float) tag.getScreenWidth() / (float) width;
					float scaleHeight = (float) tag.getScreenHeight() / (float) height;
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
