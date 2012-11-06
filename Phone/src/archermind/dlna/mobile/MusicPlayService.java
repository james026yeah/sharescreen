package archermind.dlna.mobile;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import archermind.dlna.media.MusicItem;

public class MusicPlayService extends Service {

	String LOGTAG = "MusicPlayService";
	
	private static List<MusicItem> mMusicList;
	private static MediaPlayer mMusicPlayer;
	private static boolean mIsSupposedToBePlaying = false;
	private static int mMusicPlayPosition = 0;
	private WakeLock mWakeLock;
	private boolean mServiceInUse = false;
	private boolean mPausedByTransientLossOfFocus = false;

	private int mServiceStartId = -1;

	public static final String CMDNAME = "command";
	public static final String CMDTOGGLEPAUSE = "togglepause";
	public static final String CMDSTOP = "stop";
	public static final String CMDPAUSE = "pause";
	public static final String CMDPREVIOUS = "previous";
	public static final String CMDNEXT = "next";

	public static final String TOGGLEPAUSE_ACTION = "archermind.dlna.mobile.music.musicservicecommand.togglepause";
	public static final String PAUSE_ACTION = "archermind.dlna.mobile.music.musicservicecommand.pause";
	public static final String PREVIOUS_ACTION = "archermind.dlna.mobile.music.musicservicecommand.previous";
	public static final String NEXT_ACTION = "archermind.dlna.mobile.music.musicservicecommand.next";

	private final static int IDCOLIDX = 0;

	private static final int TRACK_ENDED = 1;
	private static final int RELEASE_WAKELOCK = 2;
	private static final int SERVER_DIED = 3;
	private static final int MAX_HISTORY_SIZE = 20;

	public static final int REPEAT_NONE = 0;
	public static final int REPEAT_CURRENT = 1;
	public static final int REPEAT_ALL = 2;

	public static final int SHUFFLE_NONE = 0;
	public static final int SHUFFLE_NORMAL = 1;
	public static final int SHUFFLE_AUTO = 2;

	private int mRepeatMode = REPEAT_NONE;
	private int mShuffleMode = SHUFFLE_NONE;
	private long[] mPlayList = null;
	private Vector<Integer> mHistory = new Vector<Integer>(MAX_HISTORY_SIZE);
	private static MultiPlayer mPlayer;

	

	private int mPlayListLen = 0;

	private AudioManager mAudioManager;
	private Cursor mCursor;// the cursor of now playing songs
	private int mPlayPos = -1;// the position of now playing song
	private String mFileToPlay;// the File path of file to play

