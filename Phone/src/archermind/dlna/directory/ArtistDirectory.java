package archermind.dlna.directory;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cybergarage.upnp.std.av.server.ConnectionManager;
import org.cybergarage.upnp.std.av.server.ContentDirectory;
import org.cybergarage.upnp.std.av.server.Directory;
import org.cybergarage.upnp.std.av.server.object.ContentNode;
import org.cybergarage.upnp.std.av.server.object.Format;
import org.cybergarage.upnp.std.av.server.object.FormatObject;
import org.cybergarage.upnp.std.av.server.object.container.ContainerNode;
import org.cybergarage.upnp.std.av.server.object.item.file.FileItemNode;
import org.cybergarage.upnp.std.av.server.object.item.file.FileItemNodeList;
import org.cybergarage.util.Debug;
import org.cybergarage.xml.AttributeList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

public class ArtistDirectory extends Directory {

	private Map artistMap;
	private ArrayList<MusicInfo> listAllMusic;
	private ArrayList<MusicInfo> listRecentlyPy;
	private Map albumsMap;

	public ArtistDirectory(String name, Map map, Map albumsMap,
			ArrayList<MusicInfo> listAllMusic) {
		super(name);
		this.artistMap = map;
		this.albumsMap = albumsMap;
		this.listAllMusic = listAllMusic;
		this.listRecentlyPy = listRecentlyPy;
	}

	@Override
	public boolean update() {
		String[] tiltes = new String[] { "所有歌曲", "专集歌曲", "最進播放", "艺术家歌曲" };
		// 艺术家歌曲
		ContainerNode cn = new ContainerNode();
		cn.setID(getContentDirectory().getNextContainerID());
		cn.setTitle(tiltes[3]);
		if (artistMap == null) {
			return false;
		}
		addNode(cn, artistMap);
		// 专集歌曲
		ContainerNode cnAlbums = new ContainerNode();
		cnAlbums.setID(getContentDirectory().getNextContainerID());
		cnAlbums.setTitle(tiltes[1]);
		if (albumsMap == null) {
			return false;
		}
		addNode(cnAlbums, albumsMap);
		// 所有歌曲
		if (listAllMusic == null) {
			return false;
		}
		Directory dirall = new MusicItemsDirectory(tiltes[0], listAllMusic);
		addNode(dirall);

		return true;
	}

	// 为专集,艺术家添加节点
	public void addNode(ContainerNode cn, Map map) {
		Iterator it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			ArrayList<MusicInfo> value = (ArrayList<MusicInfo>) entry.getValue();
			String key = (String) entry.getKey();
			Directory dir = new MusicItemsDirectory(key, value);
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
}
