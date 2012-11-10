package com.archermind.ashare.network;

public class Commands {
	public final static String KEY_CMD = "COMMAND-ID";
	
	public final static int CMD_INVALID = -1;
	
	// ------------------------------------------------------------
	// Command send from stick STB to Phone
	// ------------------------------------------------------------
	
	/**
	 *  server connection error
	 *  "COMMAND-ID":1000, "SERVER-CONN-ERROR-ID":$ERRORID(integer)
	 */
	public final static int CMD_SERVER_CONNECT_ERROR = 1000;

	public final static String KEY_SERVER_CONNECT_ERROR_ID = "SERVER-CONN-ERROR-ID";
	public final static int INVALID_ERROR_ID = -1;
	public final static int CONNECT_ERROR_EXCEED_MAX_CLIENT_COUNT = 0;
	
	// ------------------------------------------------------------
	// Command send from Phone to stick STB 
	// ------------------------------------------------------------
	/**
	 *  Request stick to connect to specific access point.
	 *  "COMMAND-ID":2000, "AP-SSID":$SSID(String), "AP-PASSWD":$password(String)
	 */
	public final static int CMD_STICK_CONNECT_AP_REQ = 2000;
	public final static String KEY_AP_SSID = "AP-SSID";
	public final static String KEY_AP_PASSWD = "AP-PASSWD";

	/**
	 *  Request stick to disconnect to current access point.
	 *  "COMMAND-ID":2001
	 */	
	public final static int CMD_STICK_DISCONNECT_WIFI = 2001;
	
	
	/**
	 *  Request stick to perform OTA.
	 *  "COMMAND-ID":2002
	 */	
	public final static int CMD_STICK_OTA = 2002;
	
	/**
	 *  Request stick to rename renderer device.
	 *  "COMMAND-ID":2010, "NEW-NAME":$NAME(String)
	 */	
	public final static int CMD_STICK_RENAME_RENDERER = 2010;
	public final static String KEY_FIRENDLY_NAME = "AP-SSID";
}
