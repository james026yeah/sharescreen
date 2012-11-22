#include "hw_jpeg.h"
#undef LOG_TAG
#define LOG_TAG "HW_ENCODE"

using namespace android;

JpegEncoder *jpgEnc;

#define RGB16(red, green, blue) ( ((red >> 3) << 11) | ((green >> 2) << 5) | (blue >> 3))
#define MAX_JPEG_SAVE_BUF_SIZE			1024*1024   //1MB

unsigned char *g_buffer = NULL;
unsigned char *g_save_buffer = NULL;

static void rgb32_to_rgb565(unsigned char  *dst,   unsigned char *src, int src_len)
{
	int i = 0;
	unsigned short *val = (unsigned short *)dst;

	for (i = 0; i < src_len; i += 4) {  
		*val = RGB16(src[i+2], src[i+1], src[i]);
		val++;
	}  
}

void free_memory(void)
{
	free(g_buffer);
	g_buffer = NULL;

	free(g_save_buffer);
	g_save_buffer = NULL;
	LOGD("%s successfully!\n", __func__);
}

int alloc_memory (uint8_t ** buffer, uint32_t buffer_len)
{
	*buffer = (uint8_t *) malloc (sizeof (uint8_t) * buffer_len);
	memset (*buffer, 0, buffer_len);

	if (NULL == *buffer)
	{   
		LOG_D ("Malloc memory error!\n");
		return -1; 
	}   

	return 0;

}

int hw_encode_alloc_memory (uint32_t width, uint32_t height, uint32_t bytes_per_pixel)
{
	int size = width * height * bytes_per_pixel;

	if (-1 == alloc_memory (&g_buffer, size))
	{   
		LOG_D ("Malloc memory error!\n");
		return -1; 
	}   
	if (-1 == alloc_memory (&g_save_buffer, MAX_JPEG_SAVE_BUF_SIZE))
	{   
		LOG_D ("Malloc memory error!\n");
		return -1; 
	}   

	return 0;
}

int hw_encode_init(uint32_t width, uint32_t height, uint32_t size)
{
	return 0;
}

