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

public class VideoDirectory extends Directory {

	private Map VideoMap;


	public VideoDirectory(String name, Map map) {
		super(name);
		this.VideoMap = map;

	}

	@Override
	public boolean update() {
		if (null != VideoMap) {
			Iterator it = VideoMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				ArrayList<VideoInfo> value = (ArrayList<VideoInfo>) entry
						.getValue();
				String key = (String) entry.getKey();
				Directory dir = new VideoItemsDirectory(key, value);
				dir.setContentDirectory(getContentDirectory());
				dir.setID(getContentDirectory().getNextContainerID());
				dir.updateContentList();
				if (dir.getNode("item") != null) {
					getContentDirectory().updateSystemUpdateID();
					this.addContentNode(dir);
				}

			}
			return true;
		} else {
			return false;
		}
	}
}
