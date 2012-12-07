package archermind.dlna.mobile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.archermind.ashare.service.IMusicPlayService;
import com.archermind.ashare.service.MusicPlayService;
import com.archermind.ashare.ui.control.VerticalSeekBar;
import com.archermind.ashare.ui.control.VerticalSeekBar.OnSeekBarChangeListener;
import com.archermind.ashare.wiremote.natives.WiRemoteAgent;

public class MusicPlayActivity extends BaseActivity implements Runnable {

	private String TAG = "MusicPlayActivity";
	private TextView mArtistTextView;
	private TextView mTitleTextView;
	private IMusicPlayService mMusicPlaySer = null;
	private VerticalSeekBar mVolumnControl;
	private SeekBar mMusicProgress;
	private AudioManager audioManager;
	private int currentVolume, maxVolume;
	private static boolean mIsPlayOnPhone = true;
	private ImageButton mPostButton;
	private ImageButton mStatusButton;
	private TextView mDuration;
	private TextView mNowPosition;
	private static Boolean mSeekBarTouchMode = false;
	private static Boolean mIsOnTVPause = true;
	private ImageButton mShuffle;
	private ImageButton mRepeat;
	private ImageView mMusicImg;
	private int mTvSeekPosition = 0;
	private static int mTvPlayPosition = 0;
	private LinearLayout mVolumeControlLayout;
	private Intent mMusicSerIntent;
	private boolean mVolumeControlVisual = false;
	private boolean mIsDLNAConnected = false;
	
