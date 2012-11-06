#ifndef _JPEG_ENCODE_H
#define _JPEG_ENCODE_H
#include <sys/mman.h>
#include <stdio.h>
#include <stdlib.h>
#include <dlfcn.h>
#include "display.h"
#include "utils.h"
#include "common.h"


#define QUALITY_LEVEL1		100
#define QUALITY_LEVEL2		75
#define QUALITY_LEVEL3		50   /*default quality*/
#define QUALITY_LEVEL4		25
#define QUALITY_LEVEL5		0

#define QUALCOMM_JPEG_LIB		"/data/data/com.archermind.ashare/lib/libqcom_jpeg.so"
#define QUALCOMM_JPEG_LIB2		"/data/data/com.archermind.ashare/lib/libqcom_jpeg2.so"
#define SAMSUNG_JPEG_LIB		"/data/data/com.archermind.ashare/lib/libsamsung_jpeg.so"

#define SYSTEM_QUALCOMM_LIB		"/system/lib/libmmjpeg.so"
#define SYSTEM_SAMSUNG_LIB		"/system/lib/libs3cjpeg.so"

#define		JPEG_ENCODER_HW_ACCELERATED_PREFERRED 		0
#define		JPEG_ENCODER_HW_ACCELERATED_ONLY		1
#define		JPEG_ENCODER_SOFTWARE_PREFERRED			2
#define		JPEG_ENCODER_SOFTWARE_ONLY			3
#define		JPEG_ENCODER_DONT_CARE				4
#define	    	JPEG_ENCODER_MAX				5


#define		RETURN_SUCCESS					0
#define		RETURN_FAILURE					-1
#define		RETURN_UNSUPPORT				-2

#define		NICE_PRIORITY					-20

typedef enum {
		QUALCOMM_PLAT	=	0,
		SAMSUNG_PLAT 		,
		MEDIATEK_PLAT		,
		LIBJPEG_TURBO		,
		UNKNOWN_PLAT		,
}cpu_platform_t;

struct hw_enc_param {
	unsigned int 		quality;
	unsigned int 		width;
	unsigned int 		height;
	int 			in_format;
	int 			out_format;
	unsigned char 		*in_buf;
	unsigned int 		in_offset;
	unsigned int 		indata_size;
	unsigned char		*out_buf;
	unsigned int 		outdata_size;
	unsigned int 		flip_offset;
	int			slice_flag;
	unsigned int 		slice_size[4];
	int			slice_num;
	uint32_t		preference;
	uint32_t		bytes_per_pixel;
	cpu_platform_t		platform_type;

};

enum {
    HW_JPG_444,
    HW_JPG_422,
    HW_JPG_420,
    HW_JPG_400,
    HW_RESERVED1,
    HW_RESERVED2,
    HW_JPG_411,
    HW_JPG_SAMPLE_UNKNOWN
};

enum {
    HW_JPG_MODESEL_YCBCR = 1,
    HW_JPG_MODESEL_RGB,
    HW_JPG_MODESEL_UNKNOWN
};

/* SW ENCODE USAGE */
#define SW_RGBX 0
#define SW_BGRX 1
#define SW_XRGB 2
#define SW_XBGR 3

int hw_jpeg_init(struct display_info *di);
int sw_jpeg_init(struct display_info *di);
void sw_jpeg_deinit(void);
void hw_jpeg_deinit(void);
int sw_jpeg(struct display_info* di);
int hw_jpeg(struct display_info* di);
void hw_jpeged(void);

#endif
