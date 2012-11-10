package archermind.dlna.media;

import java.util.ArrayList;

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.std.av.controller.MediaController;
import org.cybergarage.upnp.std.av.server.object.ContentNode;
import org.cybergarage.upnp.std.av.server.object.DIDLLiteNode;
import org.cybergarage.upnp.std.av.server.object.container.ContainerNode;
import org.cybergarage.upnp.std.av.server.object.item.ItemNode;

import archermind.dlna.directory.Util.UPnp;


public class MeidaDataLoadUtil {
	
	public static final int INDEX_OF_PHOTO_ROOT_NODE =  0;
	public static final int INDEX_OF_MUSIC_ROOT_NODE =  1;
	public static final int INDEX_OF_VIDEO_ROOT_NODE =  2;
	public static final int INDEX_OF_MUSIC_ARTISITS_NODE =  0;
	public static final int INDEX_OF_MUSIC_ALBUMS_NODE =  1;
	public static final int INDEX_OF_ALL_MUSIC_NODE =  2;
	
	private static ContentNode getPhotoRootNode(MediaController mc, Device device) {
		if (mc == null || device == null) return null;
		ContainerNode rootContainer = mc.browse(device, "0", true);
		return rootContainer.getContentNode(INDEX_OF_PHOTO_ROOT_NODE);
	}
	
