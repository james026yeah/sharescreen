#ifndef __RGB_2_YUV_H2V2_H__
#define __RGB_2_YUV_H2V2_H__

#include <stdio.h>

void init_ycbcr(void);
void rgb2yuv16bit(unsigned char *rgb, unsigned char *y, unsigned char *uv, int width, int height);
void rgb2yuv32bit(unsigned char *rgb, unsigned char *y, unsigned char *uv, int width, int height);

#endif

