package com.archermind.ashare.misc;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.util.Log;

public class StickAccessPoint {
	static final String TAG = "StickAccessPoint";
	
    /** These values are matched in string arrays -- changes must be kept in sync */
    static final int SECURITY_NONE = 0;
    static final int SECURITY_WEP = 1;
    static final int SECURITY_PSK = 2;
    static final int SECURITY_EAP = 3;

    enum PskType {
        UNKNOWN,
        WPA,
        WPA2,
        WPA_WPA2
    }

    String ssid;
    String bssid;
    int security;
    int networkId;
    boolean wpsAvailable = false;

    PskType pskType = PskType.UNKNOWN;
    private WifiConfiguration mConfig;
    ScanResult mScanResult;
    
    private int getSecurity() {
		if (mScanResult.capabilities.contains("WEP")) {
		    return SECURITY_WEP;
		} else if (mScanResult.capabilities.contains("PSK")) {
		    return SECURITY_PSK;
		} else if (mScanResult.capabilities.contains("EAP")) {
		    return SECURITY_EAP;
		}
		return SECURITY_NONE;
    }    

    private static PskType getPskType(ScanResult result) {
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            return PskType.WPA_WPA2;
        } else if (wpa2) {
            return PskType.WPA2;
        } else if (wpa) {
            return PskType.WPA;
        } else {
            Log.w(TAG, "Received abnormal flag string: " + result.capabilities);
            return PskType.UNKNOWN;
        }
    }
    
    public StickAccessPoint(ScanResult result, String password) {
        loadResult(result);
        generateOpenNetworkConfig(password);
    }
    
    private void loadResult(ScanResult result) {
		mScanResult = result;
		ssid = result.SSID;
		bssid = result.BSSID;
		security = getSecurity();
		wpsAvailable = security != SECURITY_EAP && result.capabilities.contains("WPS");
		if (security == SECURITY_PSK)
		    pskType = getPskType(result);
		networkId = -1;
		//mRssi = result.level;        
    }

    protected void generateOpenNetworkConfig(String password) {       
		if (mConfig != null || mScanResult == null)
		    return;
		mConfig = new WifiConfiguration();
		mConfig.SSID = StickAccessPoint.convertToQuotedString(ssid);
		mConfig.BSSID = bssid;
		mConfig.hiddenSSID = true;
		switch(security) {
		case SECURITY_PSK:        	
			mConfig.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
			if(password.matches("[0-9A-Fa-f]{64}")) {
				mConfig.preSharedKey = password;
			} else {
				mConfig.preSharedKey = '"' + password + '"';
			}
			Log.d(TAG, "mConfig.preSharedKey=" + mConfig.preSharedKey);
			break;
		case SECURITY_EAP:
			mConfig.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
			mConfig.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
			break;
		case SECURITY_NONE:
			mConfig.allowedKeyManagement.set(KeyMgmt.NONE);
			break;
		}
		
		if(mScanResult.capabilities.contains("TKIP")) {
			mConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			mConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
		}
        
		if(mScanResult.capabilities.contains("CCMP")) {
			mConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			mConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
		}       
		
		Log.d(TAG, mConfig.toString());
    }

    static String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }
    
    public WifiConfiguration getConfig() {
        return mConfig;
    }
}
