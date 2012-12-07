package com.archermind.ashare.service;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;

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

import archermind.dlna.mobile.MusicData;

import com.archermind.ashare.dlna.localmedia.MusicItem;

public class MusicPlayService extends Service implements OnAudioFocusChangeListener {

	String TAG = "MusicPlayService";
	
	private static final int ONPHONE = 0;
	private static final int ONTV = 1;
	private static final int PUSHING = 2;
	
	private static MusicItem mMusicPlayingItem;
//	private static List<MusicItem> mMusicShowList;
//	private static List<MusicItem> MusicData.getMusicPlayList();
	private static MediaPlayer mMusicPlayer;
	
	private static boolean mIsSupposedToBePlaying = false;
	private static int mMusicPlayPosition = -1;
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
		mMusicPlayer = new MediaPlayer();
		mMusicPlayer.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				Intent intent = new Intent("statuschanged");
				intent.putExtra("title", "from playFrom"+mMusicPlayPosition);
				sendBroadcast(intent);
			}
		});
		mMusicPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				if (mMusicPlayPosition != MusicData.getMusicPlayList().size() - 1) {
					next();
					if (mIsPlayOnPhone) {
						mp.start();
					}
					Intent intent = new Intent("statuschanged");
					intent.putExtra("title", "from playFrom" + mMusicPlayPosition);
					sendBroadcast(intent);
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
				// TODO Auto-generated method stub
				if (mIsInitialed) {
					mMusicPlayer.reset();
					try {
						mMusicPlayer.setDataSource(mMusicPlayingItem.getFilePath());
						mMusicPlayer.prepare();
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
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
		// mDelayedStopHandler.removeCallbacksAndMessages(null);
		mServiceInUse = true;
		return mBinder;
	}

	
//****************************888888888888888888888888888888*************************8
	public void play() {
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
		    AudioManager.AUDIOFOCUS_GAIN);
		
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
			mMusicPlayer.start();
			mIsSupposedToBePlaying = true;
		}
		Intent intent = new Intent("statuschanged");
		intent.putExtra("title", "from playFrom"+mMusicPlayPosition);
		sendBroadcast(intent);
	}
	
	private void pause() {
		mMusicPlayer.pause();
		mIsSupposedToBePlaying = false;
		Intent intent = new Intent("statuschanged");
		intent.putExtra("title", "from playFrom" + mMusicPlayPosition);
		sendBroadcast(intent);
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
	
	private void stop() {
		mIsInitialed = false;
		mMusicPlayer.stop();
		mIsSupposedToBePlaying = false;
		
		Intent intent = new Intent("statuschanged");
		intent.putExtra("title", "from playFrom" + mMusicPlayPosition);
		sendBroadcast(intent);
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
//			mIsSupposedToBePlaying = true;
			mMusicPlayPosition = i;
			mMusicPlayingItem = MusicData.getMusicPlayList().get(i);
			Log.d(TAG,"musicPlayingItem:" + mMusicPlayingItem.getTitle());
		} else if (mRepeatMode == 2) {
			if (mMusicPlayPosition == MusicData.getMusicPlayList().size() - 1){
				playFrom(0);
			} else {
				playFrom(MusicData.getMusicPlayList().size() - 1);
			}
		} else {
			playFrom(mMusicPlayPosition);
		}
	}
	
	public void next() {
		if (mRepeatMode == 1) {
			playFrom(mMusicPlayPosition);
			return;
		}
		if (mIsPrepared == true) {
			if (!mIsShuffleMode) {
				playFrom(mMusicPlayPosition + 1);
			} else {
				Random mRandom = new Random();
				playFrom(mRandom.nextInt(MusicData.getMusicPlayList().size()));
			}
		}
		Intent intent = new Intent("statuschanged");
		intent.putExtra("title", "from playFrom"+mMusicPlayPosition);
		sendBroadcast(intent);
	}
	
	public void prev() {
		if (mRepeatMode == 1) {
			playFrom(mMusicPlayPosition);
			return;
		}
		if (mIsPrepared == true) {
			if (!mIsShuffleMode) {
				playFrom(mMusicPlayPosition - 1);
			} else {
				Random mRandom = new Random();
				playFrom(mRandom.nextInt(MusicData.getMusicPlayList().size()));
			}
		}
		Intent intent = new Intent("statuschanged");
		intent.putExtra("title", "from playFrom"+mMusicPlayPosition);
		sendBroadcast(intent);
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
	
	public int getQueuePosition() {
		 return mMusicPlayPosition;
	}
	
	public String getTrackName() {
		return mMusicPlayingItem.getTitle();
	}
	
	public boolean getPreparedStatus() {
		return mIsPrepared;
	}
	
	public String getArtistName() {
		return mMusicPlayingItem.getArtist();
	}
	
	static class ServiceStub extends IMusicPlayService.Stub {
		WeakReference<MusicPlayService> mService;

		ServiceStub(MusicPlayService service) {
			mService = new WeakReference<MusicPlayService>(service);
		}

		public int getQueuePosition() {
			 return mService.get().getQueuePosition();
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
		public void setPlayList(List<MusicItem> playlist) {
			MusicData.setMusicPlayList(playlist);
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
		
		@Override
		public void setMusicShowList(List<MusicItem> showlist) {
//			mMusicShowList = showlist;
		}

		@Override
		public List<MusicItem> getMusicShowList() {
			return MusicData.getMusicShowList();
		}

		@Override
		public List<MusicItem> getPlayList() {
			return MusicData.getMusicPlayList();
		}
		
		public long position() {
			 return mService.get().position();
		}

		@Override
		public MusicItem getNowPlayItem() {
			return mMusicPlayingItem;
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
		public void setPlayOnPhone(boolean onphone) {
			// TODO Auto-generated method stub
			mIsPlayOnPhone = onphone;
		}

		@Override
		public boolean getPlayOnPhone() {
			// TODO Auto-generated method stub
			return mIsPlayOnPhone;
		}

		@Override
		public boolean getInitialed() {
			// TODO Auto-generated method stub
			return mIsInitialed;
		}

		@Override
		public void setInitialed(boolean ini){
			// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
		switch (focusChange) {
		case AudioManager.AUDIOFOCUS_GAIN:
			Log.e(TAG, "AUDIOFOCUS_GAIN");
			break;
		case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
			Log.e(TAG, "UDIOFOCUS_GAIN_TRANSIENT");
			break;
		case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
			Log.e(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
			break;
		case AudioManager.AUDIOFOCUS_LOSS:
			Log.e(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
			pause();
			break;
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			Log.e(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
			break;
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			Log.e(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
			break;
		case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
			Log.e(TAG, "AUDIOFOCUS_REQUEST_FAILED");
			break;
		}
	}
}
