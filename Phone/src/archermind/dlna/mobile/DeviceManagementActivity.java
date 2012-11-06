package archermind.dlna.mobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Matrix;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import archermind.dlna.miscellaneous.DeviceInfo;
import archermind.dlna.mobile.data.TvDevicesProvider;
import archermind.dlna.mobile.data.util.LoginDialog;
import archermind.dlna.mobile.scanner.CaptureActivity;

public class DeviceManagementActivity extends BaseActivity implements OnItemClickListener, View.OnClickListener {
	private static final String START_SCAN_SERVICE_ACTION = "scan.tvdevices.service.ACTION";

	public static int DEVICE_MODE = 0;
	public static final String DEVICE_MODE_KEY = "device_mode_key";
	public static final String DEVICE_MODE_DATA = "device_mode_data";
//	public static final String DEVICE_MODE_IS_DEVICE_TAB = "device_mode_is_device_tab";
	private static final int DEVICE_MODE_DEFAULT = 0;
	private static final int DEVICE_MODE_STICK = 1;
	private static final int DEVICE_MODE_WIFI = 2;

	private static final String DEVICES_ID_KEY = "devices_id_key";
	private static final String DEVICES_NAME_KEY = "devices_name_key";
	private static final String DEVICES_SSHID_KEY = "devices_sshid_key";
	private static final String DEVICES_LINK_STATE_KEY = "devices_link_state_key";

	private static final int LISTVIEW_ITEM_COUNT_ONE = 0;
	private static final int LISTVIEW_ITEM_COUNT_TWO = 1;
	private static final int LISTVIEW_ITEM_COUNT_THREE = 2;
	private static final int LISTVIEW_ITEM_COUNT_FOUR = 3;
	private static final int LISTVIEW_ITEM_COUNT_FIVE = 4;
	private static final int LISTVIEW_ITEM_COUNT_SIX = 5;

	private static final String OPERATION_NAME_KEY = "operation_name_key";
	private static final String OPERATION_STATE_KEY = "operation_state_key";

	private static final String SHOW_LINK_ICON = 1 + "";

	private static final int START_TASK = 1;
	private static final int END_TASK = 2;

	private static final int UPDATE_PROGRESS_TIME = 100;

	private static final int ERROR_DIALOG_WIDTH = 425;
	private static final int ERROR_DIALOG_HEIGHT = 280;

	private ProgressBar mScanDevicesProgressBar;
	private ListView mDevicesListView;

	private ArrayList<DeviceInfo> mDeviceList = null;

	private RotateTimerTask mRotateTimerTask;
	private Timer mTimer;
	private int mFromAngle;

	private RelativeLayout mProgressLayout;

//	private boolean mIsUpdateListView = false;

	private DeviceBaseAdapter mDeviceBaseAdapter;
	private TvDevicesProvider mTvDevicesProvider;

//	private ScanDevicesService.ScanBinder mBinder;
	private Intent mIntent;
	private long mID;
	private int mPosition;

	private RelativeLayout mLoginLayout;
	private LoginDialog mLoginDialog;
	private LoginDialog mErrorDialog;

	/* Mode stick */
	private Button mBackBtn;
	private TextView mTitle;
	private ListView mOperatedList;
	private StickBaseAdapter mAdapter;
	private ArrayList<HashMap<String, String>> mStickList = null;
	private String[] mNameStrArray = null;
	private String[] mSecondaryTileArray = null;
	private String mTitleStr = "";

	/* Mode wifi */
	private ImageView mWifiConnectStatus;
	private TextView mWifiConnectMessage;
	private ListView mWifiListView;
	private WifiBaseAdapter mWifiAdapter;
//	private ArrayList<MyWifiInfo> mWifiList = null;
   private List<ScanResult> mWifiScanList;
   private ScanResult mScanResult;
   private DeviceWifiManager mDeviceWifiManager;

