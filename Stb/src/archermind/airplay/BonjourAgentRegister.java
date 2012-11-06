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

//import java.net.Inet4Address;
//import java.net.InetAddress;

public class BonjourAgentRegister {
	
	private static final String TAG = "BonjourAgentRegister";
	private static final String AIRPLAY_SERVICE_TYPE = "_airplay._tcp.local.";
	private static final String AIRPLAY_SERVICE_NAME = "aShare_Test";
	private static final int AIRPLAY_SERVICE_PORT = 36667;
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
		boolean ok = true;
		accquireMCLock();
		WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifi.getConnectionInfo();
		String macAddr = info.getMacAddress();
		try {
			mJmdns = JmDNS.create();
			Map<String, String> map = new HashMap<String, String>();
			map.put("deviceid", macAddr);
			map.put("features", "0x7");
			map.put("model", "AppleTV2,1");
			ServiceInfo pairservice = ServiceInfo.create(AIRPLAY_SERVICE_TYPE,
					AIRPLAY_SERVICE_NAME, AIRPLAY_SERVICE_PORT, 0, 0, map);
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
