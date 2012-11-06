package archermind.dlna.media;

import java.util.ArrayList;

public class Artist {
	
	public Artist(String name, ArrayList<MusicItem> musicList) {
		this.name = name;
		this.musicList = musicList;
	}
	
	private String name;
	
	private ArrayList<MusicItem> musicList = new ArrayList<MusicItem>();
	
	public String getName() {
		return name;
	}
	
	public ArrayList<MusicItem> getMusicsList() {
		return musicList;
	}
}
