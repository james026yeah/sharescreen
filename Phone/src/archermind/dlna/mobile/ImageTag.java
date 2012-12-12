package archermind.dlna.mobile;

public class ImageTag {

	public static final int IMAGE_THUMBNAIL = 0;
	public static final int IMAGE_FULL = 1;
	public static final int VIDEO_THUMBNAIL = 2;
	
	private String mFilePath;
	private String mThumbnailPath;
	private float mRotateValue;
	private float mMaxScale;
	private float mMinScale;
	private float mScale;
	private boolean mIsBigImage;
	private int mType;

	//set methods
	
	public void setFilePath(String path) {
		mFilePath = path;
	}
	
	public void setThumbnailPath(String path) {
		mThumbnailPath = path;
	}
	
	public void setMaxScale(float scale) {
		mMaxScale = scale;
	}

	public void setMinScale(float scale) {
		mMinScale = scale;
	}

	public void setScale(float scale) {
		mScale = scale;
	}

	public void setIsBigImage(boolean isBigImage) {
		mIsBigImage = isBigImage;
	}
	
	public void setRotateValue(float rotateValue) {
		mRotateValue = rotateValue;
	}
	
	public void setType(int type) {
		mType = type;
	}
	
	
	//get methods
	
	public float getMaxScale() {
		return mMaxScale;
	}

	public float getMinScale() {
		return mMinScale;
	}

	public float getScale() {
		return mScale;
	}

	public String getFilePath() {
		return mFilePath;
	}
	
	public String getThumbnailPath() {
		return mThumbnailPath;
	}
	
	public float getRotateValue() {
		return mRotateValue;
	}

	public boolean getIsBigImage() {
		return mIsBigImage;
	}
	
	public int getType() {
		return mType;
	}
	
}
