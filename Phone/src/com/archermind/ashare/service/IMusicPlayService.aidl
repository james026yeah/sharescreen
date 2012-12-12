package com.archermind.ashare.service;

import com.archermind.ashare.dlna.localmedia.MusicItem;

interface IMusicPlayService
{
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
    void postToRemote();
    long position();
    boolean getShuffleMode();
    void setShuffleMode(boolean shufflemode);
    int getRepeatMode();
    void setRepeatMode(int repeatmode);
    boolean getPreparedStatus();
    void pauseButtonPressed();
    boolean getInitialed();
    void setInitialed(boolean ini);
}

