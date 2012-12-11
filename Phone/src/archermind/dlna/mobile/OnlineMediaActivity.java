package archermind.dlna.mobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.archermind.ashare.ui.control.ScrollLayout;
import com.archermind.ashare.ui.control.ScrollLayout.PageChangeListener;

public class OnlineMediaActivity extends Activity {

	private static final String TAG = "OnlineMediaActivity";
	private ScrollLayout mScrollLayout;
	private RadioGroup mGroup;
	private static final float APP_PAGE_SIZE = 16.0f;
	private Context mContext;

	private RadioButton[] mRadios;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mContext = this;
		setContentView(R.layout.online_media);
		
		findViewById(R.id.image_left_top).setVisibility(View.GONE);
		findViewById(R.id.top_bar_left_line).setVisibility(View.GONE);
		findViewById(R.id.top_bar_right_line).setVisibility(View.GONE);
		((TextView)findViewById(R.id.title)).setText(R.string.bottom_tab_online_media);

		mScrollLayout = (ScrollLayout) findViewById(R.id.media_scroll);
		mScrollLayout.setOnPageChangeListener(new PageChangeListener() {
			@Override
			public void onPageChange(int view) {
				mRadios[view].setChecked(true);
			}
		});
		initViews();
	}

	public void initViews() {
		final PackageManager packageManager = getPackageManager();

		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		final List<ResolveInfo> apps = new ArrayList<ResolveInfo>();

		List<ResolveInfo> temp;
		PackageInfo packageInfo;
		for (String name : appInfos.keySet()) {
			try {
				packageInfo = getPackageManager().getPackageInfo(name, 0);
			} catch (NameNotFoundException e) {
				packageInfo = null;
				e.printStackTrace();
			}

			if (packageInfo != null) {
				Intent intent = packageManager.getLaunchIntentForPackage(name);
				temp = packageManager.queryIntentActivities(intent, 0);
				apps.addAll(temp);
			} else {
				ResolveInfo info = new ResolveInfo();
				ActivityInfo activityInfo = new ActivityInfo();
				activityInfo.packageName = name;
				info.resolvePackageName = "archermind.dlna.mobile";
				info.activityInfo = activityInfo;
				info.icon = R.drawable.app_add;
				info.labelRes = R.string.app_name;
				apps.add(info);
			}
		}

		// the total pages
		final int pageCount = (int) Math.ceil(apps.size() / APP_PAGE_SIZE);
		Log.e(TAG, "size:" + apps.size() + " page:" + pageCount);
		for (int i = 0; i < pageCount; i++) {
			GridView appPage = new GridView(this);
			// get the "i" page data
			appPage.setAdapter(new AppAdapter(this, apps, i));
			appPage.setSelector(android.R.color.transparent);
			
			appPage.setNumColumns(4);
			appPage.setOnItemClickListener(listener);
			mScrollLayout.addView(appPage);
		}

		mGroup = (RadioGroup) findViewById(R.id.radio_group);
		mRadios = new RadioButton[pageCount];
		for (int i = 0; i < pageCount; i++) {
			RadioButton tempButton = new RadioButton(this, null, R.style.CustomRadioButton);
			tempButton.setBackgroundResource(R.drawable.app_apoint_sel);
			mRadios[i] = tempButton;
			mGroup.addView(tempButton, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		}

		mGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				Log.d(TAG, "checkedId: " + checkedId);
				for (int i = 0; i < mRadios.length; i++) {
					if (mRadios[i].getId() == checkedId) {
						mScrollLayout.setToScreen(i);
					}
				}
			}
		});

		mRadios[0].setChecked(true);

	}

	/**
	 * gridView 的onItemLick响应事件
	 */
	public OnItemClickListener listener = new OnItemClickListener() {

		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub
			ResolveInfo appInfo = (ResolveInfo) parent
					.getItemAtPosition(position);

			if (appInfo.resolvePackageName != null) {
				Toast.makeText(mContext, "无法找到文件",
						Toast.LENGTH_SHORT).show();
//				UpdateManager manager = new UpdateManager(OnlineMediaActivity.this);
//				manager.checkUpdate();
				return;
			}

			Intent mainIntent = mContext
					.getPackageManager()
					.getLaunchIntentForPackage(appInfo.activityInfo.packageName);
			mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			try {
				// launcher the package
				mContext.startActivity(mainIntent);
				// Toast.makeText(getApplicationContext(), "onclick",
				// Toast.LENGTH_LONG).show();
				getParent().overridePendingTransition(R.anim.bottom_in, R.anim.top_out);
			} catch (ActivityNotFoundException noFound) {
				Toast.makeText(mContext, "无法找到文件", Toast.LENGTH_SHORT).show();
			}
		}

	};

	public static final Map<String, String[]> appInfos = new HashMap<String, String[]>();

	static {
		appInfos.put("com.sohu.sohuvideo", new String[]{"搜狐视频", "url"});
		appInfos.put("com.pplive.androidphone", new String[]{"PPTV"});
		appInfos.put("com.tencent.qqlive", new String[]{"腾讯视频"});
		appInfos.put("com.togic.mediacenter", new String[]{"泰捷视频"});
	}

}
