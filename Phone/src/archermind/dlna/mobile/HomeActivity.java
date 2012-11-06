package archermind.dlna.mobile;

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.std.av.renderer.MediaRenderer;
import org.cybergarage.upnp.std.av.server.MediaServer;
import org.cybergarage.upnp.std.av.server.object.ContentNode;
import org.cybergarage.upnp.std.av.server.object.container.ContainerNode;
import org.cybergarage.upnp.std.av.server.object.item.ItemNode;
import org.cybergarage.upnp.std.av.server.object.item.ResourceNode;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

public class HomeActivity extends BaseActivity 
	implements View.OnClickListener,ListView.OnItemClickListener {
	private final static String TAG = "HomeActivity";
	private ListView mServerList;
	private ListView mRendererList;
	private TextView mIndicator;
	private DeviceList mRl = new DeviceList();
	private DeviceList mSl = new DeviceList();
	private ContainerNode mCurParent = null;
	private int mCurDevPos = -1;
	private Device mRenderer;

	/*
	private boolean mIsBound = false;
	private Messenger mService = null;
	final Messenger mMessenger = new Messenger(new IncomingHandler());
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
          switch (msg.what) {
			case ControllerProcess.MSG_CTRL_ON_SEARCH_RESPONSE:
				onSearchResponse(msg);
				break;
			case ControllerProcess.MSG_CTRL_ON_DEV_ADDED:
				onDeviceAdded(msg);
				break;
			case ControllerProcess.MSG_CTRL_ON_DEV_REMOVED:
				onDeviceRemoved(msg);
				break;
			case ControllerProcess.MSG_CTRL_ON_GET_CONTENT:
				onGetContent(msg);
				break;  
			case DLNAService.MSG_ON_DEVICE_CHANGED:
				@SuppressWarnings("unchecked")
				List<ScanResult> results = (List<ScanResult>)msg.obj;
        		for(ScanResult result : results) {
        			Log.d(TAG, "result:" + result.toString());
        		}     
				break;
			default:
			    super.handleMessage(msg);
            }
        }
    }*/
	
	private class ServerListAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return (null == mSl) ? 0: mSl.size();
		}

		@Override
		public Object getItem(int position) {
			return (null == mSl) ? null : mSl.getDevice(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view = null;			
			if (null == convertView) {
				view = (TextView)HomeActivity.this.getLayoutInflater().inflate(
						android.R.layout.simple_list_item_1, parent, false);
			} else {
				view = (TextView)convertView;
			}
			if(null != mSl)
				view.setText(mSl.getDevice(position).getFriendlyName());
			return view;
		}		
	};
	
	private class ContentAdpater extends BaseAdapter {		
		@Override
		public int getCount() {
			return (null == mCurParent) ? 0: mCurParent.getChildCount();
		}

		@Override
		public Object getItem(int position) {
			return (null == mCurParent) ? null : mCurParent.getContentNode(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view = null;			
			if (null == convertView) {
				view = (TextView)HomeActivity.this.getLayoutInflater().inflate(
						android.R.layout.simple_list_item_1, parent, false);
			} else {
				view = (TextView)convertView;
			}
			if(null != mCurParent)
				view.setText(mCurParent.getContentNode(position).getTitle());
			return view;
		}		
	};
	
	private class RendererListAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return (null == mRl) ? 0: mRl.size();
		}

		@Override
		public Object getItem(int position) {
			return (null == mRl) ? null : mRl.getDevice(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView view = null;			
			if (null == convertView) {
				view = (TextView)HomeActivity.this.getLayoutInflater().inflate(
						android.R.layout.simple_list_item_single_choice, parent, false);
			} else {
				view = (TextView)convertView;
			}
			if(null != mRl)
				view.setText(mRl.getDevice(position).getFriendlyName());
			return view;
		}		
	};

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main); 
        mServerList = (ListView) findViewById(R.id.dms_list);
        mRendererList = (ListView) findViewById(R.id.dmr_list);
        mIndicator = (TextView)findViewById(R.id.current_renderer);
        mIndicator.setOnClickListener(this);
        mServerList.setAdapter(new ServerListAdapter());
        mServerList.setOnItemClickListener(this);
        mRendererList.setAdapter(new RendererListAdapter());
        mRendererList.setOnItemClickListener(this);
        //bind2RendererService();        
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        //unbind2RendererService();
    }
