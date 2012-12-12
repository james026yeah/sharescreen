package archermind.dlna.mobile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.archermind.ashare.dlna.localmedia.MusicItem;
import com.archermind.ashare.service.IMusicPlayService;

public class MusicPlayActivity extends BaseActivity implements Runnable {

	private String TAG = "MusicPlayActivity";
	private TextView mArtistTextView;
	private TextView mTitleTextView;
	private IMusicPlayService mMusicPlayService = null;
	private SeekBar mMusicProgress;
//	private static boolean mIsPlayOnLocal = true;
	private ImageButton mPostButton;
	private ImageButton mStatusButton;
	private TextView mDuration;
	private TextView mNowPosition;
	private static Boolean mSeekBarTouchMode = false;
	private static Boolean mIsOnRemotePause = true;
	private ImageButton mShuffle;
	private ImageButton mRepeat;
	private ImageView mMusicImg;
	private int mRemoteSeekPosition = 0;
	private static int mRemotePlayPosition = 0;
	private boolean mIsDLNAConnected = false;
	
	public static final int TIME_SECOND = 1000;
	public static final int TIME_MINUTE = TIME_SECOND * 60;
	public static final int TIME_HOUR = TIME_MINUTE * 60;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_play);
		bindService(new Intent(IMusicPlayService.class.getName()),mMusicSerConn, Context.BIND_AUTO_CREATE);
		mArtistTextView = (TextView) findViewById(R.id.artist);
		mTitleTextView = (TextView) findViewById(R.id.musictitle);
		mMusicProgress = (SeekBar) findViewById(R.id.musicprogress);
		mDuration = (TextView) findViewById(R.id.totaltime);
		mNowPosition = (TextView) findViewById(R.id.nowposition);
		mStatusButton = (ImageButton) findViewById(R.id.pause_play);
		mShuffle = (ImageButton) findViewById(R.id.shuffle);
		mRepeat = (ImageButton) findViewById(R.id.repeat);
		mMusicImg = (ImageView) findViewById(R.id.musicimg);
		mPostButton = (ImageButton) findViewById(R.id.post);
		

		IntentFilter filter = new IntentFilter();
		filter.addAction("statuschanged");
		registerReceiver(mReceiver, filter);
		Message msg = new Message();
		msg.what = 0;
		handler.sendMessageDelayed(msg, 100);
		Thread currentProgress = new Thread(this);
		currentProgress.start();
		
		setMusicImg();
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
		super.onServiceConnected();
		super.getFriendlyNameOfRenderer();
	}
	
	protected void onGetFriendlyName(String friendlyName) {
		if (friendlyName != null) {
			mIsDLNAConnected = true;
			LocalMediaUtil.setConnected(true);
		}
	};

	Handler handler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				try {
					updateMusicInfo();
					mNowPosition.setText(setDurationFormat((int) mMusicPlayService.position()));
					mMusicProgress.setProgress((int) mMusicPlayService.position());
					mRepeat.setOnClickListener(new RepeatBtnClickListener());
					mShuffle.setOnClickListener(new ShuffleBtnClickListener());
					mPostButton.setOnClickListener(new PostBtnListener());
					if (mMusicPlayService.isPlaying()) {
						mStatusButton.setImageResource(R.drawable.video_btn_pause);
					} else {
						mStatusButton.setImageResource(R.drawable.video_btn_start);
					}
					if (mMusicPlayService.getShuffleMode()) {
						mShuffle.setBackgroundResource(R.drawable.music_bth_shuffle_play_normal);
					} else {
						mShuffle.setBackgroundResource(R.drawable.music_bth_shuffle_play_closed_normal);
					}
					if (mMusicPlayService.getRepeatMode() == 0) {
						mRepeat.setBackgroundResource(R.drawable.music_bth_list_normal);
					} else if (mMusicPlayService.getRepeatMode() == 1) {
						mRepeat.setBackgroundResource(R.drawable.music_bth_single_cycle_normal);
						mMusicPlayService.setShuffleMode(false);
						mShuffle.setBackgroundResource(R.drawable.music_bth_shuffle_play_closed_normal);
					} else if (mMusicPlayService.getRepeatMode() == 2) {
						mRepeat.setBackgroundResource(R.drawable.music_bth_repeat_play_normal);
					}
					mMusicProgress.setOnSeekBarChangeListener(new MusicProgressChangeListener());
					if (LocalMediaUtil.getWhichOnRemote() == LocalMediaUtil.Defs.MUSIC) {
						postGetPositionInfo();
						if (!mIsOnRemotePause) {
							mStatusButton.setImageResource(R.drawable.video_btn_pause);
						}
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				break;

			case 1:
				if (LocalMediaUtil.getWhichOnRemote() != LocalMediaUtil.Defs.MUSIC) {
					try {
						mMusicProgress.setProgress((int) mMusicPlayService.position());
						if (mMusicPlayService.isPlaying()) {
							mNowPosition.setText(setDurationFormat((int) mMusicPlayService.position()));
							mNowPosition.setVisibility(View.VISIBLE);
		                } else {
		                    int vis = mNowPosition.getVisibility();
		                    mNowPosition.setVisibility(vis == View.INVISIBLE ? View.VISIBLE : View.INVISIBLE);
		                }
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
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
			mMusicPlayService = null;
		}
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mMusicPlayService = IMusicPlayService.Stub.asInterface(service);
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (LocalMediaUtil.getWhichOnRemote() == LocalMediaUtil.Defs.MUSIC) {
				postSetVolume(MusicUtils.Defs.VOLUME_UP);
				return true;
			}
			break;

		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (LocalMediaUtil.getWhichOnRemote() == LocalMediaUtil.Defs.MUSIC) {
				postSetVolume(MusicUtils.Defs.VOLUME_DOWN);
				return true;
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
			if (LocalMediaUtil.getWhichOnRemote() == LocalMediaUtil.Defs.MUSIC) {
				if (mIsOnRemotePause) {
					postPauseToPlay();
					mStatusButton.setImageResource(R.drawable.video_btn_pause);
					mIsOnRemotePause = false;
				} else {
					postpause();
					mStatusButton.setImageResource(R.drawable.video_btn_start);
					mIsOnRemotePause = true;
				}
			} else {
				mMusicPlayService.pauseButtonPressed();
			}
			break;
		case R.id.stop:
			if (LocalMediaUtil.getWhichOnRemote() == LocalMediaUtil.Defs.MUSIC) {
				poststop();
				LocalMediaUtil.setWhichOnRemote(LocalMediaUtil.Defs.NOT_ANY_ONE);
				mIsOnRemotePause = true;
			}
			mMusicPlayService.stop();
			MusicData.setNowPlayingMusic(null);
			mMusicPlayService.setInitialed(false);
			finish();
			overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
			break;
		case R.id.next:
			mMusicPlayService.next();
			updateMusicInfo();
			if (LocalMediaUtil.getWhichOnRemote() == LocalMediaUtil.Defs.MUSIC) {
				MusicItem music = MusicData.getNowPlayingMusic();
				mMusicPlayService.pause();
//				mMusicProgress.setMax((int) mMusicPlayService.duration());
//				mArtistTextView.setText(music.getArtist());
//				mTitleTextView.setText(music.getTitle());
//				mDuration.setText(setDurationFormat(Integer.parseInt(music.getDuration())));
				postNext(music.getItemUri(),music.metaData);
			} else {
				if (mMusicPlayService.getPreparedStatus()) {
					mMusicPlayService.play();
				}
			}
			break;
		case R.id.prev:
			mMusicPlayService.prev();
			updateMusicInfo();
			if (LocalMediaUtil.getWhichOnRemote() == LocalMediaUtil.Defs.MUSIC) {
				MusicItem music = MusicData.getNowPlayingMusic();
				mMusicPlayService.pause();
//				mMusicProgress.setMax((int) mMusicPlayService.duration());
//				mArtistTextView.setText(music.getArtist());
//				mTitleTextView.setText(music.getTitle());
//				mDuration.setText(setDurationFormat(Integer.parseInt(music.getDuration())));
				postNext(music.getItemUri(),music.metaData);
			} else {
				if (mMusicPlayService.getPreparedStatus()) {
					mMusicPlayService.play();
				}
			}
			break;
		case R.id.post:
			break;
		case R.id.nowplayinglist:
			MusicData.setMusicShowList(MusicData.getMusicPlayList());
			Intent intent = new Intent();
			intent.putExtra("title", getResources().getString(R.string.playing_list));
			intent.putExtra("scrollto", true);
			intent.setClass(getApplicationContext(), MusicListActivity.class);
			startActivityForResult(intent,	0);
			overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
				mStatusButton.setImageResource(R.drawable.video_btn_pause);
				mMusicPlayService.pause();
//				mIsPlayOnLocal = false;
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
			int positionInt = (int) fromDateStringToInt((String) obj.get("relTime"));
			mRemotePlayPosition = positionInt;
			if (!mSeekBarTouchMode && (LocalMediaUtil.getWhichOnRemote() == LocalMediaUtil.Defs.MUSIC)) {
				mNowPosition.setText(setDurationFormat(positionInt));
				mMusicProgress.setProgress(positionInt);
				postGetPositionInfo();
			}
		} else {
//			mIsPlayOnLocal = true;
			try {
//				mMusicPlayService.setPlayOnPhone(true);
				mMusicPlayService.seekTo(mRemotePlayPosition);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			try {
				if (mMusicPlayService.isPlaying()) {
					mStatusButton.setImageResource(R.drawable.video_btn_pause);
				} else {
					mStatusButton.setImageResource(R.drawable.video_btn_start);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onGetSeekresult(Boolean obj) {
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
	
	public void setMusicImg() {
		MusicItem music = MusicData.getNowPlayingMusic();
		if (getMusicImg(music.getAlbumArtURI()) == null) {
			mMusicImg.setImageResource(R.drawable.phone_music_album_default);
		} else {
			mMusicImg.setImageBitmap(getMusicImg(music.getAlbumArtURI()));
		}
	}
	
	public void updateMusicInfo() {
		MusicItem music = MusicData.getNowPlayingMusic();
		mArtistTextView.setText(music.getArtist());
		mTitleTextView.setText(music.getTitle());
		mDuration.setText(setDurationFormat(Integer.parseInt(music.getDuration())));
		mMusicProgress.setMax(Integer.parseInt(music.getDuration()));
		setMusicImg();
	}
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (LocalMediaUtil.getWhichOnRemote() != LocalMediaUtil.Defs.MUSIC) {
				try {
					mMusicProgress.setProgress((int) mMusicPlayService.position());
					if (mMusicPlayService.isPlaying()) {
						updateMusicInfo();
						mStatusButton.setImageResource(R.drawable.video_btn_pause);
					} else {
						mStatusButton.setImageResource(R.drawable.video_btn_start);
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
				if (LocalMediaUtil.getWhichOnRemote() != LocalMediaUtil.Defs.MUSIC) {
					MusicItem music = MusicData.getNowPlayingMusic();
					try {
						   LocalMediaUtil.setWhichOnRemote(LocalMediaUtil.Defs.MUSIC);
							mPostButton.setClickable(false);
							mMusicPlayService.pause();
//							mMusicPlayService.setPlayOnPhone(false);
//							mIsPlayOnLocal = false;
							mIsOnRemotePause = false;
							mMusicPlayService.postToRemote();
							Log.e(TAG,music.getItemUri());
							postSeek(mMusicPlayService.position() + "");
//							Log.d(TAG, "isPlayOnPhone" + mIsPlayOnLocal);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				} else {
//					mIsPlayOnLocal = true;
					poststop();
					LocalMediaUtil.setWhichOnRemote(LocalMediaUtil.Defs.NOT_ANY_ONE);
					try {
//						mMusicPlayService.setPlayOnPhone(true);
						mMusicPlayService.play();
						mMusicPlayService.seekTo(mRemotePlayPosition);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					try {
						if (mMusicPlayService.isPlaying()) {
							mStatusButton.setImageResource(R.drawable.video_btn_pause);
						} else {
							mStatusButton.setImageResource(R.drawable.video_btn_start);
						}
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			} else {
				Toast.makeText(getApplicationContext(), R.string.video_not_connection_toast_message, Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	class ShuffleBtnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			try {
				if (mMusicPlayService.getShuffleMode()) {
					mMusicPlayService.setShuffleMode(false);
					mShuffle.setBackgroundResource(R.drawable.music_bth_shuffle_play_closed_normal);
					Toast.makeText(getApplicationContext(), R.string.shuffle_close, Toast.LENGTH_SHORT).show();
				} else {
					mMusicPlayService.setShuffleMode(true);
					mShuffle.setBackgroundResource(R.drawable.music_bth_shuffle_play_normal);
					Toast.makeText(getApplicationContext(), R.string.shuffle_open, Toast.LENGTH_SHORT).show();
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	class MusicProgressChangeListener implements OnSeekBarChangeListener {
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			if (LocalMediaUtil.getWhichOnRemote() == LocalMediaUtil.Defs.MUSIC) {
				postSeek(mRemoteSeekPosition + "");
				Log.e("james","postSeekto" + mRemoteSeekPosition);
			}
			mSeekBarTouchMode = false;
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			mSeekBarTouchMode = true;
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
			if (LocalMediaUtil.getWhichOnRemote() == LocalMediaUtil.Defs.MUSIC) {
				if (mSeekBarTouchMode) {
					mRemoteSeekPosition = progress;
				}
			} else {
				if (mSeekBarTouchMode) {
					try {
						mMusicPlayService.seekTo(progress);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	class RepeatBtnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			try {
				mMusicPlayService.setRepeatMode((mMusicPlayService.getRepeatMode()+1)%3);
				if (mMusicPlayService.getRepeatMode() == 0) {
					mRepeat.setBackgroundResource(R.drawable.music_bth_list_normal);
					Toast.makeText(getApplicationContext(), R.string.list_normal, Toast.LENGTH_SHORT).show();
				} else if (mMusicPlayService.getRepeatMode() == 1) {
					mRepeat.setBackgroundResource(R.drawable.music_bth_single_cycle_normal);
					Toast.makeText(getApplicationContext(), R.string.single_cycle, Toast.LENGTH_SHORT).show();
					mMusicPlayService.setShuffleMode(false);
					mShuffle.setBackgroundResource(R.drawable.music_bth_shuffle_play_closed_normal);
				} else if (mMusicPlayService.getRepeatMode() == 2) {
					mRepeat.setBackgroundResource(R.drawable.music_bth_repeat_play_normal);
					Toast.makeText(getApplicationContext(), R.string.list_cycle, Toast.LENGTH_SHORT).show();
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			
		}
	
		
	}
	
}