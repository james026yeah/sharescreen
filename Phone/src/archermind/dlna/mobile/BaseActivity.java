package archermind.dlna.mobile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cybergarage.upnp.Device;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.archermind.ashare.dlna.localmedia.Album;
import com.archermind.ashare.dlna.localmedia.Artist;
import com.archermind.ashare.dlna.localmedia.MusicCategoryInfo;
import com.archermind.ashare.dlna.localmedia.MusicItem;
import com.archermind.ashare.dlna.localmedia.PhotoAlbum;
import com.archermind.ashare.dlna.localmedia.VideoCategory;
import com.archermind.ashare.misc.DeviceInfo;
import com.archermind.ashare.service.DLNAService;

@SuppressLint("HandlerLeak")
public class BaseActivity extends Activity {
	private final static String TAG = "BaseActivity";
	protected boolean mIsBound = false;
	private Messenger mService = null;
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	public final String IMAGE_TYPE="IMAGE";
	public final String VIDEO_TYPE="VIDEO";
	public final String MUSIC_TYPE="MUSIC";

	/* Add by yuanhong.dai*/
	private Button mLeftBtn;
	private TextView mTitleText;
	private ImageButton mRightBtn;
	private LinearLayout mContentLayout;
	public int getmute;
	/* End by yuanhong.dai */

