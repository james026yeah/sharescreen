package archermind.dlna.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.location.Geocoder;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import archermind.dlna.gallery.IImage;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A utility class to handle various kinds of menu operations.
 */
public class MenuHelper {
	private static final String TAG = "MenuHelper";
	
	public static final int INCLUDE_ALL = 0xFFFFFFFF;
	public static final int INCLUDE_VIEWPLAY_MENU = (1 << 0);
	public static final int INCLUDE_SHARE_MENU = (1 << 1);
	public static final int INCLUDE_SET_MENU = (1 << 2);
	public static final int INCLUDE_CROP_MENU = (1 << 3);
	public static final int INCLUDE_DELETE_MENU = (1 << 4);
	public static final int INCLUDE_ROTATE_MENU = (1 << 5);
	public static final int INCLUDE_DETAILS_MENU = (1 << 6);
	public static final int INCLUDE_SHOWMAP_MENU = (1 << 7);
	
	public static final int MENU_IMAGE_SHARE = 1;
	public static final int MENU_IMAGE_SHOWMAP = 2;
	
	public static final int POSITION_SWITCH_CAMERA_MODE = 1;
	public static final int POSITION_GOTO_GALLERY = 2;
	public static final int POSITION_VIEWPLAY = 3;
	public static final int POSITION_CAPTURE_PICTURE = 4;
	public static final int POSITION_CAPTURE_VIDEO = 5;
	public static final int POSITION_IMAGE_SHARE = 6;
	public static final int POSITION_IMAGE_ROTATE = 7;
	public static final int POSITION_IMAGE_TOSS = 8;
	public static final int POSITION_IMAGE_CROP = 9;
	public static final int POSITION_IMAGE_SET = 10;
	public static final int POSITION_DETAILS = 11;
	public static final int POSITION_SHOWMAP = 12;
	public static final int POSITION_SLIDESHOW = 13;
	public static final int POSITION_MULTISELECT = 14;
	public static final int POSITION_CAMERA_SETTING = 15;
	public static final int POSITION_GALLERY_SETTING = 16;
	
	public static final int NO_STORAGE_ERROR = -1;
	public static final int CANNOT_STAT_ERROR = -2;
	public static final String EMPTY_STRING = "";
	public static final String JPEG_MIME_TYPE = "image/jpeg";
	// valid range is -180f to +180f
	public static final float INVALID_LATLNG = 255f;
	
	/**
	 * Activity result code used to report crop results.
	 */
	public static final int RESULT_COMMON_MENU_CROP = 490;
	
	public interface MenuItemsResult {
		public void gettingReadyToOpen(Menu menu, IImage image);
		
		public void aboutToCall(MenuItem item, IImage image);
	}
	
	public interface MenuInvoker {
		public void run(MenuCallback r);
	}
	
	public interface MenuCallback {
		public void run(Uri uri, IImage image);
	}
	
	public static void closeSilently(Closeable c) {
		if (c != null) {
			try {
				c.close();
			}
			catch (Throwable e) {
				// ignore
			}
		}
	}
	
	public static long getImageFileSize(IImage image) {
		java.io.InputStream data = image.fullSizeImageData();
		if (data == null)
			return -1;
		try {
			return data.available();
		}
		catch (java.io.IOException ex) {
			return -1;
		}
		finally {
			closeSilently(data);
		}
	}
	
	// This is a hack before we find a solution to pass a permission to other
	// applications. See bug #1735149, #1836138.
	// Checks if the URI is on our whitelist:
	// content://media/... (MediaProvider)
	// file:///sdcard/... (Browser download)
	public static boolean isWhiteListUri(Uri uri) {
		if (uri == null)
			return false;
		
		String scheme = uri.getScheme();
		String authority = uri.getAuthority();
		
		if (scheme.equals("content") && authority.equals("media")) {
			return true;
		}
		
		if (scheme.equals("file")) {
			List<String> p = uri.getPathSegments();
			
			if (p.size() >= 1 && p.get(0).equals("sdcard")) {
				return true;
			}
		}
		
		return false;
	}
	
	public static void enableShareMenuItem(Menu menu, boolean enabled) {
		MenuItem item = menu.findItem(MENU_IMAGE_SHARE);
		if (item != null) {
			item.setVisible(enabled);
			item.setEnabled(enabled);
		}
	}
	
	public static boolean hasLatLngData(IImage image) {
		ExifInterface exif = getExif(image);
		if (exif == null)
			return false;
		float latlng[] = new float[2];
		return exif.getLatLong(latlng);
	}
	
