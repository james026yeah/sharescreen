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
import android.widget.Toast;

public class OnlineMediaActivity extends Activity {

	private static final String TAG = "OnlineMediaActivity";
	private ScrollLayout mScrollLayout;
	private static final float APP_PAGE_SIZE = 16.0f;
	private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mContext = this;
		setContentView(R.layout.online_media);

		mScrollLayout = (ScrollLayout) findViewById(R.id.media_scroll);
		initViews();
	}

	public void initViews() {
		final PackageManager packageManager = getPackageManager();

		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		// get all apps
		// Intent intent =
		// packageManager.getLaunchIntentForPackage("com.sohu.sohuvideo");
		// final List<ResolveInfo> apps = packageManager.queryIntentActivities(
		// intent, 0);
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
			}else {
				ResolveInfo info = new ResolveInfo();
				ActivityInfo activityInfo = new ActivityInfo();
				activityInfo.packageName = name;
				info.resolvePackageName = "archermind.dlna.mobile";
				info.activityInfo = activityInfo;
				info.icon = R.drawable.ic_launcher;
				info.labelRes = R.string.app_name;
				apps.add(info);
			}
		}

		// the total pages
		final int PageCount = (int) Math.ceil(apps.size() / APP_PAGE_SIZE);
		Log.e(TAG, "size:" + apps.size() + " page:" + PageCount);
		for (int i = 0; i < PageCount; i++) {
			GridView appPage = new GridView(this);
			// get the "i" page data
			appPage.setAdapter(new AppAdapter(this, apps, i));

			appPage.setNumColumns(4);
			appPage.setOnItemClickListener(listener);
			mScrollLayout.addView(appPage);
		}
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
				Toast.makeText(mContext, "Package not found!",
						Toast.LENGTH_SHORT).show();
				return;
			}
			
			Intent mainIntent = mContext
					.getPackageManager()
					.getLaunchIntentForPackage(appInfo.activityInfo.packageName);
			mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			try {
				// launcher the package
				mContext.startActivity(mainIntent);
			} catch (ActivityNotFoundException noFound) {
				Toast.makeText(mContext, "Package not found!",
						Toast.LENGTH_SHORT).show();
			}
		}

	};

	private static final Map<String, String> appInfos = new HashMap<String, String>();

	static {
		appInfos.put("com.sohu.sohuvideo", "url");
		appInfos.put("com.pplive.androidphone", "url");
		appInfos.put("com.rock.androidphone", "url");
	}
	
	public static class AppInfo {}

}
