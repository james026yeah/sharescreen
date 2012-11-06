package archermind.ashare;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import archermind.airplay.NativeAirplay;
import archermind.airplay.BonjourAgentRegister;
import archermind.ashare.AShareJniCallBack.AShareJniCallBackListener;
import archermind.dlna.household.DLNAPlayer;

public class AshareProcess extends HandlerThread implements AShareJniCallBackListener {
	private final static String TAG = "AshareProcess";
	
	private Handler mHandler;
	private Handler mUIHandler;
	private Context mContext;
	private AShareJniCallBack mJniCallBack;
	private static final int MSG_ASHARE_SERVER_CONNECTED = 1001;
	private static final int MSG_ASHARE_SERVER_DISCONNECTED = 1002;
	private static final int MSG_STOP_PROCESS = 1003;
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
				*/
			case MSG_STOP_PROCESS:
				NativeAshare.deinitAShareService();
				AshareProcess.this.quit();
				ret = true;
				break;
			case MSG_ASHARE_SERVER_CONNECTED:
				//NativeAshare.startDisplay(surface)
				startAshareMirror();
				break;
			case MSG_ASHARE_SERVER_DISCONNECTED:
				break;
			}
			return ret;
		}		
	};
	
	public void startAshareMirror() {
		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setClass(mContext, ScreenDisplayActivity.class);
		mContext.startActivity(intent);
	}
	
	public AshareProcess(Handler uiHandler, Context context) {
		super(TAG);
		mUIHandler = uiHandler;
		mContext = context;
	}
	
    @Override
    protected void onLooperPrepared() {
    	mHandler = new Handler(getLooper(), mCb);
    	mJniCallBack = AShareJniCallBack.getInstance();
    	mJniCallBack.addCallBackListener(this);
    	NativeAshare.initAShareService(mJniCallBack);
    }

	@Override
	public void onAShareServerConnected() {
		Log.d("jni_debug","onAShareServerConnected.......");
		if (null != mHandler) {
			mHandler.sendEmptyMessage(MSG_ASHARE_SERVER_CONNECTED);
		}
	}

	@Override
	public void onAShareServerDisconnected() {
		Log.d("jni_debug","onAShareServerDisconnected.......");
		if (null != mHandler) {
			mHandler.sendEmptyMessage(MSG_ASHARE_SERVER_DISCONNECTED);
		}
	}
    
    public void stopProcess(){
    	if (null != null) {
    		mHandler.sendEmptyMessage(MSG_STOP_PROCESS);
    	}
    }
}