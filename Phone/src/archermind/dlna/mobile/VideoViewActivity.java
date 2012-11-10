package archermind.dlna.mobile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import archermind.dlna.media.VideoItem;
import archermind.dlna.verticalseekbar.VerticalSeekBar;

@SuppressLint({ "HandlerLeak", "HandlerLeak"
})
public class VideoViewActivity extends BaseActivity implements OnClickListener {
	
	private static final String TAG = "VideoViewActivity";
	
	public static final String VIDEO_INDEX = "video_index";
	public static ArrayList<VideoItem> sVideoItemList = new ArrayList<VideoItem>();
	
	private static final int TIME_SECOND = 1000;
	private static final int TIME_MINUTE = TIME_SECOND * 60;
	private static final int TIME_HOUR = TIME_MINUTE * 60; 
	
	private static final int HIDE_CONTROL_LAYOUT = 0;
	private static final int PROGRESS_SEEKBAR_REFRESH = 1;
	private static final int HIDE_CONTROL_DEFAULT_TIME = 20 * 1000;
	
	private static final int TIMER_INTERVAL_TIME = 500;
	private static final int START_TASK = 1;
	private static final int END_TASK = 2;
	
	private RelativeLayout mTopLayout;
	private RelativeLayout mBottomLayout;
	private RelativeLayout mSoundLayout;
	
	private RelativeLayout mBackView;
	private RelativeLayout mPushView;
	private ImageView mMuteView;
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
	private VerticalSeekBar mSoundSeekBar;
	
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
	private int mRealSound = 20;
	private int mCurrentIndex;
	private int mVideoListMaxSize;
	
	private boolean mIsPaused = false;
	private boolean mIsReleased = false;
	private boolean mIsSilenced = false;
	private boolean mIsHideControlLayout = false;
	private boolean mIsPushed = false;
	private boolean mIsTimeSeekBarTouched = false;
	
	private Handler mTimeSeekBarHandler;
	private Timer mTimer;
	private QueryStateTask mQueryStateTask;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mCurrentIndex = getIntent().getIntExtra(VIDEO_INDEX, 0);
		mVideoListMaxSize = sVideoItemList.size();
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		
		getVideoName();
		getVideoPath();
		
		log("mCurrentIndex = " + mCurrentIndex);
		log("mVideoPath = " + mVideoPath);
		log("mVideoName = " + mVideoName);
		
