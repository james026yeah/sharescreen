package archermind.dlna.household;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import archermind.airplay.AirplayProcess;
import archermind.ashare.AshareProcess;
import archermind.dlna.renderer.RendererProcess;

import com.archermind.ashare.TypeDefs;

public class RendererService extends Service {
	private final static String TAG = "RendererService";
	private final static int TIME_INTERVAL_SCAN_AP = 6000;
	public final static int MSG_REGISTER_CLIENT = 0;
	public final static int MSG_UNREGISTER_CLIENT = 1;
	private WifiManager mWifiMgr = null;	
	
	private RendererProcess mRendererProc;
	private AirplayProcess mAirplayProc;
	private AshareProcess mAShareProcess;
	private String mMediaURI;
	private int mCurrentMediaType;
	
	private MulticastLock mMulticastLock;
	private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
	private Handler mHandler = new IncomingHandler();
	private class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case TypeDefs.MSG_DMR_ON_PROC_PREPARED:
				String nameFromDb = "TV Dongle";
				Log.v(TAG, "MSG_DMR_ON_PROC_PREPARED friendname:" + nameFromDb);
				mRendererProc.startRenderer(nameFromDb);
				break;
			case AirplayProcess.MSG_AIRPLAY_PREPARED:
				mAirplayProc.startAirplay();
				break;
			case TypeDefs.MSG_DMR_AV_TRANS_SET_URI:
			case AirplayProcess.MSG_AIRPLAY_SET_URI:
				mCurrentMediaType = msg.arg1;
				mMediaURI = (String)msg.obj;
				break;
			case TypeDefs.MSG_DMR_AV_TRANS_PLAY:
			case AirplayProcess.MSG_AIRPLAY_PLAY:
				if(null == mMediaURI) {
					Log.e(TAG, "media URI should not be null when start play!!!!!");
					break;
				}
				if(mCurrentMediaType == TypeDefs.MEDIA_TYPE_DLNA_IMAGE) {
					// Play DLNA Photo
					Intent tostart = new Intent(RendererService.this, ImageShow.class);
					tostart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					tostart.putExtra(TypeDefs.KEY_MEDIA_TYPE, mCurrentMediaType);
					tostart.putExtra(TypeDefs.KEY_MEDIA_URI, mMediaURI);
					startActivity(tostart);
				} else {
					// Play DLNA Video/Music AIRPLAY Video/Music
					Intent tostart = new Intent(RendererService.this, DLNAPlayer.class);
					tostart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					tostart.putExtra(TypeDefs.KEY_MEDIA_TYPE, mCurrentMediaType);
					tostart.putExtra(TypeDefs.KEY_MEDIA_URI, mMediaURI);
					getApplicationContext().startActivity(tostart);
				}
				break;
			case AirplayProcess.MSG_AIRPLAY_SHOW_PHOTO:
				Intent tostart = new Intent(RendererService.this, ImageShow.class);
				tostart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				tostart.putExtra(TypeDefs.KEY_MEDIA_TYPE, TypeDefs.MEDIA_TYPE_AIRPLAY_IMAGE);
				tostart.putExtra(TypeDefs.KEY_AIRPLAY_IMAGE_DATA, (byte[])msg.obj);
				startActivity(tostart);
				break;
			case TypeDefs.MSG_DMR_AV_TRANS_SET_NEXT_URI:
			case TypeDefs.MSG_DMR_AV_TRANS_PAUSE_TO_PLAY:
			case TypeDefs.MSG_DMR_AV_TRANS_PLAY_TO_PAUSE:
			case TypeDefs.MSG_DMR_AV_TRANS_SEEK:
			case TypeDefs.MSG_DMR_AV_TRANS_STOP:
			case TypeDefs.MSG_DMR_RENDERER_UPDATEDATA:
			case TypeDefs.MSG_DMR_AV_TRANS_SET_VOLUME:
			case TypeDefs.MSG_DMR_AV_TRANS_SET_MUTE:
			case AirplayProcess.MSG_AIRPLAY_UPDATEDATA:
			case AirplayProcess.MSG_AIRPLAY_PLAY_TO_PAUSE:
			case AirplayProcess.MSG_AIRPLAY_PAUSE_TO_PLAY:
			case AirplayProcess.MSG_AIRPLAY_STOP:
			case AirplayProcess.MSG_AIRPLAY_SEEK:
			case AirplayProcess.MSG_AIRPLAY_SET_VOLUME:
			for(Messenger messenger : mClients) {
				try {
					messenger.send(Message.obtain(msg));
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			break;
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
			default:
				super.handleMessage(msg);
			}
		}
	}
