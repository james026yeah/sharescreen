package archermind.dlna.household;

import java.io.DataInputStream;
import java.io.IOException;

import com.archermind.ashare.TypeDefs;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import archermind.airtunes.AirtunesProcess;

public class AirTunesPlayer extends Activity {
	private final static String TAG = "AirTunesPlayer";
	private Handler mHandler = new IncomingHandler();
	private Messenger mService = null;
	private boolean mIsBound = false;
	private final Messenger mMessenger = new Messenger(mHandler);
	protected AudioPlayer m_player = new AudioPlayer();

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case TypeDefs.MSG_DMR_AV_TRANS_SEEK:
				break;
			case TypeDefs.MSG_DMR_RENDERER_UPDATEDATA:
				break;
			default:
				super.handleMessage(msg);
				break;
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
		mIsBound = bindService(new Intent(AirTunesPlayer.this,
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
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bind2RendererService();
		m_player.init();
		m_player.start();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	public class AudioPlayer extends Thread {

		protected AudioTrack m_out_trk;
		protected int m_out_buf_size;
		protected byte[] m_out_bytes;
		protected boolean m_keep_running;
		private DataInputStream din;
		public final static int MSG_AIRTUENS_WRITE_BUF = 300;
		byte[] bytes_pkg = null;

		public synchronized void audioPlay() {

			if (AirtunesProcess.backStack.size() >= 2) {
				bytes_pkg = AirtunesProcess.backStack.poll();

				if (bytes_pkg == null || bytes_pkg.length == 0) {

				} else {
				}
				m_out_trk.write(bytes_pkg, 0, bytes_pkg.length);
			}
		}

		public void init() {
			try {
				m_keep_running = true;
				m_out_buf_size = AudioTrack.getMinBufferSize(44100,
						AudioFormat.CHANNEL_CONFIGURATION_STEREO,
						AudioFormat.ENCODING_PCM_16BIT);

				m_out_trk = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
						AudioFormat.CHANNEL_CONFIGURATION_STEREO,
						AudioFormat.ENCODING_PCM_16BIT, m_out_buf_size,
						AudioTrack.MODE_STREAM);

				m_out_bytes = new byte[m_out_buf_size];

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void free() {
			m_keep_running = false;
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				Log.d("sleep exceptions...\n", "");
			}
		}

		public void run() {
			m_out_trk.play();
			while (m_keep_running) {
				try {

					audioPlay();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			//AirtunesProcess.available = false;
			//notifyAll();
			m_out_trk.stop();
			m_out_trk = null;
			try {
				din.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbind2RendererService();
	}
}
