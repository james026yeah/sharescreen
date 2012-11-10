package com.archermind.dlna.localmedia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.cybergarage.upnp.std.av.server.Directory;

import android.util.Log;
import archermind.dlna.media.VideoItem;

public class VideoClollectionDirectory extends Directory {
	private final static String TAG = "VideoClollectionDirectory";
	private HashMap<String, ArrayList<VideoItem>> mVideoData;
	private boolean bNeedUpdate = true;
	public VideoClollectionDirectory(String name) {
		super(name);
		mVideoData = MediaCache.instance().getVideoData();
	}

	@Override
	public boolean update() {
		if(!bNeedUpdate) return false;
		bNeedUpdate = false;
		Log.v(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> before add to dms");
		printDatas();
		if (mVideoData != null) {
			Iterator<Map.Entry<String, ArrayList<VideoItem>>> it = mVideoData.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, ArrayList<VideoItem>> entry = it.next();
				ArrayList<VideoItem> value = entry.getValue();
				String key = (String) entry.getKey();
				Directory dir = new VideoItemDirectory(key, value);
				dir.setContentDirectory(getContentDirectory());
				dir.setID(getContentDirectory().getNextContainerID());
				dir.updateContentList();
				if (dir.getNode("item") != null) {
					getContentDirectory().updateSystemUpdateID();
					this.addContentNode(dir);
				}
			}
		}
		Log.v(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> after add to dms");
		printDatas();
		return true;
	}
	
	protected void printDatas() {
		Log.v(TAG, "Video contain:" + mVideoData.size() + " albums");
		if (mVideoData != null) {
			Iterator<Map.Entry<String, ArrayList<VideoItem>>> it = mVideoData.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, ArrayList<VideoItem>> entry = it.next();
				ArrayList<VideoItem> value = entry.getValue();
				String key = (String) entry.getKey();
				Log.v(TAG, "Print Artist << " + key + " >>");
				for(VideoItem item : value) {
					Log.v(TAG, "Print image title:" + item.title + 
							", item id:" + item.item_id + ", url:" + item.itemUri + ", path:" + item.filePath);
				}
			}
		}
	}
}
