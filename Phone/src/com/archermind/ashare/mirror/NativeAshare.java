package com.archermind.ashare.mirror;

import android.util.Log;

public class NativeAshare {
	private static final String TAG = "NativeAshare";
	static {
		try {
			System.loadLibrary("jpeg-turbo");
			System.loadLibrary("ashare_sdk");
		} catch (UnsatisfiedLinkError e) {
			Log.d(TAG, "libjpeg-turbo or libashare_sdk so error!!");
		}
   }
	
    public native static void shareScreen(AShareJniCallBack callback, String remoteHostIp);
    public native static void stopShare();
    public native static void setRotate(int rotate);
}
