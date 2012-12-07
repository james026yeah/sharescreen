package archermind.dlna.mobile;

import android.content.Intent;

public final class TabUtils {
	private static final String EXTRA_TAB_INDEX = "MobileDLNA_TABS";
	
	public static Intent setTab(Intent intent, int tabIndex) {		
		if (intent != null) {			
		    intent.putExtra(EXTRA_TAB_INDEX, tabIndex);
		}
		return intent;
    }
}
