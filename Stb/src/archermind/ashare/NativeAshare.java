package archermind.ashare;

import android.util.Log;
import android.view.Surface;

public class NativeAshare {
	private static final String TAG = "NativeAshare";
	static {
		try {
			System.loadLibrary("stlport_shared");
			System.loadLibrary("jpeg-turbo");
			System.loadLibrary("ashare_sdk");
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
			Log.d(TAG, "libjpeg-turbo or libashare_sdk so error!!");
		}
   }
	
    public native static void initAShareService(AShareJniCallBack callback);
    public native static void deinitAShareService();
    public native static void startDisplay(Surface surface);
    public native static void stopDisplay();
}
