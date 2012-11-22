#include <ctype.h>
#ifdef __cplusplus
extern "C" {
#endif
#include "sw_decode.h"
#include "jversion.h"
#include "config.h"
#include "utils.h"
#include "common.h"
#ifdef __cplusplus
}
#endif

#undef LOG_TAG
#define LOG_TAG "SW_DECODE"

int sw_decode(char *src, int in_size, char *dst, int *out_size, int *width, int *height)
{
#if 0
	int i;
	int row_stride;
#endif
	char * buf = src;
	int file_size = in_size;
	struct jpeg_error_mgr jerr;
	struct jpeg_decompress_struct jinfo;
	JSAMPROW ptr = (JSAMPLE *)dst;

#if 0
	FILE *pfile = fopen("fbx.jpg", "r");
	//load jpeg image file
	printf("pfile = %p\n", pfile);
	fseek(pfile, 0, SEEK_END);
	file_size = ftell(pfile);
	fseek(pfile, 0, SEEK_SET);
	buf = (char *)calloc(file_size, 1);
	fread(buf, 1, file_size, pfile);
	printf("file_size = %d\n", file_size);
#endif
	//LOG_D("%s: %d, in_size = %d\n", __func__, __LINE__, in_size);

	jinfo.err = jpeg_std_error(&jerr);
    jpeg_create_decompress(&jinfo);
    jpeg_mem_src(&jinfo, (unsigned char*)buf, file_size);
    if (jpeg_read_header(&jinfo, TRUE) != JPEG_HEADER_OK) goto bail;
    jinfo.dct_method = JDCT_ISLOW;

    if (!jpeg_start_decompress(&jinfo)) goto bail;

    if (jinfo.num_components != 1 && jinfo.num_components != 3) goto bail;
	//char * outbuf = NULL;
	//int out_size = ( jinfo.output_width * jinfo.output_height * jinfo.output_components);
	//outbuf = malloc(out_size);
	*out_size = ( jinfo.output_width * jinfo.output_height * jinfo.output_components);
	//LOG_D("out_size = %d\n");

    if (!buf) goto bail;

	while (jinfo.output_scanline < jinfo.output_height)
	{
		if (jpeg_read_scanlines(&jinfo, &ptr, 1) != 1) goto bail;
		ptr += jinfo.output_width * jinfo.output_components;
	}

	//LOG_D("%s: %d jinfo.output_width = %d\n", __func__, __LINE__, jinfo.output_width);
	*width = jinfo.output_width;
	*height = jinfo.output_height;
    //LOG_D("bpp = %d\n", jinfo.output_components);
    if (!jpeg_finish_decompress(&jinfo)) goto bail;
    jpeg_destroy_decompress(&jinfo);

	//jerry dump to BMP file
	static int firstOut = 0;

	if (firstOut == 0)
	{
	  firstOut = 1;
	  FILE *fout = fopen("/mnt/sdcard/fbxx.raw", "w");
		if (!fout)
		{
			printf("outfile is NULL!\n");
			return -1;
		}
	  fwrite(dst, 1, *out_size, fout);
	  fclose(fout);
	}
	return 0;			/* suppress no-return-value warnings */

bail:
	LOG_D("%s: %d, go to ERROR!\n", __func__, __LINE__);
    jpeg_destroy_decompress(&jinfo);
	return -1;			/* suppress no-return-value warnings */
}
