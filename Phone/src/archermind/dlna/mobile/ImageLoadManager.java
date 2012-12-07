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
import android.util.Log;
import android.widget.ImageView;

import com.archermind.ashare.misc.CustomAsyncTask;

public class ImageLoadManager {
	
	private static final int IMAGE_THUMBNAIL = ImageTag.IMAGE_THUMBNAIL;
	private static final int IMAGE_FULL = ImageTag.IMAGE_FULL;
	private static final int VIDEO_THUMBNAIL = ImageTag.VIDEO_THUMBNAIL;
	
	private static Map<String, SoftReference<Bitmap>> mBitmaps;
	
	public ImageLoadManager() {
		if(mBitmaps != null) {
			mBitmaps = null;
		}
		mBitmaps = new HashMap<String, SoftReference<Bitmap>>();
	}
	
	public BitmapDrawable loadImage(ImageView target) {

		ImageTag tag = (ImageTag) target.getTag();
		String path = tag.getFilePath();
		int type = tag.getType();
		
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
			mBitmaps.put(path, null);
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
		
		if(thumbnailPath != null) {
			Log.e("ImageViewActivity", "########### use thumbnail path ##############");
			bitmap = createThumbnail(thumbnailPath);
		} else {
			Log.e("ImageViewActivity", "~~~~~~~~~~~ use file path ~~~~~~~~~~~~~~");
			int type = tag.getType();
			switch(type) {
			case IMAGE_THUMBNAIL:
				bitmap = createImageThumbnail(tag);
				break;
			case IMAGE_FULL:
				bitmap = createImageFull(tag);
				break;
			case VIDEO_THUMBNAIL:
				bitmap = createVideoThumbnail(tag);
				break;
			}
		}
		return bitmap;
	}
	
	
	private Bitmap createThumbnail(String thumbnailPath) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		Bitmap bitmap = BitmapFactory.decodeFile(thumbnailPath, options);
		return bitmap;
	}
	
	
	private Bitmap createImageThumbnail(ImageTag tag) {
		Bitmap bitmap = null;

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(tag.getFilePath(), options);

		int type = tag.getType();
		int width = 0;
		int height = 0;
		int scale = 1;

		if (IMAGE_THUMBNAIL == type) {
			width = tag.getScreenWidth() / 3;
			height = tag.getScreenHeight() / 3;
		}
		if (IMAGE_FULL == type) {
			width = tag.getScreenWidth();
			height = tag.getScreenHeight();
		}
		while ((options.outWidth / scale > width * 2) || (options.outHeight / scale > height * 2)) {
			scale *= 2;
		}
		options.inJustDecodeBounds = false;
		options.inSampleSize = scale;
		bitmap = BitmapFactory.decodeFile(tag.getFilePath(), options);

		return bitmap;
	}
	
	
	private Bitmap createImageFull(ImageTag tag) {
		Bitmap bitmap = null;
		bitmap = createImageThumbnail(tag);
		
		if (bitmap != null) {

			int width = bitmap.getWidth();
			int height = bitmap.getHeight();

			Matrix matrix = new Matrix();

			if (tag.getScale() == 0) {
				// float scaleWidth;
				// float scaleHeight;

				if (width > tag.getScreenWidth() || height > tag.getScreenHeight()) {
					// scaleWidth = (float) width / (float)
					// tag.getScreenWidth();
					// scaleHeight = (float) height / (float)
					// tag.getScreenHeight();
					// maxScale = Math.max(scaleWidth, scaleHeight);
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
	
	
	private Bitmap createVideoThumbnail(ImageTag tag) {
		Bitmap bitmap = null;
		bitmap = ThumbnailUtils.createVideoThumbnail(tag.getFilePath(), Thumbnails.MINI_KIND);
		if (bitmap != null) {
			Matrix matrix = new Matrix();
			matrix.setScale(0.4f, 0.4f);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		}
		return bitmap;
	}
	
}
