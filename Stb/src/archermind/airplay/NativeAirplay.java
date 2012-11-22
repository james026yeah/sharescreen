package archermind.airplay;

import android.util.Log;

public class NativeAirplay {
	
	static {
		try {
			System.loadLibrary("stlport_shared");
			System.loadLibrary("airplay");
		} catch (UnsatisfiedLinkError e) {
			Log.d("airplay", "load airplay so error");
		}
   }
	
    public native static int doCallBackWork(AirplayProcess airplayobject);
    public native static boolean startService();
    public native static void stopService();
}
