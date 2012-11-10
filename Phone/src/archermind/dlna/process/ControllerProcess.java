package archermind.dlna.process;

import java.util.ArrayList;
import java.util.Map;

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
import org.cybergarage.upnp.std.av.server.object.ContentNode;
import org.cybergarage.upnp.std.av.server.object.container.ContainerNode;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import archermind.dlna.control.ControlVidio;
import archermind.dlna.media.Album;
import archermind.dlna.media.Artist;
import archermind.dlna.media.MusicItem;
import archermind.dlna.media.PhotoAlbum;
import archermind.dlna.media.VideoCategory;
import archermind.dlna.mobile.MessageDefs;

import com.archermind.dlna.localmedia.MediaCache;
import com.archermind.dlna.localmedia.MusicCategoryInfo;

public class ControllerProcess extends HandlerThread 
	implements SearchResponseListener, NotifyListener, DeviceChangeListener, EventListener {
	private final static String TAG = "ControllerProcess";
	//private Device mRenderer;
	private MediaController mMediaCtrl;
	private Handler mHandler;
	private Handler mUIHandler;
	private Handler.Callback mCb = new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			boolean ret = false;
			ControlVidio control = new ControlVidio(mMediaCtrl);
			switch (msg.what) {
			case MessageDefs.MSG_MDMC_START_DMC:
				mMediaCtrl.start();
				ret = true;
				break;
			case MessageDefs.MSG_MDMC_STOP_DMC:
				mMediaCtrl.stop();
				Log.v(TAG, "-------------------->stop controller actually!");
				if(null != mUIHandler)
					mUIHandler.sendMessage(Message.obtain(null, 
							MessageDefs.MSG_SERVICE_ON_DMC_STOPPED));
				ret = true;
				break;
			case MessageDefs.MSG_MDMC_SEARCH_DEVICE:
				mMediaCtrl.search();
				ret = true;
				break;
			case MessageDefs.MSG_MDMC_STOP_PROC:
				ControllerProcess.this.quit();
				ret = true;
				break;
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
				Boolean playc=control.play(
						findDevByURI(data.getString(MessageDefs.KEY_DEV_URI)),
						data.getString(MessageDefs.KEY_ITEM_URI),data.getString("type"));
				Message plMsg = new Message();
				plMsg.what = MessageDefs.MSG_MDMC_ON_GET_PLAY;
				plMsg.obj = playc;
				if (null != mUIHandler) {
					mUIHandler.sendMessage(plMsg);
				}
				//mRenderer = findDevByURI(data.getString(DLNAService.KEY_DEV_URI));				
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
				Boolean pcheck = control.play(findDevByURI(dataPausePlay
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
				Boolean pausecheck = control.pause(findDevByURI(dataPause
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
				Boolean stopcheck = control.stop(findDevByURI(dataStop
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
				Map m = control.getPositionInfo(findDevByURI(bdgetInfo
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
				Boolean seekcheck = control
						.seek(findDevByURI(bdseek
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
				Boolean nextcheck = control
						.next(findDevByURI(bdnext
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
				Boolean previouscheck = control.previous(
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
					Boolean setavcheck = control.setAVTransportURI(
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
					Boolean setcolumecheck = control.setVolume(
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
					String getvolumecheck=control.getVolume(findDevByURI(datagetVolume
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
					Boolean setmutecheck=control.setMute(findDevByURI(datasetMute
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
					String getmutecheck=control.getMute(findDevByURI(datagetMute
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
					String getactionscheck=control.getCurrentTranSportActions(findDevByURI(datagetCurrent
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
					Map getmapcheck=control.getTransportInfo(findDevByURI(datagetTransportinfo
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
					Boolean setmutecheck=control.setPlayMode(findDevByURI(datasetPlayMode
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
					Boolean setmutecheck=control.seekImage(findDevByURI(datasetImageSeek
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
		mMediaCtrl = new MediaController();
		mMediaCtrl.addSearchResponseListener(this);
		mMediaCtrl.addNotifyListener(this);
		mMediaCtrl.addDeviceChangeListener(this);
		mMediaCtrl.addEventListener(this);
		Log.d(TAG, "in Constructor of class ControllerProcess");
	}
	
    @Override
    protected void onLooperPrepared() {
    	Log.d(TAG, "on looper prepared!");
    	mHandler = new Handler(getLooper(), mCb);
    	if(null != mUIHandler)
    		mUIHandler.sendEmptyMessage(MessageDefs.MSG_MDMC_ON_PROC_PREPARED);
    }
    
    public void startController() {
    	Log.d(TAG, "startController 0");
    	if(null != mHandler) {
    		Log.d(TAG, "startController 1");
    		mHandler.sendEmptyMessage(MessageDefs.MSG_MDMC_START_DMC);
    	}
    }
    
    public void stopController() {
    	if(null != mHandler) {
    		mHandler.sendEmptyMessage(MessageDefs.MSG_MDMC_STOP_DMC);
    	}    	
    }
    
    public void searchDevice() {
    	if(null != mHandler) {
    		mHandler.sendEmptyMessage(MessageDefs.MSG_MDMC_SEARCH_DEVICE);
    	}    	
    }
    
    public void stopProcess() {
    	Log.v(TAG, "stop controller asynchronized!!!");
    	stopController();
    	if(null != mHandler) {
    		mHandler.sendEmptyMessage(MessageDefs.MSG_MDMC_STOP_PROC);
    	}   
    }
    
    public void getMusicCategory(Device device) {
    	if (null != mHandler) {
    		Message msg = new Message();
    		msg.what = MessageDefs.MSG_MDMC_GET_MUSIC_CATEGORY_DATA;
    		msg.obj = device;
    		mHandler.sendMessage(msg);
    	}
    }
    
    public void getMusicArtists(Device device) {
    	if (null != mHandler) {
    		Message msg = new Message();
    		msg.what = MessageDefs.MSG_MDMC_GET_MUSIC_ARTISTS_DATA;
    		msg.obj = device;
    		mHandler.sendMessage(msg);
    	}
    }
    
    public void getMusicAlbums(Device device) {
    	if (null != mHandler) {
    		Message msg = new Message();
    		msg.what = MessageDefs.MSG_MDMC_GET_MUSIC_ALBUMS_DATA;
    		msg.obj = device;
    		mHandler.sendMessage(msg);
    	}
    }
    
    public void getMusicAll(Device device) {
    	if (null != mHandler) {
    		Message msg = new Message();
    		msg.what = MessageDefs.MSG_MDMC_GET_MUSIC_ALL_DATA;
    		msg.obj = device;
    		mHandler.sendMessage(msg);
    	}
    }
    
    public void getVideos(Device device) {
    	if (null != mHandler) {
    		Message msg = new Message();
    		msg.what = MessageDefs.MSG_MDMC_GET_VIDEOS_DATA;
    		msg.obj = device;
    		mHandler.sendMessage(msg);
    	}
    }
    
    public void getPhotos(Device device) {
    	if (null != mHandler) {
    		Message msg = new Message();
    		msg.what = MessageDefs.MSG_MDMC_GET_PHOTOS_DATA;
    		msg.obj = device;
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
		DeviceList dl = mMediaCtrl.getDeviceList();
		for(int index = 0; index < dl.size(); ++index) {
			Device dev = dl.getDevice(index);
			Log.d(TAG, dev.getFriendlyName());
		}
		Log.d(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");
		Message msg = new Message();
		msg.what = MessageDefs.MSG_MDMC_ON_SEARCH_RESPONSE;
		msg.obj = dl;
		mUIHandler.sendMessage(msg);
	}

	@Override
	public void deviceAdded(Device dev) {
		Log.d(TAG, "Add device:" + dev.getFriendlyName());
		ServiceList sl = dev.getServiceList();
		for(int index = 0; index < sl.size(); ++index) {
			Service service = sl.getService(index);
			mMediaCtrl.subscribe(service);
		}
		Message msg = new Message();
		msg.what = MessageDefs.MSG_MDMC_ON_DEV_ADDED;
		msg.obj = dev;
		mUIHandler.sendMessage(msg);		
	}

	@Override
	public void deviceRemoved(Device dev) {		
		Log.d(TAG, "Remove device:" + dev.getFriendlyName());		
		mMediaCtrl.unsubscribe(dev);
		Message msg = new Message();
		msg.what = MessageDefs.MSG_MDMC_ON_DEV_REMOVED;
		msg.obj = dev;
		mUIHandler.sendMessage(msg);
	}
	
	/*public void loadMediaDirectoryTest() {
		ArrayList<MusicCategoryInfo> category  = MeidaDataLoadUtil.loadMusicCategory(mMediaCtrl, mMsDevice);
		ArrayList<MusicItem> allmusiclist  = MeidaDataLoadUtil.loadAllMusics(mMediaCtrl, mMsDevice);
		ArrayList<Album> albumlist  = MeidaDataLoadUtil.loadAlbums(mMediaCtrl, mMsDevice);
		ArrayList<Artist> artistlist  = MeidaDataLoadUtil.loadArtists(mMediaCtrl, mMsDevice);
		ArrayList<VideoCategory> videolist =  MeidaDataLoadUtil.loadVideoCategories(mMediaCtrl, mMsDevice);
	}*/
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
}
