package archermind.dlna.control;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.StateVariable;
import org.cybergarage.upnp.std.av.controller.MediaController;
import org.cybergarage.upnp.std.av.renderer.AVTransport;
import org.cybergarage.upnp.std.av.renderer.RenderingControl;

import android.util.Log;

public class ControlVidio {
	
	public static final int TIME_SECOND = 1000;
	public static final int TIME_MINUTE = TIME_SECOND * 60;
	public static final int TIME_HOUR = TIME_MINUTE * 60;
	
	public ControlVidio(MediaController m) {
		mMediaCtrl = m;
	}

	private MediaController mMediaCtrl;

	// 起动播放
	public Boolean play(Device dev, String url, String type) {
		stop(dev);
		if (setAVTransportURI(dev, url, type) == false)
			return false;
		return play(dev);
	}

	// 暂停后在播
	public Boolean play(Device dev) {
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
	public Boolean stop(Device dev) {
		Boolean result = mMediaCtrl.stop(dev);
		return result;
	}

	// 暂停
	public Boolean pause(Device dev) {
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
	public Map getPositionInfo(Device dev) {
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
					Map map = new HashMap();
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

	public static String fromIntToDateString(long inVal) {
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
	public Boolean seek(Device dev, String target) {
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
	public Boolean seekImage(Device dev, String target, String type) {
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
	public Boolean next(Device dev, String uri, String type) {
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
	public Boolean previous(Device dev, String uri, String type) {
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
	public Boolean setAVTransportURI(Device dev, String uri, String type) {
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
	public boolean setNextAVTransportURI(Device dev, String uri, String type) {
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
	public Boolean setVolume(Device dev, Float VOLUME) {
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
	public String getVolume(Device dev) {
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
	public Boolean setMute(Device dev, Float VOLUME) {
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
	public String getMute(Device dev) {
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
	public String getCurrentTranSportActions(Device dev) {
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
	public Map getTransportInfo(Device dev) {
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
					Map map = new HashMap();
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
	public Boolean setPlayMode(Device dev, String newPlayMode) {
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
