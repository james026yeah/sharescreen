package archermind.dlna.media;

import org.cybergarage.upnp.std.av.server.object.container.ContainerNode;

public class MusicCategoryInfo {
	
	public String name;
	
	public int itemCount;
	
	public MusicCategoryInfo(String name, int itemCount) {
		this.name = name;
		this.itemCount = itemCount;
	}
	
	
}