int hw_encode(struct hw_enc_param *param)
{
	unsigned char *in_buf = NULL;
	unsigned char *out_buf = NULL;
	unsigned char *dst_buffer = NULL;
	unsigned int  real_size = 0;
	unsigned int  size = 0;
	unsigned int  slice_offset = 0;
	unsigned int  dst_buf_size = 0;
	int i = 0, j = 0;
	uint64_t outbuf_size;
	image_quality_type_t jpeg_quality;
	jpgEnc = new JpegEncoder();

	LOG_TIME_DEFINE(0);
	LOG_TIME_DEFINE(1);
	LOG_TIME_DEFINE(2);
	LOG_TIME_DEFINE(3);
	LOG_TIME_DEFINE(4);

	LOG_TIME_START(0);


	if (jpgEnc->setConfig(JPEG_SET_ENCODE_IN_FORMAT,
			      param->in_format) != JPG_SUCCESS)
		LOGE("[JPEG_SET_ENCODE_IN_FORMAT] Error\n");

	if (jpgEnc->setConfig(JPEG_SET_SAMPING_MODE,
			      param->out_format) != JPG_SUCCESS)
		LOGE("[JPEG_SET_SAMPING_MODE] Error\n");

	if (param->quality >= 90)
		jpeg_quality = JPG_QUALITY_LEVEL_1;
	else if (param->quality >= 80)
		jpeg_quality = JPG_QUALITY_LEVEL_2;
	else if (param->quality >= 70)
		jpeg_quality = JPG_QUALITY_LEVEL_3;
	else
		jpeg_quality = JPG_QUALITY_LEVEL_4;

	if (jpgEnc->setConfig(JPEG_SET_ENCODE_QUALITY,jpeg_quality) != JPG_SUCCESS)
		LOGE("[JPEG_SET_ENCODE_QUALITY] Error\n");

	if (jpgEnc->setConfig(JPEG_SET_ENCODE_WIDTH,param->width) != JPG_SUCCESS)
		LOGE("[JPEG_SET_ENCODE_WIDTH] Error\n");

	if(1 == param->slice_flag)
	{

		if (jpgEnc->setConfig(JPEG_SET_ENCODE_HEIGHT,param->height/param->slice_num) != JPG_SUCCESS)
			LOGE("[JPEG_SET_ENCODE_HEIGHT] Error\n");

		size = (param->width) * (param->height);  
		in_buf = (unsigned char *) jpgEnc->getInBuf(size);
	}
	else
	{
		if (jpgEnc->setConfig(JPEG_SET_ENCODE_HEIGHT,param->height) != JPG_SUCCESS)
		{
			LOGE("[JPEG_SET_ENCODE_HEIGHT] Over Max Height!\n");

			param->slice_num = (param->height)/(MAX_JPG_HEIGHT) + 1;    //We assume width max support 800, in other words, slice on height only.			
//			param->slice_num = 4;
			LOGD("[HW_ENCODE]: Need slice frame\n");
		}

		in_buf = (unsigned char *) jpgEnc->getInBuf(param->indata_size);
	}
		
	if(in_buf == NULL)	
	{
		LOGE("JPEG input buffer is NULL!!\n");
		return -1;
	}

	if(1 == param->slice_flag)
	{
		real_size = param->width * param->height * param->bytes_per_pixel;  //rgba bytes_per_pixel = 4  current frame real size
		dst_buf_size =  real_size/param->slice_num;     //rgb 565 bytes_per_pixel = 2

		dst_buffer = (unsigned char *)malloc(sizeof(char) * dst_buf_size);

		if(!dst_buffer)
		{
			LOGE("There is no any more memory to malloc!\n");
			return -1;
		}
		LOG_TIME_START(1);
		memcpy(g_buffer, param->in_buf, real_size);    //store current frame
		LOG_TIME_END(1);
		LOG_D("hw_encode_engine memcpy framebuffer time : %lu ms(%lu us)\n", LOG_TIME(1)/1000, LOG_TIME(1));

		LOG_TIME_START(2);
		rgb32_to_rgb565(dst_buffer, g_buffer, real_size);
		LOG_TIME_END(2);
		LOG_D("hw_encode_engine rgb32_to_rgb565 framebuffer time : %lu ms(%lu us)\n",LOG_TIME(2)/1000, LOG_TIME(2));

		for(i=0; i<param->slice_num; i++)
		{
			slice_offset = 0;
			#if 0    //move above
			LOG_TIME_START(2);
			rgb32_to_rgb565(dst_buffer ,g_buffer + i * (real_size/param->slice_num), real_size/param->slice_num);
			LOG_TIME_END(2);
			LOG_D("hw_encode_engine rgb32_to_rgb565 slice(%d) time : %lu ms(%lu us)\n", i + 1, LOG_TIME(2)/1000, LOG_TIME(2));
			#endif
			LOG_TIME_START(3);
			memcpy(in_buf, dst_buffer + i * (dst_buf_size/param->slice_num), dst_buf_size/param->slice_num);
			LOG_TIME_END(3);
			LOG_D("hw_encode_engine memcpy slice(%d) in_buf time : %lu ms(%lu us)\n", i + 1,LOG_TIME(3)/1000, LOG_TIME(3));

			LOG_TIME_START(4);
			if (jpgEnc->encode(&param->outdata_size, NULL) != JPG_SUCCESS)
				LOGE("JPEG encode failed!\n");
			LOG_TIME_END(4);
			LOG_D("hw_encode_engine slice(%d) encode time : %lu ms(%lu us)\n", i + 1, LOG_TIME(4)/1000, LOG_TIME(4));

			out_buf = (unsigned char *) jpgEnc->getOutBuf(&outbuf_size);

			for(j = 0; j < i; j++)
				slice_offset += param->slice_size[j];      //first slice frame offset = 0;

			param->slice_size[i] = outbuf_size;

			memcpy(g_save_buffer + slice_offset, out_buf, outbuf_size);

			if (out_buf == NULL)
			{
				LOGE("JPEG output buffer is NULL!!\n");
				return -1;
			}
		}

		param->out_buf = g_save_buffer;
		for(i=0; i<param->slice_num; i++)
		{
			param->outdata_size += param->slice_size[i];
		}
		free(dst_buffer);
	}

	LOG_TIME_END(0);
	LOG_D("hw_encode_engine total time : %lu ms(%lu us)\n", LOG_TIME(0)/1000, LOG_TIME(0));
	LOG_D("JPEG OUT SIZE = %u\n", param->outdata_size);


	return 0;
}

void hw_encoded(void)
{
	delete(jpgEnc);
	return;
}
