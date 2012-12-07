package archermind.dlna.mobile;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.OnTabChangeListener;

public class MobileDLNAActivity extends TabActivity implements OnTabChangeListener {
	private static final String TAB_KEY = "tab_key";
	private TabHost mTabHost;
	private final static int TAB_INDEX_LOCAL_MEDIA = 0;
	private final static int TAB_INDEX_ONLINE_MEDIA = 1;
	private final static int TAB_INDEX_REMOTE_CONTROLLER = 2;
	private final static int TAB_INDEX_DEVICE_MANAGEMENT = 3;
	private final static int TAB_INDEX_MORE = 4;

	public static int mMode = 0;

	private final static String TAB_TAG_LOCAL_MEDIA = "local-media";
	private final static String TAB_TAG_ONLINE_MEDIA = "online-media";
	private final static String TAB_TAG_REMOTE_CONTROLLER = "remote-controller";
	private final static String TAB_TAG_DEVICE_MANAGEMENT = "device-management";
	private final static String TAB_TAG_MORE = "more";

//	private int mTabIndex;
	
	private TextView mRemoteControlCover;

	private SharedPreferences mSharedPreferences;
	private SharedPreferences.Editor mEditor;
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.mobile_dlna);
		mTabHost = getTabHost();
		mTabHost.setOnTabChangedListener(this);
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		mEditor = mSharedPreferences.edit();
		
		mRemoteControlCover = (TextView) findViewById(R.id.remoteControlCover);
		mRemoteControlCover.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MobileDLNAActivity.this, RemoteControllerActivity.class));
			}
		});
		
		setupLocalMediaTab();
		setupOnlineMediaTab();
		setupRemoteControllerTab();
		setupDeviceManagementTab();
		setupMoreTab();
		
	}

    public void setTab(int tabIndex) {
    	mTabHost.setCurrentTab(tabIndex);
    }

//	protected void onResume() {
//		super.onResume();
//		mTabIndex = mSharedPreferences.getInt(TAB_KEY, 0);
//
//		if (mTabHost != null) {
//			mTabHost.clearAllTabs();
//		}
//		Intent intent = getIntent();
//		mMode = intent.getIntExtra(DeviceManagementActivity.DEVICE_MODE_KEY, 0);
//		setupLocalMediaTab();
//		setupOnlineMediaTab();
//		setupRemoteControllerTab();
//		setupDeviceManagementTab();
//		setupMoreTab();
//		setTab(mTabIndex);
//	}

	public void onTabChanged(String tabId) {
		if (TAB_TAG_LOCAL_MEDIA.equals(tabId)) {			
			mEditor.putInt(TAB_KEY, TAB_INDEX_LOCAL_MEDIA);
		} else if (TAB_TAG_ONLINE_MEDIA.equals(tabId)) {
			mEditor.putInt(TAB_KEY, TAB_INDEX_ONLINE_MEDIA);
		} else if (TAB_TAG_REMOTE_CONTROLLER.equals(tabId)) {
			mEditor.putInt(TAB_KEY, TAB_INDEX_REMOTE_CONTROLLER);
		} else if (TAB_TAG_DEVICE_MANAGEMENT.equals(tabId)) {
			mEditor.putInt(TAB_KEY, TAB_INDEX_DEVICE_MANAGEMENT);
		} else if (TAB_TAG_MORE.equals(tabId)) {
			mEditor.putInt(TAB_KEY, TAB_INDEX_MORE);
		}
		mEditor.commit();
	}

	private void setupLocalMediaTab() {
		Intent intent = new Intent();
		intent.setClass(this, LocalMediaActivity.class);
		View mLocalTab = (View) LayoutInflater.from(this).inflate(R.layout.local_media_tab_inner, null);
		TabUtils.setTab(intent, TAB_INDEX_LOCAL_MEDIA);
		mTabHost.addTab(mTabHost.newTabSpec(TAB_TAG_LOCAL_MEDIA)
				.setIndicator(mLocalTab).setContent(intent));
	}

	private void setupOnlineMediaTab() {
		Intent intent = new Intent();
		intent.setClass(this, OnlineMediaActivity.class);
		View mOnlineTab = (View) LayoutInflater.from(this).inflate(R.layout.online_media_tab_inner, null);
		TabUtils.setTab(intent, TAB_INDEX_ONLINE_MEDIA);
		mTabHost.addTab(mTabHost.newTabSpec(TAB_TAG_ONLINE_MEDIA)
				.setIndicator(mOnlineTab).setContent(intent));
	}

	private void setupRemoteControllerTab() {
		Intent intent = new Intent();
		intent.setClass(this, RemoteControllerActivity.class);
		View mLocalTab = (View) LayoutInflater.from(this).inflate(R.layout.remote_controller_tab_inner, null);
		TabUtils.setTab(intent, TAB_INDEX_REMOTE_CONTROLLER);
		mTabHost.addTab(mTabHost.newTabSpec(TAB_TAG_REMOTE_CONTROLLER)
				.setIndicator(mLocalTab).setContent(intent));
	}

	private void setupDeviceManagementTab() {
		Intent intent = new Intent();
//		intent.setClass(this, DeviceManagementActivity.class);
		intent.setClass(this, DeviceConfigActivity.class);
		View mLocalTab = (View) LayoutInflater.from(this).inflate(R.layout.device_management_tab_inner, null);
		TabUtils.setTab(intent, TAB_INDEX_DEVICE_MANAGEMENT);
		mTabHost.addTab(mTabHost.newTabSpec(TAB_TAG_DEVICE_MANAGEMENT)
				.setIndicator(mLocalTab).setContent(intent));
	}

	private void setupMoreTab() {
		Intent intent = new Intent();
		intent.setClass(this, MoreOptionsActivity.class);
		View mLocalTab = (View) LayoutInflater.from(this).inflate(R.layout.more_options_tab_inner, null);
		TabUtils.setTab(intent, TAB_INDEX_MORE);
		mTabHost.addTab(mTabHost.newTabSpec(TAB_TAG_MORE).setIndicator(mLocalTab)
				.setContent(intent));
	}
}
