#include "jpeg_enc.h"
#include <unistd.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/resource.h>
#undef LOG_TAG
#define LOG_TAG	"HW_JPEG"

const char platform_type_to_string[4][20] = {
	"QUALCOMM"	,
	"SAMSUNG"	,
	"MEDIATEK"	,
	"UNKNOWN"	,
};

const char preference_type_to_string[6][50] = {
	"JPEG_ENCODER_HW_ACCELERATED_PREFERRED"   ,
	"JPEG_ENCODER_HW_ACCELERATED_ONLY"        ,
	"JPEG_ENCODER_SOFTWARE_PREFERRED"         ,
	"JPEG_ENCODER_SOFTWARE_ONLY"              ,
	"JPEG_ENCODER_DONT_CARE"                  ,
	"JPEG_ENCODER_MAX"                     	  ,
};

const char in_format_to_string[3][30] = {
	"HW_JPG_MODESEL_YCBCR"	,
	"HW_JPG_MODESEL_RGB"	,
	"HW_JPG_MODESEL_UNKNOWN",
};

const char out_format_to_string[8][30] = {
	"HW_JPG_444"	,
	"HW_JPG_422"	,
	"HW_JPG_420"	,
	"HW_JPG_400"	,
	"HW_RESERVED1"	,
	"HW_RESERVED2"	,
	"HW_JPG_411"	,
	"HW_JPG_SAMPLE_UNKNOWN",
};

static void *handler = NULL;
static int (*hw_encode_init_func)(uint32_t, uint32_t, uint32_t) = NULL;
static int (*hw_encode_func)(struct hw_enc_param *) = NULL;
static int (*hw_encode_alloc_memory_func)(uint32_t, uint32_t, uint32_t) = NULL;
static struct hw_enc_param g_param;

extern int sw_encode (struct hw_enc_param *param);
extern int sw_encode_init (uint32_t width, uint32_t height, uint32_t bytes_per_pixel);
extern void sw_encode_deinit (void);

void param_dump(struct hw_enc_param *param)
{
	LOGD("-------------Param Dump------------\n");
	switch(param->platform_type)
	{
		case QUALCOMM_PLAT:
				LOGD("param->platform_type = %s\n", platform_type_to_string[param->platform_type]);
				LOGD("param->preference = %s\n", preference_type_to_string[param->preference]);
				break;
		case SAMSUNG_PLAT:
				LOGD("param->platform_type = %s\n", platform_type_to_string[param->platform_type]);
				LOGD("param->in_format = %s\n", in_format_to_string[param->in_format - 1]);
				LOGD("param->out_format = %s\n",out_format_to_string[param->out_format]);
				break;
		default:
				break;
	}
	LOGD("param->height = %d\n", param->height);
	LOGD("param->width = %d\n", param->width);
	LOGD("param->indata_size = %d\n", param->indata_size);
	LOGD("param->quality = %d\n", param->quality);
	LOGD("param->in_buf = 0x%08x\n", (int)param->in_buf);
	LOGD("param->flip_offset = %d\n", param->flip_offset);
	LOGD("param->bytes_per_pixel = %d\n", param->bytes_per_pixel);
	LOGD("param->slice_flag = %d\n", param->slice_flag);
	LOGD("param->slice_num = %d\n", param->slice_num);
	LOGD("-----------  DONE  ------------\n");
}

static void param_init(struct display_info *di)
{
	int i = 0;

#ifndef di_width
#define di_width(di)  ((di)->vi.xres)
#define di_height(di) ((di)->vi.yres)
#define di_bpp(di)    ((di)->vi.bits_per_pixel>>3)
#endif
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

	int r = di->vi.red.offset;
	//int g = di->vi.green.offset;
	//int b = di->vi.blue.offset;
	int x = di->vi.transp.offset;
	LOG_D("R = %d, X = %d\n", r, x);

	if (x == 0)
	{
		if (r == 24)
			g_param.in_format = SW_RGBX;
		else
			g_param.in_format = SW_BGRX;
	}
	else
	{
		if (r == 16)
			g_param.in_format = SW_BGRX; //for samsung SIII and S plus
		else
			g_param.in_format = SW_RGBX;
	}

	//for HTC G12
	if (x == 0 && r == 0)
		g_param.in_format = SW_RGBX;

	LOG_D("g_param.in_format = %d\n", g_param.in_format);
	g_param.platform_type	= di->machine;
	g_param.height 		= di->vi.yres;
	g_param.width 		= di->vi.xres;
	g_param.indata_size 	= di_size(di);
	g_param.quality 	= di->quality;
	g_param.slice_flag	= 0;
	g_param.slice_num	= 1;

	g_param.in_buf 		= di->frame_base ;
	g_param.out_buf 	= NULL;
	g_param.in_offset 	= 0;
	g_param.flip_offset 	= di_offset(di);
	g_param.bytes_per_pixel = di_bpp(di);

	for(i=0; i<4; i++)
		g_param.slice_size[i] = 0;

}