		initUnPushedUI();
	}
	
	private void initVideoView() {
		
		mTopLayout = (RelativeLayout) findViewById(R.id.video_view_top_layout);
		mBottomLayout = (RelativeLayout) findViewById(R.id.video_view_bottom_layout);
		mSoundLayout = (RelativeLayout) findViewById(R.id.video_view_sound_layout);
		mTopLayout.getBackground().setAlpha(180);
		mBottomLayout.getBackground().setAlpha(180);
		
		mPromptView = (TextView) findViewById(R.id.video_view_prompt);
		mBackView = (RelativeLayout) findViewById(R.id.video_view_back);
		mNameView = (TextView) findViewById(R.id.video_view_name);
		mPushView = (RelativeLayout) findViewById(R.id.video_view_push);
		mPrevView = (ImageView) findViewById(R.id.video_view_prev);
		mPlayView = (ImageView) findViewById(R.id.video_view_play);
		mPauseView = (ImageView) findViewById(R.id.video_view_pause);
		mStopView = (ImageView) findViewById(R.id.video_view_stop);
		mNextView = (ImageView) findViewById(R.id.video_view_next);
		mMuteView = (ImageView) findViewById(R.id.video_view_mute);
		
		mTopLayout.setOnClickListener(this);
		mBottomLayout.setOnClickListener(this);
		mSoundLayout.setOnClickListener(this);
		mPromptView.setOnClickListener(this);
		mBackView.setOnClickListener(this);
		mPushView.setOnClickListener(this);
		mPrevView.setOnClickListener(this);
		mPlayView.setOnClickListener(this);
		mPauseView.setOnClickListener(this);
		mStopView.setOnClickListener(this);
		mNextView.setOnClickListener(this);
		mMuteView.setOnClickListener(this);
		
		mRealTimeView = (TextView) findViewById(R.id.video_view_current_time);
		mAllTimeView = (TextView) findViewById(R.id.video_view_all_time);
		
		mTimeSeekBar = (SeekBar) findViewById(R.id.video_view_progress_seekbar);
		mTimeSeekBar.setOnSeekBarChangeListener(timeSeekBarListener);
		
		mSoundSeekBar = (VerticalSeekBar) findViewById(R.id.video_view_sound_seekbar);
		mSoundSeekBar.setOnSeekBarChangeListener(soundSeekBarListener);
		
	}
	
	private void initMediaPlayer() {
		try {
			if(mMediaPlayer != null) {
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
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (IllegalStateException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
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
				log("surfaceCreated is call");
				initMediaPlayer();
				mMediaPlayer.setDisplay(mSurfaceHolder);
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				
			}
			
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				
			}
			
		});
	}
	
	OnSeekBarChangeListener timeSeekBarListener = new OnSeekBarChangeListener() {
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			if (mIsPushed) {
				log("mRealTime = " + mRealTime);
				postSeek(mRealTime + "");
				mIsTimeSeekBarTouched = false;
			}
			else {
				mMediaPlayer.seekTo(mRealTime);
				mMediaPlayer.start();
			}
			mPlayView.setVisibility(View.GONE);
			mPauseView.setVisibility(View.VISIBLE);
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			if(!mIsPushed) {
				mMediaPlayer.pause();
			}
			else {
				mIsTimeSeekBarTouched = true;
			}
			mPauseView.setVisibility(View.GONE);
			mPlayView.setVisibility(View.VISIBLE);
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			mRealTime = progress;
		}
	};
	
	VerticalSeekBar.OnSeekBarChangeListener soundSeekBarListener = new VerticalSeekBar.OnSeekBarChangeListener() {
		
		@Override
		public void onStopTrackingTouch(VerticalSeekBar Verticalseekbar) {
			if(mIsPushed) {
				if(mIsSilenced) {
					postSetMute(0.0f);
				}
				else {
					float h = mRealSound;
					postSetVolume(h/100);
				}
			}
		}
		
		@Override
		public void onStartTrackingTouch(VerticalSeekBar Verticalseekbar) {
			
		}
		
		@Override
		public void onProgressChanged(VerticalSeekBar Verticalseekbar, int progress, boolean fromUser) {
			mRealSound = progress;
			if(!mIsPushed) {
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mRealSound, 0);
			}
			if(mRealSound == 0) {
				mMuteView.setBackgroundResource(R.drawable.music_icon_no_sound);
				mIsSilenced = true;
			}
			else {
				mMuteView.setBackgroundResource(R.drawable.music_icon_sound);
				mIsSilenced = false;
			}
		}
	};
	
	OnPreparedListener preparedListener = new OnPreparedListener() {
		
		@Override
		public void onPrepared(MediaPlayer mp) {
			log("onPrepared is call");
			
			getVideoName();
			mNameView.setText(mVideoName);
			
			log("prepare mRealTime = " + mRealTime);
			
			mp.seekTo(mRealTime);
			
			mDuration = mp.getDuration();
			mTimeSeekBar.setMax(mDuration);
			mAllTimeView.setText(setDurationToTime(mDuration));
			mRealTimeView.setText(setDurationToTime(mRealTime));
			
			initTimeSeekBar();
			mTimeSeekBarHandler.sendEmptyMessage(PROGRESS_SEEKBAR_REFRESH);
			
			mMaxSound = 100;
			mSoundSeekBar.setMax(mMaxSound);
			mSoundSeekBar.setProgress(mRealSound);
			
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mRealSound, 0);
			
			mp.start();
			
			mHandler.sendEmptyMessageDelayed(HIDE_CONTROL_LAYOUT, HIDE_CONTROL_DEFAULT_TIME);
			
			mPlayView.setVisibility(View.GONE);
			mPauseView.setVisibility(View.VISIBLE);
		}
	};
	
	OnCompletionListener completionListener = new OnCompletionListener() {
		
		@Override
		public void onCompletion(MediaPlayer mp) {
			log("onCompletion is call");
			mPlayView.setVisibility(View.VISIBLE);
			mPauseView.setVisibility(View.GONE);
			mMediaPlayer.release();
			mIsReleased = true;
			mMediaPlayer = null;
			mSurfaceView.destroyDrawingCache();
			mTimeSeekBar.setProgress(0);
			mNameView.setText("");
			mPromptView.setVisibility(View.VISIBLE);
			mPromptView.setText(getResources().getString(R.string.video_play_end_prompt_message));
		}
	};
	
	OnBufferingUpdateListener bufferingUpdateListener = new OnBufferingUpdateListener() {
		
		@Override
		public void onBufferingUpdate(MediaPlayer mp, int percent) {
			
		}
	};
	
	private void initTimeSeekBar() {
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
	
	private void getVideoName() {
		mVideoName = sVideoItemList.get(mCurrentIndex).getTitle();
	}
	
	private void getVideoPath() {
		mVideoPath = sVideoItemList.get(mCurrentIndex).getFilePath();
	}
	
	private void showPromptView() {
		getVideoName();
		mPromptView.setVisibility(View.VISIBLE);
		mPromptView.setText(mVideoName + "  " + getResources().getString(R.string.video_prompt_prev_message) 
				+ "  " + mDeviceName + "  " + getResources().getString(R.string.video_prompt_next_message));
	}
	
	private void hidePromptView() {
		if(mPromptView.getVisibility() == View.VISIBLE) {
			mPromptView.setText("");
			mPromptView.setVisibility(View.GONE);
		}
	}
	
	private void initUnPushedUI() {
		log("initUnPushedUI is call");
		mIsPaused = false;
		mIsReleased = false;
		setContentView(R.layout.local_media_video_view);
		initVideoView();
		initSurfaceView();
		hidePromptView();
	}
	
	private void initPushedOutUI() {
		log("initPushedOutUI is call");
		setContentView(R.layout.local_media_video_view);
		initVideoView();
		getVideoName();
		mNameView.setText(mVideoName);
		mMaxSound = 100;
		mSoundSeekBar.setMax(mMaxSound);
		mPlayView.setVisibility(View.GONE);
		mPauseView.setVisibility(View.VISIBLE);
		showPromptView();
	}
	
	private void showOrHideControlLayout() {
		if (mIsHideControlLayout) {
			showControlLayout();
			mHandler.sendEmptyMessageDelayed(HIDE_CONTROL_LAYOUT, HIDE_CONTROL_DEFAULT_TIME);
		}
		else {
			hideControlLayout();
		}
	}
	
	private void setMute() {
		if (mIsSilenced) {
			if (mIsPushed) {
				postGetMute();
			}
			else {
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mRealSound, 0);
			}
			mMuteView.setBackgroundResource(R.drawable.music_icon_sound);
			mSoundSeekBar.setProgress(mRealSound);
			mIsSilenced = false;
		}
		else {
			if (mIsPushed) {
				postSetMute(0.0f);
			}
			else {
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
			}
			mMuteView.setBackgroundResource(R.drawable.music_icon_no_sound);
			mSoundSeekBar.setProgress(mRealSound);
			mIsSilenced = true;
		}
	}
	
	private void pushVideo() {
		if(!mIsPushed) {
			log("push out to TV");
			postPlay(sVideoItemList.get(mCurrentIndex).getItemUri(), VIDEO_TYPE);
		}
		else {
			log("push back to phone");
			mIsPushed = false;
			cancelTask();
			postGetPositionInfo();
			poststop();
		}
	}
	
	@Override
	public void onGetPlayresult(Boolean obj) {
		log("onGetPlayresult is call");
		if(obj) {
			mIsPushed = true;
			if(mMediaPlayer != null) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
				mIsReleased = true;
			}
			initPushedOutUI();
			mTimer = new Timer();
			mQueryStateTask = new QueryStateTask();
			mTimer.schedule(mQueryStateTask, TIMER_INTERVAL_TIME, TIMER_INTERVAL_TIME);
			postGetMute();
			postGetVolume();
		}
		else {
			log("onGetPlayresult is null");
		}
	}
	
	private class QueryStateTask extends TimerTask {

		@Override
		public void run() {
			try {
//				log("QueryStateTask is call");
				mHandler.sendEmptyMessage(START_TASK);
			} catch (Exception e) {
				mHandler.sendEmptyMessage(END_TASK);
				e.printStackTrace();
			}
		}
	}
	
	private void cancelTask() {
		if(mTimer != null) {
			mTimer.cancel();
		}
		if(mQueryStateTask != null) {
			mQueryStateTask.cancel();
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void onGettransinforesult(Map obj) {
//		log("onGettransinforesult is call");
		if(obj != null) {
			String state = (String) obj.get("state");
			String statu = (String) obj.get("statu");
			String speed = (String) obj.get("speed");
//			log("state = " + state + "  statu = " + statu + "  speed = " + speed);
		}
		else {
			log("onGettransinforesult data is null");
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void onGetPositioninforesult(Map obj) {
//		log("onGetPositioninforesult is call");
		if(obj != null) {
			String duration = (String) obj.get("trackDuration");
			String realTime = (String) obj.get("relTime");
			
			log("realTime = " + realTime);
//			log("duration = " + duration + "  realTime = " + realTime);
			
			int progressTV = setTimeToDuration(realTime);
			int durationTV = setTimeToDuration(duration);
			
//			log("progressTV = " + progressTV + "  durationTV = " + durationTV);
			
			if(!mIsPushed) {
				mRealTime = progressTV;
				initUnPushedUI();
				return;
			}
			
			if(mAllTimeView != null) {
				if(!mIsTimeSeekBarTouched) {
					mTimeSeekBar.setMax(durationTV);
					mTimeSeekBar.setProgress(progressTV);
					mAllTimeView.setText(duration);
					mRealTimeView.setText(realTime);
				}
			}
			
			if(durationTV == progressTV) {
				cancelTask();
				mPromptView.setText(getResources().getString(R.string.video_play_end_prompt_message));
			}
		}
		else {
			log("onGetPositioninforesult data is null");
		}
	}
	
	@Override
	public void onGetGetmuteresult(String obj) {
		if(obj != null) {
			mIsSilenced = Boolean.parseBoolean(obj);
			log("mIsSilenced = " + mIsSilenced);
			if(mIsSilenced) {
				mMuteView.setBackgroundResource(R.drawable.music_icon_no_sound);
			}
			else {
				mMuteView.setBackgroundResource(R.drawable.music_icon_sound);
			}
		}
		else {
			log("onGetGetmuteresult is null");
		}
	}
	
	@Override
	public void onGetGetvolumeresult(String obj) {
		if(obj != null) {
			mRealSound = (int) (Float.parseFloat(obj) * 100);
			log("mRealSound = " + mRealSound);
			mSoundSeekBar.setProgress(mRealSound);
			if(mRealSound == 0) {
				mMuteView.setBackgroundResource(R.drawable.music_icon_no_sound);
			}
			else {
				mMuteView.setBackgroundResource(R.drawable.music_icon_sound);
			}
		}
		else {
			log("onGetGetvolumeresult is null");
		}
	}
	
	private void prevVideo() {
		log("prev mIsPushed = " + mIsPushed);
		if(mCurrentIndex == 0) {
			Toast.makeText(VideoViewActivity.this, R.string.video_first_message, Toast.LENGTH_SHORT).show();
		}
		else {
			mCurrentIndex--;
			if(mIsPushed) {
				log("prev video when is pushed");
				cancelTask();
				postPrevious(sVideoItemList.get(mCurrentIndex).getItemUri(), VIDEO_TYPE);
				initPushedOutUI();
				mTimer = new Timer();
				mQueryStateTask = new QueryStateTask();
				mTimer.schedule(mQueryStateTask, TIMER_INTERVAL_TIME, TIMER_INTERVAL_TIME);
				postGetMute();
				postGetVolume();
			}
			else {
				initUnPushedUI();
			}
		}
	}
	
	private void playVideo() {
		if(mIsPushed) {
			postPauseToPlay();
			mPlayView.setVisibility(View.GONE);
			mPauseView.setVisibility(View.VISIBLE);
		}
		else {
			if(mIsReleased) {
				mIsReleased = false;
				initUnPushedUI();
			}
			else {
				mIsPaused = false;
				mPlayView.setVisibility(View.GONE);
				mPauseView.setVisibility(View.VISIBLE);
				mMediaPlayer.start();
			}
		}
	}
	
	private void pauseVideo() {
		if(mIsPushed) {
			postpause();
			mPauseView.setVisibility(View.GONE);
			mPlayView.setVisibility(View.VISIBLE);
		}
		else {
			if (mMediaPlayer != null) {
				if (mIsReleased == false) {
					if (mIsPaused == false) {
						log("1111111111111");
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
		if(mIsPushed) {
			poststop();
		}
		else {
			if (mMediaPlayer != null) {
				if (mIsReleased == false) {
					log("222222222222222");
					mPauseView.setVisibility(View.GONE);
					mPlayView.setVisibility(View.VISIBLE);
					mTimeSeekBar.setProgress(0);
					mTimeSeekBarHandler.removeMessages(PROGRESS_SEEKBAR_REFRESH);
					mMediaPlayer.stop();
					mMediaPlayer.release();
					mIsReleased = true;
				}
			}
		}
	}
	
	private void nextVideo() {
		log("next mIsPushed = " + mIsPushed);
		if(mCurrentIndex == mVideoListMaxSize - 1) {
			Toast.makeText(VideoViewActivity.this, R.string.video_last_message, Toast.LENGTH_SHORT).show();
		}
		else {
			mCurrentIndex++;
			if(mIsPushed) {
				log("next video when is pushed");
				cancelTask();
				postNext(sVideoItemList.get(mCurrentIndex).getItemUri(), VIDEO_TYPE);
				initPushedOutUI();
				mTimer = new Timer();
				mQueryStateTask = new QueryStateTask();
				mTimer.schedule(mQueryStateTask, TIMER_INTERVAL_TIME, TIMER_INTERVAL_TIME);
				postGetMute();
				postGetVolume();
			}
			else {
				initUnPushedUI();
			}
		}
	}
	
	private void log(String str) {
		Log.e(TAG, str);
	}

	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case HIDE_CONTROL_LAYOUT:
					hideControlLayout();
					break;
				case START_TASK:
					postGetTransportInfo();
					postGetPositionInfo();
					break;
				case END_TASK:
					cancelTask();
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
		switch(keyCode) {
			case KeyEvent.KEYCODE_BACK:
				if (mMediaPlayer != null) {
					mMediaPlayer.stop();
					mMediaPlayer.release();
					mMediaPlayer = null;
					mHandler.removeMessages(HIDE_CONTROL_LAYOUT);
				}
				cancelTask();
				poststop();
				finish();
				break;
		}
		return true;
	}
	
	private void showControlLayout() {
		mIsHideControlLayout = false;
		mTopLayout.setVisibility(View.VISIBLE);
		mBottomLayout.setVisibility(View.VISIBLE);
		mSoundLayout.setVisibility(View.VISIBLE);
	}
	
	private void hideControlLayout() {
		mIsHideControlLayout = true;
		mTopLayout.setVisibility(View.GONE);
		mBottomLayout.setVisibility(View.GONE);
		mSoundLayout.setVisibility(View.GONE);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.video_view_top_layout:
			case R.id.video_view_bottom_layout:
			case R.id.video_view_sound_layout:
				showControlLayout();
				break;
			case R.id.video_view_back:
				finish();
				break;
			case R.id.video_view_push:
				pushVideo();
				break;
			case R.id.video_view_prompt:
			case R.id.video_view_surface:
				showOrHideControlLayout();
				break;
			case R.id.video_view_mute:
				setMute();
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

}
