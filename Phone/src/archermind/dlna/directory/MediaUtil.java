package archermind.dlna.directory;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

public class MediaUtil {
	private Context mContext;

	public MediaUtil(Context context) {
		mContext = context;
	}

	// 歌手歌曲列表
	public Map getAudioArtist() {
		Map map = new HashMap();
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
				null, null, null, MediaStore.Audio.Artists.DEFAULT_SORT_ORDER);
		if (null == cursor) {
			return null;
		}
		if (cursor.moveToFirst()) {
			do {
				ArrayList<File> fileList = new ArrayList<File>();
				String artist = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Artists.ARTIST));
				String where = "artist=?";
				String[] args = new String[] { artist };
				ArrayList<MusicInfo> list = getAudioList(where, args);
				if (artist.lastIndexOf("unknown") != -1) {
					artist = "unknown";
				}
				map.put(artist, list);
			} while (cursor.moveToNext());
		}
		cursor.close();
		return map;
	}

	// 专集歌曲列表
	public Map getAudioAlbum() {
		Map map = new HashMap();
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
				null, null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
		if (null == cursor) {
			return null;
		}
		if (cursor.moveToFirst()) {
			do {
				ArrayList<File> fileList = new ArrayList<File>();
				String album = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Albums.ALBUM));
				String where = "album=?";
				String[] args = new String[] { album };
				ArrayList<MusicInfo> list = getAudioList(where, args);
				if (album.lastIndexOf("unknown") != -1) {
					album = "unknown";
				}
				map.put(album, list);
			} while (cursor.moveToNext());
            
		}
		cursor.close();
		return map;
	}

	// 所有Music
	public ArrayList<MusicInfo> getAudioList(String where, String[] args) {
		ArrayList<MusicInfo> fileList = new ArrayList<MusicInfo>();
		ContentResolver cr = mContext.getContentResolver();
		String[] str = new String[] { MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.YEAR, MediaStore.Audio.Media.SIZE,
				MediaStore.Audio.Media.MIME_TYPE,
				MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.ALBUM };
		Cursor cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				str, where, args, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		if (null == cursor) {
			return null;
		}
		if (cursor.moveToFirst()) {
			do {
				MusicInfo info = new MusicInfo();
				info.atist = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.ARTIST));
				info.album = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.ALBUM));
				info.album_id = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
				info.data = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.DATA));
				info.duration = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.DURATION));
				info.size = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.SIZE));
				info.title = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.TITLE));
				info.trim = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE));
				info.year = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.YEAR));
				info.album_art = getAlbumArt(info.album_id);
				if (null == info.atist || info.atist.equals("<unknown>")) {
					info.atist = "unknown";
				}
				if (null == info.album_art
						|| info.album_art.equals("<unknown>")) {
					info.album_art = "unknown";
				}
				if (null == info.album || info.album.equals("<unknown>")) {
					info.album = "unknown";
				}
				fileList.add(info);
			} while (cursor.moveToNext());
			cursor.close();
		}
		return fileList;
	}

	// video
	public Map initVideo() {
		Map mapVideo = new HashMap<String, ArrayList<VideoInfo>>();
		String[] thumbColumns = new String[] {
				MediaStore.Video.Thumbnails.DATA,
				MediaStore.Video.Thumbnails.VIDEO_ID };

		String[] mediaColumns = new String[] { MediaStore.Video.Media.DATA,
				MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE,
				MediaStore.Video.Media.MIME_TYPE,
				MediaStore.Video.Media.DURATION,
				MediaStore.Video.Media.DATE_TAKEN };

		// 首先检索SDcard上所有的video
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
				mediaColumns, null, null, null);
		if (null == cursor) {
			return null;
		}
		ArrayList<VideoInfo> videoList = new ArrayList<VideoInfo>();
		HashSet<String> set = new HashSet<String>();
		if (cursor.moveToFirst()) {
			do {
				VideoInfo info = new VideoInfo();

				info.filePath = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
				info.mimeType = cursor
						.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
				info.title = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
				info.date_taken = cursor
						.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN));
				if (info.date_taken != null) {
					Long ldate = Long.parseLong(info.date_taken);
					Date date = new Date(ldate);
					SimpleDateFormat myFmt=new SimpleDateFormat("yyyy-MM"); 
					String mydate=myFmt.format(date);
					set.add(mydate);
					info.date_taken = mydate;
				}
				info.duration = cursor
						.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));

				// 获取当前Video对应的Id，然后根据该ID获取其Thumb
				int id = cursor.getInt(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
				String selection = MediaStore.Video.Thumbnails.VIDEO_ID + "=?";
				String[] selectionArgs = new String[] { id + "" };
				Cursor thumbCursor = cr.query(
						MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
						thumbColumns, selection, selectionArgs, null);

				if (thumbCursor.moveToFirst()) {
					info.thumbPath = thumbCursor
							.getString(thumbCursor
									.getColumnIndexOrThrow(MediaStore.Video.Thumbnails.DATA));

				}
				thumbCursor.close();
				// 然后将其加入到videoList
				videoList.add(info);

			} while (cursor.moveToNext());
			cursor.close();
		}
		Iterator<String> it = set.iterator();
		while (it.hasNext()) {
			String date = it.next();
			ArrayList<VideoInfo> arry = new ArrayList<VideoInfo>();
			for (int i = 0; i < videoList.size(); i++) {
				VideoInfo info = videoList.get(i);
				if (date.equals(info.date_taken)) {
					arry.add(info);
				}

			}
			mapVideo.put(date, arry);
		}
		return mapVideo;

	}

	// album_id是专集的id
	public String getAlbumArt(String album_id) {
		String[] projection = new String[] { "album_id" };
		String mUriAlbums = "content://media/external/audio/albums";
		projection = new String[] { "album_art" };
		Cursor cur = mContext.getContentResolver().query(
				Uri.parse(mUriAlbums + "/" + album_id), projection, null, null,
				null);

		String album_art = null;
		if (cur.getCount() > 0 && cur.getColumnCount() > 0) {
			cur.moveToNext();
			album_art = cur.getString(0);
		}
		cur.close();
		cur = null;

		return album_art;
	}

	public Map initImages() {
		Map mapImages = new HashMap<String, ArrayList<ImagesInfo>>();
		String[] thumbColumns = new String[] {
				MediaStore.Images.Thumbnails.DATA,
				MediaStore.Images.Thumbnails.IMAGE_ID };

		String[] mediaColumns = new String[] { MediaStore.Images.Media.DATA,
				MediaStore.Images.Media._ID, MediaStore.Images.Media.TITLE,
				MediaStore.Images.Media.MIME_TYPE,
				MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
				MediaStore.Images.Media.DATE_TAKEN };

		// 首先检索SDcard上所有的video
		ContentResolver cr = mContext.getContentResolver();
		Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
				mediaColumns, null, null, null);
		if (null == cursor) {
			return null;
		}
		ArrayList<ImagesInfo> imageList = new ArrayList<ImagesInfo>();
		HashSet<String> set = new HashSet<String>();
		if (cursor.moveToFirst()) {
			do {
				ImagesInfo info = new ImagesInfo();

				info.data = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
				info.mime_type = cursor
						.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
				info.title = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
				info.bucket_display_name = cursor
						.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME));
				if (info.data != null) {
					String path = info.data.substring(0,
							info.data.lastIndexOf("/"));
					set.add(path);
				}
				// 获取当前Images对应的Id，然后根据该ID获取其Thumb
				int id = cursor.getInt(cursor
						.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
				String selection = MediaStore.Images.Thumbnails.IMAGE_ID + "=?";
				String[] selectionArgs = new String[] { id + "" };
				Cursor thumbCursor = cr.query(
						MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
						thumbColumns, selection, selectionArgs, null);
				if (thumbCursor != null) {
					if (thumbCursor.moveToFirst()) {
						info.thumbPath = thumbCursor
								.getString(thumbCursor
										.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.DATA));

					}
				}
				thumbCursor.close();
				// 然后将其加入到videoList
				imageList.add(info);

			} while (cursor.moveToNext());
			cursor.close();
		}
		Iterator<String> it = set.iterator();

		while (it.hasNext()) {
			String img_path = it.next();
			ArrayList<ImagesInfo> arry = new ArrayList<ImagesInfo>();
			String itempath = "";
			for (int i = 0; i < imageList.size(); i++) {
				ImagesInfo info = imageList.get(i);
				if (img_path.equals(info.data.substring(0,
						info.data.lastIndexOf("/")))) {
					itempath = info.bucket_display_name;
					arry.add(info);
				}

			}
			mapImages.put(itempath, arry);
		}
		return mapImages;
	}
}
