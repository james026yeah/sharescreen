package com.archermind.ashare.mirror;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.archermind.ashare.mirror.AShareJniCallBack.AShareJniCallBackListener;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import archermind.dlna.mobile.BaseActivity;
import archermind.dlna.mobile.R;
import archermind.dlna.mobile.R.layout;

public class AshareStartActivity extends BaseActivity implements OnClickListener, AShareJniCallBackListener{
	private final static String TAG = "AshareStartActivity";
	private Button mStartBtn;
	private Button mStopBtn;
	private AShareJniCallBack mJniCallback;
	private Display mDisplay;
	private String mCurrentRenderIp;
	private MyOrientationEventListener mOrentationListener;
	 public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.ashare);
	    mStartBtn = (Button) findViewById(R.id.start_btn);
	    mStopBtn = (Button) findViewById(R.id.stop_btn);
	    mStartBtn.setOnClickListener(this);
	    mStopBtn.setOnClickListener(this);
	    mJniCallback = AShareJniCallBack.getInstance();
	    mJniCallback.addCallBackListener(this);
	    mDisplay = AshareStartActivity.this.getWindowManager().getDefaultDisplay();
	    getIPAddrOfCurrentRenderer();
	 }
	 
	 
	@Override
	protected void onGetIPAddrOfCurrentRenderer(String ipAddr) {
		super.onGetIPAddrOfCurrentRenderer(ipAddr);
		mCurrentRenderIp = ipAddr;
		Log.d(TAG, "mCurrentRenderIp=" + mCurrentRenderIp);
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start_btn:
			if (makeFbReadable()/* && mCurrentRenderIp != null*/) {
				NativeAshare.setRotate(mDisplay.getRotation());
				// stb ip
				NativeAshare.shareScreen(mJniCallback, "192.168.43.1");
				mOrentationListener = new MyOrientationEventListener(AshareStartActivity.this);
				mOrentationListener.enable();
			} else {
				Log.d(TAG,"fb open failure!!!");
			}
			
			break;
		case R.id.stop_btn:
			NativeAshare.stopShare();
			if (mOrentationListener != null) {
				mOrentationListener.disable();
			}
			break;
		default:
			break;
		}
	}
	@Override
	public void onAShareClientConnected() {
		Log.d(TAG,"onAShareClientConnected...");
	}
	@Override
	public void onAShareClientDisconnected() {
		Log.d(TAG,"onAShareClientDisconnected...");
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
		// 检测是否ROOT过
		DataInputStream stream;
		boolean flag = false;
		try {
			stream = terminal("ls /data/");
			// 目录哪都行，不一定要需要ROOT权限的
			if (stream.readLine() != null)
				flag = true;
			// 根据是否有返回来判断是否有root权限
			Log.d(TAG, "Root flag: " + flag);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();

		}

		return flag;
	}
	
	public DataInputStream terminal(String command) throws Exception {
		Process process = Runtime.getRuntime().exec("su");
		// 执行到这，Superuser会跳出来，选择是否允许获取最高权限
		OutputStream outstream = process.getOutputStream();
		DataOutputStream DOPS = new DataOutputStream(outstream);
		InputStream instream = process.getInputStream();
		DataInputStream DIPS = new DataInputStream(instream);
		String temp = command + "\n";
		// 加回车
		DOPS.writeBytes(temp);
		// 执行
		DOPS.flush();
		// 刷新，确保都发送到outputstream
		DOPS.writeBytes("exit\n");
		// 退出
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