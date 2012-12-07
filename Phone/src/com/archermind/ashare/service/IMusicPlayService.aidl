package com.archermind.ashare.service;

import com.archermind.ashare.dlna.localmedia.MusicItem;

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
    boolean getPreparedStatus();
    void pauseButtonPressed();
    void setPlayOnPhone(boolean onphone);
    boolean getPlayOnPhone();
    boolean getInitialed();
    void setInitialed(boolean ini);
}

