package archermind.airplay;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.util.Log;

public class BonjourAgentRegister {
	
	public static final String TAG = "BonjourAgentRegister";
	public static final String AIRPLAY_SERVICE_TYPE = "_airplay._tcp.local.";
	public static final String AIRPLAY_SERVICE_NAME = "aShare_Test";
	public static final int AIRPLAY_SERVICE_PORT = 36667;
	
	public static final String AIRTUNES_SERVICE_TYPE = "_raop._tcp.local.";
	public static final int AIRTUNES_SERVICE_PORT = 49152; 
	public static final String AIRTUNES_SERVICE_NAME = "XXAirTunes";
	
	
	private static BonjourAgentRegister sBonjourAgentRegister;
	private MulticastLock mMulticastLock;
	android.net.wifi.WifiManager.MulticastLock lock;
	private Context mContext;
	private JmDNS mJmdns;
	
	private BonjourAgentRegister(Context context) {
		mContext = context;
	}
	public static BonjourAgentRegister getInstance(Context context) {
		if (sBonjourAgentRegister == null) {
			sBonjourAgentRegister = new BonjourAgentRegister(context);
		}
		return sBonjourAgentRegister;
	}

	public boolean registerBonjourService() {
		return registerBonjourService(null);
	}
	
	public boolean registerBonjourService(String name) {
		WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifi.getConnectionInfo();
		String macAddr = info.getMacAddress();
		Map<String, String> map = new HashMap<String, String>();
		map.put("deviceid", macAddr);
		map.put("features", "0x7");
		map.put("model", "AppleTV2,1");
		return registerBonjourService(AIRPLAY_SERVICE_TYPE, name != null ? name : AIRPLAY_SERVICE_NAME, AIRPLAY_SERVICE_PORT, map);
	}
	
	public boolean registerBonjourService(String type, String name, int port, Map map) {
		boolean ok = true;
		accquireMCLock();
		try {
			mJmdns = JmDNS.create();
			ServiceInfo pairservice = ServiceInfo.create(type,
					name, port, 0, 0, map);
			mJmdns.registerService(pairservice);
		} catch (IOException e) {
			Log.d(TAG, "registerService error");
			e.printStackTrace();
			ok = false;
		}
		return ok;
	}
	
	public void unRegisterBonjourService() {
		releaseMCLock();
		if (mJmdns == null) {
			return;
		}
		mJmdns.unregisterAllServices();
		mJmdns = null;
	}

	private void accquireMCLock() {
		WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		mMulticastLock = wifiManager.createMulticastLock("archermind.airplay");
		mMulticastLock.acquire();
	}
	
	private void releaseMCLock() {
		if (mMulticastLock != null) {
			mMulticastLock.acquire();
		}
	}
}
