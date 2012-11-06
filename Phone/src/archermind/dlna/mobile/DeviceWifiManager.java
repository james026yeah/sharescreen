package archermind.dlna.mobile;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.util.Log;

public class DeviceWifiManager {
     private WifiManager mWifiManager;
     private WifiInfo mWifiInfo;
     private List<ScanResult> mWifiList;
     private List<WifiConfiguration> mWifiConfigurations;
     WifiLock mWifiLock;

 	private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent intent) {
        	String action = intent.getAction();
        	if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)){

        	} else if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
        		int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
        		
        	} else if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
        		NetworkInfo networkInfo = (NetworkInfo)intent.getParcelableExtra(
        				WifiManager.EXTRA_NETWORK_INFO);
        		DetailedState detailState = networkInfo.getDetailedState();
        	}
        }
    };
     
     
     public DeviceWifiManager(Context context){  
         mWifiManager=(WifiManager) context.getSystemService(Context.WIFI_SERVICE);  
         mWifiInfo = mWifiManager.getConnectionInfo();  
     }

     public void openWifi(){
         if(!mWifiManager.isWifiEnabled()){
             mWifiManager.setWifiEnabled(true);
         }
     }

     public void closeWifi(){
         if(!mWifiManager.isWifiEnabled()){
             mWifiManager.setWifiEnabled(false);
         }  
     }

     public int checkState() {
         return mWifiManager.getWifiState();    
     }
 
     public void acquireWifiLock(){
         mWifiLock.acquire();  
     }

     public void releaseWifiLock(){

         if(mWifiLock.isHeld()){  
             mWifiLock.acquire();  
         }
     }

     public void createWifiLock(){
         mWifiLock=mWifiManager.createWifiLock("test");
     }

     public List<WifiConfiguration> getConfiguration(){  
         return mWifiConfigurations;  
     }

     public void connetionConfiguration(int index){  
         if(index>mWifiConfigurations.size()){  
             return ;  
         }
         mWifiManager.enableNetwork(mWifiConfigurations.get(index).networkId, true);  
     }
     public void startScan(){
         mWifiManager.startScan();  
         mWifiList=mWifiManager.getScanResults();
         mWifiConfigurations=mWifiManager.getConfiguredNetworks();  
     }

     public List<ScanResult> getWifiList(){
         return mWifiList;  
     }

     public StringBuffer lookUpScan(){
         StringBuffer sb=new StringBuffer();
         for(int i=0;i<mWifiList.size();i++){
             sb.append("Index_" + new Integer(i + 1).toString() + ":");
             sb.append((mWifiList.get(i)).toString()).append("\n");
         }
         return sb;
     }

     public String getMacAddress(){
         return (mWifiInfo==null) ? "NULL" : mWifiInfo.getMacAddress();
     }

     public String getBSSID(){
         return (mWifiInfo==null) ? "NULL" : mWifiInfo.getBSSID();
     }

     public int getIpAddress(){
         return (mWifiInfo==null) ? 0 : mWifiInfo.getIpAddress();  
     }

     public int getNetWordId(){
         return (mWifiInfo==null) ? 0 : mWifiInfo.getNetworkId();  
     }


     public String getWifiInfo(){
         return (mWifiInfo == null) ? "NULL" : mWifiInfo.toString();  
     }

     public void addNetWork(WifiConfiguration configuration){
         int wcgId=mWifiManager.addNetwork(configuration);
         mWifiManager.enableNetwork(wcgId, true);
     }

     public void disConnectionWifi(int netId){  
         mWifiManager.disableNetwork(netId);  
         mWifiManager.disconnect();  
     }

     public WifiConfiguration CreateWifiInfo(String SSID, String Password, int Type) { 
         WifiConfiguration config = new WifiConfiguration();   
          config.allowedAuthAlgorithms.clear(); 
          config.allowedGroupCiphers.clear(); 
          config.allowedKeyManagement.clear(); 
          config.allowedPairwiseCiphers.clear(); 
          config.allowedProtocols.clear(); 
          config.SSID = "\"" + SSID + "\"";
          config.wepKeys[0] = ""; 
          config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE); 
          config.wepTxKeyIndex = 0; 
          return config; 
       }
}
