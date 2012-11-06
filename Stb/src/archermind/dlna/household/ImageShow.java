package archermind.dlna.household;

import com.archermind.ashare.TypeDefs;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.ImageView;
import archermind.dlna.renderer.RendererProcess;
import archermind.airplay.AirplayProcess;
import archermind.ashare.R;

public class ImageShow extends Activity {
	private final static String TAG = "ImageShow";
	private String mMediaURI;
	private boolean mIsBound = false;
	private Messenger mService = null;
	private ImageView image;
	private int mMediaType;
	final Messenger mMessenger = new Messenger(new IncomingHandler());

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
          switch (msg.what) {
            case AirplayProcess.MSG_AIRPLAY_SHOW_PHOTO:
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
		mIsBound = bindService(new Intent(ImageShow.this, RendererService.class), 
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
        setContentView(R.layout.image_show);
        image = (ImageView)findViewById(R.id.img_view);
        Log.d(TAG, "onCreate()");        
        bind2RendererService();
        
        mMediaType = getIntent().getExtras().getInt(TypeDefs.KEY_MEDIA_TYPE);
        if(mMediaType == TypeDefs.MEDIA_TYPE_AIRPLAY_IMAGE)
        {
        	byte[] imgbytes = getIntent().getExtras().getByteArray(TypeDefs.KEY_AIRPLAY_IMAGE_DATA);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imgbytes, 0, imgbytes.length);
            image.setImageBitmap(bitmap);
        }else if(mMediaType == TypeDefs.MEDIA_TYPE_DLNA_IMAGE)
        {
        	mMediaURI = getIntent().getExtras().getString(TypeDefs.KEY_MEDIA_URI);
        	image.setImageURI(Uri.parse(mMediaURI));
        }
        
    } 
    
    @Override  
    protected void onPause() {  
        // TODO Auto-generated method stub  
        System.out.println("FirstAcvity --->onPause");  
        super.onPause();  
    }  
  
    @Override  
    protected void onRestart() {  
        // TODO Auto-generated method stub  
        System.out.println("FirstAcvity --->onRestart");  
        super.onRestart();  
    }  
  
    @Override  
    protected void onResume() {  
        // TODO Auto-generated method stub  
        System.out.println("FirstAcvity --->onResume");  
        super.onResume();  
    }  
  
    @Override  
    protected void onStart() {  
        // TODO Auto-generated method stub  
        System.out.println("FirstAcvity --->onStart");  
        super.onStart();  
    }  
  
    @Override  
    protected void onStop() {  
        // TODO Auto-generated method stub  
        System.out.println("FirstAcvity --->onStop");  
        super.onStop();  
    }  
    
    @Override
    public void onDestroy() {
        super.onDestroy();  
        Log.d(TAG, "onDestroy()");
        unbind2RendererService();
    }   

}