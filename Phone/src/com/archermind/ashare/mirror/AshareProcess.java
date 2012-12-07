package com.archermind.ashare.mirror;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import com.archermind.ashare.mirror.AShareJniCallBack.AShareJniCallBackListener;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.widget.Toast;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import archermind.dlna.mobile.R;


public class AshareProcess implements AShareJniCallBackListener {
	private final static String TAG = "AshareProcess";
	private Activity mActivity;
	private AShareJniCallBack mJniCallback;
	private Display mDisplay;
	private String mCurrentSTBIp;
	private boolean running = false;
	private static AshareProcess sAshareProcess;
	private MyOrientationEventListener mOrentationListener;
	
	private AshareProcess(Activity activity) {
		mActivity = activity;
		mJniCallback = AShareJniCallBack.getInstance();
		mJniCallback.addCallBackListener(this);
	}
	
	public static AshareProcess newInstance(Activity activity){
		if (sAshareProcess == null) {
			sAshareProcess = new AshareProcess(activity);
		}
		return sAshareProcess;
	}
	
    public boolean isRunning() {
    	return running;
    }
    
    public void stopAshareMirror() {
    	NativeAshare.stopShare();
    }

	public void startAshareMirror(String ip) {
		mCurrentSTBIp = ip;
		if (makeFbReadable() && mCurrentSTBIp != null) {
			mDisplay = mActivity.getWindowManager().getDefaultDisplay();
			mOrentationListener = new MyOrientationEventListener(mActivity);
			mOrentationListener.enable();
			NativeAshare.setRotate(mDisplay.getRotation());
			// stb ip
			NativeAshare.shareScreen(mJniCallback, mCurrentSTBIp);
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_HOME);
			mActivity.startActivity(intent);
		} else {
			Log.d(TAG,"fb open failure!!! or ip == null");
			Toast.makeText(mActivity, R.string.ashare_need_root, Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public void onAShareClientConnected() {
		Log.d(TAG,"jni_debug onAShareClientConnected...phone....");
		running = true;
	}

	@Override
	public void onAShareClientDisconnected() {
		Log.d(TAG,"jni_debug onAShareClientDisconnected...phone....");
		running = false;
		mOrentationListener.disable();
	}
	
	private boolean makeFbReadable() {
		Log.d(TAG, "makeFbReadable: ......");
		boolean success = false;
		Process process = null;
		DataOutputStream os = null;
		int exitValue = 0;
		try {
			if (isRooted()) {
				process = Runtime.getRuntime().exec("su");
				os = new DataOutputStream(process.getOutputStream());
				os.writeBytes("chmod 666 /dev/graphics/fb0 \n");
				os.writeBytes("exit\n");
				os.flush();
				exitValue = process.waitFor();
				Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@ exitValue::" + exitValue);

				if (exitValue == 0) {
					success = true;
				}
			}
		} catch (Exception e) {
		} finally {
			//mIsRoot = true;
		}
		return success;
	}
	
	public boolean isRooted() {
		DataInputStream stream;
		boolean flag = false;
		try {
			stream = terminal("ls /data/");
			if (stream.readLine() != null)
				flag = true;
			Log.d(TAG, "Root flag: " + flag);
		} catch (Exception e1) {
			e1.printStackTrace();

		}

		return flag;
	}
	
	public DataInputStream terminal(String command) throws Exception {
		Process process = Runtime.getRuntime().exec("su");
		OutputStream outstream = process.getOutputStream();
		DataOutputStream DOPS = new DataOutputStream(outstream);
		InputStream instream = process.getInputStream();
		DataInputStream DIPS = new DataInputStream(instream);
		String temp = command + "\n";
		DOPS.writeBytes(temp);
		DOPS.flush();
		DOPS.writeBytes("exit\n");
		DOPS.flush();
		process.waitFor();
		return DIPS;
	}
	
	private class MyOrientationEventListener extends OrientationEventListener {
		
		private int mOrientation = 0;
		private int mRotationSent2Server = 0;
		public MyOrientationEventListener(Context context) {
			super(context);
		}
		@Override
		public void onOrientationChanged(int orientation) {
			// We keep the last known orientation. So if the user first orient
			// the camera then point the camera to floor or sky, we still have
			// the correct orientation.
			if (orientation == ORIENTATION_UNKNOWN) {
				return;
			} else {
				if (mOrientation != mDisplay.getRotation()) {
						switch (mDisplay.getRotation()) {
						case Surface.ROTATION_0:
							mRotationSent2Server = 0;
							break;
						case Surface.ROTATION_90:
							mRotationSent2Server = 90;
							break;
						case Surface.ROTATION_180:
							mRotationSent2Server = 180;
							break;
						case Surface.ROTATION_270:
							mRotationSent2Server = 270;
							break;
						default:
							break;
						
					}
					Log.d(TAG,"onOrientationChanged=" + orientation + " mDisplay.getRotation=" + mDisplay.getRotation());
					mOrientation = mDisplay.getRotation();
					NativeAshare.setRotate(mRotationSent2Server);
				}
			}
		}
	}
}