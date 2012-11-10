package archermind.dlna.mobile;

import android.os.Bundle;
import android.util.Log;

public class ConfigTVActivity extends BaseActivity {

	private static final String TAG = "ConfigTVActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate: ");

		setCustomContentView(R.layout.config_tv_layout, true);
		setCustomTitle(0, 0, 0, 0);
	}
	protected void onServiceConnected() {
		stbDisconnectWifi();
	}
}
