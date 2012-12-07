package archermind.dlna.mobile;

import java.util.Observable;

/**
 * A ZoomState holds zoom and pan values and allows the user to read and listen
 * to changes. Clients that modify ZoomState should call notifyObservers()
 */
public class ImageZoomState extends Observable {

	/**
	 * Which type of control is used
	 */
	public enum ControlType {
		PAN, ZOOM
	}

	/**
	 *  Current control type being used 
	 */
	private ControlType mControlType = ControlType.PAN;
	
	/**
	 * Zoom level A value of 1.0 means the content fits the view.
	 */
	private float mZoom;

	/**
	 * Pan position x-coordinate X-coordinate of zoom window center position,
	 * relative to the width of the content.
	 */
	private float mPanX;

	/**
	 * Pan position y-coordinate Y-coordinate of zoom window center position,
	 * relative to the height of the content.
	 */
	private float mPanY;

	/**
	 * The picture central point big figure slide to the right-most
	 */
	private float mMaxPanX = 1.0f;
	
	/**
	 * The picture central point big figure slide to the left-most
	 */
	private float mMinPanX = 0.0f;
	
	/**
	 * Enlarge slide to most top edge of the picture center point
	 */
	private float mMaxPanY = 1.0f;
	
	/**
	 * Enlarge slide to the most bottom edge of the picture when the center point
	 */
	private float mMinPanY = 0.0f;
	
	// Public methods

	/**
	 * Get current x-pan
	 * 
	 * @return current x-pan
	 */
	public float getPanX() {
		return mPanX;
	}

	/**
	 * Get current y-pan
	 * 
	 * @return Current y-pan
	 */
	public float getPanY() {
		return mPanY;
	}

	/**
	 * Get current zoom value
	 * 
	 * @return Current zoom value
	 */
	public float getZoom() {
		return mZoom;
	}

	public float getMaxPanX() {
		return mMaxPanX;
	}
	
	public float getMinPanX() {
		return mMinPanX;
	}
	
	public float getMaxPanY() {
		return mMaxPanY;
	}
	
	public float getMinPanY() {
		return mMinPanY;
	}
	
	public ControlType getControlType() {
		return mControlType;
	}
	
	/**
	 * Help function for calculating current zoom value in x-dimension
	 * 
	 * @param aspectQuotient
	 *            (Aspect ratio content) / (Aspect ratio view)
	 * @return Current zoom value in x-dimension
	 */
	public float getZoomX(float aspectQuotient) {
		return Math.min(mZoom, mZoom * aspectQuotient);
	}

	/**
	 * Help function for calculating current zoom value in y-dimension
	 * 
	 * @param aspectQuotient
	 *            (Aspect ratio content) / (Aspect ratio view)
	 * @return Current zoom value in y-dimension
	 */
	public float getZoomY(float aspectQuotient) {
		return Math.min(mZoom, mZoom / aspectQuotient);
	}

	/**
	 * Set pan-x
	 * 
	 * @param panX
	 *            Pan-x value to set
	 */
	public void setPanX(float panX) {
		if (panX != mPanX) {
			mPanX = panX;
			setChanged();
		}
	}

	/**
	 * Set pan-y
	 * 
	 * @param panY
	 *            Pan-y value to set
	 */
	public void setPanY(float panY) {
		if (panY != mPanY) {
			mPanY = panY;
			setChanged();
		}
	}

	/**
	 * Set zoom
	 * 
	 * @param zoom
	 *            Zoom value to set
	 */
	public void setZoom(float zoom) {
		if(zoom != mZoom) {
			mZoom = zoom;
			setChanged();
		}
	}
	
	/**
	 * Sets the control type to use
	 * 
	 * @param controlType
	 *            Control type
	 */
	public void setControlType(ControlType controlType) {
		mControlType = controlType;
	}
	
	public void setMaxPanX(float maxPanX) {
		mMaxPanX = maxPanX;
	}
	
	public void setMinPanX(float minPanX) {
		mMinPanX = minPanX;
	}
	
	public void setMaxPanY(float maxPanY) {
		mMaxPanY = maxPanY;
	}
	
	public void setMinPanY(float minPanY) {
		mMinPanY = minPanY;
	}
	
}
