package archermind.dlna.mobile;

import com.archermind.ashare.dlna.localmedia.MusicItem;
import com.archermind.ashare.service.IMusicPlayService;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.List;

public class MusicListActivity extends BaseActivity {

	public static final int TIME_SECOND = 1000;
	public static final int TIME_MINUTE = TIME_SECOND * 60;
	public static final int TIME_HOUR = TIME_MINUTE * 60;
	
//	private final int ALL_MUSIC = 0;
//	private final int ARTIST = 1;
//	private final int ALBUM = 2;
	
//	private List<MusicItem> mMusicShowList;
//	private WeakReference<MusicData> mMusicData;
	
	private IMusicPlayService mMusicPlayService = null;
	private MusicListAdapter adapter = null;
	private ListView mMusicList;
	private TextView mTitle;
	private Animation mProgressBarAnim;
//	private static List<MusicItem> mAllMusic;
	
	private ServiceConnection mMusicSerConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			mMusicPlayService = null;
		}
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			mMusicPlayService = IMusicPlayService.Stub.asInterface(service);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_list_show);
		mMusicList = (ListView) findViewById(R.id.music_show_list);
		mTitle = (TextView) findViewById(R.id.list_title);
//		mMusicData = new MusicData();
		Intent intent = getIntent();
		mTitle.setText(intent.getStringExtra("title"));
		bindService(new Intent(IMusicPlayService.class.getName()),
				mMusicSerConn, Context.BIND_AUTO_CREATE);
		adapter = new MusicListAdapter(MusicData.getMusicShowList());
		mMusicList.setAdapter(adapter);
		mMusicList.setOnItemClickListener(new MusicListItemClickListener());
		if (getIntent().getBooleanExtra("scrollto", false)) {
			mMusicList.setSelection((int) MusicData.getNowPlayPositionInList());
		}
		mProgressBarAnim = AnimationUtils.loadAnimation(this, R.anim.progress_bar_anim);
		
		IntentFilter filter = new IntentFilter();
        filter.addAction(MusicUtils.Defs.MUSIC_STOP);
        filter.addAction(MusicUtils.Defs.MUSIC_INFO_REFRESH);
        registerReceiver(mReceiver, filter);
//		Message msg = new Message();
//		msg.what = 0;
//		handler.sendMessageDelayed(msg, 100);
	}

//	private Handler handler= new Handler(){
//		@Override 
//		public void handleMessage(Message msg) {
//			switch (msg.what) {
//			case 0:
//				try {
//					if (getIntent().getBooleanExtra("scrollto", false)) {
//						mMusicList.setSelection((int) MusicData.getNowPlayPositionInList());
//					}
//					
//				} catch (RemoteException e) {
//					e.printStackTrace();
//				}
//				break;
//
//			default:
//				break;
//			}
//		}
//	};

//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		if (requestCode == 0) {
//			finish();
//		}
//	};
	
	class MusicListItemClickListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long arg3) {
			try {
				if (MusicData.getMusicShowList().get(position) != MusicData.getNowPlayingMusic()){
					MusicData.setMusicPlayList(MusicData.getMusicShowList());
					mMusicPlayService.playFrom(position);
					if (LocalMediaUtil.getWhichOnRemote() == LocalMediaUtil.Defs.MUSIC) {
						mMusicPlayService.postToRemote();
					} else {
						mMusicPlayService.play();
					}
//						mMusicPlayService.postToRemote();
				}
				Intent intent = new Intent();
				intent.setClass(getApplicationContext(), MusicPlayActivity.class);
				startActivity(intent);
				overridePendingTransition(R.anim.pull_right_in, R.anim.pull_left_out);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	class MusicListAdapter extends BaseAdapter {
		private List<MusicItem> mAllMusic;
		
		MusicListAdapter(List<MusicItem> AllMusicItem) {
			mAllMusic = AllMusicItem;
		}
		
		public int getCount() {
			return mAllMusic.size();
		}
		
		public Object getItem(int position) {
			return position;
		}
		
		public long getItemId(int position) {
			return position;
		}
		
		public View getView(final int position, View convertView, ViewGroup parent) {
			
			View view;
			ImageView img;
			TextView titlemain;
			TextView titlesec;
			TextView detail;
			ImageView statusAroundImg;
			FrameLayout statusImg;
			
			if (convertView == null) {
				view = getLayoutInflater().inflate(R.layout.music_list_item, null);
			}
			else {
				view = convertView;
			}
			img = (ImageView) view.findViewById(R.id.img);
			statusAroundImg = (ImageView) view.findViewById(R.id.list_play_status_around);
			statusImg = (FrameLayout) view.findViewById(R.id.list_play_status);
			statusImg.setVisibility(View.GONE);
			if (MusicData.getMusicShowList().get(position) == MusicData.getNowPlayingMusic()) {
				statusImg.setVisibility(View.VISIBLE);
			}
			statusAroundImg.startAnimation(mProgressBarAnim);
			img.setImageBitmap(null);
			img.setBackgroundDrawable(getResources().getDrawable(R.drawable.icon_music));
			titlemain = (TextView) view.findViewById(R.id.title_main);
			titlesec = (TextView) view.findViewById(R.id.title_sec);
			detail = (TextView) view.findViewById(R.id.detail);

			titlemain.setText(mAllMusic.get(position).getTitle());
			titlesec.setText(mAllMusic.get(position).getArtist());
			detail.setText(setDurationFormat(Integer.parseInt(mAllMusic.get(position).getDuration())));
			if (getMusicImg(mAllMusic.get(position).getAlbumArtURI()) != null) {
				img.setImageBitmap(getMusicImg(mAllMusic.get(position).getAlbumArtURI()));
			}
			return view;
		}
	}
	
	@Override
	public void onDestroy() {
	    unbindService(mMusicSerConn);
	    super.onDestroy();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public void doClick(View view) {
		switch (view.getId()) {
		case R.id.back_arrow:
			finish();
			overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
			break;

		default:
			break;
		}
	}
	
	public static String setDurationFormat(int duration) {

		int hour = duration / TIME_HOUR;
		int minute = (duration - hour * TIME_HOUR) / TIME_MINUTE;
		int second = (duration - hour * TIME_HOUR - minute * TIME_MINUTE)
				/ TIME_SECOND;

		if (hour == 0) {
			return String.format("%02d:%02d", minute, second);
		} else {
			return String.format("%02d:%02d:%02d", hour, minute, second);
		}
	}
	
	public Bitmap getMusicImg(String mAlbumArtURI) {
        File file = new File(mAlbumArtURI);
        WeakReference<Bitmap> bt = null;
        try {
			ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
			if (BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, null) != null){
				bt = new WeakReference<Bitmap>(BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, null));
				return bt.get();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("james","onreceive stop");
            mMusicList.invalidateViews();
        }
    };
	
    protected void onResume() {
        super.onResume();
        mMusicList.invalidateViews();
    };
//	public List<MusicItem> getMusicShowItem() {
//		int which = mMusicData.getShowList();
//		switch (which) {
//		case ALL_MUSIC:
//			return mMusicData.getAllMusicData();
//		case ARTIST:
//			return mMusicData.getMusicArtistsData().get(getIntent().);
//		case ALBUM:
//			return mMusicData.getMusicAlbumData();
//		}
//		return null;
//	}
}