int check_qualcomm_version(void)
{
	int ret = 0;
	void *function;
	void *mmjpeg_lib = dlopen("libmmjpeg.so", RTLD_NOW);
	if(!mmjpeg_lib)
	{
		LOGE("%s error\n", __func__);
		return -1;
	}
	dlerror();
	function = dlsym(mmjpeg_lib, "jpege_enqueue_output_buffer");
	if(dlerror() != NULL)
		ret = 1; //version 1
	else
		ret = 2; //version 2
	LOGE("%s = %d\n", __func__, ret);
	return ret;
}

int platform_support_check(int type)
{
	switch(type)
	{
		case QUALCOMM_PLAT :
			if(!access(SYSTEM_QUALCOMM_LIB, F_OK))
				return RETURN_SUCCESS;
			else
				return RETURN_FAILURE;
		case SAMSUNG_PLAT  :
			if(!access(SYSTEM_SAMSUNG_LIB, F_OK))
				return RETURN_SUCCESS;
			else
				return RETURN_FAILURE;
		default:
			return RETURN_FAILURE;
	}
}

int sw_jpeg_init(struct display_info *di)
{

	param_init(di);
	return sw_encode_init(di->vi.xres, di->vi.yres, di_bpp(di));
}

int hw_jpeg_init(struct display_info *di)
{
	int ret = 0;
	LOGD("%s func start!\n", __func__);

	int prio_process;
	pid_t pid;

	pid = getpid();
	ret = setpriority(PRIO_PROCESS, pid, NICE_PRIORITY);
	if(-1 == ret)
		LOGD("Set Process (%d) Priority Error!\n", pid);

	prio_process = getpriority(PRIO_PROCESS, pid);
	LOGD("Process (%d) Priority now is %d.\n", pid, prio_process);

	param_init(di);

	ret = platform_support_check(g_param.platform_type);
	if(ret)
	{
		LOGE("Platform not support!\n");
		return RETURN_UNSUPPORT;
	}

	switch(g_param.platform_type)
	{
		case QUALCOMM_PLAT :
			ret = check_qualcomm_version();
			if(ret == 1)
				handler = dlopen(QUALCOMM_JPEG_LIB, RTLD_NOW);
			else if(ret == 2)
				handler = dlopen(QUALCOMM_JPEG_LIB2, RTLD_NOW);
			else
				return RETURN_FAILURE;
			break;
		case SAMSUNG_PLAT :
			handler = dlopen(SAMSUNG_JPEG_LIB, RTLD_NOW);
			break;
		default:
			LOGD("%s is not supported now!\n", platform_type_to_string[g_param.platform_type]);
			return RETURN_FAILURE;
	}

	if (!handler)
	{
		LOGD("dlopen %s library failed!\n", dlerror());
		return RETURN_FAILURE;
	}

	hw_encode_init_func = dlsym(handler, "hw_encode_init");
	if(!hw_encode_init_func){
		LOGD("dlsym hw_encode_init function error in %d line!\n",__LINE__);
		return RETURN_FAILURE;
	}

	ret = hw_encode_init_func(di->vi.xres, di->vi.yres, di_bpp(di));
	if(ret < 0){
		LOGD("hw_code_init failed!");
		return RETURN_FAILURE;
	}

	hw_encode_func = dlsym(handler, "hw_encode");
	if(!hw_encode_func){
		LOGD("dlsym hw_encode function error in %d line!\n",__LINE__);
		return RETURN_FAILURE;
	}

	ret = hw_encode_func(&g_param);
	if(g_param.platform_type == QUALCOMM_PLAT)
	{
		g_param.quality = QUALITY_LEVEL1;//leve2 --> level1 wenhuan
		if (ret){
			g_param.preference = JPEG_ENCODER_SOFTWARE_ONLY;
			LOGE("Hardware encoder engine is not supported!\n");

			ret = hw_encode_func(&g_param);
			if(ret){
				LOGE("platform not support!\n");
				return  RETURN_UNSUPPORT;
			}
			hw_jpeged();
		}else{
			LOGE("Hardware encoder engine is supported!\n");
			hw_jpeged();   //clear buffer and buffer size
		}
	}
	else if(g_param.platform_type == SAMSUNG_PLAT)
	{
		g_param.quality = QUALITY_LEVEL1;
		g_param.in_format = HW_JPG_MODESEL_RGB;
		g_param.out_format = HW_JPG_422;
		hw_jpeged();   //release mmap memory /dev/s3c-jpeg

		if(ret)
		{
			g_param.slice_flag = 1;
			LOGD("slice_flag is %d, slice frame into %d part!\n", g_param.slice_flag, g_param.slice_num);
		}
	}

	if(1 == g_param.slice_flag)
	{
		hw_encode_alloc_memory_func = dlsym(handler, "hw_encode_alloc_memory");
		if(!hw_encode_alloc_memory_func){
			LOGD("dlsym hw_encode_alloc_memory function error in %d line!\n",__LINE__);
			return RETURN_FAILURE;
		}
		if(-1 == hw_encode_alloc_memory_func(g_param.width, g_param.height, g_param.bytes_per_pixel))
		{
			LOGE("hw_encode_alloc_memory failed!\n");
			return RETURN_FAILURE;
		}
	}

	param_dump(&g_param);
	LOGD("%s func end!\n", __func__);

	return RETURN_SUCCESS;
}

