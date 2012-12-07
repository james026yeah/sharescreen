#include <dlfcn.h>
#include <log.h>
#include "DllLibShairport.h"

#include <android/log.h> 
#undef LOG_TAG 
#define LOG_TAG "AIRTUNES_DLL"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG ,  LOG_TAG , __VA_ARGS__)    

DllLibShairport::DllLibShairport():handle(NULL),
				   m_DelayUnload(false),
				   _shairport_main(NULL),
				   _shairport_exit(NULL),
				   _shairport_loop(NULL),
				   _shairport_is_running(NULL),
				   _shairport_set_ao(NULL)
{
}

bool DllLibShairport::Load()
{
    LOGD("Enter %s:%d",__func__,__LINE__);
/*
	handle = dlopen(DLL_PATH_LIBSHAIRPORT, RTLD_LOCAL | RTLD_LAZY);
	if (handle == NULL)
	{
        LOGD("open libshairport error.\n");
		return false;
	}

	_shairport_main = (__shairport_main)dlsym(handle, "shairport_main");
	_shairport_exit = (__shairport_exit)dlsym(handle, "shairport_exit");
	_shairport_loop = (__shairport_loop)dlsym(handle, "shairport_loop");
	_shairport_is_running = (__shairport_is_running)dlsym(handle, "shairport_is_running");
	_shairport_set_ao = (__shairport_set_ao)dlsym(handle, "shairport_set_ao");
        if (_shairport_main && _shairport_exit && _shairport_loop && _shairport_is_running && _shairport_set_ao)
	{
            LOGD("return function sucess");
            return true;
        }
	    else
        {
            LOGD("return function error");
	        return false;
	}
*/
return true;
}

void DllLibShairport::Unload()
{
    LOGD("Enter %s:%d",__func__,__LINE__);
	//dlclose(handle);
}

bool DllLibShairport::EnableDelayedUnload(bool delay)
{
    LOGD("Enter %s:%d",__func__,__LINE__);
	m_DelayUnload = delay;

	return true;
}

int DllLibShairport::shairport_main (int p1, char **p2)
{   
    LOGD("Enter %s:%d",__func__,__LINE__);

   // return _shairport_main (p1, p2);
}

void DllLibShairport::shairport_exit ()
{   
    LOGD("Enter %s:%d",__func__,__LINE__);

    //return _shairport_exit (); 
}   

int DllLibShairport::shairport_loop ()
{   
    LOGD("Enter %s:%d",__func__,__LINE__);

	//return _shairport_loop (); 
}   

int DllLibShairport::shairport_is_running ()
{   
    LOGD("Enter %s:%d",__func__,__LINE__);
	//return _shairport_is_running (); 
}   

void DllLibShairport::shairport_set_ao (struct AudioOutput *p1)
{   
    LOGD("Enter %s:%d",__func__,__LINE__);
	//return _shairport_set_ao (p1);
}   

/*virtual void DllLibShairport::shairport_set_printf (struct printfPtr *p1)
  { 
  return _shairport_set_printf (p1);
  }*xx*/


bool DllLibShairport::ResolveExports()
{   
    LOGD("Enter %s:%d",__func__,__LINE__);
    return true;
}
