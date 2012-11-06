package archermind.dlna.mobile.data;

import java.util.ArrayList;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ScanDevicesService extends Service {

	private static final String UPDATE_LISTVIEW = "android.action.update_listView";
	private static final String TV_DATA_KEY = "tv_data_key";

	private ScanBinder mScanBinder = new ScanBinder();
	private TvDevicesInfo mTvDevicesData;

	private boolean mIsUpdateListView = false;
	private boolean mIsClearDatabase = false;
	private boolean mIsScanFinish = false;

	private ArrayList<TvDevicesInfo> mTvDevicesList = null;

	public class ScanBinder extends Binder {
		public boolean getIsUpdateDevicesList() {
			return mIsUpdateListView;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mScanBinder;
	}

	private void initComponents() {
		mTvDevicesList = new ArrayList<TvDevicesInfo>();
	}

	private void clearList() {
		if (mTvDevicesList != null) {
			mTvDevicesList.clear();
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();

		initComponents();
		clearList();
		scanTVDevices();
		getbooleanUpdateListView();
		getIsUpdateListView();
	}

	private void scanTVDevices() {
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	private void getbooleanUpdateListView() {
		if (mIsScanFinish && mTvDevicesList != null && mTvDevicesList.size() > 0) {
			mIsClearDatabase = true;
		} else {
			mIsClearDatabase = false;
		}
	}

	private boolean getIsUpdateListView() {
		if (mIsClearDatabase) {
			clearDatabase();
			insertDatebase();
			mIsUpdateListView = true;
		} else {
			clearDatabase();
			insertDatebase();
			mIsUpdateListView = true;
		}
		return mIsUpdateListView;
	}

	private void clearDatabase() {
		getContentResolver().delete(TvDevicesProvider.CONTENT_URI, TvDevicesProvider._ID + ">0", null);
	}

	private void insertDatebase() {
		ContentValues values = new ContentValues();
		values.clear();

		values.put(TvDevicesProvider.FRIENDY_NAME, "TV Dongle0");
		values.put(TvDevicesProvider.LINK_STATE, 0);
		values.put(TvDevicesProvider.SS_ID, "8330998");
		values.put(TvDevicesProvider.PASS_WORD, "530415");

		getContentResolver().insert(TvDevicesProvider.CONTENT_URI, values);
	}

}
