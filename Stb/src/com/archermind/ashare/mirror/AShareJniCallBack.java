package com.archermind.ashare.mirror;

import java.util.Vector;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
public class AShareJniCallBack {
	
	private static AShareJniCallBack sAshareJniCallBack;
	private static final String TAG = "AshareJniCallBack";
	private static Vector<AShareJniCallBackListener> sCallbackListener = new Vector<AShareJniCallBackListener>();
	private AShareJniCallBack() {
		
	}
	public static AShareJniCallBack getInstance() {
		if (sAshareJniCallBack == null) {
			sAshareJniCallBack = new AShareJniCallBack();
		}
		return sAshareJniCallBack;
	}
	
	public void addCallBackListener(AShareJniCallBackListener listener) {
		sCallbackListener.add(listener);
	}
	
	public void removeCallBackListener(AShareJniCallBackListener listener) {
		if (sCallbackListener.contains(listener)) {
			sCallbackListener.remove(listener);
		}
	}
	
	public void onConnectionStatusChanged(int connected) {
		Log.d(TAG,"onConnectionStatusChanged connected=" + connected);
		for (AShareJniCallBackListener lis : sCallbackListener) {
			if (connected == 1) {
				lis.onAShareServerConnected();
			} else {
				lis.onAShareServerDisconnected();
			}
		}
	}
	
	interface AShareJniCallBackListener {
		void onAShareServerConnected();
		void onAShareServerDisconnected();
	}
}
