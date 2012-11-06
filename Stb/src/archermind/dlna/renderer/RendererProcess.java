package archermind.dlna.renderer;

import java.util.UUID;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.control.ActionListener;
import org.cybergarage.upnp.device.InvalidDescriptionException;
import org.cybergarage.upnp.std.av.renderer.AVTransport;
import org.cybergarage.upnp.std.av.renderer.ConnectionManager;
import org.cybergarage.upnp.std.av.renderer.MediaRenderer;
import org.cybergarage.upnp.std.av.renderer.RenderingControl;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import archermind.dlna.household.DLNAPlayer;

import com.archermind.ashare.TypeDefs;

public class RendererProcess extends HandlerThread implements ActionListener {
	private final static String TAG = "RendererProcess";
	private final static String RENDERER_DESCRIPTION = 
			"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
			"<root xmlns=\"urn:schemas-upnp-org:device-1-0\">\n" +
			"   <specVersion>\n" +
			"      <major>1</major>\n" +
			"      <minor>0</minor>\n" +
			"   </specVersion>\n" +
			"   <device>\n" +
			"      <deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>\n" +
			"      <friendlyName>TV Dongle</friendlyName>\n" +
			"      <manufacturer>Archermind Inc</manufacturer>\n" +
			"      <manufacturerURL>http://www.cybergarage.org</manufacturerURL>\n" +
			"      <modelDescription>Provides content through UPnP ContentDirectory service</modelDescription>\n" +
			"      <modelName>aShare Media Renderer</modelName>\n" +
			"      <modelNumber>1.0</modelNumber>\n" +
			"      <modelURL>http://www.cybergarage.org</modelURL>\n" +
			"      <UDN>uuid:362d9414-31a0-48b6-b684-2b4bd38391d0</UDN>\n" +
			"      <serviceList>\n" +
			"         <service>\n" +
			"            <serviceType>urn:schemas-upnp-org:service:RenderingControl:1</serviceType>\n" +
			"            <serviceId>RenderingControl</serviceId>\n" +
			"            <SCPDURL>/service/RenderingControl.xml</SCPDURL>\n" +
			"            <controlURL>/service/RenderingControl_control</controlURL>\n" +
			"            <eventSubURL>/service/RenderingControl_event</eventSubURL>\n" +
			"         </service>\n" +
			"         <service>\n" +
			"            <serviceType>urn:schemas-upnp-org:service:ConnectionManager:1</serviceType>\n" +
			"            <serviceId>ConnectionManager</serviceId>\n" +
			"            <SCPDURL>/service/ConnectionManager.xml</SCPDURL>\n" +
			"            <controlURL>/service/ConnectionManager_control</controlURL>\n" +
			"            <eventSubURL>/service/ConnectionManager_event</eventSubURL>\n" +
			"         </service>\n" +
			"         <service>\n" +
			"            <serviceType>urn:schemas-upnp-org:service:AVTransport:1</serviceType>\n" +
			"            <serviceId>AVTransport</serviceId>\n" +
			"            <SCPDURL>/service/AVTransport.xml</SCPDURL>\n" +
			"            <controlURL>/service/AVTransport_control</controlURL>\n" +
			"            <eventSubURL>/service/AVTransport_event</eventSubURL>\n" +
			"         </service>\n" +
			"      </serviceList>\n" +
			"   </device>\n" +
			"</root>";

