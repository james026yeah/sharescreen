package com.archermind.ashare.service;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cybergarage.upnp.std.av.renderer.AVTransport;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import archermind.dlna.mobile.LocalMediaUtil;
import archermind.dlna.mobile.MusicData;
import archermind.dlna.mobile.MusicUtils;

import com.archermind.ashare.dlna.localmedia.MusicItem;

public class MusicPlayService extends MusicService implements OnAudioFocusChangeListener {

	String TAG = "MusicPlayService";
	private static MediaPlayer mMusicPlayer;
	
	private static boolean mIsSupposedToBePlaying = false;
	private static boolean mIsPrepared = false;
	private static boolean mIsPlayOnPhone = true;
	private boolean mServiceInUse = false;
	private static boolean mIsInitialed = false;
	
	private boolean mIsShuffleMode = false;
	private int mRepeatMode = 0;//0---->list
	                            //1--->single recycle
	                            //2------>list recycle
	

	@Override
	public void onCreate() {
		super.onCreate();
		mMusicPlayer = new MediaPlayer();
		mMusicPlayer.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				Intent intent = new Intent("statuschanged");
				intent.putExtra("title", "from playFrom"+MusicData.getNowPlayPositionInList());
				sendBroadcast(intent);
			}
		});
		mMusicPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				if (MusicData.getNowPlayPositionInList() != MusicData.getMusicPlayList().size() - 1) {
					next();
					if (LocalMediaUtil.getWhichOnRemote() != LocalMediaUtil.Defs.MUSIC) {
						mp.start();
					}
					Intent intent = new Intent(MusicUtils.Defs.MUSIC_PLAY_ON_COMPLETE);
					sendBroadcast(intent);
					
					Intent xintent = new Intent("statuschanged");
					xintent.putExtra("title", "from playFrom" + MusicData.getNowPlayPositionInList());
					sendBroadcast(xintent);
				} else {
					pause();
				}
			}
		});
		mMusicPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {
			@Override
			public void onSeekComplete(MediaPlayer mp) {
			}
		});
		mMusicPlayer.setOnErrorListener(new OnErrorListener() {
			
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				if (mIsInitialed) {
					mMusicPlayer.reset();
					try {
						mMusicPlayer.setDataSource(MusicData.getNowPlayingMusic().getFilePath());
						mMusicPlayer.prepare();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				return true;
			}
		});
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.archermind.exit");
		registerReceiver(mReceiver, filter);
	}

	@Override
	public IBinder onBind(Intent intent) {
		mServiceInUse = true;
		return mBinder;
	}

	