	private static final int KEY_DOWN = 1;
	private static final int KEY_UP = 0;
	private static final int KEYCODE_VOLUMEDOWN = 114;
	private static final int KEYCODE_VOLUMEUP = 115;
	public static final int TIME_SECOND = 1000;
	public static final int TIME_MINUTE = TIME_SECOND * 60;
	public static final int TIME_HOUR = TIME_MINUTE * 60;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_play);
		mMusicSerIntent = new Intent(getApplicationContext(), MusicPlayService.class);
		startService(mMusicSerIntent);
		bindService(new Intent(IMusicPlayService.class.getName()),
				mMusicSerConn, Context.BIND_AUTO_CREATE);
		mArtistTextView = (TextView) findViewById(R.id.artist);
		mTitleTextView = (TextView) findViewById(R.id.musictitle);
		mMusicProgress = (SeekBar) findViewById(R.id.musicprogress);
		mVolumnControl = (VerticalSeekBar) findViewById(R.id.volumecontrol);
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mVolumeControlLayout = (LinearLayout) findViewById(R.id.volum_contro);

		maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mVolumnControl.setMax(maxVolume);
		currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		mVolumnControl.setProgress(currentVolume);
		
		mDuration = (TextView) findViewById(R.id.totaltime);
		mNowPosition = (TextView) findViewById(R.id.nowposition);
		mStatusButton = (ImageButton) findViewById(R.id.pause_play);
		
		mShuffle = (ImageButton) findViewById(R.id.shuffle);
		mRepeat = (ImageButton) findViewById(R.id.repeat);
		mMusicImg = (ImageView) findViewById(R.id.musicimg);
		
		mPostButton = (ImageButton) findViewById(R.id.post);

		mVolumnControl.setOnSeekBarChangeListener(new VolumControlBarListener());
		IntentFilter filter = new IntentFilter();
		filter.addAction("statuschanged");
		registerReceiver(mReceiver, filter);
		Message msg = new Message();
		msg.what = 0;
		handler.sendMessageDelayed(msg, 100);
		Thread currentProgress = new Thread(this);
		currentProgress.start();
	}

	@Override
	public void run() {
		try {
			while (true) {
				Message msg = new Message();
				msg.what = 1;
				handler.sendMessage(msg);
				Thread.sleep(500);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onServiceConnected() {
		// TODO Auto-generated method stub
		super.onServiceConnected();
		super.getFriendlyNameOfRenderer();
	}
	
	protected void onGetFriendlyName(String friendlyName) {
		if (friendlyName != null) {
			mIsDLNAConnected = true;
		}
	};

	Handler handler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				try {
					if (getMusicImg(mMusicPlaySer.getNowPlayItem().getAlbumArtURI()) != null) {
						mMusicImg.setImageBitmap(getMusicImg(mMusicPlaySer.getNowPlayItem().getAlbumArtURI()));
					} else {
						mStatusButton.setImageResource(R.drawable.video_bth_play_normal);
					}
					mMusicProgress.setMax((int) mMusicPlaySer.duration());
					mMusicProgress.setProgress((int) mMusicPlaySer.position());
					mNowPosition.setText(setDurationFormat((int) mMusicPlaySer.position()));
					Log.d(TAG,"mMusicProgress max:"+ mMusicProgress.getMax());
					Log.d(TAG,"mMusicplay position:" + mMusicPlaySer.position());
					mRepeat.setOnClickListener(new RepeatBtnClickListener());
					mShuffle.setOnClickListener(new ShuffleBtnClickListener());
					mPostButton.setOnClickListener(new PostBtnListener());
					mArtistTextView.setText(mMusicPlaySer.getArtistName());
					mTitleTextView.setText(mMusicPlaySer.getTrackName());
					mDuration.setText(setDurationFormat((int) mMusicPlaySer.duration()));
					if (mMusicPlaySer.isPlaying()) {
						mStatusButton.setImageResource(R.drawable.video_bth_pause_normal);
					} else {
						mStatusButton.setImageResource(R.drawable.video_bth_play_normal);
					}
					if (mMusicPlaySer.getShuffleMode()) {
						mShuffle.setBackgroundResource(R.drawable.music_bth_shuffle_play_normal);
					} else {
						mShuffle.setBackgroundResource(R.drawable.music_bth_shuffle_play_closed_normal);
					}
					if (mMusicPlaySer.getRepeatMode() == 0) {
						mRepeat.setBackgroundResource(R.drawable.music_bth_list_normal);
					} else if (mMusicPlaySer.getRepeatMode() == 1) {
						mRepeat.setBackgroundResource(R.drawable.music_bth_single_cycle_normal);
						mMusicPlaySer.setShuffleMode(false);
						mShuffle.setBackgroundResource(R.drawable.music_bth_shuffle_play_closed_normal);
					} else if (mMusicPlaySer.getRepeatMode() == 2) {
						mRepeat.setBackgroundResource(R.drawable.music_bth_repeat_play_normal);
					}
					mMusicProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
						
						@Override
						public void onStopTrackingTouch(SeekBar seekBar) {
							if (!mIsPlayOnPhone) {
								postSeek(mTvSeekPosition + "");
								Log.e("james","postSeek success" + mTvSeekPosition);
							}
							mSeekBarTouchMode = false;
						}
						
						@Override
						public void onStartTrackingTouch(SeekBar seekBar) {
							mSeekBarTouchMode = true;
						}
						
						@Override
						public void onProgressChanged(SeekBar seekBar, int progress,
								boolean fromUser) {
							if (!mIsPlayOnPhone) {
								if (mSeekBarTouchMode) {
									mTvSeekPosition = progress;
								}
							} else {
								if (mSeekBarTouchMode) {
									try {
										mMusicPlaySer.seekTo(progress);
									} catch (RemoteException e) {
										e.printStackTrace();
									}
								}
							}
						}
					});
					if (!mIsPlayOnPhone) {
						postGetPositionInfo();
						if (!mIsOnTVPause) {
							mStatusButton.setImageResource(R.drawable.video_bth_pause_normal);
						}
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				break;

			case 1:
				if (mIsPlayOnPhone) {
					try {
						mMusicProgress.setProgress((int) mMusicPlaySer.position());
						mNowPosition.setText(setDurationFormat((int) mMusicPlaySer.position()));
						if (mMusicPlaySer.isPlaying()) {
							mNowPosition.setVisibility(View.VISIBLE);
		                } else {
		                    // blink the counter
		                    int vis = mNowPosition.getVisibility();
		                    mNowPosition.setVisibility(vis == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
		                }
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
				break;
			case 2:
				mVolumeControlLayout.setVisibility(LinearLayout.GONE);
				mVolumeControlVisual = false;
				break;
			default:
				break;
			}
		};
	};
	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService(mMusicSerConn);
		unregisterReceiver(mReceiver);
	}


	private ServiceConnection mMusicSerConn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mMusicPlaySer = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mMusicPlaySer = IMusicPlayService.Stub.asInterface(service);
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
//		Message msg = new Message();
//		msg.what = 2;
//		case KeyEvent.KEYCODE_VOLUME_UP:
//			mVolumeControlLayout.setVisibility(LinearLayout.VISIBLE);
//			mVolumeControlVisual = true;
//			handler.removeMessages(2);
//			handler.sendMessageDelayed(msg, 3000);
//			if (currentVolume < maxVolume){
//				currentVolume += 1;
//				mVolumnControl.setProgress(currentVolume);
//			}
//			if (!mIsPlayOnPhone) {
//				postSetVolume((float) currentVolume/maxVolume);
//				Log.e("james","postSetVolume" + currentVolume);
//			}
//			if (currentVolume == audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
//				Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//				vib.vibrate(200);
//			}
//			break;
//
//		case KeyEvent.KEYCODE_VOLUME_DOWN:
//			mVolumeControlLayout.setVisibility(LinearLayout.VISIBLE);
//			mVolumeControlVisual = true;
//			handler.removeMessages(2);
//			handler.sendMessageDelayed(msg, 3000);
//			if (currentVolume > 0){
//				currentVolume -= 1;
//				Log.e("james",currentVolume + "volume");
//				mVolumnControl.setProgress(currentVolume);
//			}
//			if (!mIsPlayOnPhone) {
//				postSetVolume((float) currentVolume/maxVolume);
//				Log.d(TAG,"postSetVolume" + currentVolume);
//			}
//			if (currentVolume == 0) {
//				Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//				vib.vibrate(200);
//			}
//			break;
			
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP: {
			if (!mIsPlayOnPhone) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					WiRemoteAgent.setKeyEvent(KEY_DOWN, KEYCODE_VOLUMEUP);
					return false;
				case MotionEvent.ACTION_UP:
					WiRemoteAgent.setKeyEvent(KEY_UP, KEYCODE_VOLUMEUP);
					return false;
				default:
					break;
				}
			}
		}
		break;
		case KeyEvent.KEYCODE_VOLUME_DOWN:{
			if (!mIsPlayOnPhone) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					WiRemoteAgent.setKeyEvent(KEY_DOWN, KEYCODE_VOLUMEDOWN);
					return false;
				case MotionEvent.ACTION_UP:
					WiRemoteAgent.setKeyEvent(KEY_UP, KEYCODE_VOLUMEDOWN);
					return false;
				default:
					break;
				}
			}
		}
		break;
		case KeyEvent.KEYCODE_BACK:
			finish();
			overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
			break;
		default:
			super.onKeyDown(keyCode, event);
			break;
		}
		return false;
	}

	public void doClick(View view) throws RemoteException {
		switch (view.getId()) {
		case R.id.pause_play:
			if (mIsPlayOnPhone) {
				mMusicPlaySer.pauseButtonPressed();
			} else {
				if (mIsOnTVPause) {
					postPauseToPlay();
					mStatusButton.setImageResource(R.drawable.video_bth_pause_normal);
					mIsOnTVPause = false;
				} else {
					postpause();
					mStatusButton.setImageResource(R.drawable.video_bth_play_normal);
					mIsOnTVPause = true;
				}
			}
			break;
		case R.id.stop:
			if (mIsPlayOnPhone) {
				mMusicPlaySer.stop();
				mMusicPlaySer.setInitialed(false);
			} else {
				poststop();
				mIsPlayOnPhone = true;
				mIsOnTVPause = true;
				mMusicPlaySer.setPlayOnPhone(true);
			}
			finish();
			overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
			break;
		case R.id.next:
			mMusicPlaySer.next();
			if (!mIsPlayOnPhone) {
				mMusicPlaySer.pause();
				mMusicProgress.setMax((int) mMusicPlaySer.duration());
				mArtistTextView.setText(mMusicPlaySer.getNowPlayItem().getArtist());
				mTitleTextView.setText(mMusicPlaySer.getNowPlayItem().getTitle());
				mDuration.setText(setDurationFormat(Integer.parseInt(mMusicPlaySer.getNowPlayItem().getDuration())));
				postPlay(mMusicPlaySer.getNowPlayItem().getItemUri(),
						mMusicPlaySer.getNowPlayItem().metaData);
			} else if (mIsPlayOnPhone) {
				if (mMusicPlaySer.getPreparedStatus()) {
					mMusicPlaySer.play();
				}
			}
			break;
		case R.id.prev:
			mMusicPlaySer.prev();
			if (!mIsPlayOnPhone) {
				mMusicPlaySer.pause();
				mMusicProgress.setMax((int) mMusicPlaySer.duration());
				mArtistTextView.setText(mMusicPlaySer.getNowPlayItem().getArtist());
				mTitleTextView.setText(mMusicPlaySer.getNowPlayItem().getTitle());
				mDuration.setText(setDurationFormat(Integer.parseInt(mMusicPlaySer.getNowPlayItem().getDuration())));
				postPlay(mMusicPlaySer.getNowPlayItem().getItemUri(),
						mMusicPlaySer.getNowPlayItem().metaData);
			} else if (mIsPlayOnPhone) {
				if (mMusicPlaySer.getPreparedStatus()) {
					mMusicPlaySer.play();
				}
			}
			break;
		case R.id.post:
			break;
		case R.id.besilient:
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
					0, 0);
			mVolumnControl.setProgress(0);
			currentVolume = 0;
			Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			vib.vibrate(300);
			if (!mIsPlayOnPhone) {
				postSetVolume((float) currentVolume/maxVolume);
			}
			break;
		case R.id.nowplayinglist:
			mMusicPlaySer.setMusicShowList(mMusicPlaySer.getPlayList());
			Intent intent = new Intent();
			intent.putExtra("title", getResources().getString(R.string.playing_list));
			intent.putExtra("scrollto", true);
			intent.setClass(getApplicationContext(), MusicListActivity.class);
			startActivityForResult(intent,	0);
			overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
			break;
		case R.id.volume_show:
			if (mVolumeControlVisual == false) {
				mVolumeControlLayout.setVisibility(LinearLayout.VISIBLE);
				mVolumeControlVisual = true;
				handler.removeMessages(2);
				Message msg = new Message();
				msg.what = 2;
				handler.sendMessageDelayed(msg, 3000);
			} else {
				mVolumeControlLayout.setVisibility(LinearLayout.GONE);
				handler.removeMessages(2);
				mVolumeControlVisual = false;
			}
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if (resultCode == 0) {
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onGetPlayresult(Boolean obj) {
		if (obj) {
			mPostButton.setClickable(true);
			try {
				mMusicPlaySer.pause();
				mIsPlayOnPhone = false;
				postGetPositionInfo();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		} else {
		}
	}
	
	@Override
	public void onGetPositioninforesult(Map obj) {
		if (obj != null) {
			int positionInt = (int) fromDateStringToInt((String) obj
					.get("relTime"));
			mTvPlayPosition = positionInt;
			if (!mSeekBarTouchMode && !mIsPlayOnPhone) {
				mMusicProgress.setProgress(positionInt);
				mNowPosition.setText(setDurationFormat(positionInt));
				postGetPositionInfo();
			}
		} else {
			mIsPlayOnPhone = true;
			try {
				mMusicPlaySer.setPlayOnPhone(true);
				mMusicPlaySer.seekTo(mTvPlayPosition);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			try {
				if (mMusicPlaySer.isPlaying()) {
					mStatusButton
							.setImageResource(R.drawable.video_bth_pause_normal);
				} else {
					mStatusButton
							.setImageResource(R.drawable.video_bth_play_normal);
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.d(TAG, "isPlayOnPhone" + mIsPlayOnPhone);
		}
	}
	
	@Override
	public void onGetSeekresult(Boolean obj) {
		Log.e(TAG,"SeekBack :" + obj);
		if (obj) {
			postGetPositionInfo();
		}
	}
	
	public Bitmap getMusicImg(String mAlbumArtURI) {
        File file = new File(mAlbumArtURI);
        Bitmap bt = null;
        try {
			ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
			bt = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, null);
			if (bt == null) {
			} else {
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return bt;
	}
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (mIsPlayOnPhone) {
				Log.d(TAG,"isPlayOnPhone");
				try {
					mMusicProgress.setMax((int) mMusicPlaySer.duration());
					mMusicProgress.setProgress((int) mMusicPlaySer.position());
					Log.d(TAG,"mMusicProgress max:"+ mMusicProgress.getMax());
					Log.d(TAG,"mMusicplay position:" + mMusicPlaySer.position());
					if (mMusicPlaySer.isPlaying()) {
						Log.d(TAG, "received from" + intent.getStringExtra("title"));
						mArtistTextView.setText(mMusicPlaySer.getNowPlayItem().getArtist());
						mTitleTextView.setText(mMusicPlaySer.getNowPlayItem().getTitle());
						mDuration.setText(setDurationFormat((int) mMusicPlaySer.duration()));
						mStatusButton.setImageResource(R.drawable.video_bth_pause_normal);
						if (getMusicImg(mMusicPlaySer.getNowPlayItem().getAlbumArtURI()) != null) {
							mMusicImg.setImageBitmap(getMusicImg(mMusicPlaySer.getNowPlayItem().getAlbumArtURI()));
						} else {
							Log.e("james","setdefault");
							mMusicImg.setImageResource(R.drawable.phone_music_album_default);
						}
					} else {
						mStatusButton.setImageResource(R.drawable.video_bth_play_normal);
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	};

	public static String setDurationFormat(int inVal) {
		long hour = inVal / TIME_HOUR;
    	long minute = (inVal - hour * TIME_HOUR) / TIME_MINUTE;
    	long second = (inVal - hour * TIME_HOUR - minute * TIME_MINUTE) / TIME_SECOND;
		
		if(hour == 0) {
			return String.format("%02d:%02d", minute, second);
		}
		else {
			return String.format("%02d:%02d:%02d", hour, minute, second);
		}
	}
	
	public static long fromDateStringToInt(String inVal)
    {
		String[] timeSlice = inVal.split(":");
		long temp = 0;
		for (int i = 0; i < timeSlice.length; i++) {
			temp = temp * 60 + Integer.parseInt(timeSlice[i]);
		}
		return temp * 1000;
    }
	
	class PostBtnListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			if (mIsDLNAConnected) {
				if (mIsPlayOnPhone) {
					try {
						mPostButton.setClickable(false);
						mMusicPlaySer.pause();
						mMusicPlaySer.setPlayOnPhone(false);
						mIsPlayOnPhone = false;
						mIsOnTVPause = false;
						postPlay(mMusicPlaySer.getNowPlayItem().getItemUri(),
								mMusicPlaySer.getNowPlayItem().metaData);
						postSetVolume((float) currentVolume / maxVolume);
						postSeek(mMusicPlaySer.position() + "");
						Log.d(TAG, "isPlayOnPhone" + mIsPlayOnPhone);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				} else {
					mIsPlayOnPhone = true;
					poststop();
					try {
						mMusicPlaySer.setPlayOnPhone(true);
						mMusicPlaySer.play();
						mMusicPlaySer.seekTo(mTvPlayPosition);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					try {
						if (mMusicPlaySer.isPlaying()) {
							mStatusButton
									.setImageResource(R.drawable.video_bth_pause_normal);
						} else {
							mStatusButton
									.setImageResource(R.drawable.video_bth_play_normal);
						}
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Log.d(TAG, "isPlayOnPhone" + mIsPlayOnPhone);
				}
			} else {
				Toast.makeText(getApplicationContext(), R.string.not_connect_to_device, Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	class ShuffleBtnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			try {
				if (mMusicPlaySer.getShuffleMode()) {
					mMusicPlaySer.setShuffleMode(false);
					mShuffle.setBackgroundResource(R.drawable.music_bth_shuffle_play_closed_normal);
					Toast.makeText(getApplicationContext(), R.string.shuffle_close, Toast.LENGTH_SHORT).show();
				} else {
					mMusicPlaySer.setShuffleMode(true);
					mShuffle.setBackgroundResource(R.drawable.music_bth_shuffle_play_normal);
					Toast.makeText(getApplicationContext(), R.string.shuffle_open, Toast.LENGTH_SHORT).show();
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	class RepeatBtnClickListener implements OnClickListener {
		
		@Override
		public void onClick(View v) {
			try {
				mMusicPlaySer.setRepeatMode((mMusicPlaySer.getRepeatMode()+1)%3);
				if (mMusicPlaySer.getRepeatMode() == 0) {
					mRepeat.setBackgroundResource(R.drawable.music_bth_list_normal);
					Toast.makeText(getApplicationContext(), R.string.list_normal, Toast.LENGTH_SHORT).show();
				} else if (mMusicPlaySer.getRepeatMode() == 1) {
					mRepeat.setBackgroundResource(R.drawable.music_bth_single_cycle_normal);
					Toast.makeText(getApplicationContext(), R.string.single_cycle, Toast.LENGTH_SHORT).show();
					mMusicPlaySer.setShuffleMode(false);
					mShuffle.setBackgroundResource(R.drawable.music_bth_shuffle_play_closed_normal);
				} else if (mMusicPlaySer.getRepeatMode() == 2) {
					mRepeat.setBackgroundResource(R.drawable.music_bth_repeat_play_normal);
					Toast.makeText(getApplicationContext(), R.string.list_cycle, Toast.LENGTH_SHORT).show();
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			
		}
	
		
	}
	
	class VolumControlBarListener implements OnSeekBarChangeListener {

		@Override
		public void onStopTrackingTouch(
				VerticalSeekBar Verticalseekbar) {
			final Message msgVolum = new Message();
			msgVolum.what = 2;
			handler.sendMessageDelayed(msgVolum, 3000);

		}

		@Override
		public void onStartTrackingTouch(
				VerticalSeekBar Verticalseekbar) {
			handler.removeMessages(2);

		}

		@Override
		public void onProgressChanged(
				VerticalSeekBar Verticalseekbar, int progress,
				boolean fromUser) {
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
					progress, 0);
			currentVolume = progress;
			if (!mIsPlayOnPhone) {
				postSetVolume((float)currentVolume/maxVolume);
				Log.d(TAG,"current Volume" + currentVolume);
				Log.d(TAG,"progress postSetVolume" + currentVolume);
			}
		}
	}
	
}
