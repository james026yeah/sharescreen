package com.archermind.ashare;

public class TypeDefs {
	public final static int MEDIA_TYPE_INVALID = -1;
	public final static int MEDIA_TYPE_DLNA_AUDIO = 0;
	public final static int MEDIA_TYPE_DLNA_VIDEO = 1;
	public final static int MEDIA_TYPE_DLNA_IMAGE = 2;
	public final static int MEDIA_TYPE_AIRPLAY_AUDIO = 3;
	public final static int MEDIA_TYPE_AIRPLAY_VIDEO = 4;
	public final static int MEDIA_TYPE_AIRPLAY_IMAGE = 5;
	public final static int MEDIA_TYPE_ASHARE_MIRROR = 6;

	public final static String KEY_MEDIA_TYPE = "media-type";
	public final static String KEY_MEDIA_URI = "media-uri";
	public final static String KEY_AIRPLAY_IMAGE_DATA = "airplay-image-data";
	//image 缩放
	public final static String KEY_IMAGE_CONTROL_SCALING="SCALING";
	//image 翻转
	public final static String KEY_IMAGE_CONTROL_FLIP="FLIP";
	public final static int IMAGE_CONTROL_SCALING = 0;
	public final static int IMAGE_CONTROL_FLIP = 1;

	// /////////////////////////////////////////////////////////////
	// Message Definitions for DMR thread
	// /////////////////////////////////////////////////////////////
	public final static int MSG_DMR_ON_PROC_PREPARED = 200;
	public final static int MSG_DMR_AV_TRANS_SET_URI = 201;
	public final static int MSG_DMR_AV_TRANS_SET_NEXT_URI = 202;
	public final static int MSG_DMR_AV_TRANS_GET_MEDIA_INFO = 203;
	public final static int MSG_DMR_AV_TRANS_GET_TRANS_INFO = 204;
	public final static int MSG_DMR_AV_TRANS_GET_POS_INFO = 205;
	public final static int MSG_DMR_AV_TRANS_GET_DEV_CAPS = 206;
	public final static int MSG_DMR_AV_TRANS_GET_TRANS_SETTING = 207;
	public final static int MSG_DMR_AV_TRANS_STOP = 208;
	public final static int MSG_DMR_AV_TRANS_PLAY = 209;
	public final static int MSG_DMR_AV_TRANS_PLAY_TO_PAUSE = 210;
	public final static int MSG_DMR_AV_TRANS_RECORD = 211;
	public final static int MSG_DMR_AV_TRANS_SEEK = 212;
	public final static int MSG_DMR_AV_TRANS_NEXT = 213;
	public final static int MSG_DMR_AV_TRANS_PREVIOUS = 214;
	public final static int MSG_DMR_AV_TRANS_SET_PLAY_MODE = 215;
	public final static int MSG_DMR_AV_TRANS_SET_REC_QULITY_MODE = 216;
	public final static int MSG_DMR_AV_TRANS_GET_CUR_TRANS_ACTION = 217;
	public final static int MSG_DMR_AV_TRANS_PAUSE_TO_PLAY = 218;
	public final static int MSG_DMR_AV_TRANS_SET_VOLUME = 219;
	public final static int MSG_DMR_AV_TRANS_SET_MUTE = 220;
	public final static int MSG_DMR_RENDERER_UPDATEDATA = 221;
	
	public final static int MSG_DMR_RENDERER_START_SUCCESS = 500;
	public final static int MSG_DMR_RENDERER_GET_DMR_IDENTIFIER = 501;
	public final static int MSG_DMR_RENDERER_ON_GET_DMR_IDENTIFIER = 502;
}
