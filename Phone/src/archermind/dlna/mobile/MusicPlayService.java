package archermind.dlna.mobile;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.IBinder;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import archermind.dlna.media.MusicItem;

public class MusicPlayService extends Service {

	String LOGTAG = "MusicPlayService";
	
	private static MusicItem mMusicPlayingItem;
	private static MusicItem mMusicNextPlayItem;
	private static List<MusicItem> mMusicShowList;
	private static List<MusicItem> mMusicList;
	private static MediaPlayer mMusicPlayer;
	private static boolean mIsSupposedToBePlaying = false;
	private static int mMusicPlayPosition = -1;
	private static boolean mIsPrepared = false;
	private WakeLock mWakeLock;
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
				// TODO Auto-generated method stub
				Intent intent = new Intent("statuschanged");
				intent.putExtra("title", "from playFrom"+mMusicPlayPosition);
				sendBroadcast(intent);
			}
		});
		mMusicPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO Auto-generated method stub
				next();
				Intent intent = new Intent("statuschanged");
				intent.putExtra("title", "from playFrom"+mMusicPlayPosition);
				sendBroadcast(intent);
			}
		});
		mMusicPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {
			@Override
			public void onSeekComplete(MediaPlayer mp) {
				// TODO Auto-generated method stub
			}
		});
	}

	@Override
	public IBinder onBind(Intent intent) {
		// mDelayedStopHandler.removeCallbacksAndMessages(null);
		mServiceInUse = true;
		return mBinder;
	}

	private static class Shuffler {
		private int mPrevious;
		private Random mRandom = new Random();

		public int nextInt(int interval) {
			int ret;
			do {
				ret = mRandom.nextInt(interval);
			} while (ret == mPrevious && interval > 1);
			mPrevious = ret;
			return ret;
		}
	};
	
//****************************888888888888888888888888888888*************************8
	public void play() {
		mMusicPlayer.start();
	}
	
	public boolean isPlaying() {
		return mIsSupposedToBePlaying;
	}
	
	private void pause(){
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
	
	private void stop() {
			mMusicPlayer.stop();
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
		Log.e("james","MusicPlayService playFrom:" + i);
		if (i >= 0 && i < mMusicList.size()){
			mMusicPlayer.reset();
			try {
				mMusicPlayer.setDataSource(mMusicList.get(i).getFilePath());
				mMusicPlayer.prepare();
				mIsPrepared = true;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mIsSupposedToBePlaying = true;
			mMusicPlayPosition = i;
			mMusicPlayingItem = mMusicList.get(i);
			Log.e("james","musicPlayingItem:" + mMusicPlayingItem.getTitle());
		} else if (mRepeatMode == 2) {
			playFrom(0);
		} else {
			stop();
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
	
	
	
	static class ServiceStub extends IMusicPlayService.Stub {
		WeakReference<MusicPlayService> mService;

		ServiceStub(MusicPlayService service) {
			mService = new WeakReference<MusicPlayService>(service);
		}

		public void openFile(String path) {
			// mService.get().open(path);
		}

		public void open(long[] list, int position) {
			// mService.get().open(list, position);
		}

		public int getQueuePosition() {
			 return mService.get().getQueuePosition();
		}

		public void setQueuePosition(int index) {
			// mService.get().setQueuePosition(index);
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

		public String getAlbumName() {
			return "";
			// return mService.get().getAlbumName();
		}

		public long getAlbumId() {
			return 0;
			// return mService.get().getAlbumId();
		}

		public long getArtistId() {
			return 0;
			// return mService.get().getArtistId();
		}

		public void enqueue(long[] list, int action) {
			// mService.get().enqueue(list, action);
		}

		public long[] getQueue() {
			return null;
			// return mService.get().getQueue();
		}

		public void moveQueueItem(int from, int to) {
			// mService.get().moveQueueItem(from, to);
		}

		public String getPath() {
			return "";
			// return mService.get().getPath();
		}

		public long getAudioId() {
			return 0;
			// return mService.get().getAudioId();
		}

		public long seek(long pos) {
			return 0;
			// return mService.get().seek(pos);
		}

		public void setShuffleMode(boolean shufflemode) {
			 mService.get().setShuffleMode(shufflemode);
		}

		public boolean getShuffleMode() {
			 return mService.get().getShuffleMode();
		}

		public int removeTracks(int first, int last) {
			return 0;
			// return mService.get().removeTracks(first, last);
		}

		public int removeTrack(long id) {
			return 0;
			// return mService.get().removeTrack(id);
		}

		public void setRepeatMode(int repeatmode) {
			 mService.get().setRepeatMode(repeatmode);
		}

		public int getRepeatMode() {
			 return mService.get().getRepeatMode();
		}

		public int getMediaMountedCount() {
			return 0;
			// return mService.get().getMediaMountedCount();
		}

		public int getAudioSessionId() {
			return 0;
			// return mService.get().getAudioSessionId();
		}

		@Override
		public void playFrom(int i) {
			mService.get().playFrom(i);
		}

		@Override
		public void setPlayList(List<MusicItem> playlist) {
			// TODO Auto-generated method stub
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
			return mMusicPlayingItem.getArtist();
			// return mService.get().getArtistName();
		}
		
		public String getTrackName() {
			return mMusicPlayingItem.getTitle();
			// return mService.get().getTrackName();
		}
		
		@Override
		public void setMusicShowList(List<MusicItem> showlist) {
			// TODO Auto-generated method stub
			mMusicShowList = showlist;
		}

		@Override
		public List<MusicItem> getMusicShowList() {
			// TODO Auto-generated method stub
			return mMusicShowList;
		}

		@Override
		public List<MusicItem> getPlayList() {
			// TODO Auto-generated method stub
			return mMusicList;
		}
		
		public long position() {
			 return mService.get().position();
		}

		@Override
		public MusicItem getNowPlayItem() {
			// TODO Auto-generated method stub
			return mMusicPlayingItem;
		}
	}

	void notifyChange() {
		
	}
	private final IBinder mBinder = new ServiceStub(this);
}
