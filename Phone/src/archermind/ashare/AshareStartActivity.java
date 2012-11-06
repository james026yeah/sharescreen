package archermind.ashare;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import archermind.ashare.AShareJniCallBack.AShareJniCallBackListener;
import archermind.dlna.mobile.R;
import archermind.dlna.mobile.R.layout;

public class AshareStartActivity extends Activity implements OnClickListener, AShareJniCallBackListener{
	private final static String TAG = "AshareStartActivity";
	private Button mStartBtn;
	private Button mStopBtn;
	private AShareJniCallBack mJniCallback;
	 public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.ashare);
	    mStartBtn = (Button) findViewById(R.id.start_btn);
	    mStopBtn = (Button) findViewById(R.id.stop_btn);
	    mStartBtn.setOnClickListener(this);
	    mStopBtn.setOnClickListener(this);
	    mJniCallback = AShareJniCallBack.getInstance();
	    mJniCallback.addCallBackListener(this);
	 }
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.start_btn:
			NativeAshare.shareScreen(mJniCallback, "192.168.1.241");
			break;
		case R.id.stop_btn:
			NativeAshare.stopShare();
			break;
		default:
			break;
		}
	}
	@Override
	public void onAShareClientConnected() {
		Log.d(TAG,"onAShareClientConnected...");
	}
	@Override
	public void onAShareClientDisconnected() {
		Log.d(TAG,"onAShareClientDisconnected...");
	}
	
	/*private void checkRootPermission() {
		Log.d(TAG, "checkRootPermission: ......");

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
					isRooted = true;
				}
			}
		} catch (Exception e) {
		} finally {
			mIsRoot = true;
		}
	}*/
	
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
}