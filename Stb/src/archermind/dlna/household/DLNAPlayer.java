package archermind.dlna.household;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.widget.VideoView;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.SurfaceHolder;
import archermind.airplay.AirplayProcess;
import archermind.ashare.R;

import com.archermind.ashare.TypeDefs;

public class DLNAPlayer extends Activity {
	//public final static int MSG_AV_TRANS_TYPE_NONE = 0;
	//public final static int MSG_AV_TRANS_TYPE_VIDIO = 1;
	//public final static int MSG_AV_TRANS_TYPE_AUDIO = 2;
	//public final static int MSG_AV_TRANS_TYPE_IMAGE = 3;
    private static final String TAG = "DLNAPlayer";
    //public final static String KEY_MEDIA_URI = "MediaURI";
    //public final static String KEY_MEDIA_TYPE = "MediaTYPE";
    private int mVideoWidth;
    private int mVideoHeight;
    private VideoView mMediaPlayer;
    private String mUri;    
    
    public static boolean mIsPlay = false;
    public static boolean mIsPause = false;
    public static int mVolume = 0;
    public static long mTotalTime = 0;
    public static long mCurrentPosition = 0;
    public static boolean mIsPlayCompletion = true;

    private boolean mIsBound = false;
	private Messenger mService = null;
	final Messenger mMessenger = new Messenger(new IncomingHandler());

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
          switch (msg.what) {
            case AirplayProcess.MSG_AIRPLAY_PLAY_TO_PAUSE:
            case TypeDefs.MSG_DMR_AV_TRANS_PLAY_TO_PAUSE:
            	mMediaPlayer.pause();
            	mIsPause = true;
            	mIsPlay = false;
				break;  
            case AirplayProcess.MSG_AIRPLAY_SEEK:
            case TypeDefs.MSG_DMR_AV_TRANS_SEEK:
            	mMediaPlayer.seekTo(msg.arg1);
				break;  
            case AirplayProcess.MSG_AIRPLAY_STOP:
            case TypeDefs.MSG_DMR_AV_TRANS_STOP:
            	mIsPlayCompletion = true;
            	mMediaPlayer.stopPlayback();
            	mIsPause = false;
            	mIsPlay = false;
            	finish();
				break;  
            case TypeDefs.MSG_DMR_AV_TRANS_SET_VOLUME:
            	mMediaPlayer.setVolume((float)msg.arg1, (float)msg.arg1);
            	mVolume = msg.arg1;
				break;  
            case AirplayProcess.MSG_AIRPLAY_PAUSE_TO_PLAY:
            case TypeDefs.MSG_DMR_AV_TRANS_PAUSE_TO_PLAY:
            	if(DLNAPlayer.mIsPlayCompletion)
    	    		break;
            	mMediaPlayer.start();
            	mIsPlay = true;
            	mIsPause = false;
				break;  
            case AirplayProcess.MSG_AIRPLAY_UPDATEDATA:
            case TypeDefs.MSG_DMR_RENDERER_UPDATEDATA:
            	Updatedata();
				break;  
            case TypeDefs.MSG_DMR_AV_TRANS_SET_MUTE:
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
		mIsBound = bindService(new Intent(DLNAPlayer.this, RendererService.class), 
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
    public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
			return;

		setContentView(R.layout.player);
		mMediaPlayer = (VideoView) findViewById(R.id.surface);
		//mMediaPlayer.setMediaController(new MediaController(this));
		Bundle data = getIntent().getExtras();
		if(null != data) {
			mUri = data.getString(TypeDefs.KEY_MEDIA_URI);
			Log.d(TAG, "Media URI:" + mUri);
		mMediaPlayer.setOnCompletionListener(mCompletionListener);
		mMediaPlayer.setVideoURI(Uri.parse(mUri));
		mMediaPlayer.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
		//String tmp = getFileName(mUri);
		//Log.v("EagleTag","getFileName:"+tmp);
		mIsPlayCompletion = false;
		}
		bind2RendererService();
     }
    
    private OnCompletionListener mCompletionListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mp) {
			mIsPlayCompletion = true;
			finish();
		}
	};
    
    public void onDestroy() {
    	mVideoWidth = 0;
        mVideoHeight = 0;
        mIsPlay = false;
        mIsPause = false;
        mVolume = 0;
        mTotalTime = 0;
        mCurrentPosition = 0;
        super.onDestroy();  
        Log.d(TAG, "onDestroy()");
        unbind2RendererService();
    }
    
    public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
        Log.d(TAG, "surfaceChanged called");

    }

    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
        Log.d(TAG, "surfaceDestroyed called");
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated called");
    }
   
    public void Updatedata()
    {
    	if(mIsPlayCompletion)
    		return;
    	mVideoWidth = mMediaPlayer.getWidth();
    	mVideoHeight = mMediaPlayer.getHeight();
    	mIsPlay = mMediaPlayer.isPlaying();
        mTotalTime = mMediaPlayer.getDuration();
        mCurrentPosition = mMediaPlayer.getCurrentPosition();
    }
    
    public String getFileName(String mUri){
        int start = mUri.lastIndexOf("/");
        int end = mUri.lastIndexOf(".");
        if (start != -1 && end != -1) {
            return mUri.substring(start+1, end);
        }
        else {
            return null;
        }
    }
    
    public static long fromDateStringToInt(String inVal)
    {
        Date date = null;
        SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm:ss");
        try {
            date = inputFormat.parse(inVal); //将字符型转换成日期型
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date.getTime();//返回毫秒数
    }

    public static String fromIntToDateString(long inVal) {
        //输入的是相对时间的 秒差
        Date date = new Date(inVal);
        SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm:ss");
         return inputFormat.format(date);
        }

}