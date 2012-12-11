package archermind.dlna.mobile;

import java.util.ArrayList;
import java.util.List;

import com.archermind.ashare.dlna.localmedia.Album;
import com.archermind.ashare.dlna.localmedia.Artist;
import com.archermind.ashare.dlna.localmedia.MusicItem;

public class MusicData{
	
	private static ArrayList<MusicItem> sAllMusic;
	private static ArrayList<Artist> sMusicArtist;
	private static ArrayList<Album> sMusicAlbum;
	private static List<MusicItem> sMusicPlayList;
	private static ArrayList<MusicItem> sMusicShowList;
	
	private static MusicItem sNowPlayingMusic = null;
	private static int sMusicPlayItemInList;
	
	private static boolean sIsMusicPosted = false;
	
	public static void setMusicPlayList(List<MusicItem> musicPlayList) {
		sMusicPlayList = musicPlayList;
	}
	
	public static List<MusicItem> getMusicPlayList() {
		return sMusicPlayList;
	}
	
	public static void setMusicShowList(List<MusicItem> list) {
		sMusicShowList = (ArrayList<MusicItem>) list;
	}
	
	public static ArrayList<MusicItem> getMusicShowList() {
		return sMusicShowList;
	}
	
	public static void setNowPlayingMusic(MusicItem item) {
		sNowPlayingMusic = item;
	}
	
	public static MusicItem getNowPlayingMusic() {
		return sNowPlayingMusic;
	}
	
	public static void setNowPlayPositionInList(int position) {
		sMusicPlayItemInList = position;
	}
	
	public static int getNowPlayPositionInList() {
		return sMusicPlayItemInList;
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
	
	public void setMusicPosted(boolean status) {
		sIsMusicPosted = status;
	}
	
	public boolean getMusicPosted() {
		return sIsMusicPosted;
	}
}