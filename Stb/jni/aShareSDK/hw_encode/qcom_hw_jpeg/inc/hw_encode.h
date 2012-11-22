#ifndef __HW_ENCODE_H__
#define __HW_ENCODE_H__

#include "rgb2yuv.h"
#include "jpege.h"
#include "jpegd.h"
#include "os_thread.h"
#include "os_timer.h"
#include "jpeg_enc.h"


int hw_encode_init(uint32_t width, uint32_t height, uint32_t bytes_per_pixel);
int hw_encode (struct hw_enc_param *);
void hw_encoded(void);
void free_memory(void);
int hw_encode_alloc_memory(void);
#endif
