package archermind.dlna.media;
import android.os.Parcel;
import android.os.Parcelable;

public class VideoItem implements Parcelable {
	public VideoItem(String duration,String thumbFilePath,String filePath,String title,String itemUri) {
		this.duration=duration;
		this.thumbFilePath=thumbFilePath;
		this.filePath=filePath;
		this.itemUri=itemUri;
		this.title=title;

	}

	public String duration;
	public String thumbFilePath;
	public String filePath;
	public String title;
	public String itemUri;
	
	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getThumbFilePath() {
		return thumbFilePath;
	}

	public void setThumbFilePath(String thumbFilePath) {
		this.thumbFilePath = thumbFilePath;
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

	public VideoItem(Parcel source) {
		duration = source.readString();
		thumbFilePath = source.readString();
		filePath = source.readString();
		title = source.readString();
		itemUri=source.readString();

	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeString(duration);
		dest.writeString(thumbFilePath);
		dest.writeString(filePath);
		dest.writeString(title);
		dest.writeString(itemUri);

	}

	// 实例化静态内部对象CREATOR实现接口Parcelable.Creator
	public static final Parcelable.Creator<VideoItem> CREATOR = new Creator<VideoItem>() {

		@Override
		public VideoItem[] newArray(int size) {
			return new VideoItem[size];
		}

		// 将Parcel对象反序列化为ParcelableDate
		@Override
		public VideoItem createFromParcel(Parcel source) {
			return new VideoItem(source);
		}
	};
}
