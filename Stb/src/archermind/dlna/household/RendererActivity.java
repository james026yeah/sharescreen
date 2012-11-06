package archermind.dlna.household;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import archermind.dlna.renderer.RendererProcess;
import archermind.airplay.AirplayProcess;
import archermind.ashare.R;

public class RendererActivity extends Activity {
	private final static String TAG = "RendererActivity";
	private boolean mIsBound = false;
	private Messenger mService = null;
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
		mIsBound = bindService(new Intent(RendererActivity.this, RendererService.class), 
        		mServConn, BIND_AUTO_CREATE);
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
        setContentView(R.layout.main); 
        Log.d(TAG, "onCreate()");        
        bind2RendererService();
    } 
    

    
    @Override
    public void onDestroy() {
        super.onDestroy();  
        Log.d(TAG, "onDestroy()");
        unbind2RendererService();
    }   

}