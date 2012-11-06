package archermind.dlna.process;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.control.ActionListener;
import org.cybergarage.upnp.std.av.renderer.ConnectionManager;
import org.cybergarage.upnp.std.av.server.ContentDirectory;
import org.cybergarage.upnp.std.av.server.MediaServer;
import org.cybergarage.upnp.std.av.server.directory.file.FileDirectory;
import org.cybergarage.upnp.std.av.server.object.format.GIFFormat;
import org.cybergarage.upnp.std.av.server.object.format.ID3Format;
import org.cybergarage.upnp.std.av.server.object.format.JPEGFormat;
import org.cybergarage.upnp.std.av.server.object.format.MPEGFormat;
import org.cybergarage.upnp.std.av.server.object.format.PNGFormat;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import archermind.dlna.directory.ArtistDirectory;
import archermind.dlna.directory.ImagesDirectory;
import archermind.dlna.directory.MusicItemsDirectory;
import archermind.dlna.directory.MediaUtil;
import archermind.dlna.directory.VideoDirectory;
import archermind.dlna.directory.VideoItemsDirectory;
import archermind.dlna.mobile.MessageDefs;

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
				if (!mMediaServer.isRunning()) {
					mMediaServer.addPlugIn(new ID3Format());
					mMediaServer.addPlugIn(new GIFFormat());
					mMediaServer.addPlugIn(new JPEGFormat());
					mMediaServer.addPlugIn(new PNGFormat());
					mMediaServer.addPlugIn(new MPEGFormat());
					MediaUtil mutil = new MediaUtil(mContext);
					ImagesDirectory imageDir = new ImagesDirectory("images",
							mutil.initImages());
					mMediaServer.getContentDirectory().addDirectory(imageDir);
					ArtistDirectory ad = new ArtistDirectory("musics",
							mutil.getAudioArtist(), mutil.getAudioAlbum(),
							mutil.getAudioList(null, null));
					mMediaServer.getContentDirectory().addDirectory(ad);
					VideoDirectory videoDir = new VideoDirectory("Videos",
							mutil.initVideo());
					mMediaServer.getContentDirectory().addDirectory(videoDir);
					UUID uuid = UUID.randomUUID();
					Log.v(TAG, "start MDMS -----------------> with UDN:" + uuid.toString());
					mMediaServer.setUDN("uuid:" + uuid.toString());
					mMediaServer.start();
				}
				break;
			case MessageDefs.MSG_MDMS_STOP_DMS:
				stopMediaServer();
				break;
			case MessageDefs.MSG_MDMS_STOP_PROC:
				ServerProcess.this.quit();
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

	private ArrayList<File> getImageFileList() {
		ArrayList<File> fileList = new ArrayList<File>();
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				null, null, null, MediaStore.Images.Media.DEFAULT_SORT_ORDER);
		if (null == cursor) {
			return null;
		}
		int i = 0;
		if (cursor.moveToFirst()) {
			do {
				String ss = cursor.getString(cursor
						.getColumnIndex(MediaStore.Images.Media.DATA));
				File file = new File(ss);

				fileList.add(file);
				i++;
			} while (cursor.moveToNext());
		}
		return fileList;
	}

	private ArrayList<File> getAudioFileList() {
		ArrayList<File> fileList = new ArrayList<File>();
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		if (null == cursor) {
			return null;
		}
		if (cursor.moveToFirst()) {
			do {
				String ss = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.DATA));

				File file = new File(ss);
				fileList.add(file);
			} while (cursor.moveToNext());
		}
		return fileList;
	}

	private Map getAudioArtistFileList(final Uri uri, final String order,
			String colum, String where) {
		Map map = new HashMap();
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(uri, null, null, null, order);
		if (null == cursor) {
			return null;
		}
		if (cursor.moveToFirst()) {
			do {
				ArrayList<File> fileList = new ArrayList<File>();
				String artist = cursor.getString(cursor.getColumnIndex(colum));
				Cursor cursor2 = cr.query(
						MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
						where + "='" + artist + "'", null,
						MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
				if (null == cursor2) {
					return null;
				}
				if (cursor2.moveToFirst()) {
					do {
						String music = cursor2.getString(cursor2
								.getColumnIndex(MediaStore.Audio.Media.DATA));
						File file = new File(music);
						fileList.add(file);
					} while (cursor2.moveToNext());
				}
				// File artistfile = new File(artist);
				/*
				 * if(artist.lastIndexOf("unknow") != -1) { artist="weizhi"; }
				 */
				map.put(artist, fileList);
			} while (cursor.moveToNext());

		}
		return map;
	}

	private ArrayList<File> getVideoFileList() {
		ArrayList<File> fileList = new ArrayList<File>();
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
				null, null, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER);
		if (null == cursor) {
			return null;
		}
		if (cursor.moveToFirst()) {
			do {
				String ss = cursor.getString(cursor
						.getColumnIndex(MediaStore.Video.Media.DATA));
				File file = new File(ss);
				fileList.add(file);
			} while (cursor.moveToNext());
		}
		return fileList;
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
			Log.d("xiaoye", "123");
			mHandler.sendEmptyMessage(MessageDefs.MSG_MDMS_START_DMS);
		}
	}

	public void stopMediaServer() {
		mMediaServer.stop();
	}

	public void addContentDirectory(String name, String path) {
		// Directory dir = new FileDirectory(name, path);
		// mMediaServer.addContentDirectory(dir);
		FileDirectory fileDir = new FileDirectory(name, path);
		mMediaServer.getContentDirectory().addDirectory(fileDir);
	}

	public void stopProcess() {
		if (null != mHandler) {
			mHandler.sendEmptyMessage(MessageDefs.MSG_MDMS_STOP_PROC);
		}
	}

	@Override
	public boolean actionControlReceived(Action action) {
		Log.d(TAG, "actionControlReceived " + action.getName());
		return false;
	}

}
