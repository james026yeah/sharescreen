package archermind.dlna.mobile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.MediaStore.Video.Thumbnails;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import archermind.dlna.media.Album;
import archermind.dlna.media.Artist;
import archermind.dlna.media.MusicCategoryInfo;
import archermind.dlna.media.MusicItem;
import archermind.dlna.media.PhotoAlbum;
import archermind.dlna.media.PhotoItem;
import archermind.dlna.media.VideoCategory;
import archermind.dlna.media.VideoItem;

@SuppressLint({ "HandlerLeak", "HandlerLeak"
})
public class LocalMediaActivity extends BaseActivity {
	
	private static final String TAG = "LocalMediaActivity";
	private static final boolean DBG = true;
	
	private static final int CURRENT_TAB_IMAGE_HEIGHT = 5;
	
	public static final int TIME_SECOND = 1000;
	public static final int TIME_MINUTE = TIME_SECOND * 60;
	public static final int TIME_HOUR = TIME_MINUTE * 60; 
	
	private static final int TAB_IMAGE = 0;
	private static final int TAB_MUSIC = 1;
	private static final int TAB_VIDEO = 2;
	
	private static final int PROGRESS_BAR_DISMISS = 0;
	
	private RelativeLayout mMainView;
	
	private LinearLayout mImageThumbnailView;
	private LinearLayout mImageThumbnailBackView;
	private TextView mImageThumbnailNameView;
	
	private LinearLayout mMusicListView;
	
	private LinearLayout mImageTab;
	private LinearLayout mMusicTab;
	private LinearLayout mVedioTab;
	private LinearLayout mCurrentTab;
	private ViewPager mViewPager;
	private GridView mImageFrameGridView;
	private GridView mImageThumbnailGridView;
	private RelativeLayout mNoImagesView;
	private RelativeLayout mNoVideosView;
	
	private Animation mProgressBarAnim;
	private LinearLayout mProgressBar;
	private ImageView mProgressIcon;
	private TextView mProgressText;
	
	private ListView mMusicList;
	
	private ArrayList<VideoCategory> mVideoCategoryList;
	private ArrayList<PhotoAlbum> mPhotoAlbumList;
	private ArrayList<View> mViewAdapters;
	
	private int mCurrentTabIndex = 0;
	private int mScreenWidth;
	private boolean mIsMusicList = false;
	private boolean mOnGetVideoFinished = false;
	private boolean mOnGetPhotoFinished = false;
	
	private ViewPagerAdapter mViewPagerAdapter;
	private MyImageAdapter mImageFrameAdapter;
	private MyImageAdapter mImageThumbnailAdapter;
	private VideoListViewAdapter mVideoListViewAdapter;
	private ExpandableListView mExpandableListView;
	
	private AlbumListAdapter mAlbumListAdapter = null;
	private ArtistListAdapter mArtistListAdapter = null;
	private MusicListAdapter mAllMusicAdapter = null;
	
	private ArrayList<MusicItem> mAllMusicItem;
	private ArrayList<MusicCategoryInfo> mMusicCateInfoItem;
	private ArrayList<Artist> mMusicArtistItem;
	private ArrayList<Album> mMusicAlbumItem;
	private boolean mOnGetMusicFinished = false;
	private boolean mOnGetMusicCateInfoFinished = false;
	private boolean mOnGetMusicArtistFinished = false;
	private boolean mOnGetMusicAlbumFinished = false;
	private IMusicPlayService mMusicPlaySer = null;
	private static List<String> mAllMusicList = new ArrayList<String>();
	private TextView mAllMusicNum;
	private TextView mAlbumNum;
	private TextView mAritistNum;
	private TextView mMusicListTitle;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log(TAG + "is onCreate");
		
		setContentView(R.layout.local_media);
		this.getApplicationContext().bindService(new Intent(IMusicPlayService.class.getName()), mMusicSerConn,
				Context.BIND_AUTO_CREATE);
		
		getScreenWidth();
		initMainUI();
		initTab();
		initViewPager();
		initCurrentTabImage(TAB_IMAGE);
		
