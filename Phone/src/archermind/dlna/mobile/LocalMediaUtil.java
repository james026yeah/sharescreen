package archermind.dlna.mobile;

public class LocalMediaUtil {

	public static class Defs {
		public static final String POST_STATUS_CHANGED = "archermind.dlna.mobile.poststatuschanged";
		public static final int NOT_ANY_ONE = -1;
		public static final int PHOTO = 0;
		public static final int MUSIC = 1;
		public static final int VIDEO = 2;
	}

	public static boolean sConnected = false;

	public static String sCurrentUri = null;

	public static int sWhichOnRemote = Defs.NOT_ANY_ONE;

	public static int getWhichOnRemote() {
		return sWhichOnRemote;
	}

	public static boolean setWhichOnRemote(int which, int useless) {
		if (getConnected()) {
			sWhichOnRemote = which;
			return true;
		} else {
			sWhichOnRemote = Defs.NOT_ANY_ONE;
			return false;
		}
	}

	public static void setWhichOnRemote(int which) {
		sWhichOnRemote = which;
	}

	public static void setConnected(boolean connected) {
		sConnected = connected;
		if (!sConnected) {
			notifyDisconnectDevice();
		}
	}

	public static boolean getConnected() {
		return sConnected;
	}

	public static void setCurrentUri(String uri) {
		sCurrentUri = uri;
	}

	public static String getCurrentUri() {
		return sCurrentUri;
	}

	private static void notifyDisconnectDevice() {

	}
}