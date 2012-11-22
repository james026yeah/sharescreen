#include "hw_encode.h"

#define DEBUG_TIMER			1
#define MAX_BUFFER_SIZE 	1024*1024	//1Mb  Accoring to jpeg quality
#define CEILING16(X) (((X) + 0x000F) & 0xFFF0)
#undef LOG_TAG
#define LOG_TAG "HW_ENCODE"

const char event_to_string[4][30] = {
	"EVENT_DONE",
	"EVENT_WARNING",
	"EVENT_ERROR",
	"EVENT_THUMBNAIL_DROPPED",
};

typedef struct
{
	char *file_name;
	uint8_t *y_buf;
	uint8_t *uv_buf;
	uint32_t width;
	uint32_t height;
	uint32_t quality;
	jpeg_color_format_t format;

} input_image_args_t;

typedef struct
{
	input_image_args_t main;
	input_image_args_t thumbnail;
	char *output_file;
	int16_t rotation;
	uint32_t preference;
	uint8_t encode_thumbnail;
	uint16_t back_to_back_count;
	uint32_t target_filesize;
	uint32_t abort_time;
	const char *reference_file;
	uint8_t output_nowrite;
	uint8_t use_pmem;
	jpege_scale_cfg_t main_scale_cfg;
	jpege_scale_cfg_t tn_scale_cfg;

} encoder_args_t;

typedef struct
{
	FILE *fout;
	os_mutex_t mutex;
	os_cond_t cond;
	uint32_t size;
	uint8_t nowrite;

} output_handler_args_t;

typedef struct
{
	int tid;
	os_thread_t thread;
	jpege_obj_t encoder;
	uint8_t encoding;
	output_handler_args_t output_handler_args;
	encoder_args_t *p_args;

} thread_ctrl_blk_t;

typedef struct
{
	uint32_t height;
	uint32_t width;
	jpege_preference_t preference;
	uint32_t quality;


} yuv_args_t;


int read_bytes_from_file (const char *file, int width, int height,
			  jpeg_buffer_t * p_luma_buf,
			  jpeg_buffer_t * p_chroma_buf, uint8_t use_pmem,
			  encoder_args_t * p_args, uint8_t is_thumbnail);

int read_bytes_from_buffer (int width, int height,
			    jpeg_buffer_t * p_luma_buf,
			    jpeg_buffer_t * p_chroma_buf, uint8_t use_pmem,
			    encoder_args_t * p_args, uint8_t is_thumbnail);

int read_bs_from_file (const char *file, int width, int height,
		       jpeg_buffer_t * p_luma_buf, uint8_t use_pmem);

OS_THREAD_FUNC_RET_T OS_THREAD_FUNC_MODIFIER	hw_engine_encode (OS_THREAD_FUNC_ARG_T arg);

void encoder_event_handler (void *p_user_data,
			    jpeg_event_t event, void *p_arg);

int encoder_output_handler (void *p_user_data,
			     void *p_arg, jpeg_buffer_t buffer, uint8_t last_buf_flag);

int jpeg_encode (uint8_t * y, uint8_t * uv, yuv_args_t * _yuv_args);

// Global variables
int concurrent_cnt = 1;
uint32_t out_buf_size = 0;
uint8_t *out_buf_ptr = NULL;
uint8_t *in_buf = NULL;
uint8_t *g_y = NULL;
uint8_t *g_uv = NULL;
thread_ctrl_blk_t *thread_ctrl_blks = NULL;


