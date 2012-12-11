package archermind.dlna.household;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import archermind.airplay.AirplayProcess;
import archermind.ashare.R;

import com.archermind.ashare.TypeDefs;

public class ImageShow extends Activity {
    private final static String TAG = "ImageShow";
    //private PhotoItem mPhotoInfo;
    private String mMediaURI;
    private boolean mIsBound = false;
    private Messenger mService = null;
    private ImageView mImageView;
    private int mMediaType;
    private Bitmap mBitmap;
    private Bitmap mChangeBitmap;
    private float mFlip = 0;
    private float mScaling = 1;
    private Handler mHandler = new IncomingHandler(this);
    DisplayMetrics mDm;
	private DownloadPhotoBackground mPhotoDownloadTask;
	private final Messenger mMessenger = new Messenger(mHandler);

	class IncomingHandler extends Handler {
		ImageShow mImageShow;

		public IncomingHandler(ImageShow imageShow) {
			mImageShow = imageShow;
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case TypeDefs.MSG_DMR_AV_TRANS_SEEK:
				if (msg.arg2 == TypeDefs.MEDIA_TYPE_DLNA_IMAGE) {
					if (msg.arg1 == TypeDefs.IMAGE_CONTROL_FLIP) {
					    mImageShow.mFlip = Float.valueOf(msg.obj.toString());
					} else if (msg.arg1 == TypeDefs.IMAGE_CONTROL_SCALING) {
					    mImageShow.mScaling = Float.valueOf(msg.obj.toString());
					}
					mImageShow.ShowChangedImage(mImageShow.mFlip, mImageShow.mScaling);
				}
				break;
            case TypeDefs.MSG_DMR_RENDERER_UPDATEDATA:
                if(msg.arg2 == TypeDefs.MEDIA_TYPE_DLNA_IMAGE) {
                    if(msg.arg1 == TypeDefs.IMAGE_CONTROL_FLIP) {
                        mImageShow.mFlip = Float.valueOf(msg.obj.toString());
                    } else if(msg.arg1 == TypeDefs.IMAGE_CONTROL_SCALING) {
                        mImageShow.mScaling = Float.valueOf(msg.obj.toString());
                    }
                }
                break;
            case TypeDefs.MSG_DMR_AV_TRANS_STOP:
            case AirplayProcess.MSG_AIRPLAY_STOP:
				finish();
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
        mImageView = (ImageView)findViewById(R.id.img_view);
        bind2RendererService();
        mDm = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(mDm);
        Log.v(TAG, "dm width:" + mDm.widthPixels + ", height:" + mDm.heightPixels);
        showPhoto(this.getIntent());
    }

    public class DownloadPhotoBackground extends AsyncTask<String, Void, Bitmap>{
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
        @Override
        protected Bitmap doInBackground(String... params) {
            Log.v(TAG, "doInBackground ......");
            long startTime, stopTime;
            
            Bitmap bitmap = null;
            URL myFileUrl = null;
            InputStream is = null;
            try {
                startTime = System.currentTimeMillis();
                myFileUrl = new URL(params[0]);
                BitmapFactory.Options options = new BitmapFactory.Options();
                is = myFileUrl.openConnection().getInputStream();
                byte[] data = getBytes(is);
                stopTime = System.currentTimeMillis();
                Log.v(TAG, "get image data cost time: " + (stopTime - startTime) + "ms");

                // Step1: Get resolution of the bitmap
                startTime = System.currentTimeMillis();
                options.inJustDecodeBounds = true;
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                Log.v(TAG, "bitmap height:" + options.outHeight +
                        ", width=" + options.outWidth);
                stopTime = System.currentTimeMillis();
                Log.v(TAG, "get image resolution: " + (stopTime - startTime) + "ms");

                // Step2: Get the actual bitmap
                startTime = System.currentTimeMillis();
                options.inJustDecodeBounds = false;
                float scaleWidth = (float)options.outWidth / (float)mDm.widthPixels;
                float scaleHeight = (float)options.outHeight / (float)mDm.heightPixels;
                Log.v(TAG, "scaleWidth: " + scaleWidth + ", scaleHeight:" + scaleHeight);
                float maxScaleRate = Math.max(scaleWidth, scaleHeight);
                if(maxScaleRate > 1.0f) {
                    options.inSampleSize = (int)maxScaleRate;
                    Log.v(TAG, "maxScaleRate: " + maxScaleRate + 
                            ", inSampleSize:" + options.inSampleSize);
                }
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                stopTime = System.currentTimeMillis();
                Log.v(TAG, "build bitmap cost: " + (stopTime - startTime) + "ms");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            if (result != null) {
                mBitmap = result;
                ImageView.ScaleType scaleType = ImageView.ScaleType.FIT_CENTER;
                if(mBitmap != null && (mBitmap.getHeight() <= mDm.heightPixels) &&
                        (mBitmap.getWidth() <= mDm.widthPixels)) {
                    scaleType = ImageView.ScaleType.CENTER;
                }
                mImageView.setScaleType(scaleType);
                mImageView.setImageBitmap(mBitmap);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
		mFlip = 0;
		mScaling = 1;
        showPhoto(intent);
    }
    
    private void showPhoto(Intent intent) {
        mMediaType = intent.getExtras().getInt(TypeDefs.KEY_MEDIA_TYPE);
        if(mMediaType == TypeDefs.MEDIA_TYPE_AIRPLAY_IMAGE) {
            byte[] imgbytes = intent.getExtras().getByteArray(TypeDefs.KEY_AIRPLAY_IMAGE_DATA);
            mBitmap = BitmapFactory.decodeByteArray(imgbytes, 0, imgbytes.length);
            mImageView.setImageBitmap(mBitmap);
        } else if(mMediaType == TypeDefs.MEDIA_TYPE_DLNA_IMAGE) {
            Log.v(TAG, "onNewIntent --- decode bitmap");
            //mPhotoInfo = intent.getExtras().getParcelable(TypeDefs.KEY_CURR_MEDIA_INFO);
            mMediaURI = intent.getExtras().getString(TypeDefs.KEY_MEDIA_URI);
            mPhotoDownloadTask = new DownloadPhotoBackground();
            mPhotoDownloadTask.execute(mMediaURI);
        }
    }

	private void ShowChangedImage(float flip, float scaling) {
	    Log.v(TAG, "flip:" + flip + "m scaling:" + scaling);
		if (mBitmap != null) {
			Matrix matrix = new Matrix();
			matrix.setScale(scaling, scaling);
				mChangeBitmap = Bitmap.createBitmap(mBitmap, 0, 0,
						mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
			matrix = new Matrix();
			matrix.setRotate(flip);
				mChangeBitmap = Bitmap.createBitmap(mChangeBitmap, 0, 0,
						mChangeBitmap.getWidth(), mChangeBitmap.getHeight(),
						matrix, true);
			mImageView.setImageBitmap(mChangeBitmap);
			/*if (mChangeBitmap != null) {
				mChangeBitmap.recycle();
				mChangeBitmap = null;
			}*/
		}
	}
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        unbind2RendererService();
        if(mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    private byte[] getBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        int len = 0;
        while ((len = is.read(b, 0, 1024)) != -1) {
            baos.write(b, 0, len);
            baos.flush();
        }
        byte[] bytes = baos.toByteArray();
        return bytes;
    }  
}