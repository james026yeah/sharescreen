package archermind.airplay;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import archermind.dlna.household.DLNAPlayer;
import archermind.airplay.BonjourAgentRegister;

import com.archermind.ashare.TypeDefs;

public class AirplayProcess extends HandlerThread {
	private final static String TAG = "AirplayProcess";
	
	public final static int MSG_AIRPLAY_PREPARED = 300;
	public final static int MSG_AIRPLAY_SET_URI = 301;	
	public final static int MSG_AIRPLAY_SET_NEXT_URI = 302;
	public final static int MSG_AIRPLAY_GET_MEDIA_INFO = 303;
	public final static int MSG_AIRPLAY_GET_TRANS_INFO = 304;
	public final static int MSG_AIRPLAY_GET_POS_INFO = 305;
	public final static int MSG_AIRPLAY_GET_DEV_CAPS = 306;
	public final static int MSG_AIRPLAY_GET_TRANS_SETTING = 307;
	public final static int MSG_AIRPLAY_STOP = 308;	
	public final static int MSG_AIRPLAY_PLAY = 309;
	public final static int MSG_AIRPLAY_PLAY_TO_PAUSE = 310;
	public final static int MSG_AIRPLAY_RECORD = 311;
	public final static int MSG_AIRPLAY_SEEK = 312;
	public final static int MSG_AIRPLAY_NEXT = 313;
	public final static int MSG_AIRPLAY_PREVIOUS = 314;
	public final static int MSG_AIRPLAY_SET_PLAY_MODE = 315;
	public final static int MSG_AIRPLAY_SET_REC_QULITY_MODE = 316;
	public final static int MSG_AIRPLAY_SET_VOLUME = 317;
	public final static int MSG_AIRPLAY_GET_VOLUME = 318;
	public final static int MSG_AIRPLAY_UPDATEDATA = 319;
	public final static int MSG_AIRPLAY_PAUSE_TO_PLAY = 320;
	public final static int MSG_AIRPLAY_SHOW_PHOTO = 321;
	
