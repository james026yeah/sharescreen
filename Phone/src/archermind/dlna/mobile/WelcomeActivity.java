package archermind.dlna.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.archermind.ashare.dlna.localmedia.LocalMediaDbHelper;
import com.archermind.ashare.dlna.localmedia.MediaCache;

public class WelcomeActivity extends Activity {
    private final static String TAG = "WelcomeActivity";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.welcome);
        if(MediaCache.instance().isInitialized()) {
            Log.v(TAG, "MediaCache is initialized so jump!");
            gotoMobileDLNAActivity();
        } else {
            Log.v(TAG, "MediaCache is not initialized so start init task!");
            new InitMediaCacheTask().execute();
        }
    }

    private class InitMediaCacheTask extends AsyncTask<Void, Void, Void > {
        @Override
        protected void onPostExecute(Void result) {
            Log.v(TAG, "InitMediaCacheTask MediaCache is initialized so jump");
            gotoMobileDLNAActivity();
        }
        @Override
        protected Void doInBackground(Void... params) {
            MediaCache.instance().init(new LocalMediaDbHelper(
                    WelcomeActivity.this.getApplicationContext()));
            return null;
        }
    }

    private void gotoMobileDLNAActivity() {
        Intent tostart = new Intent(WelcomeActivity.this, MobileDLNAActivity.class);
        tostart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(tostart);
        WelcomeActivity.this.finish();
    }
}