int jpeg_encode (uint8_t * Y, uint8_t * UV, yuv_args_t * _yuv_args)
{
	int rc, i;
	encoder_args_t encoder_args;

	memset (&encoder_args, 0, sizeof (encoder_args));

	LOG_D ("=============================================================\n");
	LOG_D ("Encoder start\n");
	LOG_D ("=============================================================\n");

	encoder_args.main.y_buf = Y;
	encoder_args.main.uv_buf = UV;
	encoder_args.main.quality = _yuv_args->quality;
//      encoder_args.thumbnail.quality = 50;
	encoder_args.main.width = _yuv_args->width;
//      encoder_args.thumbnail.width = 0;
	encoder_args.main.height = _yuv_args->height;
//      encoder_args.thumbnail.height = 0;
	encoder_args.rotation = 0;
//      encoder_args.encode_thumbnail = true;
	encoder_args.main.format = YCBCRLP_H2V2;
//      encoder_args.thumbnail.format = YCBCRLP_H2V2;
	encoder_args.preference = _yuv_args->preference;
	encoder_args.back_to_back_count = 1;
#if 0
	encoder_args.main_scale_cfg.enable = false;
	encoder_args.main_scale_cfg.input_width = 0;
	encoder_args.main_scale_cfg.input_height = 0;
	encoder_args.main_scale_cfg.output_width = 0;
	encoder_args.main_scale_cfg.output_height = 0;
	encoder_args.main_scale_cfg.h_offset = 0;
	encoder_args.main_scale_cfg.v_offset = 0;
	encoder_args.tn_scale_cfg.enable = false;
	encoder_args.tn_scale_cfg.input_width = 0;
	encoder_args.tn_scale_cfg.input_height = 0;
	encoder_args.tn_scale_cfg.output_width = 0;
	encoder_args.tn_scale_cfg.output_height = 0;
	encoder_args.tn_scale_cfg.h_offset = 0;
	encoder_args.tn_scale_cfg.v_offset = 0;
	encoder_args.target_filesize = 0;
#endif
	encoder_args.abort_time = 0;
	encoder_args.use_pmem = false;

	// Double check all the required arguments are set
#if 0
	if (!encoder_args.main.file_name || !encoder_args.output_file ||
	    !encoder_args.main.width ||
	    !encoder_args.main.height || encoder_args.main.format == 8)
	{
		LOG_D ("Missing required arguments.\n");
		return 1;
	}

	if (encoder_args.encode_thumbnail &&
	    (!encoder_args.thumbnail.file_name ||
	     !encoder_args.thumbnail.width ||
	     !encoder_args.thumbnail.height ||
	     encoder_args.thumbnail.format == 8))
	{
		LOG_D ("Missing thumbnail arguments.\n");
		return 1;
	}
#endif
	// Create thread control blocks


	thread_ctrl_blks = (thread_ctrl_blk_t *) malloc (concurrent_cnt * sizeof (thread_ctrl_blk_t));
	if (!thread_ctrl_blks)
	{
		LOG_D ("hw_engine_encode failed: insufficient memory in creating thread control blocks\n");
		return 1;
	}
	memset (thread_ctrl_blks, 0, concurrent_cnt * sizeof (thread_ctrl_blk_t));
	// Initialize the blocks and kick off the threads
	for (i = 0; i < concurrent_cnt; i++)
	{
		thread_ctrl_blks[i].tid = i;
		thread_ctrl_blks[i].p_args = &encoder_args;
		os_mutex_init (&thread_ctrl_blks[i].output_handler_args.mutex);
		os_cond_init (&thread_ctrl_blks[i].output_handler_args.cond);
		if (os_thread_create(&thread_ctrl_blks[i].thread, hw_engine_encode,&thread_ctrl_blks[i]))
		{
			LOG_D ("hw_engine_encode: os_create failed\n");
			return 1;
		}
	}

	rc = 0;
	// Join the threads
	for (i = 0; i < concurrent_cnt; i++)
	{
		OS_THREAD_FUNC_RET_T ret;
		os_thread_join (&thread_ctrl_blks[i].thread, &ret);
		if (ret)
		{
			LOG_D ("hw_engine_encode: thread %d failed\n", i);
			rc = (int) OS_THREAD_FUNC_RET_FAILED;
		}
	}

	free (thread_ctrl_blks);

	if (!rc)
		LOG_D ("hw_engine_encode finished successfully\n");

	LOG_D ("exit value: %d\n", rc);
	return rc;
}

