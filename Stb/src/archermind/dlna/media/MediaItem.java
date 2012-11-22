package archermind.dlna.media;

public abstract class MediaItem {
	enum MediaType {
		VIDEO,PHOTO,MUSIC
	}
	public abstract MediaType getMeidaType();
}