	private void showToast(String str) {
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DEVICE_MODE = MobileDLNAActivity.mMode;
//		DEVICE_MODE = 2;
	}

	private void initDeviceComponents() {
		mScanDevicesProgressBar = (ProgressBar)findViewById(R.id.scan_progressBar);
		mDevicesListView = (ListView)findViewById(R.id.devices_listView);
		mProgressLayout = (RelativeLayout)findViewById(R.id.progressBar_layout);
		mDeviceList = new ArrayList<DeviceInfo>();
		mTimer = new Timer();
		mRotateTimerTask = new RotateTimerTask();
		mTvDevicesProvider = new TvDevicesProvider();
		mTvDevicesProvider.onCreate();
		mTimer.schedule(mRotateTimerTask, 0, UPDATE_PROGRESS_TIME);

		setViewOnClickListener();
//		bindService();
	}

	private void initStickComponents() {
		mTitleStr = getIntent().getStringExtra(DEVICES_NAME_KEY);

		mBackBtn = (Button)findViewById(R.id.titlbarback);
		mTitle = (TextView)findViewById(R.id.title);
		mOperatedList = (ListView)findViewById(R.id.tv_stick_list);

		mBackBtn.setOnClickListener(this);
		mTitle.setText(mTitleStr);

		mStickList = new ArrayList<HashMap<String,String>>();
		mNameStrArray = new String[] {
				getString(R.string.link_network),
				getString(R.string.phone_image),
				getString(R.string.OTA_update),
				getString(R.string.sleep),
				getString(R.string.safity),
				getString(R.string.modified_device_name)
		};
		mSecondaryTileArray = new String[] {
				String.format(getString(R.string.link_network_secondary), mTitleStr),
				String.format(getString(R.string.phone_image_secondary), mTitleStr),
				String.format(getString(R.string.OTA_update_secondary), mTitleStr),
				String.format(getString(R.string.sleep_secondary), mTitleStr),
				String.format(getString(R.string.satity_secondary), mTitleStr),
				String.format(getString(R.string.modified_device_name_secondary), mTitleStr)
		};

		setViewOnClickListener();
	}

	private void initWifiComponents() {
		mWifiConnectStatus = (ImageView) findViewById(R.id.connect_status);
		mWifiConnectMessage = (TextView) findViewById(R.id.wifi_connect_message);
		mWifiListView = (ListView) findViewById(R.id.wifi_list);
		mWifiAdapter = new WifiBaseAdapter(this);
//		mWifiList = new ArrayList<MyWifiInfo>();

		mDeviceWifiManager = new DeviceWifiManager(this);
		mDeviceWifiManager.startScan();
		mWifiScanList = mDeviceWifiManager.getWifiList();
		if(mWifiScanList!=null){
			StringBuffer sb=new StringBuffer(); 
			for(int i=0;i<mWifiScanList.size();i++){
				//得到扫描结果
				mScanResult=mWifiScanList.get(i);  
				sb = sb.append(mScanResult.BSSID+"  ").append(mScanResult.SSID+"   ")  
				.append(mScanResult.capabilities+"   ").append(mScanResult.frequency+"   ")  
				.append(mScanResult.level+"\n\n");
				Log.i("fff", "==========" + sb);
			}
		}
	}

	private void setViewOnClickListener() {
		if (DEVICE_MODE == DEVICE_MODE_DEFAULT) {
			mDevicesListView.setOnItemClickListener(this);
			getTitleLeftBtn().setOnClickListener(this);
		} else if (DEVICE_MODE == DEVICE_MODE_STICK) {
			mOperatedList.setOnItemClickListener(this);
		} else if (DEVICE_MODE == DEVICE_MODE_WIFI) {
			mWifiListView.setOnItemClickListener(this);
		}
	}

//	private void bindService() {
//		Intent intent = new Intent();
//		intent.setClass(DeviceManagementActivity.this, DLNAService.class);
//		this.getApplicationContext().bindService(intent, mConn, Service.BIND_AUTO_CREATE);
//	}

