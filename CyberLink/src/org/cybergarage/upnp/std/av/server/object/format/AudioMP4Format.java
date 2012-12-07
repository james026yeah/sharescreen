/******************************************************************
*
*	MediaServer for CyberLink
*
*	Copyright (C) Satoshi Konno 2003
*
*	File : ID3.java
*
*	Revision:
*
*	12/03/03
*		- first revision.
*
******************************************************************/

package org.cybergarage.upnp.std.av.server.object.format;

import java.io.*;

import org.cybergarage.upnp.std.av.server.object.*;
import org.cybergarage.upnp.std.av.server.object.item.*;
import org.cybergarage.xml.*;
import org.cybergarage.util.*;

public class AudioMP4Format extends AudioIOFormat
{
	
	private static final String mimeType = "audio/mp4";
	public AudioMP4Format()
	{
	}
	
	public AudioMP4Format(File file)
	{
		super(file);
	}

	////////////////////////////////////////////////
	// Abstract Methods
	////////////////////////////////////////////////
	
	public boolean equals(File file)
	{
		String headerID = Header.getIDString(file, 3);
		if (headerID.startsWith(HEADER_ID) == true)
			return true;		
		return false;
	}
	
	public FormatObject createObject(File file)
	{
		return new AudioMP4Format(file);
	}
	
	public String getMimeType()
	{
		return mimeType;
	}
}

