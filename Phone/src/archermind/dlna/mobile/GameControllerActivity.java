package archermind.dlna.mobile;

import java.util.ArrayList;
import java.util.List;

import com.archermind.ashare.wiremote.natives.WiRemoteAgent;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.AbsoluteLayout;
import android.widget.TextView;
import android.widget.AbsoluteLayout.LayoutParams;

public class GameControllerActivity extends BaseActivity {

	private static final String TAG = "GameControllerActivity";

	private AbsoluteLayout mTouchArea;
	private TextView mTouchPoint1;
	private TextView mTouchPoint2;
	private List<TextView> mPoints;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate: ");

		setContentView(R.layout.game_controller);
		
		mPoints = new ArrayList<TextView>();
		
		mTouchPoint1 = (TextView) findViewById(R.id.touch_point1);
		mTouchPoint2 = (TextView) findViewById(R.id.touch_point2);
		mPoints.add(mTouchPoint1);
		mPoints.add(mTouchPoint2);

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
					WiRemoteAgent.setTouchEvent(Math.round(event.getX(0)),
							Math.round(event.getY(0)),
							Math.round(event.getX(1)),
							Math.round(event.getY(1)), event.getAction());
				} else {
					WiRemoteAgent.setTouchEvent(Math.round(event.getX(0)),
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

	}
	
	@Override
	protected void onResume() {
		super.onResume();
		WiRemoteAgent.gyroMouseControl(0);
	}

}
