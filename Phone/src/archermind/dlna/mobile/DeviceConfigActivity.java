package archermind.dlna.mobile;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.archermind.ashare.misc.DeviceInfo;
import com.archermind.ashare.misc.StickAccessPoint;
import com.archermind.ishare.apihacker.ApiHacker;

public class DeviceConfigActivity extends BaseActivity {

	private static final String TAG = "DeviceConfigActivity";

	private static final String CUSTOM_SSID_PREFIX = "##";

	private static final int DIALOG_SSID_AND_PWD = 0;
	private static final int DIALOG_WAIT_FOR_TV = 1;
	private static final int DIALOG_NO_DEVICE = 2;

	private ListView mDevicesListView;
	private final ArrayList<DeviceInfo> mDeviceList = new ArrayList<DeviceInfo>();
	private ScanResult mTargetAP = null;
	private String mPassword = "";
	private final List<ScanResult> mScanResult = new ArrayList<ScanResult>();

	private static final int DEVICE_LIST_MODE_AP = 0;
	private static final int DEVICE_LIST_MODE_TV = 1;
	private static int mCurrentMode = DEVICE_LIST_MODE_TV;

	private DeviceBaseAdapter mDeviceBaseAdapter;

	private WifiManager mWifiManager;

	private Animation mProgressBarAnim;
	private LinearLayout mProgressBarContainer;
	private ImageView mProgressIcon;

	private TextView mTitle;
	private ImageView mLeftTopBtn;
	
	private TextView mSSID;
	
	private boolean mPaused = false;

	private AdapterView.OnItemClickListener mItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (mCurrentMode == DEVICE_LIST_MODE_AP) {
				Log.d(TAG, "Show password input dialog: ");
				mTargetAP = mScanResult.get(position);
				showDialog(DIALOG_SSID_AND_PWD);
				mSSID.setText("SSID: " + mTargetAP.SSID);
			} else {
				Log.d(TAG, "Connect to render: "
						+ mDeviceList.get(position).mDevName);
				DeviceConfigActivity.this.connectDevice(mDeviceList
						.get(position));
			}

		}
	};

	void showProgress() {
		mProgressBarContainer.setVisibility(View.VISIBLE);
		mProgressIcon.startAnimation(mProgressBarAnim);
	}

	void dismissProgress() {
		mProgressBarContainer.setVisibility(View.INVISIBLE);
		mProgressIcon.clearAnimation();
	}

	void showTVList() {
		mCurrentMode = DEVICE_LIST_MODE_TV;
		mLeftTopBtn.setImageResource(R.drawable.btn_add_sel);
//		mLeftTopBtn.setText(R.string.device_config_title_add);
		mTitle.setText(R.string.device_config_title_tv);
		if (mDeviceList.isEmpty()) {
			showProgress();
		} else {
			dismissProgress();
		}
		mDeviceBaseAdapter.notifyDataSetChanged();
	}

	void showAPList() {
		mCurrentMode = DEVICE_LIST_MODE_AP;
		mLeftTopBtn.setImageResource(R.drawable.btn_local_connection_sel);
//		mLeftTopBtn.setText(R.string.device_config_title_local);
		mTitle.setText(R.string.device_config_title_ap);
		mScanResult.clear();
		List<ScanResult> list = mWifiManager.getScanResults();
		if (list != null) {
			mScanResult.addAll(list);
		}
		mDeviceBaseAdapter.notifyDataSetChanged();
	}

	protected void onDeviceConnected(DeviceInfo info) {
		Log.d(TAG, "onDeviceConnected: ");
		for (DeviceInfo device : mDeviceList) {
			Log.d(TAG, "onDeviceConnected name: " + device.mDevName);
			if (device.mUDN.equals(info.mUDN)) {
				Log.d(TAG, "onDeviceConnected udn: " + device.mUDN);
				Log.d(TAG, "onDeviceConnected state: " + info.mState);
				device.mState = info.mState;
			} else {
				device.mState = DeviceInfo.DEV_STATE_DISCONNECTED;
			}
		}
		mDeviceBaseAdapter.notifyDataSetChanged();
	};

	private String getCustomSSID() {
		String result = "";
		String[] temp = mTargetAP.BSSID.split(":");
		result = CUSTOM_SSID_PREFIX + temp[4] + temp[5] + mPassword;
		return result;
	}

	private BroadcastReceiver mNetworkReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityManager connManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			State state = connManager.getNetworkInfo(
					ConnectivityManager.TYPE_WIFI).getState();
			if (State.CONNECTED == state) {
				Log.d(TAG, "onReceive: WIFI CONNECTED");
				if (mTargetAP != null) {
					connectToTargetAp(mTargetAP);
				}
			}

		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate: ");

		setContentView(R.layout.device_management);

		mProgressBarContainer = (LinearLayout) findViewById(R.id.progress_bar_container);
		mProgressBarAnim = AnimationUtils.loadAnimation(this, R.anim.progress_bar_anim);
		mProgressBarAnim.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				mDevicesListView.setVisibility(View.INVISIBLE);
			}
			@Override
			public void onAnimationRepeat(Animation animation) {}
			@Override
			public void onAnimationEnd(Animation animation) {
				mDevicesListView.setVisibility(View.VISIBLE);
			}
		});
		
		mProgressIcon = (ImageView) findViewById(R.id.progress_icon);

		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		mDevicesListView = (ListView) findViewById(R.id.devices_listView);
		mDevicesListView.setOnItemClickListener(mItemClickListener);

		mDeviceBaseAdapter = new DeviceBaseAdapter(this);
		mDevicesListView.setAdapter(mDeviceBaseAdapter);

		mTitle = (TextView) findViewById(R.id.title);

		mLeftTopBtn = (ImageView) findViewById(R.id.image_left_top);
		mLeftTopBtn.setImageResource(R.drawable.btn_add_sel);
