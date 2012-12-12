package archermind.dlna.mobile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.cybergarage.upnp.std.av.renderer.AVTransport;

import com.archermind.ashare.dlna.localmedia.VideoItem;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import archermind.dlna.mobile.LocalMediaUtil.Defs;

@SuppressLint({ "HandlerLeak", "HandlerLeak" })
public class VideoViewActivity extends BaseActivity implements OnClickListener, OnAudioFocusChangeListener {

	private static final String TAG = "VideoViewActivity";
	private static final boolean DBUG = true;
	private static final boolean EBUG = true;

	public static final String VIDEO_INDEX = "video_index";
	public static ArrayList<VideoItem> sVideoItemList = new ArrayList<VideoItem>();

	// The number of milliseconds into hours, minutes, seconds
	private static final int TIME_SECOND = 1000;
	private static final int TIME_MINUTE = TIME_SECOND * 60;
	private static final int TIME_HOUR = TIME_MINUTE * 60;

	private static final int HIDE_CONTROL_LAYOUT = 0;
	private static final int PROGRESS_SEEKBAR_REFRESH = 1;
	private static final int HIDE_CONTROL_DEFAULT_TIME = 20 * 1000;

	private static final int TIMER_INTERVAL_TIME = 800;
	private static final float VOLUME_UP = 1f;
	private static final float VOLUME_DOWN = -1f;

	private RelativeLayout mTopLayout;
	private RelativeLayout mBottomLayout;
	private RelativeLayout mBackView;
	private RelativeLayout mPushView;

	private LinearLayout mPrevView;
	private LinearLayout mPlayView;
	private LinearLayout mPauseView;
	private LinearLayout mStopView;
	private LinearLayout mNextView;

	private TextView mNameView;
	private TextView mPromptView;
	private TextView mRealTimeView;
	private TextView mAllTimeView;

	private SeekBar mTimeSeekBar;

	private Animation mProgressBarAnim;
	private LinearLayout mProgressBar;
	private ImageView mProgressIcon;
	private TextView mProgressText;

	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private MediaPlayer mMediaPlayer;
	private AudioManager mAudioManager;

	private String mDeviceName;

	private int mRealTime;
	private int mCurrentIndex;
	private int mVideoListMaxSize;

	private boolean mIsPaused = false;
	private boolean mIsReleased = false;
	private boolean mIsHideControlLayout = false;
	private boolean mIsTimeSeekBarTouched = false;
	private boolean mIsPushStoped = false;
	private boolean mIsPushPlayStoped = false;
	private boolean mIsActivityOnPaused = false;

	private Handler mTimeSeekBarHandler;
	private Timer mTimer;
	private QueryStateTask mQueryStateTask;

	private PowerManager mPowerManager;
	private WakeLock mWakeLock;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Dlog("onCreate");

		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");
		mWakeLock.acquire();

		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

		mCurrentIndex = getIntent().getIntExtra(VIDEO_INDEX, 0);
		mVideoListMaxSize = sVideoItemList.size();

