/*
 * Copyright (C) 2012 Havlena Petr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef NDK
#include <utils/Log.h>
#include <surfaceflinger/Surface.h>
#include <SkCanvas.h>
#include <SkBitmap.h>
#include <SkImageDecoder.h>
#include <SkStream.h>
#include <SkMatrix.h>
#include <SkRect.h>
#else
#include <android/log.h>
#include "common.h"
#include "comm.h"
#include "SkCanvas.h"
#include "SkBitmap.h"
#include "SkImageDecoder.h"
#include "SkStream.h"
#include "SkMatrix.h"
#include "SkRect.h"
#include <android/native_window_jni.h>
#endif

#include "surface.h"
#include "sw_decode.h"

#undef LOG_TAG
#define LOG_TAG "ASurface"

#define CHECK(val) \
    if(!val) { \
        LOGE("%s [%i]: NULL pointer exception!", __func__, __LINE__); \
        return -1; \
    }

#define SDK_VERSION_FROYO 8

using namespace android;

typedef struct ASurface {
    /* our private members here */
#ifdef NDK
	ANativeWindow *surface;
#else
    Surface* surface;
#endif
    SkCanvas* canvas;
} ASurface;

SkBitmap    srcBitmap;

//static int out_size;
static char *pBuf888, *pBuf8888;
#define MAX_RGB888_BUFFER_SIZE 1024 * 1024 * 10
#define MAX_RGB8888_BUFFER_SIZE 1024 * 1024 * 10
//static char *outBuf;
//static char *outBuf2;

int g_clear_screen_client;

unsigned long long _get__system_mstime(void)
{
	unsigned long long mstime = 0;

	struct timeval curr_time;

	if (gettimeofday(&curr_time, 0) != 0)
	{
		printf("error!\n");
		return -1;
	}

	mstime = (unsigned long long)curr_time.tv_sec * 1000 + curr_time.tv_usec / 1000;
	LOGD("mstime = %lld\n", mstime); 
	return mstime;
}

#ifndef NDK
static Surface* getNativeSurface(JNIEnv* env, jobject jsurface, int sdkVersion) {
    /* we know jsurface is a valid local ref, so use it */
    jclass clazz = env->GetObjectClass(jsurface);
    if(clazz == NULL) {
        LOGE("Can't find surface class!");
        return NULL;
    }

    jfieldID field_surface = env->GetFieldID(clazz,
                                             sdkVersion > SDK_VERSION_FROYO ? "mNativeSurface" : "mSurface",
                                             "I");
    if(field_surface == NULL) {
        LOGE("Can't find native surface field!");
        return NULL;
    }
    return (Surface *) env->GetIntField(jsurface, field_surface);
}
#endif

int ASurface_init(JNIEnv* env, jobject jsurface, int sdkVersion, ASurface** aSurface) {

	LOGD("%s: start", __func__);
    if(!env || jsurface == NULL) {
        LOGE("JNIEnv or jsurface obj is NULL!");
        return -1;
    }

#ifdef NDK
	sdkVersion = sdkVersion;
	ANativeWindow *surface= ANativeWindow_fromSurface(env, jsurface);
#else
    Surface* surface = getNativeSurface(env, jsurface, sdkVersion);
#endif
    if(!surface) {
        LOGE("Can't obtain native surface!");
        return -1;
    }

    *aSurface = (ASurface *) malloc(sizeof(ASurface));
    (*aSurface)->surface = surface;
    (*aSurface)->canvas = new SkCanvas;

	/* allocate RGB buffer */
	pBuf888 = (char *) malloc (MAX_RGB888_BUFFER_SIZE);
	pBuf8888 = (char *) malloc (MAX_RGB8888_BUFFER_SIZE);
	if (!pBuf888 || !pBuf8888)
	{
		LOGE("allocate RGB buffer FAILURE!\n");
		return -1;
	}

	LOGD("%s: end", __func__);
    return 0;
}

