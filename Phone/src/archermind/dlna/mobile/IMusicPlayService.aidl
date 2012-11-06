package archermind.dlna.mobile;

import archermind.dlna.media.MusicItem;

interface IMusicPlayService
{
    void setPlayList(inout List<MusicItem> playlist);
    void playFrom(int i);
    boolean isPlaying();
    void stop();
    void pause();
    void play();
    void prev();
    void next();
    long duration();
    void seekTo(int position);
    
    void openFile(String path);
    void open(in long [] list, int position);
    int getQueuePosition();
    long position();
    long seek(long pos);
    String getTrackName();
    String getAlbumName();
    long getAlbumId();
    String getArtistName();
    long getArtistId();
    void enqueue(in long [] list, int action);
    long [] getQueue();
    void moveQueueItem(int from, int to);
    void setQueuePosition(int index);
    String getPath();
    long getAudioId();
    void setShuffleMode(int shufflemode);
    int getShuffleMode();
    int removeTracks(int first, int last);
    int removeTrack(long id);
    void setRepeatMode(int repeatmode);
    int getRepeatMode();
    int getMediaMountedCount();
    int getAudioSessionId();
}

