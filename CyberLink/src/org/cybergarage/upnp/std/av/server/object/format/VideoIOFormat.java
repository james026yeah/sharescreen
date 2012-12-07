/******************************************************************
*
*	MediaServer for CyberLink
*
*	Copyright (C) Satoshi Konno 2003-2004
*
*	File : ImageIOPlugIn.java
*
*	Revision:
*
*	01/25/04
*		- first revision.
*
******************************************************************/

package org.cybergarage.upnp.std.av.server.object.format;

import java.io.*;
/*
import javax.imageio.*;
import javax.imageio.stream.*;
*/

import org.cybergarage.xml.*;
import org.cybergarage.upnp.std.av.server.object.*;
import org.cybergarage.upnp.std.av.server.object.item.ItemNode;
import org.cybergarage.util.Debug;

public abstract class VideoIOFormat extends Header implements Format, FormatObject
{
	////////////////////////////////////////////////
	// Member
	////////////////////////////////////////////////

	private File videoFile;

	////////////////////////////////////////////////
	// Constroctor
	////////////////////////////////////////////////
	
	public VideoIOFormat()
	{	
		videoFile = null;
	}
	
	public VideoIOFormat(File file)
	{
		videoFile = file;
	}

	////////////////////////////////////////////////
	// Abstract Methods
	////////////////////////////////////////////////
	
	public abstract boolean equals(File file);
	public abstract FormatObject createObject(File file);
	public abstract String getMimeType();
	
	public String getMediaClass()
	{
		return "object.item.videoItem.movie";
	}
	
	public AttributeList getAttributeList()
	{
		AttributeList attrList = new AttributeList();
		
		try {
			// Size 
			long fsize = videoFile.length();
			Attribute sizeStr = new Attribute(ItemNode.SIZE, Long.toString(fsize));
			attrList.add(sizeStr);
		}
		catch (Exception e) {
			Debug.warning(e);
		}
		
		return attrList;	
	}
	
	public String getTitle()
	{
		String fname = videoFile.getName();
		int idx = fname.lastIndexOf(".");
		if (idx < 0)
			return "";
		String title = fname.substring(0, idx);
		return title;
	}
	
	public String getCreator()
	{
		return "";
	}
}

