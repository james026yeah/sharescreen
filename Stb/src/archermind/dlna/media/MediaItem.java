package archermind.dlna.media;

public abstract class MediaItem {
	public enum MediaType {
		VIDEO,PHOTO,MUSIC
	}
	public abstract MediaType getMeidaType();
	public abstract String getItemUri();
}