		if (LocalMediaUtil.getWhichOnRemote() == Defs.VIDEO) {
			getFriendlyNameOfRenderer();
			// Now you can directly push
			Dlog("mVideoUri = " + getVideoUri());
			Dlog("local uri = " + LocalMediaUtil.getCurrentUri());
			if (getVideoUri().equals(LocalMediaUtil.getCurrentUri())) {
				// TV has to play the contents of the current push
				initPushedUI();
				if (mWakeLock.isHeld()) {
					mWakeLock.release();
				}
			} else {
				// Can directly push the new content to the TV
				postPlay(getVideoUri(), getVideoMetaData());
				initPushedUI();
				hideControlLayout();
				mPromptView.setVisibility(View.GONE);
				showProgressBar();
			}
		} else {
			// Direct play of local video
			initUnPushedUI();
		}

	}

	private void initVideoView() {
		mTopLayout = (RelativeLayout) findViewById(R.id.video_view_top_layout);
		mBottomLayout = (RelativeLayout) findViewById(R.id.video_view_bottom_layout);
		mTopLayout.getBackground().setAlpha(180);
		mBottomLayout.getBackground().setAlpha(180);

		mPromptView = (TextView) findViewById(R.id.video_view_prompt);
		mBackView = (RelativeLayout) findViewById(R.id.video_view_back);
		mPushView = (RelativeLayout) findViewById(R.id.video_view_push);
		mPrevView = (LinearLayout) findViewById(R.id.video_view_prev);
		mPlayView = (LinearLayout) findViewById(R.id.video_view_play);
		mPauseView = (LinearLayout) findViewById(R.id.video_view_pause);
		mStopView = (LinearLayout) findViewById(R.id.video_view_stop);
		mNextView = (LinearLayout) findViewById(R.id.video_view_next);
		mRealTimeView = (TextView) findViewById(R.id.video_view_current_time);
		mAllTimeView = (TextView) findViewById(R.id.video_view_all_time);

		mNameView = (TextView) findViewById(R.id.video_view_name);
		mNameView.setText(getVideoName());

		mTimeSeekBar = (SeekBar) findViewById(R.id.video_view_progress_seekbar);
		mTimeSeekBar.setProgress(mRealTime);
		mTimeSeekBar.setOnSeekBarChangeListener(timeSeekBarListener);

		mTopLayout.setOnClickListener(this);
		mBottomLayout.setOnClickListener(this);
		mPromptView.setOnClickListener(this);
		mBackView.setOnClickListener(this);
		mPushView.setOnClickListener(this);
		mPrevView.setOnClickListener(this);
		mPlayView.setOnClickListener(this);
		mPauseView.setOnClickListener(this);
		mStopView.setOnClickListener(this);
		mNextView.setOnClickListener(this);

	}

	private void initMediaPlayer() {
		try {
			if (mMediaPlayer != null) {
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
			mMediaPlayer = new MediaPlayer();
			String path = getVideoPath();
			mMediaPlayer.setDataSource(path);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mMediaPlayer.setOnPreparedListener(preparedListener);
			mMediaPlayer.setOnCompletionListener(completionListener);
			mMediaPlayer.setOnBufferingUpdateListener(bufferingUpdateListener);
			mMediaPlayer.prepare();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initSurfaceView() {

		mSurfaceView = (SurfaceView) findViewById(R.id.video_view_surface);
		mSurfaceView.setOnClickListener(this);

		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {

			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				Dlog("surfaceCreated");
				initMediaPlayer();
				mMediaPlayer.setDisplay(mSurfaceHolder);
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				Dlog("surfaceDestroyed");
			}

		});
	}

	OnSeekBarChangeListener timeSeekBarListener = new OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			if (LocalMediaUtil.getWhichOnRemote() == Defs.VIDEO) {
				Dlog("seek mRealTime = " + mRealTime);
				postSeek(mRealTime + "");
				mIsTimeSeekBarTouched = false;
				mPlayView.setVisibility(View.GONE);
				mPauseView.setVisibility(View.VISIBLE);
			} else {
				if (mMediaPlayer != null) {
					mMediaPlayer.seekTo(mRealTime);
					mMediaPlayer.start();
					mIsPaused = false;
					mPlayView.setVisibility(View.GONE);
					mPauseView.setVisibility(View.VISIBLE);
				}
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			if (LocalMediaUtil.getWhichOnRemote() == Defs.VIDEO) {
				mIsTimeSeekBarTouched = true;
				mPlayView.setVisibility(View.GONE);
				mPauseView.setVisibility(View.VISIBLE);
			} else {
				if (mMediaPlayer != null) {
					mMediaPlayer.pause();
					mPauseView.setVisibility(View.GONE);
					mPlayView.setVisibility(View.VISIBLE);
				}
			}
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			mRealTime = progress;
		}
	};

	OnPreparedListener preparedListener = new OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer mp) {
			Dlog("prepare mRealTime = " + mRealTime);
			mp.seekTo(mRealTime);
			int duration = mp.getDuration();
			mTimeSeekBar.setMax(duration);
			mAllTimeView.setText(setDurationToTime(duration));
			mRealTimeView.setText(setDurationToTime(mRealTime));
			initTimeSeekBarHandler();
			mTimeSeekBarHandler.sendEmptyMessage(PROGRESS_SEEKBAR_REFRESH);
			mHandler.sendEmptyMessageDelayed(HIDE_CONTROL_LAYOUT, HIDE_CONTROL_DEFAULT_TIME);
			mIsActivityOnPaused = false;
			mp.start();
		}
	};

	OnCompletionListener completionListener = new OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			Dlog("onCompletion is call");
			if (!mIsActivityOnPaused) {
				setContentView(R.layout.local_media_video_view);
				initVideoView();
				removeMediaPlayer();
				mPauseView.setVisibility(View.GONE);
				mPlayView.setVisibility(View.VISIBLE);
				mNameView.setText("");
				mTimeSeekBar.setProgress(0);
				mPromptView.setVisibility(View.VISIBLE);
				mPromptView.setText(getResources().getString(R.string.video_play_end_prompt_message));
				showControlLayout();
				mHandler.removeMessages(HIDE_CONTROL_LAYOUT);
			}
		}
	};

	OnBufferingUpdateListener bufferingUpdateListener = new OnBufferingUpdateListener() {

		@Override
		public void onBufferingUpdate(MediaPlayer mp, int percent) {

		}
	};

	private void initTimeSeekBarHandler() {
		mTimeSeekBarHandler = new Handler() {
			@SuppressLint("HandlerLeak")
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case PROGRESS_SEEKBAR_REFRESH:
					if (mMediaPlayer != null) {
						mRealTime = mMediaPlayer.getCurrentPosition();
						mTimeSeekBar.setProgress(mRealTime);
						mRealTimeView.setText(setDurationToTime(mRealTime));
						mTimeSeekBarHandler.sendEmptyMessage(PROGRESS_SEEKBAR_REFRESH);
					}
					break;
				}
			}
		};
	}

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
	}

	@Override
	protected void onGetFriendlyName(String friendlyName) {
		if (friendlyName != null && !friendlyName.equals("")) {
			mDeviceName = friendlyName;
		} else {
			Elog("friendlyName is null");
		}
	}

	private String getVideoName() {
		return sVideoItemList.get(mCurrentIndex).getTitle();
	}

	private String getVideoPath() {
		return sVideoItemList.get(mCurrentIndex).getFilePath();
	}

	private String getVideoUri() {
		return sVideoItemList.get(mCurrentIndex).getItemUri();
	}

	private String getVideoMetaData() {
		return sVideoItemList.get(mCurrentIndex).metaData;
	}

	private void showProgressBar() {
		mProgressBar = (LinearLayout) findViewById(R.id.video_push_progress_bar);
		mProgressBarAnim = AnimationUtils.loadAnimation(this, R.anim.progress_bar_anim);
		mProgressIcon = (ImageView) findViewById(R.id.progress_icon);
		mProgressText = (TextView) findViewById(R.id.progress_text);

		mProgressBar.setVisibility(View.VISIBLE);
		mProgressIcon.startAnimation(mProgressBarAnim);
		mProgressText.setText(getResources().getString(R.string.video_push_progress_dialog_message));
		mProgressText.setTextColor(Color.WHITE);
	}

	private void dismissProgressBar() {
		if (mProgressBar != null && mProgressIcon != null) {
			mProgressBar.setVisibility(View.INVISIBLE);
			mProgressIcon.clearAnimation();
		}
	}

	private void initUnPushedUI() {
		Dlog("initUnPushedUI is call");
		mIsPaused = false;
		mIsReleased = false;
		setContentView(R.layout.local_media_video_view);
		initVideoView();
		initSurfaceView();
	}

	private void initPushedUI() {
		Dlog("initPushedUI is call");
		mRealTime = 0;
		setContentView(R.layout.local_media_video_view);
		initVideoView();
		mPromptView.setVisibility(View.VISIBLE);
		mPromptView.setText(getVideoName() + "  " + getResources().getString(R.string.video_prompt_prev_message)
				+ "  " + mDeviceName + "  " + getResources().getString(R.string.video_prompt_next_message));
	}

	private void pushOutVideo() {
		removeMediaPlayer();
		Dlog("push out to TV,  push uri = " + getVideoUri());
		postPlay(getVideoUri(), getVideoMetaData());
		Dlog("mRealTime = " + mRealTime);
		postSeek(mRealTime + "");
		hideControlLayout();
		mPromptView.setVisibility(View.GONE);
		showProgressBar();
	}

	@Override
	public void onGetPlayresult(Boolean state) {
		// the message send to TV success
		if (state) {
			LocalMediaUtil.setWhichOnRemote(Defs.VIDEO);
			LocalMediaUtil.setCurrentUri(getVideoUri());
			dismissProgressBar();
			initPushedUI();
			createTimer();
			if (mWakeLock.isHeld()) {
				mWakeLock.release();
			}
		} else {
			// the message send TV failed
			Elog("onGetPlayresult is null");
		}
	}

	private void createTimer() {
		mTimer = new Timer();
		mQueryStateTask = new QueryStateTask();
		mTimer.schedule(mQueryStateTask, TIMER_INTERVAL_TIME, TIMER_INTERVAL_TIME);
	}

	private class QueryStateTask extends TimerTask {

		@Override
		public void run() {
			try {
				postGetTransportInfo();
				postGetPositionInfo();
			} catch (Exception e) {
				cancelTask();
				e.printStackTrace();
			}
		}
	}

	private void cancelTask() {
		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
		if (mQueryStateTask != null) {
			mQueryStateTask.cancel();
			mQueryStateTask = null;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onGettransinforesult(Map map) {
		if (map != null) {
			String state = (String) map.get("state");
			Dlog("state = " + state);
			if (state.equals(AVTransport.PLAYING)) {
				mIsPushPlayStoped = false;
			}
			if (state.equals(AVTransport.STOPPED)) {
				mIsPushPlayStoped = true;
			}
		} else {
			Elog("onGettransinforesult data is null");
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onGetPositioninforesult(Map map) {
		if (map != null) {
			String duration = (String) map.get("trackDuration");
			String realTime = (String) map.get("relTime");
			int progressTV = setTimeToDuration(realTime);
			int durationTV = setTimeToDuration(duration);
			Dlog("progressTV = " + progressTV + "   durationTV = " + durationTV);
			mRealTime = progressTV;
			// Sudden interruption of the equipment or push back
			if (LocalMediaUtil.getWhichOnRemote() != Defs.VIDEO) {
				cancelTask();
				poststop();
				initUnPushedUI();
				LocalMediaUtil.setWhichOnRemote(Defs.NOT_ANY_ONE);
				LocalMediaUtil.setCurrentUri("");
				return;
			}
			// The push status, drag the progress bar
			if (!mIsTimeSeekBarTouched) {
				mTimeSeekBar.setMax(durationTV);
				mTimeSeekBar.setProgress(progressTV);
				mAllTimeView.setText(duration);
				mRealTimeView.setText(realTime);
			}
			// The push status, play back ends
			if (mIsPushPlayStoped) {
				cancelTask();
				mNameView.setText("");
				mPromptView.setText(getResources().getString(R.string.video_play_end_prompt_message));
				mPauseView.setVisibility(View.GONE);
				mPlayView.setVisibility(View.VISIBLE);
				mIsPushStoped = true;
				mIsPushPlayStoped = false;
				showControlLayout();
				mHandler.removeMessages(HIDE_CONTROL_LAYOUT);
			}
		} else {
			Elog("onGetPositioninforesult data is null");
		}
	}

	@Override
	protected void onRestart() {
		Dlog("onRestart");
		if (LocalMediaUtil.getWhichOnRemote() == Defs.VIDEO) {
			initPushedUI();
			if (mWakeLock.isHeld()) {
				mWakeLock.release();
			}
		} else {
			initUnPushedUI();
			if (!mWakeLock.isHeld()) {
				mWakeLock.acquire();
			}
		}
		super.onRestart();
	}

	@Override
	protected void onPause() {
		Dlog("onPause");
		mIsActivityOnPaused = true;
		if (mMediaPlayer != null) {
			if (mWakeLock.isHeld()) {
				mWakeLock.release();
			}
		}
		removeMediaPlayer();
		super.onPause();
	}

	@Override
	protected void onStop() {
		Dlog("onStop");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		Dlog("onDestroy");
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
		super.onDestroy();
	}

	private void prevVideo() {
		if (mCurrentIndex == 0) {
			Toast.makeText(this, R.string.video_first_number_toast_message, Toast.LENGTH_SHORT).show();
		} else {
			mCurrentIndex--;
			if (LocalMediaUtil.getWhichOnRemote() == Defs.VIDEO) {
				Dlog("prev video");
				postPlay(getVideoUri(), getVideoMetaData());
				removeMediaPlayer();
				hideControlLayout();
				mPromptView.setVisibility(View.GONE);
				showProgressBar();
			} else {
				mRealTime = 0;
				initUnPushedUI();
			}
		}
	}

	private void playVideo() {
		if (LocalMediaUtil.getWhichOnRemote() == Defs.VIDEO) {
			if (mIsPushStoped) {
				Dlog("push out to TV");
				postPlay(getVideoUri(), getVideoMetaData());
				mIsPushStoped = false;
				removeMediaPlayer();
				hideControlLayout();
				mPromptView.setVisibility(View.GONE);
				showProgressBar();
			} else {
				postPauseToPlay();
			}
			mPlayView.setVisibility(View.GONE);
			mPauseView.setVisibility(View.VISIBLE);
		} else {
			// The local play video end
			if (mIsReleased) {
				mIsReleased = false;
				initUnPushedUI();
			}
			// The local play video pause
			if (mIsPaused) {
				mIsPaused = false;
				mPlayView.setVisibility(View.GONE);
				mPauseView.setVisibility(View.VISIBLE);
				mMediaPlayer.start();
			}
		}
	}

	private void pauseVideo() {
		if (LocalMediaUtil.getWhichOnRemote() == Defs.VIDEO) {
			postpause();
			mPauseView.setVisibility(View.GONE);
			mPlayView.setVisibility(View.VISIBLE);
		} else {
			if (mMediaPlayer != null) {
				if (!mIsReleased && !mIsPaused) {
					mMediaPlayer.pause();
					mPauseView.setVisibility(View.GONE);
					mPlayView.setVisibility(View.VISIBLE);
					mIsPaused = true;
				}
			}
		}
	}

	private void stopVideo() {
		if (LocalMediaUtil.getWhichOnRemote() == Defs.VIDEO) {
			cancelTask();
			poststop();
			mIsPushStoped = true;
		} else {
			removeMediaPlayer();
		}
		setContentView(R.layout.local_media_video_view);
		initVideoView();
		mPauseView.setVisibility(View.GONE);
		mPlayView.setVisibility(View.VISIBLE);
		mNameView.setText("");
		mTimeSeekBar.setProgress(0);
		mPromptView.setVisibility(View.VISIBLE);
		mPromptView.setText(getResources().getString(R.string.video_stop_prompt_message));
	}

	private void nextVideo() {
		if (mCurrentIndex == mVideoListMaxSize - 1) {
			Toast.makeText(this, R.string.video_last_number_toast_message, Toast.LENGTH_SHORT).show();
		} else {
			mCurrentIndex++;
			if (LocalMediaUtil.getWhichOnRemote() == Defs.VIDEO) {
				Dlog("next video");
				postPlay(getVideoUri(), getVideoMetaData());
				removeMediaPlayer();
				hideControlLayout();
				mPromptView.setVisibility(View.GONE);
				showProgressBar();
			} else {
				mRealTime = 0;
				initUnPushedUI();
			}
		}
	}

	private void removeMediaPlayer() {
		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
			mIsReleased = true;
			mIsPaused = false;
		}
	}

	private void Elog(String str) {
		if (EBUG)
			Log.e(TAG, str);
	}

	private void Dlog(String str) {
		if (DBUG)
			Log.d(TAG, str);
	}

	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HIDE_CONTROL_LAYOUT:
				hideControlLayout();
				break;
			}
		};
	};

	private String setDurationToTime(int duration) {
		int hour = duration / TIME_HOUR;
		int minute = (duration - hour * TIME_HOUR) / TIME_MINUTE;
		int second = (duration - hour * TIME_HOUR - minute * TIME_MINUTE) / TIME_SECOND;
		return String.format("%02d:%02d:%02d", hour, minute, second);
	}

	private int setTimeToDuration(String time) {
		String[] str = time.split(":");
		int hour = Integer.parseInt(str[0]);
		int minute = Integer.parseInt(str[1]);
		int second = Integer.parseInt(str[2]);
		return hour * TIME_HOUR + minute * TIME_MINUTE + second * TIME_SECOND;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			removeMediaPlayer();
			mHandler.removeMessages(HIDE_CONTROL_LAYOUT);
			break;
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (LocalMediaUtil.getWhichOnRemote() == Defs.VIDEO) {
				// set the message to TV to increase the volume
				postSetVolume(VOLUME_UP);
				return true;
			} else {
				showControlLayout();
			}
			break;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (LocalMediaUtil.getWhichOnRemote() == Defs.VIDEO) {
				// set the message to TV to decrease the volume
				postSetVolume(VOLUME_DOWN);
				return true;
			} else {
				showControlLayout();
			}
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void showControlLayout() {
		mHandler.removeMessages(HIDE_CONTROL_LAYOUT);
		mTopLayout.setVisibility(View.VISIBLE);
		mBottomLayout.setVisibility(View.VISIBLE);
		mIsHideControlLayout = false;
		mHandler.sendEmptyMessageDelayed(HIDE_CONTROL_LAYOUT, HIDE_CONTROL_DEFAULT_TIME);
	}

	private void hideControlLayout() {
		mTopLayout.setVisibility(View.GONE);
		mBottomLayout.setVisibility(View.GONE);
		mIsHideControlLayout = true;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.video_view_top_layout:
		case R.id.video_view_bottom_layout:
			showControlLayout();
			break;
		case R.id.video_view_back:
			removeMediaPlayer();
			mHandler.removeMessages(HIDE_CONTROL_LAYOUT);
			finish();
			break;
		case R.id.video_view_push:
			// Connected equipment
			if (LocalMediaUtil.getConnected()) {
				// is in the push state now
				if (LocalMediaUtil.getWhichOnRemote() == Defs.VIDEO) {
					Dlog("push back to phone");
					LocalMediaUtil.setWhichOnRemote(Defs.NOT_ANY_ONE);
					if (!mWakeLock.isHeld()) {
						mWakeLock.acquire();
					}
				} else {
					// is not in the push state now
					getFriendlyNameOfRenderer();
					pushOutVideo();
				}
			} else {
				// not connected equipment
				Toast.makeText(this, R.string.video_not_connection_toast_message, Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.video_view_prompt:
		case R.id.video_view_surface:
			if (mIsHideControlLayout) {
				showControlLayout();
			} else {
				hideControlLayout();
			}
			break;
		case R.id.video_view_prev:
			prevVideo();
			break;
		case R.id.video_view_play:
			playVideo();
			break;
		case R.id.video_view_pause:
			pauseVideo();
			break;
		case R.id.video_view_stop:
			stopVideo();
			break;
		case R.id.video_view_next:
			nextVideo();
			break;
		}
	}

	@Override
	public void onAudioFocusChange(int focusChange) {
		switch (focusChange) {
		case AudioManager.AUDIOFOCUS_LOSS:
			pauseVideo();
			break;
		}
	}

}
