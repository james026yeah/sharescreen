package archermind.dlna.mobile.data.util;

import android.app.Dialog;
import android.content.Context;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import archermind.dlna.mobile.R;


public class LoginDialog extends Dialog implements android.view.View.OnClickListener {

	private Context mContext;

	public LoginDialog(Context context) {
		super(context);
		setDialogView();
		mContext = context;
	}

	public void setDialogView() {
		setContentView(R.layout.enter_sshid_dialog);
	}

	public void dismissLoginDialog() {
		dismiss();
	}

	public Button getLinkButton() {
		Button linkBtn = (Button)findViewById(R.id.linkBtn);
		return linkBtn;
	}

	public Button getCancelButton() {
		Button cancelBtn = (Button)findViewById(R.id.cancelBtn);
		return cancelBtn;
	}

	public TableLayout getTableLayout() {
		TableLayout tabLayout = (TableLayout)findViewById(R.id.loginFrom);
		return tabLayout;
	}

	public TextView getSecondaryTitle() {
		TextView toastText = (TextView)findViewById(R.id.dialog_toast);
		return toastText;
	}

	public void setDialogProperty(int w, int h) {
		android.view.WindowManager.LayoutParams p = getWindow().getAttributes();

		p.height = h;
		p.width = w;
		getWindow().setAttributes(p);
	}

	@Override
	public void onClick(View v) {

		switch(v.getId()) {
		case R.id.cancelBtn: {
			dismiss();
		}
		}
	}

}
