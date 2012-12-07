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
	
	public MusicData(ArrayList<MusicItem> allMusicItem,ArrayList<Artist> musicArtistItem,ArrayList<Album> musicAlbumItem) {
		// TODO Auto-generated constructor stub
		sAllMusic = allMusicItem;
		sMusicArtist = musicArtistItem;
		sMusicAlbum = musicAlbumItem;
	};
	
	public MusicData() {
		// TODO Auto-generated constructor stub
		sAllMusic = null;
		sMusicArtist = null;
		sMusicAlbum = null;
		sMusicPlayList = null;
	};
	
	public void setMusicPlayList(ArrayList<MusicItem> musicPlayList) {
		sMusicPlayList = musicPlayList;
	}
	
	public ArrayList<MusicItem> getMusicPlayList() {
		return sMusicPlayList;
	}
	
	public void setMusicShowList(ArrayList<MusicItem> musicPlayList) {
		sMusicShowList = musicPlayList;
	}
	
	public ArrayList<MusicItem> getMusicShowList() {
		return sMusicShowList;
	}
	
	public void setAllMusic(ArrayList<MusicItem> allMusic) {
		sAllMusic = allMusic;
	}
	
	public ArrayList<MusicItem> getAllMusicData() {
		return sAllMusic;
	}
	
	
	public void setMusicArtist(ArrayList<Artist> musicArtist) {
		sMusicArtist = musicArtist;
	}
	
	public ArrayList<Artist> getMusicArtistsData() {
		return sMusicArtist;
	}
	
	public void setMusicAlbum(ArrayList<Album> musicAlbum) {
		sMusicAlbum = musicAlbum;
	}
	
	public ArrayList<Album> getMusicAlbumData() {
		return sMusicAlbum;
	}
	
	
	
}