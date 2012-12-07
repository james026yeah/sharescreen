package archermind.dlna.media;

import org.cybergarage.upnp.UPnP;
import org.cybergarage.upnp.std.av.controller.server.BrowseResult;
import org.cybergarage.upnp.std.av.server.object.item.ItemNode;
import org.cybergarage.xml.Attribute;
import org.cybergarage.xml.Node;
import org.cybergarage.xml.Parser;
import org.cybergarage.xml.ParserException;

public class MediaInfoUtil {
	public static MediaItem parseMediaInfo(String didlNodeStr) {
		if (didlNodeStr == null) return null;
		MediaItem mediaItem = null;
		Parser parser = UPnP.getXMLParser();
		try {
			Node resultNode = parser.parse(didlNodeStr);
			if (resultNode == null)
				return null;
			BrowseResult browseResult = new BrowseResult(resultNode);
			if (browseResult.getNContentNodes() < 1) 
				return null;
			Node xmlNode = browseResult.getContentNode(0);
			ItemNode itemNode = new ItemNode();
			if (ItemNode.isItemNode(xmlNode)) {
				itemNode.set(xmlNode);
				if (itemNode.isMovieClass()) {
					Attribute durationAttr = itemNode.getResourceNode(0).getAttribute("duration");
					String duration = null;
					if (durationAttr != null) {
						duration = durationAttr.getValue();
					}
					String albumArtURI = itemNode.getAlbumArtURI();
					String filePath = itemNode.getPropertyValue(UPnp.FILEPATH);
					String title = itemNode.getTitle();
					String itemUri = itemNode.getFirstResource().getURL();
					mediaItem = new VideoItem(duration, albumArtURI, filePath,
							title, itemUri, itemNode.toString());
				} else if (itemNode.isImageClass()) {

					mediaItem = new PhotoItem(itemNode.getAlbumArtURI(),
							itemNode.getPropertyValue(UPnp.FILEPATH),
							itemNode.getTitle(), itemNode.getFirstResource()
									.getURL(), itemNode.toString());
				} else if (itemNode.isAudioClass()) {

					Attribute durationAttr = itemNode.getResourceNode(0).getAttribute("duration");
					String duration = null;
					if (durationAttr != null) {
						duration = durationAttr.getValue();
					}
					String artist = itemNode.getPropertyValue(UPnp.ARITIST);
					String malbum = itemNode.getPropertyValue(UPnp.ALBUM);
					String albumArtURI = itemNode.getAlbumArtURI();
					String filePath = itemNode.getPropertyValue(UPnp.FILEPATH);
					String title = itemNode.getTitle();
					String itemUri = itemNode.getFirstResource().getURL();
					mediaItem = new MusicItem(duration, artist, malbum,
							albumArtURI, filePath, title, itemUri,
							itemNode.toString());
				}

			}
		} catch (ParserException e) {
			e.printStackTrace();
		}

		return mediaItem;
	}
}
