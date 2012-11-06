package archermind.ashare;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class ServerScreenView extends SurfaceView implements SurfaceHolder.Callback {

	private static final String TAG = "ServerScreenView";
	SurfaceHolder mHolder;
	public ServerScreenView(Context context) {
		super(context);
		mHolder = this.getHolder();
		mHolder.addCallback(this);
		setFocusable(false);
	}
	
	public ServerScreenView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mHolder = this.getHolder();
		mHolder.addCallback(this);
		setFocusable(false);
	}
	
	
	public ServerScreenView(Context context, Runnable startServiceRunnable) {
		super(context);
		mHolder = this.getHolder();
		mHolder.addCallback(this);
		setFocusable(false);
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(TAG,"surfaceChanged....///////////////");
		NativeAshare.startDisplay(holder.getSurface());
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG,"surfaceCreated///////////////");
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG,"surfaceDestroyed............");
		NativeAshare.stopDisplay();
	}
}