	@Override
	public void onCreate() {
		super.onCreate();
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		mMusicPlayer = new MediaPlayer();
		mMusicPlayer.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				// TODO Auto-generated method stub
				Intent intent = new Intent("statuschanged");
				sendBroadcast(intent);
			}
		});
		mMusicPlayer.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO Auto-generated method stub
			}
		});
		mMusicPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {
			@Override
			public void onSeekComplete(MediaPlayer mp) {
				// TODO Auto-generated method stub
			}
		});
		mPlayer = new MultiPlayer();

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this
				.getClass().getName());
		mWakeLock.setReferenceCounted(false);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// mDelayedStopHandler.removeCallbacksAndMessages(null);
		mServiceInUse = true;
		return mBinder;
	}

	@Override
	public void onRebind(Intent intent) {
		// mDelayedStopHandler.removeCallbacksAndMessages(null);
		mServiceInUse = true;
	}

	


	// ****************************************providers a
	// player***********************
	/**
	 * Provides a unified interface for dealing with midi files and other media
	 * files.
	 */
	private class MultiPlayer {
		private MediaPlayer mMediaPlayer = new MediaPlayer();
		private Handler mHandler;
		private boolean mIsInitialized = false;

		public MultiPlayer() {
			mMediaPlayer.setWakeMode(MusicPlayService.this,
					PowerManager.PARTIAL_WAKE_LOCK);
		}

		public void setDataSource(String path) {
			try {
				mMediaPlayer.reset();
				mMediaPlayer.setOnPreparedListener(null);
				if (path.startsWith("content://")) {
					mMediaPlayer.setDataSource(MusicPlayService.this,
							Uri.parse(path));
				} else {
					mMediaPlayer.setDataSource(path);
				}
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mMediaPlayer.prepare();
			} catch (IOException ex) {
				mIsInitialized = false;
				return;
			} catch (IllegalArgumentException ex) {
				mIsInitialized = false;
				return;
			}
			mMediaPlayer.setOnCompletionListener(listener);
			mMediaPlayer.setOnErrorListener(errorListener);
			Intent i = new Intent(
					AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
			i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
			i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
			sendBroadcast(i);
			mIsInitialized = true;
		}

		public boolean isInitialized() {
			return mIsInitialized;
		}

		public void start() {
			mMediaPlayer.start();
		}

		public void stop() {
			mMediaPlayer.reset();
			mIsInitialized = false;
		}

		/**
		 * You CANNOT use this player anymore after calling release()
		 */
		public void release() {
			stop();
			mMediaPlayer.release();
		}

		public void pause() {
			mMediaPlayer.pause();
		}

		public void setHandler(Handler handler) {
			mHandler = handler;
		}

		MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				// Acquire a temporary wakelock, since when we return from
				// this callback the MediaPlayer will release its wakelock
				// and allow the device to go to sleep.
				// This temporary wakelock is released when the RELEASE_WAKELOCK
				// message is processed, but just in case, put a timeout on it.
				mWakeLock.acquire(30000);
				mHandler.sendEmptyMessage(TRACK_ENDED);
				mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
			}
		};

		MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
			public boolean onError(MediaPlayer mp, int what, int extra) {
				switch (what) {
				case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
					mIsInitialized = false;
					mMediaPlayer.release();
					// Creating a new MediaPlayer and settings its wakemode does
					// not
					// require the media service, so it's OK to do this now,
					// while the
					// service is still being restarted
					mMediaPlayer = new MediaPlayer();
					mMediaPlayer.setWakeMode(MusicPlayService.this,
							PowerManager.PARTIAL_WAKE_LOCK);
					mHandler.sendMessageDelayed(
							mHandler.obtainMessage(SERVER_DIED), 2000);
					return true;
				default:
					Log.d("MultiPlayer", "Error: " + what + "," + extra);
					break;
				}
				return false;
			}
		};

		public long duration() {
			return mMediaPlayer.getDuration();
		}

		public long position() {
			return mMediaPlayer.getCurrentPosition();
		}

		public long seek(long whereto) {
			mMediaPlayer.seekTo((int) whereto);
			return whereto;
		}

		public void setVolume(float vol) {
			mMediaPlayer.setVolume(vol, vol);
		}

		public void setAudioSessionId(int sessionId) {
			mMediaPlayer.setAudioSessionId(sessionId);
		}

		public int getAudioSessionId() {
			return mMediaPlayer.getAudioSessionId();
		}
	}

	// *********control MusicPlayer********
	/**
	 * Seeks to the position specified.
	 * 
	 * @param pos
	 *            The position to seek to, in milliseconds
	 */
	public long seek(long pos) {
		if (mPlayer.isInitialized()) {
			if (pos < 0)
				pos = 0;
			if (pos > mPlayer.duration())
				pos = mPlayer.duration();
			return mPlayer.seek(pos);
		}
		return -1;
	}

	/**
	 * Returns whether something is currently playing
	 * 
	 * @return true if something is playing (or will be playing shortly, in case
	 *         we're currently transitioning between tracks), false if not.
	 */
	public boolean isPlaying() {
		return mIsSupposedToBePlaying;
	}

	/**
	 * Returns the current playback position in milliseconds
	 */
	public long position() {
		if (mPlayer.isInitialized()) {
			return mPlayer.position();
		}
		return -1;
	}

	private void stop(boolean remove_status_icon) {
		if (mPlayer.isInitialized()) {
			mPlayer.stop();
		}
		mFileToPlay = null;
		if (mCursor != null) {
			mCursor.close();
			mCursor = null;
		}
		if (remove_status_icon) {
			// gotoIdleState();
		} else {
			stopForeground(false);
		}
		if (remove_status_icon) {
			mIsSupposedToBePlaying = false;
		}
	}


	/**
	 * Opens the specified file and readies it for playback.
	 * 
	 * @param path
	 *            The full path of the file to be opened.
	 */

	private void ensurePlayListCapacity(int size) {
		if (mPlayList == null || size > mPlayList.length) {
			// reallocate at 2x requested size so we don't
			// need to grow and copy the array for every
			// insert
			long[] newlist = new long[size * 2];
			int len = mPlayList != null ? mPlayList.length : mPlayListLen;
			for (int i = 0; i < len; i++) {
				newlist[i] = mPlayList[i];
			}
			mPlayList = newlist;
		}
		// FIXME: shrink the array when the needed size is much smaller
		// than the allocated size
	}

	// A simple variation of Random that makes sure that the
	// value it returns is not equal to the value it returned
	// previously, unless the interval is 1.
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
	private void pause(){
		if (isPlaying()){
			mMusicPlayer.pause();
			mIsSupposedToBePlaying = false;
		} else {
			mMusicPlayer.start();
			mIsSupposedToBePlaying = true;
		}
	}
	/*
	 * By making this a static class with a WeakReference to the Service, we
	 * ensure that the Service can be GCd even when the system process still has
	 * a remote reference to the stub.
	 */
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
			return 0;
			// return mService.get().getQueuePosition();
		}

		public void setQueuePosition(int index) {
			// mService.get().setQueuePosition(index);
		}

		public boolean isPlaying() {
			return mIsSupposedToBePlaying;
			// return mService.get().isPlaying();
		}

		public void pause() {
			mService.get().pause();
		}

		public void play() {
			Log.i("james", "play");
			// mService.get().play();
		}

		public String getTrackName() {
			return "";
			// return mService.get().getTrackName();
		}

		public String getAlbumName() {
			return "";
			// return mService.get().getAlbumName();
		}

		public long getAlbumId() {
			return 0;
			// return mService.get().getAlbumId();
		}

		public String getArtistName() {
			return "";
			// return mService.get().getArtistName();
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

		public long position() {
			return 0;
			// return mService.get().position();
		}

		public long duration() {
			return 0;
			// return mService.get().duration();
		}

		public long seek(long pos) {
			return 0;
			// return mService.get().seek(pos);
		}

		public void setShuffleMode(int shufflemode) {
			// mService.get().setShuffleMode(shufflemode);
		}

		public int getShuffleMode() {
			return 0;
			// return mService.get().getShuffleMode();
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
			// mService.get().setRepeatMode(repeatmode);
		}

		public int getRepeatMode() {
			return 0;
			// return mService.get().getRepeatMode();
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
			// TODO Auto-generated method stub
			if (i >= 0 && i < mMusicList.size()){
				mMusicPlayer.reset();
				try {
					mMusicPlayer.setDataSource(mMusicList.get(i).getFilePath());
					mMusicPlayer.prepare();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mMusicPlayer.start();
				mIsSupposedToBePlaying = true;
				mMusicPlayPosition = i;
			} else {
				stop();
			}
		}

		@Override
		public void setPlayList(List<MusicItem> playlist) {
			// TODO Auto-generated method stub
			mMusicList = playlist;
		}

		@Override
		public void seekTo(int position) {
			mMusicPlayer.seekTo(position);
		}

		public void prev() {
			// mService.get().prev();
			playFrom(mMusicPlayPosition - 1);
		}

		public void next() {
			// mService.get().next(true);
			playFrom(mMusicPlayPosition + 1);
		}

		public void stop() {
			// mService.get().stop();
			if (isPlaying()) {
				pause();
				seekTo(0);
			}
		}
	}

	void notifyChange() {
		Intent intent = new Intent("statuschanged");
		sendStickyBroadcast(intent);
	}
	private final IBinder mBinder = new ServiceStub(this);
}
