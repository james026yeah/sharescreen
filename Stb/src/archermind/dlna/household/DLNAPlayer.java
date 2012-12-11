package archermind.dlna.household;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import archermind.airplay.AirplayProcess;
import archermind.ashare.R;
import archermind.dlna.media.MediaItem;
import archermind.dlna.media.MusicItem;
import archermind.dlna.media.VideoItem;

import com.archermind.ashare.TypeDefs;

public class DLNAPlayer extends Activity {
	private static final String TAG = "DLNAPlayer";
	private int mVideoWidth;
	private int mVideoHeight;
	private SurfaceView mSurfaceView;
	private android.media.MediaPlayer mMediaPlayer;
	private android.widget.VideoView mVPlayer;
	private SurfaceHolder holder;
	private String mUri;
	private ImageView mStartView;
	private ImageView mPauseView;
	private SeekBar mSeekBar;
	private TextView mCurrentTimeView;
	private TextView mTotalTimeView;
	private TextView mVideoName;
	private TextView mMusicName;
	private TextView mAuthorName;
	private RelativeLayout mVideoTopLayout;
	private RelativeLayout mVideoBottomLayout;
	private RelativeLayout mMusicBottomLayout;
	private RelativeLayout mVideoStartPauseLayout;
	private Handler mDismissHandler;
	private ImageView mCdView;
	private ImageView mArmartImageView;
	private Animation hideAnimation;
	private AnimationDrawable mCdAnim = null;
	private final Handler cdHandler = new Handler();
	private final static int DEFAULT_MAX = 1000;
	private final static String DEFAULT_VIDEO_NAME = "Video";
	private final static String DEFAULT_MUSIC_NAME = "Music";
	private final static String DEFAULT_AUTHOR_NAME = "";
	private final static float DEFAULE_SCALE = 1920 / 1080l;
	private final static int HIDE_CONTROL_LAYOUT = 100;
	private final static int DEFAULT_HIDE_TIME = 4000;
	private final float UP_VOLUME = 1;
	private final float DOWN_VOLUME = -1;
	private int mMediaType;
	private MusicItem mCurrAudioInfo;
	private VideoItem mVideoInfo;
	private AudioManager mAudioManager;
	private float mMaxVolume = 0;

	public static boolean mIsPlay = false;
	public static boolean mIsPause = false;
	public static float mVolume = 0.5f;
	public static long mTotalTime = 0;
	public static long mCurrentPosition = 0;
	public static boolean mIsPlayCompletion = true;

	public static final int TIME_SECOND = 1000;
	public static final int TIME_MINUTE = TIME_SECOND * 60;
	public static final int TIME_HOUR = TIME_MINUTE * 60;