    class IncomingHandler extends Handler {
       @SuppressWarnings("unchecked")
		@Override
        public void handleMessage(Message msg) {
          switch (msg.what) {
			case MessageDefs.MSG_MDMC_ON_SEARCH_RESPONSE:
				onSearchResponse(msg);
				break;
			case MessageDefs.MSG_MDMC_ON_DEV_ADDED:
				onDeviceAdded(msg);
				break;
			case MessageDefs.MSG_MDMC_ON_DEV_REMOVED:
				onDeviceRemoved(msg);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_CONENT:
				onGetContent(msg);
				break;  
			case MessageDefs.MSG_SERVICE_ON_DEVICE_CHANGED:
				onDeviceChanged(msg);
				break;
			case MessageDefs.MSG_SERVICE_ON_GET_LOCAL_MDMS_STATUS:
				onGetLocalMDMSStatus(msg);
				break;
			case MessageDefs.MSG_SERVICE_ON_LOCAL_MDMS_STATUS_CHANGED:
				onLocalMDMSStatusChanged(msg);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_MUSIC_CATEGORY_DATA:
				onGetMusicCategoryData((ArrayList<MusicCategoryInfo>)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_MUSIC_ARTISTS_DATA:
				onGetMusicArtistsData((ArrayList<Artist>)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_MUSIC_ALBUMS_DATA:
				onGetMusicAlbumsData((ArrayList<Album>)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_MUSIC_ALL_DATA:
				onGetMusicAllData((ArrayList<MusicItem>)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_VIDEOS_DATA:
				onGetVideoCategory((ArrayList<VideoCategory>)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_PTOTOS_DATA:
				onGetPhotos((ArrayList<PhotoAlbum>)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_PLAY:
				onGetPlayresult((Boolean)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_PAUSETOPLAY:
				onGetPauseTOPlayresult((Boolean)msg.obj);
			break;
			case MessageDefs.MSG_MDMC_ON_GET_STOP:
				onGetStopresult((Boolean)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_PAUSE:
				onGetPouseresult((Boolean)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_NEXT:
				onGetNextresult((Boolean)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_PREVIOUS:
				onGetPreviousresult((Boolean)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_GETPOSITIONINFO:
				onGetPositioninforesult((Map)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_SEEK:
				onGetSeekresult((Boolean)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_SETAVTRANSPORTURI:
				onGetSetAVtransresult((Boolean)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_SETVOLUME:
				onGetSetvolumeresult((Boolean)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_GETVOLUME:
				onGetGetvolumeresult((String)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_SETMUTE:
				onGetSetmuteresult((Boolean)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_GETMUTE:
				onGetGetmuteresult((String)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_GETCURRENTTRANSPORTACTIONS:
				onGetActionsresult((String)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_GETTRANSPORTINFO:
				onGettransinforesult((Map)msg.obj);
				break;
				
			case MessageDefs.MSG_MDMC_ON_GET_IMAGESEEK:
				onGetImageresult((Boolean)msg.obj);
				break;
			case MessageDefs.MSG_MDMC_ON_GET_SETPLAYMODE:
				onGetSetplayModeresult((Boolean)msg.obj);
				break;
			case MessageDefs.MSG_SERVICE_ON_GET_DEVICE_LIST:
				onGetDeviceList((ArrayList<DeviceInfo>)msg.obj);
				break;
			case MessageDefs.MSG_SERVICE_ON_DEVICE_CONNECTED:
				onDeviceConnected((DeviceInfo)msg.obj);
				break;
			case MessageDefs.MSG_SERVICE_ON_GET_IP_OF_CURRENT_RENDERER:
				onGetIPAddrOfCurrentRenderer((String)msg.obj);
				break;
			case MessageDefs.MSG_SERVICE_ON_QUITED:
				onServiceQuited();
				break;
			case MessageDefs.MSG_SERVICE_ON_GET_NAME_OF_CURRENT_RENDERER:
			    onGetFriendlyName((String)msg.obj);
			    break;
			default:
			    super.handleMessage(msg);
            }
        }
    }
    protected void onServiceQuited() {
    }
    protected void onGetFriendlyName(String friendlyName) {
        Log.v(TAG, "onGetFriendlyName" + friendlyName);
    }
	protected void quit() {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_SERVICE_QUIT);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
    protected void getFriendlyNameOfRenderer() {
        if(null != mService) {
            try {
                Message msg = Message.obtain(null,
                        MessageDefs.MSG_SERVICE_GET_NAME_OF_CURRENT_RENDERER);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
            }
        }
    }
    /**
     * 
     * @param obj
     * map.put("track", track);// value (0,1)
	 * map.put("trackDuration", trackDuration);//value 音频時长
	 * map.put("trackMetaData", trackMetaData);
	 * map.put("trackURI", trackURI);
	 * map.put("relTime", relTime);// 播放过的位置
	 * map.put("absTime", absTime);// 
	 * map.put("relCount", relCount);
	 * map.put("absCount", absCount);
     */
    public void onGetPositioninforesult(Map obj) {
		// TODO Auto-generated method stub
		
	}
    public void onGetImageresult(Boolean obj) {
		// TODO Auto-generated method stub
		
	}
	public void onGetSetplayModeresult(Boolean obj) {
		// TODO Auto-generated method stub
		
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
	public void onGetActionsresult(String obj) {
		// TODO Auto-generated method stub
		
	}
	public void onGetGetmuteresult(String obj) {
		// TODO Auto-generated method stub
		
	}
	public void onGetSetmuteresult(Boolean obj) {
		// TODO Auto-generated method stub
		
	}
	public void onGetGetvolumeresult(String obj) {
		// TODO Auto-generated method stub
		if(obj!=null){
		  postSetVolume(Float.parseFloat(obj));
		}
	}
	public void onGetSetvolumeresult(Boolean obj) {
		// TODO Auto-generated method stub
		Log.d("yexiaoyan", "onGetSetvolumeresult "+obj);
		
	}
	public void onGetSetAVtransresult(Boolean obj) {
		// TODO Auto-generated method stub
		
	}
	public void onGetSeekresult(Boolean obj) {
		// TODO Auto-generated method stub
		
	}
	public void onGetPreviousresult(Boolean obj) {
		// TODO Auto-generated method stub
		
	}
	public void onGetNextresult(Boolean obj) {
		// TODO Auto-generated method stub
		
	}
	public void onGetPouseresult(Boolean obj) {
		// TODO Auto-generated method stub
		
	}
	public void onGetStopresult(Boolean obj) {
		// TODO Auto-generated method stub
		
	}
	public void onGetPauseTOPlayresult(Boolean obj) {
		// TODO Auto-generated method stub
		
	}
	public void onGetPlayresult(Boolean obj) {
		// TODO Auto-generated method stub
		
	}
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getApplicationContext().startService(
				new Intent(getApplicationContext(), DLNAService.class));
        bind2RendererService();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.base_layout);
    }

	/* Add by yuanhong.dai*/
	public void setCustomContentView(int layoutResId, boolean isFillParent) {
		if (mContentLayout == null) {
			mContentLayout = (LinearLayout)findViewById(R.id.content);
		}
		if (mContentLayout != null) {
			LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = inflater.inflate(layoutResId, null);
			if (isFillParent) {
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
						                                   FrameLayout.LayoutParams.MATCH_PARENT,
						                                   FrameLayout.LayoutParams.MATCH_PARENT);
				mContentLayout.addView(view, params);
			} else {
				mContentLayout.addView(view);
			}
		}
	}

	public void updateTitle(String title) {
		if (mTitleText != null) {
			mTitleText.setText(title);
		}
	}

	public void removeCustomContentView() {
		if (mContentLayout != null) {
			mContentLayout.removeAllViews();
		}
	}

	public void setCustomTitle(int leftResId, int leftStrResId, int titleResId, int rightResId) {

		mLeftBtn = (Button)findViewById(R.id.titlbarback);
		if (mLeftBtn != null && leftResId != 0) {			
			mLeftBtn.setBackgroundResource(leftResId);
		}
		if (mLeftBtn != null && leftStrResId != 0) {
			mLeftBtn.setText(leftStrResId);
		}

		mTitleText = (TextView)findViewById(R.id.title);
		if (mTitleText != null && titleResId != 0) {
			mTitleText.setText(titleResId);
		}

		mRightBtn = (ImageButton)findViewById(R.id.function_btn);
		if (mRightBtn != null && rightResId != 0) {
			mRightBtn.setImageResource(rightResId);
		}
	}

	public void setNoTitle() {
		RelativeLayout titleView = (RelativeLayout)findViewById(R.id.title_layout);
		if (titleView != null) {
			titleView.setVisibility(View.GONE);
		}
	}

	public void setEnabledLeftBtn(boolean isEnbled) {
		if (mLeftBtn != null) {
			mLeftBtn.setEnabled(isEnbled);
		}
	}

	public void setEnableRightBtn(boolean isEnabled) {
		if (mRightBtn != null) {
			mRightBtn.setEnabled(isEnabled);
		}
	}

	public Button getTitleLeftBtn() {
		return mLeftBtn;
	}

	public TextView getTitleTextView() {
		return mTitleText;
	}

	public ImageButton getTitleRightBtn() {
		return mRightBtn;
	}
	/* End by yuanhong.dai */

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbind2RendererService();
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
			BaseActivity.this.onServiceConnected();
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {	
			Log.d(TAG, "onServiceDisconnected!");			
			mService = null;
			BaseActivity.this.onServiceDisconnected();
		}	
	};
	
	protected void onServiceConnected() {
		
	}
	
	protected void onServiceDisconnected() {
		
	}
	
	private void bind2RendererService() {
		mIsBound = getApplicationContext().bindService(new Intent(this, DLNAService.class), 
        		mServConn, BIND_AUTO_CREATE);
		Log.d(TAG, "mIsBound: " + mIsBound);
	}
	
	protected void getMusicCategoryData() {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_GET_MUSIC_CATEGORY_DATA);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	
	protected void getMusicAlbumsData() {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_GET_MUSIC_ALBUMS_DATA);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	
	protected void getMusicAllData() {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_GET_MUSIC_ALL_DATA);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	
	protected void getMusicArtistssData() {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_GET_MUSIC_ARTISTS_DATA);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	
	protected void getVideosData() {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_GET_VIDEOS_DATA);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	
	protected void getPhotosData() {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_GET_PHOTOS_DATA);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	
	protected void postBrowseRequest(Device dev, String objectId) {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_CONTENT_DIR_BROWSE);
			    Bundle data = new Bundle();
			    data.putString(MessageDefs.KEY_OBJ_ID, objectId);
			    data.putString(MessageDefs.KEY_DEV_URI, dev.getLocation());
			    Log.d(TAG, "Location:" + dev.getLocation());
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    msg.setData(data);
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	
	protected void postBrowseRequest(String objectId) {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_CONTENT_DIR_BROWSE);
			    Bundle data = new Bundle();
			    data.putString(MessageDefs.KEY_OBJ_ID, objectId);
			    msg.replyTo = mMessenger;
			    msg.setData(data);
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}		
	}
	
	protected void postPlayRequest(Device dev, String uri) {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_PLAY);
			    Bundle data = new Bundle();
			    data.putString(MessageDefs.KEY_ITEM_URI, uri);
			    data.putString(MessageDefs.KEY_DEV_URI, dev.getLocation());
			    Log.d(TAG, "Location:" + dev.getLocation());
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    msg.setData(data);
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}		
	}
	
	protected void queryLocalMDMSStatus() {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_SERVICE_QUERY_LOCAL_MDMS_STATUS);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}		
	}
	
	protected void getDeviceList() {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_SERVICE_GET_DEVICE_LIST);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}		
	}

	protected void connectDevice(DeviceInfo devInfo) {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_SERVICE_CONNECT_DEVICE, devInfo);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}

	protected void getIPAddrOfCurrentRenderer() {
		if(null != mService) {
			try {
				Message msg = Message.obtain(null,
						MessageDefs.MSG_SERVICE_GET_IP_OF_CURRENT_RENDERER);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
/*	
	protected void subscribeDeviceList() {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_SERVICE_SUBSCRIBE_DEVICE_CHANGE);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}		
	}
	
	protected void unsubscribeDeviceList() {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_SERVICE_UNSUBSCRIBE_DEVICE_CHANGE);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}		
	}
*/	
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
    
	protected void onSearchResponse(Message msg) {    	
    }
    
	protected void onDeviceAdded(Message msg) {
    }
    
	protected void onDeviceRemoved(Message msg) {
    } 
    
	protected void onGetContent(Message msg) {
    }
	
	protected void onGetMusicCategoryData(ArrayList<MusicCategoryInfo> musicCategory) {
	}
	
	protected void onGetMusicArtistsData(ArrayList<Artist> musicCategory) {
	}
	
	protected void onGetMusicAlbumsData(ArrayList<Album> albums) {
	}
	
	protected void onGetMusicAllData(ArrayList<MusicItem> musics) {
	}
	
	protected void onGetVideoCategory(ArrayList<VideoCategory> videoCategory) {
	}
	
	protected void onGetPhotos(ArrayList<PhotoAlbum> photoAlbum) {
	}
	
	protected void onDeviceChanged(Message msg) {
		@SuppressWarnings("unchecked")
		List<DeviceInfo> devices = ((List<DeviceInfo>)msg.obj);
		for(DeviceInfo devInfo : devices) {
			Log.d(TAG, "on DeviceChanged Device name:" + devInfo.mDevName + 
					", Dev udn:" + devInfo.mUDN);
		}		
		
		Log.d(TAG, "Query local DMS status-------------------------------->");
		queryLocalMDMSStatus();
    }
	
	protected void onGetLocalMDMSStatus(Message msg) {
		Log.d(TAG, "onGetLocalMDMSStatus local DMS status-------------------------------->");
		Log.d(TAG, "status 0(OFFLINE) 1(ONLINE) status:" + msg.arg1);
	}
	
	protected void onLocalMDMSStatusChanged(Message msg) {
		Log.d(TAG, "onLocalMDMSStatusChanged local DMS status-------------------------------->");
		Log.d(TAG, "status 0(OFFLINE) 1(ONLINE) status:" + msg.arg1);		
	}
	
	protected void onGetDeviceList(ArrayList<DeviceInfo> devices) {
		Log.v(TAG, "onGetDeviceList -------------------------------->");
		for(DeviceInfo dev : devices) {
			Log.v(TAG, "DEV NAME:" + dev.mDevName);
		}
	}
	
	protected void onDeviceConnected(DeviceInfo info) {
		
	}
	
	protected void onGetIPAddrOfCurrentRenderer(String ipAddr) {
		Log.v(TAG, "onGetIPAddrOfCurrentRenderer --------------------------------> ipAddr:" + ipAddr);
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
	protected void postPauseToPlay(){
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_PAUSETOPLAY);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}		
	}
	protected void poststop(){
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_STOP);
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}		
	}
	protected void postpause(){
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_PAUSE);
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}		
	}
	protected void postGetPositionInfo(){
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_GETPOSITIONINFO);
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	protected void postSeek( String target){
		if(null != mService) {
			try {
				 Message msg = Message.obtain(null,
				    		MessageDefs.MSG_MDMC_AV_TRANS_SEEK);
				    Bundle data = new Bundle();
				    data.putString("TARGET", target);
				    // Location is unique for devices
				    msg.replyTo = mMessenger;
				    msg.setData(data);
				    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	//image 缩放
	protected void postScalingImage( String target){
		if(null != mService) {
			try {
				 Message msg = Message.obtain(null,
				    		MessageDefs.MSG_MDMC_AV_TRANS_IMAGESEEK);
				    Bundle data = new Bundle();
				    data.putString("TARGET", target);
				    data.putString("UNIT", "SCALING");
				    // Location is unique for devices
				    msg.replyTo = mMessenger;
				    msg.setData(data);
				    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	//image 翻转
	protected void postFlipImage( String target){
		if(null != mService) {
			try {
				Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_IMAGESEEK);
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    Bundle data = new Bundle();
			    data.putString("UNIT", "FLIP");
			    data.putString("TARGET", target);
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
	protected void postPrevious(String uri,String type){
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_PREVIOUS);
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
	
	protected void postSetAVTransportURI(String uri){
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_SETAVTRANSPORTURI);
			    Bundle data = new Bundle();
			    data.putString(MessageDefs.KEY_ITEM_URI, uri);
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    msg.setData(data);
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	protected void postSetVolume(Float DesiredVolume){
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_SETVOLUME);
			    Bundle data = new Bundle();
			    data.putFloat("DesiredVolume", DesiredVolume);
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    msg.setData(data);
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	
	protected void postGetVolume(){
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_GETVOLUME);
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	protected void postSetMute(Float DesiredVolume){
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_SETMUTE);
			    Bundle data = new Bundle();
			    data.putFloat("DesiredVolume", DesiredVolume);
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    msg.setData(data);
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	protected void postGetMute(){
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_GETMUTE);
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			   
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	
	protected void postGetCurrentTranSportActions(){
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_GETCURRENTTRANSPORTACTIONS);
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	protected void postGetTransportInfo(){
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
	protected void postSePlayMode(String newPlayMode){
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		MessageDefs.MSG_MDMC_AV_TRANS_SETPLAYMODE);
			    Bundle data = new Bundle();
			    data.putString("NewPlayMode", newPlayMode);
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    msg.setData(data);
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	
	protected void stbDisconnectWifi() {
		if(null != mService) {
			try {
				Message msg = Message.obtain(null,
						MessageDefs.MSG_SERVICE_RENDERER_DISCONNECT_WIFI);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
}