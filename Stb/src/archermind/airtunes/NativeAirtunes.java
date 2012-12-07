package archermind.airtunes;

import android.util.Log;

public class NativeAirtunes {
	static {
		try {
			//System.loadLibrary("crypto");
			//System.loadLibrary("ssl");
			//System.loadLibrary("stlport_shared"); //if natice using stlport shared libarary,open it
			//System.loadLibrary("shairport");
			System.loadLibrary("airtunes");
		} catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
		}
   }
	
	public static native int startAirtunes(String strMacAddress,int port);
	public static native int stopAirtunes();
	public native static int doCallBackWork(AirtunesProcess audioobject);
}