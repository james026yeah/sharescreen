package com.archermind.ashare.service;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;

import com.archermind.ashare.dlna.localmedia.MusicItem;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import archermind.dlna.mobile.R;

public class MusicPlayService extends Service {

	String TAG = "MusicPlayService";
	
	private static final int ONPHONE = 0;
	private static final int ONTV = 1;
	private static final int PUSHING = 2;
	
	private static MusicItem mMusicPlayingItem;
	private static List<MusicItem> mMusicShowList;
	private static List<MusicItem> mMusicList;
	private static MediaPlayer mMusicPlayer;
	
	private static boolean mIsSupposedToBePlaying = false;
	private static int mMusicPlayPosition = -1;
	private static boolean mIsPrepared = false;
	private static boolean mIsPlayOnPhone = true;
	private boolean mServiceInUse = false;
	
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
				next();
				if (mIsPlayOnPhone) {
					mp.start();
				}
				Intent intent = new Intent("statuschanged");
				intent.putExtra("title", "from playFrom" + mMusicPlayPosition);
				sendBroadcast(intent);
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
	
	private void stop() {
			mMusicPlayer.stop();
			mIsSupposedToBePlaying = false;
			Intent intent = new Intent("statuschanged");
			intent.putExtra("title", "from playFrom"+mMusicPlayPosition);
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
		if (i >= 0 && i < mMusicList.size()){
			mMusicPlayer.reset();
			try {
				mMusicPlayer.setDataSource(mMusicList.get(i).getFilePath());
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
			mMusicPlayingItem = mMusicList.get(i);
			Log.d(TAG,"musicPlayingItem:" + mMusicPlayingItem.getTitle());
		} else if (mRepeatMode == 2) {
			if (mMusicPlayPosition == mMusicList.size() - 1){
				playFrom(0);
			} else {
				playFrom(mMusicList.size() - 1);
			}
		} else {
			mIsPrepared = false;
			pause();
			seekTo(0);
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
				playFrom(mRandom.nextInt(mMusicList.size()));
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
				playFrom(mRandom.nextInt(mMusicList.size()));
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
			mMusicList = playlist;
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
			mMusicShowList = showlist;
		}

		@Override
		public List<MusicItem> getMusicShowList() {
			return mMusicShowList;
		}

		@Override
		public List<MusicItem> getPlayList() {
			return mMusicList;
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
}
