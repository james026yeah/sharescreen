package com.archermind.ashare.dlna.localmedia;
import android.os.Parcel;
import android.os.Parcelable;

public class PhotoItem  implements Parcelable {
	public String filePath;
	public String thumbFilePath;
	public String title;
	public String itemUri;
	public String metaData;
	public String mime_type;
	public String bucket_display_name;
	public int itemId;
	public String getFilePath() {
		return filePath;
	}


	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}


	public String getThumbFilePath() {
		return thumbFilePath;
	}



	public void setThumbFilePath(String thumbFilePath) {
		this.thumbFilePath = thumbFilePath;
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


	
	
	public PhotoItem(String thumbFilePath,String filePath,String title,String itemUri,String metaData) {
		this.filePath=filePath;
		this.itemUri=itemUri;
		this.title=title;
		this.thumbFilePath=thumbFilePath;
		this.metaData = metaData;
	}

	public PhotoItem() {
		
	}

	public PhotoItem(Parcel source) {
		thumbFilePath = source.readString();
		filePath = source.readString();
		title = source.readString();
		itemUri = source.readString();
		metaData = source.readString();
		mime_type = source.readString();
		bucket_display_name = source.readString();
		itemId = source.readInt();
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeString(thumbFilePath);
		dest.writeString(filePath);
		dest.writeString(title);
		dest.writeString(itemUri);
		dest.writeString(metaData);
		dest.writeString(mime_type);
		dest.writeString(bucket_display_name);
		dest.writeInt(itemId);
	}

	// 实例化静态内部对象CREATOR实现接口Parcelable.Creator
	public static final Parcelable.Creator<PhotoItem> CREATOR = new Creator<PhotoItem>() {

		@Override
		public PhotoItem[] newArray(int size) {
			return new PhotoItem[size];
		}

		// 将Parcel对象反序列化为Parcelable
		@Override
		public PhotoItem createFromParcel(Parcel source) {
			return new PhotoItem(source);
		}
	};
}
