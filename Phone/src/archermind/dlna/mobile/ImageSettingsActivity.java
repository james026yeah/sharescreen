package archermind.dlna.mobile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

public class ImageSettingsActivity extends PreferenceActivity {

	public static final String SLIDE_TIME_KEY = "slide";
	public static final String POST_TIME_KEY = "post";
	public static final String ROTATE_ANGLE_KEY = "rotate";

	private SharedPreferences mPreferences;
	private PreferenceManager mManager;
	private SharedPreferences.Editor mEditor;

	private ListPreference mSlideTime;
	private ListPreference mPostTime;
	private ListPreference mRotateAngle;

	private String[] mSlideTimeValues;
	private String[] mPostTimeValues;
	private String[] mRotateAngleValues;

	private String[] mSlideTimeSummary;
	private String[] mPostTimeSummary;
	private String[] mRotateAngleSummary;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.local_media_image_settings);
		initUI();
	}
	
	public void initUI(){

		mManager = getPreferenceManager();
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mEditor = mPreferences.edit();

		mSlideTime = (ListPreference) mManager.findPreference(SLIDE_TIME_KEY);
		mPostTime = (ListPreference) mManager.findPreference(POST_TIME_KEY);
		mRotateAngle = (ListPreference) mManager.findPreference(ROTATE_ANGLE_KEY);

		mSlideTimeValues = getResources().getStringArray(R.array.image_slide_time_values);
		mPostTimeValues = getResources().getStringArray(R.array.image_post_time_values);
		mRotateAngleValues = getResources().getStringArray(R.array.image_rotate_angle_values);

		mSlideTimeSummary = getResources().getStringArray(R.array.image_slide_time);
		mPostTimeSummary = getResources().getStringArray(R.array.image_post_time);
		mRotateAngleSummary = getResources().getStringArray(R.array.image_rotate_angle);

		for(int i = 0;i < mSlideTimeValues.length;i ++) {
			if(mSlideTimeValues[i].equals(mPreferences.getString(SLIDE_TIME_KEY,
					getResources().getString(R.string.image_slide_time_default_value)))) {
				mSlideTime.setSummary(mSlideTimeSummary[i]);
			}
		}

		for(int i = 0;i < mPostTimeValues.length;i ++) {
			if(mPostTimeValues[i].equals(mPreferences.getString(POST_TIME_KEY,
					getResources().getString(R.string.image_post_time_default_value)))) {
				mPostTime.setSummary(mPostTimeSummary[i]);
			}
		}

		for(int i = 0;i < mRotateAngleValues.length;i ++) {
			if(mRotateAngleValues[i].equals(mPreferences.getString(ROTATE_ANGLE_KEY,
					getResources().getString(R.string.image_rotate_angle_default_value)))) {
				mRotateAngle.setSummary(mRotateAngleSummary[i]);
			}
		}

		mSlideTime.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String value = (String)newValue;
				for(int i = 0;i < mSlideTimeValues.length;i ++) {
					if(mSlideTimeValues[i].equals(value)) {
						mSlideTime.setSummary(mSlideTimeSummary[i]);
					}
				}
				mEditor.putString(SLIDE_TIME_KEY, value);
				mEditor.commit();
				return true;
			}
		});
		
		mPostTime.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String value = (String)newValue;
				for(int i = 0;i < mPostTimeValues.length;i ++) {
					if(mPostTimeValues[i].equals(value)) {
						mPostTime.setSummary(mPostTimeSummary[i]);
					}
				}
				mEditor.putString(POST_TIME_KEY, value);
				mEditor.commit();
				return true;
			}
		});
		
		mRotateAngle.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String value = (String)newValue;
				for(int i = 0;i < mRotateAngleValues.length;i ++) {
					if(mRotateAngleValues[i].equals(value)) {
						mRotateAngle.setSummary(mRotateAngleSummary[i]);
					}
				}
				mEditor.putString(ROTATE_ANGLE_KEY, value);
				mEditor.commit();
				return true;
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch(keyCode) {
			case KeyEvent.KEYCODE_BACK:
				setResult(RESULT_OK);
				finish();
				break;
		}
		return super.onKeyDown(keyCode, event);
	}

}