void ASurface_deinit(ASurface** aSurface) {
    delete (*aSurface)->canvas;
    free(*aSurface);
    *aSurface = NULL;

	/* clear RGB buffer */
	if (pBuf888)
	{
		free(pBuf888);
		pBuf8888 = NULL;
	}
	if (pBuf8888)
	{
		free(pBuf8888);
		pBuf8888 = NULL;
	}
}

int ASurface_lock(ASurface* aSurface, AndroidSurfaceInfo* info) {
    CHECK(aSurface);
    CHECK(aSurface->surface);

#ifdef NDK
	ANativeWindow_Buffer buf;
	ANativeWindow_acquire(aSurface->surface);
	ANativeWindow_lock(aSurface->surface, &buf, NULL);

    info->w = buf.width;
    info->h = buf.height;
    info->s = buf.stride;
    info->usage = 0;
    info->format = buf.format;
    info->bits = buf.bits;
#else
    static Surface::SurfaceInfo surfaceInfo;
    Surface* surface = aSurface->surface;
    if (!surface->isValid()) {
        LOGE("Native surface isn't valid!");
        return -1;
    }

    int res = surface->lock(&surfaceInfo);
    if(res < 0) {
        LOGE("Can't lock native surface!");
        return res;
    }

    info->w = surfaceInfo.w;
    info->h = surfaceInfo.h;
    info->s = surfaceInfo.s;
    info->usage = surfaceInfo.usage;
    info->format = surfaceInfo.format;
    info->bits = surfaceInfo.bits;
#endif

    return 0;
}

static SkBitmap::Config
convertPixelFormat(APixelFormat format) {
    switch(format) {
        case ANDROID_PIXEL_FORMAT_RGBX_8888:
        case ANDROID_PIXEL_FORMAT_RGBA_8888:
            return SkBitmap::kARGB_8888_Config;
        case ANDROID_PIXEL_FORMAT_RGB_565:
            return SkBitmap::kRGB_565_Config;
    }
    return SkBitmap::kNo_Config;
}

static void
initBitmap(SkBitmap& bitmap, AndroidSurfaceInfo* info) {
    bitmap.setConfig(convertPixelFormat(info->format), info->w, info->h);
    if (info->format == ANDROID_PIXEL_FORMAT_RGBX_8888) {
        bitmap.setIsOpaque(true);
    }
#if 1
    if (info->w > 0 && info->h > 0) {
        bitmap.setPixels(info->bits);
    } else {
        // be safe with an empty bitmap.
        bitmap.setPixels(NULL);
    }
#else
        bitmap.setPixels(NULL);
        bitmap.setPixels(info->bits);
#endif
}

int ASurface_decode(SkBitmap* bitmap, const void* src, size_t size, int *width, int *height) {
    SkImageDecoder::Format fmt;

	unsigned long long stamp1, stamp2;
#ifndef SKIA_DECODE
	outBuf = pBuf888;
	outBuf2 = pBuf8888;
	int bit_per_pixel = 3; //sw_decode out format is 888
	int i;

	stamp1 = _get__system_mstime();
	if(-1 == sw_decode((char *)src, size, (char *)outBuf, &out_size, width, height))
	{
		LOGE("sw_decode failed!\n");
		return -1;
	}
	//LOGD("out_size is %d\n", out_size);

	//888->8888
	for(i = 0; i < out_size / bit_per_pixel; i++)
	{
		*outBuf2++ = *outBuf++;
		*outBuf2++ = *outBuf++;
		*outBuf2++ = *outBuf++;
		*outBuf2++ = 0xFF;
	}
	outBuf2 = pBuf8888;
	stamp2 = _get__system_mstime();
	LOGD("---------------------------------------\n");
	LOGD("\t\tsw decode pure = %lld ms\n", stamp2 - stamp1);
	LOGD("---------------------------------------\n");

#else
	stamp1 = _get__system_mstime();
    bool result = SkImageDecoder::DecodeMemory(src, size, bitmap,
		    SkBitmap::kARGB_8888_Config, SkImageDecoder::kDecodePixels_Mode,
		    &fmt); //SkBitmap::kRGB_565_Config
    if (!result) {
        LOGE("decoder file fail!");
        return -1;
    } else {
        if (fmt!= SkImageDecoder::kJPEG_Format) {
            LOGI("decoder file not jpeg!");
	    return -1;
		}
    }
	stamp2 = _get__system_mstime();
	LOGD("---------------------------------------\n");
	LOGD("\t\tsw decode SkImageDecoder::DecodeMemory = %lld ms\n", stamp2 - stamp1);
	LOGD("---------------------------------------\n");
#endif
    return 0;
}

