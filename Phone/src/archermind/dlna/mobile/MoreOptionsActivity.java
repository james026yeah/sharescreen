package archermind.dlna.mobile;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import com.archermind.ashare.mirror.AShareJniCallBack;
import com.archermind.ashare.mirror.NativeAshare;
import com.archermind.ashare.mirror.AShareJniCallBack.AShareJniCallBackListener;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MoreOptionsActivity extends BaseActivity implements AShareJniCallBackListener{
	private static final String TAG = "MoreOptionsActivity";
	private AShareJniCallBack mJniCallback;
	private Display mDisplay;
	private String mCurrentSTBIp;
	private MyOrientationEventListener mOrentationListener;
	private RelativeLayout mItemSetting;
	private View.OnClickListener mClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent sendIntent;
			switch (v.getId()) {
			case R.id.item_ashare:
				startAshareOnclick();
				break;
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
		
		TextView title = (TextView) findViewById(R.id.title);
		title.setText(R.string.more_option_title);
		
		try {
			String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			((TextView)findViewById(R.id.item_update_version)).setText(versionName);
		} catch (NameNotFoundException e) {
		}

		mItemSetting = (RelativeLayout) findViewById(R.id.item_setting);
		mItemSetting.setOnClickListener(mClickListener);
		findViewById(R.id.item_ashare).setOnClickListener(mClickListener);
		findViewById(R.id.item_gesture).setOnClickListener(mClickListener);
		findViewById(R.id.item_feedback).setOnClickListener(mClickListener);
		findViewById(R.id.item_about).setOnClickListener(mClickListener);
		findViewById(R.id.more_option_btn_exit).setOnClickListener(mClickListener);
		mJniCallback = AShareJniCallBack.getInstance();
		mJniCallback.addCallBackListener(this);
		mDisplay = this.getWindowManager().getDefaultDisplay();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume.....getIPAddrOfCurrentRenderer");
		getIPAddrOfCurrentRenderer();
	}



	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		Log.d(TAG, "onServiceConnected........");
		getIPAddrOfCurrentRenderer();
	}



	private void onClickBtnExit() {
		quit();
		//((MobileApplication)getApplication()).unbindDLNAService();
		//System.exit(0);
	}
	
	@Override
	protected void onServiceQuited() {
		finish();
	}
	
	private void startAshareOnclick() {
		if (makeFbReadable() && mCurrentSTBIp != null) {
			NativeAshare.setRotate(mDisplay.getRotation());
			// stb ip
			NativeAshare.shareScreen(mJniCallback, mCurrentSTBIp);
			mOrentationListener = new MyOrientationEventListener(this);
			mOrentationListener.enable();
		} else {
			Log.d(TAG,"fb open failure!!! or ip == null");
			Toast.makeText(this, R.string.ashare_not_available, Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	protected void onGetIPAddrOfCurrentRenderer(String ipAddr) {
		super.onGetIPAddrOfCurrentRenderer(ipAddr);
		mCurrentSTBIp = ipAddr;
		Log.d(TAG, "mCurrentRenderIp=" + mCurrentSTBIp);
	}
	
	@Override
	public void onAShareClientConnected() {
		Log.d(TAG,"onAShareClientConnected...");
	}
	@Override
	public void onAShareClientDisconnected() {
		Log.d(TAG,"onAShareClientDisconnected...");
	}
	
	private boolean makeFbReadable() {
		Log.d(TAG, "makeFbReadable: ......");
		boolean success = false;
		Process process = null;
		DataOutputStream os = null;
		int exitValue = 0;
		try {
			if (isRooted()) {
				process = Runtime.getRuntime().exec("su");
				os = new DataOutputStream(process.getOutputStream());
				os.writeBytes("chmod 666 /dev/graphics/fb0 \n");
				os.writeBytes("exit\n");
				os.flush();
				exitValue = process.waitFor();
				Log.d(TAG, "@@@@@@@@@@@@@@@@@@@@ exitValue::" + exitValue);

				if (exitValue == 0) {
					success = true;
				}
			}
		} catch (Exception e) {
		} finally {
			//mIsRoot = true;
		}
		return success;
	}
	
	public boolean isRooted() {
		// 检测是否ROOT过
		DataInputStream stream;
		boolean flag = false;
		try {
			stream = terminal("ls /data/");
			// 目录哪都行，不一定要需要ROOT权限的
			if (stream.readLine() != null)
				flag = true;
			// 根据是否有返回来判断是否有root权限
			Log.d(TAG, "Root flag: " + flag);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();

		}

		return flag;
	}
	
	public DataInputStream terminal(String command) throws Exception {
		Process process = Runtime.getRuntime().exec("su");
		// 执行到这，Superuser会跳出来，选择是否允许获取最高权限
		OutputStream outstream = process.getOutputStream();
		DataOutputStream DOPS = new DataOutputStream(outstream);
		InputStream instream = process.getInputStream();
		DataInputStream DIPS = new DataInputStream(instream);
		String temp = command + "\n";
		// 加回车
		DOPS.writeBytes(temp);
		// 执行
		DOPS.flush();
		// 刷新，确保都发送到outputstream
		DOPS.writeBytes("exit\n");
		// 退出
		DOPS.flush();
		process.waitFor();
		return DIPS;
	}
	
	private class MyOrientationEventListener extends OrientationEventListener {
		
		private int mOrientation = 0;
		private int mRotationSent2Server = 0;
		public MyOrientationEventListener(Context context) {
			super(context);
		}
		@Override
		public void onOrientationChanged(int orientation) {
			// We keep the last known orientation. So if the user first orient
			// the camera then point the camera to floor or sky, we still have
			// the correct orientation.
			if (orientation == ORIENTATION_UNKNOWN) {
				return;
			} else {
				if (mOrientation != mDisplay.getRotation()) {
						switch (mDisplay.getRotation()) {
						case Surface.ROTATION_0:
							mRotationSent2Server = 0;
							break;
						case Surface.ROTATION_90:
							mRotationSent2Server = 90;
							break;
						case Surface.ROTATION_180:
							mRotationSent2Server = 180;
							break;
						case Surface.ROTATION_270:
							mRotationSent2Server = 270;
							break;
						default:
							break;
						
					}
					Log.d(TAG,"onOrientationChanged=" + orientation + " mDisplay.getRotation=" + mDisplay.getRotation());
					mOrientation = mDisplay.getRotation();
					NativeAshare.setRotate(mRotationSent2Server);
				}
			}
			
		}
	}
}
