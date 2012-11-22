#ifndef __SW_DECODE_H_
#define __SW_DECODE_H_

#define JPEG_CJPEG_DJPEG	/* define proper options in jconfig.h */
#define JPEG_INTERNAL_OPTIONS	/* cjpeg.c,djpeg.c need to see xxx_SUPPORTED */
#include "jinclude.h"
#include "jpeglib.h"
#include "jerror.h"		/* get library error codes too */
#include "cderror.h"		/* get application-specific error codes */

#ifdef __cplusplus
extern "C"
{
#endif
	int sw_decode(char *src, int in_size, char *dst, int *out_size, int *width, int *height);
#ifdef __cplusplus
}
#endif

#endif
