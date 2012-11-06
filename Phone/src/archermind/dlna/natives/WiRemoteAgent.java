package archermind.dlna.natives;

import android.util.Log;

public class WiRemoteAgent {

	private static final String TAG = "JniAgent";

	public WiRemoteAgent() {

	}

	static {
		try {
			System.loadLibrary("wi_remote");
		} catch (java.lang.UnsatisfiedLinkError e) {
			e.printStackTrace();
		}
	}

	public static native int init();

	public static native int setKeyEvent(int isPress, int keyCode);

	public static native int setTouchEvent(int isPress, int x, int y);

	public static native int mouseEvent(int x, int y);

	public static native int mouseEvent(int x1, int y1, int x2, int y2, int a);

	public static native int gyroMouseControl(int isAirMouse);

	public static void callback(String msg) {
		Log.d(TAG, "callback: " + msg);
	}
}