//		mLeftTopBtn.setBackgroundResource(R.drawable.left_top_btn_bg);
//		mLeftTopBtn.setText(R.string.device_config_title_add);
		mLeftTopBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mCurrentMode == DEVICE_LIST_MODE_AP) {
					WifiConfiguration wc = new WifiConfiguration();
					wc.SSID = CUSTOM_SSID_PREFIX;
					wc.allowedKeyManagement.set(KeyMgmt.NONE);
					mWifiManager.setWifiEnabled(false);
					ApiHacker.setWifiApEnabled(mWifiManager, wc, true);
					showDialog(DIALOG_WAIT_FOR_TV);
				} else {
					dismissProgress();
					showAPList();
				}
			}
		});

//		TextView temp = (TextView) findViewById(R.id.btn_right_top);
//		temp.setVisibility(View.VISIBLE);
//		temp.setBackgroundResource(R.drawable.btn_refresh_sel);
//		temp.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				if (mCurrentMode == DEVICE_LIST_MODE_AP) {
//					showAPList();
//				} else {
//					getDeviceList();
//					showProgress();
//				}
//			}
//		});
		
		ImageView temp = (ImageView) findViewById(R.id.image_right_top);
		temp.setVisibility(View.VISIBLE);
		temp.setImageResource(R.drawable.btn_refresh_sel);
		temp.setBackgroundResource(R.drawable.image_right_top_bg_sel);
		temp.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mCurrentMode == DEVICE_LIST_MODE_AP) {
					showAPList();
				} else {
					getDeviceList();
					showProgress();
				}
			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume: ");
		mPaused = false;
		IntentFilter filter = new IntentFilter();  
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION); 
		registerReceiver(mNetworkReceiver, filter); 
		showTVList();
	}
	
	@Override
	protected void onPause() {
		mPaused = true;
		super.onPause();
		unregisterReceiver(mNetworkReceiver);
	}

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		getDeviceList();
	}

	@Override
	protected void onGetDeviceList(ArrayList<DeviceInfo> devices) {
		super.onGetDeviceList(devices);

		if (devices.isEmpty()) {
			Log.d(TAG, "Device list is empty ? true");
			if (!mPaused) {
				showDialog(DIALOG_NO_DEVICE);
			}
		} else {
			Log.d(TAG, "Device list is empty ? false");
			mDeviceList.clear();
			mDeviceList.addAll(devices);
			mDeviceBaseAdapter.notifyDataSetChanged();
		}
		dismissProgress();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = new Dialog(this, R.style.Theme_DeleteImageDialog);
		View dlgContent;
		switch (id) {
		case DIALOG_SSID_AND_PWD:
			dlgContent = getLayoutInflater().inflate(
					R.layout.dialog_ssid_and_pwd, null);
			dialog.setContentView(dlgContent);

			mSSID = (TextView) dlgContent.findViewById(R.id.item_ssid);
			mSSID.setText("SSID: " + mTargetAP.SSID);

			final EditText pwd = (EditText) dialog.findViewById(R.id.item_pwd);
			TextView send = (TextView) dlgContent.findViewById(R.id.item_ok);
			send.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mPassword = pwd.getText().toString();
					Log.d(TAG, "Broadcast custom SSID and password: "
							+ mTargetAP.SSID);
					pwd.setText("");
					WifiConfiguration wc = new WifiConfiguration();
					// wc.SSID = "\"" + getCustomSSID() + "\"";
					wc.SSID = getCustomSSID();
					wc.allowedKeyManagement.set(KeyMgmt.NONE);
					mWifiManager.setWifiEnabled(false);
					ApiHacker.setWifiApEnabled(mWifiManager, wc, true);
					dismissDialog(DIALOG_SSID_AND_PWD);
					showDialog(DIALOG_WAIT_FOR_TV);
				}
			});
			break;
		case DIALOG_WAIT_FOR_TV:
			dlgContent = getLayoutInflater().inflate(R.layout.dialog_tv_check,
					null);
			dialog.setContentView(dlgContent);

			send = (TextView) dlgContent.findViewById(R.id.item_ok);
			send.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mCurrentMode = DEVICE_LIST_MODE_TV;
					ApiHacker.setWifiApEnabled(mWifiManager,
							new WifiConfiguration(), false);
					mWifiManager.setWifiEnabled(true);
					dismissDialog(DIALOG_WAIT_FOR_TV);
					showTVList();
				}
			});
			break;
		case DIALOG_NO_DEVICE:
			dlgContent = getLayoutInflater().inflate(R.layout.dialog_no_device,
					null);
			dialog.setContentView(dlgContent);

			send = (TextView) dlgContent.findViewById(R.id.item_ok);
			send.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					showAPList();
					dismissDialog(DIALOG_NO_DEVICE);
				}
			});
			break;
		default:
			break;
		}
		return dialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);

	}

	protected void onDeviceChanged(Message msg) {
		super.onDeviceChanged(msg);
		Log.d(TAG, "onDeviceChanged: ");
		mDeviceList.clear();
		mDeviceList.addAll((ArrayList<DeviceInfo>) msg.obj);
		mDeviceBaseAdapter.notifyDataSetChanged();
		dismissProgress();
	}

	private void connectToTargetAp(ScanResult targetAp) {

		WifiConfiguration wifiConfig = getSavedWifiConfig(targetAp.BSSID);
		if (wifiConfig != null) {
			boolean result = mWifiManager.enableNetwork(wifiConfig.networkId,
					true);
			Log.v(TAG, "Found remembered wifi config id:"
					+ wifiConfig.networkId + " enable result:" + result);
		} else {
			StickAccessPoint stickAp = new StickAccessPoint(targetAp, mPassword);
			wifiConfig = stickAp.getConfig();
			int res = mWifiManager.addNetwork(wifiConfig);
			Log.v(TAG, "add Network returned " + res);
			boolean result = mWifiManager.enableNetwork(res, true);
			Log.v(TAG, "enableNetwork returned " + result);
		}
	}

	private WifiConfiguration getSavedWifiConfig(String bssid) {
		WifiConfiguration wifiConfig = null;
		List<WifiConfiguration> configs = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
				.getConfiguredNetworks();
		if (configs == null) {
			return null;
		}
		for (WifiConfiguration config : configs) {
			if (config.BSSID != null && config.BSSID.equals(bssid)) {
				wifiConfig = config;
				break;
			}
		}
		return wifiConfig;
	}

	private class DeviceBaseAdapter extends BaseAdapter {

		private Context mContext;

		public DeviceBaseAdapter(Context context) {
			mContext = context;
		}

		@Override
		public int getCount() {
			int result = 0;
			if (mCurrentMode == DEVICE_LIST_MODE_AP) {
				result = mScanResult == null ? 0 : mScanResult.size();
			} else {
				result = mDeviceList == null ? 0 : mDeviceList.size();
			}
			return result;
		}

		@Override
		public Object getItem(int arg0) {
			Object result = null;
			if (mCurrentMode == DEVICE_LIST_MODE_AP) {
				result = mScanResult.get(arg0);
			} else {
				result = mDeviceList.get(arg0);
			}
			return result;
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int position, View converView,
				android.view.ViewGroup parent) {
			DeviceHolder holder = null;
			if (mCurrentMode == DEVICE_LIST_MODE_AP) {
				converView = getLayoutInflater().from(mContext).inflate(
						R.layout.ap_list_item, null);
				holder = new DeviceHolder(converView);
				ScanResult deviceInfo = mScanResult.get(position);
				holder.tvNameText.setText(deviceInfo.SSID);
				if (mWifiManager.getConnectionInfo().getBSSID() != null
						&& mWifiManager.getConnectionInfo().getBSSID()
								.equals(deviceInfo.SSID)) {
					holder.tvImage
							.setBackgroundResource(R.drawable.wifi_list_icon_lock);
				} else {
					holder.tvImage
							.setBackgroundResource(R.drawable.wifi_list_icon_lock);
				}
			} else {
				converView = getLayoutInflater().from(mContext).inflate(
						R.layout.devices_list_item, null);
				holder = new DeviceHolder(converView);
				DeviceInfo deviceInfo = mDeviceList.get(position);
				holder.tvNameText.setText(deviceInfo.mDevName);
				Log.d(TAG, "deviceInfo.mState: " + deviceInfo.mState);
				if (deviceInfo.mState == DeviceInfo.DEV_STATE_CONNECTED) {
					holder.linkStateBtn
							.setBackgroundResource(R.drawable.icon_setting);
					holder.linkStateBtn
							.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
//									startActivity(new Intent(
//											DeviceConfigActivity.this,
//											ConfigTVActivity.class));
								}
							});
				} else {
					holder.linkStateBtn
							.setBackgroundResource(android.R.color.transparent);
					// holder.linkStateBtn.setText(R.string.not_link);
				}
			}
			converView.setTag(holder);
			return converView;
		}
	}

	class DeviceHolder {
		ImageView tvImage;
		TextView tvNameText;
		TextView tvSshIdText;
		Button linkStateBtn;

		public DeviceHolder(View itemView) {
			this.tvImage = (ImageView) itemView.findViewById(R.id.tv_iamge);
			this.tvNameText = (TextView) itemView.findViewById(R.id.tv_name);
			this.tvSshIdText = (TextView) itemView.findViewById(R.id.tv_sshid);
			this.linkStateBtn = (Button) itemView.findViewById(R.id.link_state);
		}
	}

}