/*
	private ServiceConnection mServConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "onServiceConnected!");
			mService = new Messenger(service);
			try {
			    Message msg = Message.obtain(null,
			            DLNAService.MSG_REGISTER_CLIENT);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			} 
			subscribeDeviceList();
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {	
			Log.d(TAG, "onServiceDisconnected!");			
			mService = null;
		}	
	};
	
	private void bind2RendererService() {
		mIsBound = bindService(new Intent(HomeActivity.this, DLNAService.class), 
        		mServConn, BIND_AUTO_CREATE);
	}
	
	private void postBrowseRequest(Device dev, String objectId) {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			            ControllerProcess.MSG_BROWSE_CONTENT);
			    Bundle data = new Bundle();
			    data.putString(DLNAService.KEY_OBJ_ID, objectId);
			    data.putString(DLNAService.KEY_DEV_URI, dev.getLocation());
			    Log.d(TAG, "Location:" + dev.getLocation());
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    msg.setData(data);
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}
	}
	
	private void postPlayRequest(Device dev, String uri) {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		ControllerProcess.MSG_PLAY);
			    Bundle data = new Bundle();
			    data.putString(DLNAService.KEY_ITEM_URI, uri);
			    data.putString(DLNAService.KEY_DEV_URI, dev.getLocation());
			    Log.d(TAG, "Location:" + dev.getLocation());
			    // Location is unique for devices
			    msg.replyTo = mMessenger;
			    msg.setData(data);
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}		
	}
	
	protected void subscribeDeviceList() {
		if(null != mService) {
			try {
			    Message msg = Message.obtain(null,
			    		DLNAService.MSG_SUBSCRIBE_DEVICES_LIST);
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
			    		DLNAService.MSG_UNSUBSCRIBE_DEVICES_LIST);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			}
		}		
	}
	
	private void unbind2RendererService() {		
		if (mIsBound && (mService != null)) {
			unsubscribeDeviceList();
			try {
			    Message msg = Message.obtain(null,
			    		DLNAService.MSG_UNREGISTER_CLIENT);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {		
			}
			Log.d(TAG, "unbind2RendererService!");
			unbindService(mServConn);			
			mIsBound = false;
		}
	}
*/    
    @SuppressWarnings("unchecked")
    @Override
	protected void onSearchResponse(Message msg) {
    	Log.d(TAG, "onSearchResponse");
    	DeviceList dl = (DeviceList)msg.obj;
    	mRl.clear();
    	mSl.clear();
    	for(int index = 0; index < dl.size(); ++index) {
    		String devType = dl.getDevice(index).getDeviceType();
    		if(devType.equals(MediaRenderer.DEVICE_TYPE)) {
    			mRl.add(dl.getDevice(index));
    		} else if(devType.equals(MediaServer.DEVICE_TYPE)) {
    			mSl.add(dl.getDevice(index));
    		}
		}
    	BaseAdapter ba = (BaseAdapter)mServerList.getAdapter();
		if(ba instanceof ServerListAdapter)
			ba.notifyDataSetChanged();
    	((BaseAdapter)mRendererList.getAdapter()).notifyDataSetChanged();
    }
    
    @Override
	protected void onDeviceAdded(Message msg) {    	
    	Device dev = (Device)msg.obj;
    	String devType = dev.getDeviceType();
    	Log.d(TAG, "onDeviceAdded devName:" + dev.getFriendlyName());
    	Log.d(TAG, "dev Type:" + devType);
    	if(devType.equals(MediaServer.DEVICE_TYPE)) {
    		if(addDevTo(mSl, dev)) {
    			BaseAdapter ba = (BaseAdapter)mServerList.getAdapter();
    			if(ba instanceof ServerListAdapter)
    				ba.notifyDataSetChanged();
    		}
    	} else if(devType.equals(MediaRenderer.DEVICE_TYPE)) {
    		if(addDevTo(mRl, dev))
    			((BaseAdapter)mRendererList.getAdapter()).notifyDataSetChanged();
    	}
    }
    
    @Override
    protected void onDeviceRemoved(Message msg) {
    	Log.d(TAG, "onDeviceRemoved");
    	Device dev = (Device)msg.obj; 
    	String devType = dev.getDeviceType();
    	if(devType.equals(MediaServer.DEVICE_TYPE)) {
    		if(removeDevFrom(mSl, dev)) {
    			BaseAdapter ba = (BaseAdapter)mServerList.getAdapter();
    			if(ba instanceof ServerListAdapter)
    				ba.notifyDataSetChanged();
    		}
    			
    	} else if(devType.equals(MediaRenderer.DEVICE_TYPE)) {
    		if(removeDevFrom(mRl, dev))
    			((BaseAdapter)mRendererList.getAdapter()).notifyDataSetChanged();
    	}		
    } 
    
    @Override
    protected void onGetContent(Message msg) {
    	Log.d(TAG, "onGetContent");
    	mCurParent = (ContainerNode)msg.obj;
    	Log.d(TAG, "title:" + mCurParent.getTitle() + ", getID:" + mCurParent.getID() +
    			", childCount:" + mCurParent.getChildCount());
    	mServerList.setAdapter(new ContentAdpater());
    }
  
	@SuppressWarnings("unchecked")
	private boolean addDevTo(DeviceList dl, Device dev) {
    	Log.d(TAG, "Fname:" + dev.getFriendlyName() + ", SN:" + dev.getUDN());    	
		boolean bExist = false;
		for(int index = 0; index < dl.size(); ++index) {
			if(dl.getDevice(index).getUDN().equals(dev.getUDN())) {
				bExist = true;
				break;
			}
		}
		if(!bExist) {
			dl.add(dev);			
		}
		Log.d(TAG, "bExist:" + bExist + ", dl:" + dl.toString());
		return !bExist;
    }
    
    private boolean removeDevFrom(DeviceList dl, Device dev) {
    	Log.d(TAG, "removeDevFrom"); 
    	boolean bRet = false;
		for(int index = 0; index < dl.size(); ++index) {
			if(dl.getDevice(index).getUDN().equals(dev.getUDN())) {
				dl.remove(index);
				bRet = true;
				break;
			}
		}
		return bRet;
    }


	@Override
	public void onClick(View v) {
		mRendererList.setVisibility(View.VISIBLE);
		mRendererList.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pull_right_in));	
		mServerList.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pull_left_out));
		mIndicator.startAnimation(AnimationUtils.loadAnimation(this, R.anim.pull_left_out));
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		if(arg0 == mRendererList) {
			onClickRendererItem(arg1, arg2, arg3);
		} else if(arg0 == mServerList) {
			onClickServerItem(arg1, arg2, arg3);
		}
	}
	
	private void onClickRendererItem(View item, int position, long id) {
		mRendererList.setVisibility(View.INVISIBLE);
		mRendererList.startAnimation(AnimationUtils.loadAnimation(this, R.anim.push_right_out));	
		mServerList.startAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
		mIndicator.startAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
		CheckedTextView ctv = (CheckedTextView)item;
		if(!ctv.isChecked()) {
			ctv.setChecked(true);
			mRenderer = mRl.getDevice(position);
			mIndicator.setText(mRenderer.getFriendlyName());
		}			
	}
	
	private void onClickServerItem(View item, int position, long id) {		
		if(null == mCurParent) {
			// click device
			postBrowseRequest(mSl.getDevice(position), null);
			mCurDevPos = position;
		} else {
			// click node in device
			ContentNode cn= mCurParent.getContentNode(position);
			if(cn.isContainerNode()) {
				postBrowseRequest(mSl.getDevice(mCurDevPos), cn.getID()); 
			} else if(null != mRenderer){
				ItemNode itemNode = (ItemNode)cn;
				ResourceNode resNode = itemNode.getFirstResource();
				Log.d(TAG, "Try to play:" + resNode.getURL());
				postPlayRequest(mRenderer, resNode.getURL());
			}
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if((View.VISIBLE == mRendererList.getVisibility()) && 
				(KeyEvent.KEYCODE_BACK == keyCode)) {
			mRendererList.setVisibility(View.INVISIBLE);
			mRendererList.startAnimation(AnimationUtils.loadAnimation(this, R.anim.push_right_out));	
			mServerList.startAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
			mIndicator.startAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
			return true;
		} else {
			return super.onKeyDown(keyCode, event);	
		}
	}
}