#include "rgb2yuv.h"

unsigned char iY_Table[384];
unsigned char iCb_Table[768 * 2];
unsigned char iCr_Table[768 * 2];
unsigned char *ipCb_Table;
unsigned char *ipCr_Table;
unsigned char xtable_y[65536];
unsigned char xtable_u[65536];
unsigned char xtable_v[65536];


void __init_ycbcr(void)
{
    int i;
    unsigned char *pTable;
    #define pv_max(a, b)    ((a) >= (b) ? (a) : (b))
    #define pv_min(a, b)    ((a) <= (b) ? (a) : (b))

    /* Table generation */
    for (i = 0; i < 384; i++)
        iY_Table[i] = (unsigned char) pv_max(pv_min(255, (int)(0.7152 * i + 16 + 0.5)), 0);

    pTable = iCb_Table + 384;
    for (i = -384; i < 384; i++)
        pTable[i] = (unsigned char) pv_max(pv_min(255, (int)(0.386 * i + 128 + 0.5)), 0);
    ipCb_Table = iCb_Table + 384;

    pTable = iCr_Table + 384;
    for (i = -384; i < 384; i++)
        pTable[i] = (unsigned char) pv_max(pv_min(255, (int)(0.454 * i + 128 + 0.5)), 0);
    ipCr_Table = iCr_Table + 384;
}


void init_ycbcr(void)
{
    int i;
    unsigned char r, g, b;
    __init_ycbcr();
    for(i = 0; i < 65536; i++)
    {
        r = (i >> 8) & 0xf8;
        g = (i >> 3) & 0xfc;
        b = (i << 3) & 0xf8;
        xtable_y[i] = iY_Table[(6616*r + (g << 16) + 19481 * b) >> 16];
        xtable_u[i] = ipCb_Table[((r << 16) - (g << 16) + 19525 * (r - b)) >> 16];
        xtable_v[i] = ipCr_Table[((b << 16) - (g << 16) + 6640 * (b - r)) >> 16];
	 }
}

void rgb2yuv24bit(unsigned char *rgb, unsigned char *y, unsigned char *uv, int width, int height)
{
	int i, j;
	unsigned short s;
	unsigned char r, g, b;
	int adjust = width * 3;
//	unsigned char *last_rgb_row = NULL;
	for(i = 0; i < height; i++)
	{
		for(j = 0; j < adjust; j += 3)
		{
			r = rgb[j] & 0xf8;
			g = rgb[j + 1] & 0xfc;
			b = rgb[j + 2] & 0xf8;
			s = r << 8 | g << 3 | b >> 3;
			*y++ = xtable_y[s];
			if((i & 1) && (j & 1))
			{
			/*  r = rgb[j - 3] & 0xf8;
				g = rgb[j - 2] & 0xfc;
				b = rgb[j - 1] & 0xf8;
				a = r << 8 | g << 3 | b >> 3;

				r = last_rgb_row[j - 3] & 0xf8;
				g = last_rgb_row[j - 2] & 0xfc;
				b = last_rgb_row[j - 1] & 0xf8;
				q = r << 8 | g << 3 | b >> 3;


				r = last_rgb_row[j] & 0xf8;
				g = last_rgb_row[j + 1] & 0xfc;
				b = last_rgb_row[j + 2] & 0xf8;
				w = r << 8 | g << 3 | b >> 3;
				*uv++ = (xtable_u[q] + xtable_u[w] + xtable_u[a] + xtable_u[s]) >> 2;
				*uv++ = (xtable_v[q] + xtable_v[w] + xtable_v[a] + xtable_v[s]) >> 2;*/
				*uv++ = xtable_u[s];
				*uv++ = xtable_v[s];
			}
		}
//		last_rgb_row = rgb;
		rgb += adjust;
	}
}

void rgb2yuv16bit(unsigned char *rgb, unsigned char *y, unsigned char *uv, int width, int height)
{
	int i, j;
	unsigned short s;
	int adjust = ((width + 15) & 0xfffffff0) << 1;

	width = width << 1;
	for(i = 0; i < height; i++)
	{
		for(j = 0; j < width; j += 2)
		{
			s = rgb[j + 1]<< 8 | rgb[j];
			*y++ = xtable_y[s];
			if((i & 1) && (j & 2))
			{
				*uv++ = xtable_v[s];
				*uv++ = xtable_u[s];
			}
		}
		rgb += adjust;
	}
}


void rgb2yuv32bit(unsigned char *rgb, unsigned char *y, unsigned char *uv, int width, int height)
{
	int i, j;
	unsigned short s;
	unsigned char r, g, b;
	int adjust = ((width + 15) & 0xfffffff0) << 2;

	width = width << 2;
	for(i = 0; i < height; i++)
	{
		for(j = 0; j < width; j += 4)
		{
			r = rgb[j + 2] & 0xf8;
			g = rgb[j + 1] & 0xfc;
			b = rgb[j + 0] & 0xf8;
			s = r << 8 | g << 3 | b >> 3;

			*y++ = xtable_y[s];
			if((i & 1) && (j & 4))
			{
				*uv++ = xtable_u[s];
				*uv++ = xtable_v[s];
			}
		}
		rgb += adjust;
	}
}

