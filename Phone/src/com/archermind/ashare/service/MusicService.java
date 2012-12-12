package com.archermind.ashare.service;

import java.util.Map;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import archermind.dlna.mobile.MessageDefs;

public class MusicService extends Service{

	private final static String TAG = "MusicService";
	protected boolean mIsBound = false;
	private Messenger mService = null;
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		getApplicationContext().startService(new Intent(getApplicationContext(), DLNAService.class));
		bind2RendererService();
		Log.e("james","misbound:" + mIsBound);
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unbind2RendererService();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void bind2RendererService() {
		mIsBound = getApplicationContext().bindService(new Intent(this, DLNAService.class), 
        		mServConn, BIND_AUTO_CREATE);
		Log.d(TAG, "mIsBound: " + mIsBound);
	}
	
	private void unbind2RendererService() {		
		if (mIsBound && (mService != null)) {
			//unsubscribeDeviceList();
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_SERVICE_UNREGISTER_CLIENT);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {		
			}
			Log.d(TAG, "unbind2RendererService!");
			getApplicationContext().unbindService(mServConn);			
			mIsBound = false;
		}
	}
	
	private ServiceConnection mServConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "onServiceConnected!");
			mService = new Messenger(service);
			try {
			    Message msg = Message.obtain(null,
			            MessageDefs.MSG_SERVICE_REGISTER_CLIENT);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			} 
			//subscribeDeviceList();
			MusicService.this.onServiceConnected();
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {	
			Log.d(TAG, "onServiceDisconnected!");			
			mService = null;
			MusicService.this.onServiceDisconnected();
		}	
	};
	

	protected void onServiceConnected() {

	}

	protected void onServiceDisconnected() {

	}
	
	class IncomingHandler extends Handler {
	       @SuppressWarnings("unchecked")
			@Override
	        public void handleMessage(Message msg) {
	          switch (msg.what) {
				case MessageDefs.MSG_MDMC_ON_SEARCH_RESPONSE:
//					onSearchResponse(msg);
					break;
				case MessageDefs.MSG_MDMC_ON_DEV_ADDED:
//					onDeviceAdded(msg);
					break;
				case MessageDefs.MSG_MDMC_ON_DEV_REMOVED:
//					onDeviceRemoved(msg);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_CONENT:
//					onGetContent(msg);
					break;  
				case MessageDefs.MSG_SERVICE_ON_DEVICE_CHANGED:
//					onDeviceChanged(msg);
					break;
				case MessageDefs.MSG_SERVICE_ON_GET_LOCAL_MDMS_STATUS:
//					onGetLocalMDMSStatus(msg);
					break;
				case MessageDefs.MSG_SERVICE_ON_LOCAL_MDMS_STATUS_CHANGED:
//					onLocalMDMSStatusChanged(msg);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_MUSIC_CATEGORY_DATA:
//					onGetMusicCategoryData((ArrayList<MusicCategoryInfo>)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_MUSIC_ARTISTS_DATA:
//					onGetMusicArtistsData((ArrayList<Artist>)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_MUSIC_ALBUMS_DATA:
//					onGetMusicAlbumsData((ArrayList<Album>)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_MUSIC_ALL_DATA:
//					onGetMusicAllData((ArrayList<MusicItem>)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_VIDEOS_DATA:
//					onGetVideoCategory((ArrayList<VideoCategory>)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_PTOTOS_DATA:
//					onGetPhotos((ArrayList<PhotoAlbum>)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_PLAY:
//					onGetPlayresult((Boolean)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_PAUSETOPLAY:
//					onGetPauseTOPlayresult((Boolean)msg.obj);
				break;
				case MessageDefs.MSG_MDMC_ON_GET_STOP:
//					onGetStopresult((Boolean)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_PAUSE:
//					onGetPouseresult((Boolean)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_NEXT:
//					onGetNextresult((Boolean)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_PREVIOUS:
//					onGetPreviousresult((Boolean)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_GETPOSITIONINFO:
//					onGetPositioninforesult((Map)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_SEEK:
//					onGetSeekresult((Boolean)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_SETAVTRANSPORTURI:
//					onGetSetAVtransresult((Boolean)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_SETVOLUME:
//					onGetSetvolumeresult((Boolean)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_GETVOLUME:
//					onGetGetvolumeresult((String)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_SETMUTE:
//					onGetSetmuteresult((Boolean)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_GETMUTE:
//					onGetGetmuteresult((String)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_GETCURRENTTRANSPORTACTIONS:
//					onGetActionsresult((String)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_GETTRANSPORTINFO:
					onGettransinforesult((Map)msg.obj);
					break;
					
				case MessageDefs.MSG_MDMC_ON_GET_IMAGESEEK:
//					onGetImageresult((Boolean)msg.obj);
					break;
				case MessageDefs.MSG_MDMC_ON_GET_SETPLAYMODE:
//					onGetSetplayModeresult((Boolean)msg.obj);
					break;
				case MessageDefs.MSG_SERVICE_ON_GET_DEVICE_LIST:
//					onGetDeviceList((ArrayList<DeviceInfo>)msg.obj);
					break;
				case MessageDefs.MSG_SERVICE_ON_DEVICE_CONNECTED:
//					onDeviceConnected((DeviceInfo)msg.obj);
					break;
				case MessageDefs.MSG_SERVICE_ON_GET_IP_OF_CURRENT_RENDERER:
//					onGetIPAddrOfCurrentRenderer((String)msg.obj);
					break;
				case MessageDefs.MSG_SERVICE_ON_QUITED:
//					onServiceQuited();
					break;
				case MessageDefs.MSG_SERVICE_ON_GET_NAME_OF_CURRENT_RENDERER:
//				    onGetFriendlyName((String)msg.obj);
				    break;
				default:
				    super.handleMessage(msg);
	            }
	        }
	    }
	
	protected void postPlay(String uri,String type){
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_PLAY);
			    Bundle data = new Bundle();
			    data.putString(MessageDefs.KEY_ITEM_URI, uri);
			    data.putString("type", type);
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    msg.setData(data);
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}		
	}
	
	protected void postNext(String uri,String type){
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_NEXT);
			    Bundle data = new Bundle();
			    data.putString(MessageDefs.KEY_ITEM_URI, uri);
			    data.putString("type", type);
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    msg.setData(data);
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	
	protected void postGetTransportInfo(){
		Log.e("james","in postGetTransportInfo");
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_GETTRANSPORTINFO);
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	
	/**@param obj
     * 	map.put("state", state);// value (stopped,playing)
	 *  map.put("statu", statu);// value (OK,error)
	 *	map.put("speed", speed);// value 1
     * 
     */
    public void onGettransinforesult(Map obj) {
		// TODO Auto-generated method stub
		
	}
	
}