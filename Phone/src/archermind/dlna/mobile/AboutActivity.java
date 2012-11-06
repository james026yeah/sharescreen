package archermind.dlna.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class AboutActivity extends Activity {
	
	private static final String TAG = "AboutActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate: ");
		
		setContentView(R.layout.about_layout);
		
		TextView temp = (TextView) findViewById(R.id.title);
		temp.setText(R.string.about_title);
		
	}

}
