package archermind.dlna.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

public class MoreOptionsActivity extends Activity {

	private RelativeLayout mItemSetting;
	
	private View.OnClickListener mClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent sendIntent;
			switch (v.getId()) {
			case R.id.item_setting:
				sendIntent = new Intent(MoreOptionsActivity.this, SettingActivity.class);
				startActivity(sendIntent);
				break;
			case R.id.item_gesture:
				sendIntent = new Intent(MoreOptionsActivity.this, GestureGuideActivity.class);
				startActivity(sendIntent);
				break;
			case R.id.item_feedback:
				sendIntent = new Intent(MoreOptionsActivity.this, FeedbackActivity.class);
				startActivity(sendIntent);
				break;
			case R.id.item_about:
				sendIntent = new Intent(MoreOptionsActivity.this, AboutActivity.class);
				startActivity(sendIntent);
				break;

			default:
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.more_options);

		mItemSetting = (RelativeLayout) findViewById(R.id.item_setting);
		mItemSetting.setOnClickListener(mClickListener);
		
		findViewById(R.id.item_gesture).setOnClickListener(mClickListener);
		findViewById(R.id.item_feedback).setOnClickListener(mClickListener);
		findViewById(R.id.item_about).setOnClickListener(mClickListener);

	}
}
