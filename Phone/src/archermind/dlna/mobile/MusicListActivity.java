package archermind.dlna.mobile;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.archermind.ashare.dlna.localmedia.MusicItem;
import com.archermind.ashare.service.IMusicPlayService;

public class MusicListActivity extends Activity {

	public static final int TIME_SECOND = 1000;
	public static final int TIME_MINUTE = TIME_SECOND * 60;
	public static final int TIME_HOUR = TIME_MINUTE * 60;
	
	private final int ALL_MUSIC = 0;
	private final int ARTIST = 1;
	private final int ALBUM = 2;
	
	private List<MusicItem> mMusicShowList;
	private MusicData mMusicData;
	
	private IMusicPlayService mMusicPlaySer = null;
	private MusicListAdapter adapter = null;
	private ListView mMusicList;
	private TextView mTitle;
	private static List<MusicItem> mAllMusic;
	
	private ServiceConnection mMusicSerConn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			mMusicPlaySer = null;
			
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			mMusicPlaySer = IMusicPlayService.Stub.asInterface(service);
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		setContentView(R.layout.music_list_show);
		mMusicList = (ListView) findViewById(R.id.music_show_list);
		mTitle = (TextView) findViewById(R.id.list_title);
		mMusicData = new MusicData();
		Intent intent = getIntent();
		mTitle.setText(intent.getStringExtra("title"));
		bindService(new Intent(IMusicPlayService.class.getName()),
				mMusicSerConn, Context.BIND_AUTO_CREATE);
		Message msg = new Message();
		msg.what = 0;
		handler.sendMessageDelayed(msg, 100);
		super.onCreate(savedInstanceState);
	}

	private Handler handler= new Handler(){
		@Override 
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0:
				try {
					adapter = new MusicListAdapter(mMusicData.getMusicShowList());
					mMusicList.setAdapter(adapter);
					if (getIntent().getBooleanExtra("scrollto", false)) {
						mMusicList.setSelection((int) mMusicPlaySer.getQueuePosition());
					}
					mMusicList.setOnItemClickListener(new OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1, int position,
								long arg3) {
							// TODO Auto-generated method stub
							try {
								if (mMusicPlaySer.getPlayOnPhone()) {
									mMusicPlaySer.setPlayList(mMusicData.getMusicShowList());
									mMusicPlaySer.playFrom(position);
									mMusicPlaySer.play();
								}
								Intent intent = new Intent();
								intent.setClass(getApplicationContext(), MusicPlayActivity.class);
								startActivity(intent);
								overridePendingTransition(R.anim.pull_right_in, R.anim.pull_left_out);
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;

			default:
				break;
			}
		}
	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0) {
			finish();
		}
	};
	
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
			
			Bitmap bm = null;

			ContentResolver res = getApplicationContext().getContentResolver();
			Uri uri = Uri.parse(mAllMusic.get(position).getAlbumArtURI());
//			BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
//			bm = BitmapFactory.decodeFile(uri);
//			if (uri != null) {
//				InputStream in = null;
//				try {
//					Log.e("james","goin");
//					in = res.openInputStream(uri);
//					bm = BitmapFactory.decodeStream(in);
//				} catch (FileNotFoundException ex) {
//					Log.e("james","fileNotfound");
//				} finally {
//					try {
//						if (in != null) {
//							in.close();
//						}
//					} catch (IOException ex) {
//					}
//				}
//			}
			if (convertView == null) {
				view = getLayoutInflater().inflate(R.layout.music_list_item, null);
			}
			else {
				view = convertView;
			}
			img = (ImageView) view.findViewById(R.id.img);
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