	private void clearList() {
		if (DEVICE_MODE == DEVICE_MODE_DEFAULT) {
			if (mDeviceList != null) {
				mDeviceList.clear();
			}
		} else if (DEVICE_MODE == DEVICE_MODE_STICK) {
			if (mStickList != null) {
				mStickList.clear();
			}
		} else if (DEVICE_MODE == DEVICE_MODE_WIFI) {
			if (mWifiScanList != null) {
				mWifiScanList.clear();
			}
		}
	}

	private void rotateImage(View v, float angle) {
		if (mFromAngle >= 360) {
			mFromAngle = 0;
		}

		mFromAngle += angle;
		RotateAnimation rotateAnomation = new RotateAnimation(mFromAngle, mFromAngle + angle, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		rotateAnomation.setFillAfter(true);
		v.startAnimation(rotateAnomation);
		Matrix matrix = new Matrix();
		matrix.postRotate(angle, v.getWidth() / 2, v.getHeight() / 2);
	}

	protected void onResume() {
		super.onResume();
		if (DEVICE_MODE == DEVICE_MODE_DEFAULT) {
			setCustomTitle(0, R.string.scan_two_dimension_code, R.string.devices_list, R.drawable.default_version_update);
			setCustomContentView(R.layout.device_management, true);
			initDeviceComponents();
		} else if (DEVICE_MODE == DEVICE_MODE_STICK) {
			setNoTitle();
			setCustomContentView(R.layout.tv_stick_view, false);
			initStickComponents();
		} else if (DEVICE_MODE == DEVICE_MODE_WIFI) {
			setNoTitle();
			setCustomTitle(R.drawable.bth_back_arrow_normal, 0, R.string.wifi_title, R.drawable.default_version_update);
			setCustomContentView(R.layout.wifi_layout, false);
			initWifiComponents();
		}

		if (DEVICE_MODE == DEVICE_MODE_DEFAULT) {
			//clearList();
//			queryDatebase();
			updateTitle();
			setMyAdapter();
		} else if (DEVICE_MODE == DEVICE_MODE_STICK) {
			//clearList();
			setListViewData();
			setMyAdapter();
		} else if (DEVICE_MODE == DEVICE_MODE_WIFI) {
//			mWifiList.add(new MyWifiInfo(true, "H3C", 0));
//			mWifiList.add(new MyWifiInfo(false, "CMCC", 1));
			setMyAdapter();
			mWifiAdapter.notifyDataSetChanged();
		}
	}

	private void setListViewData() {
		for (int i = 0; i < mNameStrArray.length; i++) {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put(OPERATION_NAME_KEY, mNameStrArray[i]);
			map.put(OPERATION_STATE_KEY, mSecondaryTileArray[i]);
			mStickList.add(map);
		}
	}

//	private void queryDatebase() {
//		Cursor cur = null;
//		HashMap<String, Object> listViewMap = null;
//		try {
//			cur = getContentResolver().query(TvDevicesProvider.CONTENT_URI, null, null, null, null);
//			if (cur != null && cur.moveToFirst()) {
//				do {
//					listViewMap = new HashMap<String, Object>();
//					long deviceId = cur.getLong(cur.getColumnIndex(TvDevicesProvider._ID));
//					String frendyName = cur.getString(cur.getColumnIndex(TvDevicesProvider.FRIENDY_NAME));
//					String sshId = cur.getString(cur.getColumnIndex(TvDevicesProvider.SS_ID));
//					int linkState = cur.getInt(cur.getColumnIndex(TvDevicesProvider.LINK_STATE));
//
//					listViewMap.put(DEVICES_ID_KEY, deviceId);
//					listViewMap.put(DEVICES_NAME_KEY, frendyName);
//					listViewMap.put(DEVICES_SSHID_KEY, sshId);
//					listViewMap.put(DEVICES_LINK_STATE_KEY, linkState + "");
//					mDeviceList.add(listViewMap);
//				}while(cur.moveToNext());
//			}
//		} finally {
//			if (cur != null) {
//				cur.close();
//			}
//		}
//	}

	private void updateTitle() {
		if (mDeviceList != null && mDeviceList.size() > 0) {
			updateTitle(getString(R.string.devices_list) + " ( " + mDeviceList.size() + " ) ");
		}
	}

	private void setMyAdapter() {
		if (DEVICE_MODE == DEVICE_MODE_DEFAULT) {
			mDeviceBaseAdapter = new DeviceBaseAdapter(this);
			mDevicesListView.setAdapter(mDeviceBaseAdapter);
			mDeviceBaseAdapter.notifyDataSetChanged();
		} else if (DEVICE_MODE == DEVICE_MODE_STICK) {
			if (mAdapter == null) {			
				mAdapter = new StickBaseAdapter(this);
			}
			mOperatedList.setAdapter(mAdapter);
		} else if (DEVICE_MODE == DEVICE_MODE_WIFI) {
			mWifiListView.setAdapter(mWifiAdapter);
		}
	}

//	private void insertDatebase() {
//		ContentValues values = new ContentValues();
//		values.clear();
//		values.put(TvDevicesProvider.FRIENDY_NAME, "TV Dongle");
//		values.put(TvDevicesProvider.SS_ID, "11254512565655355");
//		getContentResolver().insert(TvDevicesProvider.CONTENT_URI, values);
//	}

	@Override
	protected void onStop() {
		super.onStop();
		removeCustomContentView();
		cancelTimer();
	}

	public void onBackPressed() {
		super.onBackPressed();
		if(DEVICE_MODE == 2) {
			DEVICE_MODE = 1;
		} else if (DEVICE_MODE == 1) {
			DEVICE_MODE = 0;
		}
	}

	class DeviceHolder  {
		ImageView tvImage;
		TextView tvNameText;
		TextView tvSshIdText;
		Button linkStateBtn;

		public DeviceHolder(View itemView) {
			this.tvImage = (ImageView)itemView.findViewById(R.id.tv_iamge);
			this.tvNameText = (TextView)itemView.findViewById(R.id.tv_name);
			this.tvSshIdText = (TextView)itemView.findViewById(R.id.tv_sshid);
			this.linkStateBtn = (Button)itemView.findViewById(R.id.link_state);
		}
	}

	private class DeviceBaseAdapter extends BaseAdapter {

		private Context mContext;

		public DeviceBaseAdapter(Context context) {
			mContext = context;
		}

		@Override
		public int getCount() {
			return mDeviceList == null ? 0 : mDeviceList.size();
		}

		@Override
		public Object getItem(int arg0) {
			return mDeviceList.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int position, View converView, android.view.ViewGroup parent) {
			DeviceHolder holder = null;
			if (converView == null) {
				converView = getLayoutInflater().from(mContext).inflate(R.layout.devices_list_item, null);
				holder = new DeviceHolder(converView);
				DeviceInfo deviceInfo = mDeviceList.get(position);
				holder.tvNameText.setText(deviceInfo.mDevName);
				holder.tvSshIdText.setText(" ( " + deviceInfo.mBSSID + " ) ");
				if (deviceInfo.mState == DeviceInfo.DEV_STATE_CONNECTED) {
					holder.linkStateBtn.setBackgroundResource(R.drawable.icon_connected);
				} else {
					holder.linkStateBtn.setText(R.string.not_link);	
				}
//				if (mDeviceList != null) {
//					DeviceInfo deviceInfo = mDeviceList.get(position);
//					holder.tvNameText.setText(deviceInfo.mDevName);
//					holder.tvSshIdText.setText(deviceInfo.mBSSID);
//					if (deviceInfo.mState == DeviceInfo.DEV_STATE_CONNECTED) {
//						holder.linkStateBtn.setBackgroundResource(R.drawable.icon_connected);
//					} else {
//						holder.linkStateBtn.setText(R.string.not_link);	
//					}
//				}
				converView.setTag(holder);
			} else {
				holder = (DeviceHolder) converView.getTag();
			}
			return converView;
		}
	}

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch(msg.what) {
			    case START_TASK: {
			    	rotateImage(mScanDevicesProgressBar, UPDATE_PROGRESS_TIME);
			    	break;
			    }
			    case END_TASK: {
			    	cancelTimer();
			    	break;
			    }
			}
		}
	};

	private void cancelTimer() {
		if (mRotateTimerTask != null) {
			mRotateTimerTask.cancel();
		}
		if (mTimer != null) {
			mTimer.cancel();
		}
	}

	class StickHolder {
		TextView operationNameText;
		TextView operationStateText;
		ImageView arrowImage;

		public StickHolder(View v) {
			this.operationNameText = (TextView)v.findViewById(R.id.operation_name);
			this.operationStateText = (TextView)v.findViewById(R.id.operation_name_secondary);
			this.arrowImage = (ImageView)v.findViewById(R.id.arrow_iamge);
		}
	}

	private class StickBaseAdapter extends BaseAdapter {

		private Context mContext;

		public StickBaseAdapter(Context context) {
			mContext = context;
		}

		@Override
		public int getCount() {
			return mStickList == null ? 0 : mStickList.size();
		}

		@Override
		public Object getItem(int arg0) {
			return mStickList.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int position, View converView, android.view.ViewGroup parent) {
			StickHolder holder = null;
			View itemView = null;

			if (converView == null) {
				getLayoutInflater();
				converView = LayoutInflater.from(mContext).inflate(R.layout.tv_stick_list_item, null);
				holder = new StickHolder(converView);

				holder.operationNameText.setText(mStickList.get(position).get(OPERATION_NAME_KEY));
				holder.operationStateText.setText(mStickList.get(position).get(OPERATION_STATE_KEY));

				converView.setTag(holder);
			} else {
				holder = (StickHolder)converView.getTag();
			}
			return converView;
		}
	}

	class WifiHolder {
		ImageView wifiConnectStatus;
		TextView wifiName;
		ImageView wifiStatus;

		public WifiHolder(View itemView) {
			this.wifiConnectStatus = (ImageView)itemView.findViewById(R.id.wifi_list_item_connect_status);
			this.wifiName = (TextView)itemView.findViewById(R.id.wifi_list_item_wifi_name);
			this.wifiStatus = (ImageView)itemView.findViewById(R.id.wifi_list_item_wifi_status);
		}
	}

	private class WifiBaseAdapter extends BaseAdapter {
		
		private Context mContext;
		
		public WifiBaseAdapter(Context context) {
			mContext = context;
		}

		@Override
		public int getCount() {
			return mWifiScanList == null ? 0 : mWifiScanList.size();
		}

		@Override
		public Object getItem(int arg0) {
			return mWifiScanList.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int position, View converView, android.view.ViewGroup parent) {
			WifiHolder holder = null;
			if (converView == null) {
				converView = getLayoutInflater().from(mContext).inflate(R.layout.wifi_list_item, null);
				holder = new WifiHolder(converView);
				ScanResult result = mWifiScanList.get(position);
//				MyWifiInfo MyWifiInfo = mWifiScanList.get(position);
				
//				if(MyWifiInfo.mIsConntectWifi) {
//					holder.wifiConnectStatus.setBackgroundResource(R.drawable.icon_connected);
//				} else {
//					holder.wifiConnectStatus.setVisibility(View.INVISIBLE);
//				}
				holder.wifiName.setText(result.SSID);
//
//				if(MyWifiInfo.mWifiState == 0) {
//					holder.wifiStatus.setImageResource(R.drawable.wifi_list_icon_lock);
//				} else {
//					holder.wifiStatus.setImageResource(R.drawable.wifi_list_icon_lock);
//				}
				converView.setTag(holder);
			} else {
				holder = (WifiHolder)converView.getTag();
			}
			return converView;
		}
	}

	class RotateTimerTask extends TimerTask {

		@Override
		public void run() {
			Message msg = new Message();
			try {
				msg.what = START_TASK;
				handler.sendMessage(msg);
				Thread.sleep(100);
			} catch (Exception e) {
				msg.what = END_TASK;
				handler.sendMessage(msg);
				e.printStackTrace();
			}
		}
	}

