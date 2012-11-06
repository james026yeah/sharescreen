package archermind.dlna.mobile;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import archermind.dlna.miscellaneous.DeviceInfo;
import archermind.dlna.miscellaneous.StickAccessPoint;

import com.archermind.ishare.apihacker.ApiHacker;

public class DeviceConfigActivity extends BaseActivity {

	private static final String TAG = "DeviceConfigActivity";

	private static final String CUSTOM_SSID_PREFIX = "##";

	private static final int DIALOG_SSID_AND_PWD = 0;
	private static final int DIALOG_WAIT_FOR_TV = 1;

	private ListView mDevicesListView;
	private ArrayList<DeviceInfo> mDeviceList = null;
	private DeviceInfo mTargetAP = null;
	private String mBSSIDOfRequestAP = null;

	private DeviceBaseAdapter mDeviceBaseAdapter;

	private WifiManager mWifiManager;

	private AdapterView.OnItemClickListener mItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Log.d(TAG, "onItemClick mDevName: "
					+ mDeviceList.get(position).mDevName);
			if (mDeviceList.get(position).mDevType == DeviceInfo.DEV_TYPE_AP) {
				Log.d(TAG, "Show password input dialog: ");
				mTargetAP = mDeviceList.get(position);
				showDialog(DIALOG_SSID_AND_PWD);
			} else {
				Log.d(TAG, "Connect to render: "
						+ mDeviceList.get(position).mDevName);
			}

		}
	};

	private String getCustomSSID() {
		String result = "";
		String[] temp = mTargetAP.mBSSID.split(":");
		result = CUSTOM_SSID_PREFIX + temp[4] + temp[5] + mTargetAP.mPassword;
		return result;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(TAG, "onCreate: ");

		setCustomContentView(R.layout.device_management, true);
		setCustomTitle(0, R.string.scan_two_dimension_code,
				R.string.devices_list, R.drawable.default_version_update);

		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume: ");

		initDeviceComponents();

	}

	private void initDeviceComponents() {
		mDeviceList = new ArrayList<DeviceInfo>();
		updateApList();
		mDevicesListView = (ListView) findViewById(R.id.devices_listView);
		mDevicesListView.setOnItemClickListener(mItemClickListener);

		mDeviceBaseAdapter = new DeviceBaseAdapter(this);
		mDevicesListView.setAdapter(mDeviceBaseAdapter);
		mDeviceBaseAdapter.notifyDataSetChanged();
	}

	private void updateApList() {
		List<ScanResult> results = mWifiManager.getScanResults();
		if (results != null) {
			DeviceInfo tempInfo;
			for (ScanResult scanResult : results) {
				tempInfo = new DeviceInfo(scanResult.SSID,
						DeviceInfo.DEV_TYPE_AP,
						DeviceInfo.DEV_STATE_DISCONNECTED, "Location");
				tempInfo.mBSSID = scanResult.BSSID;
				mDeviceList.add(tempInfo);
			}
		}
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

			final EditText pwd = (EditText) dialog.findViewById(R.id.item_pwd);
			TextView send = (TextView) dlgContent.findViewById(R.id.item_ok);
			send.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mTargetAP.mPassword = pwd.getText().toString();
					Log.d(TAG, "Broadcast custom SSID and password: "
							+ mTargetAP.mDevName);
					WifiConfiguration wc = new WifiConfiguration();
//					wc.SSID = "\"" + getCustomSSID() + "\"";
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
					// WifiConfiguration wc = new WifiConfiguration();
					// wc.SSID = "\""+ getCustomSSID() +"\"";
					// wc.allowedKeyManagement.set(KeyMgmt.NONE);
					// mWifiManager.setWifiEnabled(false);
					// ApiHacker.setWifiApEnabled(mWifiManager, wc, true);
					// dialog.dismiss();
					Toast.makeText(getApplicationContext(),
							"Connect to target AP", Toast.LENGTH_SHORT).show();
					dismissDialog(DIALOG_WAIT_FOR_TV);
					mDeviceList.clear();
					mDeviceBaseAdapter.notifyDataSetChanged();
					ApiHacker.setWifiApEnabled(mWifiManager, new WifiConfiguration(), false);
					mWifiManager.setWifiEnabled(true);
				}
			});
			break;
		default:
			break;
		}
		return dialog;
	}
	
	protected void onDeviceChanged(Message msg) {
		// TODO Auto-generated method stub
		super.onDeviceChanged(msg);
		mDeviceList = (ArrayList<DeviceInfo>)msg.obj;
		mDeviceBaseAdapter.notifyDataSetChanged();
//		mProgressLayout.setVisibility(View.GONE);
		Log.i("ddd", "------------------" + ((List<DeviceInfo>)msg.obj).size());
		Log.i("ddd", "------------------" + mDeviceList.size());
	}

	private void connectToTargetAp(DeviceInfo devInfo) {
		if (devInfo == null) {
			Log.e(TAG, "Device info should not be null!");
		}

		if (devInfo.mDevType != DeviceInfo.DEV_TYPE_AP) {
			Log.e(TAG, "Wrong device type!");
			return;
		}

		if (isWifiConnected(devInfo.mBSSID)) {
			Log.d(TAG, "Already connected!");
			return;
		}
		ScanResult targetAp = null;
		for (ScanResult ap : mWifiManager.getScanResults()) {
			if (ap.BSSID.equals(devInfo.mBSSID)) {
				targetAp = ap;
				break;
			}
		}

		// if the AP configure is remembered
		mBSSIDOfRequestAP = targetAp.BSSID;
		WifiConfiguration wifiConfig = getSavedWifiConfig(targetAp.BSSID);
		if (wifiConfig != null) {
			boolean result = mWifiManager.enableNetwork(wifiConfig.networkId, true);
			Log.v(TAG, "Found remembered wifi config id:"
					+ wifiConfig.networkId + " enable result:" + result);
		} else {
			StickAccessPoint stickAp = new StickAccessPoint(targetAp,
					devInfo.mPassword);
			wifiConfig = stickAp.getConfig();
			int res = mWifiManager.addNetwork(wifiConfig);
			Log.v(TAG, "add Network returned " + res);
			boolean result = mWifiManager.enableNetwork(res, true);
			Log.v(TAG, "enableNetwork returned " + result);
		}
	}
	
    private boolean isWifiConnected(String bssid) {
    	boolean isConnected = false;
    	WifiInfo wifiInfo = mWifiManager.getConnectionInfo();    	
    	if(wifiInfo != null && bssid.equals(wifiInfo.getBSSID()) &&
    			wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
    		
    		isConnected = true;
    	}
    	return isConnected;
    }
    
    private WifiConfiguration getSavedWifiConfig(String bssid) {
    	WifiConfiguration wifiConfig = null;
    	List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
    	for(WifiConfiguration config : configs) {
    		if(config.BSSID != null && config.BSSID.equals(bssid)) {
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
		public View getView(int position, View converView,
				android.view.ViewGroup parent) {
			DeviceHolder holder = null;
			if (converView == null) {
				converView = getLayoutInflater().from(mContext).inflate(
						R.layout.devices_list_item, null);
				holder = new DeviceHolder(converView);
				DeviceInfo deviceInfo = mDeviceList.get(position);
				holder.tvNameText.setText(deviceInfo.mDevName);
				holder.tvSshIdText.setText(" ( " + deviceInfo.mBSSID + " ) ");
				if (deviceInfo.mState == DeviceInfo.DEV_STATE_CONNECTED) {
					holder.linkStateBtn
							.setBackgroundResource(R.drawable.icon_connected);
				} else {
					holder.linkStateBtn.setText(R.string.not_link);
				}
				converView.setTag(holder);
			} else {
				holder = (DeviceHolder) converView.getTag();
			}
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
