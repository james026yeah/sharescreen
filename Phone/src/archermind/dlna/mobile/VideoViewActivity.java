package archermind.dlna.mobile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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
import android.os.Vibrator;
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

@SuppressLint({ "HandlerLeak", "HandlerLeak" })
public class VideoViewActivity extends BaseActivity implements OnClickListener, OnAudioFocusChangeListener {

	private static final String TAG = "VideoViewActivity";

	public static final String VIDEO_INDEX = "video_index";
	public static ArrayList<VideoItem> sVideoItemList = new ArrayList<VideoItem>();

	//The number of milliseconds into hours, minutes, seconds
	private static final int TIME_SECOND = 1000;
	private static final int TIME_MINUTE = TIME_SECOND * 60;
	private static final int TIME_HOUR = TIME_MINUTE * 60;

	private static final int HIDE_CONTROL_LAYOUT = 0;
	private static final int PROGRESS_SEEKBAR_REFRESH = 1;
	private static final int HIDE_CONTROL_DEFAULT_TIME = 20 * 1000;

	private static final int TIMER_INTERVAL_TIME = 800;
	public static final float VOLUME_UP = 1f;
	public static final float VOLUME_DOWN = -1f;

	private RelativeLayout mTopLayout;
	private RelativeLayout mBottomLayout;
	private RelativeLayout mBackView;
	private RelativeLayout mPushView;
	
//	private ImageView mMuteView;
	private ImageView mPrevView;
	private ImageView mPlayView;
	private ImageView mPauseView;
	private ImageView mStopView;
	private ImageView mNextView;

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

	private String mVideoPath;
	private String mVideoName;
	private String mDeviceName = "XXX";

	private int mDuration;
	private int mRealTime;
	private int mMaxSound;
	private int mRealSound;
	private int mCurrentIndex;
	private int mVideoListMaxSize;

	private boolean mIsPaused = false;
	private boolean mIsReleased = false;
//	private boolean mIsSilenced = false;
	private boolean mIsHideControlLayout = false;
	private boolean mIsPushed = false;
	private boolean mIsTimeSeekBarTouched = false;
	private boolean mIsPushStoped = false;
//	private boolean mIsFirstPushed = false;
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
		log("onCreate");
		
		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");
		mWakeLock.acquire();
		
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

		mCurrentIndex = getIntent().getIntExtra(VIDEO_INDEX, 0);
		mVideoListMaxSize = sVideoItemList.size();

		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		mMaxSound = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//		mRealSound = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		
		getVideoName();
		getVideoPath();

//		log("mCurrentIndex = " + mCurrentIndex);
//		log("mVideoPath = " + mVideoPath);
//		log("mVideoName = " + mVideoName);

