package archermind.dlna.mobile;

public class MessageDefs {
	// /////////////////////////////////////////////////////////////
	// Message Definitions for DLNA Service
	// /////////////////////////////////////////////////////////////

	public final static int MSG_SERVICE_REGISTER_CLIENT = 0;

	public final static int MSG_SERVICE_UNREGISTER_CLIENT = 1;

	/**
	 * Message definition For service
	 */
	public final static int MSG_SERVICE_SUBSCRIBE_DEVICE_CHANGE = 2;

	public final static int MSG_SERVICE_UNSUBSCRIBE_DEVICE_CHANGE = 3;

	public final static int MSG_SERVICE_ON_DEVICE_CHANGED = 4;

	public final static int MSG_SERVICE_QUERY_LOCAL_MDMS_STATUS = 6;

	public final static int MSG_SERVICE_ON_GET_LOCAL_MDMS_STATUS = 7;

	public final static int MSG_SERVICE_ON_LOCAL_MDMS_STATUS_CHANGED = 8;

	public final static int MSG_SERVICE_GET_DEVICE_LIST = 9;
	
	public final static int MSG_SERVICE_ON_GET_DEVICE_LIST = 10;
	
	public final static int MSG_SERVICE_CONNECT_DEVICE = 11;
	
	public final static int MSG_SERVICE_ON_DEVICE_CONNECTED = 12;

	// /////////////////////////////////////////////////////////////
	// Message Definitions for M-DMC thread
	// /////////////////////////////////////////////////////////////
	public final static int MSG_MDMC_ON_PROC_PREPARED = 100;

	public final static int MSG_MDMC_STOP_PROC = 101;

	public final static int MSG_MDMC_START_DMC = 110;

	public final static int MSG_MDMC_STOP_DMC = 111;

	public final static int MSG_MDMC_SEARCH_DEVICE = 120;

	public final static int MSG_MDMC_ON_DEV_ADDED = 121;

	public final static int MSG_MDMC_ON_DEV_REMOVED = 122;

	public final static int MSG_MDMC_ON_SEARCH_RESPONSE = 123;

	public final static int MSG_MDMC_CONTENT_DIR_BROWSE = 130;

	public final static int MSG_MDMC_ON_GET_CONENT = 131;

	public final static int MSG_MDMC_AV_TRANS_PLAY = 140;

	public final static int MSG_MDMC_AV_TRANS_PAUSE = 141;

	public final static int MSG_MDMC_AV_TRANS_STOP = 142;

	public final static int MSG_MDMC_AV_TRANS_SETPLAYMODE = 143;

	public final static int MSG_MDMC_GET_MUSIC_CATEGORY_DATA = 150;

	public final static int MSG_MDMC_ON_GET_MUSIC_CATEGORY_DATA = 151;

	public final static int MSG_MDMC_GET_MUSIC_ALBUMS_DATA = 152;

	public final static int MSG_MDMC_ON_GET_MUSIC_ALBUMS_DATA = 153;

	public final static int MSG_MDMC_GET_MUSIC_ARTISTS_DATA = 154;

	public final static int MSG_MDMC_ON_GET_MUSIC_ARTISTS_DATA = 155;

	public final static int MSG_MDMC_GET_MUSIC_ALL_DATA = 156;

	public final static int MSG_MDMC_ON_GET_MUSIC_ALL_DATA = 157;

	public final static int MSG_MDMC_GET_VIDEOS_DATA = 158;

	public final static int MSG_MDMC_ON_GET_VIDEOS_DATA = 159;

	public final static int MSG_MDMC_GET_PHOTOS_DATA = 160;

	public final static int MSG_MDMC_ON_GET_PTOTOS_DATA = 161;

	public final static int MSG_MDMC_AV_TRANS_GETPOSITIONINFO = 165;

	public final static int MSG_MDMC_AV_TRANS_SEEK = 166;

	public final static int MSG_MDMC_AV_TRANS_NEXT = 167;

	public final static int MSG_MDMC_AV_TRANS_PREVIOUS = 168;

	public final static int MSG_MDMC_AV_TRANS_SETAVTRANSPORTURI = 169;

	public final static int MSG_MDMC_AV_TRANS_SETVOLUME = 170;

	public final static int MSG_MDMC_AV_TRANS_GETVOLUME = 171;

	public final static int MSG_MDMC_AV_TRANS_SETMUTE = 172;

	public final static int MSG_MDMC_AV_TRANS_GETMUTE = 173;

	public final static int MSG_MDMC_AV_TRANS_GETCURRENTTRANSPORTACTIONS = 174;

	public final static int MSG_MDMC_AV_TRANS_GETTRANSPORTINFO = 175;

	public final static int MSG_MDMC_AV_TRANS_PAUSETOPLAY = 176;

	public final static int MSG_MDMC_ON_GET_PLAY = 177;

	public final static int MSG_MDMC_ON_GET_PAUSETOPLAY = 178;

	public final static int MSG_MDMC_ON_GET_STOP = 179;

	public final static int MSG_MDMC_ON_GET_PAUSE = 180;

	public final static int MSG_MDMC_ON_GET_NEXT = 181;

	public final static int MSG_MDMC_ON_GET_PREVIOUS = 182;

	public final static int MSG_MDMC_ON_GET_GETPOSITIONINFO = 183;

	public final static int MSG_MDMC_ON_GET_SEEK = 184;

	public final static int MSG_MDMC_ON_GET_SETAVTRANSPORTURI = 185;

	public final static int MSG_MDMC_ON_GET_SETVOLUME = 186;

	public final static int MSG_MDMC_ON_GET_GETVOLUME = 187;

	public final static int MSG_MDMC_ON_GET_SETMUTE = 188;

	public final static int MSG_MDMC_ON_GET_GETMUTE = 189;

	public final static int MSG_MDMC_ON_GET_GETCURRENTTRANSPORTACTIONS = 190;

	public final static int MSG_MDMC_ON_GET_GETTRANSPORTINFO = 191;

	public final static int MSG_MDMC_ON_GET_SETPLAYMODE = 192;
	
	public final static int MSG_MDMC_AV_TRANS_IMAGESEEK = 193;
	
	public final static int MSG_MDMC_ON_GET_IMAGESEEK = 194;

	// /////////////////////////////////////////////////////////////
	// Message Definitions for M-DMS thread
	// /////////////////////////////////////////////////////////////
	public final static int MSG_MDMS_ON_PROC_PREPARED = 300;

	public final static int MSG_MDMS_STOP_PROC = 301;

	public final static int MSG_MDMS_START_DMS = 302;

	public final static int MSG_MDMS_STOP_DMS = 303;

	// /////////////////////////////////////////////////////////////
	// Key Definitions
	// /////////////////////////////////////////////////////////////
	public final static String KEY_OBJ_ID = "objectId";
	public final static String KEY_DEV_URI = "deviceURI";
	public final static String KEY_ITEM_URI = "itemURI";
	public final static String KEY_DEVICE_OBJ = "deviceObj";

	// /////////////////////////////////////////////////////////////
	// Local M-DMS status
	// /////////////////////////////////////////////////////////////
	public final static int LOCAL_MDMS_STATUS_OFFLINE = 0;
	public final static int LOCAL_MDMS_STATUS_ONLINE = 1;

}
