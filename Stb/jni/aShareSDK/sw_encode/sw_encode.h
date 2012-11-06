#ifndef __SW_ENCODE_H__
#define __SW_ENCODE_H__

#include "jpeg_enc.h"

int hw_encode_init(uint32_t width, uint32_t height, uint32_t bytes_per_pixel);
int hw_encode (struct hw_enc_param *);
void hw_encoded(void);
void free_memory(void);
void rgb_scale(char *dst, char *src, unsigned int dst_width, unsigned
		int dst_height, unsigned int src_width, unsigned int src_height);
#endif