	private boolean mIsBound = false;
	private Messenger mService = null;
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case AirplayProcess.MSG_AIRPLAY_PLAY_TO_PAUSE:
			case TypeDefs.MSG_DMR_AV_TRANS_PLAY_TO_PAUSE:
				pause();
				break;
			case AirplayProcess.MSG_AIRPLAY_SEEK:
			case TypeDefs.MSG_DMR_AV_TRANS_SEEK:
				if (msg.arg2 == TypeDefs.MEDIA_TYPE_DLNA_IMAGE)
					break;
				seek(msg.arg1);
				break;
			case AirplayProcess.MSG_AIRPLAY_STOP:
			case TypeDefs.MSG_DMR_AV_TRANS_STOP:
				stop();
				finish();
				break;
			case TypeDefs.MSG_DMR_AV_TRANS_SET_VOLUME:
				setvolume(Float.parseFloat(msg.obj.toString()));
				if (mAudioManager != null) {
					mVolume = mAudioManager
							.getStreamVolume(AudioManager.STREAM_MUSIC);
				}
				break;
			case AirplayProcess.MSG_AIRPLAY_PAUSE_TO_PLAY:
			case TypeDefs.MSG_DMR_AV_TRANS_PAUSE_TO_PLAY:
				if (DLNAPlayer.mIsPlayCompletion)
					break;
				resume();
				break;
			case AirplayProcess.MSG_AIRPLAY_UPDATEDATA:
			case TypeDefs.MSG_DMR_RENDERER_UPDATEDATA:
				Updatedata();
				break;
			case TypeDefs.MSG_DMR_AV_TRANS_SET_MUTE:
				setvolume((float) msg.arg1);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private ServiceConnection mServConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = new Messenger(service);
			try {
				Message msg = Message.obtain(null,
						RendererService.MSG_REGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	};

	private void bind2RendererService() {
		mIsBound = bindService(new Intent(DLNAPlayer.this,
				RendererService.class), mServConn, BIND_AUTO_CREATE);
	}

	private void unbind2RendererService() {
		if (mIsBound && (mService != null)) {
			try {
				Message msg = Message.obtain(null,
						RendererService.MSG_UNREGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
			}
			unbindService(mServConn);
			mIsBound = false;
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		Bundle data = getIntent().getExtras();
		if (null != data) {
			mUri = data.getString(TypeDefs.KEY_MEDIA_URI);
			Log.d(TAG, "Media URI:" + mUri);
			if(mUri.isEmpty())
			{
				finish();
				return;
			}
			mMediaType = data.getInt(TypeDefs.KEY_MEDIA_TYPE);
			if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_AUDIO) {
				mCurrAudioInfo = data.getParcelable(TypeDefs.KEY_CURR_MEDIA_INFO);
				setContentView(R.layout.music_player);
				mStartView = (ImageView) findViewById(R.id.music_view_start);
				mPauseView = (ImageView) findViewById(R.id.music_view_pause);
				mSeekBar = (SeekBar) findViewById(R.id.music_view_seekbar);
				mCurrentTimeView = (TextView) findViewById(R.id.music_view_current_time);
				mTotalTimeView = (TextView) findViewById(R.id.music_view_all_time);
				mMusicName = (TextView) findViewById(R.id.music_view_name);
				mAuthorName = (TextView) findViewById(R.id.music_view_author);
				mMusicBottomLayout = (RelativeLayout) findViewById(R.id.music_view_bottom_layout);
				mCdView = (ImageView) findViewById(R.id.music_album);
				mCdAnim = (AnimationDrawable) mCdView.getBackground();
				mArmartImageView = (ImageView) findViewById(R.id.music_album_right);
			} else {
				mVideoInfo = data.getParcelable(TypeDefs.KEY_CURR_MEDIA_INFO);
				requestWindowFeature(Window.FEATURE_NO_TITLE);
				getWindow().setFlags(
						WindowManager.LayoutParams.FLAG_FULLSCREEN,
						WindowManager.LayoutParams.FLAG_FULLSCREEN);
				setContentView(R.layout.video_player);
				mSurfaceView = (SurfaceView) findViewById(R.id.video_view_surface_dlna);
				mStartView = (ImageView) findViewById(R.id.video_view_start);
				mPauseView = (ImageView) findViewById(R.id.video_view_pause);
				mSeekBar = (SeekBar) findViewById(R.id.video_view_seekbar);
				mCurrentTimeView = (TextView) findViewById(R.id.video_view_current_time);
				mTotalTimeView = (TextView) findViewById(R.id.video_view_all_time);
				mVideoName = (TextView) findViewById(R.id.video_view_name);
				mVideoTopLayout = (RelativeLayout) findViewById(R.id.video_view_top_layout);
				mVideoBottomLayout = (RelativeLayout) findViewById(R.id.video_view_bottom_layout);
				mVideoStartPauseLayout = (RelativeLayout) findViewById(R.id.video_start_pause_layout);
			}
			mSeekBar.setMax(DEFAULT_MAX);
			if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_AUDIO) {
				mMediaPlayer = new android.media.MediaPlayer();
			} else if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_VIDEO
					|| mMediaType == TypeDefs.MEDIA_TYPE_AIRPLAY_VIDEO) {
				mVPlayer = (android.widget.VideoView) findViewById(R.id.surface_view_p);
			}
			mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
			mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			play(mUri);
			initInfo();
		}
		bind2RendererService();
	}

	private OnCompletionListener mAudioCompletionListener = new OnCompletionListener() {
		public void onCompletion(android.media.MediaPlayer mp) {
			stop();
			finish();
		}
	};

