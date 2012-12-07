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
import org.cybergarage.upnp.std.av.server.UPnP;
import org.cybergarage.util.Debug;
import org.cybergarage.xml.Attribute;
import org.cybergarage.xml.AttributeList;

import android.util.Log;


public class MusicItemDirectory extends Directory {
	
	public static final String TAG = "MusicItemDirectory";
	private ArrayList<MusicItem> mFileList;
	
	public MusicItemDirectory(String name, ArrayList<MusicItem> fileList) {
		super(name);
		mFileList = fileList;
	}

	@SuppressWarnings("unchecked")
	private boolean updateItemNode(FileItemNode itemNode, File file,MusicItem info)
	{
		Format format = getContentDirectory().getFormat(info.mime_type);
		if (format == null) {
			Log.d(TAG, "dms file format ==null filepath=" + info.filePath);
			return false;
		}
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
		itemNode.setMimeType(format.getMimeType());
		itemNode.addProperty(UPnP.ALBUM, info.album);
		itemNode.addProperty(UPnP.ARITIST, info.artist);
		itemNode.addProperty(UPnP.FILEPATH, info.filePath);
		itemNode.setAlbumArtURI(getContentDirectory().getContentExportArmArtURL(info.albumArtURI));
		
		// ProtocolInfo
		String mimeType = format.getMimeType();
		String protocol = ConnectionManager.HTTP_GET + ":*:" + mimeType + ":*";
		String id = itemNode.getID();
		String url = getContentDirectory().getContentExportURL(id);
		info.itemUri = url;
		AttributeList objAttrList = formatObj.getAttributeList();
		objAttrList.add(new Attribute(UPnP.DURATION, info.duration));
		itemNode.setResource(url, protocol, objAttrList);
		
		DIDLLite didlLite = new DIDLLite();
		didlLite.setContentNode(itemNode);
		info.metaData = didlLite.toString();
		// Update SystemUpdateID
		getContentDirectory().updateSystemUpdateID();
		
		return true;
	}
	
	private FileItemNode createCompareItemNode(File file)
	{
		/*Format format = getContentDirectory().getFormat(file);
		if (format == null) {
			if (file != null)
				Log.d(TAG,"format == null ,filename=" + file.getAbsolutePath());
			return null;
		}*/
		FileItemNode itemNode = new FileItemNode();
		itemNode.setFile(file);
		return itemNode;
	}
	
	////////////////////////////////////////////////
	// FileList
	////////////////////////////////////////////////
	/*
	private int getDirectoryItemNodeList(File dirFile, FileItemNodeList itemNodeList)
	{
		File childFile[] = dirFile.listFiles();
		int fileCnt = childFile.length;
		for (int n=0; n<fileCnt; n++) {
			File file = childFile[n];
			if (file.isDirectory()) {
				getDirectoryItemNodeList(file, itemNodeList);
				continue;
			}
			if (file.isFile() == true) {
				FileItemNode itemNode = createCompareItemNode(file);
				if (itemNode == null)
					continue;						
				itemNodeList.add(itemNode);
			}
		}
		return itemNodeList.size();
	}

	private FileItemNodeList getCurrentDirectoryItemNodeList()
	{
		FileItemNodeList itemNodeList = new FileItemNodeList();
		String path = getPath();
		File pathFile = new File(path);
		getDirectoryItemNodeList(pathFile, itemNodeList);
		return itemNodeList;
	}
	*/

	////////////////////////////////////////////////
	// updateItemNodeList
	////////////////////////////////////////////////
	
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
	
	private boolean updateItemNodeList(FileItemNode newItemNode, MusicItem info)
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
			if (updateItemNodeList(itemNode,mFileList.get(n)) == true)
				updateFlag = true;
		}
		
		return updateFlag;
	}
	
	@SuppressWarnings("unchecked")
	private FileItemNodeList createItemNodeList() {
		if (mFileList == null) {
			return null;
		}
		FileItemNodeList nodeList = new FileItemNodeList();
		
		for (MusicItem info : mFileList) {
			File file=new File(info.filePath);
			if (!file.exists()) {
				Log.d(TAG,"dms file not exist path=" + info.filePath);
				continue;
			}
			FileItemNode itemNode = createCompareItemNode(file);
			if (itemNode == null) {
				Log.d(TAG,"dms create music Item node is null...." + info.filePath);
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
