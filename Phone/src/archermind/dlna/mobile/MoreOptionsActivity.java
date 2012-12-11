package archermind.dlna.mobile;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MoreOptionsActivity extends BaseActivity {
	private static final String TAG = "MoreOptionsActivity";
	private RelativeLayout mItemSetting;
	
	private LinearLayout mProgressBarContainer;
	private ImageView mProgressBar;
	
	private View.OnClickListener mClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent sendIntent;
			switch (v.getId()) {
			case R.id.item_setting:
				sendIntent = new Intent(MoreOptionsActivity.this, SettingActivity.class);
				startActivity(sendIntent);
				MoreOptionsActivity.this.getParent().overridePendingTransition(R.anim.pull_right_in, R.anim.pull_left_out);
				break;
			case R.id.item_gesture:
				sendIntent = new Intent(MoreOptionsActivity.this, GestureGuideActivity.class);
				startActivity(sendIntent);
				break;
			case R.id.item_feedback:
				sendIntent = new Intent(MoreOptionsActivity.this, FeedbackActivity.class);
				startActivity(sendIntent);
				MoreOptionsActivity.this.getParent().overridePendingTransition(R.anim.pull_right_in, R.anim.pull_left_out);
				break;
			case R.id.item_about:
				sendIntent = new Intent(MoreOptionsActivity.this, AboutActivity.class);
				startActivity(sendIntent);
				MoreOptionsActivity.this.getParent().overridePendingTransition(R.anim.pull_right_in, R.anim.pull_left_out);
				break;
			case R.id.item_update:
				showProgress();
				UpdateManager manager = new UpdateManager(MoreOptionsActivity.this);
				manager.checkUpdate();
				break;
			case R.id.item_exit:
			case R.id.more_option_btn_exit:
				onClickBtnExit();
				Intent intent = new Intent("com.archermind.exit");
				sendBroadcast(intent);
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
		
		findViewById(R.id.btn_left_top).setVisibility(View.GONE);
		findViewById(R.id.image_left_top).setVisibility(View.GONE);
		findViewById(R.id.top_bar_left_line).setVisibility(View.GONE);
		findViewById(R.id.top_bar_right_line).setVisibility(View.GONE);
		
		TextView title = (TextView) findViewById(R.id.title);
		title.setText(R.string.more_option_title);
		
		try {
			String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			((TextView)findViewById(R.id.item_update_version)).setText(versionName);
		} catch (NameNotFoundException e) {
		}

		mItemSetting = (RelativeLayout) findViewById(R.id.item_setting);
		mItemSetting.setOnClickListener(mClickListener);
		findViewById(R.id.item_gesture).setOnClickListener(mClickListener);
		findViewById(R.id.item_feedback).setOnClickListener(mClickListener);
		findViewById(R.id.item_about).setOnClickListener(mClickListener);
		findViewById(R.id.item_update).setOnClickListener(mClickListener);
		findViewById(R.id.item_exit).setOnClickListener(mClickListener);
		findViewById(R.id.more_option_btn_exit).setOnClickListener(mClickListener);
		
		mProgressBarContainer = (LinearLayout) findViewById(R.id.progress_bar_container);
		mProgressBarContainer.setBackgroundColor(getResources().getColor(R.color.progress_bg));
		mProgressBarContainer.setVisibility(View.INVISIBLE);
		
		mProgressBar = (ImageView) findViewById(R.id.progress_icon);
		
	}
	
	public void showProgress() {
		mProgressBarContainer.setVisibility(View.VISIBLE);
		mProgressBar.startAnimation(AnimationUtils.loadAnimation(this, R.anim.progress_bar_anim));
	}

	public void dismissProgress() {
		mProgressBarContainer.setVisibility(View.INVISIBLE);
		mProgressBar.clearAnimation();
	}
	
	private void onClickBtnExit() {
		quit();
		//((MobileApplication)getApplication()).unbindDLNAService();
		//System.exit(0);
	}
	
	@Override
	protected void onServiceQuited() {
		finish();
		getParent().overridePendingTransition(R.anim.bottom_in, R.anim.top_out);
	}
}
