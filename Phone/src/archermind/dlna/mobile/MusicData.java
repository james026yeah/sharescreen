package archermind.dlna.mobile;

import java.util.ArrayList;

import com.archermind.ashare.dlna.localmedia.Album;
import com.archermind.ashare.dlna.localmedia.Artist;
import com.archermind.ashare.dlna.localmedia.MusicItem;

public class MusicData{
	private static ArrayList<MusicItem> sAllMusic;
	private static ArrayList<Artist> sMusicArtist;
	private static ArrayList<Album> sMusicAlbum;
	private static ArrayList<MusicItem> sMusicPlayList;
	private static ArrayList<MusicItem> sMusicShowList;
	
	public static void setMusicPlayList(ArrayList<MusicItem> musicPlayList) {
		sMusicPlayList = musicPlayList;
	}
	
	public static ArrayList<MusicItem> getMusicPlayList() {
		return sMusicPlayList;
	}
	
	public static void setMusicShowList(ArrayList<MusicItem> musicPlayList) {
		sMusicShowList = musicPlayList;
	}
	
	public static ArrayList<MusicItem> getMusicShowList() {
		return sMusicShowList;
	}
	
	public static void setAllMusic(ArrayList<MusicItem> allMusic) {
		sAllMusic = allMusic;
	}
	
	public static ArrayList<MusicItem> getAllMusicData() {
		return sAllMusic;
	}
	
	
	public static void setMusicArtist(ArrayList<Artist> musicArtist) {
		sMusicArtist = musicArtist;
	}
	
	public static ArrayList<Artist> getMusicArtistsData() {
		return sMusicArtist;
	}
	
	public static void setMusicAlbum(ArrayList<Album> musicAlbum) {
		sMusicAlbum = musicAlbum;
	}
	
	public static ArrayList<Album> getMusicAlbumData() {
		return sMusicAlbum;
	}
	
	
	
}