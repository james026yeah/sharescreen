package archermind.dlna.mobile;

import android.widget.GridView;

public class VideoThumbnailGridView extends GridView
{
	public VideoThumbnailGridView(android.content.Context context,
			android.util.AttributeSet attrs)
	{
		super(context, attrs);
	}

	/**
	 * ÉèÖÃ²»¹ö¶¯
	 */
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
				MeasureSpec.AT_MOST);
		super.onMeasure(widthMeasureSpec, expandSpec);

	}

}