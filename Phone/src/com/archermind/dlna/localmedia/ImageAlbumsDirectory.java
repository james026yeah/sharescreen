package com.archermind.dlna.localmedia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.cybergarage.upnp.std.av.server.Directory;

import android.util.Log;
import archermind.dlna.media.PhotoItem;

public class ImageAlbumsDirectory extends Directory {
	private final static String TAG = "AlbumsDirectory";
	private boolean isUpdated = false;
	public ImageAlbumsDirectory(String name) {
		super(name);
	}

	@Override
	public boolean update() {
		if(isUpdated) return false;
		boolean ret = false;
		Log.v(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> before add to dms");
		printDatas();
		HashMap<String, ArrayList<PhotoItem>> albums = MediaCache.instance().getImageData();
		if (albums != null) {
			Iterator<Map.Entry<String, ArrayList<PhotoItem>>> it = albums.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, ArrayList<PhotoItem>> entry = it.next();
				ArrayList<PhotoItem> value = entry.getValue();
				String key = (String) entry.getKey();
				Directory dir = new ImageAlbumDirectory(key, value);
				dir.setContentDirectory(getContentDirectory());
				dir.setID(getContentDirectory().getNextContainerID());
				dir.updateContentList();
				if (dir.getNode("item") != null) {
					getContentDirectory().updateSystemUpdateID();
					this.addContentNode(dir);
				}
			}
			ret =  true;
		} 
		Log.v(TAG, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< after add to dms");
		printDatas();
		isUpdated = true;
		return ret;
	}
	
	protected void printDatas() {
		HashMap<String, ArrayList<PhotoItem>> albums = MediaCache.instance().getImageData();
		Log.v(TAG, "albums contain:" + albums.size() + " albums");
		if (albums != null) {
			Iterator<Map.Entry<String, ArrayList<PhotoItem>>> it = albums.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, ArrayList<PhotoItem>> entry = it.next();
				ArrayList<PhotoItem> value = entry.getValue();
				String key = (String) entry.getKey();
				Log.v(TAG, "Print album << " + key + " >>");
				for(PhotoItem item : value) {
					Log.v(TAG, "Print image title:" + item.title + 
							", buket name:" + item.bucket_display_name + 
							", item id:" + item.itemId + ", url:" + item.itemUri + ", path:" + item.filePath);
				}
			}
		}
	}
}
