package com.archermind.ashare.dlna.localmedia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.util.Log;

public class MediaCache {
	private static MediaCache sInstance = null;
	private HashMap<String, ArrayList<PhotoItem>> mImageData = null;
	private HashMap<String, ArrayList<MusicItem>> mMusicArtistData = null;
	private HashMap<String, ArrayList<MusicItem>> mMusicAlbumData = null;
	private ArrayList<MusicItem> mAllMusicData = null;	
	private HashMap<String, ArrayList<VideoItem>> mVideoData = null;
	private LocalMediaDbHelper mDbHelper;
	private boolean mInitialized = false;
	protected MediaCache() {
	}
	public static MediaCache instance() {
		if(sInstance == null) {
			sInstance = new MediaCache();
		}
		return sInstance;
	}
	public boolean isInitialized() {
	    return mInitialized;
	}
	public void init(LocalMediaDbHelper dbHelper) {
		if(mInitialized || dbHelper == null) return;
		long startTime, stopTime;
		mDbHelper = dbHelper;

		// Prepare Image datas
		startTime = System.currentTimeMillis();
		mImageData = mDbHelper.getImageAlbums();
		stopTime = System.currentTimeMillis();
		Log.v("DLNAService", "get image data cost time: " + (stopTime - startTime) + "ms");

		startTime = System.currentTimeMillis();
		mAllMusicData = mDbHelper.getAudioList(null, null);
		stopTime = System.currentTimeMillis();
		Log.v("DLNAService", "get all music data cost time: " + (stopTime - startTime) + "ms");

		startTime = System.currentTimeMillis();
		mMusicArtistData = mDbHelper.getAudioArtists(mAllMusicData);
		stopTime = System.currentTimeMillis();
		Log.v("DLNAService", "get artist data cost time: " + (stopTime - startTime) + "ms");

		startTime = System.currentTimeMillis();
		mMusicAlbumData = mDbHelper.getAudioAlbums(mAllMusicData);
		stopTime = System.currentTimeMillis();
		Log.v("DLNAService", "get album data cost time: " + (stopTime - startTime) + "ms");

		startTime = System.currentTimeMillis();
		mVideoData = mDbHelper.getVideoData();
		stopTime = System.currentTimeMillis();
		Log.v("DLNAService", "get video data cost time: " + (stopTime - startTime) + "ms");
		
		mInitialized = true;
	}

	public HashMap<String, ArrayList<PhotoItem>> getImageData() {
		return mImageData;
	}

	public HashMap<String, ArrayList<MusicItem>> getMusicArtistData() {
		return mMusicArtistData;
	}

	public HashMap<String, ArrayList<MusicItem>> getMusicAlbumData() {
		return mMusicAlbumData;
	}

	public ArrayList<MusicItem> getAllMusicData() {
		return mAllMusicData;
	}
	public HashMap<String, ArrayList<VideoItem>> getVideoData() {
		return mVideoData;
	}
	
	public ArrayList<MusicCategoryInfo> getMusicCategory() {
		ArrayList<MusicCategoryInfo> categoryInfos = new ArrayList<MusicCategoryInfo>();
		if(mAllMusicData != null) {
			categoryInfos.add(new MusicCategoryInfo("所有音乐", mAllMusicData.size()));
		}
		if(mMusicArtistData != null) {
			categoryInfos.add(new MusicCategoryInfo("歌手", mMusicArtistData.size()));
		}
		if(mMusicAlbumData != null) {
			categoryInfos.add(new MusicCategoryInfo("专辑", mMusicAlbumData.size()));
		}
		return categoryInfos;
	}
	
	public ArrayList<Artist> getArtistList() {
		ArrayList<Artist> artists = new ArrayList<Artist>();
		if(mMusicArtistData != null) {
			Iterator<Map.Entry<String, ArrayList<MusicItem>>> it = mMusicArtistData.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, ArrayList<MusicItem>> entry = it.next();
				ArrayList<MusicItem> value = entry.getValue();
				String key = entry.getKey();
				artists.add(new Artist(key, value));
			}
		}
		return artists;
	}
	
	public ArrayList<Album> getAlbumList() {
		ArrayList<Album> albums = new ArrayList<Album>();
		if(mMusicAlbumData != null) {
			Iterator<Map.Entry<String, ArrayList<MusicItem>>> it = mMusicAlbumData.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, ArrayList<MusicItem>> entry = it.next();
				ArrayList<MusicItem> value = entry.getValue();
				String key = entry.getKey();
				albums.add(new Album(key, value));
			}
		}
		return albums;
	}
	
	public ArrayList<VideoCategory> getVideoList() {
		ArrayList<VideoCategory> videos = new ArrayList<VideoCategory>();
		if(mVideoData != null) {
			Iterator<Map.Entry<String, ArrayList<VideoItem>>> it = mVideoData.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, ArrayList<VideoItem>> entry = it.next();
				ArrayList<VideoItem> value = entry.getValue();
				String key = entry.getKey();
				videos.add(new VideoCategory(key, value));
			}
		}
		return videos;
	}

	public ArrayList<PhotoAlbum> getPhotoAlbums() {
		ArrayList<PhotoAlbum> photoAlbums = new ArrayList<PhotoAlbum>();
		if(mImageData != null) {
			Iterator<Map.Entry<String, ArrayList<PhotoItem>>> it = mImageData.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, ArrayList<PhotoItem>> entry = it.next();
				ArrayList<PhotoItem> value = entry.getValue();
				String key = entry.getKey();
				photoAlbums.add(new PhotoAlbum(key, value));
			}
		}
		return photoAlbums;
	}
}