	public static void enableShowOnMapMenuItem(Menu menu, boolean enabled) {
		MenuItem item = menu.findItem(MENU_IMAGE_SHOWMAP);
		if (item != null) {
			item.setEnabled(enabled);
		}
	}
	
	private static void setDetailsValue(View d, String text, int valueId) {
		((TextView) d.findViewById(valueId)).setText(text);
	}
	
	private static void hideDetailsRow(View d, int rowId) {
		d.findViewById(rowId).setVisibility(View.GONE);
	}
	
	private static class UpdateLocationCallback implements ReverseGeocoderTask.Callback {
		WeakReference<View> mView;
		
		public UpdateLocationCallback(WeakReference<View> view) {
			mView = view;
		}
		
		public void onComplete(String location) {
			// View d is per-thread data, so when setDetailsValue is
			// executed by UI thread, it doesn't matter whether the
			// details dialog is dismissed or not.
			View view = mView.get();
			if (view == null)
				return;
			if (!location.equals(MenuHelper.EMPTY_STRING)) {
			}
			else {
			}
		}
	}
	
	private static void setLatLngDetails(final View d, Activity context, ExifInterface exif) {
		float[] latlng = new float[2];
		if (exif.getLatLong(latlng)) {
			
			if (latlng[0] == INVALID_LATLNG || latlng[1] == INVALID_LATLNG) {
				return;
			}
			
			UpdateLocationCallback cb = new UpdateLocationCallback(new WeakReference<View>(d));
			Geocoder geocoder = new Geocoder(context);
			new ReverseGeocoderTask(geocoder, latlng, cb).execute();
		}
		else {
		}
	}
	
	private static ExifInterface getExif(IImage image) {
		if (!JPEG_MIME_TYPE.equals(image.getMimeType())) {
			return null;
		}
		
		try {
			return new ExifInterface(image.getDataPath());
		}
		catch (IOException ex) {
			Log.e(TAG, "cannot read exif", ex);
			return null;
		}
	}
	
	// Called when "Show on Maps" is clicked.
	// Displays image location on Google Maps for further operations.
	private static boolean onShowMapClicked(MenuInvoker onInvoke, final Handler handler, final Activity activity) {
		onInvoke.run(new MenuCallback() {
			public void run(Uri u, IImage image) {
				if (image == null) {
					return;
				}
				
				boolean ok = false;
				ExifInterface exif = getExif(image);
				float latlng[] = null;
				if (exif != null) {
					latlng = new float[2];
					if (exif.getLatLong(latlng)) {
						ok = true;
					}
				}
				
				if (!ok) {
					handler.post(new Runnable() {
						public void run() {
						}
					});
					return;
				}
				
				// Can't use geo:latitude,longitude because it only centers
				// the MapView to specified location, but we need a bubble
				// for further operations (routing to/from).
				// The q=(lat, lng) syntax is suggested by geo-team.
				String uri = "http://maps.google.com/maps?f=q&" + "q=(" + latlng[0] + "," + latlng[1] + ")";
				activity.startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri)));
			}
		});
		return true;
	}
	
	private static void hideExifInformation(View d) {
	}
	
	private static void showExifInformation(IImage image, View d, Activity activity) {
		ExifInterface exif = getExif(image);
		if (exif == null) {
			hideExifInformation(d);
			return;
		}
		
		String value = exif.getAttribute(ExifInterface.TAG_MAKE);
		if (value != null) {
		}
		else {
		}
		
		value = exif.getAttribute(ExifInterface.TAG_MODEL);
		if (value != null) {
		}
		else {
		}
		
		value = getWhiteBalanceString(exif);
		if (value != null && !value.equals(EMPTY_STRING)) {
		}
		else {
		}
		
		setLatLngDetails(d, activity, exif);
	}
	
	/**
	 * Returns a human-readable string describing the white balance value.
	 * Returns empty
	 * string if there is no white balance value or it is not recognized.
	 */
	private static String getWhiteBalanceString(ExifInterface exif) {
		int whitebalance = exif.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1);
		if (whitebalance == -1)
			return "";
		
		switch (whitebalance) {
			case ExifInterface.WHITEBALANCE_AUTO:
				return "Auto";
			case ExifInterface.WHITEBALANCE_MANUAL:
				return "Manual";
			default:
				return "";
		}
	}
	
	// Called when "Rotate left" or "Rotate right" is clicked.
	private static boolean onRotateClicked(MenuInvoker onInvoke, final int degree) {
		onInvoke.run(new MenuCallback() {
			public void run(Uri u, IImage image) {
				if (image == null || image.isReadonly()) {
					return;
				}
				image.rotateImageBy(degree);
			}
		});
		return true;
	}
	
	// Called when "Crop" is clicked.
