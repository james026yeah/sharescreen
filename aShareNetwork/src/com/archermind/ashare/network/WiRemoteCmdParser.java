package com.archermind.ashare.network;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;


public class WiRemoteCmdParser {
	private final static String TAG = "WiRemoteCmdParser";
	private JSONObject mJSON;
	public WiRemoteCmdParser(String message) {
		try {
			mJSON = new JSONObject(message);
		} catch (JSONException e) {
			Log.e(TAG, "Parse failed");
			mJSON  = null;
			e.printStackTrace();
		}
	}
	
	public int getInt(String key, int defaultVal) {
		int val = defaultVal;
		if(mJSON != null) {
			try {
				val = mJSON.getInt(key);
			} catch (JSONException e) {
				Log.e(TAG, "Parse failed key:" + key);
				e.printStackTrace();
			}
		}
		return val;
	}
	
	public long getLong(String key, long defaultVal) {
		long val = defaultVal;
		if(mJSON != null) {
			try {
				val = mJSON.getLong(key);
			} catch (JSONException e) {
				Log.e(TAG, "Parse failed key:" + key);
				e.printStackTrace();
			}
		}
		return val;
	}
	
	public String getString(String key) {
		String val = null;
		if(mJSON != null) {
			try {
				val = mJSON.getString(key);
			} catch (JSONException e) {
				Log.e(TAG, "Parse failed key:" + key);
				e.printStackTrace();
			}
		}
		return val;
	}
}
