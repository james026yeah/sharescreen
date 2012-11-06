#include "sw_encode.h"

#define DEBUG_TIMER			1
#define MAX_BUFFER_SIZE 	1024*1024	//1Mb  Accoring to jpeg quality
#define MAX_RGB_BUFFER MAX_BUFFER_SIZE * 5
#undef LOG_TAG
#define LOG_TAG "SW_ENCODE"

#include <sys/types.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "cdjpeg.h"		/* Common decls for cjpeg/djpeg applications */
#include "jpeglib.h"
#include "jversion.h"		/* for version message */
#include "config.h"

// Global variables
static uint8_t *rgb_buf;
//static uint8_t in_buf_size = 0;
static uint32_t out_buf_size = 0;
static uint8_t *out_buf_ptr = NULL;
static uint8_t *in_buf = NULL;
static uint8_t *in_buf_qcom = NULL;
static uint8_t *in_buf_rotation = NULL;
static uint8_t *out_buf = NULL;
static uint8_t *dummy_buf = NULL;

int support_qcom = 0;

unsigned long long _get_system_mstime(void)
{
	unsigned long long mstime = 0;

	struct timeval curr_time;

	if (gettimeofday(&curr_time, 0) != 0)
	{
		LOG_D("error!\n");
		return -1;
	}

	mstime = (unsigned long long)curr_time.tv_sec * 1000 + curr_time.tv_usec / 1000;
	LOG_D("mstime = %lld\n", mstime);
	return mstime;
}

int hw_encode_alloc_memory(void)
{
	return 0;
}

void free_memory (void)
{
	if (out_buf_ptr)
	{
		free (out_buf_ptr);
		out_buf_ptr = NULL;
	}

	if (in_buf)
	{
		free (in_buf);
		in_buf = NULL;
	}

	if (in_buf_qcom)
	{
		free (in_buf_qcom);
		in_buf_qcom = NULL;
	}

	if (in_buf_rotation)
	{
		free (in_buf_rotation);
		in_buf_rotation = NULL;
	}

	if (rgb_buf)
	{
		free (rgb_buf);
		rgb_buf = NULL;
	}

	LOGD("%s successfully!\n", __func__);
}

int alloc_memory (uint8_t ** buffer, uint32_t buffer_len)
{
	*buffer = (uint8_t *) malloc (sizeof (uint8_t) * buffer_len);

	if (NULL == *buffer)
	{
		LOG_D ("Malloc memory error!\n");
		return -1;
	}

	memset (*buffer, 0, buffer_len);
	return 0;

}

int sw_encode_init (uint32_t width, uint32_t height, uint32_t bytes_per_pixel)
{
	int adjust = 0;

	LOG_D("%s: %d\n", __func__, __LINE__);
	/*
	dummy_buf = malloc(1024 * 1024 * 100);

	if (dummy_buf)
	{
		*(dummy_buf + 1024 * 1024 * 5) = 0xFF;
		*dummy_buf = 0xFF;
	}
	*/

	if (-1 == alloc_memory (&out_buf_ptr, MAX_BUFFER_SIZE))
	{
		LOG_D ("Malloc memory error!\n");
		return -1;
	}
	adjust = (width + 15) & 0xfffffff0;
	if (-1 == alloc_memory (&in_buf, adjust * height * bytes_per_pixel))
	{
		LOG_D ("Malloc memory error!\n");
		return -1;
	}

	if (-1 == alloc_memory (&in_buf_qcom, adjust * height * bytes_per_pixel))
	{
		LOG_D ("Malloc memory error!\n");
		return -1;
	}

	if (-1 == alloc_memory (&in_buf_rotation, adjust * height * bytes_per_pixel))
	{
		LOG_D ("Malloc memory error!\n");
		return -1;
	}

	if (-1 == alloc_memory (&rgb_buf, MAX_RGB_BUFFER))
	{
		LOG_D ("Malloc memory error!\n");
		return -1;
	}

	return 0;
}

void sw_encode_deinit (void)
{
	if (dummy_buf)
		free(dummy_buf);
	else
		LOG_D("dummy_buf is NULL!\n");
	return free_memory();
}

