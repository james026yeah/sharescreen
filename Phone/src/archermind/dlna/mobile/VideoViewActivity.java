package archermind.dlna.mobile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import archermind.dlna.media.VideoItem;
import archermind.dlna.verticalseekbar.VerticalSeekBar;

public class VideoViewActivity extends BaseActivity implements OnBufferingUpdateListener,
			OnCompletionListener, OnPreparedListener, SurfaceHolder.Callback {
	
	private static final String TAG = "VideoViewActivity";
	
	public static final String VIDEO_INDEX = "video_index";
	public static ArrayList<VideoItem> sVideoItemList = new ArrayList<VideoItem>();
	
	private static final int HIDE_CONTROL_LAYOUT = 0;
	private static final int PROGRESS_SEEKBAR_REFRESH = 1;
	private static final int HIDE_CONTROL_DEFAULT_TIME = 20 * 1000;
	
	private static final int VIDEO_MODE_DEFAULT = 0;
	private static final int VIDEO_MODE_PUSHED = 1;
	private static final int VIDEO_MODE_UNPUSHED = 2;
	
	private RelativeLayout mTopLayoutView;
	private RelativeLayout mBottomLayoutView;
	private RelativeLayout mSoundLayoutView;
	
	private LinearLayout mListView;
	private LinearLayout mPushView;
	private ImageView mSoundView;
	private ImageView mPrevView;
	private ImageView mStartView;
	private ImageView mPauseView;
	private ImageView mStopView;
	private ImageView mNextView;
	
	private TextView mNameView;
	private TextView mVideoMessageView;
	private TextView mCurrentTiemView;
	private TextView mAllTimeView;
	
	private SeekBar mProgressSeekBarView;
	private VerticalSeekBar mSoundSeekBarView;
	
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private MediaPlayer mMediaPlayer;
	private AudioManager mAudioManager;
	
	private String mVideoPath;
	private String mVideoName;
	
	private int mDuration;
	private int mCurrentProgress = 0;
	private int mCurrentSound = 30;
	private int mCurrentIndex;
	private int mVideoListMaxSize;
	private int mVideoMode = VIDEO_MODE_DEFAULT;
	
	private boolean mIsPaused = false;
	private boolean mIsReleased = false;
	private boolean mIsSilenced = false;
	private boolean mIsHideControlLayout = false;
	private boolean mIsPushed = false;
	
	private Handler mSeekBarHandler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mCurrentIndex = getIntent().getIntExtra(VIDEO_INDEX, 0);
		mVideoListMaxSize = sVideoItemList.size();
		
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		
		init();
	}
	
	private void initVideoView() {
		
		mTopLayoutView = (RelativeLayout) findViewById(R.id.video_view_top_layout);
		mTopLayoutView.getBackground().setAlpha(180);
		
		mBottomLayoutView = (RelativeLayout) findViewById(R.id.video_view_bottom_layout);
		mBottomLayoutView.getBackground().setAlpha(180);
		
		mSoundLayoutView = (RelativeLayout) findViewById(R.id.video_view_sound_layout);
		
		mVideoMessageView = (TextView) findViewById(R.id.video_view_message);
		mVideoMessageView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mIsHideControlLayout) {
					showControlLayout();
					mHandler.sendEmptyMessageDelayed(HIDE_CONTROL_LAYOUT, HIDE_CONTROL_DEFAULT_TIME);
				}
				else {
					hideControlLayout();
				}
			}
		});
		
		mSurfaceView = (SurfaceView) findViewById(R.id.video_view_surface);
		mSurfaceView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mIsHideControlLayout) {
					showControlLayout();
					mHandler.sendEmptyMessageDelayed(HIDE_CONTROL_LAYOUT, HIDE_CONTROL_DEFAULT_TIME);
				}
				else {
					hideControlLayout();
				}
			}
		});
		
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		
		mListView = (LinearLayout) findViewById(R.id.video_view_list);
		mListView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		mPushView = (LinearLayout) findViewById(R.id.video_view_push);
		mPushView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(!mIsPushed) {		
					postPlay(sVideoItemList.get(mCurrentIndex).getItemUri(), "videos");
//					mVideoMode = VIDEO_MODE_PUSHED;
					mIsPushed = true;
					init();
				}
				else {
					poststop();
					postGetPositionInfo();
				}
				mStartView.setVisibility(View.GONE);
				mPauseView.setVisibility(View.VISIBLE);
			}
		});
		
		mStartView = (ImageView) findViewById(R.id.video_view_start);
		mStartView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mIsPushed) {
					postPauseToPlay();
					mStartView.setVisibility(View.GONE);
					mPauseView.setVisibility(View.VISIBLE);
				}
				else {
					playVideo();
				}
			}
		});
		
		mPauseView = (ImageView) findViewById(R.id.video_view_pause);
		mPauseView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mIsPushed) {
					postpause();
					mPauseView.setVisibility(View.GONE);
					mStartView.setVisibility(View.VISIBLE);
				}
				else {
					pauseVideo();
				}
			}
		});
		
		mStopView = (ImageView) findViewById(R.id.video_view_stop);
		mStopView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mIsPushed) {
					poststop();
				}
				else {
					stopVideo();
				}
			}
		});
		
		mPrevView = (ImageView) findViewById(R.id.video_view_prev);
		mPrevView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mCurrentIndex == 0) {
					Toast.makeText(VideoViewActivity.this, "is the first video", Toast.LENGTH_SHORT).show();
				}
				else {
					mCurrentIndex--;
					if(mIsPushed) {
						postPrevious(sVideoItemList.get(mCurrentIndex).getItemUri(), "videos");
						mStartView.setVisibility(View.GONE);
						mPauseView.setVisibility(View.VISIBLE);
					}
					else {
						prevVideo();
					}
				}
			}
		});
		
		mNextView = (ImageView) findViewById(R.id.video_view_next);
		mNextView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(mCurrentIndex == mVideoListMaxSize - 1) {
					Toast.makeText(VideoViewActivity.this, "is the last video", Toast.LENGTH_SHORT).show();
				}
				else {
					mCurrentIndex++;
					if(mIsPaused) {
						postNext(sVideoItemList.get(mCurrentIndex).getItemUri(), "videos");
						mStartView.setVisibility(View.GONE);
						mPauseView.setVisibility(View.VISIBLE);
					}
					else {
						nextVideo();
					}
				}
			}
		});
		
		mSoundView = (ImageView) findViewById(R.id.video_view_sound);
		mSoundView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (mIsSilenced) {
					if (mIsPushed) {
						postGetMute();
					}
					mSoundView.setBackgroundResource(R.drawable.music_icon_sound);
					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentSound, 0);
					mSoundSeekBarView.setProgress(mCurrentSound);
					mIsSilenced = false;
				}
				else {
					if (mIsPushed) {
						postSetMute(0);
					}
					mSoundView.setBackgroundResource(R.drawable.music_icon_no_sound);
					mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
					mSoundSeekBarView.setProgress(0);
					mIsSilenced = true;
				}
			}
		});
		
		mNameView = (TextView) findViewById(R.id.video_view_name);
		mCurrentTiemView = (TextView) findViewById(R.id.video_view_current_time);
		mAllTimeView = (TextView) findViewById(R.id.video_view_all_time);
		
		mProgressSeekBarView = (SeekBar) findViewById(R.id.video_view_progress_seekbar);
		mProgressSeekBarView.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mStartView.setVisibility(View.GONE);
				mPauseView.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				mPauseView.setVisibility(View.GONE);
				mStartView.setVisibility(View.VISIBLE);
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				mCurrentProgress = progress;
				if(fromUser) {
					if(mIsPushed) {
						postSeek(mCurrentProgress + "");
					}
					else {
						mMediaPlayer.seekTo(progress);
					}
				}
			}
		});
		
		mSoundSeekBarView = (VerticalSeekBar) findViewById(R.id.video_view_sound_seekbar);
		mSoundSeekBarView.setOnSeekBarChangeListener(new VerticalSeekBar.OnSeekBarChangeListener() {
			
			public void onStopTrackingTouch(VerticalSeekBar Verticalseekbar) {
				
			}
			
			public void onStartTrackingTouch(VerticalSeekBar Verticalseekbar) {
				
			}
			
			public void onProgressChanged(VerticalSeekBar Verticalseekbar, int progress, boolean fromUser) {
				mCurrentSound = progress;
				if(fromUser) {
					if(mIsPushed) {
						postSetVolume(progress);
					}
					else {
						mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
					}
				}
			}
		});
		
	}
	
	@Override
	protected void onGetPositioninforesult(Map obj) {
		mCurrentProgress = (Integer) obj.get("relTime");
		log("mCurrentProgress = " + mCurrentProgress);
		mVideoMode = VIDEO_MODE_UNPUSHED;
		mIsPushed = false;
		init();
	}
	
	@Override
	public void onGetGetmuteresult(String obj) {
		mCurrentSound = Integer.parseInt(obj);
		log("mCurrentSound = " + mCurrentSound);
	}
	
	private void log(String str) {
		Log.e(TAG, str);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		// nothing to do
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		log("surfaceCreated is call");
		
		try {
			if(mIsPushed) {
//				mSurfaceView.setVisibility(View.GONE);
//				mVideoMessageView.setVisibility(View.VISIBLE);
			}
			mMediaPlayer.setDisplay(mSurfaceHolder);
			mMediaPlayer.prepare();
		}
		catch (IllegalStateException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// nothing to do
	}

	private void initMediaPlayer(String path) {
		try {
			
			if(mMediaPlayer != null) {
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
			
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setDataSource(path);
			
			mMediaPlayer.setOnBufferingUpdateListener(this);
			mMediaPlayer.setOnCompletionListener(this);
			mMediaPlayer.setOnPreparedListener(this);
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			
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
	
	private void initSeekBar() {
		mSeekBarHandler = new Handler() {
			public void handleMessage(Message msg) {
				switch (msg.what) {
					case PROGRESS_SEEKBAR_REFRESH:
						if (mMediaPlayer != null) {
							mCurrentProgress = mMediaPlayer.getCurrentPosition();
							mProgressSeekBarView.setProgress(mCurrentProgress);
							mCurrentTiemView.setText(LocalMediaActivity.setDurationFormat(mCurrentProgress));
							mSeekBarHandler.sendEmptyMessage(PROGRESS_SEEKBAR_REFRESH);
						}
						break;
				}
			}
		};
	}
	
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case HIDE_CONTROL_LAYOUT:
					hideControlLayout();
					break;
			}
		};
	};
	
	@Override
	public void onPrepared(MediaPlayer mp) {
		mDuration = mp.getDuration();
		mp.seekTo(mCurrentProgress);
		initSeekBar();
		setVideoInformation();
		mp.start();
		mHandler.sendEmptyMessageDelayed(HIDE_CONTROL_LAYOUT, HIDE_CONTROL_DEFAULT_TIME);
	}

	private void setVideoInformation() {
		mStartView.setVisibility(View.GONE);
		mPauseView.setVisibility(View.VISIBLE);
		
		mVideoName = sVideoItemList.get(mCurrentIndex).getTitle();
		mNameView.setText(mVideoName);
		if(!mIsPushed) {
			mVideoMessageView.setText(mVideoName + " 正在XXX上播放");
		}
		
		mProgressSeekBarView.setMax(mDuration);
		mSeekBarHandler.sendEmptyMessage(PROGRESS_SEEKBAR_REFRESH);
		
		mCurrentTiemView.setText(LocalMediaActivity.setDurationFormat(mCurrentProgress));
		mAllTimeView.setText(LocalMediaActivity.setDurationFormat(mDuration));
		
		int maxSound = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mSoundSeekBarView.setMax(maxSound);
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentSound, 0);
		mSoundSeekBarView.setProgress(mCurrentSound);
	}
	
	@Override
	public void onCompletion(MediaPlayer mp) {
		log("onCompletion is call");
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		log("onBufferingUpdate is call");
	}

	private void playVideo() {
		mStartView.setVisibility(View.GONE);
		mPauseView.setVisibility(View.VISIBLE);
		mMediaPlayer.start();
		mIsPaused = false;
	}
	
	private void pauseVideo() {
		if (mMediaPlayer != null) {
			if (mIsReleased == false) {
				if (mIsPaused == false) {
					mMediaPlayer.pause();
					mPauseView.setVisibility(View.GONE);
					mStartView.setVisibility(View.VISIBLE);
					mIsPaused = true;
				}
			}
		}
	}
	
	private void stopVideo() {
		if (mMediaPlayer != null) {
			if (mIsReleased == false) {
				mPauseView.setVisibility(View.GONE);
				mStartView.setVisibility(View.VISIBLE);
				mProgressSeekBarView.setProgress(0);
				mSeekBarHandler.removeMessages(PROGRESS_SEEKBAR_REFRESH);
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mIsReleased = true;
			}
		}
	}

	private void init() {
		setContentView(R.layout.local_media_video_view);
		initVideoView();
		mVideoPath = sVideoItemList.get(mCurrentIndex).getFilePath();
		initMediaPlayer(mVideoPath);
	}
	
	private void prevVideo() {
		init();
	}
	
	private void nextVideo() {
		init();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode) {
			case KeyEvent.KEYCODE_BACK:
				if (mMediaPlayer != null) {
					mMediaPlayer.reset();
					mMediaPlayer.release();
					mMediaPlayer = null;
				}
				break;
		}
		mHandler = null;
		return true;
	}
	
	private void showControlLayout() {
		mIsHideControlLayout = false;
		mTopLayoutView.setVisibility(View.VISIBLE);
		mBottomLayoutView.setVisibility(View.VISIBLE);
		mSoundLayoutView.setVisibility(View.VISIBLE);
	}
	
	private void hideControlLayout() {
		mIsHideControlLayout = true;
		mTopLayoutView.setVisibility(View.GONE);
		mBottomLayoutView.setVisibility(View.GONE);
		mSoundLayoutView.setVisibility(View.GONE);
	}

}