		showProgressBar();
		mHandler.sendEmptyMessageDelayed(PROGRESS_BAR_DISMISS, 1000 * 15);
	}
	
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case PROGRESS_BAR_DISMISS: {
					dismissProgressBar();
					break;
				}
			}
		};
	};
	
	@Override
	protected void onGetMusicCategoryData(ArrayList<MusicCategoryInfo> musicCategory) {
		mOnGetMusicCateInfoFinished = true;
		if (musicCategory == null) {
			log("no musicCategory");
		}
		else {
			mMusicCateInfoItem = musicCategory;
			log("musics number = " + mMusicCateInfoItem.size());
		}
	}
	
	@Override
	protected void onGetMusicArtistsData(ArrayList<Artist> artists) {
		mOnGetMusicArtistFinished = true;
		if (artists == null) {
			log("no artists exists");
		}
		else {
			mMusicArtistItem = artists;
			log("artists number = " + mMusicArtistItem.size());
			initMusicArtistList();
			mAritistNum = (TextView) findViewById(R.id.artistcount);
			mAritistNum.setText(getResources().getString(R.string.lable_artist) + "（" + mMusicArtistItem.size() + "）");
		}
	}
	
	@Override
	protected void onGetMusicAlbumsData(ArrayList<Album> albums) {
		mOnGetMusicAlbumFinished = true;
		if (albums == null) {
			log("no albums info");
		}
		else {
			mMusicAlbumItem = albums;
			log("albums number = " + mMusicAlbumItem.size());
			initMusicAlbumList();
			mAlbumNum = (TextView) findViewById(R.id.albumcount);
			mAlbumNum.setText(getResources().getString(R.string.lable_album) + "（" + mMusicAlbumItem.size() + "）");
		}
	}
	
	@Override
	protected void onGetMusicAllData(ArrayList<MusicItem> musics) {
		mOnGetMusicFinished = true;
		if (musics == null) {
			log("no musics");
		}
		else {
			mAllMusicItem = musics;
			for (int position = 0; position < mAllMusicItem.size(); position++){
				mAllMusicList.add(mAllMusicItem.get(position).getFilePath());
			}
			log("musics number = " + mAllMusicItem.size());
			initMusicList();
			mAllMusicNum = (TextView) findViewById(R.id.allmusiccount);
			mAllMusicNum.setText(getResources().getString(R.string.lable_all_music) + "（" + mAllMusicItem.size() + "）");
		}
	}
	
	@Override
	protected void onGetVideoCategory(ArrayList<VideoCategory> videoCategory) {
		mOnGetVideoFinished = true;
		if (videoCategory == null) {
			log("video categroy is null");
		}
		else {
			mVideoCategoryList = videoCategory;
			log("video categroy size = " + mVideoCategoryList.size());
			initVideoGridView();
		}
	}
	
	@Override
	protected void onGetPhotos(ArrayList<PhotoAlbum> photoAlbum) {
		mOnGetPhotoFinished = true;
		dismissProgressBar();
		if (photoAlbum == null) {
			log("photo album is null");
		}
		else {
			mPhotoAlbumList = photoAlbum;
			log("photo album size = " + mPhotoAlbumList.size());
			initImageFrameGridView();
		}
	}
	
	@Override
	protected void onLocalMDMSStatusChanged(Message msg) {
		super.onLocalMDMSStatusChanged(msg);
		log("onLocalMDMSStatusChanged is call");
		if (msg.arg1 == MessageDefs.LOCAL_MDMS_STATUS_ONLINE) {
			log("connection is success");
			getMusicCategoryData();
			getMusicAlbumsData();
			getMusicArtistssData();
			getMusicAllData();
			getVideosData();
			getPhotosData();
		}
	}
	
	private void showProgressBar() {
		mProgressBar = (LinearLayout) findViewById(R.id.progress_bar);
		mProgressBarAnim = AnimationUtils.loadAnimation(this, R.anim.progress_bar_anim);
		mProgressIcon = (ImageView) findViewById(R.id.progress_icon);
		mProgressText = (TextView) findViewById(R.id.progress_text);
		
		mProgressBar.setVisibility(View.VISIBLE);
		mProgressIcon.startAnimation(mProgressBarAnim);
		mProgressText.setText(getResources().getString(R.string.local_media_progress_dialog_message));
	}
	
	private void dismissProgressBar() {
		mProgressBar.setVisibility(View.INVISIBLE);
		mProgressIcon.clearAnimation();
	}
	
	private void getScreenWidth() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		mScreenWidth = dm.widthPixels;
	}
	
	private void initMainUI() {
		mMainView = (RelativeLayout) findViewById(R.id.local_media_main);
		mImageThumbnailView = (LinearLayout) findViewById(R.id.local_media_image_thumbnail);
		mImageThumbnailView.setVisibility(View.GONE);
		mMusicListView = (LinearLayout) findViewById(R.id.local_media_music_list);
		mMusicListView.setVisibility(View.GONE);
	}
	
	private void initTab() {
		mImageTab = (LinearLayout) findViewById(R.id.tab_image);
		mMusicTab = (LinearLayout) findViewById(R.id.tab_music);
		mVedioTab = (LinearLayout) findViewById(R.id.tab_video);
		
		mImageTab.setOnClickListener(new TabOnClickListener(TAB_IMAGE));
		mMusicTab.setOnClickListener(new TabOnClickListener(TAB_MUSIC));
		mVedioTab.setOnClickListener(new TabOnClickListener(TAB_VIDEO));
	}
	
	private void initViewPager() {
		mViewPager = (ViewPager) findViewById(R.id.view_pager);
		mViewAdapters = new ArrayList<View>();
		
		LayoutInflater inflater = getLayoutInflater();
		
		View imageView = inflater.inflate(R.layout.local_media_image, null);
		mImageFrameGridView = (GridView) imageView.findViewById(R.id.image_frame_gridview);
		mNoImagesView = (RelativeLayout) imageView.findViewById(R.id.no_images);
		mViewAdapters.add(imageView);
		
		View musicView = inflater.inflate(R.layout.local_media_music, null);
		mViewAdapters.add(musicView);
		
		View videoView = inflater.inflate(R.layout.local_media_video, null);
		mExpandableListView = (ExpandableListView) videoView.findViewById(R.id.expandable_listview);
		mNoVideosView = (RelativeLayout) videoView.findViewById(R.id.no_videos);
		mViewAdapters.add(videoView);
		
		mViewPagerAdapter = new ViewPagerAdapter(mViewAdapters);
		mViewPager.setAdapter(mViewPagerAdapter);
		mViewPager.setCurrentItem(TAB_IMAGE);
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			
			@Override
			public void onPageSelected(int arg0) {
				mCurrentTabIndex = arg0;
				initCurrentTabImage(arg0);
			}
			
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}
			
			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});
	}
	
	private void initCurrentTabImage(int index) {
		mCurrentTab = (LinearLayout) findViewById(R.id.current_tab);
		mCurrentTab.removeAllViews();
		
		LinearLayout tab = (LinearLayout) findViewById(R.id.tab);
		int height = tab.getLayoutParams().height;
		
		int currentTabImageWidth = mScreenWidth / 3;
		int left = index * currentTabImageWidth;
		int right = left + currentTabImageWidth;
		
		mCurrentTab.addView(new CurrentTabView(this, left, height - CURRENT_TAB_IMAGE_HEIGHT * 2, right, height
				- CURRENT_TAB_IMAGE_HEIGHT));
		
	}
	
	private void initImageFrameGridView() {
		log("initImageFrameGridView is call");
		
		// get the data from the service
		if (mOnGetPhotoFinished) {
			// the data is null
			if (mPhotoAlbumList.size() == 0) {
				mImageFrameGridView.setVisibility(View.GONE);
				mNoImagesView.setVisibility(View.VISIBLE);
			}
			// have the photo
			else {
				mNoImagesView.setVisibility(View.GONE);
				mImageFrameGridView.setVisibility(View.VISIBLE);
				mImageFrameAdapter = new MyImageAdapter(mPhotoAlbumList, null);
				mImageFrameGridView.setAdapter(mImageFrameAdapter);
				mImageFrameGridView.setOnItemClickListener(new OnItemClickListener() {
					
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						PhotoAlbum album = mPhotoAlbumList.get(position);
						String folderName = album.getName();
						ArrayList<PhotoItem> list = album.getImageList();
						int size = list.size();
						initImageGridView(folderName, size, list);
					}
				});
			}
		}
	}
	
	private void initImageGridView(String folderName, int size, final ArrayList<PhotoItem> imageList) {
		log("call initImageGridView");
		
		mMainView.setVisibility(View.GONE);
		mImageThumbnailView.setVisibility(View.VISIBLE);
		
		mImageThumbnailBackView = (LinearLayout) findViewById(R.id.image_thumbnail_back);
		mImageThumbnailBackView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mImageThumbnailView.setVisibility(View.GONE);
				mMainView.setVisibility(View.VISIBLE);
			}
		});
		
		mImageThumbnailNameView = (TextView) findViewById(R.id.image_thumbnail_name);
		mImageThumbnailNameView.setText(folderName + " ( " + size + " ) ");
		
		mImageThumbnailGridView = (GridView) findViewById(R.id.image_thumbnail_gridview);
		mImageThumbnailAdapter = new MyImageAdapter(null, imageList);
		mImageThumbnailGridView.setAdapter(mImageThumbnailAdapter);
		mImageThumbnailGridView.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				log("image uri = " + imageList.get(position).getItemUri());
				postPlay(imageList.get(position).getItemUri(), IMAGE_TYPE);
				
				Intent intent = new Intent(LocalMediaActivity.this, ImageViewActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
				intent.putExtra(ImageViewActivity.IMAGE_INDEX, position);
				ImageViewActivity.sImageItemList = imageList;
				startActivity(intent);
				
			}
		});
	}
	
	private void initVideoGridView() {
		log("call initVideoFrameGridView");
		
		// get the data from service
		if (mOnGetVideoFinished) {
			// the data is null
			if (mVideoCategoryList.size() == 0) {
				mExpandableListView.setVisibility(View.GONE);
				mNoVideosView.setVisibility(View.VISIBLE);
			}
			// the data is not null
			else {
				mNoVideosView.setVisibility(View.GONE);
				mExpandableListView.setVisibility(View.VISIBLE);
				mVideoListViewAdapter = new VideoListViewAdapter(this, mVideoCategoryList);
				mExpandableListView.setAdapter(mVideoListViewAdapter);
				mExpandableListView.setGroupIndicator(null);
				
				// default to show all video
				int groupCount = mVideoListViewAdapter.getGroupCount();
				for (int i = 0; i < groupCount; i++) {
					mExpandableListView.expandGroup(i);
				}
			}
		}
		
	}
	
	private void initMusicList() {
		log("initMusicList is call");
		// get the data from the service
		if (mOnGetMusicFinished) {
			// the data is null
			if (mAllMusicItem == null) {
				mAllMusicAdapter = null;
			}
			// have the music list
			else {
				mAllMusicAdapter = new MusicListAdapter(mAllMusicItem);
			}
		}
	}
	
	private void initMusicAlbumList() {
		log("initMusicAlbumList is call");
		// get the data from the service
		if (mOnGetMusicAlbumFinished) {
			// the data is null
			if (mMusicAlbumItem == null) {
				mAlbumListAdapter = null;
			}
			// have the music album list
			else {
				mAlbumListAdapter = new AlbumListAdapter(mMusicAlbumItem);
			}
		}
	}
	
	private void initMusicArtistList() {
		log("initMusicArtistList is call");
		
		// get the data from the service
		if (mOnGetMusicArtistFinished) {
			// the data is null
			if (mMusicArtistItem == null) {
				mArtistListAdapter = null;
			}
			// have the music artst list
			else {
				mArtistListAdapter = new ArtistListAdapter(mMusicArtistItem);
			}
		}
	}
	
	@SuppressLint("DrawAllocation")
	private class CurrentTabView extends View {
		
		private int left;
		private int right;
		private int top;
		private int bottom;
		
		public CurrentTabView(Context context, int l, int t, int r, int b) {
			super(context);
			left = l;
			right = r;
			top = t;
			bottom = b;
		}
		
		@SuppressLint("DrawAllocation")
		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setColor(Color.rgb(127, 185, 234));
			paint.setStyle(Paint.Style.FILL);
			
			canvas.drawRect(left, top, right, bottom, paint);
		}
		
	}
	
	private class TabOnClickListener implements OnClickListener {
		
		private int index = 0;
		
		public TabOnClickListener(int i) {
			index = i;
		}
		
		@Override
		public void onClick(View v) {
			initCurrentTabImage(index);
			mViewPager.setCurrentItem(index);
		}
	}
	
	private class ViewPagerAdapter extends PagerAdapter {
		
		private ArrayList<View> list;
		
		public ViewPagerAdapter(ArrayList<View> views) {
			list = views;
		}
		
		@Override
		public int getItemPosition(Object object) {
			return POSITION_NONE;
		}
		
		@Override
		public int getCount() {
			return list.size();
		}
		
		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == arg1;
		}
		
		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			((ViewPager) container).removeView(list.get(position));
		}
		
		@Override
		public Object instantiateItem(ViewGroup container, int position) {
			((ViewPager) container).addView(list.get(position), 0);
			return list.get(position);
		}
		
	}
	
	class MusicListAdapter extends BaseAdapter {
		private ArrayList<MusicItem> mAllMusic;
		
		MusicListAdapter(ArrayList<MusicItem> AllMusicItem) {
			mAllMusic = AllMusicItem;
		}
		
		public int getCount() {
			return mAllMusic.size();
		}
		
		public Object getItem(int position) {
			return position;
		}
		
		public long getItemId(int position) {
			return position;
		}
		
		public View getView(final int position, View convertView, ViewGroup parent) {
			
			View view;
			ImageView img;
			TextView titlemain;
			TextView titlesec;
			TextView detail;
			
			Bitmap bm = null;

			ContentResolver res = getApplicationContext().getContentResolver();
			Uri uri = Uri.parse(mAllMusic.get(position).getAlbumArtURI());
			if (convertView == null) {
				view = getLayoutInflater().inflate(R.layout.music_list_item, null);
			}
			else {
				view = convertView;
			}
			img = (ImageView) view.findViewById(R.id.img);
			titlemain = (TextView) view.findViewById(R.id.title_main);
			titlesec = (TextView) view.findViewById(R.id.title_sec);
			detail = (TextView) view.findViewById(R.id.detail);

			titlemain.setText(mAllMusic.get(position).getTitle());
			titlesec.setText(mAllMusic.get(position).getArtist());
			log("artist =" + mAllMusic.get(position).getArtist());
			detail.setText(setDurationFormat(Integer.parseInt(mAllMusic.get(position).getDuration())));
			if (getMusicImg(mAllMusic.get(position).getAlbumArtURI()) != null) {
				img.setImageBitmap(getMusicImg(mAllMusic.get(position).getAlbumArtURI()));
			}
			return view;
		}
	}
	
	class AlbumListAdapter extends BaseAdapter {
		private ArrayList<Album> mMusicAlbum;
		
		AlbumListAdapter(ArrayList<Album> AlbumListItem) {
			mMusicAlbum = AlbumListItem;
		}
		
		public int getCount() {
			return mMusicAlbum.size();
		}
		
		public Object getItem(int position) {
			return position;
		}
		
		public long getItemId(int position) {
			return position;
		}
		
		public View getView(final int position, View convertView, ViewGroup parent) {
			
			View view;
			ImageView img;
			TextView titlemain;
			TextView titlesec;
			TextView detail;

			if (convertView == null) {
				view = getLayoutInflater().inflate(R.layout.music_list_item, null);
			}
			else {
				view = convertView;
			}

			img = (ImageView) view.findViewById(R.id.img);
			titlemain = (TextView) view.findViewById(R.id.title_main);
			titlesec = (TextView) view.findViewById(R.id.title_sec);
			detail = (TextView) view.findViewById(R.id.detail);

			titlemain.setText(mMusicAlbum.get(position).getName());
			titlesec.setText(mMusicAlbum.get(position).getMusicsList().size() + getResources().getString(R.string.music_num));
			detail.setText("");
			if (getMusicImg(mMusicAlbum.get(position).getMusicsList().get(0).getAlbumArtURI()) != null) {
				img.setImageBitmap(getMusicImg(mMusicAlbum.get(position).getMusicsList().get(0).getAlbumArtURI()));
			}

			return view;
		}
	}
	
	class ArtistListAdapter extends BaseAdapter {
		private ArrayList<Artist> mMusicArtist;
		
		ArtistListAdapter(ArrayList<Artist> ArtistListItem) {
			mMusicArtist = ArtistListItem;
		}
		
		public int getCount() {
			return mMusicArtist.size();
		}
		
		public Object getItem(int position) {
			return position;
		}
		
		public long getItemId(int position) {
			return position;
		}
		
		public View getView(final int position, View convertView, ViewGroup parent) {
			
			View view;
			ImageView img;
			TextView titlemain;
			TextView titlesec;
			TextView detail;

			if (convertView == null) {
				view = getLayoutInflater().inflate(R.layout.music_list_item, null);
			}
			else {
				view = convertView;
			}

			img = (ImageView) view.findViewById(R.id.img);
			titlemain = (TextView) view.findViewById(R.id.title_main);
			titlesec = (TextView) view.findViewById(R.id.title_sec);
			detail = (TextView) view.findViewById(R.id.detail);

			titlemain.setText(mMusicArtist.get(position).getName());
			titlesec.setText(mMusicArtistItem.get(position).getMusicsList().size() + getResources().getString(R.string.music_num));
			detail.setText("");
			if (getMusicImg(mMusicArtistItem.get(position).getMusicsList().get(0).getAlbumArtURI()) != null) {
				img.setImageBitmap(getMusicImg(mMusicArtistItem.get(position).getMusicsList().get(0).getAlbumArtURI()));
			}

			return view;
		}
	}
	
	private class MyImageAdapter extends BaseAdapter {
		
		private ArrayList<PhotoAlbum> mPhotoAlbums;
		private ArrayList<PhotoItem> mPhotoItems;
		
		public MyImageAdapter(ArrayList<PhotoAlbum> photoAlbums, ArrayList<PhotoItem> photoItems) {
			mPhotoAlbums = photoAlbums;
			mPhotoItems = photoItems;
		}
		
		@Override
		public int getCount() {
			if (mPhotoAlbums != null) {
				return mPhotoAlbums.size();
			}
			else if (mPhotoItems != null) {
				return mPhotoItems.size();
			}
			return 0;
		}
		
		@Override
		public Object getItem(int position) {
			return position;
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		private Bitmap createThumbnail(String path, int width, int height) {
			try {
				File file = new File(path);
				// Decode image size
				BitmapFactory.Options o = new BitmapFactory.Options();
				o.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(new FileInputStream(file), null, o);
				
				// The new size we want to scale to
				// final int REQUIRED_SIZE=getScreenWidthAndHeight();
				// Find the correct scale value. It should be the power of
				// 2.
				int scale = 1;
				while (o.outWidth / scale / 2 >= width - 20 && o.outHeight / scale / 2 >= height - 20)
					scale *= 2;
				o.inJustDecodeBounds = false;
				// Decode with inSampleSize
				BitmapFactory.Options o2 = new BitmapFactory.Options();
				o2.inSampleSize = scale;
				return ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(new FileInputStream(file), null, o2),
						width - 20, height - 20, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
			}
			catch (FileNotFoundException e) {
			}
			return null;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			View view = null;
			
			if (mPhotoAlbums != null) {
				if (convertView == null) {
					view = getLayoutInflater().inflate(R.layout.local_media_image_frame_item, null);
				}
				else {
					view = convertView;
				}
				
				int width = mScreenWidth / 2;
				int height = width;
				
				mImageFrameGridView.setColumnWidth(width);
				
				ImageFrameItem image = (ImageFrameItem) view.findViewById(R.id.frame_thumbnail);
				image.setLayoutParams(new LinearLayout.LayoutParams(width - 20, height - 20));
				
				String path = mPhotoAlbums.get(position).getImageList().get(0).getFilePath();
				// Bitmap bitmap = BitmapFactory.decodeFile(path);
				image.setBackgroundDrawable(new BitmapDrawable(createThumbnail(path, width, height)));
				
				TextView count = (TextView) view.findViewById(R.id.frame_count);
				TextView name = (TextView) view.findViewById(R.id.frame_name);
				
				count.setText(mPhotoAlbums.get(position).getImageList().size() + "");
				name.setText(mPhotoAlbums.get(position).getName());
			}
			else if (mPhotoItems != null) {
				
				if (convertView == null) {
					view = getLayoutInflater().inflate(R.layout.local_media_image_thumbnail_item, null);
				}
				else {
					view = convertView;
				}
				
				int width = mScreenWidth / 3;
				int height = width;
				
				mImageThumbnailGridView.setColumnWidth(width);
				
				ImageThumbnailItem image = (ImageThumbnailItem) view.findViewById(R.id.image_thumbnail_item);
				image.setLayoutParams(new LinearLayout.LayoutParams(width - 20, height - 20));
				
				String path = mPhotoItems.get(position).getFilePath();
				image.setBackgroundDrawable(new BitmapDrawable(createThumbnail(path, width, height)));
			}
			return view;
		}
		
	}
	
	private class VideoListViewAdapter extends BaseExpandableListAdapter {
		
		public static final int mItemHeight = 45;
		
		private VideoGridView mVideoGridView;
		private ArrayList<VideoCategory> mVideoCategories;
		private List<TreeNode> treeNodes = new ArrayList<TreeNode>();
		private Context mContext;
		private int mGroupPosition;
		
		public VideoListViewAdapter(Context context, ArrayList<VideoCategory> videoCategory) {
			mContext = context;
			mVideoCategories = videoCategory;
			setTreeNode();
		}
		
		private void setTreeNode() {
			for (int i = 0; i < mVideoCategories.size(); i++) {
				TreeNode node = new TreeNode();
				node.parent = mVideoCategories.get(i).getName();
				node.childs.add(mVideoCategories.get(i).getVideosList());
				treeNodes.add(node);
			}
		}
		
		@SuppressWarnings("unused")
		public void RemoveAll() {
			treeNodes.clear();
		}
		
		public Object getChild(int groupPosition, int childPosition) {
			return treeNodes.get(groupPosition).childs.get(childPosition);
		}
		
		public int getChildrenCount(int groupPosition) {
			return treeNodes.get(groupPosition).childs.size();
		}
		
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
				ViewGroup parent) {
			convertView = getLayoutInflater().inflate(R.layout.local_media_video_gridview, null);
			mVideoGridView = (VideoGridView) convertView.findViewById(R.id.video_gridview);
			mVideoGridView.setNumColumns(3);
			mVideoGridView.setGravity(Gravity.CENTER);
			mVideoGridView.setHorizontalSpacing(15);
			ArrayList<VideoItem> videoItem = mVideoCategories.get(groupPosition).getVideosList();
			final VideoGridViewAdapter adapter = new VideoGridViewAdapter(mVideoGridView, videoItem, groupPosition);
			mVideoGridView.setAdapter(adapter);
			mVideoGridView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					mGroupPosition = adapter.getGroupPosition();
					Intent intent = new Intent(mContext, VideoViewActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
					intent.putExtra(VideoViewActivity.VIDEO_INDEX, position);
					log("mGroupPosition = " + mGroupPosition);
					VideoViewActivity.sVideoItemList = mVideoCategories.get(mGroupPosition).getVideosList();
					mContext.startActivity(intent);
				}
			});
			return convertView;
		}
		
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, mItemHeight);
			convertView = (RelativeLayout) getLayoutInflater().inflate(R.layout.local_media_video_group_item, null);
			convertView.setLayoutParams(lp);
			TextView textView = (TextView) convertView.findViewById(R.id.video_group_item_text);
			textView.setText(getGroup(groupPosition).toString());
			return convertView;
		}
		
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}
		
		public Object getGroup(int groupPosition) {
			return treeNodes.get(groupPosition).parent;
		}
		
		public int getGroupCount() {
			return treeNodes.size();
		}
		
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}
		
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
		
		public boolean hasStableIds() {
			return true;
		}
		
		private class TreeNode {
			Object parent;
			List<Object> childs = new ArrayList<Object>();
		}
		
	}
	
	private class VideoGridViewAdapter extends BaseAdapter {

		private VideoGridView mVideoGridView;
		private ArrayList<VideoItem> mVideoItems;
		private int mGroupPosition;
		
		public VideoGridViewAdapter(VideoGridView view, ArrayList<VideoItem> videoItems, 
				int groupPosition) {
			mGroupPosition = groupPosition;
			mVideoGridView = view;
			mVideoItems = videoItems;
		}
		
		@Override
		public int getCount() {
			return mVideoItems.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		public int getGroupPosition() {
			return mGroupPosition;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			
			if(convertView == null) {
				view = getLayoutInflater().inflate(R.layout.local_media_video_gridview_item, null);
			}
			else {
				view = convertView;
			}
			
			int width = mScreenWidth / 3;
			int height = width;
			
			mVideoGridView.setColumnWidth(width);
			mVideoGridView.setNumColumns(GridView.AUTO_FIT);
			
			ImageThumbnailItem image = (ImageThumbnailItem) view.findViewById(R.id.video_gridview_item_image);
			image.setLayoutParams(new LinearLayout.LayoutParams(width, height));
			
			VideoItem videoItem = mVideoItems.get(position);
			
			String path = videoItem.getFilePath();
			image.setBackgroundDrawable(new BitmapDrawable(ThumbnailUtils.createVideoThumbnail(path, Thumbnails.MINI_KIND)));
			
			int duration = Integer.parseInt(videoItem.getDuration());
			TextView time = (TextView) view.findViewById(R.id.video_gridview_item_time);
			time.setText(setDurationFormat(duration));
			
			return view;
		}
		
	}
	
	public static String setDurationFormat(int duration) {
		
		int hour = duration / TIME_HOUR;
		int minute = (duration - hour * TIME_HOUR) / TIME_MINUTE;
		int second = (duration - hour * TIME_HOUR - minute * TIME_MINUTE) / TIME_SECOND;
		
		if(hour == 0) {
			return String.format("%02d:%02d", minute, second);
		}
		else {
			return String.format("%02d:%02d:%02d", hour, minute, second);
		}
	}
	
	private void log(String str) {
		if (DBG) {
			Log.e(TAG, str);
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				if (mCurrentTabIndex == TAB_MUSIC && mIsMusicList) {
					mIsMusicList = false;
					mMusicListView.setVisibility(View.GONE);
					mMainView.setVisibility(View.VISIBLE);
					mViewPagerAdapter.notifyDataSetChanged();
					return true;
				}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public void doClick(View view) throws RemoteException {

		switch (view.getId()) {
		case R.id.play_info:
			if (mMusicPlaySer.getPlayList() != null) {
				Intent intent = new Intent();
				intent.setClass(getApplicationContext(),
						MusicPlayActivity.class);
				startActivity(intent);
			} else {
				Toast.makeText(getApplicationContext(), R.string.no_music_playing, Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.play_status_button:
			break;
		case R.id.allsongs_btn:
			if (mOnGetMusicFinished) {

				mIsMusicList = true;
				mMusicListTitle = (TextView) findViewById(R.id.list_title);
				mMusicList = (ListView) findViewById(R.id.music_list);
				mMusicListTitle.setText(getResources().getString(R.string.lable_all_music) + "（" + mAllMusicItem.size() + "）");
				mMusicList.setAdapter(mAllMusicAdapter);
				try {
					mMusicPlaySer.setPlayList(mAllMusicItem);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				mMusicList.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						try {
							mMusicPlaySer.playFrom(position);
							mMusicPlaySer.play();
						} catch (RemoteException e) {
							e.printStackTrace();
						}
						Intent intent = new Intent(getApplicationContext(),
								MusicPlayActivity.class);
						startActivity(intent);
					}
				});
				mMainView.setVisibility(View.GONE);
				mMusicListView.setVisibility(View.VISIBLE);
				mViewPagerAdapter.notifyDataSetChanged();
			} else {
				Toast.makeText(getApplicationContext(), R.string.music_data_not_init, Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.album_btn:
			if (mOnGetMusicAlbumFinished) {
				mIsMusicList = true;
				mMusicListTitle = (TextView) findViewById(R.id.list_title);
				mMusicList = (ListView) findViewById(R.id.music_list);
				mMusicList.setAdapter(mAlbumListAdapter);
				mMusicListTitle.setText(getResources().getString(R.string.lable_album) + "（" + mMusicAlbumItem.size() + "）");
				mMainView.setVisibility(View.GONE);
				mMusicListView.setVisibility(View.VISIBLE);
				mMusicList.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int position, long arg3) {
						Intent intent = new Intent();
						intent.putExtra("title", mMusicAlbumItem.get(position)
								.getName());
						ArrayList<MusicItem> music = mMusicAlbumItem.get(
								position).getMusicsList();
						try {
							mMusicPlaySer.setMusicShowList(music);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
						intent.setClass(getApplicationContext(),
								MusicListActivity.class);
						startActivity(intent);
					}
				});
				mViewPagerAdapter.notifyDataSetChanged();
			} else {
				Toast.makeText(getApplicationContext(), R.string.music_data_not_init, Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.artist_btn:
			if (mOnGetMusicArtistFinished) {
				mIsMusicList = true;
				mMusicListTitle = (TextView) findViewById(R.id.list_title);
				mMusicList = (ListView) findViewById(R.id.music_list);
				mMusicList.setAdapter(mArtistListAdapter);
				mMusicListTitle.setText(getResources().getString(R.string.lable_artist) + "（" + mMusicArtistItem.size() + "）");
				mMainView.setVisibility(View.GONE);
				mMusicListView.setVisibility(View.VISIBLE);
				mMusicList.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int position, long arg3) {
						Intent intent = new Intent();
						intent.putExtra("title", mMusicArtistItem.get(position)
								.getName());
						ArrayList<MusicItem> music = mMusicArtistItem.get(
								position).getMusicsList();
						try {
							mMusicPlaySer.setMusicShowList(music);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
						intent.setClass(getApplicationContext(),
								MusicListActivity.class);
						startActivity(intent);
					}
				});
				mViewPagerAdapter.notifyDataSetChanged();
			} else {
				Toast.makeText(getApplicationContext(), R.string.music_data_not_init, Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.back_arrow:
			mIsMusicList = false;
			mMusicListView.setVisibility(View.GONE);
			mMainView.setVisibility(View.VISIBLE);
			mViewPagerAdapter.notifyDataSetChanged();
		}
	}
	
	public Bitmap getMusicImg(String mAlbumArtURI) {
        File file = new File(mAlbumArtURI);
        Bitmap bt = null;
        try {
			ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
			bt = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, null);
			if (bt == null) {
			} else {
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bt;
	}

	private ServiceConnection mMusicSerConn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mMusicPlaySer = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mMusicPlaySer = IMusicPlayService.Stub.asInterface(service);
		}
	};
}

class VideoGridView extends GridView {
	public VideoGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
		super.onMeasure(widthMeasureSpec, expandSpec);
	}
	
}