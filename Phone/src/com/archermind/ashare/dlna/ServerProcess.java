package com.archermind.ashare.dlna;

import java.util.UUID;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.control.ActionListener;
import org.cybergarage.upnp.std.av.renderer.ConnectionManager;
import org.cybergarage.upnp.std.av.server.ContentDirectory;
import org.cybergarage.upnp.std.av.server.MediaServer;
import org.cybergarage.upnp.std.av.server.object.format.GIFFormat;
import org.cybergarage.upnp.std.av.server.object.format.ID3Format;
import org.cybergarage.upnp.std.av.server.object.format.JPEGFormat;
import org.cybergarage.upnp.std.av.server.object.format.MPEGFormat;
import org.cybergarage.upnp.std.av.server.object.format.PNGFormat;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import archermind.dlna.mobile.MessageDefs;

import com.archermind.ashare.dlna.localmedia.ImageAlbumsDirectory;
import com.archermind.ashare.dlna.localmedia.LocalMediaDbHelper;
import com.archermind.ashare.dlna.localmedia.MediaCache;
import com.archermind.ashare.dlna.localmedia.MusicAlbumArtistDirectory;
import com.archermind.ashare.dlna.localmedia.VideoClollectionDirectory;

public class ServerProcess extends HandlerThread implements ActionListener {
	private final static String TAG = "ServerProcess";
	private final static String DESCRIPTION = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
			+ "<root xmlns=\"urn:schemas-upnp-org:device-1-0\">\n"
			+ "   <specVersion>\n"
			+ "      <major>1</major>\n"
			+ "      <minor>0</minor>\n"
			+ "   </specVersion>\n"
			+ "   <device>\n"
			+ "      <deviceType>urn:schemas-upnp-org:device:MediaServer:1</deviceType>\n"
			+ "      <friendlyName>amDLNA Media Server</friendlyName>\n"
			+ "      <manufacturer>Archermind</manufacturer>\n"
			+ "      <manufacturerURL>http://www.archermind.com</manufacturerURL>\n"
			+ "      <modelDescription>Provides content through UPnP ContentDirectory service</modelDescription>\n"
			+ "      <modelName>aShare Media Server</modelName>\n"
			+ "      <modelNumber>1.0</modelNumber>\n"
			+ "      <modelURL>http://www.archermind.com</modelURL>\n"
			+ "      <UDN>uuid:362d9414-31a0-48b6-b684-2b4bd38391d9</UDN>\n"
			+ "      <serviceList>\n"
			+ "         <service>\n"
			+ "            <serviceType>urn:schemas-upnp-org:service:ContentDirectory:1</serviceType>\n"
			+ "            <serviceId>urn:upnp-org:serviceId:urn:schemas-upnp-org:service:ContentDirectory</serviceId>\n"
			+ "            <SCPDURL>/service/ContentDirectory1.xml</SCPDURL>\n"
			+ "            <controlURL>/service/ContentDirectory_control</controlURL>\n"
			+ "            <eventSubURL>/service/ContentDirectory_event</eventSubURL>\n"
			+ "         </service>\n"
			+ "         <service>\n"
			+ "            <serviceType>urn:schemas-upnp-org:service:ConnectionManager:1</serviceType>\n"
			+ "            <serviceId>urn:upnp-org:serviceId:urn:schemas-upnp-org:service:ConnectionManager</serviceId>\n"
			+ "            <SCPDURL>/service/ConnectionManager1.xml</SCPDURL>\n"
			+ "            <controlURL>/service/ConnectionManager_control</controlURL>\n"
			+ "            <eventSubURL>/service/ConnectionManager_event</eventSubURL>\n"
			+ "         </service>\n"
			+ "      </serviceList>\n"
			+ "   </device>\n" + "</root>";

	private MediaServer mMediaServer;
	private Handler mHandler;
	private Handler mUIHandler;
	private Context mContext;
	private Handler.Callback mCb = new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			boolean ret = false;
			switch (msg.what) {
			case MessageDefs.MSG_MDMS_START_DMS:
				Log.v(TAG, "-------------->MessageDefs.MSG_MDMS_START_DMS");
				if (!mMediaServer.isRunning()) {
					Log.v(TAG, "first time -------------->MessageDefs.MSG_MDMS_START_DMS");
					mMediaServer.addPlugIn(new ID3Format());
					mMediaServer.addPlugIn(new GIFFormat());
					mMediaServer.addPlugIn(new JPEGFormat());
					mMediaServer.addPlugIn(new PNGFormat());
					mMediaServer.addPlugIn(new MPEGFormat());
					ImageAlbumsDirectory imageDir = new ImageAlbumsDirectory("images");
					mMediaServer.getContentDirectory().addDirectory(imageDir);
					MusicAlbumArtistDirectory musicDir = new MusicAlbumArtistDirectory("musics");
					mMediaServer.getContentDirectory().addDirectory(musicDir);
					VideoClollectionDirectory videoDir = new VideoClollectionDirectory("Videos");
					mMediaServer.getContentDirectory().addDirectory(videoDir);
					UUID uuid = UUID.randomUUID();
					Log.v(TAG, "start MDMS -----------------> with UDN:" + uuid.toString());
					mMediaServer.setUDN("uuid:" + uuid.toString());
					mMediaServer.start();
					Log.v(TAG, "before start server -------------->MessageDefs.MSG_MDMS_START_DMS");
				}
				break;
			case MessageDefs.MSG_MDMS_STOP_DMS:
				if(mMediaServer != null) {
					mMediaServer.stop();
				}
				break;
			default:
				break;
			}
			return ret;
		}
	};

	public ServerProcess(Handler uiHander, Context context) {
		super(TAG);
		mUIHandler = uiHander;
		mContext = context;
		try {
			mMediaServer = new MediaServer(DESCRIPTION, ContentDirectory.SCPD,
					ConnectionManager.SCPD);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Override
	protected void onLooperPrepared() {
		super.onLooperPrepared();
		mHandler = new Handler(mCb);
		if (null != mUIHandler) {
			mUIHandler.sendEmptyMessage(MessageDefs.MSG_MDMS_ON_PROC_PREPARED);
		}
	}

	public void startMediaServer() {
		if (null != mHandler) {
			Log.d(TAG, "Server proc------------------------------> startMediaServer");
			mHandler.sendEmptyMessage(MessageDefs.MSG_MDMS_START_DMS);
		}
	}

	public void stopMediaServer() {
		if (null != mHandler) {
			Log.d(TAG, "Server proc------------------------------> stopMediaServer");
			mHandler.sendEmptyMessage(MessageDefs.MSG_MDMS_STOP_DMS);
		}
	}

	@Override
	public boolean actionControlReceived(Action action) {
		Log.d(TAG, "actionControlReceived " + action.getName());
		return false;
	}

}
