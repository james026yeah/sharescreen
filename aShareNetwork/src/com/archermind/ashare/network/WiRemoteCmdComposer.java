package com.archermind.ashare.network;

import org.json.JSONException;
import org.json.JSONObject;


public class WiRemoteCmdComposer {	
	
	public static String obtainServerConnErrorCmd(int error) {
		JSONObject jsobj = new JSONObject();
		try {
			jsobj.put(Commands.KEY_CMD, 
					Commands.CMD_SERVER_CONNECT_ERROR);
			jsobj.put(Commands.KEY_SERVER_CONNECT_ERROR_ID, 
					error);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsobj.toString();
	}
	
	public static String obtainConnApCmd(String ssid, String passwd) {
		JSONObject jsobj = new JSONObject();
		try {
			jsobj.put(Commands.KEY_CMD, 
					Commands.CMD_STICK_CONNECT_AP_REQ);
			jsobj.put(Commands.KEY_AP_SSID, ssid);
			jsobj.put(Commands.KEY_AP_PASSWD, passwd);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsobj.toString();
	}
}