OS_THREAD_FUNC_RET_T OS_THREAD_FUNC_MODIFIER hw_engine_encode (OS_THREAD_FUNC_ARG_T arg)
{
//	char *output_filename;
	thread_ctrl_blk_t *p_thread_arg = (thread_ctrl_blk_t *) arg;
	int rc, i;
	jpege_obj_t encoder;
	jpege_src_t source;
	jpege_dst_t dest;
	jpege_cfg_t config;
	jpege_img_data_t img_info;	//tn_img_info;
	jpeg_buffer_t main_luma_buf, main_chroma_buf;
	jpeg_buffer_t tn_luma_buf, tn_chroma_buf;
	encoder_args_t *p_args = p_thread_arg->p_args;
	uint8_t use_pmem = false;
	jpeg_buffer_t buffers[2];
	// Append the output file name with a number to avoid multiple writing to the same file
#if 0
	if (p_thread_arg->tid)
	{
		// Look for the last occurence of '/' and then last occurence of '.'
		char *s = strrchr (p_args->output_file, '/');
		if (s)
		{
			s = strrchr (s, '.');
		}
		else
		{
			s = strrchr (p_args->output_file, '.');
		}
		output_filename =
			(char *) malloc (5 + strlen (p_args->output_file));
		sLOG_D (output_filename, "%s", p_args->output_file);
		if (s)
		{
			sLOG_D (output_filename +
				(s - p_args->output_file), "_%.2d%s",
				p_thread_arg->tid, s);
		}
		else
		{
			sLOG_D (output_filename, "%s_%.2d",
				output_filename, p_thread_arg->tid);
		}
	}
#endif
	// Determine whether pmem should be used (useful for pc environment testing where
	// pmem is not available)
	use_pmem = p_args->use_pmem;
	if ((jpege_preference_t) p_args->preference == JPEG_ENCODER_PREF_SOFTWARE_PREFERRED
	    || (jpege_preference_t) p_args->preference == JPEG_ENCODER_PREF_SOFTWARE_ONLY)
	{
		use_pmem = false;
	}

	// Initialize source buffers
	if (jpeg_buffer_init (&main_luma_buf) ||
	    jpeg_buffer_init (&main_chroma_buf) ||
	    jpeg_buffer_init (&tn_luma_buf) ||
	    jpeg_buffer_init (&tn_chroma_buf))
	{
		return OS_THREAD_FUNC_RET_FAILED;
	}

	// Open input file(s) and read the contents
	if (p_args->main.format == YCBCRLP_H2V2
	    || p_args->main.format == YCRCBLP_H2V2)
	{
		if (read_bytes_from_buffer (p_args->main.width,
					    p_args->main.height,
					    &main_luma_buf,
					    &main_chroma_buf, use_pmem,
					    p_args, false))
			return OS_THREAD_FUNC_RET_FAILED;
	}
#if 0
	else if ((p_args->main.format >= JPEG_BITSTREAM_H2V2) &&
		 (p_args->main.format < JPEG_COLOR_FORMAT_MAX))
	{
		if (read_bs_from_file
		    (p_args->main.file_name, p_args->main.width,
		     p_args->main.height, &main_luma_buf, use_pmem))
			return OS_THREAD_FUNC_RET_FAILED;
	}
#endif
	else
	{
		LOG_D ("hw_engine_encode: main inage color format not supported\n");
		return OS_THREAD_FUNC_RET_FAILED;
	}

#if 0
	if (p_args->encode_thumbnail)
	{
		if (p_args->thumbnail.format <= YCBCRLP_H1V1)
		{
			if (read_bytes_from_buffer
			    (p_args->thumbnail.width,
			     p_args->thumbnail.height, &tn_luma_buf,
			     &tn_chroma_buf, use_pmem, p_args, true))
				return OS_THREAD_FUNC_RET_FAILED;
		}
		else if ((p_args->thumbnail.format >= JPEG_BITSTREAM_H2V2)
			 && (p_args->main.format < JPEG_COLOR_FORMAT_MAX))
		{
			if (read_bs_from_file
			    (p_args->thumbnail.file_name,
			     p_args->thumbnail.width,
			     p_args->thumbnail.height, &tn_luma_buf,
			     use_pmem))
				return OS_THREAD_FUNC_RET_FAILED;
		}
		else
		{
			LOG_D ("hw_engine_encode: thumbnail color format not supported\n");
			return OS_THREAD_FUNC_RET_FAILED;
		}
	}
#endif
	// Initialize encoder
	rc = jpege_init (&encoder, &encoder_event_handler,(void *) p_thread_arg);
	if (JPEG_FAILED (rc))
	{
		LOG_D ("hw_engine_encode: jpege_init failed\n");
		return OS_THREAD_FUNC_RET_FAILED;
	}
	p_thread_arg->encoder = encoder;

	// Set source information (main)
	img_info.color_format = p_args->main.format;
	img_info.width = p_args->main.width;
	img_info.height = p_args->main.height;
	img_info.fragment_cnt = 1;
	img_info.p_fragments[0].width = p_args->main.width;
	img_info.p_fragments[0].height = p_args->main.height;
//    	if (img_info.color_format <= YCBCRLP_H1V1)
	{
		img_info.p_fragments[0].color.yuv.luma_buf = main_luma_buf;
		img_info.p_fragments[0].color.yuv.chroma_buf =
			main_chroma_buf;
	}
#if 0
	else if ((img_info.color_format >= JPEG_BITSTREAM_H2V2) &&
	    (img_info.color_format < JPEG_COLOR_FORMAT_MAX))
	{
		img_info.p_fragments[0].color.bitstream.bitstream_buf =
			main_luma_buf;
	}
#endif
	source.p_main = &img_info;
	source.p_thumbnail = NULL;
#if 0
	// Set source information (thumbnail)
	if (p_args->encode_thumbnail)
	{
		tn_img_info.color_format = p_args->thumbnail.format;
		tn_img_info.width = p_args->thumbnail.width;
		tn_img_info.height = p_args->thumbnail.height;
		tn_img_info.fragment_cnt = 1;
		tn_img_info.p_fragments[0].width = p_args->thumbnail.width;
		tn_img_info.p_fragments[0].height = p_args->thumbnail.height;
		if (tn_img_info.color_format <= YCBCRLP_H1V1)
		{
			tn_img_info.p_fragments[0].color.yuv.luma_buf =
				tn_luma_buf;
			tn_img_info.p_fragments[0].color.yuv.chroma_buf =
				tn_chroma_buf;
		}
		else if ((tn_img_info.color_format >= JPEG_BITSTREAM_H2V2)
			 && (tn_img_info.color_format <= YCBCRLP_H1V1))
		{
			tn_img_info.p_fragments[0].color.bitstream.
				bitstream_buf = tn_luma_buf;
		}
		source.p_thumbnail = &tn_img_info;
	}
#endif
	rc = jpege_set_source (encoder, &source);
	if (JPEG_FAILED (rc))
	{
		LOG_D ("hw_engine_encode: jpege_set_source failed\n");
		return OS_THREAD_FUNC_RET_FAILED;
	}

	// Loop to perform n back-to-back encoding (to the same output file)
	for (i = 0; i < p_args->back_to_back_count; i++)
	{
		if (p_args->back_to_back_count > 1)
		{
			LOG_D ("Encoding (%d-th time)\n", i + 1);
		}

		// Open output file
#if 0
		p_thread_arg->output_handler_args.fout =
			fopen (output_filename, "wb");
		if (!p_thread_arg->output_handler_args.fout)
		{
			LOG_D ("hw_engine_encode: failed to open output file: %s\n", output_filename);
			return OS_THREAD_FUNC_RET_FAILED;
		}
#endif
		p_thread_arg->output_handler_args.size = 0;
		p_thread_arg->output_handler_args.nowrite =
			p_args->output_nowrite;

		// Set destination information
		dest.p_output_handler = encoder_output_handler;
		dest.p_arg = (void *) &p_thread_arg->output_handler_args;
		dest.buffer_cnt = 2;
		if (JPEG_FAILED (jpeg_buffer_init (&buffers[0])) ||
		    JPEG_FAILED (jpeg_buffer_init (&buffers[1])) ||
		    JPEG_FAILED (jpeg_buffer_allocate
				 (buffers[0], 8192, use_pmem))
		    ||
		    JPEG_FAILED (jpeg_buffer_allocate
				 (buffers[1], 8192, use_pmem)))
		{
			LOG_D ("hw_engine_encode: failed to allocate destination buffers\n");
			jpeg_buffer_destroy (&buffers[0]);
			jpeg_buffer_destroy (&buffers[1]);
			dest.buffer_cnt = 0;
		}

		dest.p_buffer = &buffers[0];
		rc = jpege_set_destination (encoder, &dest);
		if (JPEG_FAILED (rc))
		{
			LOG_D ("hw_engine_encode: jpege_set_destination failed\n");
			return OS_THREAD_FUNC_RET_FAILED;
		}

		// Set default configuration
		rc = jpege_get_default_config (&config);
		if (JPEG_FAILED (rc))
		{
			LOG_D ("hw_engine_encode: jpege_set_default_config failed\n");
			return OS_THREAD_FUNC_RET_FAILED;
		}
		// Set custom configuration
		config.main_cfg.rotation_degree_clk = p_args->rotation;
		if (p_args->main.quality)
			config.main_cfg.quality = p_args->main.quality;
#if 0
		config.thumbnail_cfg.rotation_degree_clk = p_args->rotation;
		if (p_args->thumbnail.quality)
			config.thumbnail_cfg.quality =
				p_args->thumbnail.quality;
		config.thumbnail_present = p_args->encode_thumbnail;
#endif
		if (p_args->preference < JPEG_ENCODER_PREF_MAX)
		{
			config.preference =
				(jpege_preference_t) p_args->preference;
		}
#if 0
		// Set scale cfg
		config.main_cfg.scale_cfg = p_args->main_scale_cfg;
		config.thumbnail_cfg.scale_cfg = p_args->tn_scale_cfg;
#endif
		// Set target file size
		config.target_filesize = p_args->target_filesize;

		// Start encoding
		p_thread_arg->encoding = true;
#if defined (DEBUG_TIMER) && (1 == DEBUG_TIMER)
		os_timer_t os_timer;
		if (os_timer_start (&os_timer) < 0)
		{
			LOG_D ("hw_engine_encode: failed to get start time\n");
		}
#endif
		rc = jpege_start (encoder, &config, NULL);
		if (JPEG_FAILED (rc))
		{
			LOG_D ("hw_engine_encode: jpege_start failed\n");
			return OS_THREAD_FUNC_RET_FAILED;
		}
		LOG_D ("hw_engine_encode: jpege_start succeeded\n");

		// Abort
		if (p_args->abort_time)
		{
			os_mutex_lock (&p_thread_arg->output_handler_args.
				       mutex);
			while (p_thread_arg->encoding)
			{
				rc = os_cond_timedwait
					(&p_thread_arg->output_handler_args.
					 cond,
					 &p_thread_arg->output_handler_args.
					 mutex, p_args->abort_time);
				if (rc == JPEGERR_ETIMEDOUT)
				{
					// Do abort
					LOG_D ("hw_engine_encode: abort now...\n");
					os_mutex_unlock
						(&p_thread_arg->
						 output_handler_args.mutex);
					rc = jpege_abort (encoder);
					if (rc)
					{
						LOG_D ("hw_engine_encode: jpege_abort failed: %d\n", rc);
						return OS_THREAD_FUNC_RET_FAILED;
					}
					break;
				}
			}
			if (p_thread_arg->encoding)
				os_mutex_unlock
					(&p_thread_arg->output_handler_args.
					 mutex);
		}
		else
		{
			// Wait until encoding is done or stopped due to error
			os_mutex_lock (&p_thread_arg->output_handler_args.
				       mutex);
			while (p_thread_arg->encoding)
			{
				os_cond_wait
					(&p_thread_arg->
					 output_handler_args.cond,
					 &p_thread_arg->output_handler_args.
					 mutex);
			}
			os_mutex_unlock (&p_thread_arg->
					 output_handler_args.mutex);
		}

		// Latency measurement
#if defined (DEBUG_TIMER) && (1 == DEBUG_TIMER)
		{
			int diff;
			// Display the time elapsed
			if (os_timer_get_elapsed (&os_timer, &diff, 0) < 0)
			{
				LOG_D ("hw_engine_encode: failed to get elapsed time\n");
			}
			else
			{
				if (p_args->abort_time)
				{
					if (p_thread_arg->encoding)
					{
						LOG_D ("hw_engine_encode: encoding aborted successfully after %d ms\n", diff);
					}
					else
					{
						LOG_D ("hw_engine_encode: encoding is done before abort is issued. " "encode time: %d ms\n", diff);
					}
				}
				else
				{
					LOG_D(  "hw_engine_encode: encode time: %d ms\n", diff);
					LOG_D ("hw_engine_encode: jpeg size: %ld bytes\n", (long) p_thread_arg->output_handler_args.size);
				}
			}
		}
#endif
		// Clean up allocated dest buffers
		jpeg_buffer_destroy (&buffers[0]);
		jpeg_buffer_destroy (&buffers[1]);

	}

	// Clean up allocated source buffers
	jpeg_buffer_destroy (&main_luma_buf);
	jpeg_buffer_destroy (&main_chroma_buf);
	jpeg_buffer_destroy (&tn_luma_buf);
	jpeg_buffer_destroy (&tn_chroma_buf);

	// Clean up encoder
	jpege_destroy (&encoder);
	LOG_D ("hw_engine_encode: jpege_destroy done\n");

	return OS_THREAD_FUNC_RET_SUCCEEDED;
}

