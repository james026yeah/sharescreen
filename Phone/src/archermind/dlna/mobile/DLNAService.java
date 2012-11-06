package archermind.dlna.mobile;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.std.av.renderer.MediaRenderer;
import org.cybergarage.upnp.std.av.server.MediaServer;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import archermind.dlna.miscellaneous.DeviceInfo;
import archermind.dlna.process.ControllerProcess;
import archermind.dlna.process.ServerProcess;

@SuppressLint("HandlerLeak")
public class DLNAService extends Service {
	private final static String TAG = "DLNAService";	

	private final static String STANDARD_MDMS_MODEL_NAME = "aShare Media Server";
	private final static String STANDARD_DMR_MODEL_NAME = "aShare Media Renderer";
	
	private final static int TIME_INTERVAL_SEARCH_DLNA_DEV = 60000; // 60secs
	
	private WifiManager mWifiMgr = null;
	
	private Device mLocalMDMS = null;
	private DeviceList mRendererList = new DeviceList();
	private Device mCurrentRenderer = null;
	private ControllerProcess mMDMCProc;
	private ServerProcess mMDMSProc;
	private MulticastLock mMulticastLock;
	private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
	private Handler mHandler = new IncomingHandler();
	private class IncomingHandler extends Handler {    
		@Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MessageDefs.MSG_MDMC_ON_PROC_PREPARED:
            	Log.v(TAG, "receive ------------------------> MSG_MDMC_ON_PROC_PREPARED");
            	if(isWifiConnected() || isWifiApEnabled()) {
            		Log.v(TAG, "wifi is connected or AP mode enabled so start DMC procs");
					mMDMCProc.startController();
					mMDMCProc.searchDevice();
            	}
				break;           
            case MessageDefs.MSG_MDMC_ON_SEARCH_RESPONSE:            	
            case MessageDefs.MSG_MDMC_ON_DEV_ADDED:            	
            case MessageDefs.MSG_MDMC_ON_DEV_REMOVED:            	
        		if(msg.what == MessageDefs.MSG_MDMC_ON_SEARCH_RESPONSE) {
        			onSearchResponse((DeviceList)msg.obj);
        		} else if(msg.what == MessageDefs.MSG_MDMC_ON_DEV_ADDED) {
        			onDeviceAdd((Device)msg.obj);
        		} else if(msg.what == MessageDefs.MSG_MDMC_ON_DEV_REMOVED) {
        			onDeviceRemoved((Device)msg.obj);
        		}
            case MessageDefs.MSG_MDMC_ON_GET_CONENT:
            case MessageDefs.MSG_MDMC_ON_GET_MUSIC_CATEGORY_DATA:
            case MessageDefs.MSG_MDMC_ON_GET_MUSIC_ALL_DATA:
            case MessageDefs.MSG_MDMC_ON_GET_MUSIC_ALBUMS_DATA:
            case MessageDefs.MSG_MDMC_ON_GET_MUSIC_ARTISTS_DATA:
            case MessageDefs.MSG_MDMC_ON_GET_VIDEOS_DATA:
            case MessageDefs.MSG_MDMC_ON_GET_PTOTOS_DATA:
				for(Messenger messenger : mClients) {
					try {
						messenger.send(Message.obtain(msg));
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
				break;
            case MessageDefs.MSG_SERVICE_REGISTER_CLIENT:
            	mClients.add(msg.replyTo);
            	break;
            case MessageDefs.MSG_SERVICE_UNREGISTER_CLIENT:
            	mClients.remove(msg.replyTo);
            	break;
            case MessageDefs.MSG_MDMC_CONTENT_DIR_BROWSE:
            	Bundle data = msg.getData();
            	if(data != null) {
            		String devUri = data.getString(MessageDefs.KEY_DEV_URI);
            		if(devUri == null && mLocalMDMS != null)
            			devUri = mLocalMDMS.getLocation();
            		if(devUri != null) {
	            		mMDMCProc.browserContent(devUri, 
	            			data.getString(MessageDefs.KEY_OBJ_ID));
            		}
            	}
            	break;
            case MessageDefs.MSG_MDMC_AV_TRANS_PLAY:
            	data = msg.getData();
            	if(null!=mCurrentRenderer){
            	    data.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
            	    if(data != null)
            		    mMDMCProc.control(msg.getData(),MessageDefs.MSG_MDMC_AV_TRANS_PLAY);
            	}
            	break;
            case MessageDefs.MSG_MDMS_ON_PROC_PREPARED:
            	Log.v(TAG, "receive ------------------------> MSG_MDMS_ON_PROC_PREPARED");
            	if(isWifiConnected() || isWifiApEnabled()) {
            		Log.v(TAG, "wifi is connected or AP mode enabled so start M-DMS procs");
            		mMDMSProc.startMediaServer();
            	}
            	break;
            case MessageDefs.MSG_SERVICE_SUBSCRIBE_DEVICE_CHANGE:
            	onRecSubscribeDevListMsg();
            	break;
            case MessageDefs.MSG_SERVICE_UNSUBSCRIBE_DEVICE_CHANGE:
            	onRecUnsubscribeDevListMsg();
            	break;
            case MessageDefs.MSG_SERVICE_CONNECT_DEVICE:
            	onConnectDeviceMessage(msg);
            	break;
            case MessageDefs.MSG_SERVICE_QUERY_LOCAL_MDMS_STATUS:
				try {
					Message reply = new Message();
					reply.what = MessageDefs.MSG_SERVICE_ON_GET_LOCAL_MDMS_STATUS;
					reply.arg1 = (mLocalMDMS != null) ? 
							MessageDefs.LOCAL_MDMS_STATUS_ONLINE : MessageDefs.LOCAL_MDMS_STATUS_OFFLINE;
					msg.replyTo.send(reply);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
            	break;
			case MessageDefs.MSG_MDMC_GET_MUSIC_CATEGORY_DATA:
            	if (mLocalMDMS != null) {
            		mMDMCProc.getMusicCategory(mLocalMDMS);
            	}
            	break;
            case MessageDefs.MSG_MDMC_GET_MUSIC_ARTISTS_DATA:
            	if (mLocalMDMS != null) {
            		mMDMCProc.getMusicArtists(mLocalMDMS);
            	}
            	break;
            case MessageDefs.MSG_MDMC_GET_MUSIC_ALBUMS_DATA:
            	if (mLocalMDMS != null) {
            		mMDMCProc.getMusicAlbums(mLocalMDMS);
            	}
            	break;
            case MessageDefs.MSG_MDMC_GET_MUSIC_ALL_DATA:
            	if (mLocalMDMS != null) {
            		mMDMCProc.getMusicAll(mLocalMDMS);
            	}
            	break;
            case MessageDefs.MSG_MDMC_GET_VIDEOS_DATA:
            	if (mLocalMDMS != null) {
            		mMDMCProc.getVideos(mLocalMDMS);
            	}
            	break;
            case MessageDefs.MSG_MDMC_GET_PHOTOS_DATA:
            	if (mLocalMDMS != null) {
            		mMDMCProc.getPhotos(mLocalMDMS);
            	}
            	break;
  	//yexiaoyan
            case MessageDefs.MSG_MDMC_AV_TRANS_PAUSETOPLAY:
            	    Bundle bd=new Bundle();
            	    if(mCurrentRenderer!=null){
            	        bd.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
            		    mMDMCProc.control(bd,MessageDefs.MSG_MDMC_AV_TRANS_PAUSETOPLAY);
            	    }
            	break;
            case MessageDefs.MSG_MDMC_AV_TRANS_PAUSE:
            	if(mCurrentRenderer!=null){
            	    Bundle bdpause=new Bundle();
            	    bdpause.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		    mMDMCProc.control(bdpause,MessageDefs.MSG_MDMC_AV_TRANS_PAUSE);
            	 }
        		break;
            case MessageDefs.MSG_MDMC_AV_TRANS_STOP:
            	 if(mCurrentRenderer!=null){
            	     Bundle bdStop=new Bundle();
            	     bdStop.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		     mMDMCProc.control(bdStop,MessageDefs.MSG_MDMC_AV_TRANS_STOP);
            	 }
        		break;
            case MessageDefs.MSG_MDMC_AV_TRANS_GETPOSITIONINFO:
            	 if(mCurrentRenderer!=null){
            	     Bundle bdgetInfo=new Bundle();
            	     bdgetInfo.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		     mMDMCProc.control(bdgetInfo,MessageDefs.MSG_MDMC_AV_TRANS_GETPOSITIONINFO);
            	 }
        		break;
            case MessageDefs.MSG_MDMC_AV_TRANS_SEEK:
            	Bundle dataSeek = msg.getData();
            	if(dataSeek!= null && mCurrentRenderer != null){
            		dataSeek.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		    mMDMCProc.control(dataSeek,MessageDefs.MSG_MDMC_AV_TRANS_SEEK);
            	}
        		break;
            case MessageDefs.MSG_MDMC_AV_TRANS_NEXT:
            	Bundle dataNext = msg.getData();
            	if(dataNext!=null && mCurrentRenderer != null){
            		dataNext.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		    mMDMCProc.control(dataNext,MessageDefs.MSG_MDMC_AV_TRANS_NEXT);
            	}
        		break;
            case MessageDefs.MSG_MDMC_AV_TRANS_PREVIOUS:
            	Bundle dataPrevious = msg.getData();
            	if(dataPrevious!=null && mCurrentRenderer != null){
            		dataPrevious.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		    mMDMCProc.control(dataPrevious,MessageDefs.MSG_MDMC_AV_TRANS_PREVIOUS);
            	}
        		break;
            case MessageDefs.MSG_MDMC_AV_TRANS_SETAVTRANSPORTURI:
            	Bundle datasetav = msg.getData();
            	if(datasetav!=null && mCurrentRenderer != null){
            		datasetav.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		    mMDMCProc.control(datasetav,MessageDefs.MSG_MDMC_AV_TRANS_SETAVTRANSPORTURI);
            	}
        		break;
            case MessageDefs.MSG_MDMC_AV_TRANS_SETVOLUME:
            	Bundle datasetVolume = msg.getData();
            	if(datasetVolume!=null && mCurrentRenderer != null){
            		datasetVolume.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		    mMDMCProc.control(datasetVolume,MessageDefs.MSG_MDMC_AV_TRANS_SETVOLUME);
            	}
        		break;
            case MessageDefs.MSG_MDMC_AV_TRANS_GETVOLUME:
            	Bundle datagetVolume=new Bundle();
            	if(mCurrentRenderer != null){
            		datagetVolume.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		    mMDMCProc.control(datagetVolume,MessageDefs.MSG_MDMC_AV_TRANS_GETVOLUME);
            	}
        		break;
            case MessageDefs.MSG_MDMC_AV_TRANS_SETMUTE:
            	Bundle datasetMute = msg.getData();
            	if(datasetMute!=null && mCurrentRenderer != null){
            		datasetMute.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		    mMDMCProc.control(datasetMute,MessageDefs.MSG_MDMC_AV_TRANS_SETMUTE);
            	}
        		break;
            case MessageDefs.MSG_MDMC_AV_TRANS_GETMUTE:
            	Bundle datagetMute=new Bundle();
            	if( mCurrentRenderer != null){
            		datagetMute.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		    mMDMCProc.control(datagetMute,MessageDefs.MSG_MDMC_AV_TRANS_GETMUTE);
            	}
        		break;
            case MessageDefs.MSG_MDMC_AV_TRANS_GETCURRENTTRANSPORTACTIONS:
            	Bundle datagetCurrent=new Bundle();
            	if( mCurrentRenderer != null){
            		datagetCurrent.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		    mMDMCProc.control(datagetCurrent,MessageDefs.MSG_MDMC_AV_TRANS_GETCURRENTTRANSPORTACTIONS);
            	}
        		break;
            case MessageDefs.MSG_MDMC_AV_TRANS_GETTRANSPORTINFO:
            	Bundle datagetTransportinfo=new Bundle();
            	if( mCurrentRenderer != null){
            		datagetTransportinfo.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		    mMDMCProc.control(datagetTransportinfo,MessageDefs.MSG_MDMC_AV_TRANS_GETTRANSPORTINFO);
            	}
        		break;
            case MessageDefs.MSG_MDMC_AV_TRANS_SETPLAYMODE:
            	Bundle datasetPlayMode= msg.getData();
            	if(datasetPlayMode!=null && mCurrentRenderer != null){
            		datasetPlayMode.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		    mMDMCProc.control(datasetPlayMode,MessageDefs.MSG_MDMC_AV_TRANS_SETPLAYMODE);
            	}
        		break;
            case MessageDefs.MSG_MDMC_AV_TRANS_IMAGESEEK:
            	Bundle datasetImage= msg.getData();
            	if(datasetImage!=null && mCurrentRenderer != null){
            		datasetImage.putString(MessageDefs.KEY_DEV_URI, mCurrentRenderer.getLocation());
        		    mMDMCProc.control(datasetImage,MessageDefs.MSG_MDMC_AV_TRANS_SETPLAYMODE);
            	}
        		break;
            case MessageDefs.MSG_SERVICE_GET_DEVICE_LIST:
            	sendOnGetDeviceListMessage();
            	break;
            default:
                super.handleMessage(msg);
            }
        }
    }
	
