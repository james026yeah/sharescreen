/*
 * =====================================================================================
 *
 *       Filename:  native_surface.c
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  05/22/2012 10:26:26 AM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  cyher (), cyher.net A~T gmail.com
 *        Company:  cyher.net
 *
 * =====================================================================================
 */
#include <pthread.h>
#include <surface.h>
#include <errno.h>
#include "common.h"
#include <unistd.h>
#include <dlfcn.h>
#include <stdio.h>
#include "buffer_client.h"
#include "utils.h"
#include "comm.h"
#include <sys/system_properties.h>
int g_clear_screen_client;
#if defined(LOG_DEBUG) && LOG_DEBUG == 1

#define LOG_TIME_DEFINE(n) \
    struct timeval time_start_##n, time_stop_##n; unsigned long log_time_##n = 0;

#define LOG_TIME_START(n) \
    gettimeofday(&time_start_##n, NULL);

#define LOG_TIME_END(n) \
    gettimeofday(&time_stop_##n, NULL); log_time_##n = measure_time(&time_start_##n, &time_stop_##n);

#define LOG_TIME(n) \
    log_time_##n

#define LOG_D(...) LOGD(__VA_ARGS__)
#else
#define LOG_TIME_DEFINE(n)
#define LOG_TIME_START(n)
#define LOG_TIME_END(n)
#define LOG_TIME(n)
#define LOG_D(...)
#endif

#undef LOG_TAG
#define LOG_TAG "ISHARE_SURFACE"

void init_native_surface(void)
{
#if 0
	lib_ASurface_init = dlsym(libsyslib, "ASurface_init");
	lib_ASurface_deinit = dlsym(libsyslib, "ASurface_deinit");
	lib_ASurface_lock = dlsym(libsyslib, "ASurface_lock");
	lib_ASurface_scaleToFullScreen_skia = dlsym(libsyslib, "ASurface_scaleToFullScreen_skia");
	lib_ASurface_unlockAndPost = dlsym(libsyslib, "ASurface_unlockAndPost");
#endif
}

void deinit_native_surface(void)
{
#if 0
	if(libsyslib)
	{
		dlclose(libsyslib);
		libsyslib = NULL;
	}
#endif
}

static int running = 0;
static int first_frame = 1;
static int frame_count = 0;
static uint64_t last_time = 0;

static void cal_fps(void)
{
	uint64_t temp = timestamp();
	int interval = temp - last_time;
	frame_count++;
	if(interval > 2000)
	{
		char fps[128];
		last_time = temp;
		interval = (frame_count * 1000) / interval;
		frame_count = 0;
		sprintf(fps, "fps:%d", interval);
	}
}

static void *surface_thread(void *arg)
{
	ASurface *surface = (ASurface *)arg;
	void *stream;
	AndroidSurfaceInfo src, dst;
	size_t size;
	LOG_TIME_DEFINE(buff);
	LOG_TIME_DEFINE(decode);

	running = 1;
	while(running == 1) {
		struct jpeg_frame *jpeg;
		LOG_TIME_START(buff);
		size = get_image_buffer(&stream);
		jpeg = stream;

		g_clear_screen_client = jpeg->encode_time;
		LOGD("Jerry: final g_clear_screen_client = %d",
				g_clear_screen_client);

		LOG_D("surface get buffer time delay: %d ms\n", (int)(timestamp() - jpeg->timestamp));
		src.bits = stream + sizeof(struct jpeg_frame);
		LOG_TIME_END(buff);

		if((size > 0) && running)
		{
			LOG_TIME_START(decode);
#ifndef PHONE_NOT_SKIA
			ASurface_lock(surface, &dst);
			ASurface_scaleToFullScreen_skia(surface, &src, &dst, size - sizeof(struct jpeg_frame));
			ASurface_unlockAndPost(surface);
#endif
			if(first_frame)
			{
				char msg[128];
				first_frame = 0;
				frame_count = 0;
				last_time = timestamp();
				sprintf(msg, "surface:%d:%d", jpeg->width, jpeg->height);
			}
			cal_fps();
			LOG_TIME_END(decode);
			LOG_D("surface display time delay: %d ms, decode and display = %ld ms\n", (int)(timestamp() - jpeg->timestamp), LOG_TIME(decode) / 1000);
			release_image_buffer();
		}

//		LOG_D("------get buff = %ld us, decode and display = %ld us-----\n",
//				LOG_TIME(buff), LOG_TIME(decode));
		g_clear_screen_client = 0;
	}

	return ((void *)0);
}

static ASurface *aSurface = NULL;
static pthread_t surface_tid;
int surface_start(JNIEnv *env, jobject jsurface)
{
	int ret;

	LOGD("%s start\n", __func__);
	first_frame = 1;

#ifndef PHONE_NOT_SKIA
	ret = ASurface_init(env, jsurface, 15, &aSurface);
#endif
	ret = pthread_create(&surface_tid, NULL, surface_thread, aSurface);
	if (ret != 0)
		LOGE("create native surface thread error:%s\n", strerror(errno));

	LOGD("%s end\n", __func__);
	return 0;
}

int surface_stop()
{
	running = 0;
	signal_client_buffer();
	LOGD("%s ++++++++++\n", __func__);
	pthread_join(surface_tid, NULL);
	LOGD("%s ----------\n", __func__);
	if(aSurface)
	{
#ifndef PHONE_NOT_SKIA
		ASurface_deinit(&aSurface);
#endif
		aSurface = NULL;
	}
	return 0;
}
