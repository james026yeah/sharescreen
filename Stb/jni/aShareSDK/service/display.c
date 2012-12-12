/*
 * =====================================================================================
 *
 *       Filename:  display.cpp
 *
 *    Description:
 *
 *        Version:  1.0
 *        Created:  04/18/2012 06:10:49 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  cyher (), cyher.net A~T gmail.com
 *        Company:  cyher.net
 *
 * =====================================================================================
 */
#include <stdio.h>
#include <errno.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>
#include "display.h"
#include "utils.h"
#include "common.h"
#include "jpeg_enc.h"

#undef LOG_TAG
#define LOG_TAG "ISHARE_DIS"

extern int sw_encode_flag;
extern int g_angle; //ratation, 0, 90, 180, 270

void dump_frame(unsigned char* buff, size_t size)
{
	FILE *fout = NULL;
	fout = fopen(DUMP_PATH, "wb");

	if (!fout) {
		perror(DUMP_PATH);
		return;
	}

	size_t nwrite = fwrite(buff, sizeof(char), size, fout);
	if (nwrite != size)
		perror(DUMP_PATH);

	fclose(fout);
	return;
}

void display_done(void)
{
	if (sw_encode_flag == 0)
		hw_jpeged();
	return;
}

int display_detect_filp(struct display_info *di)
{
	static int same_times;
	LOG_D("IN [%s:%d]\n", __func__, __LINE__);

	if (ioctl(di->fd, FBIOGET_VSCREENINFO, &di->vi) < 0)
		return -1;

	if (di->fb_pre_offset != di->vi.yoffset) {
		same_times = 0;
		di->fb_pre_offset = di->vi.yoffset;
		return 1;
	}

	if (++same_times < 10) {
		usleep(1000 * same_times);
		return 1;
	} else {
		usleep(33333); /* 30 fps period */
		return 0;
	}
	LOG_D("OUT [%s:%d]\n", __func__, __LINE__);

	return 0;
}

int display_get_frame(struct display_info *di)
{
	di->timestamp = timestamp();
	di->angle = g_angle;

	if (sw_encode_flag)
		return sw_jpeg(di);
	else
		return hw_jpeg(di);
}

int display_info_init(struct display_info *di)
{

	LOG_D("In [%s:%d]\n", __func__, __LINE__);

	if (di == NULL) {
		LOGE("display init error %s", strerror(errno));
		return -1;
	}

	di->fd = open(FB_PATH, O_RDWR);

	if (di->fd < 0) {
		LOGE("open fb node : %s", strerror(errno));
		return -1;
	}

	if (ioctl(di->fd, FBIOGET_FSCREENINFO, &di->fi) < 0)
		goto fail;

	if (ioctl(di->fd, FBIOGET_VSCREENINFO, &di->vi) < 0)
		goto fail;

	di->frame_base = (unsigned char*)mmap(0,
				di_size(di),
				PROT_READ | PROT_WRITE,
				MAP_SHARED, di->fd, 0);

	if (di->frame_base == MAP_FAILED)
		goto fail;
	/*
	 * save offset to detect framebuffer filp
	 * initialize fb_pre_offset != yoffset
	 */	
	di->fb_pre_offset = !di->vi.yoffset;
	di->quality = QUALITY_LEVEL1;
	LOG_D("--------------------------------\n  width : %8d\n  height: %8d\n  bpp   : %8d\n	R(%2d, %2d)\n	G(%2d, %2d)\n	B(%2d, %2d)\n	A(%2d, %2d)\n"
			"  yoffset=%d\n"
			"  xoffset=%d\n"
			"  fix->smem_start=0x%08x\n"
			"  quality=%d\n",
		di_width(di), di_height(di), di_bpp(di),
		di->vi.red.offset, di->vi.red.length,
		di->vi.green.offset, di->vi.green.length,
		di->vi.blue.offset, di->vi.blue.length,
		di->vi.transp.offset, di->vi.transp.length,
		di->vi.yoffset, di->vi.xoffset,
		(unsigned int)di->fi.smem_start,
		di->quality);
	LOG_D("----------------------------------\n");
	LOG_D("OUT [%s:%d]\n", __func__, __LINE__);
	return 0;

fail:
	LOGE("ERROR!!!!\n");
	close(di->fd);
	free(di);
	return -1;
}

void display_info_deinit(struct display_info *di)
{
	munmap(di->frame_base, di_size(di));
	close(di->fd);
	return;
}
