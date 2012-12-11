package archermind.dlna.mobile;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.VideoView;

public class SettingActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	public static final String PREFS_SHOW_GUIDE_PAGE = "showGuidePage";
	
	public static final String PREFS_IGNORE_VERSION = "ignoreVersion";

	private EditTextPreference mLocalServiceName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
		setContentView(R.layout.set_preference_main);
		
		findViewById(R.id.top_bar_right_line).setVisibility(View.GONE);

		findViewById(R.id.image_left_top).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
				overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
			}
		});
		
		TextView title = (TextView) findViewById(R.id.title);
		title.setText(R.string.setting_title);
		
		mLocalServiceName = (EditTextPreference) getPreferenceScreen()
				.findPreference(
						getResources().getString(
								R.string.setting_local_service_name));
		
		mLocalServiceName.setSummary(getPreferenceScreen().getSharedPreferences().getString(
				getResources().getString(R.string.setting_local_service_name), 
				getResources().getString(R.string.setting_local_service_name_default)));
		
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		Log.d("SettingActivity", "onSharedPreferenceChanged key: " + key);
		if (key.equals(getResources().getString(R.string.setting_local_service_name))) {
			mLocalServiceName.setSummary(sharedPreferences.getString(key, 
					getResources().getString(R.string.setting_local_service_name_default)));
		}
		if (key.equals(getResources().getString(R.string.setting_use_notification_key))) {
			NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
			nm.cancel(R.string.notification_content_txt);
		}
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference.getKey() != null
				&& preference.getKey().equals("setting_share_apk")) {
			Intent sendIntent = new Intent();
			sendIntent.setAction(Intent.ACTION_SEND);
			sendIntent.putExtra(Intent.EXTRA_TEXT, "http://www.archermind.com");
			sendIntent.setType("text/plain");
			startActivity(sendIntent);
		}
		if (preference.getKey() != null
				&& preference.getKey().equals("setting_about")) {
			// Intent sendIntent = new Intent(this, AboutActivtiy.class);
			// startActivity(sendIntent);
		}

		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
	
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d("MoreOptionsActivity", "keyCode: " + keyCode);
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		Log.d("dispatchKeyEvent", "keyCode: " + event.getKeyCode());
		return super.dispatchKeyEvent(event);
	}
}