	private final static int MSG_START_AIRPLAY = 0;
	private final static int MSG_STOP_AIRPLAY = 1;
	private final static int MSG_STOP_PROCESS = 2;
	private Handler mHandler;
	private Handler mUIHandler;
	private Context mContext;
	BonjourAgentRegister mBAR;
	private Handler.Callback mCb = new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			boolean ret = false;
			switch(msg.what) {
			case MSG_START_AIRPLAY:
				String deviceName = (String)msg.obj;
				Log.d(TAG, "start airplay! deviceName=" + deviceName);
				if(NativeAirplay.startService()) {
					startBonjour(deviceName);
				}
				ret = true;
				break;
			case MSG_STOP_AIRPLAY:
				Log.d(TAG, "stop airplay!");
				NativeAirplay.stopService();
				if(mBAR != null)
				    mBAR.unRegisterBonjourService();
				ret = true;
				break;
			case MSG_STOP_PROCESS:
				AirplayProcess.this.quit();
				ret = true;
				break;
			}
			return ret;
		}		
	};
	
	public AirplayProcess(Handler uiHandler, Context context) {
		super(TAG);
		mUIHandler = uiHandler;
		mContext = context;
	}
	
    @Override
    protected void onLooperPrepared() {
    	mHandler = new Handler(getLooper(), mCb);
    	NativeAirplay.doCallBackWork(AirplayProcess.this);
    	if(null != mUIHandler)
    		mUIHandler.sendEmptyMessage(MSG_AIRPLAY_PREPARED);
    }
    
    public void startAirplay(String deviceName) {
    	Log.d(TAG,"startAirplay......" + deviceName);
    	if(null != mHandler) {
    		Message msg = new Message();
    		msg.what = MSG_START_AIRPLAY;
    		msg.obj = deviceName;
    		mHandler.sendMessage(msg);
    	}
    }
    
    public void stopAirplay() {
    	if(null != mHandler) {
    		mHandler.sendEmptyMessage(MSG_STOP_AIRPLAY);
    	}
    }    
    
    public void stopProcess() {
    	stopAirplay();  
    	if(null != mHandler) {
    		mHandler.sendEmptyMessage(MSG_STOP_PROCESS);
    	}   
    }
    
    public void startBonjour(final String deviceName){
        Thread thread = new Thread() {
     	public void run() {
     		if (mBAR == null) {
     			mBAR = BonjourAgentRegister.getInstance(mContext);
     		}
     		boolean ok = mBAR.registerBonjourService(deviceName);
     		if (ok) {
     			Log.d(TAG, "register Bonjour service ok! deviceName=" + deviceName);
     		} else {
     			Log.d(TAG, "register Bonjour service error! deviceName=" + deviceName);
     		}
     	}
     }; 
     thread.start();
    }
    
    public void setResourceUrl(String uri)
    {
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
        Log.v("EagleTag","back message:"+uri);
        Message msg = new Message();
        msg.what = MSG_AIRPLAY_SET_URI;
		msg.obj = uri;
		msg.arg1 = TypeDefs.MEDIA_TYPE_AIRPLAY_VIDEO;
		if(null != mUIHandler)
	    	mUIHandler.sendMessage(msg); 
    }
    
    public void airplayPlay()
    {
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
		mUIHandler.sendEmptyMessage(MSG_AIRPLAY_PLAY);
    }
    
    public void setRate(int rate)
    {
    	if(DLNAPlayer.mIsPlayCompletion)
    		return;
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
    	if(1 == rate)
    	{
    		mUIHandler.sendEmptyMessage(MSG_AIRPLAY_PAUSE_TO_PLAY);
    	}
    	else
    	{
    		mUIHandler.sendEmptyMessage(MSG_AIRPLAY_PLAY_TO_PAUSE);
    	}
    }

    public boolean IsPlayCompletion()
    {
    	return DLNAPlayer.mIsPlayCompletion;
    }
    
    public void setVolume(int volume)
    {
    	if(DLNAPlayer.mIsPlayCompletion)
    		return;
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
        Log.v("EagleTag","back message:"+volume);
        Message msg = new Message();
        msg.what = MSG_AIRPLAY_SET_VOLUME;
		msg.arg1 = volume;
		if(null != mUIHandler)
	    	mUIHandler.sendMessage(msg);  
		
    }
    
    public int getVolume()
    {
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
    	mUIHandler.sendEmptyMessage(MSG_AIRPLAY_UPDATEDATA);
    	return (int) DLNAPlayer.mVolume;
    }
    
    public void playVideo(String url,int length,float startPositon)
    {
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
    	Log.v("EagleTag","Callback java function:"+url+startPositon);
    	setResourceUrl(url);
    	airplayPlay();
    	seekPosition((startPositon/100) * getTotalTime());
    	DLNAPlayer.mIsPlayCompletion = false;
    }

    public float getTotalTime()
    {
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
    	mUIHandler.sendEmptyMessage(MSG_AIRPLAY_UPDATEDATA);
    	return DLNAPlayer.mTotalTime;
    }
    public float getCurrentPosition()
    {
    	mUIHandler.sendEmptyMessage(MSG_AIRPLAY_UPDATEDATA);
    	return DLNAPlayer.mCurrentPosition;
    }
    public void seekPosition(float position)
    {
    	if(DLNAPlayer.mIsPlayCompletion)
    		return;
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
    	Log.v("EagleTag","back message:"+position);
        Message msg = new Message();
        msg.what = MSG_AIRPLAY_SEEK;
 		msg.arg1 = (int)position;
 		if(null != mUIHandler)
 	        mUIHandler.sendMessage(msg);  
    }

    public void stopVideo()
    {
    	if(DLNAPlayer.mIsPlayCompletion)
    		return;
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
    	DLNAPlayer.mIsPlayCompletion = true;
    	mUIHandler.sendEmptyMessage(MSG_AIRPLAY_STOP);
    }
    
    public void closeWindow()
    {
    	if(DLNAPlayer.mIsPlayCompletion)
    		return;
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
    	mUIHandler.sendEmptyMessage(MSG_AIRPLAY_STOP);
    }
  
    public void showPhoto(byte[] image,int length)
    {
    	Log.d("EagleTag","cacheBytes=" + length + " image" + image.length);
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
    	Message msg = new Message();
    	msg.obj = image;
    	msg.arg1 = TypeDefs.MEDIA_TYPE_AIRPLAY_IMAGE;
    	msg.what = MSG_AIRPLAY_SHOW_PHOTO;
    	mUIHandler.sendMessage(msg);
    }
    
    public boolean IsCaching()
    {
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
    	mUIHandler.sendEmptyMessage(MSG_AIRPLAY_UPDATEDATA);
    	return false;
    }
    
    public boolean IsPlaying()//此playing是相对于stop的
    {
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
    	mUIHandler.sendEmptyMessage(MSG_AIRPLAY_UPDATEDATA);
    	return DLNAPlayer.mIsPlay;
    }
    
    public boolean IsPaused()//此playing是相对于stop的
    {
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
    	mUIHandler.sendEmptyMessage(MSG_AIRPLAY_UPDATEDATA);
    	return DLNAPlayer.mIsPause;
    }
   
    public float getCachPosition()
    {
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
    	mUIHandler.sendEmptyMessage(MSG_AIRPLAY_UPDATEDATA);
    	return DLNAPlayer.mCurrentPosition + 5;
    }
    public String getDeviceId()//get MAC address   
    {
    	Log.v("EagleTag","Callback java function:"+Thread.currentThread().getStackTrace()[2].getMethodName());
    	return "11:22:33:44:55:66";
    }
}