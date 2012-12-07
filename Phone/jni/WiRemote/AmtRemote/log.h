#ifndef _LOG_H
#define _LOG_H

#include "amt_remote.h"

struct amt_log_handle
{
	int log_flag;
	amt_log_callback log_cb;
};

void __amt_log(int tag, struct amt_log_handle *handle, const char *fmt, ...);
#define LOGD(log, ...) __amt_log(CB_LOGD, log, __VA_ARGS__)
#define LOGE(log, ...) __amt_log(CB_LOGE, log, __VA_ARGS__)
#define LOGI(log, ...) __amt_log(CB_LOGI, log, __VA_ARGS__)
#define LOGW(log, ...) __amt_log(CB_LOGW, log, __VA_ARGS__)
#define LOGV(log, ...) __amt_log(CB_LOGV, log, __VA_ARGS__)
#define LOGH(log, ...) __amt_log(CB_LOGH, log, __VA_ARGS__) //HOOK
void amt_log_register(struct amt_log_handle *handle, amt_log_callback cb);
void amt_log_control(struct amt_log_handle *handle, int tag_on);
#endif

