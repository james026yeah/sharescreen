package archermind.dlna.mobile;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import archermind.dlna.media.MusicItem;

public class MusicPlayActivity extends BaseActivity implements
		OnClickListener {

	private TextView mArtistTV;
	private TextView mTitleTV;
	private IMusicPlayService mMusicPlaySer = null;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			try {
				if (mMusicPlaySer.isPlaying()) {
					Log.e("james","received");
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.music_play);
		bindService(new Intent(IMusicPlayService.class.getName()), mMusicSerConn,
				Context.BIND_AUTO_CREATE);
		mArtistTV = (TextView) findViewById(R.id.artist);
		mTitleTV= (TextView) findViewById(R.id.musictitle);

		IntentFilter filter = new IntentFilter();
		filter.addAction("statuschanged");
		registerReceiver(mReceiver, filter);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unbindService(mMusicSerConn);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
	}

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
	
	
	public void doClick(View view) throws RemoteException {
		switch (view.getId()) {
		case R.id.pause_play:
			mMusicPlaySer.pause();
			Log.e("james","pause");
			break;
		case R.id.stop:
			mMusicPlaySer.stop();
			break;
		case R.id.next:
			mMusicPlaySer.next();
			break;
		case R.id.prev:
			mMusicPlaySer.prev();
			break;
		case R.id.post:
			break;
		}
		
	}
}