int sw_jpeg(struct display_info *di)
{
	int ret;
	int i = 0;

	LOGD("IN [%s:%d]\n", __func__, __LINE__);

	g_param.angle = di->angle;
	ret = sw_encode(&g_param);

	if (ret)
	{
		LOGE("hw_encode failed! ret = %d\n", ret);
		return -1;
	}
	di->jpeg_base 	= 	g_param.out_buf;
	di->jpeg_size 	= 	g_param.outdata_size;
	di->slice_flag  =	g_param.slice_flag;
	di->slice_num   =	g_param.slice_num;
	for(i = 0; i < di->slice_num; i++)
	{
		di->slice_size[i] = g_param.slice_size[i];
	}

	LOGD("OUT [%s:%d]\n", __func__, __LINE__);
	return 0;
}

int hw_jpeg(struct display_info *di)
{
	int ret;
	int i = 0;

	LOGD("IN [%s:%d]\n", __func__, __LINE__);

//	param_dump(&param);

	ret = hw_encode_func(&g_param);

	if (ret)
	{
		LOGE("hw_encode failed! ret = %d\n", ret);
		return -1;
	}
	di->jpeg_base 	= 	g_param.out_buf;
	di->jpeg_size 	= 	g_param.outdata_size;
	di->slice_flag  =	g_param.slice_flag;
	di->slice_num   =	g_param.slice_num;
	for(i = 0; i < di->slice_num; i++)
	{
		di->slice_size[i] = g_param.slice_size[i];
	}

	LOGD("OUT [%s:%d]\n", __func__, __LINE__);
	return 0;
}

void hw_jpeged(void)
{
	void (*hw_encoded_func)(void);

	hw_encoded_func	= dlsym(handler, "hw_encoded");
	if(!hw_encode_func){
		LOGD("dlsym hw_encode_func function error in %d line!\n",__LINE__);
		return ;
	}
	hw_encoded_func();

	return ;

}

static void memory_free(void)
{
	void (*free_memory_func)(void);

	if(!handler)
	{
		LOGE("Handler is NULL! Who close it??\n");
		return;
	}

	free_memory_func = dlsym(handler, "free_memory");
	if(!free_memory_func){
		LOGD("dlsym free_memory function error in %d line!\n",__LINE__);
		return ;
	}
	free_memory_func();

	return;
}

void sw_jpeg_deinit(void)
{
	LOGD("%s func at %d line\n", __func__, __LINE__);
	memory_free();
	//sw_jpeg_deinit();
	return ;
}

void hw_jpeg_deinit(void)
{
	LOGD("%s func at %d line\n", __func__, __LINE__);
	memory_free();
	if(handler)
		dlclose(handler);
	return ;
}
