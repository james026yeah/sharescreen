/******************************************************************
*
*	MediaServer for CyberLink
*
*	Copyright (C) Satoshi Konno 2003-2004
*
*	File : GIFPlugIn.java
*
*	Revision:
*
*	01/25/04
*		- first revision.
*
******************************************************************/

package org.cybergarage.upnp.std.av.server.object.format;

import java.io.*;

import org.cybergarage.upnp.std.av.server.object.*;

public class BMPFormat extends ImageIOFormat
{
	private static final String MIMETYPE = "image/bmp";
	private static final String HEADERID = "BMP";
	////////////////////////////////////////////////
	// Constroctor
	////////////////////////////////////////////////
	
	public BMPFormat()
	{
	}
	
	public BMPFormat(File file)
	{
		super(file);
	}

	////////////////////////////////////////////////
	// Abstract Methods
	////////////////////////////////////////////////
	
	public boolean equals(File file)
	{
		String headerID = Header.getIDString(file, 3);
		if (headerID.startsWith("BMP") == true)
			return true;		
		return false;
	}
	
	public FormatObject createObject(File file)
	{
		return new BMPFormat(file);
	}
	
	public String getMimeType()
	{
		return MIMETYPE;
	}

}

