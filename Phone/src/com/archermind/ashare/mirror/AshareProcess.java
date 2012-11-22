package com.archermind.ashare.mirror;

import com.archermind.ashare.mirror.AShareJniCallBack.AShareJniCallBackListener;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


public class AshareProcess extends HandlerThread implements AShareJniCallBackListener {
	private final static String TAG = "AshareProcess";
	
	private Handler mHandler;
	private Handler mUIHandler;
	private Context mContext;
	private AShareJniCallBack mJniCallBack;
	//private MyOrientationEventListener mOrentationListener;
	private Display mDisplay;
	private static final int MSG_CONNECT_REQUEST = 1001;
	public static final String ACTION_STOP_ASHARE = "stop_ashare";
	private Handler.Callback mCb = new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			boolean ret = false;
			switch(msg.what) {
			/*case MSG_START_AIRPLAY:
				Log.d(TAG, "start airplay!");
				if(NativeAirplay.startService()) {
					startBonjour();
				}
				ret = true;
				break;
			case MSG_STOP_AIRPLAY:
				Log.d(TAG, "stop airplay!");
				NativeAirplay.stopService();
				mBAR.unRegisterBonjourService();
				ret = true;
				break;
			case MSG_STOP_PROCESS:
				AshareProcess.this.quit();
				ret = true;
				break;*/
			}
			return ret;
		}		
	};
	
	public AshareProcess(Handler uiHandler, Context context) {
		super(TAG);
		mUIHandler = uiHandler;
		mContext = context;
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_STOP_ASHARE);
		mContext.registerReceiver(mStopAshareReceiver, intentFilter);
		Log.d(TAG,"registerReceiver  mStopAshareReceiver................");
	}
	
	BroadcastReceiver mStopAshareReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			if (intent.getAction().equals(ACTION_STOP_ASHARE)) {
				Log.d(TAG,"mStopAshareReceiver  onReceive................");
				NativeAshare.stopShare();
			}
		}
	};
	
    @Override
    protected void onLooperPrepared() {
    	mHandler = new Handler(getLooper(), mCb);
    	mJniCallBack = AShareJniCallBack.getInstance();
    	mJniCallBack.addCallBackListener(this);
    	//NativeAshare.shareScreen(mJniCallBack, "");
    }

	@Override
	public void onAShareClientConnected() {
		Log.d("jni_debug","jni_debug onAShareClientConnected...phone....");
	}

	@Override
	public void onAShareClientDisconnected() {
		Log.d("jni_debug","jni_debug onAShareClientDisconnected...phone....");
	}
	
	/*@Override
	public void onPrepareSurfaceRequest() {
		if (mHandler != null) {
			mHandler.sendEmptyMessage(MSG_CONNECT_REQUEST);
		}
	}*/
    
   /* public void stopVideo()
    {
    	if(DLNAPlayer.mIsPlayCompletion)
    		return;
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
    	mUIHandler.sendEmptyMessage(MSG_AIRPLAY_STOP);
    }*/
	/*private class MyOrientationEventListener extends OrientationEventListener {
		
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
					NativeAgent.commandSetRotation(mRotationSent2Server);
				}
			}
			
		}
	}*/
}