void encoder_event_handler (void *p_user_data, jpeg_event_t event, void *p_arg)
{
	thread_ctrl_blk_t *p_thread_arg = (thread_ctrl_blk_t *) p_user_data;

	LOG_D ("encoder_event_handler: %s\n", event_to_string[event]);
	// If it is not a warning event, encoder has stopped; Signal
	// main thread to clean up
	if (event == JPEG_EVENT_DONE || event == JPEG_EVENT_ERROR)
	{
		os_mutex_lock (&p_thread_arg->output_handler_args.mutex);
		p_thread_arg->encoding = false;
		os_cond_signal (&p_thread_arg->output_handler_args.cond);
		os_mutex_unlock (&p_thread_arg->output_handler_args.mutex);
	}
}

int encoder_output_handler (void *p_user_data, void *p_arg, jpeg_buffer_t buffer, uint8_t last_buf_flag)
{
	output_handler_args_t *p_output_args =
		(output_handler_args_t *) p_arg;
	uint8_t *buf_ptr = NULL;
	uint32_t buf_size = 0;
	uint32_t rc;
	thread_ctrl_blk_t* p_thread_arg = (thread_ctrl_blk_t *)p_user_data;

	os_mutex_lock (&p_output_args->mutex);
#if 0
	if (!p_output_args->fout)
	{
		LOG_D ("encoder_output_handler: invalid p_arg\n");
		return;
	}
#endif
	if (JPEG_FAILED (jpeg_buffer_get_actual_size (buffer, &buf_size)))
		LOG_D ("encoder_output_handler: jpeg_buffer_get_actual_size failed\n");
	if (JPEG_FAILED (jpeg_buffer_get_addr (buffer, &buf_ptr)))
		LOG_D ("encoder_output_handler: jpeg_buffer_get_addr failed\n");

	memcpy (out_buf_ptr, buf_ptr, buf_size);

	out_buf_ptr += buf_size;
	out_buf_size += buf_size;

	LOG_D ("encoder_output_handler: writing 0x%p (%d bytes)\n", buf_ptr,
	       buf_size);
#if 0
	//LOG_D(  "encoder_output_handler: writing 0x%p (%d bytes)\n", buf_ptr, buf_size);
	if (!p_output_args->nowrite)
	{
		fwrite (buf_ptr, 1, buf_size, p_output_args->fout);
	}
#endif
	p_output_args->size += buf_size;
	os_mutex_unlock (&p_output_args->mutex);

	if (last_buf_flag)
	{
		LOG_D("encoder_output_handler:  received last output buffer\n");
	}

	// Set the output buffer offset to zero
	if (JPEG_FAILED(jpeg_buffer_set_actual_size(buffer, 0)))
		LOG_D("encoder_output_handler: jpeg_buffer_set_actual_size failed\n");

	// Enqueue back the output buffer to queue
	rc = jpege_enqueue_output_buffer((p_thread_arg->encoder),
					&buffer, 1);
	if (JPEG_FAILED(rc))
	{
		LOG_D("encoder_output_handler: jpege_enqueue_output_buffer failed\n");
		return 1;
	}

	return 0;
}


