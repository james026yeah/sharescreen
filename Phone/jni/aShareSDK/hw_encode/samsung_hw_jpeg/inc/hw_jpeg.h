#ifndef _HW_JPEG_H_
#define _HW_JPEG_H_
#include "JpegEncoder.h"

extern "C" {
	#include "jpeg_enc.h"
	int hw_encode(struct hw_enc_param *param);
	void hw_encoded(void);
	int hw_encode_alloc_memory(uint32_t, uint32_t, uint32_t);
	void free_memory(void);
	int hw_encode_init(uint32_t, uint32_t, uint32_t);
} 
#endif