	private final static int MSG_START_RENDERER = 0;
	private final static int MSG_STOP_RENDERER = 1;
	private final static int MSG_STOP_PROCESS = 2;
	private MediaRenderer mRenderer;
	private Handler mHandler;
	private Handler mUIHandler;
	private Handler.Callback mCb = new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			boolean ret = false;
			switch(msg.what) {
			case MSG_START_RENDERER:
				String friendlyName = (String)msg.obj;
				Log.d(TAG, "start renderer------------>with name:" + friendlyName);
				if(friendlyName != null)
					mRenderer.setFriendlyName(friendlyName);
				UUID uuid = UUID.randomUUID();
				Log.d(TAG, "start renderer------------>with random uuid:" + uuid.toString());
				mRenderer.setUDN("uuid:" + uuid.toString());
				mRenderer.start();
				ret = true;
				break;
			case MSG_STOP_RENDERER:
				Log.d(TAG, "stop renderer!");
				mRenderer.stop();
				ret = true;
				break;
			case MSG_STOP_PROCESS:
				RendererProcess.this.quit();
				ret = true;
				break;
			}
			return ret;
		}		
	};
	
	public RendererProcess(Handler uiHandler) {
		super(TAG);
		mUIHandler = uiHandler;
		try {
			mRenderer = new MediaRenderer(
					RENDERER_DESCRIPTION, RenderingControl.SCPD,
					ConnectionManager.SCPD, AVTransport.SCPD);
		} catch (InvalidDescriptionException e) {
			e.printStackTrace();
		}
		mRenderer.setActionListener(this);
	}
	
    @Override
    protected void onLooperPrepared() {
    	Log.d(TAG, "onLooperPrepared..............");
    	mHandler = new Handler(getLooper(), mCb);
    	if(null != mUIHandler)
    		mUIHandler.sendEmptyMessage(TypeDefs.MSG_DMR_ON_PROC_PREPARED);
    }
    
    public void startRenderer(String friendlyName) {
    	if(null != mHandler) {
    		mHandler.sendMessage(Message.obtain(null, MSG_START_RENDERER, friendlyName));
    	}
    }
    
    public void stopRenderer() {
    	/*
    	if(null != mHandler) {
    		mHandler.sendEmptyMessage(MSG_STOP_RENDERER);
    	}*/
    	Log.v(TAG, "synchronized --> stop renderer!");
		mRenderer.stop();
    }    
    
    public void stopProcess() {    	
    	stopRenderer();  
    	if(null != mHandler) {
    		mHandler.sendEmptyMessage(MSG_STOP_PROCESS);
    	}   
    }
    
    public int getMediatype(String mediatype) { 
    	int ret = TypeDefs.MEDIA_TYPE_INVALID;
    	if(mediatype.equals("videos"))
    	{
    		ret = TypeDefs.MEDIA_TYPE_DLNA_VIDEO;
    	}
    	else if(mediatype.equals("Audio"))
    	{
    		ret = TypeDefs.MEDIA_TYPE_DLNA_AUDIO;
    	}
    	else if(mediatype.equals("Image"))
    	{
    		ret = TypeDefs.MEDIA_TYPE_DLNA_IMAGE;
    	}
    	return ret;
    }

	@Override
	public boolean actionControlReceived(Action action) {	
		Log.d(TAG, "actionName is :" + action.getName());
		String actionName = action.getName();
		if(actionName.equals(AVTransport.SETAVTRANSPORTURI)) {
			Argument arg = action.getArgument(AVTransport.CURRENTURI);
			Argument mediatype = action.getArgument(AVTransport.CURRENTURIMETADATA);
			/*XmlPullParser parser = new XmlPullParser();
			try {
				String ss = mediatype.getValue();
				Node node = parser.parse(ss);
				//ContentNode contentNode = (ContentNode)node;
				ItemNode itemNode = new ItemNode();
				DIDLLiteNode dn = (DIDLLiteNode)node;
				dn.addNode(node);
				Log.d("ss","xxx");
			} catch (ParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			Message msg = new Message();
			msg.what = TypeDefs.MSG_DMR_AV_TRANS_SET_URI;
			msg.obj = arg.getValue();
			msg.arg1 = getMediatype(mediatype.getValue());
			Log.d(TAG, "CURRENTURI:" + arg.getValue());
			if(null != mUIHandler)
			{
	    		mUIHandler.sendMessage(msg);
	    		return true;
			}
		} else if(actionName.equals(AVTransport.SETNEXTAVTRANSPORTURI)) {
			Argument arg = action.getArgument(AVTransport.NEXTAVTRANSPORTURI);
			Message msg = new Message();
			msg.what = TypeDefs.MSG_DMR_AV_TRANS_SET_NEXT_URI;
			msg.obj = arg.getValue();
			Log.d(TAG, "Next URI:" + arg.getValue());
			if(null != mUIHandler)
			{
	    		mUIHandler.sendMessage(msg);
	    		return true;
			}
		} else if(actionName.equals(AVTransport.STOP)) {
			if(null != mUIHandler)
			{
	    		mUIHandler.sendEmptyMessage(TypeDefs.MSG_DMR_AV_TRANS_STOP);
	    		return true;
			}
		} else if(actionName.equals(AVTransport.PLAY)) {
			if(DLNAPlayer.mIsPlayCompletion)
			{
				if(null != mUIHandler)
				{
	    		    mUIHandler.sendEmptyMessage(TypeDefs.MSG_DMR_AV_TRANS_PLAY);
				    return true;
				}
			}
			else
			{
				if(null != mUIHandler)
				{
	    		    mUIHandler.sendEmptyMessage(TypeDefs.MSG_DMR_AV_TRANS_PAUSE_TO_PLAY);
				    return true;
				}
			}		
		} else if(actionName.equals(AVTransport.PAUSE)) {
			if(DLNAPlayer.mIsPlayCompletion)
	    		return false;
			if(null != mUIHandler)
			{
	    		mUIHandler.sendEmptyMessage(TypeDefs.MSG_DMR_AV_TRANS_PLAY_TO_PAUSE);
			    return true;
			}
		} else if(actionName.equals(AVTransport.SEEK)) {
			if(DLNAPlayer.mIsPlayCompletion)
	    		return false;
			Argument unit = action.getArgument(AVTransport.UNIT);
			Argument target = action.getArgument(AVTransport.TARGET);
			Message msg = new Message();
			msg.what = TypeDefs.MSG_DMR_AV_TRANS_SEEK;
			msg.arg1 = (int)DLNAPlayer.fromDateStringToInt(target.getValue());
			Log.d(TAG, "Seek uint:" + unit.getValue() + ", target:" + target.getValue());
			if(null != mUIHandler)
			{
	    		mUIHandler.sendMessage(msg);
	    		return true;
			}
		} else if(actionName.equals(AVTransport.GETPOSITIONINFO)) {			
			if(null != mUIHandler)
			{
	    		mUIHandler.sendEmptyMessage(TypeDefs.MSG_DMR_RENDERER_UPDATEDATA);
	    		action.setArgumentValue(AVTransport.RELTIME, DLNAPlayer.fromIntToDateString(DLNAPlayer.mCurrentPosition));
	    		action.setArgumentValue(AVTransport.TRACKDURATION, DLNAPlayer.fromIntToDateString(DLNAPlayer.mTotalTime));
	    		return true;
			}
		}
		else if(actionName.equals(AVTransport.NEXT)) {			
			if(null != mUIHandler)
			{
	    		mUIHandler.sendEmptyMessage(TypeDefs.MSG_DMR_AV_TRANS_PLAY);
	    		return true;
			}
		} else if(actionName.equals(AVTransport.PREVIOUS)) {			
			if(null != mUIHandler)
			{
	    		mUIHandler.sendEmptyMessage(TypeDefs.MSG_DMR_AV_TRANS_PLAY);
	    		return true;
			}
		}
		else if(actionName.equals(RenderingControl.SETVOLUME)) {
			if(DLNAPlayer.mIsPlayCompletion)
	    		return false;
			Argument volume = action.getArgument(RenderingControl.DESIREDVOLUME);
			Message msg = new Message();
			msg.what = TypeDefs.MSG_DMR_AV_TRANS_SET_VOLUME;
			Log.d(TAG, "volume:" + volume.getValue());
		    msg.arg1 = Integer.parseInt(volume.getValue());
			if(null != mUIHandler)
			{
	    		mUIHandler.sendMessage(msg);
	    		return true;
			}
		}
		else if(actionName.equals(RenderingControl.SETMUTE)) {
			if(DLNAPlayer.mIsPlayCompletion)
	    		return false;
			Argument mute = action.getArgument(RenderingControl.DESIREDMUTE);
			Message msg = new Message();
			msg.what = TypeDefs.MSG_DMR_AV_TRANS_SET_MUTE;
			Log.d(TAG, "volume:" + mute.getValue());
		    msg.arg1 = Integer.parseInt(mute.getValue());
			if(null != mUIHandler)
			{
	    		mUIHandler.sendMessage(msg);
	    		return true;
			}
		}
		else if(actionName.equals(RenderingControl.GETVOLUME)) {			
			if(null != mUIHandler)
			{
	    		mUIHandler.sendEmptyMessage(TypeDefs.MSG_DMR_RENDERER_UPDATEDATA);
	    		action.setArgumentValue(RenderingControl.DESIREDMUTE, String.valueOf(DLNAPlayer.mVolume));
	    		return true;
			}
		}
		else if(actionName.equals(RenderingControl.GETMUTE)) {			
			if(null != mUIHandler)
			{
	    		mUIHandler.sendEmptyMessage(TypeDefs.MSG_DMR_RENDERER_UPDATEDATA);
	    		action.setArgumentValue(RenderingControl.GETMUTE, String.valueOf(DLNAPlayer.mVolume));
	    		return true;
			}
		}
		return false;
	}
}
