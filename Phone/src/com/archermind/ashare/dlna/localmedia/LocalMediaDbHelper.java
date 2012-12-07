package com.archermind.ashare.dlna.localmedia;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class LocalMediaDbHelper {
	private Context mCtx;

	public LocalMediaDbHelper(Context ctx) {
		mCtx = ctx;
	}

	public HashMap<String, ArrayList<PhotoItem>> getImageAlbums() {
		long startTime = System.currentTimeMillis();
		ContentResolver cr = mCtx.getContentResolver();
		String[] projection = new String[] {
				MediaStore.Images.Media.DATA,
				MediaStore.Images.Media._ID,
				MediaStore.Images.Media.TITLE,
				MediaStore.Images.Media.MIME_TYPE,
				MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
				MediaStore.Images.Media.DATE_TAKEN };
		Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				projection, null, null, null);
		if (cursor == null) {
			return null;
		}
		
		if(!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}

		HashMap<String, ArrayList<PhotoItem>> albums = 
				new HashMap<String, ArrayList<PhotoItem>>();
		ArrayList<PhotoItem> allImages = new ArrayList<PhotoItem>();
		HashSet<String> set = new HashSet<String>();
		String[] thumbColumns = new String[] {
				MediaStore.Images.Thumbnails.DATA,
				MediaStore.Images.Thumbnails.IMAGE_ID };
		Cursor thumbCursor = cr.query(
				MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
				thumbColumns, null, null, null);

		do {
			String filePath = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.DATA));
			String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.MIME_TYPE));
			if (!MediaCache.supportedMediaItem(mimeType, filePath)) continue;
			
			PhotoItem info = new PhotoItem();
			info.filePath = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.DATA));
			info.mime_type = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.MIME_TYPE));
			info.title = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.TITLE));
			info.bucket_display_name = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.BUCKET_DISPLAY_NAME));
			if (info.filePath != null) {
				String path = info.filePath.substring(0, info.filePath.lastIndexOf("/"));
				set.add(path);
			}

			// get thumbnail of the image
			int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
			if (thumbCursor != null && thumbCursor.moveToFirst()) {
				do {
					int curId = thumbCursor.getInt(thumbCursor.getColumnIndexOrThrow(
							MediaStore.Images.Thumbnails.IMAGE_ID));
					if(curId == id) {
						info.thumbFilePath = thumbCursor.getString(thumbCursor.getColumnIndexOrThrow(
								MediaStore.Images.Thumbnails.DATA));
						break;
					}
				} while (thumbCursor.moveToNext());
			}
			allImages.add(info);
		} while (cursor.moveToNext());
		cursor.close();
		if(thumbCursor != null)
			thumbCursor.close();
		
		Iterator<String> it = set.iterator();
		while (it.hasNext()) {
			String img_path = it.next();
			ArrayList<PhotoItem> album = new ArrayList<PhotoItem>();
			String itempath = "";
			for (int i = 0; i < allImages.size(); i++) {
				PhotoItem info = allImages.get(i);
				if (img_path.equals(info.filePath.substring(0,
						info.filePath.lastIndexOf("/")))) {
					itempath = info.bucket_display_name;
					album.add(info);
				}

			}
			albums.put(itempath, album);
		}
		long stopTime = System.currentTimeMillis();
		Log.v("AlbumsDirectory", "cost time: " + (stopTime - startTime) + "ms");
		return albums;
	}

	public HashMap<String, ArrayList<MusicItem>> getAudioArtists(ArrayList<MusicItem> allMusic) {
		if (null == allMusic || allMusic.size() == 0) {
			return null;
		}
		ContentResolver cr = mCtx.getContentResolver();
		Cursor cursor = cr.query(
				MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, null, null, null,
				MediaStore.Audio.Artists.DEFAULT_SORT_ORDER);
		if (null == cursor) {
			return null;
		}
		if(!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		HashMap<String, ArrayList<MusicItem>> map = new HashMap<String, ArrayList<MusicItem>>();
		do {
			ArrayList<MusicItem> musics = new ArrayList<MusicItem>();
			String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
			for(MusicItem item : allMusic) {
				if(item.artist.equals(artist)) {
					musics.add(item);
				}
			}
			if (artist.lastIndexOf("unknown") != -1) {
				artist = "unknown";
			}
			map.put(artist, musics);
		} while (cursor.moveToNext());

		cursor.close();
		return map;
	}
		
	public HashMap<String, ArrayList<MusicItem>> getAudioAlbums(ArrayList<MusicItem> allMusic) {
		if (null == allMusic || allMusic.size() == 0) {
			return null;
		}
		ContentResolver cr = mCtx.getContentResolver();
		Cursor cursor = cr.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
				null, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
		if (null == cursor) {
			return null;
		}
		if(!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		HashMap<String, ArrayList<MusicItem>> map = new HashMap<String, ArrayList<MusicItem>>();
		do {
			ArrayList<MusicItem> musics = new ArrayList<MusicItem>();
			String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
			for(MusicItem item : allMusic) {
				if(item.album.equals(album)) {
					musics.add(item);
				}
			}
			if (album.lastIndexOf("unknown") != -1) {
				album = "unknown";
			}
			map.put(album, musics);
		} while (cursor.moveToNext());
		cursor.close();
		return map;
	}

	public ArrayList<MusicItem> getAudioList(String where, String[] args) {
		ContentResolver cr = mCtx.getContentResolver();
		String[] str = new String[] { MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.YEAR, MediaStore.Audio.Media.SIZE,
				MediaStore.Audio.Media.MIME_TYPE,
				MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.ALBUM };
		Cursor cursor = cr.query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				str, where, args, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		if (null == cursor) {
			return null;
		}
		if(!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}

		ArrayList<MusicItem> fileList = new ArrayList<MusicItem>();
		do {
			String filePath = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.DATA));
			String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.MIME_TYPE));
			if (!MediaCache.supportedMediaItem(mimeType, filePath)) continue;
			
			MusicItem info = new MusicItem();
			info.artist = cursor.getString(cursor.getColumnIndex(
					MediaStore.Audio.Media.ARTIST));
			info.album = cursor.getString(cursor.getColumnIndex(
					MediaStore.Audio.Media.ALBUM));
			info.filePath = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.DATA));
			info.duration = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.DURATION));
			info.size = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.SIZE));
			info.title = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.TITLE));
			info.mime_type = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE));
			info.year = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.YEAR));
			String albumId = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
			info.albumArtURI = getAlbumArt(albumId);
			if (null == info.artist || info.artist.equals("<unknown>")) {
				info.artist = "unknown";
			}
			if (null == info.albumArtURI
					|| info.albumArtURI.equals("<unknown>")) {
				info.albumArtURI = "";
			}
			if (null == info.album || info.album.equals("<unknown>")) {
				info.album = "unknown";
			}
			fileList.add(info);
		} while (cursor.moveToNext());
		cursor.close();
		return fileList;
	}

	public String getAlbumArt(String album_id) {
		String[] projection = new String[] { "album_id" };
		String mUriAlbums = "content://media/external/audio/albums";
		projection = new String[] { "album_art" };
		Cursor cursor = mCtx.getContentResolver().query(
				Uri.parse(mUriAlbums + "/" + album_id), projection, null, null,
				null);
		String album_art = null;
		if (cursor.getCount() > 0 && cursor.getColumnCount() > 0) {
			cursor.moveToNext();
			album_art = cursor.getString(0);
		}
		cursor.close();
		cursor = null;

		return album_art;
	}
	
	public HashMap<String, ArrayList<VideoItem>> getVideoData() {
		String[] thumbColumns = new String[] {
				MediaStore.Video.Thumbnails.DATA,
				MediaStore.Video.Thumbnails.VIDEO_ID };

		String[] mediaColumns = new String[] { MediaStore.Video.Media.DATA,
				MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE,
				MediaStore.Video.Media.MIME_TYPE,
				MediaStore.Video.Media.DURATION,
				MediaStore.Video.Media.DATE_TAKEN };
		ContentResolver cr = mCtx.getContentResolver();
		Cursor cursor = cr.query(
				MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
				mediaColumns, null, null, null);
		if (cursor == null) {
			return null;
		}
		if(!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		Cursor thumbCursor = cr.query(
				MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
				thumbColumns, null, null, null);
		HashMap<String, ArrayList<VideoItem>> mapVideo = new HashMap<String, ArrayList<VideoItem>>();
		ArrayList<VideoItem> videoList = new ArrayList<VideoItem>();
		HashSet<String> set = new HashSet<String>();
		do {
			String filePath = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.DATA));
			String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.MIME_TYPE));
			if (!MediaCache.supportedMediaItem(mimeType, filePath)) continue;
			
			VideoItem info = new VideoItem();
			info.filePath = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.DATA));
			info.mime_type = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.MIME_TYPE));
			info.title = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.TITLE));
			info.date_taken = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.DATE_TAKEN));
			if (info.date_taken != null) {
				Long ldate = Long.parseLong(info.date_taken);
				Date date = new Date(ldate);
				SimpleDateFormat myFmt=new SimpleDateFormat("yyyy-MM");
				String mydate=myFmt.format(date);
				set.add(mydate);
				info.date_taken = mydate;
			}
			info.duration = cursor.getString(cursor.getColumnIndexOrThrow(
					MediaStore.Video.Media.DURATION));

			int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
			if (thumbCursor != null && thumbCursor.moveToFirst()) {
				do {
					int curId = thumbCursor.getInt(thumbCursor.getColumnIndexOrThrow(
							MediaStore.Video.Thumbnails.VIDEO_ID));
					if(curId == id) {
						info.thumbFilePath = thumbCursor.getString(thumbCursor.getColumnIndexOrThrow(
								MediaStore.Video.Thumbnails.DATA));
						break;
					}
				} while (thumbCursor.moveToNext());
			}
			
			videoList.add(info);
		} while (cursor.moveToNext());

		cursor.close();
		if(thumbCursor != null)
			thumbCursor.close();

		Iterator<String> it = set.iterator();
		while (it.hasNext()) {
			String date = it.next();
			ArrayList<VideoItem> arry = new ArrayList<VideoItem>();
			for (VideoItem item : videoList) {
				if (date.equals(item.date_taken)) {
					arry.add(item);
				}
			}
			mapVideo.put(date, arry);
		}
		return mapVideo;
	}
}