//****************************888888888888888888888888888888*************************8
	public void play() {
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
		
		mMusicPlayer.start();
		mIsSupposedToBePlaying = true;
	}
	
	public boolean isPlaying() {
		return mIsSupposedToBePlaying;
	}
	
	private void pauseButtonPressed(){
		if (isPlaying()){
			mMusicPlayer.pause();
			mIsSupposedToBePlaying = false;
		} else {
			play();
		}
		Intent intent = new Intent("statuschanged");
		intent.putExtra("title", "from playFrom"+MusicData.getNowPlayPositionInList());
		sendBroadcast(intent);
	}
	
	private void pause() {
		mMusicPlayer.pause();
		mIsSupposedToBePlaying = false;
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
	
	private void stop() {
		mIsInitialed = false;
		mMusicPlayer.stop();
		mIsSupposedToBePlaying = false;
		
		Intent intent = new Intent(MusicUtils.Defs.MUSIC_STOP);
		sendBroadcast(intent);
		
		Intent xintent = new Intent("statuschanged");
		xintent.putExtra("title", "from playFrom" + MusicData.getNowPlayPositionInList());
		sendBroadcast(xintent);
	}
	
	private void seekTo(int position) {
		mMusicPlayer.seekTo(position);
	}
	
	public long duration() {
		return mMusicPlayer.getDuration();
	}
	
	public long position() {
		return mMusicPlayer.getCurrentPosition();
	}
	
	private void playFrom(int i) {
		Log.d(TAG,"MusicPlayService playFrom:" + i);
		if (i >= 0 && i < MusicData.getMusicPlayList().size()){
			mIsInitialed = true;
			MusicData.setNowPlayPositionInList(i);
			MusicData.setNowPlayingMusic(MusicData.getMusicPlayList().get(i));
			mMusicPlayer.reset();
			try {
				mMusicPlayer.setDataSource(MusicData.getMusicPlayList().get(i).getFilePath());
				mMusicPlayer.prepare();
				mIsPrepared = true;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		} else if (mRepeatMode == 2) {
			if (MusicData.getNowPlayPositionInList() == MusicData.getMusicPlayList().size() - 1){
				playFrom(0);
			} else {
				playFrom(MusicData.getMusicPlayList().size() - 1);
			}
		} else {
			playFrom(MusicData.getNowPlayPositionInList());
		}
	}
	
	public void next() {
		if (mRepeatMode == 1) {
			playFrom(MusicData.getNowPlayPositionInList());
			return;
		}
		if (mIsPrepared == true) {
			if (!mIsShuffleMode) {
				playFrom(MusicData.getNowPlayPositionInList() + 1);
			} else {
				Random mRandom = new Random();
				playFrom(mRandom.nextInt(MusicData.getMusicPlayList().size()));
			}
		}
		Intent intent = new Intent(MusicUtils.Defs.MUSIC_INFO_REFRESH);
		sendBroadcast(intent);
		
		Intent xintent = new Intent("statuschanged");
		xintent.putExtra("title", "from playFrom"+MusicData.getNowPlayPositionInList());
		sendBroadcast(xintent);
	}
	
	public void prev() {
		if (mRepeatMode == 1) {
			playFrom(MusicData.getNowPlayPositionInList());
			return;
		}
		if (mIsPrepared == true) {
			if (!mIsShuffleMode) {
				playFrom(MusicData.getNowPlayPositionInList() - 1);
			} else {
				Random mRandom = new Random();
				playFrom(mRandom.nextInt(MusicData.getMusicPlayList().size()));
			}
		}
		Intent intent = new Intent(MusicUtils.Defs.MUSIC_INFO_REFRESH);
		sendBroadcast(intent);
		
		Intent xintent = new Intent("statuschanged");
		xintent.putExtra("title", "from playFrom"+MusicData.getNowPlayPositionInList());
		sendBroadcast(xintent);
	}
	
	public boolean getShuffleMode() {
		 return mIsShuffleMode;
	}
	
	public void setShuffleMode(boolean shufflemode) {
		 mIsShuffleMode = shufflemode;
	}
	
	public int getRepeatMode() {
		 return mRepeatMode;
	}
	
	public void setRepeatMode(int repeatmode) {
		 mRepeatMode = repeatmode;
	}
	
	public void postToRemote() {
		MusicItem music = MusicData.getNowPlayingMusic();
		postPlay(music.getItemUri(),music.metaData);
		postGetTransportInfo();
	}
	
	@Override
	public void onGettransinforesult(Map obj) {
		Log.e("james","" + "onGettransinforesult" + obj.get("state"));
		if (obj.get("state") != AVTransport.STOPPED) {
//			postGetTransportInfo();
		} else if (LocalMediaUtil.getWhichOnRemote() == LocalMediaUtil.Defs.MUSIC) {
			
		}
//		if (obj.get("state") == AVTransport.STOPPED) {
//			next();
//			MusicItem music = MusicData.getNowPlayingMusic();
//			postNext(music.getItemUri(), music.metaData);
//		} else {
//			postGetCurrentTranSportActions();
//		}
	}
	
	public String getTrackName() {
		return MusicData.getNowPlayingMusic().getTitle();
	}
	
	public boolean getPreparedStatus() {
		return mIsPrepared;
	}
	
	public String getArtistName() {
		return MusicData.getNowPlayingMusic().getArtist();
	}
	
	static class ServiceStub extends IMusicPlayService.Stub {
		WeakReference<MusicPlayService> mService;

		ServiceStub(MusicPlayService service) {
			mService = new WeakReference<MusicPlayService>(service);
		}

		public boolean isPlaying() {
			return mService.get().isPlaying();
		}

		public void pause() {
			mService.get().pause();
		}

		public void play() {
			 mService.get().play();
		}

		public void setShuffleMode(boolean shufflemode) {
			 mService.get().setShuffleMode(shufflemode);
		}

		public boolean getShuffleMode() {
			 return mService.get().getShuffleMode();
		}

		public void setRepeatMode(int repeatmode) {
			 mService.get().setRepeatMode(repeatmode);
		}

		public int getRepeatMode() {
			 return mService.get().getRepeatMode();
		}

		@Override
		public void playFrom(int i) {
			mService.get().playFrom(i);
		}

		@Override
		public void seekTo(int position) {
			mService.get().seekTo(position);
		}

		public void prev() {
			 mService.get().prev();
		}

		public void next() {
			 mService.get().next();
		}

		public void stop() {
			 mService.get().stop();
		}
		
		public long duration() {
			 return mService.get().duration();
		}

		public String getArtistName() {
			 return mService.get().getArtistName();
		}
		
		public String getTrackName() {
			 return mService.get().getTrackName();
		}
		
		public long position() {
			 return mService.get().position();
		}

		@Override
		public MusicItem getNowPlayItem() {
			return MusicData.getNowPlayingMusic();
		}

		@Override
		public boolean getPreparedStatus() {
			return mService.get().getPreparedStatus();
		}

		@Override
		public void pauseButtonPressed() {
			mService.get().pauseButtonPressed();
		}

		@Override
		public void postToRemote(){
			mService.get().postToRemote();
		}

		@Override
		public boolean getInitialed() {
			return mIsInitialed;
		}

		@Override
		public void setInitialed(boolean ini){
			mIsInitialed = ini;
		}

	}

	private final IBinder mBinder = new ServiceStub(this);
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (isPlaying()) {
				stop();
				stopSelf();
			}
		}
	};


	@Override
	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
		case AudioManager.AUDIOFOCUS_LOSS:
			Log.e(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
			pause();
			break;
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			Log.e(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
			pause();
			break;
		}
	}
}