void ASurface_scaleToFullScreen(ASurface* aSurface, AndroidSurfaceInfo* src, AndroidSurfaceInfo* dst) {
    SkBitmap    srcBitmap;
    SkBitmap    dstBitmap;
    SkMatrix    matrix;

    initBitmap(srcBitmap, src);
    initBitmap(dstBitmap, dst);
    matrix.setRectToRect(SkRect::MakeWH(srcBitmap.width(), srcBitmap.height()),
                         SkRect::MakeWH(dstBitmap.width(), dstBitmap.height()),
                         SkMatrix::kCenter_ScaleToFit);

    aSurface->canvas->setBitmapDevice(dstBitmap);
    aSurface->canvas->drawBitmapMatrix(srcBitmap, matrix);
    return;
}

void ASurface_scaleToFullScreen_skia(ASurface* aSurface, AndroidSurfaceInfo* src,
							AndroidSurfaceInfo* dst,
							size_t size) {
    SkBitmap    dstBitmap;
    SkMatrix    matrix;
	void *pixel = NULL;
	int swidth = 0;
	int sheight = 0;
	int i, width, height, buf_size;

	width = 0;
	height = 0;

	struct jpeg_ext *ext = (struct jpeg_ext *)src->bits;
	char *real_jpeg = (char *)src->bits + sizeof(struct jpeg_ext);

	LOGD("```````````````````````````````````");
	LOGD("%s: %d\n", __func__, __LINE__);
	if(ext->fragment_num == 1)
	{
		if (ASurface_decode(&srcBitmap, real_jpeg, size, &swidth, &sheight)) {
		    LOGE("decode error\n");
		    return;
	    }
		LOGD("swidth = %d, sheight = %d\n", swidth, sheight);
	}
	else
	{
		SkBitmap sktemp[6];
		width = 0;
		height = 0;
		for(i = 0; i < ext->fragment_num; i++)
		{
			if (ASurface_decode(&sktemp[i], real_jpeg + ext->fragment[i].offset, ext->fragment[i].size, &swidth, &sheight)) {
				LOGE("decode error\n");
				return;
			}
			width = sktemp[i].width();
			height += sktemp[i].height();
		}
		srcBitmap.setConfig(SkBitmap::kRGB_565_Config, width, height);
		LOGD("%s: real height = %d width = %d\n", __func__, height, width);
		buf_size =  width * height * 2;
		pixel = malloc(buf_size);
		if(!pixel)
			return;
		width =  height = 0;
		for(i = 0; i < ext->fragment_num; i++)
		{
			void *frag_buf;
			width = sktemp[i].width();
			frag_buf = sktemp[i].getPixels();
			memcpy((char *)pixel + (height * width * 2), frag_buf, sktemp[i].getSize());
			height += sktemp[i].height();
		}
		srcBitmap.setPixels(pixel);
	}

	if(dst->w != dst->s)
	{
		void *buf_temp;
		int i, pixel_size;
		SkBitmap temp;
		if(dst->format == ANDROID_PIXEL_FORMAT_RGB_565)
			pixel_size = 2;
		else
			pixel_size = 4;
		buf_temp = malloc(dst->w * dst->h * pixel_size);
		if(!buf_temp)
			return;
		temp.setConfig(convertPixelFormat(dst->format), dst->w, dst->h);
		temp.setPixels(buf_temp);
		matrix.setRectToRect(SkRect::MakeWH(srcBitmap.width(), srcBitmap.height()),
			                 SkRect::MakeWH(temp.width(), temp.height()),
			                 SkMatrix::kFill_ScaleToFit);

		for(i = 0; i < (int)dst->h; i++)
			memcpy((char *)dst->bits + i * dst->s * pixel_size, (char *)buf_temp + i * dst->w * pixel_size, dst->w * pixel_size);
		dstBitmap.setConfig(convertPixelFormat(dst->format), dst->s, dst->h);
		dstBitmap.setPixels(dst->bits);
		aSurface->canvas->setBitmapDevice(dstBitmap);
		aSurface->canvas->drawBitmapMatrix(srcBitmap, matrix);
		free(buf_temp);
	}
	else
	{
#ifndef SKIA_DECODE
		srcBitmap.setConfig(convertPixelFormat(ANDROID_PIXEL_FORMAT_RGBA_8888), swidth, sheight);
		srcBitmap.setPixels(outBuf2);
#else
#endif

		initBitmap(dstBitmap, dst);
#if 0 /*original*/
		LOGD("Jerry catch Tom: SKIA : g_clear_screen_client = %d\n", g_clear_screen_client);
		matrix.setRectToRect(SkRect::MakeWH(srcBitmap.width(), srcBitmap.height()),
			                 SkRect::MakeWH(dstBitmap.width(), dstBitmap.height()),
			                 SkMatrix::kFill_ScaleToFit);
#else

	/* clear screen */
	//memset(srcBitmap.getPixels(), 0, width * height * 2);
	LOGD("Hi, Tom, I'm Jerry!\n");
	LOGD("dst->w = %d, dst->h = %d", dst->w, dst->h);
	memset(dst->bits, 0, dst->w * dst->h * 2);

if (g_clear_screen_client)
{
		matrix.setRectToRect(SkRect::MakeWH(srcBitmap.width(), srcBitmap.height()),
			                 SkRect::MakeWH(dstBitmap.width(), dstBitmap.height()),
			                 SkMatrix::kFill_ScaleToFit);
		aSurface->canvas->setBitmapDevice(dstBitmap);
		aSurface->canvas->drawBitmapMatrix(srcBitmap, matrix);
}
else
{
		matrix.setRectToRect(SkRect::MakeWH(srcBitmap.width(), srcBitmap.height()),
			                 SkRect::MakeWH(dstBitmap.width(), dstBitmap.height()),
			                 SkMatrix::kCenter_ScaleToFit);
		aSurface->canvas->setBitmapDevice(dstBitmap);
		aSurface->canvas->drawBitmapMatrix(srcBitmap, matrix);
}
#endif

	}
	if(ext->fragment_num > 1)
	{
		free(pixel);
		srcBitmap.setPixels(NULL);
	}
    return;
}

int ASurface_rotate(ASurface* aSurface, AndroidSurfaceInfo* src, uint32_t degrees) {
    SkBitmap    bitmap;

    CHECK(aSurface);
    CHECK(src);

    initBitmap(bitmap, src);
    aSurface->canvas->setBitmapDevice(bitmap);
    return aSurface->canvas->rotate(SkScalar(degrees)) ? 0 : -1;
}


int ASurface_unlockAndPost(ASurface* aSurface) {
    CHECK(aSurface);
    CHECK(aSurface->surface);
#ifdef NDK
	ANativeWindow_unlockAndPost(aSurface->surface);
	ANativeWindow_release(aSurface->surface);
	return 0;
#else
    return aSurface->surface->unlockAndPost();
#endif
}