int sw_encode (struct hw_enc_param *param)
{
	static unsigned char *pdest = NULL;
	static unsigned long dest_size;
	static struct jpeg_compress_struct cinfo;
	static struct jpeg_error_mgr jerr;
	static unsigned long long stamp, stamp2;
	int i;
	int is_scaled = 0;
	int scale_rate;
	int scale_rate_div;
	static JSAMPROW rowPointer[1];
	static int started = 0;
	static int started2 = 0;

	char *p_in_buf = (char *)in_buf;
	char *p_in_buf_qcom = (char *)in_buf_qcom;
	int line_bytes = 0;

	pdest = out_buf_ptr;
	dest_size = MAX_BUFFER_SIZE;


	if (param->in_buf == NULL)
	{
		LOG_D ("JPEG input buffer is NULL!!\n");
		return -1;
	}

	/* discard verbose data for qcom platform */
	int pad = 0;
	int loop;
	if (support_qcom)
		pad = (16 - ( param->width % 16 ) ) % 16;

	LOG_D("pad = %d, support_qcom = %d\n", pad, support_qcom);
	LOG_D("param->height = %d, param->width = %d",
			param->height, param->width);
	LOG_D("bytes_per_pixel = %d", param->bytes_per_pixel);

	memcpy (in_buf, param->in_buf, param->height * (param->width + pad) * param->bytes_per_pixel);

#if 0
	//dump in_buf
	LOG_D("==========================================================");
	for (loop = 0; loop < param->width * param->bytes_per_pixel * 2;)
	{
		LOG_D("in_buf[%d] = %0X %0X %0X %0X",
				loop / 4,
				*(in_buf + loop),
				*(in_buf + loop + 1),
				*(in_buf + loop + 2),
				*(in_buf + loop + 3)
				);
		loop += param->bytes_per_pixel;
	}
	LOG_D("***********************************************************");
#endif

	if(pad)
	{
		/* in_buf -> in_buf_qcom */
		for (loop = 0; loop < param->height; loop++)
		{
			line_bytes = param->bytes_per_pixel * param->width;
			memcpy(p_in_buf_qcom, p_in_buf, line_bytes);
			p_in_buf_qcom += line_bytes;
			p_in_buf += line_bytes + pad * param->bytes_per_pixel;
		}

	}

	p_in_buf = pad ? (char *)in_buf_qcom : (char *)in_buf;

#if 0
	//dump in_buf_qcom
	LOG_D("==========================================================");
	for (loop = 0; loop < param->width * param->bytes_per_pixel * 2;)
	{
		LOG_D("in_buf_qcom[%d] = %0X %0X %0X %0X",
				loop / 4,
				*(in_buf_qcom + loop),
				*(in_buf_qcom + loop + 1),
				*(in_buf_qcom + loop + 2),
				*(in_buf_qcom + loop + 3)
				);
		loop += param->bytes_per_pixel;
	}
	LOG_D("***********************************************************");
#endif

	/* rgb scale */
	out_buf = rgb_buf;
	if (param->height > 1024 || param->width > 768)
	{
		is_scaled = 1;
		scale_rate = 3;
		scale_rate_div = 5;
	}

	if (is_scaled)
	{
		LOG_D("rgb scaling ......\n");
		stamp = _get_system_mstime();
		rgb_scale((char *)out_buf, (char *)p_in_buf,
				(unsigned int)((( param->width ) / scale_rate_div) * scale_rate ) & 0xfffffff0,
				(unsigned int)((( param->height ) / scale_rate_div) * scale_rate ) & 0xfffffff0,
				(unsigned int)param->width, (unsigned int)param->height);
		stamp2 = _get_system_mstime();
		LOG_D("scale time: %lldms\n", stamp2 - stamp);
	}

	if (started2 == 0)
	cinfo.err = jpeg_std_error(&jerr);
	if (started == 0)
	jpeg_create_compress(&cinfo);

	cinfo.image_width = is_scaled ?
		((param->width / scale_rate_div) * scale_rate ) & 0xfffffff0
		:
		param->width;
	cinfo.image_height = is_scaled ?
		((param->height / scale_rate_div) * scale_rate ) & 0xfffffff0
		:
		param->height;

	/* Rotation */
	/* WARNING:
	 * If pad is existed,
	 * This portion should be added for qualcomm 16-pixels alignment
	 * Ha, nothing qualcomm phone is available, so addition later...
	 */
	/* Ratation image from A * B to B * A */
	char * pSrc = p_in_buf;
	char * pDest = (char *)in_buf_rotation;
	int i2,j2;
	int _height = cinfo.image_height;
	int _width = cinfo.image_width;
	int cube = param->bytes_per_pixel;
	for (i2 = 0; i2 < _height; i2++)
		for (j2 = 0; j2 < _width; j2++)
			memcpy((pDest + j2 * _height * cube + (_height - i2) * cube),
				   (pSrc + j2 * cube + i2 * _width * cube), cube);

	cinfo.image_height = _width;
	cinfo.image_width = _height;
	p_in_buf = (char *)in_buf_rotation;

	cinfo.input_components = param->bytes_per_pixel;
	switch (param->in_format)
	{
	case SW_RGBX:
		cinfo.in_color_space = JCS_EXT_RGBX;
		break;
	case SW_BGRX:
		cinfo.in_color_space = JCS_EXT_BGRX;
		break;
	case SW_XRGB:
		cinfo.in_color_space = JCS_EXT_XRGB;
		break;
	case SW_XBGR:
		cinfo.in_color_space = JCS_EXT_XBGR;
		break;
	default:
		LOGE("unsupport color format param->in_format = %d\n", param->in_format);
		cinfo.in_color_space = JCS_UNKNOWN;
	}
	if (started == 0)
	jpeg_set_defaults(&cinfo);

	LOG_D("cinfo.in_color_space = %d\n", cinfo.in_color_space);
	if (started == 0)
	jpeg_default_colorspace(&cinfo);
	if (started == 0)
	jpeg_set_quality(&cinfo, is_scaled ? 80 : 75, TRUE);
	if (started == 0)
	jpeg_mem_dest((j_compress_ptr)&cinfo, (unsigned char **)&pdest, &dest_size);  // Data written to mem

	jpeg_start_compress(&cinfo, TRUE);
	rowPointer[0] = is_scaled ? (JSAMPROW)out_buf : (JSAMPROW)p_in_buf;

	LOGD("O o O o O o O o O o O o O o");
	LOGD("cinfo.image_height = %d, cinfo.image_width = %d\n",
			cinfo.image_height, cinfo.image_width);
	//stamp = _get_system_mstime();
	int count = 0;
	for (i = 0; i < cinfo.image_height; ++i)
	{
		++count;
		if (is_scaled)
		{
			LOG_D("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");
			LOG_D("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");
			LOG_D("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");
			LOG_D("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");
			LOG_D("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");
			LOG_D("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");
			rowPointer[0] = (out_buf + cinfo.input_components * i * cinfo.image_width);
		}
		else
		{
			//LOG_D("%d,%d, --", __LINE__, count);
			if (p_in_buf == NULL)
				LOG_D("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\n");
			rowPointer[0] = (JSAMPROW)((char *)p_in_buf + cinfo.input_components * i * cinfo.image_width);
		}
			jpeg_write_scanlines(&cinfo, rowPointer, 1);
	}
	//stamp2 = _get_system_mstime();
	//LOG_D("\t\t sw encode time: %lldms, jpeg size is %d\n", stamp2 - stamp, dest_size);

	if (started == 0)
	jpeg_finish_compress(&cinfo);
	if (started2 == 0)
	//jpeg_destroy_compress(&cinfo);

	param->out_buf = pdest;
	param->outdata_size = dest_size;
	out_buf_size = dest_size;
	printf("param->out_buf = %X, param->outdata_size = %d\n\n",
		  (unsigned int)param->out_buf, param->outdata_size);

	static int firstIn = 0;
	if(firstIn == 0)
	{
		firstIn = 1;
		FILE *pfile = fopen("/data/jpeg/out.jpg", "w");
		if (pfile == NULL)
		{
			LOG_D("open file error!\n");
			goto AAAA;
		}
		fwrite(pdest, 1, dest_size, pfile);
		fclose(pfile);
	}
AAAA:
	if(!out_buf_size)
	{
		LOGE("Out size is 0!\n");
		return -1;
	}

	if (started == 0)
		started = 0;
	if (started2 == 0)
		started2 = 0;

	return 0;
}

