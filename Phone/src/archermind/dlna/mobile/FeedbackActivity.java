package archermind.dlna.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class FeedbackActivity extends Activity {
	
	private static final String TAG = "FeedbackActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate: ");
		
		setContentView(R.layout.feedback_layout);
		
		findViewById(R.id.btn_left_top).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
				overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
			}
		});
		
		TextView temp = (TextView) findViewById(R.id.title);
		temp.setText(R.string.feedback_title);
		temp = (TextView) findViewById(R.id.btn_right_top);
		temp.setText(R.string.feedback_btn_right_top);
		temp.setVisibility(View.VISIBLE);
		temp.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Send feedback
				finish();
				overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
			}
		});
		
		
		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			overridePendingTransition(R.anim.push_left_in, R.anim.push_right_out);
		}
		return super.onKeyDown(keyCode, event);
	}

}