	private void onLocalMDMSStatusChanged() {
		for(Messenger messenger : mClients) {
			Message msg = new Message();
			msg.what = MessageDefs.MSG_SERVICE_ON_LOCAL_MDMS_STATUS_CHANGED;
			msg.arg1 = (mLocalMDMS != null) ? 
					MessageDefs.LOCAL_MDMS_STATUS_ONLINE : MessageDefs.LOCAL_MDMS_STATUS_OFFLINE;			
			try {
				messenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void onSearchResponse(DeviceList dl) {
		// Filter stick M-DMR and local m-DMS
		DeviceList sticks = new DeviceList();
		for(int pos = 0; pos < dl.size(); ++pos) {
			Device dev = dl.getDevice(pos);
			Log.d(TAG, "onSearchResponse--> Dev name:" + dev.getFriendlyName());
			if(isStickMDMR(dev)) {
				Log.d(TAG, "onSearchResponse--> : found stick M-DMR");		
				sticks.add(dev);
			}
			
			if(isLocalMDMS(dev)) {
				Log.d(TAG, "onSearchResponse--> : found local M-DMS");
				mLocalMDMS = dev;
				onLocalMDMSStatusChanged();
			}
		}
		
		// merge stick M-DMR list
		boolean isStickListChanged = false;
		DeviceList tmpSticks = new DeviceList();
		for(int pos = 0; pos < mRendererList.size(); ++pos) {
			Device dev = mRendererList.getDevice(pos);
			Log.d(TAG, "onSearchResponse--> : old MDR name:" + dev.getLocation());
			if(isContain(sticks, dev)) {
				Log.d(TAG, "onSearchResponse--> : old MDR name:" + dev.getLocation() + " still there");
				tmpSticks.add(dev);
			} else {
				Log.d(TAG, "onSearchResponse--> : old MDR name:" + dev.getLocation() + " disapeared!!");
				isStickListChanged = true;
			}
		}
		
		for(int pos = 0; pos < sticks.size(); ++pos) {
			Device dev = sticks.getDevice(pos);
			if(!isContain(mRendererList, dev)) {
				Log.d(TAG, "onSearchResponse--> : new added MDR name:" + dev.getLocation());
				tmpSticks.add(dev);
				isStickListChanged = true;
			} 
		}
		
		if(isStickListChanged) {
			mRendererList = tmpSticks;
			for(int pos = 0; pos < mRendererList.size(); ++pos) {
				Log.d(TAG, "After Merge:" + 
						mRendererList.getDevice(pos).getLocation());
			}
			sendDeviceChangedMessage();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void onDeviceAdd(Device dev) {
		Log.v(TAG, "onDeviceAdd 0 --> : add new renderer friendName:" + dev.getFriendlyName());
		if(isStickMDMR(dev) && !isContain(mRendererList, dev)) {
			Log.d(TAG, "onDeviceAdd 1--> : add new renderer friendName:" + dev.getFriendlyName()
					+ ", loc:" + dev.getLocation());
			mRendererList.add(dev);
			sendDeviceChangedMessage();
		}
		
		if(isLocalMDMS(dev)) {
			Log.d(TAG, "onDeviceAdd--> : found local DMS !!!:" + dev.getFriendlyName()
					+ ", loc:" + dev.getLocation());
			mLocalMDMS = dev;
			onLocalMDMSStatusChanged();
		}		
	}
	
	private void onDeviceRemoved(Device dev) {
		if(isStickMDMR(dev) && isContain(mRendererList, dev)) {
			if(mCurrentRenderer != null && 
					mCurrentRenderer.getLocation().equals(dev.getLocation())) {
				Log.v(TAG, "onDeviceRemoved--> current renderer removed");
				mCurrentRenderer = null;
			}
			Log.v(TAG, "onDeviceRemoved--> renderer removed loc:" + dev.getLocation());
			removeFromDeviceList(mRendererList, dev);
			sendDeviceChangedMessage();
		}
		
		if(isLocalMDMS(dev)) {
			Log.v(TAG, "onDeviceRemoved--> local DMS removed loc:" + dev.getLocation());
			mLocalMDMS = null;
			onLocalMDMSStatusChanged();
		}
	}
	
	private boolean isStickMDMR(Device dev) {
		return (dev != null && dev.getDeviceType().equals(MediaRenderer.DEVICE_TYPE) &&
				dev.getModelName().equals(STANDARD_DMR_MODEL_NAME));
	}
	
	private boolean isLocalMDMS(Device dev) {
		return (dev != null && dev.getDeviceType().equals(MediaServer.DEVICE_TYPE) && 
				dev.getModelName().equals(STANDARD_MDMS_MODEL_NAME) && 
				dev.getLocation().contains(getLocalIpAddress()));
	}
	
	private boolean isContain(DeviceList dl, Device target) {
		boolean isContain = false;
		if(dl != null && target != null) {
			for(int pos = 0; pos < dl.size(); ++pos) {
				Device dev = dl.getDevice(pos);
				if(dev.getLocation().equals(target.getLocation())) {
					isContain = true;
					break;
				}
			}
		}
		return isContain;
	}    

    
    private void removeFromDeviceList(DeviceList dl, Device dev) {
    	if(dl == null || dev == null || dl.size() == 0) return;
    	for(int pos = 0; pos < dl.size(); ++pos) {
    		Device device = dl.getDevice(pos);
    		if(device.getLocation().equals(dev.getLocation())) {
    			mRendererList.remove(device);
    			break;
    		}
    	}
    }

	private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent intent) {
        	String action = intent.getAction();
        	if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
        		int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
        		Log.v(TAG, "WIFI_STATE_CHANGED_ACTION: wifi state --> " + wifiState);
        		if(wifiState == WifiManager.WIFI_STATE_DISABLED && !isWifiApEnabled()) {
        			Log.v(TAG, "wifi disabled and wifi AP mode disable, clear states and stop procs");
        			stopProcs();
        		}
        	} else if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
        		NetworkInfo networkInfo = (NetworkInfo)intent.getParcelableExtra(
        				WifiManager.EXTRA_NETWORK_INFO);
        		DetailedState detailState = networkInfo.getDetailedState();
        		Log.v(TAG, "NETWORK_STATE_CHANGED_ACTION : detailState:" + detailState.toString());
        		if(DetailedState.CONNECTED == detailState) {
        			startProcs();
        		} else if(DetailedState.DISCONNECTED == detailState && !isWifiApEnabled()){
        			Log.v(TAG, "wifi disconnected and wifi AP mode disabled, clear states and stop procs");
        			stopProcs();
        		}
        	}
        }
    };

	private Runnable mDLNAScanner = new Runnable() {
		@Override
		public void run() {
			Log.v(TAG, "Execute DLNA Scanner now!");
			if(mMDMCProc != null)
				mMDMCProc.searchDevice();
			mHandler.postDelayed(mDLNAScanner, TIME_INTERVAL_SEARCH_DLNA_DEV);
		}
	};

	final Messenger mMessenger = new Messenger(mHandler);
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}
	
