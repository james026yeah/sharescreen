package archermind.dlna.media;

import org.cybergarage.upnp.std.av.server.object.item.ItemNode;
import org.cybergarage.upnp.std.av.server.object.item.ResourceNode;
import org.cybergarage.xml.Attribute;

import archermind.dlna.directory.Util.UPnp;

public class MediaItem {
	
	protected ItemNode itemNode;
	
	public MediaItem(ItemNode node) {
		this.itemNode = node;
	}
	
	public String getTitle() {
		if (itemNode == null) return null;
		return itemNode.getTitle();
	}
	
	public String getFilePath() {
		if (itemNode == null) return null;
		return itemNode.getPropertyValue(UPnp.FILEPATH);
	}
}