static int use_padded_buffer (encoder_args_t * p_args)
{
	int use_padding = false;
#ifdef GEMINI_HW_ENCODE
	if (((p_args->main.format == YCRCBLP_H2V2)
	     || (p_args->main.format == YCBCRLP_H2V2))
	    &&
	    (((jpege_preference_t) p_args->preference ==
	      JPEG_ENCODER_PREF_HW_ACCELERATED_ONLY)
	     || ((jpege_preference_t) p_args->preference ==
		 JPEG_ENCODER_PREF_HW_ACCELERATED_PREFERRED)))
	{
		uint32_t actual_size =
			p_args->main.height * p_args->main.width;
		uint32_t padded_size =
			CEILING16 (p_args->main.width) *
			CEILING16 (p_args->main.height);
		if (actual_size != padded_size)
		{
			use_padding = true;	/* For gemini */
		}
	}
#endif
	return use_padding;
}

int read_bytes_from_buffer (int width, int height,
			jpeg_buffer_t * p_luma_buf,
			jpeg_buffer_t * p_chroma_buf,
			uint8_t use_pmem,
			encoder_args_t * p_args, uint8_t is_thumbnail)
{
	long file_size = width * height * 1.5;
	uint8_t *buf_ptr;
	uint32_t size = width * height;
	int start_offset = 0;
	int use_offset = (p_args->rotation == 90)
		|| (p_args->rotation == 180);
	int cbcr_size = 0;

	if (!p_args)
	{
		LOG_D ("read_bytes_from_buffer: p_args is NULL\n");
		return 1;
	}
	int use_padding = is_thumbnail ? false : use_padded_buffer (p_args);
	LOG_D ("read_bytes_from_buffer: use_padding %d\n", use_padding);


	uint32_t actual_size = width * height;
	uint32_t padded_size = CEILING16 (width) * CEILING16 (height);
	LOG_D ("read_bytes_from_buffer: file_size %ld padded_size %d\n",
	       file_size, padded_size);
	if (use_padding)
	{
		size = padded_size;
		if (jpeg_buffer_allocate
		    (*p_luma_buf, padded_size * 1.5, use_pmem))
		{
			return 1;
		}
	}
	else
	{
		if (jpeg_buffer_allocate (*p_luma_buf, file_size, use_pmem))
		{
			return 1;
		}
	}

	jpeg_buffer_attach_existing (*p_chroma_buf, *p_luma_buf, size);

	if (use_padding)
	{
		size = actual_size;
	}

	// Read the content
	if (JPEG_FAILED (jpeg_buffer_get_addr (*p_luma_buf, &buf_ptr)))
	{
		return 1;
	}
	if (use_padding && use_offset)
		start_offset = padded_size - actual_size;

	memcpy (buf_ptr + start_offset, p_args->main.y_buf, size);

	if (JPEG_FAILED (jpeg_buffer_get_addr (*p_chroma_buf, &buf_ptr)))
	{
		return 1;
	}
	cbcr_size = file_size - size;
	if (use_padding)
	{
		cbcr_size = size >> 1;
		if (use_offset)
		{
			start_offset = (padded_size - actual_size) >> 1;
		}
	}

	memcpy (buf_ptr + start_offset, p_args->main.uv_buf, cbcr_size);

	if (use_padding)
	{
		size = padded_size;
	}

	jpeg_buffer_set_actual_size (*p_luma_buf, size);
	if (use_padding)
	{
		jpeg_buffer_set_actual_size (*p_chroma_buf, padded_size >> 1);

		if (use_offset)
		{
			jpeg_buffer_set_start_offset (*p_luma_buf,
						      (padded_size -
						       actual_size));
			jpeg_buffer_set_start_offset (*p_chroma_buf,
						      ((padded_size -
							actual_size) >> 1));
		}
	}
	else
	{
		jpeg_buffer_set_actual_size (*p_chroma_buf, file_size - size);
	}

	return 0;
}

