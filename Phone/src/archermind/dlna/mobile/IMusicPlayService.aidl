package archermind.dlna.mobile;

import archermind.dlna.media.MusicItem;

interface IMusicPlayService
{
    void setPlayList(inout List<MusicItem> playlist);
    List<MusicItem> getPlayList();
    void setMusicShowList(inout List<MusicItem> showlist);
    List<MusicItem> getMusicShowList();
    void playFrom(int i);
    boolean isPlaying();
    String getArtistName();
    String getTrackName();
    void stop();
    MusicItem getNowPlayItem();
    void pause();
    void play();
    void prev();
    void next();
    long duration();
    void seekTo(int position);
    long position();
    boolean getShuffleMode();
    void setShuffleMode(boolean shufflemode);
    int getRepeatMode();
    void setRepeatMode(int repeatmode);
    int getQueuePosition();
    
    void openFile(String path);
    void open(in long [] list, int position);
    long seek(long pos);
    String getAlbumName();
    long getAlbumId();
    long getArtistId();
    void enqueue(in long [] list, int action);
    long [] getQueue();
    void moveQueueItem(int from, int to);
    void setQueuePosition(int index);
    String getPath();
    long getAudioId();
    int removeTracks(int first, int last);
    int removeTrack(long id);
    int getMediaMountedCount();
    int getAudioSessionId();
}