	@Override
    public void onCreate() {	
		super.onCreate();
		mWifiMgr = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		registerReceiver(mWifiReceiver, filter);

		accquireMCLock();
		Log.d(TAG, "onCreate() start Procs");
		startProcs();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mWifiReceiver);
		releaseMCLock();
		Log.d(TAG, "onDestroy() stop procs:");
		stopProcs();
	}
	
	private void startProcs() {
		Log.v(TAG, "------------------------start Procs()");
		if(mMDMCProc == null) {
			Log.v(TAG, "------------------------start ControllerProcess()");
			mMDMCProc = new ControllerProcess(mHandler);
			mMDMCProc.start();
		}
		if(mMDMSProc == null) {
			Log.v(TAG, "------------------------start ServerProcess()");
			mMDMSProc = new ServerProcess(mHandler, getApplicationContext());
			mMDMSProc.start();
		}
	}
	
	private void stopProcs() {
		// Stop the DLNA PROCS and clear DLNA related datas
    	Log.v(TAG, "------------------------stopProcs()");
		mLocalMDMS = null;
		onLocalMDMSStatusChanged();
		mRendererList.clear();
		mCurrentRenderer = null;	
		sendDeviceChangedMessage();

		if(mMDMCProc != null) {
			Log.v(TAG, "------------------------stop ControllerProcess()");
			mMDMCProc.stopProcess();
			mMDMCProc = null;
		}
		
		if(mMDMSProc != null) {
			Log.v(TAG, "------------------------stop ServerProcess()");
			mMDMSProc.stopProcess();
			mMDMSProc = null;
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand Current Thread ID:" + Thread.currentThread().getId());
		return START_STICKY;
	}
	
    private void accquireMCLock() {   
        if(mWifiMgr != null)  {
	        mMulticastLock = mWifiMgr.createMulticastLock("AShare.DMR");   
	        mMulticastLock.acquire();   
        }
    }
    
    private void releaseMCLock() {   
		 if(mMulticastLock != null) {
			 mMulticastLock.release();  
		 }
    }
    
    private void onRecSubscribeDevListMsg() {
    	// scan DLNA devices in preset period
    	mHandler.post(mDLNAScanner);	
    }
    
    private void onRecUnsubscribeDevListMsg() {
    	mHandler.removeCallbacks(mDLNAScanner);
    }
    
    private void sendDeviceChangedMessage() {
    	ArrayList<DeviceInfo> devInfoList = new ArrayList<DeviceInfo>();
    	int pos;
    	for(pos = 0; pos < mRendererList.size(); ++pos) {
    		Device dev = mRendererList.getDevice(pos);
    		String devName =
    				(mRendererList.size() == 1) ? dev.getFriendlyName() : (dev.getFriendlyName() + " " + Integer.toString(pos + 1));
    		int state = (mCurrentRenderer == dev) ? DeviceInfo.DEV_STATE_CONNECTED :
    				DeviceInfo.DEV_STATE_DISCONNECTED;
    		devInfoList.add(new DeviceInfo(devName, DeviceInfo.DEV_TYPE_RENDERER, 
    				state, dev.getLocation()));
    	}
    	
    	for(Messenger messenger : mClients) {
			try {
				messenger.send(Message.obtain(null, 
						MessageDefs.MSG_SERVICE_ON_DEVICE_CHANGED, devInfoList));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
    }
    
    private void sendOnGetDeviceListMessage() {
    	ArrayList<DeviceInfo> devInfoList = new ArrayList<DeviceInfo>();
    	for(int pos = 0; pos < mRendererList.size(); ++pos) {
    		Device dev = mRendererList.getDevice(pos);
    		String devName =
    				(mRendererList.size() == 1) ? dev.getFriendlyName() : (dev.getFriendlyName() + " " + Integer.toString(pos + 1));
    		int state = (mCurrentRenderer == dev) ? DeviceInfo.DEV_STATE_CONNECTED :
    				DeviceInfo.DEV_STATE_DISCONNECTED;
    		devInfoList.add(new DeviceInfo(devName, DeviceInfo.DEV_TYPE_RENDERER, 
    				state, dev.getLocation()));
    	}
    	
    	for(Messenger messenger : mClients) {
			try {
				messenger.send(Message.obtain(null, 
						MessageDefs.MSG_SERVICE_ON_GET_DEVICE_LIST, devInfoList));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
    	Log.v(TAG, "sendOnGetDeviceListMessage() devInfoList.size:" + devInfoList.size());
    }
    
    private void onConnectDeviceMessage(Message msg) {
    	DeviceInfo info = (DeviceInfo) msg.obj;
    	Log.v(TAG, "onConnectDeviceMessage---------------> connect to DEV:" + info.mDevName);
    	for(int pos = 0; pos < mRendererList.size(); ++pos) {
    		Device dev = mRendererList.getDevice(pos);
    		if(dev.getLocation().equals(info.mLocation)) {
    			Log.v(TAG, "onConnectDeviceMessage---------------> found the dev location:" +
    					dev.getLocation());
    			mCurrentRenderer = dev;
    			break;
    		}
    	}
    	for(Messenger messenger : mClients) {
			try {
				Message response = Message.obtain(null, 
						MessageDefs.MSG_SERVICE_ON_DEVICE_CONNECTED, info);
				messenger.send(response);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
    }
    
    public String getLocalIpAddress() {
    	String availableIp = null;
		Enumeration<NetworkInterface> interfaces = null;
			try {
				//the WiFi network interface will be one of these.
				interfaces = NetworkInterface.getNetworkInterfaces();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			} 
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				//since each interface could have many InetAddresses...
				for (InetAddress inetAddress : Collections.list(iface.getInetAddresses())) {
					if (inetAddress == null)
						continue;
					if (isUsableAddress(inetAddress)) {
						availableIp = inetAddress.getHostAddress();
					}
				}
			}
		return availableIp;
	 }
    
    private static boolean isUsableAddress(InetAddress address) {
	    if (!(address instanceof Inet4Address) || "127.0.0.1".equals(address.getHostAddress())) {
	        //Log.d(TAG,"Skipping unsupported non-IPv4 address: " + address);
	        return false;
	    }
	    return true;
	}

	protected boolean isWifiConnected() {
		boolean isConnected = false;
		if(mWifiMgr != null) {
			WifiInfo wifiInfo = mWifiMgr.getConnectionInfo();
			if(wifiInfo != null && wifiInfo.getBSSID() != null && 
					wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
				isConnected = true;
			}
		}
		return isConnected;
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
