package com.archermind.ashare.dlna;

import java.util.ArrayList;
import java.util.HashMap;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.ServiceList;
import org.cybergarage.upnp.device.DeviceChangeListener;
import org.cybergarage.upnp.device.NotifyListener;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.event.EventListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;
import org.cybergarage.upnp.std.av.controller.MediaController;
import org.cybergarage.upnp.std.av.renderer.AVTransport;
import org.cybergarage.upnp.std.av.renderer.RenderingControl;
import org.cybergarage.upnp.std.av.server.object.ContentNode;
import org.cybergarage.upnp.std.av.server.object.container.ContainerNode;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import archermind.dlna.mobile.MessageDefs;

import com.archermind.ashare.dlna.localmedia.Album;
import com.archermind.ashare.dlna.localmedia.Artist;
import com.archermind.ashare.dlna.localmedia.MediaCache;
import com.archermind.ashare.dlna.localmedia.MusicCategoryInfo;
import com.archermind.ashare.dlna.localmedia.MusicItem;
import com.archermind.ashare.dlna.localmedia.PhotoAlbum;
import com.archermind.ashare.dlna.localmedia.VideoCategory;

public class ControllerProcess extends HandlerThread 
	implements SearchResponseListener, NotifyListener, DeviceChangeListener, EventListener {
	private static final int TIME_SECOND = 1000;
	private static final int TIME_MINUTE = TIME_SECOND * 60;
	private static final int TIME_HOUR = TIME_MINUTE * 60;
	private final static String TAG = "ControllerProcess";
	private MediaController mMediaCtrl;
	private Handler mHandler;
	private Handler mUIHandler;
	private Handler.Callback mCb = new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			boolean ret = false;
			//ControlVidio control = new ControlVidio(mMediaCtrl);
			switch (msg.what) {
			case MessageDefs.MSG_MDMC_CONTENT_DIR_BROWSE:
				BrowserParam bp = (BrowserParam)msg.obj;
				ContainerNode cn = null;
				if(bp.mObjectId == null) {
					// browser from object "0"					
					Log.d(TAG, "browser from root! dev:" + bp.mDev.getFriendlyName());
					Log.d(TAG, bp.mDev.getRootNode().toString());
					
					ContentNode content = mMediaCtrl.getContentDirectory(bp.mDev);
					Log.d(TAG, "title:" + content.getTitle() + ", getID:" + content.getID());
					/*
					ContainerNode root = (ContainerNode)bp.mDev.getRootNode();					
					Log.d(TAG, "root:" + root.getTitle() + " child count:" + root.getChildCount() + 
							", id:" + root.getID());*/
					cn = mMediaCtrl.browse(bp.mDev, "0", true);
					Log.d(TAG, "title:" + cn.getTitle() + ", getID:" + cn.getID() +
			    			", childCount:" + cn.getChildCount());
				} else {
					cn = mMediaCtrl.browse(bp.mDev, bp.mObjectId, true);
				}
				Message echoMsg = new Message();
				echoMsg.what = MessageDefs.MSG_MDMC_ON_GET_CONENT;
				echoMsg.obj = cn;
				Log.d(TAG, "getContentNode!");
				if(null != mUIHandler)
					mUIHandler.sendMessage(echoMsg);
				ret = true;
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_PLAY:
				Bundle data = msg.getData();
				Log.d(TAG, data.getString(MessageDefs.KEY_DEV_URI) + ", "
						+ data.getString(MessageDefs.KEY_ITEM_URI));
				Boolean playc = play(
						findDevByURI(data.getString(MessageDefs.KEY_DEV_URI)),
						data.getString(MessageDefs.KEY_ITEM_URI),data.getString("type"));
				Message plMsg = new Message();
				plMsg.what = MessageDefs.MSG_MDMC_ON_GET_PLAY;
				plMsg.obj = playc;
				if (null != mUIHandler) {
					mUIHandler.sendMessage(plMsg);
				}
				break;
			case MessageDefs.MSG_MDMC_GET_MUSIC_CATEGORY_DATA:
				ArrayList<MusicCategoryInfo> musicCategory = MediaCache.instance().getMusicCategory();
				Message musicCategoryMsg = new Message();
				musicCategoryMsg.what = MessageDefs.MSG_MDMC_ON_GET_MUSIC_CATEGORY_DATA;
				musicCategoryMsg.obj = musicCategory;
				if(null != mUIHandler && musicCategory != null)
					mUIHandler.sendMessage(musicCategoryMsg);
				break;
			case MessageDefs.MSG_MDMC_GET_MUSIC_ARTISTS_DATA:
				ArrayList<Artist> artists  = MediaCache.instance().getArtistList();
				Message artistsMsg = new Message();
				artistsMsg.what = MessageDefs.MSG_MDMC_ON_GET_MUSIC_ARTISTS_DATA;
				artistsMsg.obj = artists;
				if(null != mUIHandler && artistsMsg != null)
					mUIHandler.sendMessage(artistsMsg);
				break;
			case MessageDefs.MSG_MDMC_GET_MUSIC_ALBUMS_DATA:
				ArrayList<Album> musicAlbums  = MediaCache.instance().getAlbumList();
				Message albumsMsg = new Message();
				albumsMsg.what = MessageDefs.MSG_MDMC_ON_GET_MUSIC_ALBUMS_DATA;
				albumsMsg.obj = musicAlbums;
				if(null != mUIHandler && musicAlbums != null)
					mUIHandler.sendMessage(albumsMsg);
				break;
			case MessageDefs.MSG_MDMC_GET_MUSIC_ALL_DATA:
				ArrayList<MusicItem> musics  = MediaCache.instance().getAllMusicData();
				Message allMusicMsg = new Message();
				allMusicMsg.what = MessageDefs.MSG_MDMC_ON_GET_MUSIC_ALL_DATA;
				allMusicMsg.obj = musics;
				if(null != mUIHandler && musics != null)
					mUIHandler.sendMessage(allMusicMsg);
				break;
			case MessageDefs.MSG_MDMC_GET_VIDEOS_DATA:
				ArrayList<VideoCategory> videos  = MediaCache.instance().getVideoList();
				Message videosMsg = new Message();
				videosMsg.what = MessageDefs.MSG_MDMC_ON_GET_VIDEOS_DATA;
				videosMsg.obj = videos;
				if(null != mUIHandler && videos != null)
					mUIHandler.sendMessage(videosMsg);
				break;
			case MessageDefs.MSG_MDMC_GET_PHOTOS_DATA:
				ArrayList<PhotoAlbum> pa = MediaCache.instance().getPhotoAlbums();
				Message paMsg = new Message();
				paMsg.what = MessageDefs.MSG_MDMC_ON_GET_PTOTOS_DATA;
				paMsg.obj = pa;
				if(null != mUIHandler && pa != null)
					mUIHandler.sendMessage(paMsg);
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_PAUSETOPLAY:
				Bundle dataPausePlay = msg.getData();
				Boolean pcheck = play(findDevByURI(dataPausePlay
						.getString(MessageDefs.KEY_DEV_URI)));
				Message pcheckMsg = new Message();
				pcheckMsg.what = MessageDefs.MSG_MDMC_ON_GET_PAUSETOPLAY;
				pcheckMsg.obj = pcheck;
				if (null != mUIHandler) {
					mUIHandler.sendMessage(pcheckMsg);
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_PAUSE:
				Bundle dataPause = msg.getData();
				Boolean pausecheck = pause(findDevByURI(dataPause
						.getString(MessageDefs.KEY_DEV_URI)));
				Message pauseMsg = new Message();
				pauseMsg.what =MessageDefs.MSG_MDMC_ON_GET_PAUSE;
				pauseMsg.obj = pausecheck;
				if (null != mUIHandler) {
					mUIHandler.sendMessage(pauseMsg);
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_STOP:
				Bundle dataStop = msg.getData();
				Boolean stopcheck = stop(findDevByURI(dataStop
						.getString(MessageDefs.KEY_DEV_URI)));
				Message stopMsg = new Message();
				stopMsg.what =MessageDefs.MSG_MDMC_ON_GET_STOP;
				stopMsg.obj = stopcheck;
				if (null != mUIHandler) {
					mUIHandler.sendMessage(stopMsg);
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_GETPOSITIONINFO:
				Bundle bdgetInfo = msg.getData();
				HashMap<String, String> m = getPositionInfo(findDevByURI(bdgetInfo
						.getString(MessageDefs.KEY_DEV_URI)));
				Message getinfoMsg = new Message();
				getinfoMsg.what =MessageDefs.MSG_MDMC_ON_GET_GETPOSITIONINFO;
				getinfoMsg.obj = m;
				if (null != mUIHandler) {
					mUIHandler.sendMessage(getinfoMsg);
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_SEEK:
				Bundle bdseek = msg.getData();
				Boolean seekcheck = seek(findDevByURI(bdseek
								.getString(MessageDefs.KEY_DEV_URI)), bdseek
								.getString("TARGET"));
				Message seekMsg = new Message();
				seekMsg.what =MessageDefs.MSG_MDMC_ON_GET_SEEK;
				seekMsg.obj = seekcheck;
				if (null != mUIHandler) {
					mUIHandler.sendMessage(seekMsg);
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_NEXT:
				Bundle bdnext = msg.getData();
				Boolean nextcheck = next(findDevByURI(bdnext
								.getString(MessageDefs.KEY_DEV_URI)), bdnext
								.getString(MessageDefs.KEY_ITEM_URI),bdnext
								.getString("type"));
				Message nextMsg = new Message();
				nextMsg.what =MessageDefs.MSG_MDMC_ON_GET_NEXT;
				nextMsg.obj = nextcheck;
				if (null != mUIHandler) {
					mUIHandler.sendMessage(nextMsg);
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_PREVIOUS:
				Bundle bdprevious = msg.getData();
				Boolean previouscheck = previous(
						findDevByURI(bdprevious
								.getString(MessageDefs.KEY_DEV_URI)),
						bdprevious.getString(MessageDefs.KEY_ITEM_URI),bdprevious.getString("type"));
				Message previousMsg = new Message();
				previousMsg.what =MessageDefs.MSG_MDMC_ON_GET_PREVIOUS;
				previousMsg.obj = previouscheck;
				if (null != mUIHandler) {
					mUIHandler.sendMessage(previousMsg);
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_SETAVTRANSPORTURI:
				Bundle datasetav = msg.getData();
				if (datasetav != null) {
					Boolean setavcheck = setAVTransportURI(
							findDevByURI(datasetav
									.getString(MessageDefs.KEY_DEV_URI)),
							datasetav.getString(MessageDefs.KEY_ITEM_URI),datasetav.getString("type"));
					Message setavMsg = new Message();
					setavMsg.what =MessageDefs.MSG_MDMC_ON_GET_SETAVTRANSPORTURI;
					setavMsg.obj = setavcheck;
					if (null != mUIHandler) {
						mUIHandler.sendMessage(setavMsg);
					}
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_SETVOLUME:
				Bundle datasetVolume = msg.getData();
				if (datasetVolume != null) {
					Boolean setcolumecheck = setVolume(
							findDevByURI(datasetVolume
									.getString(MessageDefs.KEY_DEV_URI)),
							datasetVolume.getFloat("DesiredVolume"));
					Message setcolumeMsg = new Message();
					setcolumeMsg.what =MessageDefs.MSG_MDMC_ON_GET_SETVOLUME;
					setcolumeMsg.obj = setcolumecheck;
					if (null != mUIHandler) {
						mUIHandler.sendMessage(setcolumeMsg);
					}
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_GETVOLUME:
				Bundle datagetVolume = msg.getData();
				if (datagetVolume != null) {
					String getvolumecheck = getVolume(findDevByURI(datagetVolume
							.getString(MessageDefs.KEY_DEV_URI)));
					Message getvolumeMsg = new Message();
					getvolumeMsg.what =MessageDefs.MSG_MDMC_ON_GET_GETVOLUME;
					getvolumeMsg.obj = getvolumecheck;
					if (null != mUIHandler) {
						mUIHandler.sendMessage(getvolumeMsg);
					}
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_SETMUTE:
				Bundle datasetMute = msg.getData();
				if (datasetMute != null) {
					Boolean setmutecheck = setMute(findDevByURI(datasetMute
							.getString(MessageDefs.KEY_DEV_URI)), datasetMute
							.getFloat("DesiredVolume"));
					Message setmuteMsg = new Message();
					setmuteMsg.what =MessageDefs.MSG_MDMC_ON_GET_SETMUTE;
					setmuteMsg.obj = setmutecheck;
					if (null != mUIHandler) {
						mUIHandler.sendMessage(setmuteMsg);
					}
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_GETMUTE:
				Bundle datagetMute = msg.getData();
				if (datagetMute != null) {
					String getmutecheck=getMute(findDevByURI(datagetMute
							.getString(MessageDefs.KEY_DEV_URI)));
					Log.d("yexiaoyan", "control getmutecheck="+getmutecheck);
					Message getmuteMsg = new Message();
					getmuteMsg.what =MessageDefs.MSG_MDMC_ON_GET_GETMUTE;
					getmuteMsg.obj = getmutecheck;
					if (null != mUIHandler) {
						mUIHandler.sendMessage(getmuteMsg);
					}
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_GETCURRENTTRANSPORTACTIONS:
				Bundle datagetCurrent = msg.getData();
				if (datagetCurrent != null) {
					String getactionscheck = getCurrentTranSportActions(findDevByURI(datagetCurrent
							.getString(MessageDefs.KEY_DEV_URI)));
					Message getactionsMsg = new Message();
					getactionsMsg.what =MessageDefs.MSG_MDMC_ON_GET_GETCURRENTTRANSPORTACTIONS;
					getactionsMsg.obj = getactionscheck;
					if (null != mUIHandler) {
						mUIHandler.sendMessage(getactionsMsg);
					}
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_GETTRANSPORTINFO:
				Bundle datagetTransportinfo = msg.getData();
				if (datagetTransportinfo != null) {
					HashMap<String, String> getmapcheck= getTransportInfo(findDevByURI(datagetTransportinfo
							.getString(MessageDefs.KEY_DEV_URI)));
					Message getTransportinfoMsg = new Message();
					getTransportinfoMsg.what =MessageDefs.MSG_MDMC_ON_GET_GETTRANSPORTINFO;
					getTransportinfoMsg.obj = getmapcheck;
					if (null != mUIHandler) {
						mUIHandler.sendMessage(getTransportinfoMsg);
					}
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_SETPLAYMODE:
				Bundle datasetPlayMode = msg.getData();
				if (datasetPlayMode != null) {
					Boolean setmutecheck=setPlayMode(findDevByURI(datasetPlayMode
							.getString(MessageDefs.KEY_DEV_URI)), datasetPlayMode
							.getString("NewPlayMode"));
					Message setmuteMsg = new Message();
					setmuteMsg.what =MessageDefs.MSG_MDMC_ON_GET_SETPLAYMODE;
					setmuteMsg.obj = setmutecheck;
					if (null != mUIHandler) {
						mUIHandler.sendMessage(setmuteMsg);
					}
				}
				break;
			case MessageDefs.MSG_MDMC_AV_TRANS_IMAGESEEK:
				Bundle datasetImageSeek = msg.getData();
				if (datasetImageSeek != null) {
					Boolean setmutecheck=seekImage(findDevByURI(datasetImageSeek
							.getString(MessageDefs.KEY_DEV_URI)),datasetImageSeek
							.getString("TARGET"),datasetImageSeek.getString("UNIT"));
					Message setmuteMsg = new Message();
					setmuteMsg.what =MessageDefs.MSG_MDMC_ON_GET_IMAGESEEK;
					setmuteMsg.obj = setmutecheck;
					if (null != mUIHandler) {
						mUIHandler.sendMessage(setmuteMsg);
					}
				}
				break;
				
			}
			return ret;
		}		
	};
	
	private class BrowserParam {
		public Device mDev;
		public String mObjectId;
	};
	public ControllerProcess(Handler uiHandler) {
		super(TAG);
		mUIHandler = uiHandler;
		/*
		mMediaCtrl = new MediaController();
		mMediaCtrl.addSearchResponseListener(this);
		mMediaCtrl.addNotifyListener(this);
		mMediaCtrl.addDeviceChangeListener(this);
		mMediaCtrl.addEventListener(this);
		*/
		Log.d(TAG, "in Constructor of class ControllerProcess");
	}
	
	@Override
	protected void onLooperPrepared() {
		Log.d(TAG, "on looper prepared!");
		mHandler = new Handler(getLooper(), mCb);
		if(null != mUIHandler)
			mUIHandler.sendEmptyMessage(MessageDefs.MSG_MDMC_ON_PROC_PREPARED);
	}
	
	public void startMediaController() {
		if(mHandler != null) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
				    if(mMediaCtrl == null) {
				        mMediaCtrl = new MediaController();
				        mMediaCtrl.addSearchResponseListener(ControllerProcess.this);
				        mMediaCtrl.addNotifyListener(ControllerProcess.this);
				        mMediaCtrl.addDeviceChangeListener(ControllerProcess.this);
				        mMediaCtrl.addEventListener(ControllerProcess.this);
				    }
					Log.v(TAG, ">>>>>>>>>> Start media controller <<<<<<<<<<");
					mMediaCtrl.start();
				}
			});
		}
	}
	
	public void stopMediaController() {
		if(mHandler != null) {
			mHandler.removeCallbacksAndMessages(null);	// remove all callbacks and messages
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if(mMediaCtrl != null) {
						Log.v(TAG, ">>>>>>>>>> stop media controller <<<<<<<<<<");
						mMediaCtrl.stop();
						mMediaCtrl = null;
					}
					if(mUIHandler != null) {
						mUIHandler.sendMessage(
								Message.obtain(null, MessageDefs.MSG_SERVICE_ON_DMC_STOPPED));
					}
				}
			});
		} else {
		    if(mUIHandler != null) {
                mUIHandler.sendMessage(
                        Message.obtain(null, MessageDefs.MSG_SERVICE_ON_DMC_STOPPED));
            }
		}
	}
	
	public void searchDevices() {
	    Log.v(TAG, ">>>>>>>>>> in search devices <<<<<<<<<<");
		if(mHandler != null) {
		    Log.v(TAG, ">>>>>>>>>> in 1 search devices <<<<<<<<<<");
			mHandler.post(new Runnable() {
				@Override
				public void run() {
				    Log.v(TAG, ">>>>>>>>>>2 in search devices <<<<<<<<<<");
					if(mMediaCtrl != null) {
						Log.v(TAG, ">>>>>>>>>> search devices <<<<<<<<<<");
						mMediaCtrl.search();
					}
				}
			});
		}
	}

    public void getMusicCategory() {
    	if (null != mHandler) {
    		Message msg = new Message();
    		msg.what = MessageDefs.MSG_MDMC_GET_MUSIC_CATEGORY_DATA;
    		mHandler.sendMessage(msg);
    	}
    }
    
    public void getMusicArtists() {
    	if (null != mHandler) {
    		Message msg = new Message();
    		msg.what = MessageDefs.MSG_MDMC_GET_MUSIC_ARTISTS_DATA;
    		mHandler.sendMessage(msg);
    	}
    }
    
    public void getMusicAlbums() {
    	if (null != mHandler) {
    		Message msg = new Message();
    		msg.what = MessageDefs.MSG_MDMC_GET_MUSIC_ALBUMS_DATA;
    		mHandler.sendMessage(msg);
    	}
    }
    
    public void getMusicAll() {
    	if (null != mHandler) {
    		Message msg = new Message();
    		msg.what = MessageDefs.MSG_MDMC_GET_MUSIC_ALL_DATA;
    		mHandler.sendMessage(msg);
    	}
    }
    
    public void getVideos() {
    	if (null != mHandler) {
    		Message msg = new Message();
    		msg.what = MessageDefs.MSG_MDMC_GET_VIDEOS_DATA;
    		mHandler.sendMessage(msg);
    	}
    }
    
    public void getPhotos() {
    	if (null != mHandler) {
    		Message msg = new Message();
    		msg.what = MessageDefs.MSG_MDMC_GET_PHOTOS_DATA;
    		mHandler.sendMessage(msg);
    	}
    }
    
    public void browserContent(Device dev, String objectId) {
    	if(null != mHandler) {
    		Message msg = new Message();
    		BrowserParam bp = new BrowserParam();
    		bp.mDev = dev;
    		bp.mObjectId = objectId;
    		msg.what = MessageDefs.MSG_MDMC_CONTENT_DIR_BROWSE;
    		msg.obj = bp;
    		mHandler.sendMessage(msg);
    	}    	
    }
    
    public void browserContent(String devURI, String objectId) {
    	if(null != mHandler) {
    		Message msg = new Message();
    		BrowserParam bp = new BrowserParam();
    		bp.mDev = findDevByURI(devURI);
    		bp.mObjectId = objectId;
    		msg.what = MessageDefs.MSG_MDMC_CONTENT_DIR_BROWSE;
    		msg.obj = bp;    		
    		mHandler.sendMessage(msg);
    	}    	
    }
    
	public void control(Bundle data, int what) {
		if (null != mHandler) {
			Message msg = new Message();
			msg.setData(data);
			msg.what = what;
			mHandler.sendMessage(msg);
		}
	}
    
    private Device findDevByURI(String uri) {
    	Device target = null;
    	DeviceList dl = mMediaCtrl.getDeviceList();
    	for(int index = 0; index < dl.size(); ++index) {
			Device dev = dl.getDevice(index);
			// Location of device description is unique
			if(dev.getLocation().equals(uri)) {
				target = dev;
				break;
			}			
		}
    	Log.d(TAG, "Location:" + target.getLocation());
    	return target;
    }

	@Override
	public void deviceSearchResponseReceived(SSDPPacket arg0) {
		Log.d(TAG, "deviceSearchResponseReceived:");
		Log.d(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		DeviceList dl = null;
		if(mMediaCtrl != null) {
    		dl = mMediaCtrl.getDeviceList();
    		for(int index = 0; index < dl.size(); ++index) {
    			Device dev = dl.getDevice(index);
    			Log.d(TAG, dev.getFriendlyName());
    		}
		}
		Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");
		Message msg = new Message();
		msg.what = MessageDefs.MSG_MDMC_ON_SEARCH_RESPONSE;
		msg.obj = (dl == null) ? (new DeviceList()) : dl;
		mUIHandler.sendMessage(msg);
	}

	@Override
	public void deviceAdded(Device dev) {
		Log.d(TAG, "Add device:" + dev.getFriendlyName());
		if(mMediaCtrl != null) {
    		ServiceList sl = dev.getServiceList();
    		for(int index = 0; index < sl.size(); ++index) {
    			Service service = sl.getService(index);
    			mMediaCtrl.subscribe(service);
    		}
		}
		Message msg = new Message();
		msg.what = MessageDefs.MSG_MDMC_ON_DEV_ADDED;
		msg.obj = dev;
		mUIHandler.sendMessage(msg);		
	}

	@Override
	public void deviceRemoved(Device dev) {		
		Log.d(TAG, "Remove device:" + dev.getFriendlyName());
		if(mMediaCtrl != null) {
		    mMediaCtrl.unsubscribe(dev);
		}
		Message msg = new Message();
		msg.what = MessageDefs.MSG_MDMC_ON_DEV_REMOVED;
		msg.obj = dev;
		mUIHandler.sendMessage(msg);
	}
	
	@Override
	public void deviceNotifyReceived(SSDPPacket packet) {
		Log.d(TAG, "deviceNotifyReceived:");
		Log.d(TAG, "-------------------------------------------------------------");
		Log.d(TAG, packet.toString());
		Log.d(TAG, "-------------------------------------------------------------\n");		
	}

	@Override
	public void eventNotifyReceived(String uuid, long seq, String varName,
			String value) {
		Log.d(TAG, "--------------->>>>>>eventNotifyReceived uuid:" + uuid + ", seq:" + seq + ", varName:" + varName
				+ "value:" + value);
	}
	
	
	// 起动播放
	private Boolean play(Device dev, String url, String type) {
		stop(dev);
		if (setAVTransportURI(dev, url, type) == false)
			return false;
		return play(dev);
	}

	// 暂停后在播
	private Boolean play(Device dev) {
		Log.d("yexiaoyan", "play init");
		Service avTrans = dev.getService(AVTransport.SERVICE_TYPE);
		if (null != avTrans) {
			Action action = avTrans.getAction(AVTransport.PLAY);
			if (null != action) {
				action.setArgumentValue(AVTransport.INSTANCEID, "0");
				Boolean result = action.postControlAction();
				if (result) {
					Log.d("yexiaoyan", "play su");
					return true;
				}

			}
		}
		return false;
	}

	// 停止
	private Boolean stop(Device dev) {
		Boolean result = mMediaCtrl.stop(dev);
		return result;
	}

	// 暂停
	private Boolean pause(Device dev) {
		Service avTrans = dev.getService(AVTransport.SERVICE_TYPE);
		if (null != avTrans) {
			Action action = avTrans.getAction(AVTransport.PAUSE);
			if (null != action) {
				action.setArgumentValue(AVTransport.INSTANCEID, "0");
				Boolean result = action.postControlAction();
				if (result) {
					return true;
				}

			}
		}
		return false;
	}

	// 得到播放時间,上层用Timer控件,定时获取
	private HashMap<String, String> getPositionInfo(Device dev) {
		Service avTrans1 = dev.getService(AVTransport.SERVICE_TYPE);
		if (null != avTrans1) {
			Action action = avTrans1.getAction(AVTransport.GETPOSITIONINFO);
			if (null != action) {
				action.setArgumentValue(AVTransport.INSTANCEID, "0");
				Boolean result = action.postControlAction();
				if (result) {
					String track = action.getArgumentValue("Track");
					String trackDuration = action
							.getArgumentValue("TrackDuration");
					String trackMetaData = action
							.getArgumentValue("TrackMetaData");
					String trackURI = action.getArgumentValue("TrackURI");
					String relTime = action.getArgumentValue("RelTime");
					String absTime = action.getArgumentValue("AbsTime");
					String relCount = action.getArgumentValue("RelCount");
					String absCount = action.getArgumentValue("AbsCount");
					HashMap<String, String> map = new HashMap<String, String>();
					map.put("track", track);// value (0,1)
					map.put("trackDuration", trackDuration);// value 音频時长
					map.put("trackMetaData", trackMetaData);
					map.put("trackURI", trackURI);
					map.put("relTime", relTime);// 播放过的位置
					map.put("absTime", absTime);//
					map.put("relCount", relCount);
					map.put("absCount", absCount);
					return map;
				}
			}
		}
		return null;
	}

	private static String fromIntToDateString(long inVal) {
		// 输入的是相对时间的 秒差
		long hour = inVal / TIME_HOUR;
    	long minute = (inVal - hour * TIME_HOUR) / TIME_MINUTE;
    	long second = (inVal - hour * TIME_HOUR - minute * TIME_MINUTE) / TIME_SECOND;
		
		if(hour == 0) {
			return String.format("%02d:%02d", minute, second);
		}
		else {
			return String.format("%02d:%02d:%02d", hour, minute, second);
		}
	}

	
	// 快进.快退,进度条 发送播放的位置时间 rel_time
	private Boolean seek(Device dev, String target) {
		target = fromIntToDateString(Long.parseLong(target));
		Log.d("yexiaoyan", "seek target=" + target);
		Service avTrans = dev.getService(AVTransport.SERVICE_TYPE);
		if (null != avTrans) {
			Action action = avTrans.getAction(AVTransport.SEEK);
			if (null != action) {
				action.setArgumentValue(AVTransport.INSTANCEID, "0");
				action.setArgumentValue(AVTransport.UNIT, "REL_TIME");
				action.setArgumentValue(AVTransport.TARGET, target);
				Boolean result = action.postControlAction();
				Log.e("yexiaoyan",result+" reuslt");
				return result;
			}
		}
		return false;
	}

	// image
	private Boolean seekImage(Device dev, String target, String type) {
		Service avTrans = dev.getService(AVTransport.SERVICE_TYPE);
		if (null != avTrans) {
			Action action = avTrans.getAction(AVTransport.SEEK);
			if (null != action) {
				action.setArgumentValue(AVTransport.INSTANCEID, "0");
				action.setArgumentValue(AVTransport.UNIT, type);
				action.setArgumentValue(AVTransport.TARGET, target);
				Boolean result = action.postControlAction();
				return result;
			}
		}
		return false;
	}

	// 下一首
	private Boolean next(Device dev, String uri, String type) {
		Log.d("yexiaoyan", "next init uri=" + uri);
		stop(dev);
		if (setNextAVTransportURI(dev, uri, type)) {
			Service avTrans = dev.getService(AVTransport.SERVICE_TYPE);
			if (null != avTrans) {
				Action action = avTrans.getAction(AVTransport.NEXT);
				if (null != action) {
					action.setArgumentValue(AVTransport.INSTANCEID, "0");
					Boolean result = action.postControlAction();
					Log.d("yexiaoyan", "next success");
					return result;
				}
			}
		}
		return false;
	}

	// 上一首
	private Boolean previous(Device dev, String uri, String type) {
		stop(dev);
		if (setNextAVTransportURI(dev, uri, type)) {
			Service avTrans = dev.getService(AVTransport.SERVICE_TYPE);
			if (null != avTrans) {
				Action action = avTrans.getAction(AVTransport.PREVIOUS);
				if (null != action) {
					action.setArgumentValue(AVTransport.INSTANCEID, "0");
					Boolean result = action.postControlAction();
					return result;
				}
			}
		}
		return false;
	}

	// 发送播放文件路经
	private Boolean setAVTransportURI(Device dev, String uri, String type) {
		Service avTrans = dev.getService(AVTransport.SERVICE_TYPE);
		if (null != avTrans) {
			Action action = avTrans.getAction(AVTransport.SETAVTRANSPORTURI);
			if (null != action) {
				action.setArgumentValue(AVTransport.INSTANCEID, "0");
				action.setArgumentValue(AVTransport.CURRENTURI, uri);
				action.setArgumentValue(AVTransport.CURRENTURIMETADATA, type);
				Boolean result = action.postControlAction();
				return result;
			}
		}
		return false;
	}

	// 发送下一个要播放的文件路经
	private boolean setNextAVTransportURI(Device dev, String uri, String type) {
		Service avTrans = dev.getService(AVTransport.SERVICE_TYPE);
		if (null != avTrans) {
			Action action = avTrans
					.getAction(AVTransport.SETNEXTAVTRANSPORTURI);
			if (null != action) {
				action.setArgumentValue(AVTransport.INSTANCEID, "0");
				action.setArgumentValue(AVTransport.NEXTURI, uri);
				action.setArgumentValue(AVTransport.NEXTURIMETADATA, type);
				Boolean result = action.postControlAction();
				return result;
			}
		}
		return false;
	}

	// 音量控制
	private Boolean setVolume(Device dev, Float VOLUME) {
		Log.d("yexiaoyan", "setVolume VOLUME="+VOLUME);
		Service avTrans = dev.getService(RenderingControl.SERVICE_TYPE);
		if (null != avTrans) {
			Action action = avTrans.getAction(RenderingControl.SETVOLUME);
			if (null != action) {
				action.setArgumentValue(RenderingControl.INSTANCEID, "0");
				action.setArgumentValue(RenderingControl.CHANNEL,
						RenderingControl.MASTER);
				action.setArgumentValue(RenderingControl.DESIREDVOLUME, VOLUME+"");
				Boolean result = action.postControlAction();
				return result;

			}
		}
		return false;

	}

	// 得到音量
	private String getVolume(Device dev) {
		Service avTrans = dev.getService(RenderingControl.SERVICE_TYPE);
		if (null != avTrans) {
			Action action = avTrans.getAction(RenderingControl.GETVOLUME);
			if (null != action) {
				action.setArgumentValue(RenderingControl.INSTANCEID, "0");
				action.setArgumentValue(RenderingControl.CHANNEL,
						RenderingControl.MASTER);
				Boolean result = action.postControlAction();
				if (result) {				
					String desiredvolume = action
							.getArgumentValue(RenderingControl.CURRENTVOLUME);
					return desiredvolume;
				}
			}
		}
		return null;

	}

	// 静音
	private Boolean setMute(Device dev, Float VOLUME) {
		 Log.d("yexiaoyan", "setMute");
		Service avTrans = dev.getService(RenderingControl.SERVICE_TYPE);
		if (null != avTrans) {
			Action action = avTrans.getAction(RenderingControl.SETMUTE);
			if (null != action) {
				action.setArgumentValue(RenderingControl.INSTANCEID, "0");
				action.setArgumentValue(RenderingControl.CHANNEL,
						RenderingControl.MASTER);
				action.setArgumentValue(RenderingControl.DESIREDMUTE, VOLUME+"");
				Boolean result = action.postControlAction();
				return result;

			}
		}
		return false;

	}

	// 得到静音值
	private String getMute(Device dev) {
		 Log.d("yexiaoyan", "getMute");
		Service avTrans = dev.getService(RenderingControl.SERVICE_TYPE);
		if (null != avTrans) {
			Action action = avTrans.getAction(RenderingControl.GETMUTE);
			if (null != action) {
				action.setArgumentValue(RenderingControl.INSTANCEID, "0");
				action.setArgumentValue(RenderingControl.CHANNEL,
						RenderingControl.MASTER);
				Boolean result = action.postControlAction();
				if (result) {
					return action
							.getArgumentValue(RenderingControl.CURRENTMUTE);
				}
			}
		}
		return null;
	}

	// 得到当前状态
	private String getCurrentTranSportActions(Device dev) {
		Service avTrans = dev.getService(AVTransport.SERVICE_TYPE);
		if (null != avTrans) {
			Action action = avTrans
					.getAction(AVTransport.GETCURRENTTRANSPORTACTIONS);
			if (null != action) {
				action.setArgumentValue(AVTransport.INSTANCEID, "0");
				Boolean result = action.postControlAction();
				if (result) {
					return action.getArgumentValue("Actions");
				}
			}
		}
		return null;
	}

	// 传输状态
	private HashMap<String, String> getTransportInfo(Device dev) {
		Service avTrans = dev.getService(AVTransport.SERVICE_TYPE);
		if (null != avTrans) {
			Action action = avTrans.getAction(AVTransport.GETTRANSPORTINFO);
			if (null != action) {
				action.setArgumentValue(AVTransport.INSTANCEID, "0");
				Boolean result = action.postControlAction();
				if (result) {
					String state = action
							.getArgumentValue("CurrentTransportState");// value
																		// (stopped,playing)
					String statu = action
							.getArgumentValue("CurrentTransportStatus");// value
																		// (OK,error)
					String speed = action.getArgumentValue("CurrentSpeed");// value
																			// 1
					HashMap<String, String> map = new HashMap<String, String>();
					map.put("state", state);// value (stopped,playing)
					map.put("statu", statu);// value (OK,error)
					map.put("speed", speed);// value 1
					return map;
				}
			}
		}
		return null;
	}

	// 播放模式
	private Boolean setPlayMode(Device dev, String newPlayMode) {
		Service avTrans = dev.getService(AVTransport.SERVICE_TYPE);
		if (null != avTrans) {
			Action action = avTrans.getAction(AVTransport.SETPLAYMODE);
			if (null != action) {
				action.setArgumentValue(AVTransport.INSTANCEID, "0");
				action.setArgumentValue("NewPlayMode", newPlayMode);
				Boolean result = action.postControlAction();
				return result;
			}
		}
		return false;
	}
}