//	protected void onDestroy() {
//		super.onDestroy();
//		this.getApplicationContext().unbindService(mConn);
//	}

	private void createLoginDialog(final DeviceInfo deviceInfo) {
		mLoginDialog = new LoginDialog(this);
		mLoginDialog.setTitle(R.string.login_title);
		Button linkBtn = mLoginDialog.getLinkButton();
		Button cancelBtn = mLoginDialog.getCancelButton();
		TextView name = (TextView) mLoginDialog.findViewById(R.id.account);
		name.setText(deviceInfo.mDevName);
		linkBtn.setOnClickListener(new android.view.View.OnClickListener () {
			@Override
			public void onClick(View v) {
				EditText code = (EditText) mLoginDialog.findViewById(R.id.password);
				deviceInfo.mPassword = code.getText().toString().trim();
				//connect2Device(deviceInfo);
				mLoginDialog.dismiss();
//				createErrorDialog();
//				updateDatebaseLinkState(1);
//				if (mDeviceBaseAdapter == null) {
//					mDeviceBaseAdapter = new DeviceBaseAdapter(DeviceManagementActivity.this);
//				}
//				mDevicesListView.setAdapter(mDeviceBaseAdapter);
			}
		});

		cancelBtn.setOnClickListener(new android.view.View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mLoginDialog.dismiss();
			}
		});
		mLoginDialog.show();
	}

