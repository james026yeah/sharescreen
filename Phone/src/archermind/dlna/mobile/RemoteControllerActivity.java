package archermind.dlna.mobile;

import java.util.ArrayList;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AbsoluteLayout;
import android.widget.AbsoluteLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.archermind.ashare.mirror.AshareProcess;
import com.archermind.ashare.misc.DeviceInfo;
import com.archermind.ashare.wiremote.natives.WiRemoteAgent;

@SuppressWarnings("deprecation")
public class RemoteControllerActivity extends BaseActivity {

	private static final String TAG = "RemoteControllerActivity";

	private static final int KEY_DOWN = 1;
	private static final int KEY_UP = 0;

	private AbsoluteLayout mTouchArea;
	private TextView mTouchPoint1;
	private TextView mTouchPoint2;
	
	private RelativeLayout mControllerArea;
	
	private ImageView mControllerBg;

	private List<TextView> mPoints;

	private ImageView mTopLeftBtn;
	private ImageView mTopRightBtn;

	private ImageView mOkBtn;
	private TextView mMenuBtn;
	private TextView mHomeBtn;
	private TextView mBackBtn;
	private TextView mSleepBtn;
	private TextView mAshareMirrorText;

	private static final int TOUCH_DLG_ID = 0;
	private static final int MOUSE_DLG_ID = 1;

	private static final int KEYCODE_BACK = 1;
	private static final int KEYCODE_MENU = 139;
	private static final int KEYCODE_HOME = 172;
	private static final int KEYCODE_OK = 272;
	private static final int KEYCODE_SLEEP = 100;
	
	private static final int KEYCODE_UP = 103;
	private static final int KEYCODE_LEFT = 105;
	private static final int KEYCODE_OK_KEYBOARD = 66;
	private static final int KEYCODE_RIGHT = 106;
	private static final int KEYCODE_DOWN = 108;
	private static final int KEYCODE_MUTE = 113;
	private static final int KEYCODE_VOLUMEDOWN = 114;
	private static final int KEYCODE_VOLUMEUP = 115;

	@SuppressWarnings("unused")
	private static boolean mIsSleeping = false;

	@SuppressWarnings("unused")
	private String[] mControlPages = null;

	private static boolean mHasGyroscope = false;
	
	private static boolean mIsMouseMode = false;
	AshareProcess mAshareProcess = AshareProcess.newInstance(this);
	private String mCurrentRenderIp;
	private View.OnTouchListener mOnTouchBtns = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int keycode = 0;
			int imageId = 0;
			switch (v.getId()) {
			case R.id.btn_menu:
				keycode = KEYCODE_MENU;
				break;
			case R.id.btn_home:
				keycode = KEYCODE_HOME;
				break;
			case R.id.btn_back:
				keycode = KEYCODE_BACK;
				break;
			case R.id.btn_ok:
				keycode = KEYCODE_OK;
				break;
			case R.id.btn_sleep:
				keycode = KEYCODE_SLEEP;
				break;
				
			case R.id.btn_switcher_up:
				imageId = R.drawable.remote_control_circle_top_select;
				keycode = KEYCODE_UP;
				break;
			case R.id.btn_switcher_left:
				imageId = R.drawable.remote_control_circle_left_select;
				keycode = KEYCODE_LEFT;
				break;
			case R.id.btn_switcher_ok:
				imageId = R.drawable.remote_control_circle_ok_select;
				keycode = KEYCODE_OK_KEYBOARD;
				break;
			case R.id.btn_switcher_right:
				imageId = R.drawable.remote_control_circle_right_select;
				keycode = KEYCODE_RIGHT;
				break;
			case R.id.btn_switcher_down:
				imageId = R.drawable.remote_control_circle_down_select;
				keycode = KEYCODE_DOWN;
				break;
			case R.id.btn_controller_mute:
				keycode = KEYCODE_MUTE;
				break;
			case R.id.btn_controller_vol_down:
				keycode = KEYCODE_VOLUMEDOWN;
				break;
			case R.id.btn_controller_vol_up:
				keycode = KEYCODE_VOLUMEUP;
				break;
			default:
				keycode = 0;
				imageId = 0;
				break;
			}
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				Log.d(TAG, "KEY_DOWN: " + keycode);
				if (imageId != 0) {
					mControllerBg.setBackgroundResource(imageId);
				}
				WiRemoteAgent.setKeyEvent(KEY_DOWN, keycode);
				v.setPressed(true);
				return true;
			case MotionEvent.ACTION_UP:
				Log.d(TAG, "KEY_UP: " + keycode);
				if (imageId != 0) {
					mControllerBg.setBackgroundResource(R.drawable.remote_control_bth_circle_normal);
				}
				WiRemoteAgent.setKeyEvent(KEY_UP, keycode);
				v.setPressed(false);
				return true;
			default:
				break;
			}
			return false;
		}
	};

	private View.OnClickListener mClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (mAshareMirrorText != null) {
				if (mAshareProcess.isRunning()) {
					mAshareMirrorText.setText(getResources().getString(R.string.stop_ashare_mirror));
				} else {
					mAshareMirrorText.setText(getResources().getString(R.string.ashare_mirror));
				}
			}
			switch (v.getId()) {
			case R.id.image_left_top:
				finish();
				break;
			case R.id.image_right_top:
				if (mTouchArea.getVisibility() == View.VISIBLE) {
					showDialog(TOUCH_DLG_ID);
				} else {
					showDialog(MOUSE_DLG_ID);
				}
				break;
			default:
				break;
			}
		}
	};
	
