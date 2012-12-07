#pragma once

/*
 *      Copyright (C) 2011-2012 Team XBMC
 *      http://www.xbmc.org
 *
 *  This Program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2, or (at your option)
 *  any later version.
 *
 *  This Program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with XBMC; see the file COPYING.  If not, see
 *  <http://www.gnu.org/licenses/>.
 *
 */


#include <shairport.h>

//#define DLL_PATH_LIBSHAIRPORT  "/data/data/com.archermind.airtunes/lib/libshairport.so"

class DllLibShairportInterface
{
	public:
		virtual ~DllLibShairportInterface() {}

		virtual int   shairport_main(int argc, char **argv    )=0;
		virtual void  shairport_exit(void                     )=0;
		virtual int   shairport_loop(void                     )=0;
		virtual int   shairport_is_running(void               )=0;
		virtual void  shairport_set_ao(struct AudioOutput *ao        )=0;
//xx		virtual void  shairport_set_printf(struct printfPtr *funcPtr)=0;
};

class DllLibShairport : public DllLibShairportInterface
{
public:
	DllLibShairport();

	bool Load();
	void Unload();
	bool IsLoaded(){return handle != NULL;}
	bool CanLoad(){return true;}
	bool EnableDelayedUnload(bool delay);

	virtual int  shairport_main (int p1, char **p2);
	virtual void  shairport_exit ();
	virtual int  shairport_loop ();
	virtual int  shairport_is_running ();
	virtual void  shairport_set_ao (struct AudioOutput *p1);
	/*virtual void  shairport_set_printf (struct printfPtr *p1)
	{ 
		return _shairport_set_printf (p1);
	}*xx*/

	virtual bool ResolveExports();

private:
	void *handle;
	bool m_DelayUnload;

	typedef int   (* __shairport_main)(int argc, char **argv);
	typedef void  (* __shairport_exit)(void);
	typedef int   (* __shairport_loop)(void);
	typedef int   (* __shairport_is_running)(void);
	typedef void  (* __shairport_set_ao)(struct AudioOutput *ao);
//	typedef void  (* __shairport_set_printf)(struct printfPtr *funcPtr);

	__shairport_main _shairport_main;
	__shairport_exit _shairport_exit;
	__shairport_loop _shairport_loop;
	__shairport_is_running _shairport_is_running;
	__shairport_set_ao _shairport_set_ao;
};
