/******************************************************************
*
*	MediaServer for CyberLink
*
*	Copyright (C) Satoshi Konno 2003-2004
*
*	File : MPEGPlugIn.java
*
*	Revision:
*
*	02/02/04
*		- first revision.
*
******************************************************************/

package org.cybergarage.upnp.std.av.server.object.format;

import java.io.*;

import org.cybergarage.xml.*;
import org.cybergarage.util.*;
import org.cybergarage.upnp.std.av.server.object.*;
import org.cybergarage.upnp.std.av.server.object.item.*;

public class VideoMPEGFormat extends VideoIOFormat
{
	////////////////////////////////////////////////
	// Constroctor
	////////////////////////////////////////////////
	
	public VideoMPEGFormat()
	{
	}
	
	public VideoMPEGFormat(File file)
	{
		super(file);
	}

	////////////////////////////////////////////////
	// Abstract Methods
	////////////////////////////////////////////////
	
	public boolean equals(File file)
	{
		String ext = Header.getSuffix(file);
		if (ext == null)
			return false;
		if (ext.startsWith("mpeg"))
			return true;
		return false;
	}
	
	public FormatObject createObject(File file)
	{
		return new VideoMPEGFormat(file);
	}
	
	public String getMimeType()
	{
		return "video/mpeg";
	}
}

