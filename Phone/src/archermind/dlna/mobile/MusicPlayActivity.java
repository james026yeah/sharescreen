package archermind.dlna.mobile;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

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
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import archermind.dlna.verticalseekbar.VerticalSeekBar;
import archermind.dlna.verticalseekbar.VerticalSeekBar.OnSeekBarChangeListener;

public class MusicPlayActivity extends BaseActivity implements OnClickListener, Runnable {

	private TextView mArtistTV;
	private TextView mTitleTV;
	private IMusicPlayService mMusicPlaySer = null;
	private VerticalSeekBar mVolumnControl;
	private SeekBar mMusicProgress;
	private AudioManager audioManager;
	private int currentVolume, maxVolume;
	private boolean mIsPlayOnPhone = true;
	private ImageButton mPostButton;
	private ImageButton mStatusButton;
	private TextView mDuration;
	private TextView mNowPosition;
	private Boolean mSeekBarTouchMode = false;
	private Boolean mIsOnTVPause = false;
	private ImageButton mShuffle;
	private ImageButton mRepeat;
	private ImageView mMusicImg;
	private int mTvSeekPosition = 0;
	private int mTvPlayPosition = 0;
	
	public static final int TIME_SECOND = 1000;
	public static final int TIME_MINUTE = TIME_SECOND * 60;
	public static final int TIME_HOUR = TIME_MINUTE * 60;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (mIsPlayOnPhone) {
				Log.e("james","isPlayOnPhone");
				try {
					mMusicProgress.setMax((int) mMusicPlaySer.duration());
					mMusicProgress.setProgress((int) mMusicPlaySer.position());
					Log.e("james","mMusicProgress max:"+ mMusicProgress.getMax());
					Log.e("james","mMusicplay position:" + mMusicPlaySer.position());
					if (mMusicPlaySer.isPlaying()) {
						Log.e("james", "received from" + intent.getStringExtra("title"));
						mArtistTV.setText(mMusicPlaySer.getNowPlayItem().getArtist());
						mTitleTV.setText(mMusicPlaySer.getNowPlayItem().getTitle());
						mDuration.setText(setDurationFormat((int) mMusicPlaySer.duration()));
						mStatusButton.setImageResource(R.drawable.video_bth_pause_normal);
						if (getMusicImg(mMusicPlaySer.getNowPlayItem().getAlbumArtURI()) != null) {
							mMusicImg.setImageBitmap(getMusicImg(mMusicPlaySer.getNowPlayItem().getAlbumArtURI()));
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_play);
		bindService(new Intent(IMusicPlayService.class.getName()),
				mMusicSerConn, Context.BIND_AUTO_CREATE);
		mArtistTV = (TextView) findViewById(R.id.artist);
		mTitleTV = (TextView) findViewById(R.id.musictitle);
		mMusicProgress = (SeekBar) findViewById(R.id.musicprogress);
		mVolumnControl = (VerticalSeekBar) findViewById(R.id.volumecontrol);
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mVolumnControl.setMax(maxVolume-1);
		currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		mVolumnControl.setProgress(currentVolume);
		
		mDuration = (TextView) findViewById(R.id.totaltime);
		mNowPosition = (TextView) findViewById(R.id.nowposition);
		mStatusButton = (ImageButton) findViewById(R.id.pause_play);
		
		mShuffle = (ImageButton) findViewById(R.id.shuffle);
		mRepeat = (ImageButton) findViewById(R.id.repeat);
		mMusicImg = (ImageView) findViewById(R.id.musicimg);
		
		mPostButton = (ImageButton) findViewById(R.id.post);
		

		mVolumnControl
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(
							VerticalSeekBar Verticalseekbar) {

					}

					@Override
					public void onStartTrackingTouch(
							VerticalSeekBar Verticalseekbar) {

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
							Log.e("james","current Volume" + currentVolume);
							Log.e("james","progress postSetVolume" + currentVolume);
						}
					}
				});
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
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	Handler handler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				try {
					if (getMusicImg(mMusicPlaySer.getNowPlayItem().getAlbumArtURI()) != null) {
						mMusicImg.setImageBitmap(getMusicImg(mMusicPlaySer.getNowPlayItem().getAlbumArtURI()));
					}
					mMusicProgress.setMax((int) mMusicPlaySer.duration());
					mMusicProgress.setProgress((int) mMusicPlaySer.position());
					mNowPosition.setText(setDurationFormat((int) mMusicPlaySer.position()));
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
					Log.e("james","mMusicProgress max:"+ mMusicProgress.getMax());
					Log.e("james","mMusicplay position:" + mMusicPlaySer.position());
					mRepeat.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
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
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
						}
					});
					
					mShuffle.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
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
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});
					mArtistTV.setText(mMusicPlaySer.getArtistName());
					mTitleTV.setText(mMusicPlaySer.getTrackName());
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
					
					mPostButton.setOnClickListener(new OnClickListener() {
						
						@Override
						public void onClick(View v) {
							if (mIsPlayOnPhone) {
								mIsPlayOnPhone = false;
								try {
									mIsOnTVPause = false;
									postPlay(mMusicPlaySer.getNowPlayItem().getItemUri(), MUSIC_TYPE);
									postSeek(mMusicPlaySer.position() + "");
									Log.e("james","isPlayOnPhone" + mIsPlayOnPhone);
								} catch (RemoteException e) {
									e.printStackTrace();
								}
							} else {
								mIsPlayOnPhone = true;
								poststop();
								try {
									mMusicPlaySer.play();
									mMusicPlaySer.seekTo(mTvPlayPosition);
								} catch (RemoteException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								Log.e("james","isPlayOnPhone" + mIsPlayOnPhone);
							}
						}
					});
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				break;

			case 1:
				if (mIsPlayOnPhone) {
					try {
						mMusicProgress.setProgress((int) mMusicPlaySer.position());
						mNowPosition.setText(setDurationFormat((int) mMusicPlaySer.position()));
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
	}

	@Override
	public void onClick(View v) {
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
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			if (currentVolume < maxVolume){
				currentVolume += 1;
				mVolumnControl.setProgress(currentVolume);
			}
			if (!mIsPlayOnPhone) {
				postSetVolume((float) currentVolume/maxVolume);
				Log.e("james","postSetVolume" + currentVolume);
			}
			if (currentVolume == audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
				Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				vib.vibrate(200);
			}
			break;

		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (currentVolume > 0){
				currentVolume -= 1;
				Log.e("james",currentVolume + "volume");
				mVolumnControl.setProgress(currentVolume);
			}
			if (!mIsPlayOnPhone) {
				postSetVolume((float) currentVolume/maxVolume);
				Log.e("james","postSetVolume" + currentVolume);
			}
			if (currentVolume == 0) {
				Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				vib.vibrate(200);
			}
			break;
			
		default:
			super.onKeyDown(keyCode, event);
			break;
		}
		return true;
	}

	public void doClick(View view) throws RemoteException {
		switch (view.getId()) {
		case R.id.pause_play:
			if (mIsPlayOnPhone) {
				mMusicPlaySer.pause();
				Log.e("james", " pause + isPlayOnPhone " + mIsPlayOnPhone );
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
				Log.e("james", " pause + isPlayOnPhone " + mIsPlayOnPhone );
			}
			break;
		case R.id.stop:
			if (mIsPlayOnPhone) {
				mMusicPlaySer.stop();
				mMusicPlaySer.setPlayList(null);
				finish();
			} else {
				poststop();
				Log.e("james","pause On Tv stop");
				finish();
			}
			break;
		case R.id.next:
			mMusicPlaySer.next();
			if (!mIsPlayOnPhone) {
				mMusicPlaySer.pause();
				poststop();
				postPlay(mMusicPlaySer.getNowPlayItem().getItemUri(), MUSIC_TYPE);
			} else if (mIsPlayOnPhone) {
				mMusicPlaySer.play();
			}
			break;
		case R.id.prev:
			mMusicPlaySer.prev();
			if (!mIsPlayOnPhone) {
				mMusicPlaySer.pause();
				poststop();
				postPlay(mMusicPlaySer.getNowPlayItem().getItemUri(), MUSIC_TYPE);
			} else if (mIsPlayOnPhone) {
				mMusicPlaySer.play();
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
			intent.setClass(getApplicationContext(), MusicListActivity.class);
			startActivity(intent);
			break;
		}

	}
	
	

	@Override
	public void onGetSetvolumeresult(Boolean obj) {
		// TODO Auto-generated method stub
		Log.e("james","setvolume finished ? " + obj);
	}

	@Override
	public void onGetPlayresult(Boolean obj) {
		if (obj) {
			Toast.makeText(getApplicationContext(), "Play On tv", Toast.LENGTH_SHORT).show();
			try {
				mMusicPlaySer.pause();
				
				mIsPlayOnPhone = false;
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Toast.makeText(getApplicationContext(), "Play On Phone", Toast.LENGTH_SHORT).show();
		}
	}

	
	@Override
	public void onGetPositioninforesult(Map obj) {
		// TODO Auto-generated method stub
		Log.e("james1",obj.get("relTime") + "");
		Log.e("james1",fromDateStringToInt((String) obj.get("relTime")) + "");
		int positionInt = (int) fromDateStringToInt((String) obj.get("relTime"));
		mTvPlayPosition = positionInt;
		if (!mSeekBarTouchMode && !mIsPlayOnPhone) {
			mMusicProgress.setProgress(positionInt);
			mNowPosition.setText(setDurationFormat(positionInt));
			postGetPositionInfo();
		}
	}

	
	@Override
	public void onGetSeekresult(Boolean obj) {
		Log.e("james","SeekBack :" + obj);
		// TODO Auto-generated method stub
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bt;
	}

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
}
