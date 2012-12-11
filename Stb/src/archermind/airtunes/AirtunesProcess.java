package archermind.airtunes;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import archermind.airplay.BonjourAgentRegister;

import com.archermind.ashare.TypeDefs;

public class AirtunesProcess extends HandlerThread {
	private final static String TAG = "AirtunesProcess";
	public final static int MSG_AIRTUNES_PREPARED = 400;
	private final static int MSG_START_TUNESAIRPLAY = 0;
	private final static int MSG_STOP_TUNESAIRPLAY = 1;
	private final static int MSG_STOP_PROCESS = 2;
	public final static int MSG_AIRTUNES_PLAY = 401;
	private Handler mHandler;
	private Handler mUIHandler;
	private Context mContext;
	private BonjourAgentRegister mBAR;
	public static LinkedList<byte[]> backStack = new LinkedList();
	public static boolean available = false;

	private Handler.Callback mCb = new Handler.Callback() {

		public boolean handleMessage(Message msg) {
			boolean ret = false;
			switch(msg.what) {
			case MSG_START_TUNESAIRPLAY:
				String deviceName = (String)msg.obj;
				Log.d(TAG, "start airplay! deviceName=" + deviceName);
				WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
				WifiInfo info = wifi.getConnectionInfo();
				String macAddr = info.getMacAddress();
			    StringTokenizer st = new StringTokenizer(macAddr, ":", false);
				String tMac = "";
				while (st.hasMoreElements()) {
					tMac += st.nextElement();
				}

				if(NativeAirtunes.startAirtunes(tMac,BonjourAgentRegister.AIRTUNES_SERVICE_PORT) == 0) {
					startBonjour(deviceName);
				}
				ret = true;
				break;
			case MSG_STOP_TUNESAIRPLAY:
				Log.d(TAG, "stop airplay!");
				NativeAirtunes.stopAirtunes();
				if(mBAR != null)
				    mBAR.unRegisterBonjourService();
				ret = true;
				break;
			case MSG_STOP_PROCESS:
				AirtunesProcess.this.quit();
				ret = true;
				break;
			}
			return ret;
		}		
	};
	
	public AirtunesProcess(Handler uiHandler, Context context) {
		super(TAG);
		mUIHandler = uiHandler;
		mContext = context;
	}
	
    @Override
    protected void onLooperPrepared() {
    	mHandler = new Handler(getLooper(), mCb);
    	NativeAirtunes.doCallBackWork(AirtunesProcess.this);
    	if(null != mUIHandler)
    		mUIHandler.sendEmptyMessage(MSG_AIRTUNES_PREPARED);
    }
    
    public void startAirplay(String deviceName) {
    	Log.d(TAG,"startAirplay......" + deviceName);
    	if(null != mHandler) {
    		Message msg = new Message();
    		msg.what = MSG_START_TUNESAIRPLAY;
    		msg.obj = deviceName;
    		mHandler.sendMessage(msg);
    	}
    }
    
    public void stopAirplay() {
    	if(null != mHandler) {
    		mHandler.sendEmptyMessage(MSG_STOP_TUNESAIRPLAY);
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
     		WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
    		WifiInfo info = wifi.getConnectionInfo();
    		String macAddr = info.getMacAddress();
     		Map<String, String> map = new HashMap<String, String>();
			map.put("tp", "UDP");
			map.put("sm", "false");
			map.put("sv", "false");
			map.put("ek", "0,1");
			map.put("et", "0,1");  
			map.put("cn", "1");      
			map.put("ch", "2");               
			map.put("ss", "16");       
			map.put("sr", "44100");            
			map.put("pw","false");                                  
			map.put("vn", "3");
			map.put("da", "true");
			map.put("md", "0,1,2");
			map.put("txtvers", "1"); 
     		boolean ok = mBAR.registerBonjourService(BonjourAgentRegister.AIRTUNES_SERVICE_TYPE, macAddr+"@"+BonjourAgentRegister.AIRTUNES_SERVICE_NAME, BonjourAgentRegister.AIRTUNES_SERVICE_PORT, map);
     		if (ok) {
     			Log.d(TAG, "register Bonjour service ok! deviceName=" + deviceName);
     		} else {
     			Log.d(TAG, "register Bonjour service error! deviceName=" + deviceName);
     		}
     	}
     }; 
     thread.start();
    }
    
	 public void setAlbum(String album_name, int length)
	 {
		 Log.d("AIRTUNES_JAVA","album_name = "+album_name);
	 }
	 public void setTitle(String song_name, int length)
	 {
		 Log.d("AIRTUNES_JAVA","song_name = "+song_name);
	 }
	 public void setArtist(String singer_name, int length)
	 {
		 Log.d("AIRTUNES_JAVA","singer_name = "+singer_name);
	 }
	 public void setAlbumThumb(byte[] buf, int length)
	 {
		 Log.d("AIRTUNES_JAVA","set artwork is be called");
	 }
	 public void setPlayStatus(int state)
	 {
		 Log.d("AIRTUNES_JAVA","playStatus = "+state);
	 }
	 public void setVolume(float vol)
	 {
		 Log.d("AIRTUNES_JAVA","volume = "+vol);
	 }
	 public void setDeviceTab(String deviceId, int length)
	 {
		 Log.d("AIRTUNES_JAVA","deviceId = "+deviceId);
	 }
    
	 public synchronized void  writeBuf(byte[] buf,int length)
	 {    
		    byte [] bytes_write_pkg = null ; 
	        bytes_write_pkg = buf.clone(); //要同步机制	    	
	        
	 
	    	backStack.add(bytes_write_pkg);
			
	    	mUIHandler.sendEmptyMessage(MSG_AIRTUNES_PLAY);
	 }
}
