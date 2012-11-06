package archermind.dlna.media;
import android.os.Parcel;
import android.os.Parcelable;

public class MusicItem implements Parcelable {

	public MusicItem(String duration,String artist,String album,String albumArtURI,String filePath,String title,String itemUri) {
		this.duration=duration;
		this.album=album;
		this.artist=artist;
		this.albumArtURI=albumArtURI;
		this.filePath=filePath;
		this.itemUri=itemUri;
		this.title=title;

	}

	public String duration;
	public String artist;
	public String album;
	public String albumArtURI;
	public String filePath;
	public String title;
	public String itemUri;

	public MusicItem(Parcel source) {
		duration = source.readString();
		artist = source.readString();
		album = source.readString();
		albumArtURI = source.readString();
		filePath = source.readString();
		title = source.readString();
		itemUri=source.readString();

	}
	
	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getAlbumArtURI() {
		return albumArtURI;
	}

	public void setAlbumArtURI(String albumArtURI) {
		this.albumArtURI = albumArtURI;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getItemUri() {
		return itemUri;
	}

	public void setItemUri(String itemUri) {
		this.itemUri = itemUri;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeString(duration);
		dest.writeString(artist);
		dest.writeString(album);
		dest.writeString(albumArtURI);
		dest.writeString(filePath);
		dest.writeString(title);
		dest.writeString(itemUri);

	}

	// 实例化静态内部对象CREATOR实现接口Parcelable.Creator
	public static final Parcelable.Creator<MusicItem> CREATOR = new Creator<MusicItem>() {

		@Override
		public MusicItem[] newArray(int size) {
			return new MusicItem[size];
		}

		// 将Parcel对象反序列化为ParcelableDate
		@Override
		public MusicItem createFromParcel(Parcel source) {
			return new MusicItem(source);
		}
	};
}
