package archermind.dlna.mobile;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class MobileApplication extends Application {
	private final static String TAG = "MobileApplication";
	private Messenger mService = null;
	private boolean mIsBound = false;
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			default:
				super.handleMessage(msg);
			}
		}
	}
	@Override
	public void onCreate() {
		mIsBound = bindService(new Intent(this, DLNAService.class), 
				mServConn, BIND_AUTO_CREATE);
		Log.d(TAG, "mIsBound: " + mIsBound);
	}
	
	public void unbindDLNAService() {
		if (mIsBound && (mService != null)) {
			try {
				Message msg = Message.obtain(null,
						MessageDefs.MSG_SERVICE_UNREGISTER_CLIENT);
				msg.replyTo = mMessenger;
				mService.send(msg);
			} catch (RemoteException e) {
			}
			Log.d(TAG, "unbind DLNAService!");
			unbindService(mServConn);
			mIsBound = false;
		}
	}
	
	private ServiceConnection mServConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "onServiceConnected!");
			mService = new Messenger(service);
			try {
			    Message msg = Message.obtain(null,
			            MessageDefs.MSG_SERVICE_REGISTER_CLIENT);
			    msg.replyTo = mMessenger;
			    mService.send(msg);
			} catch (RemoteException e) {
			} 
		}
		@Override
		public void onServiceDisconnected(ComponentName name) {	
			Log.d(TAG, "onServiceDisconnected!");	
		}	
	};
}
