package archermind.dlna.media;

import java.util.ArrayList;

public class VideoCategory {
	
	private ArrayList<VideoItem> videoList = new ArrayList<VideoItem>();
	
	public VideoCategory(String name, ArrayList<VideoItem> videoList) {
		this.name = name;
		this.videoList = videoList;
	}
	
	private String name;
	
	public String getName() {
		return name;
	}
	
	public ArrayList<VideoItem> getVideosList() {
		return videoList;
	}
}