//	private static boolean onCropClicked(MenuInvoker onInvoke, final Activity activity) {
//		onInvoke.run(new MenuCallback() {
//			public void run(Uri u, IImage image) {
//				if (u == null) {
//					return;
//				}
//				
//				Intent cropIntent = new Intent("com.android.camera.action.CROP");
//				cropIntent.setData(u);
//				activity.startActivityForResult(cropIntent, RESULT_COMMON_MENU_CROP);
//			}
//		});
//		return true;
//	}
	
	// Called when "Set as" is clicked.
//	private static boolean onSetAsClicked(MenuInvoker onInvoke, final Activity activity) {
//		onInvoke.run(new MenuCallback() {
//			public void run(Uri u, IImage image) {
//				if (u == null || image == null) {
//					return;
//				}
//				
//				Intent intent = Util.createSetAsIntent(image);
//				activity.startActivity(Intent.createChooser(intent, activity.getText(R.string.setImage)));
//			}
//		});
//		return true;
//	}
	
	// Called when "Share" is clicked.
//	private static boolean onImageShareClicked(MenuInvoker onInvoke, final Activity activity) {
//		onInvoke.run(new MenuCallback() {
//			public void run(Uri u, IImage image) {
//				if (image == null)
//					return;
//				
//				Intent intent = new Intent();
//				intent.setAction(Intent.ACTION_SEND);
//				String mimeType = image.getMimeType();
//				intent.setType(mimeType);
//				intent.putExtra(Intent.EXTRA_STREAM, u);
//				boolean isImage = ImageManager.isImage(image);
//				try {
//					activity.startActivity(Intent.createChooser(intent,
//							activity.getText(isImage ? R.string.sendImage : R.string.sendVideo)));
//				}
//				catch (android.content.ActivityNotFoundException ex) {
////					Toast.makeText(activity, isImage ? R.string.no_way_to_share_image : R.string.no_way_to_share_video,
////							Toast.LENGTH_SHORT).show();
//				}
//			}
//		});
//		return true;
//	}
	
	// Called when "Play" is clicked.
	private static boolean onViewPlayClicked(MenuInvoker onInvoke, final Activity activity) {
		onInvoke.run(new MenuCallback() {
			public void run(Uri uri, IImage image) {
				if (image != null) {
					Intent intent = new Intent(Intent.ACTION_VIEW, image.fullSizeImageUri());
					activity.startActivity(intent);
				}
			}
		});
		return true;
	}
	
	// Called when "Delete" is clicked.
//	private static boolean onDeleteClicked(MenuInvoker onInvoke, final Activity activity, final Runnable onDelete) {
//		onInvoke.run(new MenuCallback() {
//			public void run(Uri uri, IImage image) {
//				if (image != null) {
//					deleteImage(activity, onDelete, image);
//				}
//			}
//		});
//		return true;
//	}
	
//		if ((inclusions & INCLUDE_SET_MENU) != 0) {
//			MenuItem setMenu = menu.add(Menu.NONE, Menu.NONE, POSITION_IMAGE_SET, R.string.camera_set);
//			setMenu.setIcon(android.R.drawable.ic_menu_set_as);
//			setMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//				public boolean onMenuItemClick(MenuItem item) {
//					return onSetAsClicked(onInvoke, activity);
//				}
//			});
//			requiresImageItems.add(setMenu);
//		}
//		
//		if ((inclusions & INCLUDE_SHARE_MENU) != 0) {
//			MenuItem item1 = menu.add(Menu.NONE, MENU_IMAGE_SHARE, POSITION_IMAGE_SHARE, R.string.camera_share)
//					.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//						public boolean onMenuItemClick(MenuItem item) {
//							return onImageShareClicked(onInvoke, activity);
//						}
//					});
//			item1.setIcon(android.R.drawable.ic_menu_share);
//			MenuItem item = item1;
//			requiresNoDrmAccessItems.add(item);
//		}
//		
//		if ((inclusions & INCLUDE_DELETE_MENU) != 0) {
//			MenuItem deleteItem = menu.add(Menu.NONE, Menu.NONE, POSITION_IMAGE_TOSS, R.string.camera_toss);
//			requiresWriteAccessItems.add(deleteItem);
//			deleteItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//				public boolean onMenuItemClick(MenuItem item) {
//					return onDeleteClicked(onInvoke, activity, onDelete);
//				}
//			}).setAlphabeticShortcut('d').setIcon(android.R.drawable.ic_menu_delete);
//		}
	
}