		initUnPushedUI();
	}

	private void initVideoView() {

		mTopLayout = (RelativeLayout) findViewById(R.id.video_view_top_layout);
		mBottomLayout = (RelativeLayout) findViewById(R.id.video_view_bottom_layout);
		mTopLayout.getBackground().setAlpha(180);
		mBottomLayout.getBackground().setAlpha(180);

		mPromptView = (TextView) findViewById(R.id.video_view_prompt);
		mBackView = (RelativeLayout) findViewById(R.id.video_view_back);
		mPushView = (RelativeLayout) findViewById(R.id.video_view_push);
		mPrevView = (ImageView) findViewById(R.id.video_view_prev);
		mPlayView = (ImageView) findViewById(R.id.video_view_play);
		mPauseView = (ImageView) findViewById(R.id.video_view_pause);
		mStopView = (ImageView) findViewById(R.id.video_view_stop);
		mNextView = (ImageView) findViewById(R.id.video_view_next);
//		mMuteView = (ImageView) findViewById(R.id.video_view_mute);
		mRealTimeView = (TextView) findViewById(R.id.video_view_current_time);
		mAllTimeView = (TextView) findViewById(R.id.video_view_all_time);

		mNameView = (TextView) findViewById(R.id.video_view_name);
		getVideoName();
		mNameView.setText(mVideoName);

		mTimeSeekBar = (SeekBar) findViewById(R.id.video_view_progress_seekbar);
		mTimeSeekBar.setProgress(mRealTime);
		mTimeSeekBar.setOnSeekBarChangeListener(timeSeekBarListener);

//		setMuteViewBg();
		
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
//		mMuteView.setOnClickListener(this);

	}

	private void initMediaPlayer() {
		try {
			if (mMediaPlayer != null) {
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
			getVideoPath();
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setDataSource(mVideoPath);
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
				log("surfaceCreated");
				initMediaPlayer();
				mMediaPlayer.setDisplay(mSurfaceHolder);
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				log("surfaceDestroyed");
			}

		});
	}

	OnSeekBarChangeListener timeSeekBarListener = new OnSeekBarChangeListener() {

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			if (mIsPushed) {
				log("seek mRealTime = " + mRealTime);
				postSeek(mRealTime + "");
				mIsTimeSeekBarTouched = false;
				mPlayView.setVisibility(View.GONE);
				mPauseView.setVisibility(View.VISIBLE);
			} else {
				if(mMediaPlayer != null) {
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
			if (!mIsPushed) {
				if(mMediaPlayer != null) {
					mMediaPlayer.pause();
					mPauseView.setVisibility(View.GONE);
					mPlayView.setVisibility(View.VISIBLE);
				}
			} else {
				mIsTimeSeekBarTouched = true;
				mPlayView.setVisibility(View.GONE);
				mPauseView.setVisibility(View.VISIBLE);
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
			log("prepare mRealTime = " + mRealTime);

			mp.seekTo(mRealTime);

			mDuration = mp.getDuration();
			mTimeSeekBar.setMax(mDuration);
			mAllTimeView.setText(setDurationToTime(mDuration));
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
			log("onCompletion is call");

			if(!mIsActivityOnPaused) {
				setContentView(R.layout.local_media_video_view);
				initVideoView();
	
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
				mIsReleased = true;
	
				mPauseView.setVisibility(View.GONE);
				mPlayView.setVisibility(View.VISIBLE);
				mNameView.setText("");
				mTimeSeekBar.setProgress(0);
//				setMuteViewBg();
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
		if(friendlyName == null || friendlyName.equals("")) {
			Toast.makeText(this, R.string.video_not_connection_toast_message, Toast.LENGTH_SHORT).show();
		} else {
			mDeviceName = friendlyName;
			pushVideo();
		}
	}
	
	private void getVideoName() {
		mVideoName = sVideoItemList.get(mCurrentIndex).getTitle();
	}

	private void getVideoPath() {
		mVideoPath = sVideoItemList.get(mCurrentIndex).getFilePath();
	}

//	private void setMuteViewBg() {
//		if(mRealSound == 0) {
//			mIsSilenced = true;
//			mMuteView.setBackgroundResource(R.drawable.btn_media_mute);
//		} else {
//			mIsSilenced = false;
//			mMuteView.setBackgroundResource(R.drawable.btn_media_volume);
//		}
//	}
	
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
		log("initUnPushedUI is call");
		mIsPaused = false;
		mIsReleased = false;
		setContentView(R.layout.local_media_video_view);
		initVideoView();
		initSurfaceView();
	}

	private void initPushedOutUI() {
		log("initPushedOutUI is call");
		mRealTime = 0;
		setContentView(R.layout.local_media_video_view);
		initVideoView();
		mPromptView.setVisibility(View.VISIBLE);
		mPromptView.setText(mVideoName + "  " + getResources().getString(R.string.video_prompt_prev_message)
				+ "  " + mDeviceName + "  " + getResources().getString(R.string.video_prompt_next_message));
	}

//	private void setMute() {
//		if (mIsSilenced) {
//			if (mIsPushed) {
//				postGetVolume();
//			} else {
//				mMuteView.setBackgroundResource(R.drawable.btn_media_volume);
//				mIsSilenced = false;
//			}
//		} else {
//			if (mIsPushed) {
//				postSetMute(0.0f);
//			} else {
//				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
//			}
//			Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//			vib.vibrate(200);
//			mMuteView.setBackgroundResource(R.drawable.btn_media_mute);
//			mIsSilenced = true;
//		}
//	}

	private void pushVideo() {
		if (!mIsPushed) {
			log("push out to TV");
			log("current uri = " + sVideoItemList.get(mCurrentIndex).getItemUri());
			mIsPushed = true;
//			mIsFirstPushed = true;
			postPlay(sVideoItemList.get(mCurrentIndex).getItemUri(),
					sVideoItemList.get(mCurrentIndex).metaData);
			if (mMediaPlayer != null) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
				mIsReleased = true;
			}
			hideControlLayout();
			mPromptView.setVisibility(View.GONE);
			showProgressBar();
		} else {
			log("push back to phone");
			if(!mWakeLock.isHeld()) {
				mWakeLock.acquire();
			}
			mIsPushed = false;
			cancelTask();
			postGetPositionInfo();
			poststop();
		}
	}

	@Override
	public void onGetPlayresult(Boolean obj) {
		log("onGetPlayresult is call");
		if (obj) {
//			log("mRealTime = " + mRealTime);
//			postSeek(mRealTime + "");
			dismissProgressBar();
			initPushedOutUI();
			createTimer();
			if(mWakeLock.isHeld()) {
				mWakeLock.release();
			}
//			postGetMute();
//			postGetVolume();
		} else {
			log("onGetPlayresult is null");
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
		}
		if (mQueryStateTask != null) {
			mQueryStateTask.cancel();
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onGettransinforesult(Map obj) {
		if (obj != null) {
			// String state = (String) obj.get("state");
			// String statu = (String) obj.get("statu");
			// String speed = (String) obj.get("speed");
			// log("state = " + state + "  statu = " + statu + "  speed = " +
			// speed);
		} else {
			log("onGettransinforesult data is null");
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onGetPositioninforesult(Map obj) {
		if (obj != null) {
			String duration = (String) obj.get("trackDuration");
			String realTime = (String) obj.get("relTime");
			int progressTV = setTimeToDuration(realTime);
			int durationTV = setTimeToDuration(duration);

			log("realTime = " + realTime + "   progressTV = " + progressTV + "   durationTV = " + durationTV);

			if (!mIsPushed) {
				mRealTime = progressTV;
				initUnPushedUI();
				return;
			}

			if (!mIsTimeSeekBarTouched) {
				mTimeSeekBar.setMax(durationTV);
				mTimeSeekBar.setProgress(progressTV);
				mAllTimeView.setText(duration);
				mRealTimeView.setText(realTime);
			}

			if(progressTV < durationTV) {
				if (progressTV == durationTV - TIME_SECOND) {
					mIsPushPlayStoped = true;
				} else {
					mIsPushPlayStoped = false;
				}
			}

			if (progressTV == 0 && mIsPushPlayStoped) {
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
			log("onGetPositioninforesult data is null");
		}
	}

//	@Override
//	public void onGetGetmuteresult(String obj) {
//		if (obj != null) {
//			mIsSilenced = Boolean.parseBoolean(obj);
//			log("mIsSilenced = " + mIsSilenced);
//			if (mIsSilenced) {
//				mMuteView.setBackgroundResource(R.drawable.btn_media_mute);
//			} else {
//				mMuteView.setBackgroundResource(R.drawable.btn_media_volume);
//			}
//		} else {
//			log("onGetGetmuteresult is null");
//		}
//	}

	@Override
	protected void onRestart() {
		log("onRestart");
		if(mIsPushed) {
			initPushedOutUI();
			if(mWakeLock.isHeld()) {
				mWakeLock.release();
			}
		} else {
			initUnPushedUI();
			if(!mWakeLock.isHeld()) {
				mWakeLock.acquire();
			}
		}
		super.onRestart();
	}
	
	@Override
	protected void onPause() {
		log("onPause");
		mIsActivityOnPaused = true;
		if(mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.release();
			mMediaPlayer = null;
			if(mWakeLock.isHeld()) {
				mWakeLock.release();
			}
		}
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		log("onStop");
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		log("onDestroy");
		if(mWakeLock.isHeld()) {
			mWakeLock.release();
		}
		super.onDestroy();
	}
	
//	@Override
//	public void onGetGetvolumeresult(String obj) {
//		if (obj != null) {
//			log("obj = " + obj);
//			if (mIsFirstPushed) {
//				postSetVolume(0.5f);
//				mRealSound = (int) (0.5 * mMaxSound);
//				mIsFirstPushed = false;
//			} else {
//				postSetVolume(Float.parseFloat(obj));
//				mRealSound = (int) (Float.parseFloat(obj) * mMaxSound);
//			}
//			setMuteViewBg();
//		} else {
//			log("onGetGetvolumeresult is null");
//		}
//	}

	private void prevVideo() {
		if(mCurrentIndex == 0) {
			Toast.makeText(this, R.string.video_first_number_toast_message, Toast.LENGTH_SHORT).show();
		} else {
			mCurrentIndex--;
			if (mIsPushed) {
				log("prev video");
				postPlay(sVideoItemList.get(mCurrentIndex).getItemUri(),
						sVideoItemList.get(mCurrentIndex).metaData);
				if (mMediaPlayer != null) {
					mMediaPlayer.stop();
					mMediaPlayer.release();
					mMediaPlayer = null;
					mIsReleased = true;
				}
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
		if (mIsPushed) {
			if (mIsPushStoped) {
				log("push out to TV");
				postPlay(sVideoItemList.get(mCurrentIndex).getItemUri(),
						sVideoItemList.get(mCurrentIndex).metaData);
				mIsPushStoped = false;
				if (mMediaPlayer != null) {
					mMediaPlayer.stop();
					mMediaPlayer.release();
					mMediaPlayer = null;
					mIsReleased = true;
				}
				hideControlLayout();
				mPromptView.setVisibility(View.GONE);
				showProgressBar();
			} else {
				postPauseToPlay();
			}
			mPlayView.setVisibility(View.GONE);
			mPauseView.setVisibility(View.VISIBLE);
		} else {
			if (mIsReleased) {
				mIsReleased = false;
				initUnPushedUI();
			}
			if(mIsPaused) {
				mIsPaused = false;
				mPlayView.setVisibility(View.GONE);
				mPauseView.setVisibility(View.VISIBLE);
				mMediaPlayer.start();
			}
		}
	}

	private void pauseVideo() {
		if (mIsPushed) {
			postpause();
			mPauseView.setVisibility(View.GONE);
			mPlayView.setVisibility(View.VISIBLE);
		} else {
			if (mMediaPlayer != null) {
				if (mIsReleased == false) {
					if (mIsPaused == false) {
						mMediaPlayer.pause();
						mPauseView.setVisibility(View.GONE);
						mPlayView.setVisibility(View.VISIBLE);
						mIsPaused = true;
					}
				}
			}
		}
	}

	private void stopVideo() {
		if (mIsPushed) {
			cancelTask();
			poststop();
			mIsPushStoped = true;
		} else {
			if (mMediaPlayer != null) {
				if (mIsReleased == false) {
					mMediaPlayer.stop();
					mMediaPlayer.release();
					mMediaPlayer = null;
					mIsReleased = true;
				}
			}
		}
		setContentView(R.layout.local_media_video_view);
		initVideoView();
		mPauseView.setVisibility(View.GONE);
		mPlayView.setVisibility(View.VISIBLE);
		mNameView.setText("");
		mTimeSeekBar.setProgress(0);
//		setMuteViewBg();
		mPromptView.setVisibility(View.VISIBLE);
		mPromptView.setText(getResources().getString(R.string.video_stop_prompt_message));
	}

	private void nextVideo() {
		if(mCurrentIndex == mVideoListMaxSize - 1) {
			Toast.makeText(this, R.string.video_last_number_toast_message, Toast.LENGTH_SHORT).show();
		} else {
			mCurrentIndex++;
			if (mIsPushed) {
				log("next video");
				postPlay(sVideoItemList.get(mCurrentIndex).getItemUri(),
						sVideoItemList.get(mCurrentIndex).metaData);
				if (mMediaPlayer != null) {
					mMediaPlayer.stop();
					mMediaPlayer.release();
					mMediaPlayer = null;
					mIsReleased = true;
				}
				hideControlLayout();
				mPromptView.setVisibility(View.GONE);
				showProgressBar();
			} else {
				mRealTime = 0;
				initUnPushedUI();
			}
		}
	}

	private void log(String str) {
		Log.e(TAG, str);
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
			if (mMediaPlayer != null) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
				mHandler.removeMessages(HIDE_CONTROL_LAYOUT);
			}
//			finish();
			break;
		case KeyEvent.KEYCODE_VOLUME_UP:
			if(!mIsPushed) {
				showControlLayout();
				mRealSound = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
				if(mRealSound < mMaxSound) {
					mRealSound ++;
				}
			} else {
				// set the message to TV to increase the volume
				postSetVolume(VOLUME_UP);
				return true;
			}
//			mMuteView.setBackgroundResource(R.drawable.btn_media_volume);
//			mIsSilenced = false;
//			if (mIsPushed) {
//				postSetVolume((float) mRealSound / mMaxSound);
//			}
			break;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if(!mIsPushed) {
				showControlLayout();
				mRealSound = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
				if(mRealSound > 0) {
					mRealSound --;
				}
				if(mRealSound == 0) {
					Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					vib.vibrate(200);
//					mMuteView.setBackgroundResource(R.drawable.btn_media_mute);
//					mIsSilenced = true;
				}
			} else {
				// set the message to TV to decrease the volume
				postSetVolume(VOLUME_DOWN);
				return true;
			}
//			if (mIsPushed) {
//				postSetVolume((float) mRealSound / mMaxSound);
//			}
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
			if (mMediaPlayer != null) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
				mHandler.removeMessages(HIDE_CONTROL_LAYOUT);
				finish();
			}
			break;
		case R.id.video_view_push:
			getFriendlyNameOfRenderer();
			break;
		case R.id.video_view_prompt:
		case R.id.video_view_surface:
			if(mIsHideControlLayout) {
				showControlLayout();
			} else {
				hideControlLayout();
			}
			break;
//		case R.id.video_view_mute:
//			setMute();
//			break;
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