	public static ArrayList<PhotoAlbum> loadPhotoCategory(MediaController mc, Device device) {
		if (mc == null || device == null) return null;
		ArrayList<PhotoAlbum> photoAblumList = new ArrayList<PhotoAlbum>();
		try {
			ContentNode photoRootNode = getPhotoRootNode(mc, device);
			ContainerNode photoContainerNode = mc.browse(device, photoRootNode.getID(), true);
			for (int i = 0; i < photoContainerNode.getChildCount();i++) {
				ContentNode photoDirectoryNode = mc.browse(device, photoContainerNode.getContentNode(i).getID());
				ArrayList<PhotoItem> photoList = new ArrayList<PhotoItem>();
				for (int j = 0; j < photoDirectoryNode.getNNodes();j++ ){
					if (!ItemNode.isItemNode(photoDirectoryNode.getNode(j))) {
						continue;
					}
					ItemNode node = (ItemNode) photoDirectoryNode.getNode(j);
					DIDLLiteNode didNode = new DIDLLiteNode();
					didNode.addNode(node);
					//String duration,String thumbFilePath,String filePath,String title,String itemUri
					photoList.add(new PhotoItem(node.getAlbumArtURI(),node.getPropertyValue(UPnp.FILEPATH),node.getTitle(),node.getFirstResource().getURL(),didNode.toString()));
				}
				PhotoAlbum pa = new PhotoAlbum(photoContainerNode.getContentNode(i).getTitle(), photoList);
				photoAblumList.add(pa);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return photoAblumList;
		}
		return photoAblumList;
	}
	
	private static ContentNode getMusicRootNode(MediaController mc, Device device) {
		if (mc == null || device == null) return null;
		ContainerNode rootContainer = mc.browse(device, "0", true);
		return rootContainer.getContentNode(INDEX_OF_MUSIC_ROOT_NODE);
	}
	
	public static ArrayList<MusicCategoryInfo> loadMusicCategory(MediaController mc, Device device) {
		if (mc == null || device == null) {
			return null;
		}
		ArrayList<MusicCategoryInfo> list = new ArrayList<MusicCategoryInfo>();
		try {
			ContentNode musicRootNode = getMusicRootNode(mc, device);
			int w =9;
			if (w == 0) throw new ArrayIndexOutOfBoundsException();
			ContainerNode musicContainerNode = mc.browse(device, musicRootNode.getID(), true);
			for (int i = 0; i < musicContainerNode.getNNodes(); i++) {
				ContentNode contentNode = musicContainerNode.getContentNode(i);
				ContainerNode containerNode = mc.browse(device, musicContainerNode.getContentNode(i).getID(), true);
				MusicCategoryInfo mcInfo = new MusicCategoryInfo(contentNode.getTitle(),containerNode.getChildCount());
				list.add(mcInfo);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return list;
		}
		return list;
	}
	
	public static ArrayList<MusicItem> loadAllMusics(MediaController mc, Device device) {
		if (mc == null || device == null) {
			return null;
		}
		ArrayList<MusicItem> allMusicList = new ArrayList<MusicItem>();
		try {
			ContentNode musicRootNode = getMusicRootNode(mc, device);
			ContainerNode musicContainerNode = mc.browse(device, musicRootNode.getID(), true);
			ContainerNode allMusicNode = mc.browse(device, musicContainerNode.getContentNode(INDEX_OF_ALL_MUSIC_NODE).getID(), true);
			if (allMusicNode.isContainerNode()) {
				int n = allMusicNode.getNNodes();
				for (int i = 0; i < n; i++) {
					if (!ItemNode.isItemNode(allMusicNode.getNode(i))) {
						continue;
					}
					ItemNode node = (ItemNode) allMusicNode.getNode(i);
					/*ResourceNode rn = node.getResourceNode(0);
					String duration = rn.getAttribute("duration").getValue();*/
					//
					String duration=node.getResourceNode(0).getAttribute("duration").getValue();
					String artist=node.getPropertyValue(UPnp.ARITIST);
					String album=node.getPropertyValue(UPnp.ALBUM);
					String albumArtURI=node.getAlbumArtURI();
					String filePath=node.getPropertyValue(UPnp.FILEPATH);
					String title=node.getTitle();
					String itemUri=node.getFirstResource().getURL();
					DIDLLiteNode didNode = new DIDLLiteNode();
					didNode.addNode(node);
					String ss = didNode.toXMLString();
					String s2 = didNode.toString();
					allMusicList.add(new MusicItem(duration,artist,album,albumArtURI,filePath,title,itemUri,didNode.toString()));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return allMusicList;
		}
		return allMusicList;
	}
	
	public static ArrayList<Album> loadAlbums(MediaController mc, Device device) {
		if (mc == null || device == null) {
			return null;
		}
		ArrayList<Album> albumList = new ArrayList<Album>();
		try {
			ContentNode musicRootNode = getMusicRootNode(mc, device);
			ContainerNode musicContainerNode = mc.browse(device, musicRootNode.getID(), true);
			String id = musicContainerNode.getContentNode(INDEX_OF_MUSIC_ALBUMS_NODE).getID();
			ContainerNode albumsMusicNode = mc.browse(device, id, true);
			for (int i = 0; i < albumsMusicNode.getChildCount();i++) {
				ContainerNode albumNode = mc.browse(device, albumsMusicNode.getContentNode(i).getID(), true);
				ArrayList<MusicItem> musicList = new ArrayList<MusicItem>();
				for (int j = 0; j < albumNode.getNNodes();j++ ){
					if (!ItemNode.isItemNode(albumNode.getNode(j))) {
						continue;
					}
					ItemNode node = (ItemNode) albumNode.getNode(j);
					String duration=node.getResourceNode(0).getAttribute("duration").getValue();
					String artist=node.getPropertyValue(UPnp.ARITIST);
					String malbum=node.getPropertyValue(UPnp.ALBUM);
					String albumArtURI=node.getAlbumArtURI();
					String filePath=node.getPropertyValue(UPnp.FILEPATH);
					String title=node.getTitle();
					String itemUri=node.getFirstResource().getURL();
					DIDLLiteNode didNode = new DIDLLiteNode();
					didNode.addNode(node);
					musicList.add(new MusicItem(duration,artist,malbum,albumArtURI,filePath,title,itemUri,didNode.toString()));
				}
				Album album = new Album(albumsMusicNode.getContentNode(i).getTitle(), musicList);
				albumList.add(album);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return albumList;
		}
		return albumList;
	}
	
	public static ArrayList<Artist> loadArtists(MediaController mc, Device device) {
		if (mc == null || device == null) {
			return null;
		}
		ArrayList<Artist> artistList = new ArrayList<Artist>();
		try {
			ContentNode musicRootNode = getMusicRootNode(mc, device);
			ContainerNode musicContainerNode = mc.browse(device, musicRootNode.getID(), true);
			ContainerNode artistsMusicNode = mc.browse(device, musicContainerNode.getContentNode(INDEX_OF_MUSIC_ARTISITS_NODE).getID());
			for (int i = 0; i < artistsMusicNode.getChildCount();i++) {
				ContainerNode artistNode = mc.browse(device, artistsMusicNode.getContentNode(i).getID());
				ArrayList<MusicItem> musicList = new ArrayList<MusicItem>();
				for (int j = 0; j < artistNode.getNNodes();j++ ){
					if (!ItemNode.isItemNode(artistNode.getNode(j))) {
						continue;
					}
					ItemNode node = (ItemNode) artistNode.getNode(j);
					String duration=node.getResourceNode(0).getAttribute("duration").getValue();
					String artist=node.getPropertyValue(UPnp.ARITIST);
					String malbum=node.getPropertyValue(UPnp.ALBUM);
					String albumArtURI=node.getAlbumArtURI();
					String filePath=node.getPropertyValue(UPnp.FILEPATH);
					String title=node.getTitle();
					String itemUri=node.getFirstResource().getURL();
					DIDLLiteNode didNode = new DIDLLiteNode();
					didNode.addNode(node);
					musicList.add(new MusicItem(duration,artist,malbum,albumArtURI,filePath,title,itemUri,didNode.toString()));
				}
				Artist artist = new Artist(artistsMusicNode.getContentNode(i).getTitle(), musicList);
				artistList.add(artist);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return artistList;
		}
		return artistList;
	}
	
	private static ContentNode getVideoRootNode(MediaController mc, Device device) {
		if (mc == null || device == null) return null;
		ContainerNode rootContainer = mc.browse(device, "0", true);
		return rootContainer.getContentNode(INDEX_OF_VIDEO_ROOT_NODE);
	}
	
	public static ArrayList<VideoCategory> loadVideoCategories(MediaController mc, Device device) {
		if (mc == null || device == null) return null;
		ArrayList<VideoCategory> videoCategoryList = new ArrayList<VideoCategory>();
		try {
			ContentNode videoRootNode = getVideoRootNode(mc, device);
			ContainerNode videoContainerNode = mc.browse(device, videoRootNode.getID(), true);
			for (int i = 0; i < videoContainerNode.getChildCount();i++) {
				ContentNode datetNode = mc.browse(device, videoContainerNode.getContentNode(i).getID());
				ArrayList<VideoItem> videoList = new ArrayList<VideoItem>();
				for (int j = 0; j < datetNode.getNNodes();j++ ){
					if (!ItemNode.isItemNode(datetNode.getNode(j))) {
						continue;
					}
					ItemNode node = (ItemNode) datetNode.getNode(j);
					String duration=node.getResourceNode(0).getAttribute("duration").getValue();
					String albumArtURI=node.getAlbumArtURI();
					String filePath=node.getPropertyValue(UPnp.FILEPATH);
					String title=node.getTitle();
					String itemUri=node.getFirstResource().getURL();
					DIDLLiteNode didNode = new DIDLLiteNode();
					didNode.addNode(node);
					videoList.add(new VideoItem(duration,albumArtURI,filePath,title,itemUri,didNode.toString()));
				}
				VideoCategory video = new VideoCategory(videoContainerNode.getContentNode(i).getTitle(), videoList);
				videoCategoryList.add(video);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return videoCategoryList;
		}
		return videoCategoryList;
	}
}
