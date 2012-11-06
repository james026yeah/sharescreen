package archermind.dlna.media;

import java.util.ArrayList;

public class PhotoAlbum {
	
	public PhotoAlbum(String name, ArrayList<PhotoItem> imageList) {
		this.name = name;
		this.imageList = imageList;
	}
	
	private String name;
	
	private ArrayList<PhotoItem> imageList = new ArrayList<PhotoItem>();
	
	public String getName() {
		return name;
	}
	
	public ArrayList<PhotoItem> getImageList() {
		return imageList;
	}
}
