package com.archermind.ashare.dlna.localmedia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.cybergarage.upnp.std.av.server.Directory;
import org.cybergarage.upnp.std.av.server.object.container.ContainerNode;

import android.util.Log;

public class MusicAlbumArtistDirectory extends Directory {
	private final static String TAG ="MusicAlbumArtistDirectory";
	private HashMap<String, ArrayList<MusicItem>> mArtistData;
	private HashMap<String, ArrayList<MusicItem>> mAlbumData;
	private ArrayList<MusicItem> mAllMusicData;
	boolean mNeedUpdate = true;
	

	public MusicAlbumArtistDirectory(String name) {
		super(name);
		mArtistData = MediaCache.instance().getMusicArtistData();
		mAlbumData = MediaCache.instance().getMusicAlbumData();
		mAllMusicData = MediaCache.instance().getAllMusicData();
	}

	@Override
	public boolean update() {
		if(!mNeedUpdate) return false;
		mNeedUpdate = false;
		Log.v(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> before add to dms");
		printDatas();
		String[] tiltes = new String[] { "所有歌曲", "专集歌曲", "最進播放", "艺术家歌曲" };
		ContainerNode cn = new ContainerNode();
		cn.setID(getContentDirectory().getNextContainerID());
		cn.setTitle(tiltes[3]);
		if (mArtistData == null) {
			return false;
		}
		addNode(cn, mArtistData);
		// 专集歌曲
		ContainerNode cnAlbums = new ContainerNode();
		cnAlbums.setID(getContentDirectory().getNextContainerID());
		cnAlbums.setTitle(tiltes[1]);
		if (mAlbumData == null) {
			return false;
		}
		addNode(cnAlbums, mAlbumData);
		// 所有歌曲
		if (mAllMusicData == null) {
			return false;
		}
		Directory dirall = new MusicItemDirectory(tiltes[0], mAllMusicData);
		addNode(dirall);
		Log.v(TAG, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> after add to dms");
		printDatas();
		return true;
	}

	// 为专集,艺术家添加节点
	public void addNode(ContainerNode cn, HashMap<String, ArrayList<MusicItem>> map) {
		Iterator<Map.Entry<String, ArrayList<MusicItem>>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, ArrayList<MusicItem>> entry = it.next();
			ArrayList<MusicItem> value = entry.getValue();
			String key = (String) entry.getKey();
			Directory dir = new MusicItemDirectory(key, value);
			dir.setContentDirectory(getContentDirectory());
			dir.setID(getContentDirectory().getNextContainerID());
			dir.updateContentList();
			if (dir.getNode("item") != null) {
				getContentDirectory().updateSystemUpdateID();
				cn.addContentNode(dir);
			}
		}
		this.addContentNode(cn);
	}

	public void addNode(Directory dir) {
		dir.setContentDirectory(getContentDirectory());
		dir.setID(getContentDirectory().getNextContainerID());
		dir.updateContentList();
		getContentDirectory().updateSystemUpdateID();
		this.addContentNode(dir);
	}
	
	protected void printDatas() {
		if (mArtistData != null) {
			Log.v(TAG, "albums contain:" + mArtistData.size() + " albums");
			Iterator<Map.Entry<String, ArrayList<MusicItem>>> it = mArtistData.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, ArrayList<MusicItem>> entry = it.next();
				ArrayList<MusicItem> value = entry.getValue();
				String key = (String) entry.getKey();
				Log.v(TAG, "Print Artist << " + key + " >>");
				for(MusicItem item : value) {
					Log.v(TAG, "Print image title:" + item.title + 
							", item id:" + item.itemId + ", url:" + item.itemUri + ", path:" + item.filePath);
				}
			}
		}
	}
}