void rotate_scale(char *dst, char *src, unsigned int dst_width,
		unsigned int dst_height, unsigned int src_width, unsigned int src_height)
{
    unsigned int x, y;
    unsigned int x_float_16 = (src_height << 16) / dst_width + 1;
    unsigned int y_float_16 = (src_width << 16) / dst_height + 1;
    unsigned int srcx_16 = 0;
    unsigned int src_width_adjust = src_width << 2;
    for(x = 0; x < dst_width; x++)
    {
        char *psrc_line = src + src_width_adjust * (srcx_16 >> 16);
        unsigned int srcy_16 = 0;
        for(y = 0; y < dst_height; y++)
        {
            int index = (dst_height - y - 1) * dst_width + x;
            ((int *)dst)[index] = ((int *)psrc_line)[srcy_16 >> 16];
            srcy_16 += y_float_16;
        }
        srcx_16 += x_float_16;
    }
}

void rgb_scale(char *dst, char *src, unsigned int dst_width, unsigned
		int dst_height, unsigned int src_width, unsigned int src_height)
{
    unsigned int x, y;
    unsigned int x_float_16 = (src_width << 16) / dst_width + 1;
    unsigned int y_float_16 = (src_height << 16) / dst_height + 1;
    unsigned int srcy_16 = 0;
    unsigned int src_width_adjust = src_width << 2;
    for(y = 0; y < dst_height; y++)
    {
        char *psrc_line = src + src_width_adjust * (srcy_16 >> 16);
        unsigned int srcx_16 = 0;
        for(x = 0; x < dst_width; x++)
        {
            int index = y * dst_width + x;
            ((int *)dst)[index] = ((int *)psrc_line)[srcx_16 >> 16];
            srcx_16 += x_float_16;
        }
        srcy_16 += y_float_16;
    }
}
