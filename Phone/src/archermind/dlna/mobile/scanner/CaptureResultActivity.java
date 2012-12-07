package archermind.dlna.mobile.scanner;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import archermind.dlna.mobile.R;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class CaptureResultActivity extends Activity implements View.OnClickListener {

	private static final String BEGIN_SSID = ";SSID:";
	private static final String BEGIN_CODE = ";Code:";
	private static int RESULT_IMAGE_SIZE = 230;
	
	private TextView mWifiSSID;
	private TextView mWifiCode;
	private ImageView mCodeImage;
	private Button mWifiConntect;
	
	private boolean mWifiButtonConnect = true;
	
	private void setupView() {
		mCodeImage = (ImageView) findViewById(R.id.code_image);
		mWifiSSID = (TextView) findViewById(R.id.wifi_ssid);
		mWifiCode = (TextView) findViewById(R.id.wifi_code);
		mWifiConntect = (Button) findViewById(R.id.wifi_connect);
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scanner_capture_result);
        setupView();
    }

	@Override
	protected void onResume() {
		super.onResume();
        Intent data = this.getIntent();
        CharSequence displayContents = data.getCharSequenceExtra(Intents.Capture.CAPTURE_RESULT_DATA);
        
        String[] ssidCode = decodeWifi(displayContents.toString());
        mWifiConntect.setOnClickListener(this);
        if (ssidCode[0] == null || ssidCode[1] == null) {
        	mWifiConntect.setText(R.string.btn_wifi_rescan);
        	mWifiButtonConnect = false;
        	return;
        }

        mWifiSSID.setText(ssidCode[0]);
        mWifiCode.setText(ssidCode[1]);
        try {
			Bitmap bitmap = create2DCode(displayContents.toString());
			mCodeImage.setImageBitmap(bitmap);
		} catch (WriterException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onClick(View v) {
		if (mWifiButtonConnect) {
			
		} else {
			finish();
		}
	}

    public Bitmap create2DCode(String str) throws WriterException {  
        BitMatrix matrix = new MultiFormatWriter().encode(str,BarcodeFormat.QR_CODE, RESULT_IMAGE_SIZE, RESULT_IMAGE_SIZE);  
        int width = matrix.getWidth();  
        int height = matrix.getHeight();  
        int[] pixels = new int[width * height];  
        for (int y = 0; y < height; y++) {  
            for (int x = 0; x < width; x++) {  
                if(matrix.get(x, y)){  
                    pixels[y * width + x] = 0xff000000;  
                }
            }  
        } 
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); 
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);  
        return bitmap;  
    }
    
    private String[] decodeWifi(String display) {
    	String[] ssidCode = new String[2];
    	int code = display.indexOf(BEGIN_CODE);
    	if (code > 0) {
        	ssidCode[0] = display.substring(BEGIN_SSID.length(), code);
        	ssidCode[1] = display.substring(BEGIN_CODE.length() + code, display.length() - 1);
    	}
    	return ssidCode;
    }
}
