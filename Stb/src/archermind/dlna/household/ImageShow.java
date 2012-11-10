package archermind.dlna.household;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import archermind.ashare.R;
import com.archermind.ashare.TypeDefs;

public class ImageShow extends Activity {
	private final static String TAG = "ImageShow";
	private String mMediaURI;
	private boolean mIsBound = false;
	private Messenger mService = null;
	private ImageView image;
	private int mMediaType;
	private Bitmap mBitmap;
	private Context mContext;
	//private DownloadPhoto mDownloadPhoto;
	private Handler xxHandler = new IncomingHandler();
	private RelativeLayout mImageTopLayout;
	private RelativeLayout mImageBottomLayout;
	private Animation hideAnimation;
	private Handler mDismissHandler;
	private final static int HIDE_CONTROL_LAYOUT = 100;
	private final static int DEFAULT_HIDE_TIME = 4000;
	private DownloadPhotoBackground mPhotoDownloadTask;
	final Messenger mMessenger = new Messenger(xxHandler);

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
          switch (msg.what) {
          case TypeDefs.MSG_DMR_AV_TRANS_SEEK:
          	if(msg.arg2 == TypeDefs.MEDIA_TYPE_DLNA_IMAGE)
          	{
          		if(msg.arg1 == TypeDefs.IMAGE_CONTROL_FLIP)
          		{
          			float FLIP = Float.valueOf(msg.obj.toString());
          		}
          		else if(msg.arg1 == TypeDefs.IMAGE_CONTROL_SCALING)
          		{
          			float SCALING = Float.valueOf(msg.obj.toString());
          		}
          	}
          		break;   
          case TypeDefs.MSG_DMR_RENDERER_UPDATEDATA:
            	if(msg.arg2 == TypeDefs.MEDIA_TYPE_DLNA_IMAGE)
            	{
            		if(msg.arg1 == TypeDefs.IMAGE_CONTROL_FLIP)
            		{
            			float FLIP = Float.valueOf(msg.obj.toString());
            		}
            		else if(msg.arg1 == TypeDefs.IMAGE_CONTROL_SCALING)
            		{
            			float SCALING = Float.valueOf(msg.obj.toString());
            		}
            	}
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
//        setContentView(R.layout.image_show);
		setContentView(R.layout.image_player);
        image = (ImageView)findViewById(R.id.img_view);
        mContext = this;
		mImageTopLayout = (RelativeLayout) findViewById(R.id.image_view_top_layout);
		mImageBottomLayout = (RelativeLayout) findViewById(R.id.image_view_bottom_layout);
        Log.d(TAG, "onCreate()");        
        bind2RendererService();
        mMediaType = getIntent().getExtras().getInt(TypeDefs.KEY_MEDIA_TYPE);
        if(mMediaType == TypeDefs.MEDIA_TYPE_AIRPLAY_IMAGE)
        {
        	byte[] imgbytes = getIntent().getExtras().getByteArray(TypeDefs.KEY_AIRPLAY_IMAGE_DATA);
        	mBitmap = BitmapFactory.decodeByteArray(imgbytes, 0, imgbytes.length);
            image.setImageBitmap(mBitmap);
        }else if(mMediaType == TypeDefs.MEDIA_TYPE_DLNA_IMAGE)
        {
        	mMediaURI = getIntent().getExtras().getString(TypeDefs.KEY_MEDIA_URI);
        	mPhotoDownloadTask = new DownloadPhotoBackground(mContext);
        	mPhotoDownloadTask.execute(mMediaURI);
        }
		initInfo();
    }
    
    public class DownloadPhotoBackground extends AsyncTask<String, Void, Bitmap>{

		private Context mContext;
		
		public DownloadPhotoBackground(Context context){
			mContext = context;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}
		@Override
		protected Bitmap doInBackground(String... params) {
			return returnBitMap(params[0]);
		}
		
		public Bitmap returnBitMap(String url) {
			URL myFileUrl = null;
			Bitmap bitmap = null;
			try {
				myFileUrl = new URL(url);
				HttpURLConnection conn;

				conn = (HttpURLConnection) myFileUrl.openConnection();

				conn.setDoInput(true);
				conn.connect();
				InputStream is = conn.getInputStream();
				bitmap = BitmapFactory.decodeStream(is);

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return bitmap;
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			if (result != null) {
				image.setImageBitmap(result);
			}
		}

	}
    
	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		if(mBitmap != null)
			mBitmap.recycle();
		mMediaType = getIntent().getExtras().getInt(TypeDefs.KEY_MEDIA_TYPE);
		if(mMediaType == TypeDefs.MEDIA_TYPE_AIRPLAY_IMAGE)
        {
        	byte[] imgbytes = intent.getExtras().getByteArray(TypeDefs.KEY_AIRPLAY_IMAGE_DATA);
        	mBitmap = BitmapFactory.decodeByteArray(imgbytes, 0, imgbytes.length);
            image.setImageBitmap(mBitmap);
        }else if(mMediaType == TypeDefs.MEDIA_TYPE_DLNA_IMAGE)
        {
        	mMediaURI = intent.getExtras().getString(TypeDefs.KEY_MEDIA_URI);
        	mPhotoDownloadTask = new DownloadPhotoBackground(mContext);
        	mPhotoDownloadTask.execute(mMediaURI);
        }
		show();
		super.onNewIntent(intent);
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

	private void initInfo() {
		mDismissHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case HIDE_CONTROL_LAYOUT:
					startHideAnimation();
					break;
				default:
					super.handleMessage(msg);
				}
			}
		};

		hideAnimation = AnimationUtils.loadAnimation(this, R.anim.player_out);
		hideAnimation.setAnimationListener(hideListener);

		mDismissHandler.sendEmptyMessageDelayed(HIDE_CONTROL_LAYOUT,
				DEFAULT_HIDE_TIME);
	}

	private void hide() {
		if (null != mImageTopLayout) {
			mImageTopLayout.setVisibility(View.GONE);
		}
		if (null != mImageBottomLayout) {
			mImageBottomLayout.setVisibility(View.GONE);
		}
	}

	private void show() {
		if (null != mImageTopLayout) {
			mImageTopLayout.setVisibility(View.VISIBLE);
		}
		if (null != mImageBottomLayout) {
			mImageBottomLayout.setVisibility(View.VISIBLE);
		}
		mDismissHandler.removeMessages(HIDE_CONTROL_LAYOUT);
		mDismissHandler.sendEmptyMessageDelayed(HIDE_CONTROL_LAYOUT,
				DEFAULT_HIDE_TIME);
	}

	private void startHideAnimation() {

		if (null != mImageTopLayout) {
			if (mImageTopLayout.getVisibility() == View.VISIBLE) {
				mImageTopLayout.startAnimation(hideAnimation);
			}
		}
		if (null != mImageBottomLayout) {
			if (mImageBottomLayout.getVisibility() == View.VISIBLE) {
				mImageBottomLayout.startAnimation(hideAnimation);
			}
		}
	}

	private AnimationListener hideListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation animation) {
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			hide();
		}
	};

}