//	private void updateDatebaseLinkState(int linkState) {
//		ContentValues values = new ContentValues();
//		values.clear();
//
//		values.put(TvDevicesProvider.LINK_STATE, linkState);
//		getContentResolver().update(TvDevicesProvider.CONTENT_URI, values, TvDevicesProvider._ID + "=" + mID, null);
//		mDeviceList.get(mPosition).put(DEVICES_LINK_STATE_KEY, "" + linkState);
//	}

	private void createErrorDialog() {
		mErrorDialog = new LoginDialog(this);
		mErrorDialog.setTitle(R.string.password_error_title);
		mErrorDialog.setDialogProperty(ERROR_DIALOG_WIDTH, ERROR_DIALOG_HEIGHT);

		TableLayout tabLayout = mErrorDialog.getTableLayout();
		TextView toastText = mErrorDialog.getSecondaryTitle();
		Button reLinkBtn = mErrorDialog.getLinkButton();
		Button cancelBtn = mErrorDialog.getCancelButton();

		tabLayout.setVisibility(View.GONE);
		toastText.setText(R.string.password_error_toast);
		toastText.setTextColor(0xff000000);
		toastText.setTextSize(20);
		reLinkBtn.setText(R.string.re_link);
		cancelBtn.setText(R.string.cancel);

		reLinkBtn.setOnClickListener(new android.view.View.OnClickListener () {
			@Override
			public void onClick(View v) {
				mErrorDialog.dismiss();
//				createLoginDialog();
			}
		});
		cancelBtn.setOnClickListener(new android.view.View.OnClickListener () {
			@Override
			public void onClick(View v) {
				mErrorDialog.dismiss();
			}
		});
		mErrorDialog.show();
	}

	private void jumpToStickView(DeviceInfo deviceInfo) {
		Intent intent = new Intent();
		intent.setClass(DeviceManagementActivity.this, MobileDLNAActivity.class);
		intent.putExtra(DEVICE_MODE_KEY, DEVICE_MODE_STICK);
		intent.putExtra(DEVICE_MODE_DATA, deviceInfo);
		startActivity(intent);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
		if (DEVICE_MODE == DEVICE_MODE_DEFAULT) {
//			mID = id;
//			mPosition = position;
			DeviceInfo deviceInfo = mDeviceList.get(position);
			if (deviceInfo.mState == DeviceInfo.DEV_STATE_CONNECTED) {
				jumpToStickView(deviceInfo);
			} else if (deviceInfo.mDevType == DeviceInfo.DEV_TYPE_AP) {
				if(deviceInfo.mState == DeviceInfo.DEV_STATE_REMEMBERED) {
					//connect2Device(deviceInfo);
				} else if (deviceInfo.mState == DeviceInfo.DEV_STATE_DISCONNECTED) {
					createLoginDialog(deviceInfo);
				}
			}
		} else if (DEVICE_MODE == DEVICE_MODE_STICK) {
			DeviceInfo deviceInfo = mDeviceList.get(position);
			switch (position) {
			case LISTVIEW_ITEM_COUNT_ONE:
				jumpToWifiList(deviceInfo);
				break;
			case LISTVIEW_ITEM_COUNT_TWO:
				break;
			case LISTVIEW_ITEM_COUNT_THREE:
				break;
			case LISTVIEW_ITEM_COUNT_FOUR:
				break;
			case LISTVIEW_ITEM_COUNT_FIVE:
				break;
			case LISTVIEW_ITEM_COUNT_SIX:
				break;
			default:
				break;
			}
		} else if (DEVICE_MODE == DEVICE_MODE_WIFI) {
			connectToWifi();
		}
	}

	private void connectToWifi() {
//        WifiConfiguration config = new WifiConfiguration();   
//        config.allowedAuthAlgorithms.clear(); 
//        config.allowedGroupCiphers.clear(); 
//        config.allowedKeyManagement.clear(); 
//        config.allowedPairwiseCiphers.clear(); 
//        config.allowedProtocols.clear(); 
//        config.SSID = "\"" + "H3C" + "\"";
//        config.wepKeys[0] = ""; 
//        config.BSSID = "3c:e5:a6:1e:05:30";
//        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE); 
//        config.wepTxKeyIndex = 0;
//		 mDeviceWifiManager.addNetWork(config);
	};

	@Override
	public void onClick(View v) {
		if (DEVICE_MODE == DEVICE_MODE_DEFAULT) {
			if (v == getTitleLeftBtn()) {
				Intent intent = new Intent();
				intent.setClass(DeviceManagementActivity.this, CaptureActivity.class);
				startActivity(intent);
			}
		} else if (DEVICE_MODE == DEVICE_MODE_STICK) {
			switch(v.getId()) {
			case R.id.titlbarback: {
				finish();
				break;
			}
			}
		} else if (DEVICE_MODE == DEVICE_MODE_WIFI) {
			
		}
	}

	private void jumpToWifiList(DeviceInfo deviceInfo) {
		Intent intent = new Intent();
		intent.setClass(this, MobileDLNAActivity.class);
		intent.putExtra(DEVICE_MODE_KEY, DEVICE_MODE_WIFI);
		intent.putExtra(DEVICE_MODE_DATA, deviceInfo);
		startActivity(intent);
	}

	protected void onDeviceChanged(Message msg) {
		// TODO Auto-generated method stub
		super.onDeviceChanged(msg);
		mDeviceList = (ArrayList<DeviceInfo>) ((List<DeviceInfo>)msg.obj);
//		mDeviceList.add(new DeviceInfo("H3C", 0, "3c:e5:a6:1e:05:30", 0));
		mDeviceBaseAdapter.notifyDataSetChanged();
		mProgressLayout.setVisibility(View.GONE);
		Log.i("ddd", "------------------" + ((List<DeviceInfo>)msg.obj).size());
		Log.i("ddd", "------------------" + mDeviceList.size());
	}
}