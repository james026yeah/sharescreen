package archermind.dlna.mobile;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
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
import android.widget.TextView;
import android.widget.Toast;
import archermind.dlna.miscellaneous.DeviceInfo;
import archermind.dlna.natives.WiRemoteAgent;

@SuppressWarnings("deprecation")
public class RemoteControllerActivity extends BaseActivity {

	private static final String TAG = "RemoteControllerActivity";

	private static final int KEY_DOWN = 1;
	private static final int KEY_UP = 0;

	private AbsoluteLayout mTouchArea;
	private TextView mTouchPoint1;
	private TextView mTouchPoint2;

	private List<TextView> mPoints;

	private TextView mTopLeftBtn;
	private TextView mTopRightBtn;

	private ImageView mOkBtn;
	private TextView mMenuBtn;
	private TextView mHomeBtn;
	private TextView mBackBtn;
	private TextView mSleepBtn;
	
	private boolean mHasTarget = false;

	private static final int TOUCH_DLG_ID = 0;
	private static final int MOUSE_DLG_ID = 1;

	private static final int KEYCODE_BACK = 1;
	private static final int KEYCODE_MENU = 139;
	private static final int KEYCODE_HOME = 172;
	private static final int KEYCODE_OK = 272;
	private static final int KEYCODE_SLEEP = 100;
	// private static final int KEYCODE_WAKEUP = 200;

	@SuppressWarnings("unused")
	private static boolean mIsSleeping = false;

	@SuppressWarnings("unused")
	private String[] mControlPages = null;

	private static boolean mHasGyroscope = false;

	private View.OnTouchListener mOnTouchBtns = new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int keycode = 0;
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
				// if (mIsSleeping) {
				// keycode = KEYCODE_SLEEP;
				// if (event.getAction() == MotionEvent.ACTION_UP) {
				// mIsSleeping = false;
				// }
				// } else {
				// keycode = KEYCODE_WAKEUP;
				// if (event.getAction() == MotionEvent.ACTION_UP) {
				// mIsSleeping = true;
				// }
				// }
				break;
			default:
				break;
			}
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				Log.d(TAG, "KEY_DOWN: " + keycode);
				WiRemoteAgent.setKeyEvent(KEY_DOWN, keycode);
				break;
			case MotionEvent.ACTION_UP:
				Log.d(TAG, "KEY_UP: " + keycode);
				WiRemoteAgent.setKeyEvent(KEY_UP, keycode);
				break;
			default:
				break;
			}
			return false;
		}
	};

	private View.OnClickListener mClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.top_left_btn:
				finish();
				break;
			case R.id.top_right_btn:
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate: ");
		super.onCreate(savedInstanceState);

		checkSensor();
		mPoints = new ArrayList<TextView>();

		setContentView(R.layout.remote_controller);

		mTopLeftBtn = (TextView) findViewById(R.id.top_left_btn);
		mTopLeftBtn.setOnClickListener(mClickListener);
		mTopRightBtn = (TextView) findViewById(R.id.top_right_btn);
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

		mTouchArea = (AbsoluteLayout) findViewById(R.id.touch_area);
		mTouchArea.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getPointerCount() > 2) {
					Log.d(TAG, "PointerCount: " + event.getPointerCount());
					return true;
				}

				int pointerCount = event.getPointerCount();

				// Log.d(TAG,
				// "x1: " + Math.round(event.getX(0)) + " y1: "
				// + Math.round(event.getY(0)) + " a1: " + "x2: "
				// + Math.round(event.getX(1)) + " y2: "
				// + Math.round(event.getY(1)) + " a2: "
				// + event.getAction());
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

		if (mHasGyroscope) {
			mTouchArea.setVisibility(View.GONE);
		} else {
			mTouchArea.setVisibility(View.VISIBLE);
		}
//		mTouchArea.setVisibility(View.VISIBLE);

		mTouchPoint1 = (TextView) findViewById(R.id.touch_point1);
		mTouchPoint2 = (TextView) findViewById(R.id.touch_point2);
		mPoints.add(mTouchPoint1);
		mPoints.add(mTouchPoint2);
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		
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
			Toast.makeText(getApplicationContext(), "未选中设备", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		
		mHasTarget = true;
		
		int width = getWindowManager().getDefaultDisplay().getWidth();
		int height = getWindowManager().getDefaultDisplay().getHeight();
		Log.d(TAG, "onGetIPAddrOfCurrentRenderer: width = " + width + " height = " + height);
		WiRemoteAgent.init(width, height);
		WiRemoteAgent.connectServer(ipAddr);
		if (mTouchArea.getVisibility() == View.VISIBLE) {
			WiRemoteAgent.gyroMouseControl(0);
		} else {
			WiRemoteAgent.gyroMouseControl(1);
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

		final Dialog dialog = new Dialog(this, R.style.Theme_DeleteImageDialog);
		// Get the layout inflater
		View dlgContent = getLayoutInflater().inflate(R.layout.dialog_control_pages_sel, null);

		final TextView controlMode = (TextView) dlgContent.findViewById(R.id.touch_or_mouse);
		if (mTouchArea.getVisibility() == View.VISIBLE) {
			controlMode.setText("Mouse");
		} else {
			controlMode.setText("Touch");
		}
		controlMode.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mTouchArea.getVisibility() == View.VISIBLE) {
					mTouchArea.setVisibility(View.GONE);
					WiRemoteAgent.gyroMouseControl(1);
				} else {
					mTouchArea.setVisibility(View.VISIBLE);
					WiRemoteAgent.gyroMouseControl(0);
				}
				dialog.dismiss();
			}
		});
		TextView gameController = (TextView) dlgContent.findViewById(R.id.game_controller);
		gameController.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(RemoteControllerActivity.this, GameControllerActivity.class));
				dialog.dismiss();
			}
		});
		
		dialog.setContentView(dlgContent);

		// Add action buttons;
		return dialog;
	}

}
