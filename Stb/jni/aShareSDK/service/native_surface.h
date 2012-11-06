/*
 * =====================================================================================
 *
 *       Filename:  native_surface.h
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  05/22/2012 02:10:45 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  cyher (), cyher.net A~T gmail.com
 *        Company:  cyher.net
 *
 * =====================================================================================
 */
#ifndef __NATIVE_SURFACE_H__
#define __NATIVE_SURFACE_H__
#include <jni.h>
#include "surface.h"

void init_native_surface(void);
void deinit_native_surface(void);
int surface_start(JNIEnv *env, jobject jsurface);
int surface_stop();


#endif