int hw_encode_alloc_memory(void)
{
	return 0;
}

void free_memory (void)
{
	free (out_buf_ptr);
	out_buf_ptr = NULL;

	free (g_y);
	g_y = NULL;

	free (g_uv);
	g_uv = NULL;

	free (in_buf);
	in_buf = NULL;

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

int hw_encode_init (uint32_t width, uint32_t height, uint32_t bytes_per_pixel)
{
	int adjust = 0;
	init_ycbcr ();

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
	if (-1 == alloc_memory (&g_y, width * height))
	{
		LOG_D ("Malloc memory error!\n");
		return -1;
	}
	if (-1 == alloc_memory (&g_uv, width * height))
	{
		LOG_D ("Malloc memory error!\n");
		return -1;
	}

	return 0;
}

int hw_encode (struct hw_enc_param *param)
{
	yuv_args_t yuv_args;
	int ret = 0;
	int elapsed_timer;
	os_timer_t encode_timer, memcpy_timer, convert_timer;
	os_timer_start (&encode_timer);

	LOG_D (" %s func\n", __func__);

	if (param->in_buf == NULL)
	{
		LOG_D ("JPEG input buffer is NULL!!\n");
		return -1;
	}

	os_timer_start (&memcpy_timer);
	memcpy (in_buf, param->in_buf,
		param->height * param->width * param->bytes_per_pixel);
	os_timer_get_elapsed (&memcpy_timer, &elapsed_timer, 0);
	LOG_D ("hw_encode memcpy buffer time is about: %d ms\n", elapsed_timer);

	yuv_args.height = param->height;
	yuv_args.width = param->width;
	yuv_args.preference = param->preference;
	yuv_args.quality = param->quality;

	os_timer_start (&convert_timer);

	rgb2yuv32bit (in_buf, g_y, g_uv, param->width, param->height);

	os_timer_get_elapsed (&convert_timer, &elapsed_timer, 0);

	LOG_D ("hw_encode rgb2yuv time is about: %d ms\n", elapsed_timer);

	ret = jpeg_encode (g_y, g_uv, &yuv_args);
	if(ret)
	{
		LOG_D("Jpeg_encoder failed!\n");
		return ret;
	}

	out_buf_ptr -= out_buf_size;
	param->out_buf = out_buf_ptr;
	param->outdata_size = out_buf_size;

	LOG_D ("JPEG OUT SIZE = %d\n", out_buf_size);
	if(!out_buf_size)
	{
		LOGE("Out size is 0!\n");
		return -1;
	}

	os_timer_get_elapsed (&encode_timer, &elapsed_timer, 0);

	LOG_D ("hw_encode total time is about: %d ms\n", elapsed_timer);

	return 0;

}

void hw_encoded (void)
{
	memset (out_buf_ptr, 0, out_buf_size);
	out_buf_size = 0;
	return;
}