/*	
	private void playVideo() {
		if(null != mMediaURI) {
			Intent tostart = new Intent(this, DLNAPlayer.class);
			tostart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			Bundle bundle = new Bundle();
			bundle.putString(DLNAPlayer.KEY_MEDIA_URI, mMediaURI);
			tostart.putExtras(bundle);
			getApplicationContext().startActivity(tostart);
		}
	}
	
	private void playPhoto(Message msg) {
		Intent tostart = new Intent(this, ImageShow.class);
		tostart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		if(msg.arg1 == DLNAPlayer.MSG_AV_TRANS_TYPE_NONE)
		{
			byte[] bytes = (byte[])msg.obj;
		    tostart.putExtra("bytes", bytes);
		}
		else if(msg.arg1 == DLNAPlayer.MSG_AV_TRANS_TYPE_IMAGE)
		{
			tostart.putExtra(DLNAPlayer.KEY_MEDIA_URI, msg.arg1);
		}
		tostart.putExtra(DLNAPlayer.KEY_MEDIA_TYPE, mMediaType);
		startActivity(tostart);
	}
*/
	final Messenger mMessenger = new Messenger(mHandler);
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}
	
    private Runnable mApScanner = new Runnable() {
		@Override
		public void run() {
			Log.v(TAG, "Execute Ap Scanner now!");
			if(mWifiMgr != null) {
				try {
					Method method = mWifiMgr.getClass().getMethod("startScanActive");					
					boolean bSucceed = ((Boolean)method.invoke(mWifiMgr)).booleanValue();
					Log.v(TAG, "Invoke startScanActive() bSucceed:" + bSucceed);
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			mHandler.postDelayed(mApScanner, TIME_INTERVAL_SCAN_AP);
		}    	
    };

	private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {		
			String action = intent.getAction();
			if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
				onGetScanResults(mWifiMgr.getScanResults());				
			} else if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
				int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 
						WifiManager.WIFI_STATE_UNKNOWN);
        		Log.v(TAG, "WIFI_STATE_CHANGED_ACTION: wifi state" + wifiState);
				if(wifiState == WifiManager.WIFI_STATE_DISABLED) {
					mHandler.removeCallbacks(mApScanner);
				} else if(wifiState == WifiManager.WIFI_STATE_ENABLED) {
					mHandler.removeCallbacks(mApScanner);
					mHandler.post(mApScanner);
				}
				
			} else if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
				NetworkInfo networkInfo = (NetworkInfo)intent.getParcelableExtra(
        				WifiManager.EXTRA_NETWORK_INFO);
        		DetailedState detailState = networkInfo.getDetailedState();
        		String bssid = getCurrentBSSID();
        		Log.v(TAG, "bssid:" + bssid + "detailState:" + detailState.toString());
        		if(DetailedState.CONNECTED == detailState) {
        			Log.v(TAG, "mWifiReceiver() wifi connected, so start procs");
        			startProcs();
        			mHandler.removeCallbacks(mApScanner);
        		} else if(DetailedState.DISCONNECTED == detailState){
        			Log.v(TAG, "mWifiReceiver() wifi disconnected, so stop procs!");
        			stopProcs();
        			mHandler.removeCallbacks(mApScanner);
        			mHandler.post(mApScanner);
        		}
			}
		}		
	};
	
	@Override
    public void onCreate() {
		accquireMCLock();
		// Set renderer service as foreground service
		setForeground(true);
		mWifiMgr = (WifiManager)getSystemService(WIFI_SERVICE);
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		registerReceiver(mWifiReceiver, filter);
		
		if(isWifiConnected() || isWifiApEnabled()) {
			Log.v(TAG, "onCreate() wifi connected, or WIFI ap enabled, so start procs");
			startProcs();
		} else {
			Log.v(TAG, "onCreate() wifi disconnected, so do nothing!");
			if(mWifiMgr.isWifiEnabled()) {
				mHandler.post(mApScanner);
			}
		}
	}
	
	@Override
  	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	  	Log.v(TAG, "============> RendererService.onStart");
  	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.v(TAG, "onDestroy() call stop procs");
		releaseMCLock();
		unregisterReceiver(mWifiReceiver);
		stopProcs();
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "onStartCommand Current Thread ID:" + Thread.currentThread().getId());
		return START_STICKY;
	}
	
	private void onGetScanResults(List<ScanResult> results) {
		Log.v(TAG, "SCAN_RESULTS_AVAILABLE_ACTION arrived");
		for(ScanResult scanResult : results)
			Log.v(TAG, "scanResult:" + scanResult.toString());
		if(results == null || results.size() == 0)
			return;
		String targetBssid = null;
		String passwd = null;	
		ScanResult target = null;
		for(ScanResult scanResult : results) {
			String ssid = scanResult.SSID;
			Log.v(TAG, "ssid:" + ssid + ", isSecretCode:" + isSecretCode(ssid));
			if(isSecretCode(ssid)) {
				targetBssid = ssid.substring(2, 6);
				passwd = ssid.substring(6, ssid.length());
				Log.v(TAG, "find secret code bssid:" + targetBssid + ", passwd:" + passwd);
				break;
			}
		}
		
		if(targetBssid != null) {
			for(ScanResult scanResult : results) {
				String bssid = scanResult.BSSID.substring(12, 14) +
						scanResult.BSSID.substring(15, 17);
				Log.v(TAG, "current bssid:" + bssid + ", targetBssid:" + targetBssid);
				if(bssid != null && bssid.equals(targetBssid)) {
					Log.v(TAG, "find target");
					target = scanResult;
					break;
				}
			}
		}	
		
		if(target != null) {
			connectToAp(target, passwd);
		}
	}
	
	private boolean isSecretCode(String ssid) {
		return (ssid.length() >= 6 && ssid.substring(0, 2).equals("##")) ? true : false;
	}
	
	private void connectToAp(ScanResult scanResult, String passwd) {
		if(isWifiConnected()) {
			Log.v(TAG, "-----> Already Connected do nothing!!1"); 
			return;
		}
		WifiConfiguration wifiConfig = getSavedWifiConfig(scanResult.BSSID);
		if(wifiConfig != null) {    		
    		boolean result = mWifiMgr.enableNetwork(wifiConfig.networkId, true);
    		Log.v(TAG, "Found remembered wifi config id:" + wifiConfig.networkId + 
    				" enable result:" + result);
    	} else {
    		AccessPoint ap = new AccessPoint(scanResult, passwd);
    		wifiConfig = ap.getConfig();
    		int res = mWifiMgr.addNetwork(wifiConfig);
    		Log.v(TAG, "add Network returned " + res );
    		boolean result = mWifiMgr.enableNetwork(res, true);        
    		Log.v(TAG, "enableNetwork returned " +result );
    	}
	}
	
	private WifiConfiguration getSavedWifiConfig(String bssid) {
    	WifiConfiguration wifiConfig = null;
    	List<WifiConfiguration> configs = mWifiMgr.getConfiguredNetworks();
    	for(WifiConfiguration config : configs) {
    		if(config.BSSID != null && config.BSSID.equals(bssid)) {
    			wifiConfig = config;
    			break;
    		}
    	}
    	return wifiConfig;
    }
	
	private void startProcs() {
		if(mRendererProc == null) {
			mRendererProc = new RendererProcess(mHandler);
			mRendererProc.start();
		}
		if(mAirplayProc == null) {
			mAirplayProc = new AirplayProcess(mHandler, getApplicationContext());
			mAirplayProc.start();
		}
	}
	
	private void stopProcs() {
		if(mRendererProc != null) {
			mRendererProc.stopProcess();
			mRendererProc = null;
		}
		if(mAirplayProc != null) {
			mAirplayProc.stopProcess();
			mAirplayProc = null;
		}
	}
	
    private void accquireMCLock() {   
        WifiManager wifiManager= (WifiManager)getSystemService(Context.WIFI_SERVICE);  
        mMulticastLock = wifiManager.createMulticastLock("AShare.DMR");   
        mMulticastLock.acquire();   
    }
    
    private void releaseMCLock() {   
		 if(null != mMulticastLock)
			 mMulticastLock.release();   
    }
    
    
    protected boolean isWifiConnected() {
    	boolean isConnected = false;
    	WifiInfo wifiInfo = mWifiMgr.getConnectionInfo();
    	if(wifiInfo != null && wifiInfo.getBSSID() != null && 
    			wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {    		
    		isConnected = true;
    	}
    	return isConnected;
    }
    
    protected String getCurrentBSSID() {
    	WifiInfo wifiInfo = mWifiMgr.getConnectionInfo();
    	return (wifiInfo != null) ? wifiInfo.getBSSID() : null;
    }
    
	protected boolean isWifiApEnabled() {
		boolean isEnabled = false;
		try {
			Method method = WifiManager.class.getMethod("isWifiApEnabled");
			isEnabled = ((Boolean)method.invoke(mWifiMgr)).booleanValue();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return isEnabled;
	}
}