//	private View.OnTouchListener mControllerTouchListener = new OnTouchListener() {
//		@Override
//		public boolean onTouch(View v, MotionEvent event) {
//			int imageId = 0;
//			switch (v.getId()) {
//			case R.id.btn_switcher_left:
//				imageId = R.drawable.remote_control_circle_left_select;
//				break;
//			case R.id.btn_switcher_up:
//				imageId = R.drawable.remote_control_circle_top_select;
//				break;
//			case R.id.btn_switcher_right:
//				imageId = R.drawable.remote_control_circle_right_select;
//				break;
//			case R.id.btn_switcher_down:
//				imageId = R.drawable.remote_control_circle_down_select;
//				break;
//			case R.id.btn_switcher_ok:
//				imageId = R.drawable.remote_control_circle_ok_select;
//				break;
//			default:
//				break;
//			}
//			switch (event.getAction()) {
//			case MotionEvent.ACTION_DOWN:
//				mControllerBg.setBackgroundResource(imageId);
//				break;
//			case MotionEvent.ACTION_UP:
//				mControllerBg.setBackgroundResource(R.drawable.remote_control_bth_circle_normal);
//				break;
//			default:
//				break;
//			}
//			return true;
//		}
//	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate: ");
		super.onCreate(savedInstanceState);

		checkSensor();
		mPoints = new ArrayList<TextView>();

		setContentView(R.layout.remote_controller);

		mTopLeftBtn = (ImageView) findViewById(R.id.image_left_top);
		mTopLeftBtn.setImageResource(R.drawable.remote_btn_close_sel);
		mTopLeftBtn.setOnClickListener(mClickListener);
		mTopRightBtn = (ImageView) findViewById(R.id.image_right_top);
		mTopRightBtn.setImageResource(R.drawable.remote_btn_switch_sel);
		mTopRightBtn.setVisibility(View.VISIBLE);
		mTopRightBtn.setOnClickListener(mClickListener);

		mOkBtn = (ImageView) findViewById(R.id.btn_ok);
		mOkBtn.setOnTouchListener(mOnTouchBtns);
		mMenuBtn = (TextView) findViewById(R.id.btn_menu);
		mMenuBtn.setOnTouchListener(mOnTouchBtns);
		mHomeBtn = (TextView) findViewById(R.id.btn_home);
		mHomeBtn.setOnTouchListener(mOnTouchBtns);
		mBackBtn = (TextView) findViewById(R.id.btn_back);
		mBackBtn.setOnTouchListener(mOnTouchBtns);
		mSleepBtn = (TextView) findViewById(R.id.btn_sleep);
		mSleepBtn.setOnTouchListener(mOnTouchBtns);
		
		findViewById(R.id.btn_switcher_up).setOnTouchListener(mOnTouchBtns);
		findViewById(R.id.btn_switcher_left).setOnTouchListener(mOnTouchBtns);
		findViewById(R.id.btn_switcher_ok).setOnTouchListener(mOnTouchBtns);
		findViewById(R.id.btn_switcher_right).setOnTouchListener(mOnTouchBtns);
		findViewById(R.id.btn_switcher_down).setOnTouchListener(mOnTouchBtns);
		findViewById(R.id.btn_controller_mute).setOnTouchListener(mOnTouchBtns);
		findViewById(R.id.btn_controller_vol_down).setOnTouchListener(mOnTouchBtns);
		findViewById(R.id.btn_controller_vol_up).setOnTouchListener(mOnTouchBtns);

		mTouchArea = (AbsoluteLayout) findViewById(R.id.touch_area);
		mTouchArea.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getPointerCount() > 2) {
					Log.d(TAG, "PointerCount: " + event.getPointerCount());
					return true;
				}

				int pointerCount = event.getPointerCount();

				if (pointerCount == 2) {
					WiRemoteAgent.mouseEvent(Math.round(event.getX(0)),
							Math.round(event.getY(0)),
							Math.round(event.getX(1)),
							Math.round(event.getX(1)), event.getAction());
				} else {
					WiRemoteAgent.mouseEvent(Math.round(event.getX(0)),
							Math.round(event.getY(0)),
							Math.round(event.getX(0)),
							Math.round(event.getY(0)), event.getAction());
				}

				for (int i = 0; i < pointerCount; i++) {
					int x = (int) event.getX(i);
					int y = (int) event.getY(i);
					LayoutParams params = new LayoutParams(
							LayoutParams.WRAP_CONTENT,
							LayoutParams.WRAP_CONTENT, x - 54, y - 54);
					mPoints.get(i).setLayoutParams(params);
				}

				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					mTouchPoint1.setVisibility(View.VISIBLE);
					break;
				case MotionEvent.ACTION_POINTER_2_DOWN:
					mTouchPoint2.setVisibility(View.VISIBLE);
					break;
				case MotionEvent.ACTION_UP:
					mTouchPoint1.setVisibility(View.GONE);
					mTouchPoint2.setVisibility(View.GONE);
					break;
				case MotionEvent.ACTION_POINTER_1_UP:
					mTouchPoint2.setVisibility(View.GONE);
					break;
				case MotionEvent.ACTION_POINTER_2_UP:
					mTouchPoint2.setVisibility(View.GONE);
					break;
				default:
					break;
				}

				return true;
			}
		});
		
		mControllerArea = (RelativeLayout) findViewById(R.id.controller_area);
		
		mControllerBg = (ImageView) findViewById(R.id.btn_background);

		if (mHasGyroscope) {
			showMouse();
		} else {
			showTouch();
		}

		mTouchPoint1 = (TextView) findViewById(R.id.touch_point1);
		mTouchPoint2 = (TextView) findViewById(R.id.touch_point2);
		mPoints.add(mTouchPoint1);
		mPoints.add(mTouchPoint2);
		
	}
	
	private void showMouse() {
		if (!mHasGyroscope) {
			Toast.makeText(this, R.string.remote_control_not_support, Toast.LENGTH_SHORT).show();
			return;
		}
		((TextView)findViewById(R.id.title)).setText(R.string.remote_control_title_mouse);
		mIsMouseMode = true;
		mTouchArea.setVisibility(View.GONE);
		mControllerArea.setVisibility(View.GONE);
		WiRemoteAgent.gyroMouseControl(1);
	}

	private void showTouch() {
		((TextView)findViewById(R.id.title)).setText(R.string.remote_control_title_touch);
		mIsMouseMode = false;
		mTouchArea.setVisibility(View.VISIBLE);
		mControllerArea.setVisibility(View.GONE);
		WiRemoteAgent.gyroMouseControl(0);
	}

	private void showController() {
		((TextView)findViewById(R.id.title)).setText(R.string.remote_control_title_tran);
		mIsMouseMode = false;
		mTouchArea.setVisibility(View.GONE);
		mControllerArea.setVisibility(View.VISIBLE);
		WiRemoteAgent.gyroMouseControl(0);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if (mIsBound) {
			getIPAddrOfCurrentRenderer();
		}
		
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		WiRemoteAgent.gyroMouseControl(0);
	}
	
	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		getIPAddrOfCurrentRenderer();
	}
	
	protected void onDeviceConnected(DeviceInfo info) {
		super.onDeviceConnected(info);
		Log.d(TAG, "onDeviceConnected: ");
	}
	
	@Override
	protected void onGetIPAddrOfCurrentRenderer(String ipAddr) {
		super.onGetIPAddrOfCurrentRenderer(ipAddr);
		Log.d(TAG, "onGetIPAddrOfCurrentRenderer: ipAddr = " + ipAddr);
		if (ipAddr == null) {
			Toast.makeText(getApplicationContext(), R.string.not_connect_to_device, Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		mCurrentRenderIp = ipAddr;
		int width = getWindowManager().getDefaultDisplay().getWidth();
		int height = getWindowManager().getDefaultDisplay().getHeight();
		Log.d(TAG, "onGetIPAddrOfCurrentRenderer: width = " + width + " height = " + height);
		WiRemoteAgent.deinit();
		WiRemoteAgent.init(width, height);
		WiRemoteAgent.connectServer(ipAddr);
		if (mIsMouseMode) {
			WiRemoteAgent.gyroMouseControl(1);
		} else {
			WiRemoteAgent.gyroMouseControl(0);
		}
		
	}

	public void checkSensor() {
		// 从系统服务中获得传感器管理器
		SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		// 从传感器管理器中获得全部的传感器列表
		List<Sensor> allSensors = sm.getSensorList(Sensor.TYPE_GYROSCOPE);

		// 显示每个传感器的具体信息
		for (Sensor s : allSensors) {
			switch (s.getType()) {
			case Sensor.TYPE_GYROSCOPE:
				Log.d(TAG, "Has gyroscope: ");
				mHasGyroscope = true;
				break;
			default:
				break;
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Log.d("jni_debug","onCreateDialog..............");

		final Dialog dialog = new Dialog(this, R.style.Theme_DeleteImageDialog);
		// Get the layout inflater
		View dlgContent = getLayoutInflater().inflate(R.layout.dialog_control_pages_sel, null);

		dlgContent.findViewById(R.id.touch).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showTouch();
				dialog.dismiss();
			}
		});
		dlgContent.findViewById(R.id.mouse).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showMouse();
				dialog.dismiss();
			}
		});
		dlgContent.findViewById(R.id.controller).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showController();
				dialog.dismiss();
			}
		});
		dlgContent.findViewById(R.id.game_controller).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(RemoteControllerActivity.this, GameControllerActivity.class));
				dialog.dismiss();
			}
		});
		mAshareMirrorText = (TextView)dlgContent.findViewById(R.id.ashare_mirror);
		dlgContent.findViewById(R.id.ashare_mirror).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mAshareProcess.isRunning()) {
					mAshareProcess.stopAshareMirror();
				} else {
					startAshareMirror(mCurrentRenderIp);
				}
				dialog.dismiss();
			}
		});
		
		dialog.setContentView(dlgContent);
		return dialog;
	}
	
	public void startAshareMirror(String ip) {
		if (ip == null) {
			Log.d(TAG,"ip null mirror error");
			return;
		}
		if (mAshareProcess == null) {
			mAshareProcess = AshareProcess.newInstance(RemoteControllerActivity.this);
		}
		mAshareProcess.startAshareMirror(ip);
	}
}
