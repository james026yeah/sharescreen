package archermind.dlna.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class WelcomeActivity extends BaseActivity {
	private final static int WELCOME_TIME = 3000;
	private Handler mHandler = new Handler();
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.welcome);		
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				Intent tostart = new Intent(WelcomeActivity.this, MobileDLNAActivity.class);
				tostart.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(tostart);
				WelcomeActivity.this.finish();
			}
		}, WELCOME_TIME);
	}
}
