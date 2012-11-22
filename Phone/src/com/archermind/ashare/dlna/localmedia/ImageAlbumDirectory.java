package com.archermind.ashare.dlna.localmedia;

import java.io.File;
import java.util.ArrayList;

import org.cybergarage.upnp.std.av.server.ConnectionManager;
import org.cybergarage.upnp.std.av.server.Directory;
import org.cybergarage.upnp.std.av.server.object.ContentNode;
import org.cybergarage.upnp.std.av.server.object.DIDLLite;
import org.cybergarage.upnp.std.av.server.object.Format;
import org.cybergarage.upnp.std.av.server.object.FormatObject;
import org.cybergarage.upnp.std.av.server.object.item.file.FileItemNode;
import org.cybergarage.upnp.std.av.server.object.item.file.FileItemNodeList;
import org.cybergarage.util.Debug;
import org.cybergarage.xml.AttributeList;
import org.cybergarage.upnp.std.av.server.UPnP;


public class ImageAlbumDirectory extends Directory {
	private ArrayList<PhotoItem> mImages;
	
	public ImageAlbumDirectory(String name, ArrayList<PhotoItem> fileList) {
		super(name);
		mImages = fileList;
	}

	private boolean updateItemNode(FileItemNode itemNode, File file,PhotoItem info)
	{
		Format format = getContentDirectory().getFormat(file);
		if (format == null)
			return false;
		FormatObject formatObj = format.createObject(file);
		
		// File/TimeStamp
		itemNode.setFile(file);
		
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
		itemNode.addProperty(UPnP.FILEPATH, info.filePath);
		itemNode.setAlbumArtURI(info.thumbFilePath);
		
		// ProtocolInfo
		String mimeType = format.getMimeType();
		String protocol = ConnectionManager.HTTP_GET + ":*:" + mimeType + ":*";
		String id = itemNode.getID();
		String url = getContentDirectory().getContentExportURL(id);
		
		AttributeList objAttrList = formatObj.getAttributeList();
		itemNode.setResource(url, protocol, objAttrList);
		info.itemUri = url;
		
		DIDLLite didlLite = new DIDLLite();
		didlLite.setContentNode(itemNode);
		info.metaData = didlLite.toString();
		// Update SystemUpdateID
		getContentDirectory().updateSystemUpdateID();
		
		return true;
	}
	
	private FileItemNode createCompareItemNode(File file)
	{
		Format format = getContentDirectory().getFormat(file);
		if (format == null) {
			if (file != null)
				System.out.println("format == null ,filename=" + file.getAbsolutePath());
			return null;
		}
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
	
	private boolean updateItemNodeList(FileItemNode newItemNode,PhotoItem info)
	{
		if (newItemNode == null) {
			return false;
		}
		File newItemNodeFile = newItemNode.getFile();
		FileItemNode currItemNode = getItemNode(newItemNodeFile);
		if (currItemNode == null) {
			int newItemID = getContentDirectory().getNextItemID();
			info.itemId = newItemID;
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
			if (updateItemNodeList(itemNode,mImages.get(n)) == true)
				updateFlag = true;
		}
		
		return updateFlag;
	}
	
	@SuppressWarnings("unchecked")
	private FileItemNodeList createItemNodeList() {
		if (mImages == null) {
			return null;
		}
		FileItemNodeList nodeList = new FileItemNodeList();
		for (PhotoItem info : mImages) {
			File file = new File(info.filePath);
			//when we detect file not exists,remove it
			if (!file.exists()) {
				System.out.println("ashare file not exist path=" + info.filePath);
				continue;
			}
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
