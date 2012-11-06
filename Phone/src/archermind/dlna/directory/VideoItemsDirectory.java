package archermind.dlna.directory;

import java.io.File;
import java.util.ArrayList;

import org.cybergarage.upnp.std.av.server.ConnectionManager;
import org.cybergarage.upnp.std.av.server.Directory;
import org.cybergarage.upnp.std.av.server.object.ContentNode;
import org.cybergarage.upnp.std.av.server.object.Format;
import org.cybergarage.upnp.std.av.server.object.FormatObject;
import org.cybergarage.upnp.std.av.server.object.item.file.FileItemNode;
import org.cybergarage.upnp.std.av.server.object.item.file.FileItemNodeList;
import org.cybergarage.util.Debug;
import org.cybergarage.xml.Attribute;
import org.cybergarage.xml.AttributeList;

import archermind.dlna.directory.Util.UPnp;

public class VideoItemsDirectory extends Directory {
	
	private ArrayList<VideoInfo> mFileList;

	
	public VideoItemsDirectory(String name, ArrayList<VideoInfo> fileList) {
		super(name);
		mFileList = fileList;
	}

	private boolean updateItemNode(FileItemNode itemNode, File file,VideoInfo info)
	{
		Format format = getContentDirectory().getFormat(file);
		if (format == null)
			return false;
		FormatObject formatObj = format.createObject(file);
		
		// File/TimeStamp
		itemNode.setFile(file);
		
		// Title
		String title = info.title;
		if (title!=null)
			itemNode.setTitle(title);
			
		// Creator
		String creator = formatObj.getCreator();
		if (0 < creator.length())
			itemNode.setCreator(creator);

		// Media Class
		String mediaClass = format.getMediaClass();
		if (0 < mediaClass.length())
			itemNode.setUPnPClass(mediaClass);

		// Date
		long lastModTime = file.lastModified();
		itemNode.setDate(lastModTime);
		
		// Storatge Used
		try {
			long fileSize = file.length();
			itemNode.setStorageUsed(fileSize);	
		}
		catch (Exception e) {
			Debug.warning(e);
		}
		itemNode.setAlbumArtURI(info.thumbPath);
		
		// ProtocolInfo
		String mimeType = format.getMimeType();
		String protocol = ConnectionManager.HTTP_GET + ":*:" + mimeType + ":*";
		String id = itemNode.getID();
		String url = getContentDirectory().getContentExportURL(id);
		AttributeList objAttrList = formatObj.getAttributeList();
		itemNode.addProperty(UPnp.FILEPATH, info.filePath);
		objAttrList.add(new Attribute(UPnp.DURATION,info.duration));
		itemNode.setResource(url, protocol, objAttrList);
		
		// Update SystemUpdateID
		getContentDirectory().updateSystemUpdateID();
		
		return true;
	}
	
	private FileItemNode createCompareItemNode(File file)
	{
		Format format = getContentDirectory().getFormat(file);
		if (format == null)
			return null;
		FileItemNode itemNode = new FileItemNode();
		itemNode.setFile(file);
		return itemNode;
	}
	
	private FileItemNode getItemNode(File file)
	{
		int nContents = getNContentNodes();
		for (int n=0; n<nContents; n++) {
			ContentNode cnode = getContentNode(n);
			if ((cnode instanceof FileItemNode) == false)
				continue;
			FileItemNode itemNode = (FileItemNode)cnode;
			if (itemNode.equals(file) == true)
				return itemNode;
		}
		return null;
	}
	
	private void addItemNode(FileItemNode itemNode)
	{
		addContentNode(itemNode);
	}
	
	private boolean updateItemNodeList(FileItemNode newItemNode,VideoInfo info)
	{
		if (newItemNode == null) {
			return false;
		}
		File newItemNodeFile = newItemNode.getFile();
		FileItemNode currItemNode = getItemNode(newItemNodeFile);
		if (currItemNode == null) {
			int newItemID = getContentDirectory().getNextItemID();
			newItemNode.setID(newItemID);
			updateItemNode(newItemNode, newItemNodeFile,info);
			addItemNode(newItemNode);
			return true;
		}
		
		long currTimeStamp = currItemNode.getFileTimeStamp();
		long newTimeStamp = newItemNode.getFileTimeStamp();
		if (currTimeStamp == newTimeStamp)
			return false;
			
		updateItemNode(currItemNode, newItemNodeFile,info);
		
		return true;
	}
	
	private boolean updateItemNodeList()
	{
		boolean updateFlag = false;
		
		// Checking Deleted Items
		int nContents = getNContentNodes();
		ContentNode cnode[] = new ContentNode[nContents];
		for (int n=0; n<nContents; n++)
			cnode[n] = getContentNode(n);
		for (int n=0; n<nContents; n++) {
			if ((cnode[n] instanceof FileItemNode) == false)
				continue;
			FileItemNode itemNode = (FileItemNode)cnode[n];
			File itemFile = itemNode.getFile();
			if (itemFile == null)
				continue;
			if (itemFile.exists() == false) {
				removeContentNode(cnode[n]);
				updateFlag = true;
			}
		}
		
		// Checking Added or Updated Items
		FileItemNodeList itemNodeList = createItemNodeList();
		
		int itemNodeCnt = itemNodeList.size();
		for (int n=0; n<itemNodeCnt; n++) {
			FileItemNode itemNode = itemNodeList.getFileItemNode(n);
			if (updateItemNodeList(itemNode,mFileList.get(n)) == true)
				updateFlag = true;
		}
		
		return updateFlag;
	}
	
	@SuppressWarnings("unchecked")
	private FileItemNodeList createItemNodeList() {
		/*
		File file = new File("/sdcard/ScreenRecorderThumbnail/1349924656.png");
		FileItemNodeList nodeList = new FileItemNodeList();
		FileItemNode itemNode = createCompareItemNode(file);
		nodeList.add(itemNode);
		*/
		if (mFileList == null) {
			return null;
		}
		FileItemNodeList nodeList = new FileItemNodeList();
		for (VideoInfo info : mFileList) {
			File file=new File(info.filePath);
			FileItemNode itemNode = createCompareItemNode(file);
			if (itemNode == null) {
				continue;
			}
			nodeList.add(itemNode);
		}
		return nodeList;
	}
	
	@Override
	public boolean update() {
		// TODO Auto-generated method stub
		return updateItemNodeList();
	}


}
