package archermind.dlna.mobile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.archermind.ashare.dlna.localmedia.LocalMediaDbHelper;
import com.archermind.ashare.dlna.localmedia.MediaCache;

public class WelcomeActivity extends Activity {
    private final static String TAG = "WelcomeActivity";
    private final static int MIN_WELCOME_DURATION = 2100;
    private Handler mHandler;
    private View mProgress;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.welcome);
        mHandler = new Handler();
        mProgress = findViewById(R.id.progress_indicator);
        if(MediaCache.instance().isInitialized()) {
            Log.v(TAG, "MediaCache is initialized so jump!");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    gotoMobileDLNAActivity();
                }
            }, MIN_WELCOME_DURATION);
            mProgress.setBackgroundResource(R.anim.loading_progress_anim);
            AnimationDrawable frameAnimation = (AnimationDrawable) mProgress.getBackground();
            frameAnimation.start();
        } else {
            Log.v(TAG, "MediaCache is not initialized so start init task!");
            mProgress.setBackgroundResource(R.drawable.welcome_progress_0);
            new InitMediaCacheTask().execute();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        mProgress.setBackgroundDrawable(null);
    }

    private class InitMediaCacheTask extends AsyncTask<Void, Integer, Void > {
        @Override
        protected void onPostExecute(Void result) {
            Log.v(TAG, "InitMediaCacheTask MediaCache is initialized so jump");
            gotoMobileDLNAActivity();
        }
        @Override
        protected Void doInBackground(Void... params) {
            MediaCache.instance().setMediaLoadListener(new MediaCache.MediaLoadProgressListener() {
                @Override
                public void onUpdateProgress(int progress) {
                    publishProgress(progress);
                }
            });
            MediaCache.instance().init(new LocalMediaDbHelper(
                    WelcomeActivity.this.getApplicationContext()));
            MediaCache.instance().setMediaLoadListener(null);
            return null;
        }
        @Override
        protected void onProgressUpdate(Integer... progress) {
            int iProgress = progress[0];
            switch(iProgress) {
                case 0:
                    mProgress.setBackgroundResource(R.drawable.welcome_progress_1);
                    break;
                case 20:
                    mProgress.setBackgroundResource(R.drawable.welcome_progress_2);
                    break;
                case 40:
                    mProgress.setBackgroundResource(R.drawable.welcome_progress_3);
                    break;
                case 60:
                    mProgress.setBackgroundResource(R.drawable.welcome_progress_4);
                    break;
                case 80:
                    mProgress.setBackgroundResource(R.drawable.welcome_progress_5);
                    break;
                case 100:
                    mProgress.setBackgroundResource(R.drawable.welcome_progress_6);
                    break;
                default:
                    break;
            }
            //setProgressPercent(progress[0]);
        }
    }

    private void gotoMobileDLNAActivity() {
        Intent tostart = new Intent(WelcomeActivity.this, MobileDLNAActivity.class);
        tostart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(tostart);
        WelcomeActivity.this.finish();
    }
}
