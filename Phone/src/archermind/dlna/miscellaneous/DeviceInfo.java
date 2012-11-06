package archermind.dlna.miscellaneous;

import java.io.Serializable;


public class DeviceInfo implements Serializable {
	public final static int DEV_TYPE_AP = 0;
	public final static int DEV_TYPE_RENDERER = 1;	
	
	public final static int DEV_STATE_CONNECTED = 0;
	public final static int DEV_STATE_CONNECTING = 1;
	public final static int DEV_STATE_DISCONNECTED = 2;
	public final static int DEV_STATE_REMEMBERED = 3;
	public DeviceInfo(String name, int devType, String bssid, int state) {
		mDevName = name;
		mDevType = devType;
		mBSSID = bssid;
		mState = state;
	}

	public DeviceInfo(String name, int devType, int state, String location) {
		mDevName = name;
		mDevType = devType;
		mLocation = location;
		mState = state;
	}

	public String mDevName;
	public int mDevType;
	public String mBSSID;		// for AP only
	public String mLocation;		// for Renderer only
	public int mState;
	public String mPassword;		// for AP only
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Device Name: ").append(mDevName).append("\n");
		if(mDevType == DEV_TYPE_AP) {
			sb.append("Device Type: Stick AP \n");
			sb.append("BSSID: ").append(mBSSID).append("\n");
		} else if(mDevType == DEV_TYPE_RENDERER) {
			sb.append("Device Type: Stick DLNA Renderer").append("\n");
			sb.append("Location: ").append(mLocation).append("\n");
		} else {
			sb.append("Device Type: Unkonwn device!!!!!!!!!!!!!\n");
		}
		if(mState == DEV_STATE_CONNECTED) {
			sb.append("State: CONNECTED\n");
		} else if(mState == DEV_STATE_CONNECTING) {
			sb.append("State: CONNECTING ... \n");
		} else if(mState == DEV_STATE_DISCONNECTED) {
			sb.append("State: DISCONNECTED\n");
		} else if(mState == DEV_STATE_REMEMBERED) {
			sb.append("State: REMEMBERED\n");
		} else {
			sb.append("State: Unknown!!!!!!!!!!!!!!1\n");
		}
		
		return sb.toString();		
	}
}
