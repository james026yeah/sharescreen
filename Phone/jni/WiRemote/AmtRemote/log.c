#include <stdio.h>
#include <stdarg.h>
#include "log.h"

void __amt_log(int tag, struct amt_log_handle *handle, const char *fmt, ...)
{
	if(handle->log_cb && (handle->log_flag & tag))
	{
		va_list ap;
		char buf[1024];
		va_start(ap, fmt);
		vsnprintf(buf, 1024, fmt, ap);
		va_end(ap);
		handle->log_cb(tag, buf);
	}
}

void amt_log_register(struct amt_log_handle *handle, amt_log_callback cb)
{
	handle->log_flag = 0;
	handle->log_cb = cb;
}

void amt_log_control(struct amt_log_handle *handle, int tag_on)
{
	handle->log_flag = tag_on & CB_LOGA;
}