	private OnCompletionListener mVideoCompletionListener = new OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			stop();
			finish();
		}
	};
	
	private OnErrorListener mAudioErrorListener = new OnErrorListener(){

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			// TODO Auto-generated method stub
			if (mMediaPlayer != null) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
			mIsPlayCompletion = true;
			finish();
			return false;
		}

	};
	
	private OnErrorListener mVideoErrorListener = new OnErrorListener(){

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			// TODO Auto-generated method stub
			mVPlayer.stopPlayback();
			mIsPlayCompletion = true;
			finish();
			return false;
		}
		
	};
	
	private OnPreparedListener mAudioPreparedListener = new OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer mp) {
			// TODO Auto-generated method stub
			mp.start();
			mIsPlayCompletion = false;
		}
	};
	
	private OnPreparedListener mVideoPreparedListener = new OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer mp) {
			// TODO Auto-generated method stub
			mVPlayer.start();
			mIsPlayCompletion = false;
		}
		
	};

	public void play(String url) {
		if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_AUDIO) {
			try {
				mMediaPlayer.reset();
				mMediaPlayer.setDataSource(url);
				mMediaPlayer.setOnCompletionListener(mAudioCompletionListener);
				mMediaPlayer.setOnPreparedListener(mAudioPreparedListener);
				mMediaPlayer.setOnErrorListener(mAudioErrorListener);
				mMediaPlayer.prepare();
				//mMediaPlayer.start();
				
			} catch (IOException ex) {
				Log.v(TAG, "Unable to open content: " + url);
				return;
			}
		} else if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_VIDEO
				|| mMediaType == TypeDefs.MEDIA_TYPE_AIRPLAY_VIDEO) {
			mVPlayer.setVideoURI(Uri.parse(url));
			mVPlayer.setOnCompletionListener(mVideoCompletionListener);
			mVPlayer.setOnErrorListener(mVideoErrorListener);
			mVPlayer.setOnPreparedListener(mVideoPreparedListener);
		}
	}

	public void stop() {
		mIsPlayCompletion = true;
		if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_AUDIO) {
			if (mMediaPlayer != null) {
				mMediaPlayer.stop();
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
		} else if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_VIDEO
				|| mMediaType == TypeDefs.MEDIA_TYPE_AIRPLAY_VIDEO) {
			mVPlayer.stopPlayback();
		}
		mIsPause = false;
		mIsPlay = false;
	}

	public void seek(int position) {
		if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_AUDIO) {
			if (mMediaPlayer != null) {
				mMediaPlayer.seekTo(position);
			}

		} else if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_VIDEO
				|| mMediaType == TypeDefs.MEDIA_TYPE_AIRPLAY_VIDEO) {
			mVPlayer.seekTo(position);
		}
		show();
	}

	public void pause() {
		mStartView.setVisibility(View.VISIBLE);
		mPauseView.setVisibility(View.GONE);
		show();
		if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_AUDIO) {
			if (mMediaPlayer != null) {
				mMediaPlayer.pause();
			}
		} else if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_VIDEO
				|| mMediaType == TypeDefs.MEDIA_TYPE_AIRPLAY_VIDEO) {
			mVPlayer.pause();
		}
		mIsPause = true;
		mIsPlay = false;
	}

	public void setvolume(float volume) {
		if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_AUDIO) {
			if (mMediaPlayer != null) {

			}
		} else if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_VIDEO
				|| mMediaType == TypeDefs.MEDIA_TYPE_AIRPLAY_VIDEO) {

		}
		if (mAudioManager != null) {
			float curvolume;
			mMaxVolume = mAudioManager
					.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			curvolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			curvolume += volume;
			if(curvolume < 0)
				curvolume = 0;
			else if(curvolume > mMaxVolume)
				curvolume = mMaxVolume;
			mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
					(int) curvolume, 0);
		}
	}

	public void resume() {
		mStartView.setVisibility(View.GONE);
		mPauseView.setVisibility(View.VISIBLE);
		show();
		if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_AUDIO) {
			if (mMediaPlayer != null) {
				mMediaPlayer.start();
			}
		} else if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_VIDEO
				|| mMediaType == TypeDefs.MEDIA_TYPE_AIRPLAY_VIDEO) {
			mVPlayer.start();
		}

		mIsPlay = true;
		mIsPause = false;
	}

	public void onDestroy() {
		mVideoWidth = 0;
		mVideoHeight = 0;
		mIsPlay = false;
		mIsPause = false;
		mTotalTime = 0;
		mCurrentPosition = 0;
		stopCd();
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
		unbind2RendererService();
	}

	public void Updatedata() {
		if (mIsPlayCompletion)
			return;
		if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_AUDIO) {
			if (mMediaPlayer != null) {
				mVideoWidth = mMediaPlayer.getVideoWidth();
				mVideoHeight = mMediaPlayer.getVideoHeight();
				mIsPlay = mMediaPlayer.isPlaying();
				mTotalTime = mMediaPlayer.getDuration();
				mCurrentPosition = mMediaPlayer.getCurrentPosition();
			}
		} else if (mMediaType == TypeDefs.MEDIA_TYPE_DLNA_VIDEO
				|| mMediaType == TypeDefs.MEDIA_TYPE_AIRPLAY_VIDEO) {
			mVideoWidth = mVPlayer.getWidth();
			mVideoHeight = mVPlayer.getHeight();
			mIsPlay = mVPlayer.isPlaying();
			mTotalTime = mVPlayer.getDuration();
			mCurrentPosition = mVPlayer.getCurrentPosition();
		}
		updateViews();
	}

	public String getFileName(String mUri) {
		int start = mUri.lastIndexOf("/");
		int end = mUri.lastIndexOf(".");
		if (start != -1 && end != -1) {
			return mUri.substring(start + 1, end);
		} else {
			return null;
		}
	}

	public static long fromDateStringToInt(String inVal) {
		String[] timeSlice = inVal.split(":");
		long temp = 0;
		for (int i = 0; i < timeSlice.length; i++) {
			temp = temp * 60 + Integer.parseInt(timeSlice[i]);
		}
		return temp * 1000;
	}

	public static String fromIntToDateString(long inVal) {
		long hour = inVal / TIME_HOUR;
		long minute = (inVal - hour * TIME_HOUR) / TIME_MINUTE;
		long second = (inVal - hour * TIME_HOUR - minute * TIME_MINUTE)
				/ TIME_SECOND;
		return String.format("%02d:%02d:%02d", hour, minute, second);
	}

	private boolean isVideo() {
		boolean flag = false;
		if (TypeDefs.MEDIA_TYPE_AIRPLAY_VIDEO == mMediaType
				|| TypeDefs.MEDIA_TYPE_DLNA_VIDEO == mMediaType) {
			flag = true;
		}
		return flag;
	}

	private void updateViews() {
		if (mMediaPlayer != null || mVPlayer != null) {
			SetTime();
			SetSeekBar();
		}
	}

	private String setDurationFormat(int duration) {

		int hour = duration / TIME_HOUR;
		int minute = (duration - hour * TIME_HOUR) / TIME_MINUTE;
		int second = (duration - hour * TIME_HOUR - minute * TIME_MINUTE)
				/ TIME_SECOND;

		if (hour == 0) {
			return String.format("%02d  :  %02d", minute, second);
		} else {
			return String
					.format("%02d  :  %02d  :  %02d", hour, minute, second);
		}
	}

	private void SetTime() {
		String current = setDurationFormat((int) mCurrentPosition);
		String total = setDurationFormat((int) mTotalTime);
		mCurrentTimeView.setText(current);
		mTotalTimeView.setText(total);
	}

	private int SetSeekBar() {
		int position = (int) mCurrentPosition;
		int mpMax = (int) mTotalTime;
		long sbMax = mSeekBar.getMax();
		if (mSeekBar != null) {
			if (mpMax > 0) {
				long pos = sbMax * position / mpMax;
				mSeekBar.setProgress((int) pos);
			}
		}
		if (mpMax < 10000) {
			return 900;
		}
		return position;
	}

	private void initInfo() {

		mDismissHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case HIDE_CONTROL_LAYOUT:
					startHideAnimation();
					break;
				default:
					super.handleMessage(msg);
				}
			}
		};

		hideAnimation = AnimationUtils.loadAnimation(this, R.anim.player_out);
		hideAnimation.setAnimationListener(hideListener);

		Updatedata();
		if (isVideo()) {
			if (mVideoInfo != null) {
				mVideoName.setText((null != mVideoInfo.getTitle()) ? mVideoInfo
						.getTitle() : DEFAULT_VIDEO_NAME);
			}
		} else {
			mMusicName
					.setText((null != mCurrAudioInfo.getTitle()) ? mCurrAudioInfo
							.getTitle() : DEFAULT_MUSIC_NAME);
			mAuthorName
					.setText((null != mCurrAudioInfo.getArtist()) ? mCurrAudioInfo
							.getArtist() : DEFAULT_AUTHOR_NAME);
			cdHandler.postDelayed(new Runnable() {
				public void run() {
					startCd();
				}
			}, 200);
			String armartUrl = mCurrAudioInfo.getAlbumArtURI();
			if (armartUrl != null && !"".equals(armartUrl)) {
				new DownloadArmartBackground().execute(armartUrl);
			}
		}

		mDismissHandler.sendEmptyMessageDelayed(HIDE_CONTROL_LAYOUT,
				DEFAULT_HIDE_TIME);
	}
	
	public class DownloadArmartBackground extends AsyncTask<String, Void, Bitmap>{
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
        @Override
        protected Bitmap doInBackground(String... params) {
        	Bitmap bitmap = getBitmapFromUrl(params[0]);
        	Log.v(TAG, "bitmap " + bitmap);
        	return bitmap;
        }
        
		public Bitmap getBitmapFromUrl(String imgUrl) {
			URL url;
			Bitmap bitmap = null;
			try {
				url = new URL(imgUrl);
				InputStream is = url.openConnection().getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] b = new byte[1024];
				int len = 0;
				while ((len = is.read(b, 0, 1024)) != -1) {
					baos.write(b, 0, len);
					baos.flush();
				}
				byte[] bytes = baos.toByteArray();
				bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return bitmap;
		}

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            if (result != null) {
            	mArmartImageView.setImageBitmap(result);
            }
        }
	}
	private void show() {
		if (isVideo()) {
			if (null != mVideoTopLayout) {
				mVideoTopLayout.setVisibility(View.VISIBLE);
			}
			if (null != mVideoBottomLayout) {
				mVideoBottomLayout.setVisibility(View.VISIBLE);
				mVideoStartPauseLayout.setVisibility(View.VISIBLE);
			}
		} else {
			if (null != mMusicBottomLayout) {
				mMusicBottomLayout.setVisibility(View.VISIBLE);
			}
		}
		mDismissHandler.removeMessages(HIDE_CONTROL_LAYOUT);
		mDismissHandler.sendEmptyMessageDelayed(HIDE_CONTROL_LAYOUT,
				DEFAULT_HIDE_TIME);
	}

	private void hide() {
		if (isVideo()) {
			if (null != mVideoTopLayout) {
				mVideoTopLayout.setVisibility(View.GONE);
			}
			if (null != mVideoBottomLayout) {
				mVideoBottomLayout.setVisibility(View.GONE);
				mVideoStartPauseLayout.setVisibility(View.GONE);
			}
		} else {
			if (null != mMusicBottomLayout) {
				mMusicBottomLayout.setVisibility(View.GONE);
			}
		}
	}

	private void startHideAnimation() {
		if (isVideo()) {
			if (null != mVideoTopLayout) {
				if (mVideoTopLayout.getVisibility() == View.VISIBLE) {
					mVideoTopLayout.startAnimation(hideAnimation);
				}
			}
			if (null != mVideoBottomLayout) {
				if (mVideoBottomLayout.getVisibility() == View.VISIBLE) {
					mVideoBottomLayout.startAnimation(hideAnimation);
				}
			}
		} else {
			if (null != mMusicBottomLayout) {
				if (mMusicBottomLayout.getVisibility() == View.VISIBLE) {
					mMusicBottomLayout.startAnimation(hideAnimation);
				}
			}
		}
	}

	private void startCd() {
		if (null != mCdAnim && !mCdAnim.isRunning()) {
			mCdAnim.start();
		}
	}

	private void stopCd() {
		if (null != mCdAnim && mCdAnim.isRunning()) {
			mCdAnim.stop();
		}
	}

	private AnimationListener hideListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			hide();
		}
	};
}