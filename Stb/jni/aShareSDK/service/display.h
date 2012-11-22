/*
 * =====================================================================================
 *
 *       Filename:  display.h
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  04/20/2012 11:07:32 AM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  cyher (), cyher.net A~T gmail.com
 *        Company:  cyher.net
 *
 * =====================================================================================
 */
#ifndef __DISPLAY_H__
#define __DISPLAY_H__

#include <linux/fb.h>
#define DUMP_PATH "/data/fb.jpg"
#define FB_PATH "/dev/graphics/fb0"

struct display_info {
	int fd;
	int machine;
	int angle;
	unsigned char *frame_base;
	struct fb_fix_screeninfo fi;
	struct fb_var_screeninfo vi;
	unsigned int fb_pre_offset;
	uint64_t timestamp;

	unsigned char *jpeg_base;
	int jpeg_size;
	unsigned int quality;
	unsigned int slice_flag;
	unsigned int slice_size[4];
	unsigned int slice_num;
#define di_width(di)  ((di)->vi.xres)
#define di_height(di) ((di)->vi.yres)
#define di_bpp(di)    ((di)->vi.bits_per_pixel>>3)
#define di_size(di)   ((di)->vi.xres_virtual * (di)->vi.yres * di_bpp(di))
#define di_offset(di)   ((di)->vi.xres * (di)->vi.yoffset * \
		di_bpp(di))
#define di_line_length(di) ((di)->fi.line_length)
};

int display_detect_filp(struct display_info *di);

int display_get_frame(struct display_info *di);

int display_info_init(struct display_info *di);

void display_info_deinit(struct display_info *di);

void display_done(void);
#endif
