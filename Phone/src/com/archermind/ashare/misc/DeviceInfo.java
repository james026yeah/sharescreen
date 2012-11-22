package com.archermind.ashare.misc;

public class DeviceInfo {
	public final static int DEV_STATE_CONNECTED = 0;
	public final static int DEV_STATE_DISCONNECTED = 1;
	public DeviceInfo(String devName, int state, String udn) {
		mDevName = devName;
		mState = state;
		mUDN = udn;
	}

	public String mDevName;
	public String mUDN;
	public int mState;

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Device Name: ").append(mDevName).append("\n");
		if(mState == DEV_STATE_CONNECTED) {
			sb.append("State: CONNECTED\n");
		} else if(mState == DEV_STATE_DISCONNECTED) {
			sb.append("State: DISCONNECTED\n");
		} else {
			sb.append("State: Unknown!!!!!!!!!!!!!!\n");
		}
		return sb.toString();
	}
}
